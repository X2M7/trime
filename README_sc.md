<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# 同文 Android 输入法平台

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

[English](README.md) | 简体中文 | [繁體中文](README_tc.md)

## 关于

源于开源的[注音仓颉输入法]前端，基于著名的 [RIME] 输入法框架，使用 JNI 的 C 语言和 Android 的 Java/Kotlin 语言书写，旨在保护汉语各地方言母语，音码、形码通用的输入法平台。

[查看文档](https://github.com/osfans/trime/wiki)

## 九键拼音

这个 fork 在 Trime 中加入基于方案词典的 `朙月拼音九键` / `luna_pinyin_t9`，两个内置主题均提供 3x3 主键区。使用 2–9 输入拼音，支持逐段锁定、中间编辑、解锁与撤销；选择拼音不提交汉字。布局支持拼音侧栏或横排、左右单手模式、键高设置、长按数字和空格滑动移光标。

[项目页面](https://x2m7.github.io/trime/) | [下载九键稳定版 APK](https://github.com/X2M7/trime/releases/latest)

稳定版目标 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 已完成C37构建/静态、API21/API35完整矩阵、真实SAF、Direct Boot压力、自然PIN及三轮冷启动验收。实际构建提交 `a5309c0256e325a0f042d2d9062ddbd4f1326c28`，冻结目录 `build/t07-privacy/stable-candidate37-final1`，时间戳1791464154404（2026-10-08T12:55:54.404Z）；完整构建708.260124秒，应用JVM582、build-logic9、Spotless与Lint0通过，编译/build warning行0，527项clean源码前后不变。

C37以自有副本固定列出的FD，优先完整fstat设备/inode身份；仅真实EACCES可在两份声音fixture全部校准后使用有界fdinfo，严格唯一inode与全部fixture不同才证明非fixture。仅取得副本时直接原因EBADF可记录原槽位消失，固定后所有错误均失败。两矩阵完成原200次扫描、同文件0→1→2→1→0、9无关句柄、隐私/TTS/SoundPool/播放/释放与30秒预算。API21仅8探针、未用fdinfo fallback；API35有11探针及实际自有副本元数据证明。实际APK审计保留19 appDEX/6 testDEX、21类/148完整body及catch/603引用、未解析0/高于21为0，56反例全拒绝、两fresh replay的31输出逐字节相同、51文件/553依赖；ARM64仍只作构建/静态。

QA3绘制身份归一化和精确旧PID INFO clean-exit分类保留实际focused112及完整raw/PID/context严格守卫；QA4只允许expected regular file且解析size0时的可信Toybox regular-empty别名，实际focused61保留原8字段、数值/inode/权限/mtime守卫、raw kind和stat命令，不编造numeric S_IFREG。QA5在父脚本内以lazy工厂隔离四核心DeviceRun子类，完整Matrix startup方法仅core2→4 literal不同，boot/storage/readiness/cleanup行为保持，Matrix脚本逐字节不变；其实际focused32和独立source peers通过。QA5 live98+139+56=293项一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`；当前受影响publication23项一次PASS，host END2026-10-08T16:49:26.873240Z，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`。QA3发布68+23仅保留历史一次实测，END2026-10-08T14:58:52.115495Z，`build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`；未变的原adjudicator68与actual matrix-only adju2保持自身证据，QA5没有重跑。每次host运行绑定自身QA13及15/17/10依赖，旧全局QAepoch仅作历史快照；当前适用性peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee`保留矩阵/SAF/byteaudit实际执行范围；527项候选源码及三APK仍是已验收C37构建。 QA6将继承的唯一handoff后user-state读取保留为完整stdout/stderr及精确command index；重放要求该相邻读取唯一、rc0、stderr空且独立解析RUNNING_UNLOCKED，然后normal input仍须立即衔接，所有原actions/deadlines保持。当前live Natural56一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`，受影响publication23一次PASS；parent与Matrix仍逐字节不变，当前适用性peer通过独立15项执行依赖保留pressure final5实际范围。QA5 host证据与更早全局QAepoch仅保留各自历史源码范围。

矩阵、真实SAF三阶段、fresh pressure final5、独立自然PIN及三轮独立冷启动均以自身实际报告和独立peer通过。压力保留原7秒/1.25秒/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PIDbootbinding/一次真实Delete/资源和清理协议；Natural无controlled rebind或stress injection。仅覆盖两个矩阵的actual adjudication通过，validation errors0/unexplained blockers0，仍保留warning记录1558、known project diagnostics480，warning_free=false；该裁决不替代独立SAF/pressure/Natural/cold门禁。六门禁独立peer SHA依次为`7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`。 Fresh pressure final5与Natural final2只覆盖明确记录的4-vCPU/2048MiB host-GPU环境，两个矩阵和冷启动仍覆盖原2-vCPU/2048MiB环境；不声称两核心环境7秒保证，也不声称已证实或修复CPU特定产品缺陷。 外部资源字节证明来自UID2000输入动作结束后的有界privileged read-only lease，随后还原UID2000；不证明AppUID直接访问。

应用APK：ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c`（22,442,517字节）、x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e`（22,539,554字节）；test `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db`（1,480,837字节）。应用各保留2条META-INF v1服务提示、test0，min21/target37/versionCode20261115与原debug包/证书保持。API21的105条AudioTrack FAST提示保留而SoundPool功能完成；不声称物理ARM/16KiB页/OEM全覆盖或全部bug/warning消除。C37旧失败保留：P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`；局部handoff/Delete不计whole或资源通过，P2资源审计body从未开始。发布目标为已授权稳定tag/链接，六文档提交后继续递归源码归档、两应用APK staging及11项实际evidence验证，不编造此前已published时间或receipt。 P3原inventory87文件、81份四步证明完成，#82 LOCK仅stat-before；其cat/device hash/stat-after和剩余5文件未到达。P4耗尽未放宽的7秒观察预算，接近边界的新进程fork未提供完整预算内进程或ready-input证明；独立时序/源码诊断 `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7`没有建立可支持的产品延迟修复，也未把LatinIME主动自杀解释为fatal/OOM。两次原失败只留历史；当前whole验收仅来自明确四核心范围的新pressure final5。 Natural1原whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c`保持：bounded handoff及Delete/UID2000局部输入证明完成，但固定尾部读取的span bookkeeping失败，资源审计未到达；独立诊断 `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93`及diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050`不重分类旧run，当前自然路径验收只来自新Natural final2。 首次publication23 fixture尝试因audit推导前缺少locked-log输入而失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`，该失败host run保持历史。第二次尝试22个方法通过，但旧反例重排记录后未重新封存命令索引而有1个失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`，仍保留为失败host尝试；最终23验收只采用修正fixture/反例自身完成且绑定实际源码的报告。

### C36/C35 历史记录（不借用为 C37 验收）

以下段落保留当时原记录，C37 当前状态以上方 pending 为准。

拟发布稳定版 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 当前 **candidate36：构建/静态及 Android 5.0 完整矩阵通过，Android 15 完整矩阵整体 FAIL，稳定发布停止**。C36 提交 `97d38863a4353fbdde95eca3592a60e4a653887e` 仅修改 AndroidTest 的 `InputFeedbackProbe.kt`，生产代码未改；冻结目录 `build/t07-privacy/stable-candidate36-final1`，时间戳 `1791460043820`（2026-10-08T11:47:23.820Z）。完整构建 458.793 秒、应用 JVM 582 项、build-logic 9 项、Spotless、Lint 0、编译/build warning 行 0，527 源码不变；C36 新 APK 身份/签名/ABI/48 资源/16 KiB ZIP/ELF 静态通过。应用各有 2 条 META-INF v1 服务提示、测试 APK 0，min21/target37 和原 debug 证书/包保留。415 项设备 QA 宿主及独立源码复核通过，11 QA 摘要冻结。C36 Android 5.0 实际完成 8 探针、13 路线、17 生命周期、100 次显示隐藏、52 个 glyph/4 次点号提交及降级输入/重试；独立复核 925 项通过。Android 15 随后在声音探针 PID5442 首次 FD 快照的 `Os.stat` 收到 EACCES，整体失败；具体 FD 槽位未记录，不能宣称已定位或修复原因。其真实持有剪贴板 UI 和此前五项探针仅作局部通过。专项字节码、发布工具最终绑定及其余门禁仍待新完整证据：C36整体运行验收FAIL：API21 whole PASS、API35 whole FAIL，其余未运行门禁不借用旧/新候选；稳定发布已停止。声音探针修正后的后续候选须重新构建并完成全部自身验收，不能借用 C36 API21 或 C35 通过。未推送、打标签或发布，Latest 仍为 `v3.3.13-t9.9`。

C35 当前 QA 的 Android 5.0 final2 矩阵已整体 FAIL：`InputFeedbackProbe.kt:103` 在 200 次同步读取后触发全进程 FD 增量超过 8 的断言，实际 before/after 数值与 FD 目标未记录，来源仍未证实，不能称已定位声音读取器泄漏或已证明假阳性；后续 SoundPool/旋律/播放检查未执行，顺序安排的 Android 15 正式矩阵未启动。C36 将该测试改为声音 fixture 的 `(st_dev, st_ino)` 逐 FD 槽位归属计数，保留全进程总数作诊断；同一文件打开两次的 1→2→1→0 控制与 9 个无关句柄控制已完成宿主复核，API21 实际校准已通过，API35 在首个快照过程中 EACCES 失败；修正后的新候选仍须重新校准并完成全部门禁。原 200 次读取、30 秒预算及隐私/TTS/SoundPool/播放/清理要求保持；不增加 GC、等待或容差，也不注入损坏 YAML 警告。

C35 历史记录（以下当时状态以本节上方 C36 记录为准）：candidate35 构建通过，完整设备验收未完成。统一隐私与固定上游 `e9b21960` 的 30 项整合提交保留；C35 使用 WorkManager 2.10.5 官方 Configuration.Provider 和稳定 lazy 配置，统一 getInstance 初始化路径，修复普通进程恢复时的双重初始化竞态，并由 SDK21 短路及实际 DPS/旧 DE/替换状态守卫拒绝不安全进程初始化。实现提交 `dc69d73baa0c06e5a103000d1cd13f106f280afd`，527 项源码，时间戳 `1791449483471`（2026-10-08T08:51:23.471Z）；正式冻结目录为 `stable-candidate35-final1`，早前中断构建没有形成身份链。完整构建 519.403 秒通过，应用 JVM 582 项、build-logic 9 项均通过，Spotless 通过，Lint 0，编译/build warning 行 0；APK 身份、静态与专项 API21 DEX 检查通过。应用 APK 各保留 2 条 META-INF v1 提示（test 0）；旧 QA 的 C35 Android 5.0 矩阵与独立审阅真实通过，但当前 QA phase4 的 Matrix/绑定工具已变，API21 矩阵须全新重跑；未改的真实 SAF 三阶段与归属审阅经当前校验器复核后保留通过；首次 SAF 尝试因准备末尾保留 Latin、Main 重定向 Setup3 而在任何选择器动作前停止，不计 SAF 通过或产品缺陷，新的 gate 前准备恢复并核验 Trime，不写 SAF tree/首选项/grants；SAF 按实际进程归属的独立日志审阅亦通过，其余设备门禁与最终裁决仍待证据，C35 整体运行验收未完成，API21 final2 整体 FAIL（历史）。C35 Android 15 matrix final1 整体失败：真实持有剪贴板 UI 与九项探针通过，ClipEditorProbe 未确认稳定键盘，后续导航/生命周期/降级未执行，失败保留，后续解析诊断不改写该矩阵结论。C35 压力 final1 整体失败：原 7 秒交接与完整 6501 ms 手势通过，但第十阶段在既定一次 focus 前要求键盘，因此未完成真实 Delete/资源审计；失败证据保留，压力 QA 顺序校正通过宿主及独立 peer，但 final2 随后在 mutation-3 超过原 1.25 秒而整体失败，6503 ms 手势只作单项。epoch3 完整窗口短诊断仍是整体 FAIL。只读重放确认 75 份 Latin 帧中，第一份过渡帧仍被当前绑定守卫拒绝；其余 74 份完整显示且当前连接有效的帧带有 `mRestartOnNextWindowFocus=true`。Android 15 将其定义为未来窗口获得焦点时的重启请求，旧 false-only 谓词错误拒绝了这些有效帧。phase4 已协调应用严格唯一的 true|false 解析，保留当前 PID、序列、有效连接、方法、EditorInfo 与可见性检查；未来标记原值继续记录，但不进入稳定绑定身份。独立复核还发现初次 /tmp 普通输入提案会漏掉空值、等号前后空白或裸字段名的重复字段，已在任何 live 应用前修正，两套普通输入测试均拒绝这些重复字段。随后全新 phase4 Android 15 剪贴板短诊断及完整原探针断言实际通过，只执行一次 Latin→fresh Trime；独立运行复核仍待完成，短诊断不计完整矩阵或发布验收。精确应用后工具的 415 项宿主检查全部通过；宿主与源码复核只证明解析修正，不把旧诊断改成通过，也不代替新设备结果。当前 API21/API35 矩阵、压力与自然 PIN、冷启动及最终裁决均待全新证据。C31–C34 结果只作历史；C34 自然交接及一次 Delete 部分证明不能代替其整体失败。原 7 秒/1.25 秒/6500 ms/60 Hz 规则保留，特权只读文件审计单独标记，不伪称应用 UID 访问证明。现有 debug 证书/包和 C31 历史 SAF 部署 6,280 ms 排队限制保留；C35 本次目录选择/完整部署也记录一次非注入的 5,434 ms Rime 排队，早于 instrumentation，三份快照为同一事件，残余性能限制保留；最终 SAF 归属审阅保留一次 root 系统 input.Input 命令退出时的 native SIGSEGV，按 PID 确认为系统命令而非 Trime，不能称完整设备日志无 fatal；ARM64 仅构建/静态检查，无 ARM64 真机或原生 ARM 镜像验收。当前未推送或发布，Latest 仍为 `v3.3.13-t9.9`。详见[发布草稿](doc/releases/v3.3.13-t9.10.md)及[T07 验证报告](script/quality/VALIDATION-T07-PRIVACY.md)。

ARM64 与 x86_64 APK 保留 Android 5.0+ 支持，延续此前 fork 的包名 `com.osfans.trime.debug` 和签名证书，以支持覆盖升级；默认及非中文语言环境的显示名称为 `Trime`，简体中文为“同文输入法”，香港/台湾繁体中文为“同文輸入法”。附件仍是调试签名、可调试构建，未启用生产签名或 R8。升级前备份配置和词库；自定义目录中的同名方案或主题可能需要合并新规则、布局并重新部署，请保留个人词库。

[T04](script/quality/t04/README.md) 在独立目录中提供固定排名与学习测试，默认继续使用朙月词典，不启用大词库或语法模型。[T05](script/quality/t05/README.md) 将六组可选模糊音与三类有界数字键纠错分开设置，默认全部关闭，建议标明来源。优化计划 T08–T10 仍是后续工作：本版不宣称已完成其要求的真机正式构建候选延迟/PSS 预算、固定混输切换次数验收，或扩展的个人词库与候选管理验收。

C35 每份应用 APK 保留两项必要协程服务文件的 v1 META-INF 签名覆盖警告（测试 APK 为 0）；C36 新 APK 静态检查也实际确认两份应用各 2 条提示、测试 APK 为 0；必要资源继续保留，不能宣称 warning_free。模拟器验收不代表 ARM64 真机性能、所有 OEM 编辑器或物理 16 KB 页设备已验证。Release 仅上传两个应用 APK 和一个递归源码包，共三项手动附件；GitHub 自动生成的源码入口保留。另见[键位规则](script/quality/T03.md)、[九键设计](script/t9/README.md)和[上游合并记录](script/t9/UPSTREAM-t9.3.zh-CN.md)。

## 下载

- 九键稳定版 [点击下载](https://github.com/X2M7/trime/releases/latest)

- 上游稳定版 <br>
  [<img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='80px'/>](https://f-droid.org/packages/com.osfans.trime)
  [<img alt='Google Play 立即下载' src='https://play.google.com/intl/en_us/badges/images/generic/zh-cn_badge_web_generic.png' height='80px'/>](https://play.google.com/store/apps/details?id=com.osfans.trime)

- 每夜版 [点击下载](https://github.com/osfans/trime/releases/tag/nightly)

- 测试版 [点击下载](https://github.com/osfans/trime/actions)

- 配置文档 [rimerc](https://github.com/Bambooin/rimerc)

## 沿革

TRIME 是 Tongwen RIME 或是 ThaeRvInputMEthod 的缩写:

- 最初，输入法是写给[泰如拼音](http://taerv.nguyoeh.com/ime/)（tae5 rv2）的，中文名为“泰如输入法”;
- 然后，添加了吴语等方言码表，做成了一个输入法平台，更名为“汉字方言输入法”;
- 后来，兼容了五笔、两笔等形码，在太空卫士、征羽的建议下，更名为“[同文输入法平台 2.x](https://github.com/osfans/trime-legacy)”。寓意音码形码同台，方言官话同文。
- 之后，借助 JNI 技术，享受了 [librime](https://github.com/rime/librime) 的成果，升级为“同文输入法平台 3.x”，简称“同文输入法”。

现在欢迎你前来[贡献](CONTRIBUTING.md) ～！

## 开发入门

### 准备

#### 开发环境要求

- Android SDK 和 Android NDK
  - 如果还不熟悉 Android 开发，建议安装 [Android Studio](https://developer.android.google.cn/studio)，它会自动安装并配置 Android 开发环境。

- JDK（OpenJDK）17
- Python 3 (用于给 OpenCC 生成词典文本文件）

#### Windows 上的前提条件

当前构建配置会使构建过程中创建符号链接，开发者需要：

- 启用[开发者模式](https://learn.microsoft.com/zh-cn/windows/apps/get-started/enable-your-device-for-development) 以在无管理员权限的情况下创建符号链接。

- 启用 `git` 的符号链接支持：

  ```powershell
  git config --global core.symlinks true
  ```

如果无法或者不想启用上述设置也没关系。构建系统会自动在符号链接创建失败时使用复制代替。

### 构建

#### 1. 克隆此项目并拉取所有子模块。

```sh
git clone git@github.com:X2M7/trime.git
cd trime
git submodule update --init --recursive
```

#### 2. 编译调试版本:

```sh
# On Linux or macOS
make debug

# On Windows
.\gradlew assembleDebug
```

#### 3. 编译正式版本：

请创建 `keystore.properties` 文件，包含以下内容，注明[签名信息](https://developer.android.com/studio/publish/app-signing.html)：

```gradle.properties
storePassword=myStorePassword
keyPassword=mykeyPassword
keyAlias=myKeyAlias
storeFile=myStoreFileLocation
```

然后执行：

```sh
# On Linux or macOS
make release

# On Windows
.\gradlew assembleRelease
```

### 故障排除

```
Target "boost_log_setup" links to target "Boost::coroutine" but the target was not found.
```

在 Linux 或 macOS 上执行 `make clean`，Windows 上执行 `.\gradlew clean`。

其他问题:

1. 首先尝试 `make clean`
2. 确保你的仓库与最新版本一致。如果你修改了一个或更多的子模块，请确保它们与当前仓库版本兼容。
3. 如果问题依然存在（不太可能）, 尝试进行一次新的克隆。
4. 检查是否有 PR/issue 与你的问题相关。 如果有的话，尝试他们的解决方案。
5. 如果以上方法都不工作，你可以提一个 issue 来寻求帮助(可选)。

## 鸣谢

- 开发：[osfans](https://github.com/osfans)
- 贡献：[boboIqiqi](https://github.com/boboIqiqi)、[Bambooin](https://github.com/Bambooin)、[senchi96](https://github.com/senchi96)、[heiher](https://github.com/heiher)、[abay](https://github.com/a342191555)、[iovxw](https://github.com/iovxw)、[huyz-git](https://github.com/huyz-git)、[tumuyan](https://github.com/tumuyan)、[WhiredPlanck](https://github.com/WhiredPlanck)、[nopdan](https://github.com/nopdan)......
- [维基](https://github.com/osfans/trime/wiki)：[xiaoqun2016](https://github.com/xiaoqun2016)、[boboIqiqi](https://github.com/boboIqiqi)......
- 翻译：天真可爱的满满（繁体中文）、点解（英文）......
- 键盘：天真可爱的满满、皛筱晓小笨鱼、吴琛 11、熊猫阿 Bo、默默ㄇㄛ ˋ......
- 捐赠：[Releases](https://github.com/osfans/trime/releases) 中的“打赏”实时更新
- 社区：在 [Issues](https://github.com/osfans/trime/issues)、[QQ 群 (811142286)](https://jq.qq.com/?_wv=1027&k=AXdR80HN)、[QQ 群 (224230445)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=pg_q7UVumWYLq1Rk8kIAqkK1xGt64VnX&authKey=04m9l7OBO5H5vgrEL8IbpsmtnptWM60xy%2FUwYCfyvw9VcRhe8zRzAS1ezoemZdFr&noverify=0&group_code=224230445)、[Google Play](https://play.google.com/store/apps/details?id=com.osfans.trime)、[贴吧](http://tieba.baidu.com/f?kw=rime)、[Telegram](https://t.me/trime_dev) 中反馈意见的网友
- 项目：[RIME]、[OpenCC]、[注音仓颉输入法]等开源项目

## 第三方库

- [Boost C++ Libraries](https://www.boost.org/) (Boost Software License)
- [darts-clone](https://github.com/s-yata/darts-clone) (New BSD License)
- [LevelDB](https://github.com/google/leveldb) (New BSD License)
- [libiconv](https://www.gnu.org/software/libiconv/) (LGPL License)
- [marisa-trie](https://github.com/s-yata/marisa-trie) (BSD License)
- [glog](https://github.com/google/glog) (New BSD License)
- [OpenCC](https://github.com/BYVoid/OpenCC) (Apache License 2.0)
- [RIME](https://rime.im) (BSD License)
- [snappy](https://github.com/google/snappy)(BSD License)
- [utfcpp](https://github.com/nemtrif/utfcpp) (Boost Software License)
- [yaml-cpp](https://github.com/jbeder/yaml-cpp) (MIT License)
- [注音仓颉输入法](https://code.google.com/p/android-traditional-chinese-ime/) (Apache License 2.0)

[注音仓颉输入法]: https://code.google.com/p/android-traditional-chinese-ime/
[RIME]: http://rime.im
[OpenCC]: https://github.com/BYVoid/OpenCC
