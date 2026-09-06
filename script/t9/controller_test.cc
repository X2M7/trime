// SPDX-License-Identifier: GPL-3.0-or-later
#include <rime/context.h>
#include <rime/key_event.h>
#include <rime/lever/user_dict_manager.h>
#include <rime/schema.h>
#include <rime_api.h>

#include <algorithm>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>

#include "../../app/src/main/jni/librime_jni/t9.h"

using namespace rime;
static int checks = 0;
static void check(bool ok, const std::string& message) {
  if (!ok) throw std::runtime_error(message);
  std::cout << "PASS " << ++checks << " " << message << std::endl;
}

int main(int argc, char** argv) {
  if (argc != 3 && argc != 4) return 2;
  auto api = rime_get_api();
  RIME_STRUCT(RimeTraits, traits);
  traits.shared_data_dir = argv[1];
  traits.user_data_dir = argv[2];
  traits.app_name = "rime.trime_t9_test";
  traits.log_dir = "";
  traits.min_log_level = 2;
  api->setup(&traits);
  api->initialize(&traits);
  auto learning_check = [&] {
    UserDictManager manager(&Service::instance().deployer());
    auto path = std::string(argv[2]) + "/learning.tsv";
    check(manager.Export("t9_test", rime::path(path)) > 0,
          "learning persisted in Rime user dictionary");
    std::ifstream file(path);
    std::stringstream buffer;
    buffer << file.rdbuf();
    auto data = buffer.str();
    check(data.find(u8"你好\tni hao") != std::string::npos,
          "learned record uses canonical syllables");
    check(data.find('~') == std::string::npos,
          "private spelling never leaks into learned code");
  };
  if (argc == 4) {
    learning_check();
    api->finalize();
    return 0;
  }
  api->start_maintenance(true);
  api->join_maintenance_thread();
  check(api->deploy_schema(
            (std::string(argv[1]) + "/luna_pinyin.schema.yaml").c_str()),
        "deploy actual full-pinyin rules");
  check(api->deploy_schema(
            (std::string(argv[1]) + "/double_pinyin.schema.yaml").c_str()),
        "deploy actual double-pinyin rules");
  auto id = api->create_session();
  check(api->select_schema(id, "luna_pinyin_t9"), "deploy fixture");
  auto session = Service::instance().GetSession(id);
  auto ctx = session->context();
  trime::T9 t9;
  auto no_commit = [&] {
    RIME_STRUCT(RimeCommit, c);
    bool committed = api->get_commit(id, &c);
    if (committed) api->free_commit(&c);
    check(!committed, "no editor commit from syllable/edit action");
  };
  auto type = [&](const std::string& input) {
    for (char c : input) {
      if (!t9.ProcessKey(session.get(), c, 0)) api->process_key(id, c, 0);
      t9.Snapshot(session.get());
    }
    no_commit();
  };
  auto fresh = [&](const std::string& input) {
    ctx->Clear();
    t9.Reset();
    type(input);
  };
  auto focus = [&](int pos) {
    auto s = t9.Snapshot(session.get());
    check(t9.Act(session.get(), s.revision, 0, pos, 0, ""),
          "focus raw position " + std::to_string(pos));
  };
  auto lock = [&](const std::string& spelling, int end) {
    auto s = t9.Snapshot(session.get());
    check(std::any_of(s.choices.begin(), s.choices.end(),
                      [&](const auto& c) {
                        return c.spelling == spelling && c.end == end;
                      }),
          "syllable index contains " + spelling);
    check(t9.Act(session.get(), s.revision, 1, s.focus, end, spelling),
          "lock " + spelling);
    no_commit();
  };
  auto action = [&](int kind, int start = 0) {
    auto s = t9.Snapshot(session.get());
    check(t9.Act(session.get(), s.revision, kind, start, 0, ""),
          "edit action " + std::to_string(kind));
    no_commit();
  };
  auto texts = [&] {
    std::vector<std::string> result;
    RimeCandidateListIterator it{};
    if (api->candidate_list_begin(id, &it)) {
      while (api->candidate_list_next(&it))
        result.emplace_back(it.candidate.text);
      api->candidate_list_end(&it);
    }
    return result;
  };
  auto contains = [&](const std::string& text) {
    auto list = texts();
    return std::find(list.begin(), list.end(), text) != list.end();
  };
  fresh("64");
  auto initial = t9.Snapshot(session.get());
  check(initial.enabled, "T9 enabled");
  lock("ni", 2);
  check(contains(u8"你") && !contains(u8"米"), "ni excludes mi");
  check(!t9.Act(session.get(), initial.revision, 1, 0, 2, "mi"),
        "stale tap rejected");
  focus(0);
  lock("mi", 2);
  check(contains(u8"米") && !contains(u8"你"), "replace ni with mi");
  action(2, 0);
  check(ctx->input() == "64", "unlock restores exact digits");
  action(3);
  check(ctx->input() == "~mi~", "undo unlock");
  fresh("64426");
  lock("ni", 2);
  check(ctx->input() == "~ni~426", "preserve suffix 426");
  check(contains(u8"你好") && !contains(u8"米高"),
        "constrain first syllable only");
  Preedit preedit;
  check(t9.GetPreedit(session.get(), &preedit) && preedit.text == "ni 426",
        "preedit hides private envelope");
  check(t9.MoveCaret(session.get(), 4) && ctx->caret_pos() == 5,
        "display caret maps after digit 4");
  type("2");
  check(t9.Snapshot(session.get()).input == "644226",
        "insert in middle preserves original digit positions");
  check(t9.ProcessKey(session.get(), XK_BackSpace, 0),
        "delete inserted middle digit");
  check(ctx->input() == "~ni~426", "middle delete leaves lock intact");
  check(t9.ProcessKey(session.get(), XK_Home, 0), "home navigation");
  check(t9.ProcessKey(session.get(), XK_Right, 0) && ctx->caret_pos() == 4,
        "right skips atomic lock envelope");
  check(t9.ProcessKey(session.get(), XK_End, 0), "end navigation");
  focus(2);
  lock("hao", 5);
  check(contains(u8"你好") && !contains(u8"泥稿"), "progressive lock");
  type("62");
  check(ctx->input() == "~ni~~hao~62", "continue after locks");
  lock("ma", 7);
  focus(2);
  lock("gao", 5);
  check(ctx->input() == "~ni~~gao~~ma~",
        "replace middle syllable preserves neighbors");
  check(contains(u8"你高吗") && !contains(u8"你好吗"),
        "middle constraint affects Hanzi");
  action(3);
  check(contains(u8"你好吗"), "undo middle replacement");
  check(t9.ProcessKey(session.get(), XK_BackSpace, 0), "backspace handled");
  check(ctx->input() == "~ni~~hao~62", "backspace unlocks last syllable first");
  check(t9.ProcessKey(session.get(), XK_BackSpace, 0),
        "second backspace handled");
  check(t9.Snapshot(session.get()).input == "644266",
        "second backspace deletes one original digit");
  fresh("9426");
  lock("xian", 4);
  check(contains(u8"先") && !contains(u8"西安"),
        "atomic xian cannot split into xi an");
  action(2, 0);
  lock("xi", 2);
  check(ctx->input() == "~xi~26", "shorter segmentation keeps suffix");
  lock("an", 4);
  check(contains(u8"西安"), "xi an remains separately selectable");
  fresh("64'426'62");
  lock("ni", 2);
  check(t9.Snapshot(session.get()).focus == 3,
        "advance to next syllable across explicit separator");
  lock("hao", 6);
  lock("ma", 9);
  check(ctx->input() == "~ni~'~hao~'~ma~" && contains(u8"你好吗"),
        "explicit separators survive progressive locking");
  fresh("646");
  auto completion = t9.Snapshot(session.get());
  check(std::any_of(
            completion.choices.begin(), completion.choices.end(),
            [](const auto& c) { return c.spelling == "ming" && c.completion; }),
        "completion distinguished in engine graph");
  lock("ming", 3);
  check(contains(u8"明"), "completion queries full syllable");
  action(2, 0);
  check(ctx->input() == "646",
        "unlock completion restores short original input");
  fresh("646");
  lock("ming", 3);
  check(t9.RawInput(session.get()) == "646",
        "raw API restores completion input");
  api->process_key(id, XK_Return, 0);
  RIME_STRUCT(RimeCommit, raw_commit);
  check(api->get_commit(id, &raw_commit),
        "Return keeps raw-code commit behavior");
  check(t9.CommitText(raw_commit.text) == "646",
        "raw commit hides exact envelope");
  api->free_commit(&raw_commit);
  fresh("64426");
  lock("ni", 2);
  lock("hao", 5);
  check(t9.CommitText(u8"你好") == u8"你好",
        "normal Hanzi commit is untouched");
  ctx->set_option("ascii_mode", true);
  check(t9.RawInput(session.get()) == "64426",
        "ASCII switch sees original digits");
  ctx->set_option("ascii_mode", false);
  fresh("64426");
  lock("ni", 2);
  auto list = texts();
  auto chosen = std::find(list.begin(), list.end(), u8"你好");
  check(
      chosen != list.end() && api->select_candidate(id, chosen - list.begin()),
      "select Hanzi candidate");
  RIME_STRUCT(RimeCommit, commit);
  check(api->get_commit(id, &commit), "only Hanzi selection commits");
  check(std::string(commit.text) == u8"你好", "editor receives ni hao Hanzi");
  api->free_commit(&commit);
  check(!t9.Snapshot(session.get()).enabled, "commit clears lock state");
  check(!t9.Act(session.get(), initial.revision, 3, 0, 0, ""),
        "undo cannot cross commit");
  fresh("64426");
  lock("ni", 2);
  list = texts();
  chosen = std::find(list.begin(), list.end(), u8"你");
  check(
      chosen != list.end() && api->select_candidate(id, chosen - list.begin()),
      "select partial Hanzi directly");
  no_commit();
  check(t9.GetPreedit(session.get(), &preedit) && preedit.text == u8"你 426",
        "confirmed Hanzi stays visible");
  check(!t9.Snapshot(session.get()).can_undo,
        "syllable undo cannot cross a Hanzi selection");
  lock("hao", 5);
  list = texts();
  chosen = std::find(list.begin(), list.end(), u8"好");
  check(
      chosen != list.end() && api->select_candidate(id, chosen - list.begin()),
      "select remaining Hanzi");
  RIME_STRUCT(RimeCommit, partial_commit);
  check(api->get_commit(id, &partial_commit),
        "partial selections eventually commit");
  check(std::string(partial_commit.text) == u8"你好",
        "partial selection preserves whole input");
  api->free_commit(&partial_commit);
  fresh("64426");
  lock("ni", 2);
  lock("hao", 5);
  check(t9.ProcessKey(session.get(), XK_Home, 0), "move before first lock");
  check(t9.ProcessKey(session.get(), XK_Delete, 0), "forward delete unlocks");
  check(
      t9.Snapshot(session.get()).input == "64426" && ctx->input() == "64~hao~",
      "forward unlock retains later lock");
  check(t9.ProcessKey(session.get(), XK_Delete, 0),
        "forward delete removes raw digit");
  check(t9.Snapshot(session.get()).input == "4426" && ctx->input() == "4~hao~",
        "later lock shifts by original input delta");
  fresh("64426");
  lock("ni", 2);
  lock("hao", 5);
  api->process_key(id, XK_BackSpace, kControlMask);
  check(t9.Snapshot(session.get()).input == "64" && ctx->input() == "~ni~",
        "Rime syllable deletion reconciles raw positions");
  api->set_option(id, "ascii_mode", true);
  check(!t9.Snapshot(session.get()).enabled, "ASCII mode hides disambiguation");
  api->set_option(id, "ascii_mode", false);
  api->process_key(id, XK_Escape, 0);
  check(!t9.Snapshot(session.get()).enabled, "escape clears disambiguation");
  no_commit();
  fresh("9464");
  lock("xing", 4);
  check(contains(u8"行"), "polyphonic xing reading");
  fresh("4264");
  lock("hang", 4);
  check(contains(u8"行"), "polyphonic hang reading");
  fresh("683");
  lock("nve", 3);
  check(contains(u8"虐"), "nue alias maps through prism to nve");
  fresh("nihao");
  check(contains(u8"你好"), "alphabetic input remains usable");
  ctx->Clear();
  auto schema = new Schema("luna_pinyin_t9");
  schema->config()->SetBool("trime/t9", false);
  session->ApplySchema(schema);
  t9.Reset();
  type("nihao");
  check(!t9.Snapshot(session.get()).enabled && contains(u8"你好"),
        "non-T9 scheme passes through");
  ctx->Clear();
  check(api->select_schema(id, "luna_pinyin"), "switch to full pinyin");
  type("nihao");
  check(!t9.Snapshot(session.get()).enabled && contains(u8"你好"),
        "full pinyin unaffected");
  ctx->Clear();
  check(api->select_schema(id, "double_pinyin"), "switch to double pinyin");
  type("nihk");
  check(!t9.Snapshot(session.get()).enabled && contains(u8"你好"),
        "double pinyin unaffected");
  ctx->Clear();
  api->destroy_session(id);
  session.reset();
  t9.Reset();
  learning_check();
  api->finalize();
  std::cout << "All " << checks << " checks passed" << std::endl;
}
