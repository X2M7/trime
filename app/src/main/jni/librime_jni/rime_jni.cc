// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

#include <rime_api.h>

#include <memory>
#include <mutex>
#include <algorithm>
#include <string>
#include <vector>

#include "frontend.h"
#include "jni-utils.h"
#include "objconv.h"
#include "session.h"
#include "t9.h"
#include <rime/key_event.h>
#include <rime/dict/dictionary.h>
#include <rime/schema.h>
#include <rime/ticket.h>

#define MAX_BUFFER_LENGTH 2048

extern void rime_require_module_lua();
extern void rime_require_module_octagram();
extern void rime_require_module_predict();
// librime is compiled as a static library, we have to link modules explicitly
static void declare_librime_module_dependencies() {
  rime_require_module_lua();
  rime_require_module_octagram();
  rime_require_module_predict();
}

class Rime {
 public:
  Rime() : rime(rime_get_api()) {}
  Rime(Rime const&) = delete;
  void operator=(Rime const&) = delete;

  static Rime& Instance() {
    static Rime instance;
    return instance;
  }

  void startup(bool fullCheck,
               const RimeNotificationHandler& notificationHandler) {
    if (!rime) return;
    const char* userDir = getenv("RIME_USER_DATA_DIR");
    const char* sharedDir = getenv("RIME_SHARED_DATA_DIR");
    const char* versionName = getenv("RIME_DISTRIBUTION_VERSION");

    RIME_STRUCT(RimeTraits, trime_traits)
    trime_traits.shared_data_dir = sharedDir;
    trime_traits.user_data_dir = userDir;
    trime_traits.log_dir = "";  // set empty log_dir to log to logcat only
    // Keep warnings/errors without logging every vocabulary entry or input query.
    trime_traits.min_log_level = 1;
    trime_traits.app_name = "rime.trime";
    trime_traits.distribution_name = "Trime";
    trime_traits.distribution_code_name = "trime";
    trime_traits.distribution_version = versionName;

    // Logging/module declarations are process-wide. initialize() below reapplies
    // directories and traits and reloads modules after every finalize().
    static std::once_flag setup_once;
    std::call_once(setup_once, [&] { rime->setup(&trime_traits); });
    rime->initialize(&trime_traits);
    rime->set_notification_handler(notificationHandler, GlobalRef->jvm);
    rime->start_maintenance(fullCheck);
    // Startup runs on RimeDispatcher, never the Android UI thread. Do not
    // publish READY or create a session while deployment is still writing data.
    rime->join_maintenance_thread();
  }

  bool deploySchema(std::string_view schemaFile) {
    return rime->deploy_schema(schemaFile.data());
  }

  bool hasUsableDictionary() {
    auto current = rime::Service::instance().GetSession(session());
    if (!current || !current->schema() || current->schema()->schema_id() == ".default")
      return false;
    auto component = rime::Dictionary::Require("dictionary");
    if (!component) return false;
    std::unique_ptr<rime::Dictionary> dictionary(
        component->Create(rime::Ticket(current->schema(), "translator")));
    return dictionary && dictionary->Load();
  }

  bool deployConfigFile(std::string_view configFile,
                        std::string_view versionKey) {
    return rime->deploy_config_file(configFile.data(), versionKey.data());
  }

  bool processKey(int keycode, int mask) {
    auto current = rime::Service::instance().GetSession(session());
    if (t9_.ProcessKey(current.get(), keycode, mask)) return true;
    return rime->process_key(session(), keycode, mask);
  }

  bool simulateKeySequence(const std::string& sequence) {
    const auto state = t9Snapshot();
    if (std::none_of(state.segments.begin(), state.segments.end(),
        [](const auto& span) { return span.locked; }))
      return rime->simulate_key_sequence(session(), sequence.data());
    rime::KeySequence keys;
    if (!keys.Parse(sequence)) return false;
    for (const auto& key : keys) processKey(key.keycode(), key.modifier());
    return true;
  }

  bool commitComposition() { return rime->commit_composition(session()); }

  void clearComposition() {
    t9_.Clear();
    rime->clear_composition(session());
  }

  trime::T9Snapshot t9Snapshot() {
    auto current = rime::Service::instance().GetSession(session());
    return t9_.Snapshot(current.get());
  }

  void setT9AssistOptions(int options) { t9_.SetAssistOptions(options); }

  bool t9Action(int revision, int action, int start, int end,
                const std::string& spelling) {
    auto current = rime::Service::instance().GetSession(session());
    return t9_.Act(current.get(), revision, action, start, end, spelling);
  }

  std::unique_ptr<CommitProto> commit() {
    RIME_STRUCT(RimeCommit, data)
    if (rime->get_commit(session(), &data)) {
      auto p = std::make_unique<CommitProto>(&data);
      p->text = t9_.CommitText(*p->text);
      rime->free_commit(&data);
      return p;
    }
    return std::make_unique<CommitProto>();
  }

  std::unique_ptr<ContextProto> context(bool includeMenu = true) {
    RIME_STRUCT(RimeContext, data)
    auto s = session();
    rime::Preedit preedit;
    auto current = rime::Service::instance().GetSession(s);
    const bool t9Preedit = t9_.GetPreedit(current.get(), &preedit);
    if (rime->get_context(s, &data)) {
      auto input = rime->get_input(s);
      auto caretPos = rime->get_caret_pos(s);
      auto p =
          std::make_unique<ContextProto>(&data, input, caretPos, includeMenu);
      if (t9Preedit) {
        auto begin = preedit.text.data();
        p->composition.length = distance(begin, begin + preedit.text.size());
        p->composition.cursorPos = distance(begin, begin + preedit.caret_pos);
        p->composition.selStart = distance(begin, begin + preedit.sel_start);
        p->composition.selEnd = distance(begin, begin + preedit.sel_end);
        p->composition.preedit = preedit.text;
      }
      rime->free_context(&data);
      return p;
    }
    return std::make_unique<ContextProto>();
  }

  std::unique_ptr<StatusProto> status() {
    RIME_STRUCT(RimeStatus, data)
    if (rime->get_status(session(), &data)) {
      auto p = std::make_unique<StatusProto>(&data);
      rime->free_status(&data);
      return p;
    }
    return std::make_unique<StatusProto>();
  }

  void setOption(std::string_view key, bool value) {
    rime->set_option(session(), key.data(), value);
  }

  bool getOption(std::string_view key) {
    return rime->get_option(session(), key.data());
  }

  std::string currentSchemaId() {
    char result[MAX_BUFFER_LENGTH];
    return rime->get_current_schema(session(), result, MAX_BUFFER_LENGTH)
               ? result
               : "";
  }

  std::vector<SchemaItem> schemaList() {
    std::vector<SchemaItem> result;
    RimeSchemaList list{};
    if (rime->get_schema_list(&list)) {
      result = SchemaItem::fromCList(list);
      rime->free_schema_list(&list);
    }
    return std::move(result);
  }

  bool selectSchema(std::string_view schemaId) {
    t9_.Reset();
    return rime->select_schema(session(), schemaId.data());
  }

  std::string rawInput() {
    auto current = rime::Service::instance().GetSession(session());
    return t9_.RawInput(current.get());
  }

  size_t caretPosition() { return rime->get_caret_pos(session()); }

  void setCaretPosition(size_t caretPos) {
    auto current = rime::Service::instance().GetSession(session());
    if (t9_.MoveCaret(current.get(), caretPos)) return;
    rime->set_caret_pos(session(), caretPos);
  }

  bool selectCandidate(size_t index, bool global) {
    if (global) {
      return rime->select_candidate(session(), index);
    } else {
      return rime->select_candidate_on_current_page(session(), index);
    }
  }

  bool deleteCandidate(size_t index, bool global) {
    if (global) {
      return rime->delete_candidate(session(), index);
    } else {
      return rime->delete_candidate_on_current_page(session(), index);
    }
  }

  bool changePage(bool backward) {
    return rime->change_page(session(), backward);
  }

  std::vector<CandidateProto> getCandidates(int startIndex, int limit) {
    std::vector<CandidateProto> result;
    result.reserve(limit);
    RimeCandidateListIterator iter{};
    if (rime->candidate_list_from_index(session(), &iter, startIndex)) {
      int count = 0;
      while (rime->candidate_list_next(&iter)) {
        if (count >= limit) break;
        result.emplace_back(iter.candidate);
        ++count;
      }
      rime->candidate_list_end(&iter);
    }
    return std::move(result);
  }

  std::tuple<int, int, std::vector<CandidateProto>> getBulkCandidates() {
    constexpr int limit = 16;
    auto list = getCandidates(0, limit);
    // use -1 to indicate it's not sure how many candidates now
    auto size = list.size() < limit ? list.size() : -1;
    auto highlighted = rime_get_highlighted_candidate_index(session());
    return std::make_tuple(size, highlighted, std::move(list));
  }

  void exit() {
    t9_.Reset();
    session_.reset();
    rime->finalize();
  }

  bool sync() {
    t9_.Reset();
    session_.reset();
    return rime->sync_user_data();
  }

 private:
  RimeApi* rime;
  trime::T9 t9_;
  std::shared_ptr<SessionHolder> session_;

  RimeSessionId session(bool requestNewSession = true) {
    if (!session_ && requestNewSession) {
      try {
        auto newSession = std::make_shared<SessionHolder>();
        session_ = newSession;
      } catch (...) {
        session_ = nullptr;
      }
    }
    if (!session_) {
      return 0;
    }
    return session_->id();
  }
};

GlobalRefSingleton* GlobalRef;

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeT9State(JNIEnv* env, jclass, jint options) {
  Rime::Instance().setT9AssistOptions(options);
  auto state = Rime::Instance().t9Snapshot();
  JRef<jclass> span_class(env, env->FindClass("com/osfans/trime/core/T9SpanProto"));
  auto span_init = env->GetMethodID(span_class, "<init>", "(IILjava/lang/String;ZZI)V");
  auto spans = [&](const std::vector<trime::T9Span>& items) {
    auto result = env->NewObjectArray(items.size(), span_class, nullptr);
    for (size_t i = 0; i < items.size(); ++i) {
      const auto& s = items[i];
      JRef item(env, env->NewObject(span_class, span_init, s.start, s.end,
          *JString(env, s.spelling), s.completion, s.locked, s.sources));
      env->SetObjectArrayElement(result, i, item);
    }
    return result;
  };
  JRef segments(env, spans(state.segments));
  JRef choices(env, spans(state.choices));
  JRef<jclass> state_class(env, env->FindClass("com/osfans/trime/core/T9StateProto"));
  auto init = env->GetMethodID(state_class, "<init>",
      "(ZILjava/lang/String;IZ[Lcom/osfans/trime/core/T9SpanProto;[Lcom/osfans/trime/core/T9SpanProto;)V");
  return env->NewObject(state_class, init, state.enabled, state.revision,
      *JString(env, state.input), state.focus, state.can_undo, *segments, *choices);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_performRimeT9Action(JNIEnv* env, jclass,
    jint revision, jint action, jint start, jint end, jstring spelling, jint options) {
  Rime::Instance().setT9AssistOptions(options);
  return Rime::Instance().t9Action(revision, action, start, end, *CString(env, spelling));
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* reserved) {
  GlobalRef = new GlobalRefSingleton(jvm);
  declare_librime_module_dependencies();
  return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL Java_com_osfans_trime_core_Rime_startupRime(
    JNIEnv* env, jclass clazz, jstring shared_dir, jstring user_dir,
    jstring version_name, jboolean full_check) {
  jniCall(env, [&] {
  // for rime shared data dir
  setenv("RIME_SHARED_DATA_DIR", CString(env, shared_dir), 1);
  // for rime user data dir
  setenv("RIME_USER_DATA_DIR", CString(env, user_dir), 1);
  setenv("RIME_DISTRIBUTION_VERSION", CString(env, version_name), 1);

  auto notificationHandler = [](void* context_object, RimeSessionId session_id,
                                const char* message_type,
                                const char* message_value) {
    auto env = GlobalRef->AttachEnv();
    int type = 0;  // unknown
    if (strcmp(message_type, "schema") == 0) {
      type = 1;
    } else if (strcmp(message_type, "option") == 0) {
      type = 2;
    } else if (strcmp(message_type, "deploy") == 0) {
      type = 3;
    }
    auto vararg = JRef<jobjectArray>(
        env, env->NewObjectArray(1, GlobalRef->Object, nullptr));
    env->SetObjectArrayElement(vararg, 0, JString(env, message_value));
    env->CallStaticVoidMethod(GlobalRef->Rime, GlobalRef->HandleRimeMessage,
                              type, *vararg);
  };

  Rime::Instance().startup(full_check, notificationHandler);
  });
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_exitRime(JNIEnv* env, jclass /* thiz */) {
  jniCall(env, [&] { Rime::Instance().exit(); });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_hasUsableRimeDictionary(JNIEnv* env, jclass /* thiz */) {
  return jniCall(env, [&] { return Rime::Instance().hasUsableDictionary(); });
}

// deployment
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deployRimeSchemaFile(JNIEnv* env,
                                                     jclass /* thiz */,
                                                     jstring schema_file) {
  return jniCall(env, [&] { return Rime::Instance().deploySchema(*CString(env, schema_file)); });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deployRimeConfigFile(JNIEnv* env,
                                                     jclass /* thiz */,
                                                     jstring file_name,
                                                     jstring version_key) {
  return jniCall(env, [&] { return Rime::Instance().deployConfigFile(*CString(env, file_name),
                                           *CString(env, version_key)); });
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_syncRimeUserData(JNIEnv* env,
                                                 jclass /* thiz */) {
  return jniCall(env, [&] { return Rime::Instance().sync(); });
}

// input
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_processRimeKey(JNIEnv* env, jclass /* thiz */,
                                               jint keycode, jint mask) {
  return Rime::Instance().processKey(keycode, mask);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_commitRimeComposition(JNIEnv* env,
                                                      jclass /* thiz */) {
  return Rime::Instance().commitComposition();
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_clearRimeComposition(JNIEnv* env,
                                                     jclass /* thiz */) {
  Rime::Instance().clearComposition();
}

// output
extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeCommit(JNIEnv* env, jclass /* thiz */) {
  auto commit = Rime::Instance().commit();
  return rimeCommitToJObject(env, *commit);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeContext(JNIEnv* env, jclass /* thiz */) {
  auto context = Rime::Instance().context();
  return rimeContextToJObject(env, *context);
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeStatus(JNIEnv* env, jclass /* thiz */) {
  auto status = Rime::Instance().status();
  return rimeStatusToJObject(env, *status);
}

// runtime options
extern "C" JNIEXPORT void JNICALL Java_com_osfans_trime_core_Rime_setRimeOption(
    JNIEnv* env, jclass /* thiz */, jstring option, jboolean value) {
  Rime::Instance().setOption(*CString(env, option), value);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_getRimeOption(JNIEnv* env, jclass /* thiz */,
                                              jstring option) {
  return Rime::Instance().getOption(*CString(env, option));
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_osfans_trime_core_Rime_getRimeSchemaList(JNIEnv* env,
                                                  jclass /* thiz */) {
  return rimeSchemaListToJObjectArray(env, Rime::Instance().schemaList());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_osfans_trime_core_Rime_getCurrentRimeSchema(JNIEnv* env,
                                                     jclass /* thiz */) {
  return makeJavaString(env, Rime::Instance().currentSchemaId());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_selectRimeSchema(JNIEnv* env, jclass /* thiz */,
                                                 jstring schema_id) {
  return Rime::Instance().selectSchema(*CString(env, schema_id));
}

// testing
extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_simulateRimeKeySequence(JNIEnv* env,
                                                        jclass /* thiz */,
                                                        jstring key_sequence) {
  return Rime::Instance().simulateKeySequence(CString(env, key_sequence));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_osfans_trime_core_Rime_getRimeRawInput(JNIEnv* env,
                                                jclass /* thiz */) {
  return makeJavaString(env, Rime::Instance().rawInput());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_osfans_trime_core_Rime_getRimeCaretPos(JNIEnv* env,
                                                jclass /* thiz */) {
  return static_cast<jint>(Rime::Instance().caretPosition());
}

extern "C" JNIEXPORT void JNICALL
Java_com_osfans_trime_core_Rime_setRimeCaretPos(JNIEnv* env, jclass /* thiz */,
                                                jint caret_pos) {
  Rime::Instance().setCaretPosition(caret_pos);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_selectRimeCandidate(JNIEnv* env,
                                                    jclass /* thiz */,
                                                    jint index,
                                                    jboolean global) {
  return Rime::Instance().selectCandidate(index, global);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_deleteRimeCandidate(JNIEnv* env,
                                                    jclass /* thiz */,
                                                    jint index,
                                                    jboolean global) {
  return Rime::Instance().deleteCandidate(index, global);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_osfans_trime_core_Rime_changeRimeCandidatePage(JNIEnv* env,
                                                        jclass clazz,
                                                        jboolean backward) {
  return Rime::Instance().changePage(backward);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_osfans_trime_core_Rime_getRimeCandidates(JNIEnv* env, jclass clazz,
                                                  jint start_index,
                                                  jint limit) {
  return rimeCandidateListToJObjectArray(
      env, Rime::Instance().getCandidates(start_index, limit));
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_osfans_trime_core_Rime_getRimeResponse(JNIEnv* env, jclass clazz,
                                                jboolean paging_mode) {
  return jniCall(env, [&]() -> jobject {
  auto commit = Rime::Instance().commit();
  // the menu is only needed in paging mode, otherwise its candidates would be
  // duplicated by the bulk candidates query below
  auto context = Rime::Instance().context(paging_mode);
  auto status = Rime::Instance().status();
  auto jCommit = JRef(env, rimeCommitToJObject(env, *commit));
  auto jComposition =
      JRef(env, rimeCompositionToJObject(env, context->composition));
  auto jStatus = JRef(env, rimeStatusToJObject(env, *status));
  // keep the local references alive until RimeResponse is constructed below
  jobject jCandidates = nullptr;
  if (paging_mode) {
    // the candidate layout is queried right where the page is built, so the
    // consumer does not need a separate rime option round-trip per key
    auto& rime = Rime::Instance();
    bool is_horizontal_layout =
        rime.getOption("_linear") || rime.getOption("_horizontal");
    jCandidates =
        rimeCandidatesPagedToJObject(env, context->menu, is_horizontal_layout);
  } else {
    auto [size, highlighted, list] = Rime::Instance().getBulkCandidates();
    auto jList =
        JRef<jobjectArray>(env, rimeCandidateListToJObjectArray(env, list));
    jCandidates =
        env->NewObject(GlobalRef->CandidatesBulk, GlobalRef->CandidatesBulkInit,
                       size, highlighted, *jList);
  }
  return rimeResponseToJObject(env, jCommit, jComposition, jCandidates,
                               jStatus);
  });
}
