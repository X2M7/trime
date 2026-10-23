# 上游贡献拆分清单

## #2187 追加审阅：父类 setter 放在首句（2026-10-23）

已按 [新审阅意见](https://github.com/osfans/trime/pull/2187#discussion_r4354993843) 正常推送 `7f909060`，仍仅一个应用文件。`super.setText(charSequence, bufferType)` 现在是第一条执行语句，之后无条件标记两类自有缓存失效并请求布局／重绘，不再读取旧文本或比较内容。AOSP 21／35 与当前标签使用点核查未发现提前置脏的现有路径要求；构造期间不读未初始化绘图缓存，同内容 BufferType 转换仍交给父类。此前顺序未被描述成已发生 bug；重复赋值成本没有性能中性保证。

[限定总表](checks/candidate-setter-order-20261023/root-qualified-scope.json)记录本地 Kotlin2.3.21／JVM11 严格编译、字节码顺序通过，上游 CI **5/5 成功**，测试及 Lint 任务实际完成；没有导出 JUnit XML，不借旧301项当新源结果。fork Actions 未触发，显式 dispatch 返回422禁用错误，原件保留。上游实际构建是 `9ff9572f`／`10f5abd5`，与 fork `7f909060`／`2da49774` 整树相差33路径；待审控件源码 `c92ded47…` 完全一致。四ABI原包身份及DEX检查通过，未改动／重签的上游原x86_64 APK在API21通过 **13阶段／39原图**，Root实际看7张，独立核全字段／类型／像素和终态。该结果是实际merge的同生产类检查，不是fork或merge全树输入法验收。

输入法／屏幕／字体与DocumentsUI状态精确恢复、10项SDK不变，child／wrapper／controller／VM／collector实际全0；最低可用6,693,528KiB。W1553／E408完整保留，既有构建／依赖与签名警告未冒称消除。本轮不重跑API35；真机、TalkBack、原生候选点击、Release及性能仍未验收。[本轮说明和原件](checks/candidate-setter-order-20261023/README.md)与所有旧轮记录分开。原PR等待维护者复审，#2183与主develop未改。

[原 PR 正文更新回执](checks/candidate-setter-order-20261023/pr-body-update1/receipt.json)确认正文逐字读回，证据固定引用 `748af2a4`；源码、标题、base、Open／非 Draft 均未改变，未发送审阅回复或自动解决线程。

## #2187 上一轮92b审阅修改（2026-10-23，历史记录）

[#2187](https://github.com/osfans/trime/pull/2187) 已按 Collaborator 审阅推送 `92b13711`，仍仅一个应用文件。重复文本只减少自有缓存失效及显式刷新，原 nullable 值与 BufferType 始终交给父类；删除新增的 `super.onLayout`，保留带说明的 `super.onMeasure`。Android 5.0 对照中，删除父类测量导致前 11 项框架 Layout 为 null、后两项可选择／按行移动通过；将整个父类 setter 放进内容比较分支则在初次测量崩溃，未导出 BufferType 测试结果，不能照搬该写法。

当前源码的 **301 项应用 JVM 新执行通过，fork CI 4/4、上游 CI 5/5 成功**。未修改、未重签的原始 Ubuntu x86_64 Debug APK 类在 API21 通过 13 阶段／39 张原图；Root 实际看 7 张硬件原图，独立核验覆盖全部文件及文本／Layout／节点／类型／像素和公开 LINE 动作。API35 本轮 **未通过前置，0 阶段**：safe 诊断 Activity 的 8 秒焦点／可见性／正宽度组合前置未满足（原件未分别记录三项），已有先发 SystemUI ANR，恢复图显示系统 ANR 弹窗；不能证明唯一因果，原 APK 及其余对照完全未运行，也不借旧源通过。两轮设置与 DocumentsUI 状态精确恢复、SDK 10／25 项不变、VM 正常退出；API35 collector 实际 255 原样保留。

见[本轮说明与原件](checks/candidate-review-20261023/README.md)及[Root 限定总表](checks/candidate-review-20261023/root-qualified-scope.json)。原始依赖、构建、运行及 META-INF 警告和所有失败轮保留。控件级外部 Activity 检查不代表真实 TalkBack、候选点击、原生输入法、真机或 Release 验收；[原 PR 正文更新回执](checks/candidate-review-20261023/pr-body-update1/receipt.json)确认正文逐字读回、源码与 Open／非 Draft 状态未变，正文证据固定引用 `9eb3810a`。当前仍待维护者复审，未解决审阅线程、自动合并或发布。#2183、主 develop 及其余贡献分支未改。

## #2183 当前整改状态（2026-10-23）

[#2183](https://github.com/osfans/trime/pull/2183) 保持 **Draft，等待真机验收**。源码 `c4b613f7` 已推至原贡献分支，基线为上游 `19b04849`，五条 Conventional Commits、零 merge。[补丁范围](checks/pr2183-rebase-20261022/schema-layout-regression/current-c4-patch-shape-root1.json)为 62 个应用代码／测试及九键说明文件，没有 fork 发布、Pages 或此索引配置。方案切换恢复旧九键、数字框转邮箱时强制 ASCII 被布局附着覆盖的两个真实缺陷已修复；这两次后续修复未改 JNI、主题资产或子模块 pin，原失败保留。

[当前源码限定总表](checks/pr2183-rebase-20261022/current-c4-qualified-scope.json)绑定 fork／上游正式 CI 各 **5/5 成功**，以及[独立控制新执行](checks/pr2183-rebase-20261022/schema-layout-regression/c4-control-run-39806534353/artifact-execution-audit.json)的 **360 项应用 JVM、115 项独立 JNI 断言**。参见[云端资格](checks/pr2183-rebase-20261022/schema-layout-regression/c4-cloud-terminal1/qualification2.json)、[原始四 ABI APK 身份审计](checks/pr2183-rebase-20261022/schema-layout-regression/c4-fork-run-39806420824-ubuntu1/apk-audit/result.json)及[终态补充](checks/pr2183-rebase-20261022/schema-layout-regression/c4-cloud-terminal1/terminal-status-addendum1.json)。上游 synthetic merge 产物单列，未冒充 fork 原包；缓存 native 不改称全量新编译。

同一未改动、未重签的原始 x86_64 Debug APK 已完成 [Android 5.0 业务](checks/pr2183-rebase-20261022/runtime/api21-c4-independent1/api21-c4-business-aggregate-qualification1.json)及 [Android 15 业务](checks/pr2183-rebase-20261022/runtime/api35-c4-independent1/api35-c4-business-aggregate-qualification1.json)限定核验：**每系统 12 个成功会话、14 项逻辑检查、134 帧**，K1 各只在两主题 R4 内执行一次。覆盖方案／强制模式恢复、拼音锁定、回车、符号混输、编辑器隔离及真实触摸 Shift。默认／标准主题快速 Shift 得到 `aa`，标准显式双击正控得到 `AA`，应用内释放／分发与原 300 ms 限制均核对。原始 `passed=false/checks=[]` 采集日志未改写，业务结论由独立资格给出；Root 仅声明实际复核的 [34／26 张原图](checks/pr2183-rebase-20261022/runtime/root-image-publication-map.json)，不称全帧人工审阅。

[Android 5.0 终态](checks/pr2183-rebase-20261022/runtime/api21-c4-independent1/api21-c4-terminal-qualification1.json)及 [Android 15 终态](checks/pr2183-rebase-20261022/runtime/api35-c4-independent1/api35-c4-terminal-qualification1.json)已复核：两个模拟器／控制器正常退出 0，清理无错误，原 SDK 10／25 项文件内容不变、原输入法默认及 enabled 恢复。最低可用内存分别 6,205,852／4,320,488 KiB；原日志分别 W2095／E2435／F0 与 W11198／E2464／F1。Android 15 采集器断开实际退出 255，前置 phone／SystemUI ANR 及 `Disabling IRQ #35` 内核 F 级事件保留，未推断唯一因果或全系统零错误。Trime 目标 PID 的 W/E 也完整分组保留，没有免责或警告清零。

[修订后的 ARM64 真机清单](checks/pr2183-rebase-20261022/device-delivery-c4-reviewed/README.md)及 [Root 字节／契约核验](checks/pr2183-rebase-20261022/device-delivery-c4-reviewed-root-verification.json)已准备。原 CI Debug APK SHA-256 为 `12fd34dad063e76a0cdd132bbed83e802e02cd5f5ada80671f7f6935c09619c0`，证书为 `f6409a2d9db2800f1acc9b2e614d73e4609df3652d265cf2c40eb8a64456deb2`。44 个真机结果字段全部 **NOT_RUN**。Git 索引没有 APK：先从[原 Actions 附件](https://github.com/X2M7/trime/actions/runs/39806420824/artifacts/12438099455)取出对应 ARM64 文件放入清单目录，再运行 `sha256sum -c SHA256SUMS`。签名不匹配时保留用户数据并停止，不卸载绕过。

本轮不是旧十七项全部重跑、展开候选滚动恢复、全应用、快速焦点竞态、全部缓存强制模式、真机、TalkBack、Release、升级或正式性能验收。Debug 首次维护单列，不代表每次弹键盘都全量编译。旧源码失败、421 ms Shift 工具拒收、API21 v7 缺失 env 的触摸前远端 127 均保留，未冒充产品通过。依赖／工具／运行／META-INF 签名警告仍存在，未 Ready、合并或发布稳定版，主 develop 未改或推送。[PR 正文实际更新回执](checks/pr2183-rebase-20261022/original-pr-update5/receipt.json)确认原源码与 Draft 未变、正文逐字读回一致，正文证据固定引用 `9b8d7472`。当前机器状态见[清单](checks/readiness-20261022.json)；下文保留原时间和源码范围的历史记录。

## 先前审阅状态（2026-10-22 06:02 UTC，历史记录）

[#2177 剪贴板编辑器高度](https://github.com/osfans/trime/pull/2177)、[#2180 回车标签与动作](https://github.com/osfans/trime/pull/2180)、[#2176 首次设置](https://github.com/osfans/trime/pull/2176)、[#2179 反馈资源](https://github.com/osfans/trime/pull/2179)、[#2182 日志](https://github.com/osfans/trime/pull/2182)、[#2187 候选 TextView](https://github.com/osfans/trime/pull/2187)、[#2186 JNI Unicode](https://github.com/osfans/trime/pull/2186)、[#2188 冷启动 InputView](https://github.com/osfans/trime/pull/2188)及 [#2183 九键](https://github.com/osfans/trime/pull/2183)目前共九项 **Ready for review**，均未自动合并。JNI 的[第四批实际转换回执](checks/ready-transitions-20261022-batch4.json)确认 Open／非 Draft、`e16ab85f` 源码未变、四项上游构建成功及正文逐字读回，证据提交为 `4c2e2596`。日志与候选的[第三批回执](checks/ready-transitions-20261022-batch3.json)及首次设置与反馈的[第二批回执](checks/ready-transitions-20261022-batch2.json)保留。冷启动的[第五批实际转换回执](checks/ready-transitions-20261022-batch5.json)确认 Open／非 Draft、`2d2fbce7` 源码未变、四项上游检查成功及正文逐字读回。九键的[第六批实际转换回执](checks/ready-transitions-20261022-batch6.json)确认 Open／非 Draft、`9f86bc8f` 源码未变、四项上游检查成功及正文逐字读回，证据提交 `3c7da424`。Ready 表示可以开始审阅，不能替代维护者批准、完整应用验收或合并；[当前状态与证据绑定](checks/readiness-20261022.json)仍列出每项缺口。

#2176 的 `a219a075` 在两个系统的 480×540／dpi160、实际字体 1／2 下通过五步触摸、导航与有界滚动，Done、图标和正文可完整到达，footer 保持固定。#2179 的 `e7e63932` 完成两个系统的实际 Manager／Looper／SoundPool 限定检查；Android 15 没有 TTS 引擎，只验证了初始化失败后的释放、清空待播及不逐键重试，没有验证朗读开始／完成或物理声音。参见 [API21 设置](checks/setup-footer-api21-20261022.json)、[API21 反馈](checks/lazy-feedback-api21-20261022.json)、[API35 原件与范围](checks/ui-api35-20261022/qualification/result.json)及 [Root 独立复核](checks/ui-api35-root-readiness-20261022.json)。诊断副本只改变签名项并核对 payload，不代表 Release 签名、升级、完整首次使用、真实按键或迟到 native 回调验收。Android 5.0 的 Pico 退出 SIGSEGV 已由[不含 Trime 的标准客户端对照](checks/lazy-feedback-pico-control-20261022.json)复现，只排除这一系统引擎原因；Android 15 的真实 Calendar 低水位回收记录仍保留，不能声称全系统零 Fatal／LMK。

[#2183 九键](https://github.com/osfans/trime/pull/2183)已转为 Ready，当前源码为 `9f86bc8f`，普通合入独立冷启动修复 [#2188](https://github.com/osfans/trime/pull/2188)／`2d2fbce7`，并包含候选修复 #2187／`7a45cf29`；#2186 是共享 JNI Unicode 工作的独立同级 PR，其 head `e16ab85f` 不是本分支祖先。原七个 Ready 分支源码未改动。精确新源通过 330 项应用 JVM、115 项独立 JNI VM 断言及 fork／上游各四项 APK 构建；独立冷启动修复通过 301 项应用 JVM 与 fork／上游各四项构建。301／330 的重叠测试不相加。[四次运行共 16／16 项 OS 终态回执](checks/deferred-input-view-cloud-final-20261022/final-cloud-supplement-summary.json)和原日志保留。fork push 构建精确 head，上游 PR 构建记录中的 synthetic merge ref；启动 fork 每平台四 ABI 的 CMake 任务与 Ninja 进入均可见，显式对象 Building／Linking 记录为 0，不宣称全部 native 对象重编。[原始 XML、无损测试日志、APK／源码与八处启动片段核验](checks/deferred-input-view-cloud-20261022/provenance.json)保留。

新源 API35 同 PID `2235` 的两主题七组差量已经[独立限定核验](checks/deferred-input-view-api35-20261022/independent/qualification1.json)：分类宏实际提交“、”并在数字页返回后保留“、2”，两个主题的 ASCII／符号路径分别保留 `a!`／`a！`，待提交 `64` 切英文提交“你”，新聊天框不继承旧 `ni` 锁定。45 个采集帧、42 对提交文本 oracle 与 89 项动作逐项核对；Root 实际复核冷启动与业务共 28 张原图、46 份 oracle。[三段会话、退出、完整系统日志及旧源复用记录](checks/deferred-input-view-api35-20261022/provenance.json)保留。旧源十五项与新源差量分列，不表示新源十七项全部重跑。首次维护 450.390 秒不代表正式性能；G1 只证明延迟补建／最新数字框／实际 SHOW 后输入 `2`，不证明 API35 窗口持续可见时自动恢复。

API21 的[独立最终资格](checks/deferred-input-view-api21-20261022/independent/qualification1.json)确认原始新源 APK 在受控前置下完成全部十七项：分类宏与两主题 C1–C8，三个会话共 104 帧，严格文本、锁定、数字／符号来回、回车与新编辑器边界断言均保留；原 `passed=false/checks=[]` 获取日志没有改写。Root 实际复核 36 张原始 PNG、66 份 oracle；三个 Observer 的认证 stop／wait 与实际子进程均退出 0，同 PID `2562`／start `20670` 贯穿业务。[无损会话归档、完整系统日志与文件清单](checks/deferred-input-view-api21-20261022/provenance.json)已发布。此前旧隔离测试档案已备份，因为 Debug 证书不同而重装测试包并准备三项存储／主题偏好；没有恢复编译缓存或替换 native 库，不代表默认首次使用、真实用户升级或公共设置验收。分类符号、标准主题全角 `a！`／默认主题半角 `a!` 和真实换行的差异均按原断言保留。

API21 的[自然延迟启动限定复核](checks/deferred-input-view-api21-startup-20261022/startup-lifecycle-qualification1.json)确认首次 scope-null 后，同 PID 在无额外切换输入法或 SHOW 的情况下 7.219 秒后附着键盘，实际维护成功后通过候选点击提交“你好”。这使用包含修复的组合 APK `9f86bc8f`；八处启动片段一致，不是 Service 全文件／完整方法一致或独立 `2d2fbce7` APK 的设备验收。首次维护 329.364 秒不代表正式性能，`show accepted:false` 与前置原文保留；隐藏后再显示／物理模式切换不在本轮启动断言内。

两个模拟器与控制器均实际退出 0、清理无错误，API35 采集器因设备断开退出 255，API21 采集器退出 0；API35／21 分别 25／10 项原 SDK 文件保持不变。两轮 981／1,270 个内存样本最低可用分别 2,268,976／3,243,756 KiB。API35 保留启动阶段 SystemUI／Launcher ANR、内核 IRQ #35 F 级事件及 W／E；API21 完整日志有 2,010 条 W、2,312 条 E，目标 PID 有 255 条 W、83 条 E，记录中的 fatal／ANR／IRQ／LMK 匹配为 0。未删除警告或把范围通过写成全系统零问题；不表示真机、Release、TalkBack、通用候选竞态或性能验收完成。

旧 `eeb4804a` 的 API35 标准主题 C1–C8 与分类宏、默认主题 C1–C6 已限定核对；尾轮在 C7／C8 前复现真实白屏：框架早于主题请求 InputView，之后缺少补建，0 项尾部业务检查不改判。Root 同 PID Latin→Trime 对照恢复可见键盘只用于查因。[原始负控、暖服务对照、修复与退出记录](checks/deferred-input-view-negative-20261022/provenance.json)保留。旧源的 15 项范围与原失败都单列，不冒充 `9f86bc8f` 每项重新执行。该失败轮模拟器与控制器均正常退出 0、日志采集因设备断开退出 255，25 个原 SDK 文件未变；更早的系统 ANR、控制器 143 和监测空档仍保留。

#2182 的 `e8e4db83` 已依据 Android 5.0／15 限定业务结果转为 Ready，349 项应用 JVM 已新执行通过。显示和待刷新队列分别限定 10,000 行／2 Mi UTF-16 单位，按帧合并更新并仅为可见行着色，UI／导出说明被省略的旧日志。[API21 限定业务结果](checks/logcat-api21-buffer-20261022/business-scopes-result.json)及 [API35 最终范围](checks/logcat-api35-buffer-20261022/final/root-final-qualification2.json)包含同 PID 的实际首次维护成功、受限日志窗口、原默认文件名一次 SAF 保存、真实同 UID 外 PID 排除和三轮一个／零个 reader。API35 的 89 个同 PID／启动时间样本中，Dalvik Heap Alloc 为 5,955–9,303 KiB、总 PSS 样本最高 324,049 KiB；不是瞬时峰值、全部堆上限或正式性能结果。实际导出的 10,000 条记录均精确匹配原日志，负 PID `10813` 排除，正例在标记前／后分别为 9,916／84 条；参见[内存复核](checks/logcat-api35-buffer-20261022/final/root-independent-three-memory-segments1.json)和[导出／生命周期复核](checks/logcat-api35-buffer-20261022/final/root-independent-export-lifecycle1.json)。

日志原失败均保留：API35 Observer1／2 的 90 秒空闲失败、steady host wrapper 和第三段内存采样的真实退出 143／缺少终态回执，以及首次 Back 后即时 reader 为 1 的失败均未改成通过。同一次 Back 后实际观察到 Developer 再读为 0；后两轮只在真实离页原帧后使用固定 5 秒观察预算，不声称 Back 起 5 秒内完成。03 帧仍是 LogView 过渡，04 才是实际 SAF，不能凭目录名认定成功。API35 冷范围的真实内核 `Disabling IRQ #35`／一条 F 级事件、既有应用／主题／词库警告与 API21 的旧工具、内存压力及未证实原因的旧流错误都保留；不能称全工具通过或系统 critical 为零。Debug 首次维护 1,472.734 秒不代表 Release 启动性能；本修复约束已接收的日志后缀，不保证持续高流量时的实时追尾，也未量化丢行或源到屏幕延迟。

#2187 的 `7a45cf29` 已依据两系统的生产控件检查转为 Ready。[API21 五阶段对照](checks/candidate-layout-api21-20261022/qualification.json)显示旧控件创建无障碍节点后丢失文本，修复原包保留文本、Layout、节点与字形；[API35 五阶段正向检查](checks/candidate-layout-api35-20261022/qualification1.json)及 [Root 原图复核](checks/candidate-layout-api35-20261022/root-qualification1.json)覆盖构造、汉字、拉丁字母、null 和重新绑定。API35 没有重跑旧版负控；该负控仅在 API21 完成。此探针执行原 APK 的生产 TextView 类，没有启动 native／Trime Application，不等于真实候选选择、TalkBack 操作、长文本、多窗口或完整 IME 验收。首次导出工具的列式 `ls` 失败保留，使用同一探针原件完成后续导出，未重跑产品来替换失败。

独立 #2186 的 `e16ab85f` 已依据两系统限定 JNI 业务结果转为 Ready。301 项应用测试、115 项 Unicode 和 46 项 UTF-16 位置 VM 检查已通过；API21 原包在 CheckJNI 下的实际 `a`／补充字符候选路径存活，`ni`／Space 提交“你”，原候选控件空白缺口仍保留。API35 同 PID `12444` 首次维护在 319.277 秒后成功，实际 CheckJNI 启动日志保留；stock `luna_pinyin`、`ascii=false` 下输入 `a`，原生日志记录词条 `𠼞` 及 `preparing 14 candidates`，这不是 Java 候选数组的读取或快照。实际删除后 `ni`／Space 提交“你”，普通 `a`／`ni` 候选行可见，但未验证罕词的可视点击或提交。[API35 限定结果](checks/jni-unicode-api35-20261022/qualification/result.json)及 [Root 资格复核](checks/jni-unicode-api35-20261022/root-qualification1.json)保留 75 项原始 payload 校验和四张实际核对原图；Observer 正常认证 stop7／host 0，`debug.checkjni` 空值与 Latin 已恢复。193 条词库编码警告、工具前置／打包失败均保留，不表示完整 IME、TalkBack、真机或 Release 验收。已由上游合入的 [#2181](https://github.com/osfans/trime/pull/2181)与 [#2178](https://github.com/osfans/trime/pull/2178)保持原合入记录。

原 Debug APK 每包两条 `META-INF/services` 签名提示、SDK／Gradle／native-access 诊断，以及固定词库和依赖警告均保留，不删除服务注册或日志来隐藏警告。没有真机验收、新稳定版发布或自动合并。下文是此前各轮的日期／SHA 历史记录，不替代本节当前源码的验收状态。

日期：2026-10-21。本分支仅用于 X2M7 fork 的拆分索引，不作为上游 PR 的内容。

上游基准是 `osfans/trime:develop` 的 `f5008260dc1e71b05e8e5508c6cfbf8ebf2507a0`；保留的原始贡献分支是 `contrib/complete-input-improvements`，快照为 `df12d0f4f62d7952a181e0ff7cf3cfd3e0077b7c`。两者之间有 67 个提交、660 个改动路径、90,665 行新增、3,388 行删除。最初一个提交就修改了 577 个文件，不能靠逐个 cherry-pick 达到“一件事一个 PR”。

维护者要求[拆成单一主题的小 PR 并清理补丁](https://github.com/osfans/trime/pull/2170#issuecomment-6279805216)。按用户选择先推送 fork 分支与清单，核验 32/32 个原分支 OS 构建后，先创建的两项小 PR 已由维护者合入；[合入与上游八项 CI 回执](checks/upstream-merges-20261020.json)保留精确 SHA。原 [#2170](https://github.com/osfans/trime/pull/2170) 已于 2026-10-20 被维护者关闭，保持关闭状态。原贡献分支、八个已验分支与 fork 的 `develop` 不改写。原八个分支基于 `f5008260`，下面新增布局提交则分别直接基于已合入两项修复的 `144f6111`。

## 后续提交粒度调整（2026-10-21）

之前优先选择能独立复现、独立合入的小修复；这让队列连续出现单个布局属性、单行 API 弃用等 PR，用户指出粒度过细。后续改为按**一个完整行为及其验收**提交：实现该行为所需的调用方、辅助类、兼容分支和回归测试一起进入同一 PR，不按文件数、函数或行数拆开。

维护者的“一件事一个 PR”仍然适用；同一目录中的无关问题不能仅为了增大体积而捆绑。已打开的 PR 保留现有 SHA、构建与审阅记录；纯清理不再优先于完整功能。下方 A–D 是实现点和依赖清单，不再表示每一行都应创建一个 PR。

| 后续模块 | 一次提交应覆盖的闭环 | 需要区分的独立目标 |
| --- | --- | --- |
| 日志采集可靠性 | 子进程所有权、取消与三流清理、错误传递、停止/重启、旧 Android 命令/PID 过滤和日志展示适配 | 同一条采集链路一次验证；不并入内容隐私策略或日志页面改版 |
| 输入引擎生命周期 | 启动/失败状态、等待队列、资源回收与失败重试的必要调用方 | 编辑器会话隔离如可独立验收，再作为下一完整目标 |
| 输入隐私 | 编辑器/手动策略、实际学习与上下文、剪贴板/TTS/日志出口，以及退出后的恢复 | 不能把每个内容出口单独发成已完成的隐私模式 |
| 九键基础输入（当前核心） | 最小方案和布局、候选/拼音筛选、编辑与必要的中英/数字切换 | 下一词联想属于后续独立功能，不提前捆绑模型 |
| 候选交互与个人词库 | 每个用户目标所需的候选身份、UI 与持久化一起提交 | 安全选词/展开定位，与置顶/词库迁移可分别验收；不按 helper 分拆 |
| 首次设置与配置管理 | 布局/方案选择、试打和返回形成可完成的流程；导入校验与失败恢复完整提交 | 引导流程与事务式导入是两个独立用户目标 |
| 无障碍与设备适配 | 按键/候选语义、探索激活、动态动作和必要的视口适配 | TalkBack 操作闭环与一般窗口适配按实测问题划分 |

[六项已开 PR 的当前复核](checks/current-pr-build-feedback-next7.json)：无新增维护者反馈；45 个构建 job 成功、3 个仍运行、没有失败。此为冻结快照，不代表所有平台或手测均通过。

## 第一批独立分支

以下分支均直接建立在上述上游基准，各只有一个修复提交，相互没有依赖。不是把原有大分支换个名字，也没有带入 fork 发布、版本、签名、CI 或工具链策略。

| 分支与差异 | 单一问题 | 范围 | 已做检查 | 尚待完成 |
| --- | --- | --- | --- | --- |
| [split-setup-footer](https://github.com/X2M7/trime/compare/f5008260...contrib/split-setup-footer) | 向导内容占到导航按钮区域 | `activity_setup.xml`：ViewPager 约束到可见底部按钮顶部的 Barrier | XML 解析、按钮显示状态源码复核、AAPT2 资源编译、diff 检查 | Release/Lint；小屏、大字体及三页实际布局 |
| [split-clipboard-editor-height](https://github.com/X2M7/trime/compare/f5008260...contrib/split-clipboard-editor-height) | 长剪贴板内容的 wrap_content 高度不遵守已有下边界 | `activity_clip_edit.xml`：一个 constrainedHeight 属性 | XML 解析、单行差异复核、AAPT2 资源编译、diff 检查 | Release/Lint；长文本、小屏及软键盘实际布局 |
| [split-native-license-versions](https://github.com/X2M7/trime/compare/f5008260...contrib/split-native-license-versions) | 许可证信息中的 librime/OpenCC 版本落后于已固定源码 | 两个 JSON 的版本字段 | JSON 解析、其他字段不变、递归源码 pin 与实际版本核对、diff 检查 | 许可证页面集成检查；不涉及依赖升级 |
| [split-jni-local-refs](https://github.com/X2M7/trime/compare/f5008260...contrib/split-jni-local-refs) | 字符串数组转换积累 JNI 局部引用 | `objconv.h`：复用现有 JRef，每轮释放数组元素引用 | 真实生产头文件的聚焦宿主回归及修复前对照，详见验证记录 | Release/Lint、Android CheckJNI/设备验证 |

精确提交、改动文件与验证范围见 [branches.json](branches.json)。两个 XML 的 AAPT2 编译均退出 0，无输出诊断；这仅验证资源编译，不等于 APK 链接、布局测量或真机验收。宿主 mock 也不替代 Android 运行时。

JNI 聚焦测试在同一 mock 调用环境转换 2,048 个元素四次：原实现留下 8,192 个局部引用，修复后剩余 0、峰值 1，同时验证字符串顺序及先释放 UTF 字符再删除引用。真实 JNI 在 native 方法返回时会回收局部引用，本修复针对返回前的引用积累，不能据此声称发生跨调用的永久泄漏。宿主严格 `-Werror` 编译在两版都遇到相同四条已有 `pessimizing-move` / `sign-compare` 诊断；仅将这两类降为警告后运行对照，未顺带修改上游代码。因此这不是“完整构建零警告”的证明。[聚焦测试源码](checks/jni_local_refs.cc) 与复现命令见 [检查说明](checks/README.md)，保留在本 fork 索引，不把整套验收工具塞进两行修复。

首批四个分支的 GitHub CI 已通过：共 16/16 个 OS job 成功，每个分支在 Ubuntu、两种 macOS 和 Windows 实际执行 Debug 编译/打包及四 ABI 原生构建任务，JNI cache 均未命中；四个 Ubuntu Spotless 实际通过。[CI 审计摘要](checks/first-batch-ci.json) 绑定了精确提交与 run 链接。CI 未执行测试或 Lint，也没有下载审计 APK，因此不能写完整应用测试、Release 或设备验收通过。

原有共同诊断仍保留，包括 SHOW_IMPLICIT/srcDirs 弃用、原生依赖 CMake/constexpr/32 位窄化、SDK/JNA/Gradle，以及 Windows 缓存归档失败。四个分支按 OS 比较的诊断候选多重集合完全相同，相关源码/依赖 pin 与上游基准一致；这不是零警告。原大分支的历史验收不能替代这些新分支的验证。[贡献指南](../../CONTRIBUTING.md) 和 [PR 模板](../../.github/pull-request-template.md) 的相关验收项不能全部勾选；本次 Android Lint、Release 和手测仍未完成。本次不发布新的稳定版。

## 第二批：反馈资源与日志进程

同样直接基于 `f5008260`，两个分支互不依赖，均只有一个主题提交：

| 分支 | 实现范围 | 已执行验证 | 限制与待验收 |
| --- | --- | --- | --- |
| [split-lazy-feedback](https://github.com/X2M7/trime/compare/f5008260...contrib/split-lazy-feedback) | 保留 `init(context)`；实际启用时才创建 TTS/自定义 SoundPool；就绪后使用；销毁后的旧回调不能激活新资源；必要的 TTS package query | 实际生产 FeedbackResource 辅助类的 8 项 Kotest；主线程/回调身份与 Manager 调用路径独立源码审阅；Manifest XML 解析；Spotless/ktlint | helper 测试不能替代 Android Manager 集成和真实播放；初始化失败须 destroy/init 后再试；音效未加载前的按键不补播；未测内存收益 |
| [split-logcat-lifecycle](https://github.com/X2M7/trime/compare/f5008260...contrib/split-logcat-lifecycle) | Process/Logcat 终止与三流清理、取消阻塞读、错误保留、停止/重启及旧 Job 隔离 | 两个生产文件的 17 项 Kotest，包括真实 JVM 子进程取消、读取失败同时发生两次 destroy 失败、正常 EOF 的 destroy 失败；Spotless/ktlint | Android/Windows/macOS 实际运行待验证；原 logcat 命令选项不改，API21 的 PID/clear 兼容另拆；同步 destroy/close 不受退出轮询的 1 秒窗口限制 |

第二批合计 25 项测试均实际执行，失败/错误/跳过均为 0。使用上游固定的 Kotlin 2.3.21、Kotest 6.0.7、coroutines 1.10.2，JVM 目标 11，编译启用 `allWarningsAsErrors`，没有 Kotlin 编译警告。宿主工程由 Gradle 9.8.1 在 JDK17 上执行；保留 Gradle 弃用汇总；额外以 warning-mode all 强制执行格式检查时，观察到 Spotless 任务使用的 Configuration.setVisible(boolean) 将在 Gradle11 移除的诊断，原件保留。未因此混入工具链升级，也不宣称整个工具链无警告。其源码绑定、测试 XML 哈希和详细范围见 [第二批宿主验证记录](checks/second-batch-host.json)。该验证工程没有带入功能分支，也不是完整 Android 应用测试：日志宿主编译仅替换资源编号及本地化错误构造，反馈仅编译/执行纯资源所有者，Manager 另做源码/格式检查。

独立审阅发现并在推送前修复了两项边界：SoundPool 创建失败后每键重复文件访问；取消 watcher 销毁失败抢先覆盖原始读取异常。后者将 watcher 错误保存到单次调用的槽，等待结构化子任务完成后与最终清理错误合并，原读取/取消异常保留为主异常。

第二批的完整 Android CI、Lint、Release 与设备验收仍以各分支后续的精确 SHA 证据为准；没有用首批或原汇总分支的通过结果代替。

第二批 Commit CI 的日志分支已完成四系统 Debug/native 编译与 Ubuntu Spotless。首次 Ubuntu job 在执行任何 Gradle task 前无法解析两个 Kotlin 2.4.10 依赖，原件见[失败范围记录](checks/second-batch-ci-pending.json)；保持源码 `f5909dcb` 不变后的重试实际通过。重试界面的另外三个成功 job 沿用原运行，不算重新编译。反馈分支最后的 Intel macOS 已通过并核对原始日志；[最终日志记录](checks/second-batch-ci.json)闭合第二批 8/8 个独立成功执行。全部已审成功 job 的诊断与首批同 OS 一致，既有警告保留。上述 Commit CI 均未运行测试或 Lint；完整应用 JVM 结果见下方单独控制验证。

## 第三批：回车动作与 JNI 线程归属

本批两个分支均已推送 fork，直接基于 `f5008260`、各只有一个提交，无相互依赖。JNI 线程分支四系统 Debug/native 构建与 Ubuntu Spotless 已通过；回车分支最后的 Intel macOS 也已通过并核对原始日志，第三批现为 8/8 个独立成功执行。[实际日志审计](checks/third-batch-ci.json) 保留精确 SHA 与验证范围。Enter Windows 的既有缓存锁归档警告读取长度由 38 变为 37 字节，原文差异保留；仅对这一动态长度做规范化后与同 OS 基准比较，未发现新增源码诊断，不能声称原始警告逐字全同。

| 分支 | 单一问题与范围 | 已执行验证 | 限制与待验收 |
| --- | --- | --- | --- |
| [split-enter-actions](https://github.com/X2M7/trime/compare/f5008260...contrib/split-enter-actions) | 让回车显示与实际派发使用同一 `EditorEnterAction` 规则；只改 Enter delegate、service 的回车逻辑及纯辅助类/测试 | 实际 12 项 Kotest 全通过，0 失败/错误/跳过；四个最终源码 SHA 与记录核对；helper/test 编译，delegate/service 仅格式与源码复核 | 完整应用编译/单元测试已见下方控制验证；真实 EditorInfo/主题标签及 `performEditorAction` 行为待验收；Gradle 弃用诊断保留 |
| [split-jni-thread-attachment](https://github.com/X2M7/trime/compare/f5008260...contrib/split-jni-thread-attachment) | `JEnv` 只 detach 自己成功 attach 的线程，禁止所有权复制/移动；仅一个生产头文件 | 真实生产头文件/工厂配合 JNI 函数表 mock：7 个案例通过，原版 4 个案例失败；独占所有权静态断言前失败后通过；两版普通 C++17 严格编译无诊断 | 无 ART/JVM/Rime 回调执行；失败 attach 的 null 环境、detach 返回值与外部线程操作边界保留；四系统 Android Debug 构建已通过；CheckJNI/设备待验收 |

验证原件绑定见[第三批回车宿主记录](checks/third-batch-enter-host.json)与[第三批 JNI 记录](checks/third-batch-jni-thread.json)，最小复现源码和新复现命令见[检查说明](checks/README.md)。本批 12 项辅助类测试与此前 25 项宿主测试分开记录，不将格式检查等同于 service/delegate 集成测试。新 JNI shell 复现器也已实际执行通过：修复版 7 个案例通过，原版 4 个失败；严格编译无诊断，见[复现结果](checks/jni-thread-reproduction.json)。它与原执行属于同一组 7 个案例的两次运行，不累加成新的测试案例。

[补丁机械检查](checks/patch-composition.json) 已逐一在临时 index 上验证 8 份补丁相对基准可应用，并按记录顺序成功组合，得到 tree `2f8317b32873ed1bcb5694462494755bb5f9dc2c`；真实工作树与分支未改动。这个组合 tree 未编译或运行，只证明文本补丁没有应用冲突，不是集成验收通过。

## 独立的完整应用测试控制分支

[contrib/split-validation](https://github.com/X2M7/trime/tree/contrib/split-validation) 是 fork 专用控制分支，不计入 8 个源码模块，也不带进功能分支或上游 PR。第一轮工作流提交 `cc126ed9`，第二轮仅更换固定源码矩阵、正常前进到 `c519e3e9`；原运行与提交均保留。两轮真实执行 `:app:testDebugUnitTest --rerun-tasks`，原 XML/HTML、完整日志、20 个递归子模块身份和官方附件校验均已复核。

| 精确源码范围 | 实际应用 JVM 测试 | 运行 |
| --- | --- | --- |
| 上游基准 `f5008260` | 301 项 | [第一轮](https://github.com/X2M7/trime/actions/runs/39401035244) |
| 反馈 `3912fb23` | 309 项，新增 8 项 | 第一轮 |
| 日志 `f5909dcb` | 318 项，新增 17 项 | 第一轮 |
| 首批四个分支，以及 JNI 线程 `3e990b2d` | 每分支各 301 项 | [第二轮](https://github.com/X2M7/trime/actions/runs/39402432371) |
| 回车 `bcb47b1a` | 313 项，新增 12 项 | 第二轮 |

所有 scope 均 0 失败、错误和跳过；任务实际执行，没有把缓存结果记作重跑。按 `(classname, name)` 多重集比较，每支保留全部 301 项基准用例；Kotest 不同上下文中的同名短名称保留，不按唯一名称错误去重。重复执行的基准用例不累加为新测试案例。[第一轮摘要](checks/application-jvm-first.json)与[第二轮摘要](checks/application-jvm-second.json)绑定精确源码及原始记录哈希。

每个 scope 都保留同一条上游已有的 `ClipEditActivity.SHOW_IMPLICIT` Kotlin 弃用警告，Gradle/SDK/原生依赖等诊断也保留；不是零警告。完整应用编译意味着 Manager、service/delegate 已在这些 Android 工程中编译，但 JVM 测试仍不能证明真实 TTS、编辑器动作、布局、CheckJNI、Lint、Release 或设备行为通过。本次不发布 APK。

## 首批两项已合入上游

八个原拆分分支的 **32/32 个 OS 构建**均完成并核对，应用 JVM 测试也均通过。[最终构建清单](checks/all-split-builds.json)绑定精确 SHA。两项提交分支随后在 fork 和上游分别完成 8/8 个成功 OS job；[最新上游合入回执](checks/upstream-merges-20261020.json)记录每个 job 的实际结果。现有上游编译、依赖及 Windows 缓存归档警告保留，不宣称零警告。`SHOW_IMPLICIT` 已由上游 [#2173](https://github.com/osfans/trime/pull/2173) 单独修复。

| 上游 PR | 提交分支与 SHA | 范围 | 当前状态 |
| --- | --- | --- | --- |
| [#2174](https://github.com/osfans/trime/pull/2174) | `contrib/pr-native-license-versions`，`77774753` | 两个许可证版本字段 | 维护者于 2026-10-20 合入 `7609c8ba` |
| [#2175](https://github.com/osfans/trime/pull/2175) | `contrib/pr-jni-local-refs`，`a83583b8` | JNI 元素引用所有权两行 | 维护者于 2026-10-20 合入 `144f6111` |

这两个提交为原补丁补齐了符合贡献规范的英文正文；Git tree 和相对 `f5008260` 的 patch 均与原已验 `f34ee4fc`／`9e296c1e` 相同。原分支与原测试证据保留。[原创建回执与等价证明](checks/upstream-submissions.json)是创建时快照，当前状态以合入回执为准。它们最初以 Draft 提交，未勾选未执行的手测；随后由维护者批准、转 Ready 并合入。合入不补充 Android CheckJNI、许可证页面 UI、Lint 或 Release 的验收结论。

许可证原 CI x86_64 Debug APK 已实际下载并检查 `res/raw/aboutlibraries.json`：`rime/librime=1.17.0`、`BYVoid/OpenCC=1.4.2` 与源码相同，官方附件 ZIP 大小与 SHA-256 一致。[打包数据核验](checks/license-apk-package.json)不等于安装、运行或许可证页面 UI 通过。

## 下一批：两个独立布局分支

两个新提交分别直接建立在最新上游 `144f6111`，各只有一个提交、一个 XML 文件，相互没有依赖。与原分支的补丁逐字节相同；目标 XML 在新旧基准上也相同。旧分支没有重写，新分支需要重新绑定验证结果。

| fork 分支 | 新 SHA | 单一范围 | 新提交 CI |
| --- | --- | --- | --- |
| [pr-setup-footer](https://github.com/X2M7/trime/tree/contrib/pr-setup-footer)／[#2176 Draft](https://github.com/osfans/trime/pull/2176) | `560b5b99` | pager 的底部约束到可见导航按钮的 TOP Barrier | [四系统构建](https://github.com/X2M7/trime/actions/runs/39417654338) |
| [pr-clipboard-editor-height](https://github.com/X2M7/trime/tree/contrib/pr-clipboard-editor-height)／[#2177 Draft](https://github.com/osfans/trime/pull/2177) | `8344231d` | wrap_content 编辑框遵守已有的高度约束 | [四系统构建](https://github.com/X2M7/trime/actions/runs/39417654398) |

独立的 fork 控制提交 `ededa40b` 为上游基准与这两个精确 SHA 执行完整应用 JVM 测试，见[第三轮控制运行](https://github.com/X2M7/trime/actions/runs/39417689930)。第一轮基准与剪贴板各 301 项实际通过、0 失败/错误/跳过，与历史 301 用例多重集一致；工具警告保留。向导在 AAPT2 下载超时后未执行测试，该次整轮失败原件保留于[首轮检查](checks/layout-jvm-attempt1.json)；仅对该失败任务请求同源码重试，重试后的向导 301 项真实通过；其余两项沿用首轮成功结果，未记作重新执行。[完整第三轮审计](checks/layout-jvm-final.json)核对了三范围的源码、20 个递归 pin、原 XML/HTML、官方附件哈希与同一组 301 用例。应用的 `SHOW_IMPLICIT` 诊断已被上游移除，build-logic 的 `srcDirs` Kotlin 弃用及其他工具警告仍存在，不能把空的应用 `w:` 前缀列表解读为全工程零警告。

[剪贴板聚焦 Android 测量](checks/clipboard-height-probe/README.md)实际比较 12 对布局：原版 7 项几何失败，修复后 12 项通过，文本均未改变；宽裕高度的四组控制前后完全相同。这只是精确 XML 的独立 inflate/measure，依赖范围与完整应用不同，不含真实 Activity、IME、滚动、触摸或保存/取消。`constrainedHeight` 不改变既有 `adjustPan`。[向导原分支实际 APK 对照](checks/setup-layout-diagnostic/README.md)已观察到 pager 底部从逻辑 y=492 移到 footer 顶部 y=444，并实际导航到 Next 隐藏的 Select 页；它不是新提交整包验收，也未证实按钮点击矩形相交。原页面仍不可滚动，Done 节点可能超出短 pager；截图未证实字形裁切，大字体实际变更亦未验收，均作为限制保留。四平台完整构建仍以各运行的最终结果为准；待验收项不勾选，也不发布 APK。

[后续只读检查](checks/layout-pr-current-check.json)确认两项 fork 的 8/8 个 OS job 均成功，未发现新的失败步骤或维护者问题反馈；上游 PR 构建仍有任务运行，不能据此宣称没有运行时问题。[PR 构建状态说明已更新](checks/layout-pr-build-status-refresh.json)，手测限制及 Draft 状态保持。

两项均已实际创建为上游 Draft，每个 PR 一个提交、一个 XML；[创建及读回回执](checks/layout-upstream-submissions.json)确认目标、源 SHA、补丁范围和正文。未标 Ready。隔离模拟器已按请求正常退出 0，无设备残留。

[8 份英文说明](drafts/README.md)与原八个分支继续保留。其余模块按各自问题和验收逐项推进，不同时提交全部补丁。

[本轮最终反馈检查](checks/layout-feedback-latest.json)仍未发现新增维护者问题；[上游 CI 最新快照](checks/layout-upstream-ci-latest.json)中 #2177 已四系统通过，#2176 仅 Intel macOS 仍在运行，无失败步骤。Draft 与运行时验收限制保持，不据此改为 Ready。

## 下一项：JNI 线程连接归属

已推送 [contrib/pr-jni-thread-attachment](https://github.com/X2M7/trime/tree/contrib/pr-jni-thread-attachment)，并创建上游 [#2178 Draft](https://github.com/osfans/trime/pull/2178)。新提交 `2283878e` 直接基于 `144f6111`，只有 `jni-utils.h` 一个文件、一个提交，新增 13 行／删除 2 行。[提交及读回回执](checks/jni-attachment-upstream-submission.json)核对了实际目标、源 SHA、单文件范围和正文。它与原 `3e990b2d` 补丁逐字节一致，原分支保留；[源码绑定](checks/jni-attachment-submission-source.json)记录精确父提交与补丁哈希。

新提交的 [301 项完整应用 JVM 用例](checks/jni-attachment-application-jvm.json)已实际执行通过、0 失败/错误/跳过，与已验 `144f6111` 基准的用例多重集完全一致；基准本轮没有重复执行。20 个递归 pin、官方测试附件及原 XML/HTML 均已核对。既有 build-logic Kotlin 弃用与其他工具警告保留。[四系统 Debug/native CI](https://github.com/X2M7/trime/actions/runs/39423511285)仍在运行。

[真实宿主 JVM 对照与复现工具](checks/jni-thread-host-vm/README.md)直接编译生产头文件，在 HotSpot 17 中创建一个 native 线程：基线作用域结束仍 attached，修复后返回 detached；借用的 Java 主线程及嵌套 guard 仍 attached，真实 JNI 字符串调用可用，两个 JVM 均正常销毁。相同案例已启用 `-Xcheck:jni` 再执行，编译及 JVM 无诊断；重复执行不增加案例数量。原 7 项失败注入 mock 也重新通过，基线 4 项失败。

当前唯一调用点是返回 void 的 Rime 通知 lambda：guard 先于局部引用声明，Java 调用返回且局部引用析构后才 detach，未发现局部引用逃逸或错误线程 detach。宿主 HotSpot 不替代 Android ART、真实 Rime 回调、Lint 或 Release 验收；保留既有错误处理边界，只作为单主题 Draft 推进。

备用回车模块的动作决策未发现新增问题，但同编辑器 `restartInput` 后的键面标签重绘仍需实际复现；本轮不提交回车模块，也不把纯 helper 单测当作动态 UI 验收。

## 当前复核与下一项反馈资源

[三项现有 PR 的只读快照](checks/current-pr-build-feedback-20261020.json)确认 #2176／#2177／#2178 仍为 Open Draft，没有新增 review、评论或内联问题；对应 fork 与上游共 24/24 个 OS 构建 job 均成功。这是构建和反馈状态复核，不是新增手测，也不是零警告证明；上面原有运行中快照保留其历史范围。

已推送 [contrib/pr-feedback-resources](https://github.com/X2M7/trime/tree/contrib/pr-feedback-resources)，并创建上游 [#2179 Draft](https://github.com/osfans/trime/pull/2179)。提交为 `e7e63932`，直接基于 `144f6111`，一个提交、六个文件，仅涉及按需反馈资源、线程与生命周期以及相应测试。原 `3912fb23` 分支继续保留；其资源 helper、原八项测试与 Manifest query 全字节未变。[源码与审阅绑定](checks/lazy-feedback-submission-source.json)记录了精确 SHA 和单主题范围。

提交前复核发现，`onText` 的非 ASCII／action 提交可能从 Rime 的后台作用域进入朗读，而原 lazy owner 假定主线程访问。受控宿主模型复现了创建尚未返回时 UI 请求清空 pending 的交错；它不是 Android TTS 设备复现。新实现让两个朗读入口在读取偏好和转换内容之前分发到主线程，并在 init／start／finish／destroy 时淘汰此前排队的反馈请求。它只隔离新反馈队列，没有顺带改造 service 的历史提交任务或编辑器会话。

[本轮聚焦检查](checks/lazy-feedback-focused-host.json)实际执行了原 8 项资源测试与新增 6 项真实 worker／受控队列测试，14/14 通过、0 失败／错误／跳过。纯 helper／测试的 Kotlin 严格编译无警告，Manager 在该宿主工程只做格式检查；独立源码复核未发现阻止本项 Draft 的问题。Spotless 的 `Configuration.setVisible(boolean)` 工具弃用提示保留，不能称完整工具链无警告。

初提交 `b8988c57` 的[315 项完整应用测试实际通过](checks/lazy-feedback-application-jvm-initial.json)，但云端为新增测试的 `poll()` 可空调用报告一条编译警告，因此没有提交 PR。已改用 `checkNotNull` 保留必须执行回调的断言，最终一提交 `e7e63932` 的其余五个文件均与初提交一致；旧源码分支和所有失败验收原件保留，既不抑制诊断也不把首轮测试通过记成警告已消除。修正后的宿主相同 14 项再次执行通过，不累加为 28 个独立用例。宿主 JDK17 与云端 JDK25 等构建上下文不同，不能以轻量结果代替最终应用构建。

最终精确提交的[315 项完整应用 JVM 验证](checks/lazy-feedback-application-jvm-final.json)实际通过，21 组 XML 的失败／错误／跳过均为 0；301 项基准完整保留，新增 14 项与最终源码标签一致。官方附件的大小／SHA／CRC、源码与控制提交身份、20 个递归 pin 及实际非缓存任务均已独立核对。新增空值调用诊断已消失，结构化 build-logic `srcDirs` 与既有工具警告仍与基准相同，不是整个工程零警告。反馈资源的 fork 与上游共 [8/8 项四系统 Debug 构建已实际通过](checks/lazy-feedback-platform-ci-final.json)，八份终态原始日志已按 `144f6111` 的同 OS 基线分类，未发现新增反馈源码诊断或未分类项；已知工具与固定依赖警告保留。fork 的 JNI cache 均 miss，上游均 hit，但没有独立 Building/Linking 命令记录，因此不声称全部原生库重新编译。上一轮 [14:08:07 UTC 进行中快照](checks/lazy-feedback-platform-ci-pending.json)按原范围保留，不能代替终态结果。Android Looper、真实 TTS／SoundPool 播放、Lint、Release 和内存收益仍未验收；初始化失败的恢复仍需 destroy/init，未加载的按键音不补播，关闭偏好也不新增即时资源回收承诺。[创建及读回回执](checks/lazy-feedback-upstream-submission.json)确认源 SHA、目标、正文及一提交六文件范围。第一次创建返回 502，确认对应 head 无 PR 后重试成功，没有重复创建。真实播放验收未完成，保持 Draft；本轮不发布 APK。

日志进程候选的再次审阅还在受控 JVM fixture 中复现了 destroy 抛错而 stdout 继续阻塞、以及 IO 池被占满时 watcher 延后两项边界；没有据此认定 Android 应用已经发生无限挂起。它仍未提交上游，不能把退出轮询的一秒窗口写成整个取消过程的硬上限，后续先补闭环再推进。

## 下一项：回车标签与实际动作一致

[现有四个 Draft 的最新复核](checks/current-pr-build-feedback-next5.json)确认 #2176–#2179 仍无评论、审阅或未解决讨论，源 SHA 未变，fork／上游合计 32/32 个构建 job 通过。#2179 的[说明更新及读回](checks/lazy-feedback-validation-refresh.json)已只将构建进度改为八系统任务成功，保留原始提交回执和真实播放限制。

已推送 [contrib/pr-enter-actions](https://github.com/X2M7/trime/tree/contrib/pr-enter-actions)，并创建上游 [#2180 Draft](https://github.com/osfans/trime/pull/2180)。新提交 `5879c1eb` 直接基于 `144f6111`，一个提交、六个文件，[源码绑定](checks/enter-actions-submission-source.json)记录精确范围。原 `bcb47b1a` 分支不改写，其 helper、十二项原测试与 service 派发代码全字节不变；新提交补齐标签委托的内部标量入口、全部缓存键面的标签变化通知及四项真实委托／广播测试。

具体修复包括：带非零 custom actionId 的已命名动作不再错误采用 imeOptions 对应的标签；TYPE_NULL／NO_ENTER_ACTION 采用原始回车的默认标签；app-only 缺省不再显示字面量 null。Mode 2 在缺少 app label 时新增实际标准动作的主题标签回退，随后才回退主题默认。原键盘没有接收标签变化通知，同缓存布局早退可能继续依赖无关事件触发绘制；现已监听广播并使全部缓存的子按键失效，不重建布局、不触发 Rime 或部署。这个源码闭环不是已完成 Android 像素验收的声明。

[本轮聚焦检查](checks/enter-actions-focused-host.json)实际重新执行 12 项纯 policy 测试通过，严格 Kotlin helper／test 编译无警告；其余四个文件在该宿主工程仅格式检查。精确提交的[完整应用 JVM 验证](checks/enter-actions-application-jvm.json)已实际通过：317 项／21 组，失败／错误／跳过均为 0；301 项基准完整保留，新增 16 项准确匹配最终源码，其中 4 项实际 Theme／Kodein／delegate／InputBroadcaster 测试已运行。官方 ZIP 大小／digest／CRC、源码／控制身份、20 个递归 pin、真实非缓存任务与原 XML／HTML 均已核对；没有新增源码编译诊断，既有 build-logic 和工具警告仍在。[fork／上游八项 Debug 构建终态](checks/enter-actions-platform-ci-final.json)已全部通过，两项 Ubuntu 全仓库样式检查亦通过；八份日志按同 OS 基线复核，未发现新增源码诊断，固定依赖与工具警告保留。[15:34 UTC 进行中快照](checks/enter-actions-platform-ci-pending.json)作为历史原件保留。[实际提交及读回回执](checks/enter-actions-upstream-submission.json)已核对目标、源 SHA、正文及一提交六文件，未报告文本合并冲突并保持 Draft；[PR 正文进度及读回](checks/enter-actions-validation-refresh.json)已同步八项构建终态，保留 Draft 和原验收限制。Android 实际 EditorInfo 构造、service 提交、屏幕重画／缓存来回切换、Lint 和 Release 仍未验收，既有工具告警保留；本轮不发布 APK。

## 下一项：预编译 JNI 目录弃用调用

[五个现有 PR 的本轮复核](checks/current-pr-build-feedback-next6.json)确认 #2176–#2180 的 fork／上游 40 个构建 job 均通过，未见新增维护者反馈；这不补充其 Android 实际运行验收。

已推送 [contrib/pr-prebuilt-jni-directories](https://github.com/X2M7/trime/tree/contrib/pr-prebuilt-jni-directories)，并创建上游 [#2181 Draft](https://github.com/osfans/trime/pull/2181)。新提交 `dfcc87df` 直接基于 `144f6111`，仅将 `NativeBaseConventionPlugin.kt` 的一行 `jniLibs.srcDirs(setOf("prebuilt"))` 改为 `jniLibs.directories.add("prebuilt")`。[源码绑定](checks/jni-source-directories-submission-source.json)确认一个提交、一个文件、增加／删除各一行；原条件、CMake 回退、ABI 与打包配置不变。最初仅提交正文超出贡献规范行宽的 `e612d66a` 分支和原运行保留，新提交 tree 与之相同；初轮构建已取消，不当作最终源码验收。

[精确源码的完整应用 JVM 验证](checks/jni-source-directories-application-jvm.json)实际通过 301 项／19 组，失败／错误／跳过均为 0，完整保留基准用例多重集。官方附件大小／digest／CRC、源码及控制身份与 20 个递归 pin 已核对。实际非缓存执行 `:build-logic:convention:compileKotlin`，原结构化 `srcDirs` 警告已消失，没有新增源码诊断；其余 98 条广义工具警告候选保留，不是全工程零警告。

[AGP API 与调用时机审阅](checks/jni-source-directories-api-review.json)以及[可复现配置对照](checks/jni-source-directories-configuration/README.md)核对了当前早期追加路径。独立 harness 使用真实 AGP 9.3、Gradle 9.8.1，在两个场景中通过 21 个断言：默认／自定义／prebuilt 目录和标记文件均保留，带空格的相对路径与顺序一致，真实 Android 插件早期的 late delegates 为 0。最终实际退出 0、两个 ProjectBuilder teardown 完成；成功路径随后显式结束独立 JVM，旧轮离线解析、Provider 前置失败与后台线程受控终止均保留，不记作通过。未验证 Provider、变体定稿后的动态添加、真实 native 文件或 APK 打包。

[四系统构建的 16:19 UTC 快照](checks/jni-source-directories-platform-ci-pending.json)仍为四项运行中、无失败，不能提前记为通过。[英文 PR 说明](drafts/jni-source-directories.md)保留此限制；[创建及独立读回](checks/jni-source-directories-upstream-submission.json)确认目标、精确 SHA、正文和一提交一文件范围，未报告文本合并冲突。Lint、Release 与预编译库实际 APK 内容仍未验收。复现工具和报告仅在 fork 索引，不带入上游的一行补丁。本轮不发布 APK。

## 日志采集可靠性：按完整行为提交

已推送 [contrib/pr-logcat-reliability](https://github.com/X2M7/trime/tree/contrib/pr-logcat-reliability)，并创建上游 [#2182 Draft](https://github.com/osfans/trime/pull/2182)。源码 `6e4f2e59`，直接基于 `144f6111`。按本轮新的分组原则，一次提交覆盖整个日志采集链路：[六文件范围](checks/logcat-reliability-submission-source.json)包含 Process、Logcat、部署日志调用方和日志 View，以及两个回归测试文件。原 `f5909dcb` 分支与失败原件保留，未把它原先存在边界的 17 项通过结果当作当前模块验收。

当前实现一起处理：阻塞读取消、销毁失败仍尝试关闭三流、正常 IO 池耗尽时独立调度清理、原始取消节点上的错误保留、停止后重开和旧 Job 隔离；API21–23 短参数、threadtime 的完整 PID 列及仅 LF 分帧；API24+ 原生 PID 参数；短行/两种格式着色、先订阅后读取以及 View 替换/脱离时停止；部署清理/快照的 IO 创建、可合作等待及失败后保留通知流程。辅助类、UI 调用方、兼容处理和测试一起进入这个 PR，不再分成数个微小 PR。

[聚焦 JVM 验证](checks/logcat-reliability-focused-jvm.json)已实际通过 32 项（Logcat 17、Process 15），失败/错误/跳过均为 0，严格 Kotlin 编译无源码警告，六文件格式通过。新的取消诊断回归先在 32 项中失败一项；实际异常图确认错误只附在协程恢复的副本，随后将其保留在原始取消节点，完整 cause/suppressed 图继续要求原错误对象可达。没有放宽为字符串检查，失败与诊断原件保留。独立 JVM 的 IO=1 用例和真实 Java 子进程取消均执行；宿主资源编号/本地化错误适配与 Android 调用方仅格式/源码审阅的范围单列。

[独立复核](checks/logcat-reliability-independent-review.json)将最终六个 Git blob、工作树和宿主输入逐字节绑定；[官方选项/调度依据](checks/logcat-reliability-api-review.json)另列。宿主 Spotless 的既有 Gradle 弃用提示保留。[精确源码的完整应用 JVM 验证](checks/logcat-reliability-application-jvm.json)实际通过 333 项／21 组：301 项基准完整保留，32 项新增标签与最终源码一致，失败／错误／跳过均为 0；官方附件大小／digest／CRC、身份与 20 个递归 pin 均核对。没有新增源码诊断，独立基线仍有原 srcDirs 警告，101 条工具候选与基线相同，不是全工程零警告。[四系统构建快照](checks/logcat-reliability-platform-ci-pending.json)仍为运行中；[英文 PR 说明](drafts/logcat-reliability.md)保留该状态。[创建及独立读回](checks/logcat-reliability-upstream-submission.json)确认目标、精确 SHA、正文及一提交六文件范围，未报告文本合并冲突。Android UI/实际 logcat、Lint、Release 未验收。同步平台销毁/关闭没有通用硬时限，未新增并行 stderr 排空；脱离 View 后仍需重新绑定，不新增同一 View 的自动重放。本轮不发布 APK。

## 关联 issue 的审慎匹配

[匹配记录](checks/issue-match.json) 保留六个原主题的只读检索结论与相近讨论，以及新主题的范围说明；未确认可直接关闭的精确开放 issue，不填写虚假的 `Fixes`/`Closes`。这是关键词和部分历史讨论的有限检索，不是全库不存在问题的证明；新的 JNI 线程主题也不冒用其他 JNI 崩溃报告。

## 后续拆分顺序

下面是重建路线，不表示这些功能已经提取、推送或通过。依赖指实际行为契约，不是必须提前提交的大型“公共基础设施”补丁。每项只从共享文件摘取所需代码与最小回归；若摘取时发现新的依赖，先更新清单，再决定合并同一功能或调整顺序。

### A. 优先选择独立问题

| 主题 | 边界 | 验证重点 |
| --- | --- | --- |
| 反馈资源后续设备验收 | 独立实现已见第二批；隐私钩子留给隐私补丁 | 实际 TTS/音效及 Android 生命周期；不重复提交第二批代码 |
| 旧 Android logcat 命令兼容 | 子进程生命周期已见第二批；另提 PID 过滤与 clear 选项 | API21 实际命令、完整 PID 匹配、不把日志正文当 PID |
| 图标字体启动开销 | Compact CommunityMaterial、固定字形表、来源/许可证与生成器 | 名称/字符/字体与原依赖一致；冷启动实际测量；独立回归，移除整套 T13 探针依赖 |
| 离线生成许可证 | AboutLibraries 离线数据与缺失条目 | 无 GitHub 请求、内容完整；不顺带升级插件 |
| 回车动作后续集成验收 | 独立实现已见第三批；只验证回车契约，不并入全部 EditorPolicy | custom actionId、NO_ENTER_ACTION、多行、搜索/发送及真实主题标签 |
| Shift 双击尊重主题动作 | KeyView/Keyboard 的 modifier 逻辑，属于上游已有双击 Shift 的回归修复 | 上游 `8545278b` 已有基础双击；自定义主题动作不被固定切换覆盖，不作为新增 CapsLock |
| 系统敏感剪贴板过滤 | 新建只读 SensitiveClipboardCompat；ClipboardHelper 在去重和异步入库之前拒绝 marked clip，两个生产文件与一个聚焦测试 | marked 文本不写入历史，也不生成新建议；普通内容及 API 分支保持；不清除既存普通建议/历史，不宣称完整 T07 |
| 格式检查跳过生成目录 | Spotless fileTree 一处 | 源码仍被检查，build/.cxx/.gradle 不被遍历；不改 ktlint 版本 |
| 原生构建并行度 | CMake Ninja job pool 独立代码块 | 未设置、有效及非法并行度；不带第三方覆盖层 |
| 构建时间戳单位 | timestamp 解析器、getter、聚焦测试 | 秒/毫秒及非法值；不改展示版本或发布策略 |
| ABI 参数校验 | ABI 解析器与 NativeBase 窄调用 | 默认值、合法覆盖、重复/空/非法值 |
| JNI 线程归属后续设备验收 | 独立实现已见第三批；保持既有错误返回约定 | 真实 native 回调/CheckJNI；不重复提交头文件或新增通知回调异常 |
| 预编辑浮层触摸与窗口 token | PreeditDelegate、删除额外触摸 PopupWindow，保留 InputView/service 的真实触摸区域 | API21 BadToken、浮层可点击、显隐及编辑器切换；不能只删除窗口而丢失触摸范围 |

这一组也不一次性创建全部 PR。先挑维护者认可且复现充分的 2–3 项，完成完整构建与针对性验收，再提交下一批。没有核实匹配的问题，不填虚假的 `Fixes #…`。

Shift 的主题动作回归与 `Keyboard.refreshModifier` 内 `result || setModifier` 短路造成的后续修饰键未更新是两项独立问题。原探针同时依赖后者，不能把整份探针或两个修复作为一项双击功能搬入；分别准备最小复现与补丁。

敏感剪贴板补丁先限制新内容进入现有唯一的入库/建议发布入口。若另外要求清除旧建议，应单独处理 nullable listener、InputBar 和旧的普通内容异步回调时序；不把这些行为默认算作“读取敏感标记”已经覆盖。

### B. 核心正确性与候选交互

| 主题 | 先决行为 | 单独验证 |
| --- | --- | --- |
| Rime 队列及失败处理 | [四文件最小闭环范围设计](scopes/rime-startup-failure.md)，尚未实现 | FAILED/ready 等待、原生幂等清理和 runConfined 必须一起接通；FIFO 只描述准入 Runnable，不等于跨挂起事务或消息输出无丢失 |
| 引擎异步启动与失败重试 | 队列契约 | 首次启动、部署失败、降级输入与重试 |
| 解锁后 Direct Boot 状态交接 | 队列/偏好生命周期 | 与上游已合入的 Direct Boot 修复去重 |
| 编辑器会话隔离 | 队列契约 | 切应用、服务重建、延迟响应及旧提交不会串框 |
| 输入框强制模式临时生效 | 编辑器边界 | 数字/邮箱/密码退出后恢复用户模式；与回车修复分离 |
| 第三方主题布局回退 | 主题加载和错误契约 | 缺失布局回退、自定义配置保留；不夹带大范围主题重写 |
| 候选刷新/点击交错安全 | 编辑器隔离 | 快照/revision、分页边界和候选身份；先不含预测、置顶、无障碍包装 |
| 展开/收起保留定位 | 候选身份 | 同一词与精确像素偏移，重新布局后保持 |
| 候选字号/注释/长句可读 | 稳定候选布局 | 大字体不裁切，长句可展开读全 |
| 可选候选横向滚动 | 稳定候选布局 | 滚动不误选，不与字号和置顶一起提交 |
| 忘记此词的反馈 | 候选身份 | 删除学习记录成功/失败反馈；不等同永久屏蔽 |
| 候选置顶/恢复排序 | 候选身份及持久化规则 | 固定词身份、重启保持、恢复默认；与删除学习记录分开 |

共享的 `Rime.kt`、`TrimeInputMethodService.kt`、`KeyboardWindow.kt`、`AppPrefs.kt` 等只能按功能块提取。不得把这些最终文件整份复制进某个“基础 PR”。

### C. 词库、隐私与新增输入功能

| 主题 | 边界及依赖 | 验证重点 |
| --- | --- | --- |
| 用户词库导入/恢复事务 | 字典文件/格式、维护排他与安全备份 | 失败和取消可恢复，覆盖安装后数据保留 |
| 常用短语编辑/导入导出 | 在安全词库维护之上增加编辑 | 格式、重复项、异常与重启持久化；不附带候选置顶 |
| 统一学习隐私 | 编辑器会话 + 原生学习/插件/上下文 + TTS/日志等全部内容出口 | 密码、NO_PERSONALIZED_LEARNING、手动隐私完整生效；正常学习恢复；不能仅隐藏 UI |
| 隐私会话剪贴板隔离 | 敏感标记过滤可先独立；手动/编辑器私密许可依赖完整隐私契约 | 延迟入库、会话切换、建议与用户明确复制动作 |
| 九键基础输入（当前核心） | JNI/候选身份/主题回退；最小可用 schema 与布局 | 一次 JNI 快照、全拼/九键切换；先移除 predictor、模糊音与 onboarding |
| 九键拼音筛选/撤销编辑 | 九键基础 | 数字串、音节歧义、撤销及候选身份 |
| 九键模糊音与纠错 | 九键基础 | 固定虚构语料、开关与错误回退；先讨论策略 |
| 手动中英文状态记忆 | 临时强制模式；仅九键动作部分依赖九键 | 强制模式退出恢复、大小写与未提交拼音 |
| 数字/符号/邮箱布局 | 主题及动作规则 | 小数点、负号、@/./，减少切换但不丢拼音 |
| 下一词联想 | 候选身份 + 隐私 + 对应 schema | 小型离线模型、缺失回退、次数限制、按数字即新输入、不串框 |
| 输入延迟测量 | 队列及候选身份稳定后加少量可选打点 | 关联输入/显示/提交；显示可合并但按键不丢失 |

九键、联想等新增功能先按上游贡献指南讨论设计。不会通过给每个功能再附整套原生覆盖/设备探针来重复扩大补丁。

### D. 设置、存储与设备适配

| 主题 | 边界及依赖 | 验证重点 |
| --- | --- | --- |
| SAF 同步与恢复 | 路径约束、指纹/枚举、取消及 Worker；不夹带设置 UI | 授权、路径逃逸、失败恢复、用户文件不被覆盖 |
| 配置导入校验与原子激活 | 校验器、文件事务、取消提交边界、偏好/授权和引擎维护 | 失败保留旧配置，成功后才激活，不重复部署无关数据 |
| 首次设置选择布局/简繁并试打 | 已有 schema 选择及导入/主题契约 | 新装默认可输入，旧配置不覆盖，高级设置返回；底部 Barrier 已独立提取 |
| 常用设置入口 | 复用已有设置能力 | 状态读回、切换布局和方案一致，无关数据不部署 |
| 键盘按键无障碍 | 按键语义、hover、激活与动态回车 | TalkBack 独立探索/激活；TTS 朗读不能替代无障碍 |
| 候选/拼音筛选无障碍 | 候选身份；拼音筛选部分再依赖九键 | 选中状态、操作、旧节点激活不误选 |
| 横屏/分屏/大字体可见区 | 实际 viewport、insets 和触摸边界 | 候选/应用内容不遮挡；与语义实现分离 |
| 窗口导航栏和重建问题 | 先在上游已有具体 Activity 上重现并局部修复 | API21/新系统、对话框、旋转；暂缓整套新窗口基类 |

## 暂留 fork 或另议的内容

- 大型验收编排、数千行设备探针、截图 Observer、外部 TalkBack/eSpeak 补丁和性能语料保留在 fork；只将与具体问题有关的精简回归随功能提交。这不是以后再造一个“测试基础设施大 PR”。
- `PublicWindowPilotActivity` 及多组 Legacy/PublicPreferenceDialog 新抽象暂缓。原分支最后几个很小的导航栏提交依赖这些上游不存在的类，不能仅凭提交小就独立 cherry-pick。
- AGP/Kotlin/JDK 兼容策略、私有 Gradle API 适配、CI 重写、生成资源/缓存、图标和版本显示分别讨论，不作为普通应用修复的前置条件。
- OpenCC/librime 的词典 mmap、缓存恢复、Darts、解析与资源释放问题，先整理成依赖项目的最小补丁。再决定 Trime 采用固定上游版本还是必要的小适配；不直接搬入全部 CMake 文本替换覆盖层。本次不向其他仓库发布。
- fork 品牌、包名、签名、Pages、发布脚本、版本策略、历史构建验收流水不随应用功能上送。
- 真机、物理 ARM64/OEM、TalkBack 实际体验、长期驻留、16 KiB 页及 P95/PSS 等没有因本次拆分而获得验收结论。

## 查漏与操作规则

[paths.tsv](paths.tsv) 保留原比较的全部 660 个路径、增删行数及初步审阅归属。归属是粗分类，不是已验证的可执行补丁划分；尤其 122 个共享集成路径仍需逐块拆解。目前 14 个原路径有主题代码已提取，另外增加了 4 个重建后的聚焦辅助类/测试文件；未涉及的 646 个原路径仍仅作归类。触及某个路径不表示其中全部功能已提取，例如 Manifest 只取 TTS query、反馈管理器不取隐私钩子、Logcat 不取旧系统命令兼容。CI 控制分支不计入这个应用模块提取数量。

1. 每个新分支从当时核实的上游 `develop` 开始，独立问题无相互依赖；必须依赖的功能明确写出父补丁，等待父补丁合入后整理，不把累积差异冒充单个 PR。
2. 每个修复说明一个具体触发场景、前后行为及对应验证；同一共享文件内无关的 import、偏好、字符串和格式修改都留到所属补丁。
3. 送审前核对相关 issue/PR，补齐上游构建、全测试和真实复现；不把“源文件少”当作验证豁免。
4. 每次只向维护者提交少数单主题 PR，收到反馈后再推进下一组；前两项已经合入；新布局分支分别验证，未验收项如实保留。
5. 原聚合分支、旧提交和发布产物继续保留，今后可通过这个索引追踪哪些行为已迁入独立分支。未取得新的明确指示，不关闭、重开或标 Ready 原 PR。

需要在本地继续某一分支时，可以递归克隆 fork，然后选择该分支：

```sh
git clone --recursive https://github.com/X2M7/trime.git
cd trime
git switch --track origin/contrib/split-setup-footer
git submodule update --init --recursive
```

实际版本以 branches.json 的 Git SHA 为准，避免之后分支移动导致误用另一轮验证结果。


## 核心模块：九键拼音输入

核心分支 [contrib/pr-t9-input](https://github.com/X2M7/trime/tree/contrib/pr-t9-input) 已更新到 `5b6d1df4`，累计三个提交、61 个文件；上游 [#2183](https://github.com/osfans/trime/pull/2183) 保持 Draft。这是一个完整九键模块：方案、两套主题、原生拼音筛选／锁定／撤销、预编辑光标和数字／符号页一起交付，不继续按 helper 细拆。预测、隐私、词库管理和 fork 发布配置保持独立范围。

本轮修复标准 UTF-8 与 JNI Modified UTF-8 混用造成的生僻字／表情损坏与预编辑长度不一致，以及字符串转换失败后的 native 副作用；清理 JNI 的 signedness、return-move 和 aggregate-init 诊断。[完整模块边界](scopes/t9-input.md)及[本轮结果与 Git blob 绑定](checks/t9-input-unicode-warning-repair.json)明确对应最终提交。

8 个 JNI 翻译单元的 NDK28 x86_64/API21 严格编译无诊断；真实 HotSpot17 checked-JNI 的 115 条检查通过，原版本对照确定失败。最终干净提交另重跑四个原生进程、共 743 条断言通过，复用此前未修改的固定引擎库，未把它写成本轮重建引擎。上轮精确 `649c0755` 的 328 项应用 JVM 结果保留，源码／资源和既有 JVM 测试树未变；[新精确源码云端检查](checks/t9-input-fixed-source-ci.json)也已核验通过：328 项应用 JVM 用例／25 组及 JDK25 的 115 条 JNI 断言通过，官方附件、原 XML、实际执行任务、源码和 20 个 pin 均已核对。

新 [fork Debug/native](https://github.com/X2M7/trime/actions/runs/39526931659)、[上游 PR Debug/native](https://github.com/osfans/trime/actions/runs/39526937528) 仍在运行；[完整应用 JVM／真实 VM](https://github.com/X2M7/trime/actions/runs/39526955308)已通过，分别核对新原件，不将未完成的 Debug 平台记为通过。既有构建工具／依赖警告、一般 OOM 异常链及 Android ART／界面／输入连接／Lint／Release 验收限制保留。原 #2170 不重开，本轮不发布 APK 或稳定版。
