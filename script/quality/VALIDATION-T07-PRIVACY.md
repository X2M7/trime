# T07 统一隐私模式验收：v3.3.13-t9.10

## C37实际验收完成

稳定版目标 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 已完成C37构建/静态、API21/API35完整矩阵、真实SAF、Direct Boot压力、自然PIN及三轮冷启动验收。实际构建提交 `a5309c0256e325a0f042d2d9062ddbd4f1326c28`，冻结目录 `build/t07-privacy/stable-candidate37-final1`，时间戳1791464154404（2026-10-08T12:55:54.404Z）；完整构建708.260124秒，应用JVM582、build-logic9、Spotless与Lint0通过，编译/build warning行0，527项clean源码前后不变。

C37以自有副本固定列出的FD，优先完整fstat设备/inode身份；仅真实EACCES可在两份声音fixture全部校准后使用有界fdinfo，严格唯一inode与全部fixture不同才证明非fixture。仅取得副本时直接原因EBADF可记录原槽位消失，固定后所有错误均失败。两矩阵完成原200次扫描、同文件0→1→2→1→0、9无关句柄、隐私/TTS/SoundPool/播放/释放与30秒预算。API21仅8探针、未用fdinfo fallback；API35有11探针及实际自有副本元数据证明。实际APK审计保留19 appDEX/6 testDEX、21类/148完整body及catch/603引用、未解析0/高于21为0，56反例全拒绝、两fresh replay的31输出逐字节相同、51文件/553依赖；ARM64仍只作构建/静态。

QA3绘制身份归一化和精确旧PID INFO clean-exit分类保留实际focused112及完整raw/PID/context严格守卫；QA4只允许expected regular file且解析size0时的可信Toybox regular-empty别名，实际focused61保留原8字段、数值/inode/权限/mtime守卫、raw kind和stat命令，不编造numeric S_IFREG。QA5在父脚本内以lazy工厂隔离四核心DeviceRun子类，完整Matrix startup方法仅core2→4 literal不同，boot/storage/readiness/cleanup行为保持，Matrix脚本逐字节不变；其实际focused32和独立source peers通过。QA5 live98+139+56=293项一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`；当前受影响publication23项一次PASS，host END2026-10-08T16:49:26.873240Z，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`。QA3发布68+23仅保留历史一次实测，END2026-10-08T14:58:52.115495Z，`build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`；未变的原adjudicator68与actual matrix-only adju2保持自身证据，QA5没有重跑。每次host运行绑定自身QA13及15/17/10依赖，旧全局QAepoch仅作历史快照；当前适用性peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee`保留矩阵/SAF/byteaudit实际执行范围；527项候选源码及三APK仍是已验收C37构建。 QA6将继承的唯一handoff后user-state读取保留为完整stdout/stderr及精确command index；重放要求该相邻读取唯一、rc0、stderr空且独立解析RUNNING_UNLOCKED，然后normal input仍须立即衔接，所有原actions/deadlines保持。当前live Natural56一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`，受影响publication23一次PASS；parent与Matrix仍逐字节不变，当前适用性peer通过独立15项执行依赖保留pressure final5实际范围。QA5 host证据与更早全局QAepoch仅保留各自历史源码范围。

矩阵、真实SAF三阶段、fresh pressure final5、独立自然PIN及三轮独立冷启动均以自身实际报告和独立peer通过。压力保留原7秒/1.25秒/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PIDbootbinding/一次真实Delete/资源和清理协议；Natural无controlled rebind或stress injection。仅覆盖两个矩阵的actual adjudication通过，validation errors0/unexplained blockers0，仍保留warning记录1558、known project diagnostics480，warning_free=false；该裁决不替代独立SAF/pressure/Natural/cold门禁。六门禁独立peer SHA依次为`7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`。 Fresh pressure final5与Natural final2只覆盖明确记录的4-vCPU/2048MiB host-GPU环境，两个矩阵和冷启动仍覆盖原2-vCPU/2048MiB环境；不声称两核心环境7秒保证，也不声称已证实或修复CPU特定产品缺陷。 外部资源字节证明来自UID2000输入动作结束后的有界privileged read-only lease，随后还原UID2000；不证明AppUID直接访问。

应用APK：ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c`（22,442,517字节）、x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e`（22,539,554字节）；test `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db`（1,480,837字节）。应用各保留2条META-INF v1服务提示、test0，min21/target37/versionCode20261115与原debug包/证书保持。API21的105条AudioTrack FAST提示保留而SoundPool功能完成；不声称物理ARM/16KiB页/OEM全覆盖或全部bug/warning消除。C37旧失败保留：P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`；局部handoff/Delete不计whole或资源通过，P2资源审计body从未开始。发布目标为已授权稳定tag/链接，六文档提交后继续递归源码归档、两应用APK staging及11项实际evidence验证，不编造此前已published时间或receipt。 P3原inventory87文件、81份四步证明完成，#82 LOCK仅stat-before；其cat/device hash/stat-after和剩余5文件未到达。P4耗尽未放宽的7秒观察预算，接近边界的新进程fork未提供完整预算内进程或ready-input证明；独立时序/源码诊断 `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7`没有建立可支持的产品延迟修复，也未把LatinIME主动自杀解释为fatal/OOM。两次原失败只留历史；当前whole验收仅来自明确四核心范围的新pressure final5。 Natural1原whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c`保持：bounded handoff及Delete/UID2000局部输入证明完成，但固定尾部读取的span bookkeeping失败，资源审计未到达；独立诊断 `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93`及diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050`不重分类旧run，当前自然路径验收只来自新Natural final2。 首次publication23 fixture尝试因audit推导前缺少locked-log输入而失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`，该失败host run保持历史。第二次尝试22个方法通过，但旧反例重排记录后未重新封存命令索引而有1个失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`，仍保留为失败host尝试；最终23验收只采用修正fixture/反例自身完成且绑定实际源码的报告。

| 当前C37项目 | 已取得实际证据与范围 |
| --- | --- |
| API21 whole matrix | PASS；`build/t07-privacy/runtime/t910-candidate37-api21-final1/summary.json` SHA `42da83c21c9211c6fd113ab4ca679ff2a05ce0b0cfa8abbf8562f2d2a609c0c3`；独立peer `7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2` |
| API35 whole matrix | PASS；`build/t07-privacy/runtime/t910-candidate37-api35-hostgpu-final1/summary.json` SHA `aff685d770074005648ff2de28b685d9d1caff949021e5dd7930579d822da634`；独立peer `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf` |
| API21 real SAF | PASS；`build/t07-privacy/runtime/t910-candidate37-api21-real-saf-final1/summary.json` SHA `b73dbfc4b7af41f45913b63789f4a6295d19690f2f6eea2da8b3eda386f84b33`；独立peer `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0` |
| API35 Direct Boot pressure final5 (4 vCPU/2048 MiB) | PASS；`build/t07-privacy/runtime/t910-candidate37-api35-direct-boot-hostgpu-final5/summary.json` SHA `ebbd9cd780167145c38bc25bab216e1e6c260f7b32cd07033a8e0fd999c07291`；独立peer `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2` |
| API35 natural PIN final2 (4 vCPU/2048 MiB) | PASS；`build/t07-privacy/runtime/t910-candidate37-api35-natural-pin-final2/summary.json` SHA `21adceb62c8981eeedcdb6879746889e14f548474d82ee9c847972958fc76433`；独立peer `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5` |
| API35 cold starts final1 (2 vCPU/2048 MiB) | PASS；`build/t07-privacy/runtime/t910-candidate37-api35-cold-gate-final1/summary.json` SHA `8ac3baaa4144e03a2b03ea94adde278c234bff1a3aa61818c9851fb6d2e84a9c`；独立peer `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5` |
| Matrix-only runtime adjudication final2 | PASS；`build/t07-privacy/reviews/candidate37-runtime-adjudication-final2.json` SHA `b4cdfe1e98121dfce7b7f2c9c7f229979a8a974ee5c0d98f48a2481dbd92fa52`；独立peer `9da79d4e78e411808621e0f1cea1e3497abaadb898225a68209e2ff821270e33`；仅两个矩阵，validation errors0/unexplained blockers0；warning1558/known project diagnostics480，非全部门禁全局去重 |
| QA5 affected98+139+56 historical scope | PASS；`build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48` |
| QA6 affected Natural56 | PASS；`build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8` |
| QA3 release tools68+23 historical scope | PASS；`build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`；独立peer `07b335960ed3bff507cbcedc98c4583697c3e7bfbf4e88d7172eb80a9bb51e14` |
| QA6 affected publication23 | PASS；`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`；独立peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee` |
| Actual APK API21 bytecode audit | PASS；`build/t07-privacy/candidate37-api21-bytecode-final1/report.json` SHA `714dea2824045e50d31f212859a4de356f750bb0e98f6a0801c397e1713a2205` |

原C35未知FD来源、C36 API21 PASS/API35 whole FAIL、旧候选部分结果仅作历史。原失败槽号仍未知，后续同UID诊断不回填。历史C35一次5434ms部署排队、C31的6280ms SAF排队及root系统input.Input原生SIGSEGV保留，不声称已在C37复现、修复或清零。最终发布校验继续由六文档提交后的归档/staging/11项evidence freeze完成；本文不生成或借用发布receipt。

## C36及更早历史原稿（不计C37验收）

以下正文保留当时状态；其中“当前”“后续C37尚待提供”为历史原文，旧待证据标记现明确为C36停止后的实际状态；标记替换另有逐项差异和逆向核对。C37提交与新构建已形成，上方表格才是本次候选的实际范围；C36 API21 PASS与API35 FAIL不改写、不借用。


## C36 历史：API21 完整矩阵通过，API35 完整矩阵整体失败

C36 实现提交 `97d38863a4353fbdde95eca3592a60e4a653887e` 仅修改 `app/src/androidTest/java/com/osfans/trime/InputFeedbackProbe.kt`，应用生产代码不变。正式候选目录为 `build/t07-privacy/stable-candidate36-final1`，时间戳 `1791460043820`（2026-10-08T11:47:23.820Z）。C36 全量 Gradle 已实际通过；582 项应用 JVM 与 9 项 build-logic 测试、Spotless、Lint 0、编译/build warning 行 0，458.793 秒。527 项源码构建前/后/当前一致且 source clean；C36 新来源/APK 身份、签名/资源/ABI/静态对齐已通过。API21 完整矩阵与独立复核通过，API35 首次 FD 快照 EACCES 整体失败；专项字节码及修正后新候选的全部验收仍待自身证据。以下 C35 及更早结果全部作为历史保留，包括 C35 真实 SAF 的通过结果，均不借用为 C36 验收。2026-10-08 11:46 UTC 重新获取上游时，固定上游仍为 `e9b21960`，fork 远端仍为 `a276c977`；本次尚未推送、打标签或发布，Latest 仍为 `v3.3.13-t9.9`。

| 当前 C36 项目 | 实际事实或待证据字段 |
| --- | --- |
| 已提交的测试探针修改 | `97d38863a4353fbdde95eca3592a60e4a653887e`；单个 AndroidTest 文件，生产代码未改 |
| 候选目录与构建时间戳 | `build/t07-privacy/stable-candidate36-final1`；`1791460043820`（2026-10-08T11:47:23.820Z） |
| 项目 Spotless | 通过；`/tmp/trime-c36-probe-project-spotless.log` SHA-256 `bd64fe92c02a34beba4b58fab05c480319279307aa4e6ac913c07ff5000778cb`；只证明格式检查，不是全量构建结论 |
| 探针独立源码/宿主复核 | 通过；`/tmp/trime-c36-feedback-fd-independent/final-peer-review.json` SHA-256 `bda04e12dfebecb9a946574ee58f045c7d1da64cccfb46c198332da4e3e64b3a`；实际私有 Kotlin 辅助函数的 26 项 owner 与 11 项独立检查，不计 Android 设备验收 |
| 正式完整构建与 JVM | 通过；458.793496 秒，Gradle BUILD SUCCESSFUL in 7m 37s；112 tasks（31 executed、81 up-to-date）；应用 JVM 582 项、build-logic 9 项 failures/errors/skipped 均为 0，Spotless 通过，Lint XML 0 issue，编译/build warning 行 0；build.log SHA-256 `d796a5568fcfe6d58166d2d761c18a331186454a8fbc20eea9d0af2f99c1420c` |
| 冻结源码及三份 APK 身份 | 通过；冻结 527 项源码在构建前/后/当前一致，source dirty=false；identity SHA-256 `a4d8e5f5c00322c0b06ba400ee0133034857b6253c9bd783e4e274870411d35e`；ARM64 APK `96cb2907026297dbefb910226bb2ffd1324fe11b59b3b19f25f2822be7012441`（22,442,573 字节）、x86_64 APK `9e756f37656f4b88e7f003d7726fe3a637ae762504dce2c46cd1ef5999afbdd2`（22,539,610 字节）、AndroidTest `223672c9e6c02b1f30430dcfe5caa64e2237915805f0cec338729cefcd58fdf8`（1,471,627 字节）；均为 C36 新冻结产物 |
| APK 签名与静态检查 | 通过；C36 report SHA-256 `6390ea082f6f132f062f54ba7294d5225aa2b4911c1458172a4c7e14f09c7e58`；三份新 APK 来源/版本/签名符合冻结身份，arm64-v8a/x86_64、48 项资源及 16 KiB ZIP/ELF 静态对齐通过。min21/target37、原 debug 证书/包保留；两份应用各有 2 条 META-INF ServiceLoader v1 提示，AndroidTest 0，不能称 warning_free。静态对齐不代表物理 16 KiB 页设备或 ARM64 设备运行通过 |
| API21 专项字节码检查 | 待 C36 三份 APK 的新检查：C36初版静态/重放存在（report ff15631c0493bebd3c32a592d8ed270bb6b93ac038d0ec1f161815c07f6a3184），旧helper有六项反事实缺口；加强helper只获TMP源码审阅（peer 7673883ac563e9478c664f40c8c191a20f0abd52cd7705ff5e5477c162ee6739），加强后的最终重放/证据闭包NOT RUN，未取得最终字节码验收 |
| 当前设备 QA 冻结与宿主复核 | 通过设备 QA 的 C36 字面值重绑定及 415 项宿主检查；host report SHA-256 `82cc63b25ed2f8bae285e292a5a4ed76eb1bfc9cfe5c6a96c5c2d304adaae52d`，独立 peer `42cb90e51dd56a60ca9bc8ae20c662273b2e1ae5bafcb67f27ea286cc8051601`；live coordinated receipt `/tmp/trime-c36-live-qa-freeze.json` SHA-256 `44a81b9ba007f67c97215db90b560e6314935e1d2ead10c7c41fce1e29a93ac1`，3 份依赖重绑定、11 QA 摘要冻结、527 源码未变。原守卫/动作/预算保持，不计设备门禁或发布裁决通过 |
| 发布校验与最终裁决工具绑定 | 待以 C36 来源、三份 APK 及全部新运行证据冻结发布工具闭包：C36仅九文件TMP来源/宿主68+23项PASS（owner b4a9b3febc3fa30224faea1f8f785584d9fc4e1ab9c2b196fb7c207e865a64fb；peer c3187bd5117fd1a6e94fd18facf1aa430e22bc55d81c5a275356878c137363fc）；API35 whole FAIL后最终运行/发布绑定NOT RUN，未采用为正式发布工具闭包 |
| API21 完整矩阵与独立复核 | 实际 whole PASS；8 探针、13 UI 路线、17 全屏生命周期、100 cycles、52 glyph/4 dot、降级/重试完整；独立 925 checks、449 运行文件不变、OFFLINE，仅限 C36 |
| API21 正式摘要 | `53e3ab98601fc8a7aec9d7b5a9fbe0481e51f83f0bad884d66d810ac7ad5dfe4`；独立 peer `9574ff5f4d22c91bc709c40c2f2111194a352e8b17d7cfb7c5fc9035cffd32da` |
| API35 完整矩阵与独立复核 | 实际 whole FAIL；feedback PID5442 首次 procfs FD snapshot Os.stat EACCES，具体槽号/目标未知；先前 UI+五探针只作局部，后续未跑；诊断复核 34 checks 通过不计矩阵通过 |
| API35 正式摘要 | `c5ed8cfcdc985f62c768367bf49da38387e872f743ee6348f6bbc619f6590135`；独立诊断 `1aec32fbaeca019586a4d4397c72e980493c7d530df796479f100e3ecdc7b618` |
| API21 真实 SAF 三阶段与归属复核 | 待 C36 自身新运行；C35 的通过只作历史：C36真实SAF roundtrip/restart/revoke NOT RUN；发布已因API35 whole FAIL停止，不借用C35或C37 |
| Direct Boot 压力门禁 | 待 C36 全十阶段、原 7 秒/1.25 秒/6500 ms/60 Hz 与真实一次 Delete：C36十阶段压力门禁NOT RUN；无自身真实Delete或完整资源裁决 |
| Direct Boot 独立自然 PIN 门禁 | 待 C36 新完整运行：C36独立自然PIN门禁NOT RUN；无自身完整无救援运行 |
| 三轮软件冷启动 | C36三轮独立软件冷启动门禁NOT RUN；矩阵中的设置冷/热检查不代替该门禁 |
| 冷启动摘要 | C36没有三轮冷启动正式摘要（NOT RUN） |
| 最终运行日志裁决 | C36最终跨门禁运行裁决NOT RUN；整体运行验收FAIL，保留API21 PASS与API35 whole FAIL各自范围 |
| 裁决摘要 | C36没有最终运行裁决摘要；API21独立review与API35失败诊断仍按原始摘要保留 |
| 运行 warning 计数与分类 | C36无最终跨门禁warning分类验收；API21原始3205条W/E/F与API35失败日志保留，不能宣称warning_free或清零 |
| 全部运行验收 | C36整体运行验收FAIL：API21 whole PASS、API35 whole FAIL，其余未运行门禁不借用旧/新候选；稳定发布已停止 |
| GitHub 推送、稳定版标签与发布 | 尚未执行；全部必要门禁通过后才发布两个应用 APK 和一个递归源码包：C36 UNPUBLISHED；未推送、未打稳定标签、未建立release，未形成正式递归源码包/资产验收 |

C36 完整构建和 APK 静态检查通过，专项字节码仍待最终证据，API35 设备矩阵已整体失败，不能宣称所有 bug 或警告已消除，也不能发布为稳定版。C36 新 APK 实际签名检查确认两份应用各有两项 META-INF v1 服务资源提示、AndroidTest 为 0；C35 的同类提示作为历史保留，必要资源不因消除提示而删除。现有 debug 包 `com.osfans.trime.debug` 与证书保留，未切换生产签名或 R8。Android 5.0+ 支持要求保留；ARM64 尚无真机或原生 ARM 镜像验收。历史 C35 单次非注入 5,434 ms 普通完整部署排队、C31 6,280 ms SAF 排队及 root 系统 `input.Input` 原生 SIGSEGV 保留，均不伪称在 C36 已复现、修复或清零。

API35 失败后当前 C36 发布停止；上表其余门禁/最终工具和裁决仍 pending。后续 C37 提交与新产物身份尚未提供；修正后必须重建并重跑全部门禁，C36 的真实 API21 通过仍仅属于 C36。

## C36 实际 API21 final1：完整矩阵与独立只读复核通过

`build/t07-privacy/runtime/t910-candidate36-api21-final1/summary.json` SHA-256 `53e3ab98601fc8a7aec9d7b5a9fbe0481e51f83f0bad884d66d810ac7ad5dfe4`，运行 2026-10-08T11:56:41.066998Z 至 12:08:54.815625Z，实际整体 PASS，模拟器停止并确认离线。独立复核 `/tmp/trime-c36-api21-matrix-independent-review-final3/peer-review.json` SHA-256 `9574ff5f4d22c91bc709c40c2f2111194a352e8b17d7cfb7c5fc9035cffd32da` 完成 925 项核对：当前 527 源码、三份 C36 APK 全字节、live 11 QA 摘要、安装身份及 raw 证据绑定；449 个运行文件前后摘要保持不变。

实际完成 privacy、editors、engine、startupFailure、feedback、shutdown、setup、clipSave 八探针；13 条真实 UI 路线与旋转/杀进程恢复，17 个全屏生命周期检查点，100 次显示/隐藏，四组字体/状态共 52 个 glyph 与四次真实点号提交；真实不可用输入界面、数字输入 `STABLE2`、一次 Delete 回到 `STABLE`、恢复资源与重试亦完成。API21 不支持分屏，本次不宣称分屏或 ARM64 设备验收；设置探针独立完成两项冷/热 ACTION_RUN 检查，13 路线的 UI 图不冒充 shell Parcelable/PendingIntent 直接触发。

新 FD 检查在 API21 实际校准：反馈 PID4934，同一 fixture 的句柄 0→1→2→1→0，两个打开槽位 28/32 具有相同 `(st_dev=19, st_ino=139744251036448)`；9 个无关句柄均识别且声音 fixture 为零。原 10 批×20 次共 200 次同步读取，每批声音 fixture 句柄为零；全进程 FD before=32、after=33、delta=1 只作诊断。旧全进程 +8 反事实使用同一快照 32→41，不能当作异步全局稳定性证明。原 30 秒预算及完整隐私/TTS/SoundPool/短旋律/播放/释放/清理检查实际完成；C35 失败的 FD 来源仍未知，不因本次通过而改写。

98 份可用 raw 文本日志按 PID 核对，未发现 fatal/ANR 命中；保留去重 3,205 条 W/E/F 原始行（应用 PID 526、其他 PID 2,679，行数不是事件次数），`warning_free=false`。其中反馈 PID4934 的 105 条 `AudioTrack: AUDIO_OUTPUT_FLAG_FAST denied by client` 提示、受控主题/启动失败、两项删行保存负例、inactive InputConnection、SQLite WAL、ART、EGL/WebView/系统警告均保留各自归属与范围。API21 `lastanr` 命令不支持的输出未删；已捕获的 DropBox 无 crash/ANR 条目和可用完整日志提供本次审阅范围，不宣称所有日志或警告清零。

## C36 实际 API35 hostgpu final1：首次 FD 快照 EACCES，完整矩阵整体失败

`build/t07-privacy/runtime/t910-candidate36-api35-hostgpu-final1/summary.json` SHA-256 `c5ed8cfcdc985f62c768367bf49da38387e872f743ee6348f6bbc619f6590135`，运行 2026-10-08T12:08:55.045781Z 至 12:17:51.135515Z，整体 FAIL；停止并确认离线。独立只读诊断 `/tmp/trime-c36-api35-fd-failure-independent-review-final2/peer-review.json` SHA-256 `1aec32fbaeca019586a4d4397c72e980493c7d530df796479f100e3ecdc7b618` 通过 34 项诊断核对，454 份失败运行文件前后不变；诊断通过不等于矩阵或反馈通过。

真实持有剪贴板 UI 及 privacy、editors（含 100 次显示/隐藏）、setup、engine、startupFailure 五探针实际通过，但仅为局部证据。反馈 PID5442 在 `InputFeedbackProbe.kt:65` 对 `/proc/self/fd` 槽位执行 `Os.stat(slot.path)` 时收到 EACCES；调用来自第 191 行 `before-controls`，第一个快照尚未完成，FD audit 为空。具体数字槽位、异常目标和全进程 before/after 均未保存；fixture 正控制、9 个无关句柄、200 次扫描以及后续 SoundPool/旋律/播放/释放断言尚未到达。shutdown、clipSave、saf、clip、t02、导航、生命周期及 unavailable 阶段未执行。

原 raw 日志同时记录 PID5442 对 `/dev/pmsg0` 的 SELinux `getattr` 拒绝（`tcontext=pmsg_device`、`permissive=0`）。这是相关原始权限拒绝证据；没有保存的槽号或逐调用映射，不能认定它就是抛出异常的具体 FD，也不能推断 socket、zygote 或继承来源。分类保留为 `ERRNO_EACCES_PROC_STAT_SLOT_UNKNOWN`；instrumentation 捕获该失败，未发现可用 raw 日志中的 Trime fatal/ANR，但平台/系统警告继续保留。需要修正测试资源验收方法，不能盲重试、跳过 EACCES 或把失败改成通过。后续修正候选（预期 C37，提交/构建身份尚待提供）必须重新冻结并完成全部新构建、静态与设备门禁；不能借用 C36 API21 的通过。

## C36 发布工具 /tmp 源码与宿主独立复核（不计发布验收）

`/tmp/trime-c36-release-retarget-preparation/release-retarget-final-host-review.json` SHA-256 `b4a9b3febc3fa30224faea1f8f785584d9fc4e1ab9c2b196fb7c207e865a64fb` 的 68 项裁决宿主与 23 项发布宿主（97.120 秒）实际通过。独立源码/收据 peer `/tmp/trime-c36-release-tools-independent-peer-final3/peer-review.json` SHA-256 `c3187bd5117fd1a6e94fd18facf1aa430e22bc55d81c5a275356878c137363fc` 完成 722 项核对：七份 Python 全文只改变声明的候选/路径/摘要字面值，逆归一 AST 与原版完全相等，原测试方法和 selftest 字节保留；压力 15、自然 17、剪贴板 10 项物理闭包、live 11 QA、527 源码、11 必需输入和六文档 allowlist 均核对。初次 23 项因 /tmp 缺 fixture 输入挂载而 setup FAIL 的原日志保留；只补 /tmp 目录符号链，未改测试源或重跑本次独立 peer 的慢测。

该 peer 只证明 /tmp 工具源码和冻结宿主收据。最终发布工具的 live 协调应用、依赖闭包及全部实际运行裁决仍需新证据，专项字节码亦等待最终独立 peer。原 7 秒、1.25 秒、6500 ms/60 Hz、raw PID/START/时序/UID/POST 和所有严格守卫不变；API35 整体失败使当前 C36 不具备稳定发布条件。

## C35 历史：API21 final2 全进程 FD 断言失败，来源未证实

当前 QA phase4 的 C35 Android 5.0 正式 final2 矩阵已整体 FAIL；`build/t07-privacy/runtime/t910-candidate35-api21-final2/summary.json` SHA-256 `d118b29aaeafbf0a9dee7be0b5c757514cce8f084fa5721451079b09f291f3ac`，运行 2026-10-08T11:18:51.417077Z 至 11:24:19.061856Z。`InputFeedbackProbe.kt:103` 在原 200 次同步声音扫描后触发“Sound descriptor handles accumulated”：全进程 FD 总数超过 before+8。该失败运行未记录实际 before/after 数值或 FD 目标，因此不能补造精确增量、归属或来源。反馈探针 PID 为 4929；这是 instrumentation 捕获的断言失败，不是 Trime 进程崩溃。

privacy、editors、engine 与 startupFailure 探针完成；SoundPool 加载、短旋律、播放及释放断言位于失败点后，未执行。后续 setup、clipSave、导航、生命周期及 unavailable 未完成；顺序安排的 API35 正式 final2 未启动。独立诊断 `build/t07-privacy/reviews/candidate35-api21-final2-feedback-fd-diagnosis.json` SHA-256 `7de3daf9cee677107300e43d494c644af62085b2ce1aa77f9f32e78b9ef39116` 完成 592 项诊断核对并保留原 115 个运行文件；诊断复核通过不等于矩阵或反馈通过。模拟器已停止并确认离线。

源码检查发现 `SoundEffectManager` 的 `bufferedReader().use { it.readText() }` 在 YAML 解码前已关闭读取器，未找到可证实的声音读取器泄漏。该 PID 的异步 WorkManager 活动可影响全进程资源计数，但只是潜在混杂因素，尚未证明是此次 FD 增长来源。故历史分类保留为 `UNKNOWN_FD_ORIGIN`；不把失败改称已证实假阳性，也不宣称 C36 修复了已定位的生产泄漏。

## C36 已提交的测试探针：按声音 fixture 的每个 FD 槽位验收

唯一修改位于 AndroidTest 探针。API21 可用的 `Os.stat` 为创建的两份声音 fixture 记录 `(st_dev, st_ino)`；对非空 `/proc/self/fd` 快照逐槽位执行 `Os.stat(/proc/self/fd/N)`，使用 magiclink 对应的真实打开对象身份，不能按路径或 inode 去重。`Os.readlink` 仅补充原始诊断标签，不决定归属；stat 已命中的 fixture 即使随后 readlink 遭遇 ENOENT 也不丢弃。仅跳过路径扫描的 ENOENT 关闭竞态；EBADF、EACCES、EIO 等其他错误均失败。身份比较限于同一 VFS 设备/inode，不宣称任意 `/data/media` 与 emulated-FUSE 视图身份相同；新 Android 运行必须由同一 fixture 的实测控制验证映射。

正控制用同一 fixture 打开一次、再打开第二次，要求 1→2→1→0 个槽位；这会拒绝把相同 inode 的两个 FD 合成一个的实现。另用 9 个有界无关文件句柄证明声音 fixture 计数仍为零，并记录该快照中旧全进程 +8 规则会拒绝的反事实；无关句柄由嵌套 `use/finally` 关闭，不注入损坏 YAML 或新解码警告。原 200 次同步 `getAll` 扫描保持为 10 批×20 次，每批要求声音 fixture 句柄为零；全进程 before/after 及 delta 仅作诊断。无 GC、额外等待、重试或容差放宽。

原隐私、TTS、SoundPool、短旋律、播放、释放、首选项/目录清理及 30 秒总预算保持。冻结提案 SHA-256 `1fafa58035d73d009f7914c994a6a6a7ab38f329b6c4f3755e4a5f70b5d11de9`；独立源码范围证明可移除允许新增的 FD 检查部分后逐字节恢复原文件。独立复核重复 Kotlin 编译并核对实际辅助函数的 26+11 项宿主检查；宿主使用 Linux procfs 与 Android Os facade，不能替代 Android 的 fixture 控制及完整设备验收。C36 API21 实际控制与完整矩阵已通过；API35 在 before-controls 第一次快照 EACCES 失败，后段控制未完成。项目 Spotless、C36 全量构建及 APK 静态检查通过只作为各自范围证据；专项字节码及后续修正候选全部新验收仍待填入。

下文保留 C35 及更早各阶段的实现、构建、局部通过与失败历史；其中旧时“当前”/“待重跑”状态以本节记录的 C35 final2 整体失败和 C36 API21 整体通过、API35 整体失败及其余门禁 pending 状态为准。

## candidate35 历史：构建与静态检查通过，设备验收未完成

C35 修复普通进程解锁恢复时 WorkManager 双重初始化的竞态：Application 实现官方 `Configuration.Provider`，配置由稳定的 lazy 实例提供，调用统一走 WorkManager 2.10.5 的 `getInstance(Context)`。Provider getter 与 worker 可用性检查在 API21 先绕过 UserManager/设备保护 API，并同时拒绝实际设备保护 Context、仍处于旧 DE 模式或正在替换的进程；旧 DE 进程解锁后也不能初始化 WorkManager。原周期任务的排队与 IO 行为保持。后台解锁接收、生命周期/Atomic 一次替换和持续隐私保护继续保留。

C35 实现提交为 `dc69d73baa0c06e5a103000d1cd13f106f280afd`，冻结 527 项源码，构建时间戳 `1791449483471`（2026-10-08T08:51:23.471Z）。正式候选路径为 `build/t07-privacy/stable-candidate35-final1`；此前 `stable-candidate35` 的中断构建没有形成 identity/APK 信任链，既不能算通过，也不作为编译失败。正式完整构建、APK 静态和专项 API21 字节码检查已实际通过；真实 SAF 三阶段及其归属审阅在当前 QA epoch3 复核后保留通过。旧 QA 的 API21 完整矩阵与独立审阅是真实历史通过；因 Matrix/绑定 QA 已变，当前 API21 矩阵必须全新重跑。API35、冷启动、压力/自然 PIN 与最终裁决仍待新证据。C31–C34 的结果仅作历史，不能作为 C35 验收。原十阶段、PIN 提交起算的精确 7 秒、1.25 秒单命令及 6500 ms/60 Hz 触摸要求均保留。

| C35 历史项目 | 实际事实或待确认字段 |
| --- | --- |
| 实现提交 SHA | `dc69d73baa0c06e5a103000d1cd13f106f280afd` |
| 构建时间戳 | `1791449483471`（2026-10-08T08:51:23.471Z） |
| 完整构建 | 通过；519.402524 秒，Gradle BUILD SUCCESSFUL in 8m 37s；112 tasks（35 executed、77 up-to-date），Spotless 通过，Lint XML 0 issue，编译/build warning 行 0；build.log SHA-256 `578574605eb0674484ff126b32dccf45cc833fbd143b5fd5850a2d77fe5c211c` |
| JVM 计数 | 应用 582 tests；build-logic 9 tests；两组 failures/errors/skipped 均为 0 |
| 冻结 APK/来源身份 | `build/t07-privacy/stable-candidate35-final1/identity.json` SHA-256 `638a5786ee49f7c8d4b9c531e8e7ecda23e43dd58042e822da3c8f59c6bfb58d`；ARM64 `b2fe19a0f81e2a1f5f3edbdaad536721dd283f6930f5e744c0ba774a40f3ba03`（22,442,508 字节）、x86_64 `de4ef390bf598d7243342db1a3fd047d7d41a0f9d463727ac4913758a7c186a7`（22,539,545 字节）、AndroidTest `d9f469cbd02f49c29d3b62c37636b834b988cca515d502cebce86082bfee5c7b`（1,464,301 字节）；构建前后 527 项源码不变，source dirty=false |
| APK 静态检查与签名警告 | 通过；冻结签名、包/版本、ABI、48 项资源及 16 KiB ZIP/ELF 静态对齐符合候选身份；report SHA-256 `94fc19c8b9954f4873f6971555dd6d694c241228ea3db69aeaf5d1071cfaad11`；两份应用各保留 2 条 META-INF ServiceLoader v1 提示，test 0，warning_free=false；debug 证书/包与 min21/target37 保留，静态通过不等于设备验收 |
| API21 专项字节码检查 | 通过；冻结 C35 三份 APK/527 源码复核，应用 19 与 test 6 个 DEX 全为 035，ARM64/x86_64 应用 DEX 摘要图相同；实际 DEX 核对 Provider/worker 的版本与旧 DE/替换状态守卫，以及官方 WorkManager 2.10.5 的有界初始化路径；report SHA-256 `262b44ff1eb4af461886254d664e874afa4acb205a7c65ca199bc14c35bfc205`。35 项已审项目高 API 引用和 5 项选定库引用分别记录；全项目 112 项发现值未归零，不代表全库或设备通过 |
| API21 完整矩阵 | phase4 final2 整体 FAIL：声音探针全进程 FD 增量断言失败，来源未证实；旧 final1 的 8 探针/13 导航/17 生命周期/100 次显示隐藏/unavailable 与独立审阅通过保留为历史，不计 C36验收 |
| API21 正式摘要 | final2 失败摘要 `d118b29aaeafbf0a9dee7be0b5c757514cce8f084fa5721451079b09f291f3ac`；诊断 `7de3daf9cee677107300e43d494c644af62085b2ce1aa77f9f32e78b9ef39116`。旧 QA 历史摘要 `c69c53f764ca94d1264a2649b7a44469e3ff1d85c00cbdf3b7a455cff81c20b6` 与 review `8f74238aa64a710d4223067529436b27b25b9882da90b10c2b6424ef3d2297c6` 保留 |
| API35 完整矩阵 | 首次 hostgpu final1 整体失败；自然持有剪贴板 UI 与前 9 项探针通过，但 ClipEditorProbe 未确认稳定 Trime 键盘，45 秒固定绑定准备亦失败；t02、导航、生命周期和 unavailable 未执行。原结果不能代替完整矩阵；新正式验收待证据：C35 final2 因前序 API21 整体失败而未启动；无后续完整通过（历史） |
| API35 正式摘要 | 首次失败 `build/t07-privacy/runtime/t910-candidate35-api35-hostgpu-final1/summary.json` SHA-256 `f549102f48e7fd1efbfe3d3f2ade1688747e475745b0c1c5db62d4e8a180b45a`；2026-10-08T10:14:52.086271Z 至 10:26:02.052812Z；后续完整矩阵正式摘要待提供：C35 final2 未启动，没有新的正式摘要（历史） |
| 三轮软件冷启动 | C35 未完成三轮正式冷启动验收（历史） |
| 冷启动摘要 | C35 无新的正式冷启动摘要（历史） |
| API21 真实 SAF 三阶段 | 通过；API21 真实可见 DocumentsUI 的 24 项 UI 动作创建并选择全新独立目录；roundtrip、进程重启后的授权/数据恢复、撤销授权后的安全降级三阶段均通过；完整 SAF 独立审阅按实际进程归属通过（122 检查、44 gate + 48 setup 文件）；全运行最终裁决仍待其他门禁 |
| 真实 SAF 摘要 | `build/t07-privacy/runtime/t910-candidate35-api21-real-saf-final2/summary.json`，SHA-256 `f741115e901d3d50e4e45281b5bb191a199a75c72f2206b0c055426b5690f9e6`；roundtrip report `5c7cbc2d21cc46602afbf45c9b4194862d2a61d12de4ba72b291b0273b9347ca`、restart `32a6b1f60ac93f6ca7d20a21ceca774bff760547dc59618f8591af83a154f3fd`、revoke `d6ae39e5a4e6ea38263aec9aeb68caa45c7611a21967ba053ef800026d0d0243`；UI result `/tmp/trime-candidate35-api21-saf-ui-final2.stdout.json` SHA-256 `95789ba1aa192012265a757f85064f519de3f4966dd315a4706072d46a126c1c` |
| Direct Boot 压力门禁 | final1 第十阶段失败（summary `54aa1717f024ef12c41465975fdb5baff614f50223bc36f037d3b6fb5e651ce8`）；final2 第九阶段 mutation-3 超过 1.25 秒失败（`242b47bf281dad2fd90f1ce464219ec38f9da96109aa451027416b7206f0fc6e`）；手势单项不能抵销整体失败；当前 phase4 新完整验收待证据：C35 后续完整压力门禁未完成，final1/final2 整体失败保留（历史） |
| Direct Boot 独立自然 PIN 门禁 | C35 完整自然 PIN 门禁未完成（历史） |
| 最终日志裁决 | C35 无最终发布通过裁决，API21 final2 整体 FAIL（历史） |
| 裁决摘要 | C35 最终发布裁决未完成（历史） |
| 运行 warning 计数与分类 | C35 未形成全部正式运行的新 warning 汇总；局部计数不代替整体审阅（历史） |
| 全部运行验收 | C35 整体运行验收未完成，API21 final2 整体 FAIL（历史） |

C35 构建、APK 静态与专项字节码检查通过，设备验收及最终裁决尚未完成，不能发布稳定版。应用 APK 各保留 2 条 META-INF 提示，完整运行 warning 待实际新日志审阅，不能称所有警告或风险已消除。ARM64 仅完成构建及静态检查，未在 ARM64 真机或原生 ARM 镜像运行；保留现有 debug 包与证书。当前未推送或发布，Latest 仍为 `v3.3.13-t9.9`。

## C35 历史：QA phase4 未来焦点标记解析修正（新设备结果待完成）

phase4 只修改 ignored QA 绑定/普通输入解析器及对应宿主测试与依赖摘要，已完成无设备在线时的协调应用；应用实现 `dc69d73baa0c06e5a103000d1cd13f106f280afd`、527 项源码、`stable-candidate35-final1` 的 582+9 JVM、构建身份、三份冻结 APK、静态与专项 API21 字节码事实保持不变。此前真实 SAF final2 三阶段与按实际进程归属的独立审阅保留通过；当前最终发布校验仍须绑定协调完成后的 phase4 工具。API21/API35 完整矩阵、压力/自然 PIN、三轮冷启动及最终裁决仍待全新正式设备证据，不能用工具宿主通过替代。

epoch3 完整窗口短诊断仍为整体 FAIL：`build/t07-privacy/runtime/diagnostic-candidate35-api35-full-window-clip-setup-epoch3/clip/binding/report.json` SHA-256 `7115185f548d5eaa6103180978acbb17658ffcc8ad37f0b3427bd0c21186ee24`，2026-10-08T10:46:07.948119Z 至 10:47:15.604697Z，原 45 秒 Latin setup 条件未完成。只读独立重放 `/tmp/trime-c35-clip-future-focus-independent-raw-peer.json` SHA-256 `c08456143989aa823392a4d5b421e2a790c35a9987fa857b6e6ae139d9ddfc15` 确认 75 份 Latin 帧中的首个过渡帧仍不满足当前可见绑定条件；另 74 份帧当前连接、方法、焦点、服务与键盘显示均有效，但未来焦点标记为 true。新旧工具重放只揭示旧解析器 false-only 条件的问题，不把原短诊断改写为运行通过；它也不是完整矩阵或 PID 交接验收。

Android 15 官方 InputMethodManager 源码定义 `mRestartOnNextWindowFocus` 为下一次窗口获得焦点时需要重启输入的标记，而不是当前 InputConnection 已失效；源码 SHA-256 `d875c29f44e9d6fd16913ca659220b6eb06aaaf80034f812cb08413cacd91231`，定义/消费见本地固定副本第 505/863 行。phase4 严格解析唯一的 true|false 标记，并继续核对当前 mActive=true、PID/绑定序列、方法、live IC、EditorInfo、焦点及窗口/服务可见性。未来标记原值保留在不可变 IMS 原始证据中，不作为当前稳定身份或前后相等条件。此前剪贴板 /tmp 方案的源码/宿主 peer `/tmp/trime-c35-clip-future-focus-independent-final-peer.json` SHA-256 `b3c7e872cc9447c1110efd7b8668bb56f011a2e83b075ca4a8099bb2b02c8ad1` 曾通过 39 项宿主与 25 项实际帧变异拒绝检查，旧 false-only 工具反事实仍拒绝全部 74 份 true 帧；随后独立复核发现更广的重复字段语法缺口，守卫修正后的最终运行工具已重新冻结，摘要见下文。此前边界检查只作准确历史，不能直接作为最终工具或设备通过。

普通输入初次 /tmp 提案通过宿主后，独立复核仍发现非空值正则漏计空值或等号后空白重复字段；第二版计入空值，但另一独立复核又发现等号前空白或裸字段名重复仍会漏计。两次待审提案都未应用到 live QA，原文件、报告与反例保留，宿主初次通过不等于最终独立验收。第三版按精确字段名 token 计数，要求全客户端区段只出现一次，主状态行仍只允许精确 true|false；空值、等号前后空白、tab 或裸字段名重复均拒绝。最终 /tmp 报告 `/tmp/trime-c35-normal-input-futureflag-final3/report.json` SHA-256 `a80f636715b566eea650112668c39e5cb2933ab0555782a998a1252428ed0f11`：139 项内置、98 项压力（90 旧项+8 新项）和 56 项自然（48 旧项+8 新项）宿主检查共 293 项通过；12 项新旧反事实重放明确证明两版旧提案误接受、第三版拒绝两组畸形重复。AST 核对只改变两套 normal_input_connection，所有前后守卫与六字段绑定返回未变，实际 527 项应用源码重新逐项核验未变。随后独立 normal/real 输入 peer `/tmp/trime-c35-future-focus-all-guards-independent-final3/peer-review.json` SHA-256 `96f8c0c93dda5ab4d24cf7b6c41f9a6c4a0afe76b5c749ea41af32da632da088` 通过：五份完整实际上下文与 188 项变异拒绝核验通过，精确字段名唯一性、现有守卫/身份 AST、原测试方法及实际 527 源码摘要均复核；这仍是工具边界通过，正式设备门禁待新运行。原十阶段、一次 focus/一次 Delete、7 秒/1.25 秒/6500 ms/60 Hz、只读资源审计与无救援约束保持。

phase4 的当前运行工具已在无在线设备时完成守卫协调应用，冻结 manifest `/tmp/trime-c35-future-focus-qa-epoch4.json` SHA-256 `25839f5b9251e459ee8180c0c4d83cdde8e1cea892247197c2c53056095ac0f6`，2026-10-08T11:14:07.317028Z；应用 527 项源码未变，旧九项工具完整副本保存在 `/tmp/trime-c35-qa-before-future-focus-epoch4`。协调后的 Natural 只额外重绑新 parent/Matrix 哈希，应用、动作及期限不变。

| phase4 当前运行工具/测试 | SHA-256 |
| --- | --- |
| `run_api35_clip_binding.py` | `59bcb0e7bc1d53f78a5506c7df1126788ae7cb2cd22014ea5f032633ede9e92b` |
| `test_api35_clip_binding.py` | `77d92805b296d3d211cceaa8ecd456fab3c797079efeac345381cd5043d6cd56` |
| `run_avd_matrix_t9_10.py` | `20807735cc8a1d8c1253acff8f8d5971115d559f875638cf2117cc0a75bcb1c2` |
| `run_api35_direct_boot_gate.py` | `d4136fe4e8134d8cc2fa6aa3f4e5e1802a57c04a20bd48a71ace66bb4f95263a` |
| `test_api35_direct_boot_gate.py` | `98687089700454978098870c519bee88386b71a115c441d7afb43b9255d58db1` |
| `run_api35_direct_boot_natural_pin.py` | `528ffad8c1d86f8054abfc11e4af5d283c26ac69f2b4fff8df8068308f79fd31` |
| `test_api35_direct_boot_natural_pin.py` | `b263208e4c5aabf85fdec8c79d43d0c07e7c3eaebf31f72a1e480a84cff48beb` |
| `run_api35_real_owned_clipboard_ui.py` | `aa511c7f9d093caff555509f822cce76dbc1a0e4adb968143910da7edc210e4a` |
| `test_api35_real_owned_clipboard_ui.py` | `b188caf21de046d89b3546a7f053e90e8a1a95cd4bf604f431f86420631ac3e7` |

随后精确 live 文件的 415 项宿主检查全部通过：Clip 41、Matrix 19、压力独立 98、自然 PIN 56、real-owned UI 62、压力内置 139；`/tmp/trime-c35-epoch4-live-host-final.json` SHA-256 `b1e4ce67708aa4cd40a6aa9efb63cfe2a9a90ea4967fb36f28b81ab634690443`。此处 live 指已应用的宿主工具文件，不指设备运行通过；报告明确 device_acceptance=false。正式 API21/API35 矩阵、压力/自然 PIN、三轮冷启动与最终日志裁决仍待全新设备证据。随后全新短诊断 `build/t07-privacy/runtime/diagnostic-candidate35-api35-full-window-clip-setup-epoch4/diagnostic-summary.json` SHA-256 `6a42e53ac97a079bd824f5af7516b429a5562cccd9f7dd5c0bc4b91f035be98b`，2026-10-08T11:14:57.249367Z 至 11:17:48.778294Z，whole diagnostic passed=true。固定绑定 report SHA-256 `de9cf4f105b121aec9a827caba71427120eed558d3688eec83a936ad0adff0d7`，probe PID 3251、mode=fixed_binding，只执行一次 Latin 选择再一次 fresh Trime 选择；首个过渡帧仍被当前守卫拒绝，4 份稳定 Latin 帧和原探针完成前的新 Trime 帧实际带 future-focus=true，原完整探针与所有断言通过。Trime 归属 fatal 与该转换的项目诊断检查为空；不据此声称完整设备日志或全部 warning 为零。stopped.json 与 summary 均确认离线，独立运行证据复核仍待完成。这是全新的短诊断通过，绝不回填 epoch3 的失败，也不替代完整 API21/API35 矩阵、Direct Boot 压力/自然 PIN、冷启动或最终运行裁决。新的 API21 final2→API35 host final2 顺序矩阵已由 root 启动（2026-10-08T11:18:51Z），当前没有它们的最终通过结论。

phase4 最终 ignored 发布/裁决工具已冻结并通过源码/宿主复核：`/tmp/trime-c35-epoch4-release-adjudication-final-tool-qa.json` SHA-256 `83b42cb2b19da5d55545918a21ef84c4baa1704dcef8b8777a36abdf21fc6a7b`。68 项裁决器检查、23 项发布工具测试通过（后者 77.136 秒），语法通过，慢宿主检查进程已停止。当前裁决器 `57038b05c1e4d2571cbacb3d13247b90612fe7eea16e70db4a9894dfe120515d`、发布校验器 `caa779d564ca8ada8e5b4ba023f06a74f50bfcde75fc94b299722321e2f8c22c`、发布自测 `52b194df8174cb04828911e11a5081eae5993dfb66b4452f621e8fcbd0b29439`；11 项必需运行输入按实际 CLI 推导，527 应用源码、15 pressure/17 Natural 依赖、3 APK 与 9 项当前 QA 摘要闭合，公开文档只允许固定六份，附件只允许两份 APK 与一个递归源码包。报告明确 runtime_acceptance=false、release_acceptance=false，工具通过不能替代正式设备结果或最终裁决。当前运行结果占位继续保留；C35 整体运行验收未完成，API21 final2 整体 FAIL（历史） 仍未完成。不得填入最终 warning 总数，也不能称全部 bug、warning 或物理 ARM64 已验证。每份应用 APK 的 2 条 META-INF v1 提示、debug 包/签名、C35 本次普通完整部署的一次 5,434 ms 排队、按 PID 归属的 root 系统 input.Input native SIGSEGV，以及 C31 历史 6,280 ms 排队限制继续保留。两份 APK 加递归源码包的公开资产边界不变；当前未作 GitHub 推送或稳定发布。

## C35 QA epoch3 历史状态与冻结交接（不计当前 phase4 设备验收）

当时采用仅修改 ignored 验收工具的 QA epoch3，应用实现 `dc69d73baa0c06e5a103000d1cd13f106f280afd`、527 项源码、582+9 JVM、冻结 APK、构建/静态/字节码事实未变。当前真实 SAF final2 的三阶段与进程归属审阅保留通过，且已由 epoch3 发布校验器复核原证据闭合。旧 QA 的 API21 final1 是真实通过，但 Matrix/窗口绑定验收工具已变，必须用当前 epoch3 全新重跑；它只作历史，不能计入当前矩阵。API35 完整矩阵、Direct Boot 压力与自然 PIN、三轮冷启动及最终裁决均待通过，当前没有可发布结论。

QA epoch3 的七项协调重绑前副本在 `/tmp/trime-c35-qa-before-full-window-retarget`；manifest `/tmp/trime-c35-full-window-qa-epoch3.json` SHA-256 `c47b56b8179d34dbec468be2f07c1fccac6ca183d40959e36435c872ef0c9e9e`。当前 Clip wrapper `839c08ebf88fe7eefb7151bc61f380409da18f4d54d8cbc4483f0a407b1ad771`、clip test `56b9eba959ddab2eb1aa526c0799fd4bd96573f52a36972b274b51549e003016`、Matrix `ccfda5e56e2562b3c96ad701dc8681f194cafd39aa9872b4459595006268f8fa`；压力 parent `c403bdde113fc39a7950d15fd4a70a319964001f5c6b20dd66bcafded89520cb` 与 test `6ce162d3d5feed45660b0cadc22302388f769a099e4882aacdc9fc1e6b7e4be2` 不变；自然 wrapper `a595e45c8b145d76ea717e6d83a856f6de728dc43600039462347a403761f6cb`、test `e877fbd34ae9606d3a9f889edd5fad2868c22d9d0e0f944fb00a219029a76db5`。34 项 clip（5.638 秒）、19 项 Matrix（4.572 秒）、48 项自然（14.436 秒）宿主检查通过；独立 clip source/negative peer SHA-256 `88de07cef236d30698a1ed9725d2f0195fc4b6c8b59210b7375a28444e0ea4bd`。这些仅证明工具契约，不能代替设备窗口/绑定验收。

当前发布/裁决工具 QA `/tmp/trime-c35-epoch3-release-adjudication-final-tool-qa.json` SHA-256 `def17dd374044f5abd5bc8e92bf4c197ee2d9c898e3becaf1f95294cf5713845`：65 项裁决器、22 项发布工具和 5 项独立重封证据负例通过，3 项 parser baseline 与语法亦通过；tool_qa_passed=true、release_acceptance=false。旧失败不能靠重新封装原始证据变绿；完整当前 API35 matrix/pressure/natural 尚无通过。真实 SAF 保留核验 `/tmp/trime-c35-epoch3-retained-api21-saf-release-verifier.json` SHA-256 `67db13c8cbc6e0827ce399fa16b1a0b2f7612206780f45bac74a121a3195ce4a` passed=true，绑定未改的 SAF summary `f741115e901d3d50e4e45281b5bb191a199a75c72f2206b0c055426b5690f9e6`；无需仅为此次 QA 重绑重跑真实 SAF。

C35 压力 final2 `build/t07-privacy/runtime/t910-candidate35-api35-direct-boot-hostgpu-final2/summary.json` SHA-256 `242b47bf281dad2fd90f1ce464219ec38f9da96109aa451027416b7206f0fc6e`，2026-10-08T10:26:55.166730Z 至 10:32:09.434086Z，整体失败。前八阶段通过，第九阶段 mutation-3（ime.setTrime）超过原 1.25 秒单命令上限，第十阶段未运行；用户解锁首见 1.689581469 秒，Latin set 从 1.689638703 至 2.789661210 秒，后续 Trime 命令实际 1.253400044 秒（3.281374790 至 4.534774834），超过 1.25 而不是完整交接通过。6503 ms 手势和 preSTART 零事件单项通过，不能抵销交接失败。PIN 清除、UID 2000 和离线均确认，原失败保留。原日志观测 IMMS 锁等待与超时相吻合，但锁持有者根因未证实；宿主套件约 50 秒前已结束，不能自动归咎平台或宿主竞争，也不能放宽期限后重试记通过。

epoch3 短诊断 `build/t07-privacy/runtime/diagnostic-candidate35-api35-full-window-clip-setup-epoch3/clip/binding/report.json` SHA-256 `7115185f548d5eaa6103180978acbb17658ffcc8ad37f0b3427bd0c21186ee24`，2026-10-08T10:46:07.948119Z 至 10:47:15.604697Z，整体失败且不是正式矩阵/PID 交接验收。原始完整 WindowManager 窗口数据在初始第三份快照确认了唯一焦点；随后仅一次 Latin 选择，75 份 Latin 帧未满足现有未来焦点 flag 谓词，原 45 秒 setup 截止到期。已完成 setup 命令最大 0.398 秒，不能描述成 3 秒窗口读取卡住。该 flag 的实际语义仍由只读诊断核对，当前没有放松 guard 或添加救援。stopped.json 已确认设备离线；短诊断与宿主拼接 fixture 不能替代正式设备验收。

补充只读诊断指出旧剪贴板绑定分支的 111 份焦点截断窗口片段缺少全局焦点信息。C31 原先通过路径也有同类早期读取错误，但它没有进入此固定 companion 绑定分支，不能宣称 C31 已覆盖这条分支。epoch3 改用完整窗口读取并保持严格焦点检查；最新短诊断只证明初始焦点已取得，尚未证明整个固定绑定流程通过。

截至此次冻结交接，没有 GitHub 推送、标签或 Release 变更，Latest 仍为 `v3.3.13-t9.9`；最后 origin 与 fork fetch 仍分别为 `e9b21960abd8c6f6aa3864991117f6a5adc48318` 与 `a276c9775429b781fe2294a4c59aa1e858e062b8`。当前设备已离线。全部新门禁及裁决通过后才允许写回六文档、推送和发布稳定版；仍只允许两份应用 APK 加固定子模块递归源码包，签名/性能/系统 native 崩溃与物理 ARM 限制继续披露，不填最终 warning 总数。



C35 首次设备尝试的 ignored 运行工具曾绑定以下冻结源码/产物（后续顺序修正须重新复核，不能借用旧自测）：压力 parent SHA-256 `bf4279d2426ca6e6fa2d92b53368330b2b1cf7ec650eb16da86897dd815dddb1`、测试 `b6240218b44392a81fb5c0117ec0fe4d4055a737d84fe3a79f0af1c55dc5f85e`，139 项内置自测与 82 项独立宿主检查通过；自然 PIN wrapper SHA-256 `fe242459fc96fe3fe1f2b16914d3c48e2ab4f1b7364096161cb3e76386bef2a6`、测试 `e877fbd34ae9606d3a9f889edd5fad2868c22d9d0e0f944fb00a219029a76db5`，48 项宿主检查通过。QA 冻结说明 `/tmp/trime-c35-directboot-qa-final.json` SHA-256 `56d1c343d06f7faed864727bc91d03f91cbb8a1db2d8a3bba72f51e5d464d486`；压力依赖 15 项，自然路径并集 17 项，527 项完整源码和 APK 身份在运行前后复核。独立发布边界复核仍待完成；上述宿主结果不等于设备通过。

触摸驱动预热只初始化反射并输出 READY，不发送任何输入；真实 PIN 提交命令完成后才接收唯一 START 换行及 EOF。原 PIN 提交起算的 7 秒交接总期限、1.25 秒单命令预算与 6500 ms、单指 pointer 0、一个 DOWN/有节奏的 MOVE/一个尝试 UP、最多 60 Hz 均保持。原压力失败也保留完整原始错误；已启动手势只允许有界完成 UP/summary，超过边界依旧失败，不能用清理代替通过。

Android 15 的独立 pilot 实际证明 `run-as` 直接路径与 `/proc/PID/root` 访问均被 FUSE/权限规则拒绝；这不是 Trime 应用 UID 文件访问失败的证明。新资源核验安排在真实普通编辑器 focus/Delete 证明之后，以独立、明确标记的只读特权阶段访问两个固定 backing root：`/data/media/0/Android/data/com.osfans.trime.debug/files/shared` 与 `.../files/rime`。审计限 300 秒、512 文件、256 MiB，保存完整 NUL 清单、前后 stat、原始 cat 字节、设备和宿主 SHA-256 及重新枚举；拒绝符号链接或缺少闭合的命令证据。审计记录 `privileged=true`、`app_uid_access_proven=false`，随后恢复 UID 2000，并核对同一 PID/boot/fixture/bind；不把特权只读核验伪称自然输入动作或应用 UID 访问证明。旧 QA 的 C35 API21 完整矩阵曾真实通过；当前 epoch3 矩阵仍待重跑，以上 Direct Boot 路径亦没有完整正式设备通过事实。

C35 专项字节码报告位于 `build/t07-privacy/candidate35-api21-bytecode/report.json`，SHA-256 `262b44ff1eb4af461886254d664e874afa4acb205a7c65ca199bc14c35bfc205`，passed=true。inventory SHA-256 `785824396f3731df07d8d1effda8ca11921b57b212de515cca3d912eaa889d23`，control-flow `f3b10decc3b0b0701f540dc7ebd674dbceaa7961a2e9ad1fc3928b32ccaca8e2`，证据 manifest `239bfb2f038e3f91e0ac6e9607f1e59f13f2532c1d40eac17e3fe33a97da7ba5`；47 项本地与 15 项外部闭合证据、两次重放一致。三份 APK 的应用 19 个及 instrumentation 6 个 DEX 全为 035，ARM64/x86_64 DEX 摘要图一致；所有 DEX 均无 `java/util/function/BiConsumer`，现存 ExternalSyntheticLambda5 实际实现 Runnable 并传给 `runOnMainSync(Runnable)`，不是按类名后缀推断 API。

实际 DEX 核对 Configuration.Provider 的稳定 lazy、getter 与 worker 的 SDK21 短路、SDK24/实际 DPS/旧 DE/替换状态守卫，以及 WorkManager 2.10.5 官方有界 `getInstance`/初始化锁路径；实际 manifest 保持 min21 并移除默认 WorkManagerInitializer。30 项 C34 已审项目高 API 指令保留，新增 5 项项目指令各有实际 SDK24 守卫，合计 35 项；选定库中的 5 项独立计数。全项目发现 112 项高 API 引用，不能称高 API 全部归零。最小初始化后后台注册/重查 UserManager、13 个 handoff/IME 核心方法与两项确切生成回调映射也已复核；注解不是运行证明。未选路径、反射、native、完整第三方传递依赖与设备运行时序不属于这次专项静态结论。

## C35 旧 QA：API21 历史通过与 SAF 首次准备失败（当前矩阵须重跑）

旧 QA 的 C35 API21 独立审阅真实通过（只作历史，当前 epoch3 矩阵须重跑）：527 项源码重新校验，116 项检查与 453 项实际证据文件闭合；隐私 16 项、编辑器 213 项状态断言、8 主探针、13 导航、17 生命周期、52 字形与 100 次显示/隐藏，以及 unavailable 的实际数字输入/Delete/重试恢复均通过。22 份完整目标应用日志中的 FATAL、BadToken、OOM、ANR、native fatal、WorkManager BOOT 竞态模式及随机虚构私密 token 模式均无命中。受控故障注入、SQLite/ART/EGL 等平台诊断仍保留，不能改写为全无 warning；当前报告没有全运行 warning 合计，最终 warning 计数与裁决仍待其余正式门禁。持久化证据来自设备探针实际读取 Room、符号历史与逻辑 userdict 导出并核对普通模式正对照；审阅者核验固定探针源码与实际 instrumentation/日志结果，没有虚构独立原始持久化快照。

API21 真实 SAF 首次 `final1` 未通过验收准备：Matrix `prepare_setup` 为探针隔离在最后切换 Latin，随后启动 Main 重定向 Setup3；UI helper 在任何 picker 动作前超时，未向 gate 写入 stdin 确认。root 随后 EOF 终止，保留失败路径 `build/t07-privacy/runtime/t910-candidate35-api21-real-saf-final1`（`failure-roundtrip.txt` SHA-256 `e7c1119e596bbe6f76ea6b9d2e6ce65a689e45e38b93fe903a680308cb6166a6`）与 UI 诊断 `build/t07-privacy/diagnostics/candidate35-api21-saf-ui-prompt1`，并已 STOP/确认设备离线。此轮没有真实目录选择或 SAF 三阶段通过证明，不能据此断言产品缺陷。

下一次仅在全新 pre-gate 准备阶段恢复并核验 Trime 为默认输入法，不写 SAF tree、首选项或 grants；校正记录 `/tmp/trime-candidate35-api21-saf-setup-correction.json` SHA-256 `11ef817d67d6051f7a6b0060ad45d7e7e0ec13f92c61184757e15297de59cf96`，只说明准备改动与源码未变。该准备校正已完成独立 peer 复核，随后全新真实 SAF final2 三阶段实际通过；准备审阅不能代替实际门禁。完整 SAF 按进程归属的独立审阅随后通过；其他未完成门禁与最终裁决继续保持 pending。

## C35 历史：真实 SAF final2 三阶段通过

C35 随后的全新真实 SAF `final2` 三阶段已实际通过。可见系统 DocumentsUI 经 24 项 UI 动作在 Documents 下创建并选择 `trime-saf-20a2a0a8-be04-42e8-a5c9-1299c28cc72c`，只在实际 UI 确认后向 gate 提交一次准确目录名；验证 roundtrip 多文件读写与历史保护、进程重启后的持久授权/数据恢复，以及撤销授权后的安全降级。正式 summary 与三份 report 摘要见当前结果表；UI 输出 SHA-256 `95789ba1aa192012265a757f85064f519de3f4966dd315a4706072d46a126c1c`。pre-gate 仅恢复/核验默认 Trime 的校正已通过独立审阅 `build/t07-privacy/reviews/candidate35-api21-saf-setup-correction-final2.json`，SHA-256 `a0ff034ce022765c80cc058bccfd832ce259104fba786754061f434b6e285d23`；这份准备审阅与真实 SAF 三阶段是不同证据。setup-final2 的 stopped.json 已确认模拟器离线。

C35 当前真实 SAF 目录选择/完整部署阶段也实际记录一次非注入的 Rime 排队 5,434 ms（guest 18:06:01.429、PID 3506、WrappedRunnable[667114707]，早于 instrumentation PID 4945）。同一原始事件同时出现在 roundtrip/restart/revoke 三份完整日志与 setup 日志，去重后只计一次，不是三次延迟，不能归为故障注入，也不是 C31 的 6,280 ms 历史事件。SAF 三阶段随后通过；这并未证明任意并发编辑器不受部署等待影响，残余性能限制必须保留。完整独立审阅按进程归属已通过，仍保留真实排队与系统 input native 崩溃，不把功能通过宣称为完整设备日志全无 fatal/warning。

正式三阶段 warning_count 分别 67/68/2，受控 blocked-file 的项目诊断行分别 65/65/0；SQLite WAL、ART 暂停与可选 GCM scheduler 查找另作平台诊断，不能把所有 warning 归为注入。当前 5,434 ms 普通 fullCheck 排队和系统 input native 崩溃均作为非注入残余记录保留；其他 API35 门禁尚未完成，暂不合计最终全运行 warning。C31 的 6,280 ms 部署排队继续作为历史观测，不能称已在 C35 复现或消除。C35 API35、冷启动、Direct Boot 压力/自然 PIN 与最终裁决仍待新通过证据，首次 SAF 准备失败不会被后续通过改写。

## C35 历史：真实 SAF 最终进程归属审阅通过

C35 SAF 最终独立审阅 `build/t07-privacy/reviews/candidate35-api21-real-saf-final2-independent-scoped.json` SHA-256 `efbac9d264c1461bd7ddc90ab46e953af0c1149886f6d9f5c80af76f3bfd7655`，按实际进程归属 passed=true：122 项检查、44 gate 与 48 setup 文件、三份实际 Trime 探针进程 PID 4945/5218/5483，核对随机目录的 24 项 UI 动作结果、原始首选项与 grants、重启恢复、真实撤销授权后的 SecurityException，以及 STOP/离线。UI helper 记录实际控件与动作选择器，但并未为每一步保留完整不可变 hierarchy；持久化内容由固定设备探针及真实 provider 前后断言证明，不虚构独立原始文件快照。

完整设备日志实际保留一次非注入的系统 native SIGSEGV：guest 18:03:29.332、PID 2963/TID 2974、uid 0，前文同 PID 的 AndroidRuntime 明确调用 `com.android.commands.input.Input`，已注入 DOWN/UP 并退出 VM 后崩溃；与当时 Trime PID 2772 及后续 3506/4945/5218/5483 不同。同一事件复制到三份 full snapshot 与 setup，去重为一次，不能描述为 Trime 崩溃，也不能称完整设备日志无 fatal 或将 native fatal 改称普通 warning。后续真实 picker、授权/数据与三个 Trime 探针全部通过，最终审阅没有未归属 fatal。

初筛采用完整设备日志 native fatal 全零条件，因此 `build/t07-privacy/reviews/candidate35-api21-real-saf-final2-independent.json` passed=false，SHA-256 `4bc1bd1c4136b1b11455a43e6269ab9f7e10b713f0314082b2dbc4444728fa56`。这份初筛与所有原始事件保持不变；最终归属复核明确记录系统 input 命令的实际崩溃，按 Trime 进程验收通过，没有删除证据或宣称全局 fatal-free。

## C35 历史：压力 final1 第十阶段失败，完整验收仍待完成

C35 压力首次 `build/t07-privacy/runtime/t910-candidate35-api35-direct-boot-hostgpu-final1/summary.json` SHA-256 `54aa1717f024ef12c41465975fdb5baff614f50223bc36f037d3b6fb5e651ce8`，2026-10-08T10:07:21.657563Z 至 10:12:47.140986Z，整体 passed=false。第一至第九阶段通过；从真实 PIN 提交起算、未放宽的原 7 秒交接中，旧 PID 消失 3.451998857 秒、新 Trime PID 2842 首见 6.414823863 秒。READY 预热在 START 前输入事件为 0；触摸原 6500 ms/60 Hz 单指要求实际得到完整 6501 ms DOWN–UP，原始接受/拒绝事件均保留，touch passed=true。第九阶段通过不能抵销第十阶段失败。

第十阶段错误为 `Fresh shell-window hierarchy unavailable: direct-boot-unlocked-keyboard`。20 次观察器均 rc=0 且返回合法的新 XML，fixture 的 UNLOCKED 字段可见而没有 IME，尚未执行计划的唯一 focus，因而没有实际 Delete 或资源审计。原 fixture 300 ms 自动 showSoftInput 在 guest 18:11:58.549 发出，18:11:58.551 因未 served 被拒绝；新 Trime 到 18:12:03.005 才 onCreate，第十阶段启动已在顶层的 fixture 实例不会再触发 onCreate 的自动显示。这条原始顺序证明门禁在计划聚焦之前提前要求键盘，不能把有效 XML 当作 adb/观察器故障，也不能把此次完整验收记作通过。

ignored QA 已在宿主校正并复核该检查顺序（新设备结果仍待证明）：先确认 fixture，再执行既定的一次字段 focus，随后检查实际 IME 与既定的一次 Delete；不添加救援 show、输入法重选、force-stop、额外动作或放宽时限。压力 parent 的完整宿主与独立 peer 已通过，自然 PIN 仅摘要重绑与 48 项宿主通过、最终 peer 仍待；同一 C35 APK 的 final2 此后已在第九阶段整体失败，见当前 epoch3 状态。当前 epoch3 完整设备结果仍待证明，不宣称已经通过；C35 产品源码/冻结 APK 未因这次门禁顺序调整而改动。PIN 已清除、adbd 已恢复 UID 2000，原 stopped.json 确认离线；失败证据原样保留。

## C35 此前 QA：API35 matrix final1 失败与压力工具复核（当前以 epoch3 为准）

C35 API35 host GPU 完整矩阵首次 `build/t07-privacy/runtime/t910-candidate35-api35-hostgpu-final1/summary.json` SHA-256 `f549102f48e7fd1efbfe3d3f2ade1688747e475745b0c1c5db62d4e8a180b45a`，2026-10-08T10:14:52.086271Z 至 10:26:02.052812Z，整体 passed=false。自然持有剪贴板条目的真实 UI 路径通过，前 9 项功能/断言探针通过，包括 privacy、editors 的 100 次显示/隐藏、setup、engine、startupFailure、feedback、shutdown、clipSave 和 saf；这些部分结果不能替代完整矩阵。

随后 ClipEditorProbe 在第 68 行失败：`A stable Trime keyboard view was not visible in the clipboard editor`；实际 audit PID 6197，application_ready_wait_ms=228。固定绑定 helper 的原 45 秒 setup 也失败，没有增加救援或额外状态变更。t02、导航、生命周期和 unavailable 阶段未执行，不能补借 API21 或历史候选的通过。原日志与摘要保留，模拟器已确认离线；目前仅做只读归因，没有新产品改动，既不直接断言为产品缺陷，也不把失败改作 QA 误报或完整通过。

压力第十阶段顺序校正已完成宿主及独立 peer：parent SHA-256 `c403bdde113fc39a7950d15fd4a70a319964001f5c6b20dd66bcafded89520cb`、测试 `6ce162d3d5feed45660b0cadc22302388f769a099e4882aacdc9fc1e6b7e4be2`；QA 冻结报告 SHA-256 `919b0b12fa1bf703edabc15033e03747f7da2533f77919b976da121644d745c5`；139 项内置、90 项独立宿主和 2 项反事实检查通过，独立审阅 `/tmp/trime-c35-independent-stage10-pressure-release-peer.json` SHA-256 `308ca21cf0e355d15caca28d3418661f5532aaa5ec391daf917e38329d6c7ca8`，peer_passed=true、release_acceptance=false。先核验 fixture、既定唯一 focus 后再核验真实键盘/Delete，原 7 秒、1.25 秒、6500 ms/60 Hz、单指与独立只读资源审计规则保持；首次实际失败仍被反事实检查拒绝，不按新解析器回填通过。

自然 PIN 仅重新绑定 parent 摘要，wrapper SHA-256 `47ee14573414fbbf37056cb553fb9315446f34f396913aa0ac5c87342d11e104`，测试 `e877fbd34ae9606d3a9f889edd5fad2868c22d9d0e0f944fb00a219029a76db5` 未改，48 项宿主检查通过（12.359 秒）；此旧绑定记录当时的最终独立 peer 尚待完成；当前自然绑定已改为 epoch3，工具审阅见当前状态。此后压力 final2 已整体失败（见当前 epoch3 状态），没有完整设备通过结果；自然 PIN、API35 矩阵、三轮冷启动与最终裁决仍待新证据。工具复核不能替代设备通过。

## candidate34 历史构建与 Direct Boot 失败（不计入 C35）

C34 提交 `72ef1d41993d39cc6b01367e200f2b1f3c5680d9`、527 项源码、时间戳 `1791446431898`（2026-10-08T08:00:31.898Z）。它只为 C33 的抽出注册方法补上 RequiresApi 导入和 `@RequiresApi(N)` 两行，原调用处 SDK 检查、min21 及验收期限保持。完整构建 472.113834 秒通过（BUILD SUCCESSFUL in 7m 50s，112 tasks：33 executed/79 up-to-date，应用 585、build-logic 9，Lint 0，编译 warning 行 0）；build.log SHA-256 `22f069f19f4217f125b6f75899945b4b40ed9918cf7216a6043ffde387ac495f`。identity SHA-256 `ac7c5bac7b80e847be89d8ce81e75bdb3046723bcac7dff1aeedd3c03dd65075`；ARM64 APK `a8e4d2c138767de523673841d378bee9e636fb00250c5279576cf0c03d060fb0`、x86_64 `ebf538809ac24903f149156a144aae576664babdbd4dd526e60dece9387c1722`、AndroidTest `db1b491f36cb213b1cc54c06fcb05cdcb4b0ddcddbd3f51dab72eae61181f90c`。APK 静态报告 `5a650d43334e2c81e171d3edcf278f7b79c473c975576a85eccaf8ef6e23e603` 与 API21 专项字节码报告 `26c1fd0bb818afe311bfef4524a9d76e6b17fee918c186612adcfc264334afae` 实际通过，但不证明设备验收。

C34 原压力 `build/t07-privacy/runtime/t910-candidate34-api35-direct-boot-hostgpu-final1/summary.json` SHA-256 `1edee5c6dd5425f1616412c4e26b3e5d5c4d7f1aba219bbf274a969d351891e5`，整体失败：前八阶段通过，第九阶段超过原精确 7 秒 PID 替换期限，第十阶段未执行；6500 ms/60 Hz 手势未形成完整 DOWN/MOVE/UP 闭合，touch passed=false。此轮不能直接沿用 C32 的 DeadObject/4000 ms 退避归因，也不能用前面阶段或清理通过改成完整压力通过。

C34 独立自然 PIN `build/t07-privacy/runtime/t910-candidate34-api35-direct-boot-natural-pin-final1/summary.json` SHA-256 `b08d60fb99bf8eba6a8f83c89c59533f49001fa97965543965b7e59a4e107385`，整体仍失败。前九阶段实际通过，新进程首见 5.809970 秒，符合原 7 秒；普通编辑器的一次 focus 与一次真实 Delete 把 UNLOCKED 变成 UNLOCKE，并保持同一新 PID 2872、绑定 seq 6。但第十阶段随后通过 run-as/find 核验 FUSE 外部目录时实际 Permission denied，完整门禁未通过；另定位到普通进程 WorkManager 双重初始化的 BOOT 竞态，后续由 C35 修复。部分输入证明只能作为准确的历史事实，不能记为整个自然门禁通过。两轮 PIN 清除、adbd UID 2000 与模拟器离线均已确认，清理不改变失败结论。

更新前六份 C34 完整草稿逐字节冻结在 `/tmp/trime-t910-doc-candidate34-final-history-snapshot`；此前 `/tmp/trime-t910-doc-candidate34-initial-snapshot` 和 C31/C32/C33 历史快照保持不变。C34 未形成全部设备验收与最终发布裁决，不能作为 C35 通过证据。C31 的 6,280 ms SAF 部署排队只作历史性能限制，不能记作 C35 复现或消除。

## candidate33 历史构建失败（不计入 C35）

C33 提交 `9535c7dcc447587af083d258f87eb37ca5810b6f`（527 项源码；时间戳 `1791445802430` / 2026-10-08T07:50:02.430Z）将解锁广播移到独立 HandlerThread，并在 AppPrefs/Privacy/Clipboard/Collection 最小初始化后注册、重新查询 UserManager。独立源码审阅未见 P0/P1，但完整构建实际失败；`build.log` 的准确末尾为 `BUILD FAILED in 8m 7s`，不能记为构建通过。

唯一 Lint issue 为 `TrimeApplication.kt:130` 的 `InlinedApi`：`Intent.ACTION_USER_UNLOCKED` 要求 API24，抽出的注册方法缺少 API24 声明。调用处原 N 版本检查已存在；C34 两行声明修正使 Lint 检查实际通过，没有压制警告或放宽门槛。C33 未形成正式冻结的 identity/APK 信任链，没有静态或设备通过证据；失败日志 SHA-256 `ecda6f77970e6bfdfe801b8327927c2e82e8e4f58f2d900d8e40257916bf185f`，路径 `build/t07-privacy/stable-candidate33/build.log`。

六份 C33 初始草稿逐字节保存在 `/tmp/trime-t910-doc-candidate33-history-snapshot`，不因后续候选而改写。C31/C32 历史和物理设备/签名/SAF 排队限制继续保留。

## candidate32 历史构建与运行尝试（不计入 C35）

candidate31 因自然 PIN 解锁的真实产品延迟已停止作为发布候选。RUNNING_UNLOCKED 后旧 DE 输入法已收到 `onStartInput`，仍等约 2.7 秒后的解锁广播才开始替换。candidate32 修复已解锁后的旧 DE 进程仍等待解锁广播的问题：由输入法生命周期检测解锁并提前请求替换，原子协调只启动一次进程替换并只结束一次广播回调；替换中的隐私状态持续保留，普通编辑器绑定、手动切换或清理不会解除它，新正常进程才恢复原设置。本次新增 12 项回归已纳入 21 项通过的专项 JVM（PrivacyModeState 12 项、DirectBootProcessHandoff 9 项），格式检查通过；完整构建已通过，应用 585 项、build-logic 9 项测试通过，Lint 0 问题；其后 API21 完整矩阵通过，但两项 Direct Boot 完整门禁失败，不能发布。

C32 六个源码文件的修改已本地提交为 `8d76ec2219e0b8f5adda6e457a63d1fe7f66405a`，冻结输入策略覆盖 527 项源码；21 项专项 JVM 与格式检查通过。完整构建与 APK 静态检查已通过，以下记录只作 C32 历史；其两个 Direct Boot 完整门禁未通过，已停止作为发布候选。

| C32 历史项目 | 实际事实或未完成范围 |
| --- | --- |
| 实现提交 SHA | `8d76ec2219e0b8f5adda6e457a63d1fe7f66405a` |
| 构建时间戳 | `1791443610624`（2026-10-08T07:13:30.624Z） |
| 完整构建 | 通过；519.350829 秒，Gradle BUILD SUCCESSFUL in 8m 37s；112 tasks（35 executed、77 up-to-date），Lint XML 0 issue，编译/build warning 行 0；build.log SHA-256 `cc25287a0ba80a826df5c49b732d3955dfdcc3e3e9991fe390a80a532dc21bf3` |
| JVM 计数 | 应用 56 份 XML、585 tests；build-logic 9 tests；两组 failures/errors/skipped 均为 0 |
| 新增 Direct Boot/隐私回归实际结果 | 21 项专项 JVM 通过：PrivacyModeState 12 项、DirectBootProcessHandoff 9 项；包含本次新增 12 项回归，格式检查通过，完整 JVM 585 项及 build-logic 9 项均通过 |
| 冻结 APK/来源身份 | `build/t07-privacy/stable-candidate32/identity.json` SHA-256 `032ae96a133372d18ab8cf54f15e71633aecdf80b49faac2da56ce4dbdc6b2b9`；ARM64 `36c4e5729f0783bfcf2d7519eef29528f9ec58cc3c1db60e7cfe2028b1ce5142`（22,442,574 字节）、x86_64 `fe46ee75f74defeb3ab4c0ec147297afa63a51c8e7fc7628d507b7eac24db596`（22,539,611 字节）、AndroidTest `dc0b5656f5f31f6658396984f1dd4ba7e45e32c8628a2e9e051a23e34c971878`（1,464,301 字节）；构建前后 527 项源码不变，source dirty=false |
| APK 静态检查与签名警告 | 通过；冻结签名、包/版本、ABI、资源与静态对齐检查符合候选身份；report SHA-256 `ea68cb0545b22aed49615eb5e57b635e33b0bba2627761c7043d0f4238ded998`；应用两份各保留 2 条必要 META-INF ServiceLoader v1 提示，test 0，warning_free=false；debug 证书/包与 min21/target37 设定保留，静态通过不等于设备验收 |
| API21 专项字节码检查 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| API21 完整矩阵 | phase4 final2 整体 FAIL：声音探针全进程 FD 增量断言失败，来源未证实；旧 final1 的 8 探针/13 导航/17 生命周期/100 次显示隐藏/unavailable 与独立审阅通过保留为历史，不计 C36验收 |
| API21 正式摘要 | `build/t07-privacy/runtime/t910-candidate32-api21-final1/summary.json`，SHA-256 `67fa7c35bd8e57e29af82baca134e4141dc009fdca351b5d889041c61657a3c6`；unavailable report SHA-256 `ea114667f5bfd6b5ad6fd6d23fcfb5fc1314e1f55c378b2bd873e733dbdfb08b` |
| API35 完整矩阵 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| API35 正式摘要 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 三轮软件冷启动 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 冷启动摘要 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| API21 真实 SAF 三阶段 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 真实 SAF 摘要 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| Direct Boot 压力门禁 | 失败（仅 C32 历史）；第九阶段 overall 5.502 秒处 am start 超过 1.25 秒单命令预算，真实 DeadObject 进程退出与 ServiceCreate 相撞，AMS 退避由 1000 增至 4000 ms；summary SHA-256 `0217dae4aec7165da34d17b9261ab6b912d312a18a89eca50cb30902e03f2544`，不是误报 |
| Direct Boot 独立自然 PIN 门禁 | 失败（仅 C32 历史）；前九阶段通过，第十阶段 Natural Delete key missing or ambiguous；新 PID 在 6.972178 秒、旧 PID 消失在 3.974909 秒，原总期限 7 秒；summary SHA-256 `45ebf7bc4d685dacc5ac833f14770ec9958da3f624d4eae33252dc8df0827a9a`，不能计完整通过 |
| 最终日志裁决 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 裁决摘要 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 运行 warning 计数与分类 | C32 本草稿未记录最终验收事实，已停止作为发布候选，不计入 C35 |
| 全部运行验收 | 未通过；C32 已停止作为发布候选，不计入 C35 验收 |

C32 的完整构建、APK 静态与 API21 矩阵通过仅作历史；自然 PIN 与压力完整门禁均失败，不能稳定发布。应用 APK 各保留 2 条 META-INF 提示，完整运行 warning 未最终裁决。

C32 首次格式检查只发现两处空行问题，修正后最终格式检查及 21 项专项 JVM 均通过；没有把首次格式失败隐去，也不将其算成产品运行故障。完整构建随后实际通过（应用 585、build-logic 9，failures/errors/skipped 均为 0，Lint 0 issue），完整记录保持 source_unchanged_during_build=true。

C32 运行工具绑定仅完成宿主检查：自然 PIN wrapper SHA-256 `78027fe54a3a46786fb96ed6fa7b67a8d2efc9cb8d8a9d7767eff20b7577f534`、测试 SHA-256 `6b549c018d6cc91903629c1283f198335d1444b796c74bd916221bf76a47a0dc`，38 项 host 检查通过；矩阵 SHA-256 `69167b4957b5aaa0fa3ba93f0d2ab3e7be102bd73757c844c46c9e43e7676271`、Clip wrapper SHA-256 `06b5d252b8b417654acbc274083965e8a0a9ea452eeda21745fef0cd6c558e26`，分别 19 与 29 项 root host 自测通过。它们不证明设备完整门禁通过。C32 随后 API21 完整矩阵通过，而独立自然 PIN 与压力门禁失败；未完成的其他范围保持历史未完成。

C32 设备尝试已结束：独立自然 PIN `final1` 前九阶段通过，第十阶段未找到唯一 Delete，整体 failed；压力 `final1` 第九阶段真实超时，整体 failed。两轮均已清除 PIN、恢复 adbd UID 2000 并确认模拟器离线。其失败不能用前面阶段的通过或清理成功抵销。

自然 PIN 的原七秒期限内，新 PID 首见 6.972178 秒，旧 PID 消失首见 3.974909 秒；但第十阶段没有形成真实 Delete 验收。随后 QA helper 已修正默认 QWERTY 的 35 格键盘识别，42 项宿主检查通过，仍没有实际 Delete 通过结果。压力路径在总耗时 5.502 秒处启动普通编辑器的 `am start` 超过 1.25 秒预算；真实 DeadObject 退出与 ServiceCreate 相撞，系统重启退避从 1000 ms 增至 4000 ms，不是纯观察误判。完整摘要分别位于 `build/t07-privacy/runtime/t910-candidate32-api35-direct-boot-natural-pin-final1/summary.json`（SHA-256 `45ebf7bc4d685dacc5ac833f14770ec9958da3f624d4eae33252dc8df0827a9a`）与 `build/t07-privacy/runtime/t910-candidate32-api35-direct-boot-hostgpu-final1/summary.json`（SHA-256 `0217dae4aec7165da34d17b9261ab6b912d312a18a89eca50cb30902e03f2544`）。

六份 C32 草稿已逐字节保存在 `/tmp/trime-t910-doc-candidate32-history-snapshot`，快照不因后续 C33 验收而改写。所有 C32 构建、静态与 API21 通过结论只证明 C32 对应范围；C32 已停止作为发布候选，不能证明 C35 通过。

## candidate31 历史快照与证据（不能用于 C35 验收）

历史六文档快照：`/tmp/trime-t910-doc-candidate31-history-snapshot`。以下保留 C31 源码、APK、测试与失败审计，所有“通过”均只属于当时的 C31。

### candidate31 最后失败与停止发布候选的原因

C31 的 API35 Matrix `final6` 完整通过是已发生的历史事实，摘要 SHA-256 `6a92939ac9526ff20fc347c6a32bdfb309860922ca041a866a4261a8944d90b6`；它不能证明 C35 的任何运行结果。

C31 60 Hz 压力 Direct Boot `final6` 的完整摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final6/summary.json`，SHA-256 `baf7402dc9e6bac32427db03314799c775503cb026b8a34f3f3a89fa8257b51a`，实际 failed；其末尾 PS 在剩余约 0.1645 秒预算中超时。独立自然 PIN `final1` 同样在第九阶段失败，第十阶段未运行，摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-natural-pin-final1/summary.json`，SHA-256 `2ee12916fcef8eb0f83cb69e8a16e58f01f1bded54c9de0af43b68b82abac48a`；末尾 PS 在剩余约 0.3835 秒预算中超时。PIN 清除、adbd UID 2000 和模拟器离线均已确认，清理不改变失败结论。

自然路径没有重选输入法或 force-stop 救援；后续事实确认系统已 RUNNING_UNLOCKED，旧 DE 进程也已收到 `onStartInput`，却仍等待约 2.7 秒后的解锁广播，属于实际产品延迟，C31 停止作为发布候选。C32 随后已实现生命周期提前请求、原子一次协调及替换期持续隐私，并完成本地提交、构建、静态与 API21 矩阵；完整 Direct Boot 门禁仍失败。当前 C35 必须重新进行设备验收，不能沿用 C31 Matrix6 或 C32 的通过。

candidate31 全新构建通过，应用 JVM 573 项及 build-logic 9 项通过，Lint 0 问题；APK、签名、资源与静态对齐检查通过，API21/API35 完整矩阵已通过；Direct Boot 压力/自然 PIN 两个设备门禁后来均失败，完整目标日志审阅及最终裁决未完成。candidate30 因真实系统 SAF roundtrip 失败已作废，其构建及 API21 矩阵通过结果不能作为 candidate31 发布证据。candidate31 未完成并已作废，不计 candidate35 通过。应用 APK 的 META-INF 签名提示仍保留，因此明确保持 `warning_free=false`。本报告验证统一隐私状态是否同时约束 Rime 学习与上下文、剪贴板历史及建议、符号历史、内联建议、朗读、内容日志和跨编辑器临时状态；测试只使用随机生成的虚构口令、验证码与标记文本。

## candidate31 历史：冻结身份

| 项目 | 冻结值 |
| --- | --- |
| 版本 | `3.3.13-t9.10` / versionCode `20261115` |
| APK 构建源码 | `53760c5b20ff49781218d2829a9696787976229b` |
| 最终固定上游 | `e9b21960abd8c6f6aa3864991117f6a5adc48318` |
| 上游合并提交 | `a276c9775429b781fe2294a4c59aa1e858e062b8`、`7a6b77de31a489d2b0e545eb32d1bafc01c343ce`、`2852b6eacf4f2cefc00835514a0e39fbd4c82b4d` |
| 构建时间戳 | `1791429468348`（2026-10-08T03:17:48.348Z） |
| 包名/变体 | `com.osfans.trime.debug` / debuggable debug；默认及非中文语言环境显示 `Trime`，简体中文显示“同文输入法”，香港/台湾繁体中文显示“同文輸入法” |
| Android 范围 | min API21、target API37；ARM64 与 x86_64 |
| 证书 SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| 冻结身份 JSON | `build/t07-privacy/stable-candidate31/identity.json`，SHA-256 `40dd9fd042ca1a3e8f1f45cbb53b61f7dcbf09763d767a7bed6396e54109726f` |

冻结 APK 取自实现提交；最终文档提交和发布标签晚于该提交。递归源码包以发布标签指向的最终文档提交为根并包含固定子模块，不能把 APK 内嵌源码 SHA 与源码包根提交混为一项。

| 冻结产物 | SHA-256 | 字节 |
| --- | --- | ---: |
| ARM64 应用 APK | `ce74f071ed5395a0c6fa8d9bac8d3496c344b4be37e149a946f80baffd7d30f3` | 22,442,576 |
| x86_64 应用 APK | `ef03cb142b25fcc5413dd50533baeb553d989e48c8bdcb90c030b03b6142620b` | 22,539,613 |
| AndroidTest APK（仅本地验收） | `e301a0730de702915fef5cd5fc62533c5483f755d5e56961456c1ffbf5923d8b` | 1,464,301 |

公开文件名固定为：

- `trime-v3.3.13-t9.10-arm64-v8a.apk`
- `trime-v3.3.13-t9.10-x86_64.apk`
- `trime-v3.3.13-t9.10-source-with-submodules.tar.gz`

## candidate31 历史：实现与安全边界

隐私状态由以下任一条件触发：

- Android 密码输入类型，包括文本、可见文本、网页和数字密码变体；
- 应用在 `EditorInfo.imeOptions` 设置 `IME_FLAG_NO_PERSONALIZED_LEARNING`；项目使用 Android 定义的 `0x01000000` 位值兼容 API21，避免直接引用 API26 才加入的字段；
- 用户在设置或输入法切换面板开启手动隐私模式。

激活后执行以下约束：

- 阻止 Rime 用户词典学习、提交历史和上下文联想；在编辑器、隐私、引擎失败/重启/停止边界清除 Context、组合、候选、T9 映射、翻页及挂起按键状态；
- 阻止剪贴板历史和收藏写入、剪贴板建议、符号历史、内联建议、按键/提交朗读及内容日志，并清理上一编辑器显示的建议与预编辑界面；
- 以编辑器所有权、隐私代次和持久化许可隔离排队回调，避免旧编辑器或切换前任务越界；
- 临时验收方案覆盖 Lua processor、segmentor、translator、filter、`tags_match`、`fini` 和用户词典接口；已启用的 3 个方案遍历隐私 guard 与历史隔离；
- 输入结束或隐私模式关闭时先清理私密临时状态，再恢复普通剪贴板、符号历史、Lua 回调、上下文与用户词典学习。

剪贴板按 Android 敏感内容约定识别 `android.content.extra.IS_SENSITIVE`。在能读取 `ClipDescription.extras` 的系统版本上，Trime 会拒绝将带敏感标记的内容写入历史或建议；Trime 自己创建的敏感剪贴板在 API21+ 还使用内容指纹一次性抑制。Android 5.0–6.0 无法读取其他应用只通过现代 extras 设置的敏感标记，这是平台范围限制。

参考定义：

- [EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING)
- [Android 敏感剪贴板说明](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste#sensitive-content)

## candidate31 历史：共享资源恢复

修复资源描述摘要未变化时跳过已安装文件检查的问题。同步会补回缺失或不是普通文件的内置共享资源，并在修改前验证资源路径规范化与目录边界；现存的本地修改文件仍被保留。JVM 回归覆盖同一描述和描述变化时的缺失文件恢复、非法路径及写入失败保护。启动故障探针会真实删除 `shared/default.yaml`，确认补回内容与原文件逐字节一致、描述文件未变化并能选择方案；candidate31 的 API21 与 API35 正式启动故障探针均已通过缺失共享文件恢复检查。

## candidate31 历史：SAF 子项枚举修复

SAF 同步与目标文件查找改用 `DocumentsContract` 的子项查询并遍历全部游标行，覆盖首项之后的文件和目录。列表在读取前后检查提供器的加载及错误状态，并拒绝缺失字段、非法名称、重复文档身份或名称、负数元数据以及把父目录列作子项；不完整查询不能被当成空目录，避免误判缺失后覆盖文件或清理导入历史。新增 SAF 子项枚举 JVM 回归 11/11 通过，覆盖多子项、重复项、查询失败与末尾加载状态变化；candidate31 完整构建及 API21 真实系统提供器 SAF 三阶段均已通过，覆盖多文件识别、替换、导入历史保护、重启及撤销授权；API35 host 完整矩阵 final6 已通过，Direct Boot 压力/自然 PIN 两门禁与最终日志裁决仍待完成。

## candidate31 历史：上游整合

相对 t9.9，本轮分三次整合 `osfans/trime:develop` 的 30 项非合并提交，并最终固定到 `e9b21960`：

| 合并提交 | 上游范围 | 数量 | 主要内容 |
| --- | --- | ---: | --- |
| `a276c977` | 至 `f3ca316f` | 12 | 主题诊断、格式工具、Setup、存储与重启恢复 |
| `7a6b77de` | `c6866f9f`…`1ba0780c` | 17 | YAML/主题/按键模型、Boost、SAF StorageAccess 与同步重构 |
| `2852b6ea` | `e9b21960` | 1 | Direct Boot 支持 |

数量由三次合并的第二父提交相对第一父提交执行 `git log --no-merges` 得到，`12 + 17 + 1 = 30`。合并后保留 fork 的九键、统一隐私、Android 5.0、启动/焦点、降级输入与重试保护。

## candidate31 历史：T08、T09 与 T10 的状态

本报告范围是 T07；candidate31 通过全部门禁后才可记为完成。相邻任务中有代码复用和回归覆盖，但未满足各自的完整验收条件：

| 计划项 | 代码与待复验范围 | 尚未形成的验收结论 |
| --- | --- | --- |
| T08 候选延迟和内存预算 | 上游按主题隔离字体缓存、缓存键动作、延迟构造液态键盘数据；Rime 响应仍使用合并快照；运行矩阵检查旧编辑器结果不越界 | 未在同一真机正式构建上测量触摸、Rime 队列、计算、显示、提交各阶段、总 PSS 与趋稳情况；不宣称 `P95 <= 100 ms` |
| T09 中英/数字/符号切换 | 输入框矩阵与回车动作、九键恢复、主题/隐私/硬件变化后的编辑器模式保持；candidate31 设备结果待复验 | 未执行固定的 `Android 16`、`Wi-Fi`、`B站`、邮箱、电话和金额混输任务及切换次数对照；不宣称完成 T09 |
| T10 个人词库与候选管理 | 私密会话禁止用户词典学习，普通会话恢复学习；既有 T04 隔离学习基线仍可用 | 未把短语编辑、导入导出、候选置顶、恢复默认排序、展开定位与迁移失败恢复作为本版功能和验收；不宣称完成 T10 |

这些是相邻计划的范围边界；candidate31 完整构建及 API21/API35 正式矩阵已通过，仍需完成 Direct Boot 两门禁、日志审阅及发布裁决。

## candidate31 历史：构建与宿主检查

| 检查 | 结果 |
| --- | --- |
| 完整冻结构建 | 通过；416.756 秒，Gradle `BUILD SUCCESSFUL in 6m 55s` |
| Gradle 任务 | 112 项：35 executed、77 up-to-date |
| 应用 JVM | 55 份 XML，共 573 项；失败 0、错误 0、跳过 0 |
| build-logic | 9 项通过；失败 0、错误 0、跳过 0 |
| 门禁与发布工具自测 | Direct Boot 60 Hz 压力工具原 135 项及专项 68 项自测、独立 68 项复核通过；自然 PIN wrapper 28 项自测及独立 28 项复核通过；其后两项 C31 设备门禁均失败；软件冷启动契约检查 36/36；自然 UI 助手 56/56；矩阵接线 19/19；上一版裁决器离线检查 44/44、Release tools 16/16，当时新绑定契约未完成最终记录；这些均为 C31 历史工具结果。自测通过不替代设备验收 |
| Lint | 0 问题 |
| 构建输入 | 构建前后源码摘要一致，`source_unchanged_during_build=true`，源码树干净 |
| 构建日志 | 本轮未检出 warning 行；`build/t07-privacy/stable-candidate31/build.log`，SHA-256 `0b146a93d3b61e9a549410d1f33e16efe300ffea3e016b2f127d3cc57cc37779` |
| APK 静态报告 | 通过；签名、版本/提交身份、单 ABI、16 KiB ZIP/ELF 静态对齐和 48 项内置资源完整性均符合冻结身份；`build/t07-privacy/stable-candidate31/apk-verification.json`，SHA-256 `75edadc56ae75fecbe2ef2ad7cb77561632c1c951283188e565605fb11edaac9` |

API21 专项静态字节码复核通过，报告 `build/t07-privacy/candidate31-api21-bytecode/report.json`，SHA-256 `7be4f6467e3b2a339bb75e3b89b877f4ca2abd6c5e0c88cace854fd50b27dab8`。校验实际三份冻结 APK；ARM64 与 x86_64 的 19 个应用 DEX 摘要相同，应用及测试 DEX 均使用 035 格式。新 SAF helper 的 43 个 Android/Java 调用符号解析至 API21 或更低；实际 DEX 中复核了隐私剪贴板清理、extras、设备保护存储、振动、API29 resolver 和状态回传路径的版本 guard。该结论只覆盖报告列出的路径与静态检查，不代表全 APK 的高版本 API 引用归零，也不替代设备验收、ARM64 原生代码或物理 Android 5.0 设备验证。

## candidate31 历史：APK、签名与静态验证

两份应用 APK 的包名、版本、versionCode、min/target SDK、BuildConfig 提交身份、48 项内置资源摘要、v1/v2 签名、证书、单 ABI 和 ELF/ZIP 静态对齐均通过；AndroidTest APK 的目标包、签名和 ZIP 静态检查通过。ARM64 验收仅覆盖构建和静态检查，没有 ARM64 真机或原生 ARM 镜像运行结果。

| 检查 | ARM64 | x86_64 | AndroidTest |
| --- | --- | --- | --- |
| SHA-256 | `ce74f071…7d30f3` | `ef03cb14…42620b` | `e301a073…923d8b` |
| 包/版本 | `com.osfans.trime.debug` / `3.3.13-t9.10` / `20261115` | 同左 | `com.osfans.trime.debug.test`，目标为应用包 |
| v1/v2 与证书 | 通过 | 通过 | 通过 |
| META-INF v1/JAR 提示 | 2 | 2 | 0 |

两份应用保留的提示为：

- `META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler`
- `META-INF/services/kotlinx.coroutines.internal.MainDispatcherFactory`

这些 ServiceLoader 条目为运行所需，不能为了清空提示而删除。v2 验证通过也不会把 v1 未覆盖条目改为已覆盖，因此本报告明确保持 `warning_free=false`，不宣称所有警告或风险已消除。静态 16 KiB 对齐不等于物理 16 KiB 页设备运行通过。

## candidate31 历史：API35 冷启动门禁

candidate31 冷启动 `final3` 已使用 API35 软件渲染、2 核、每轮全新 wipe 的模拟器完成三轮。每轮在初始部署仍运行时点击 DONE，以设备 monotonic 时钟读取 `wm_on_top_resumed_gained_called`，要求 MainActivity 严格小于 5 秒达到 top-resumed、连续获得至少 2 秒焦点，并检查新增 ANR 文件、包名 trace 与文本命中。旧候选的具体时延不作为 candidate31 证据。

通过；API35 软件渲染、2 核、三轮独立全新 wipe；MainActivity top-resumed 分别 1.079 / 0.668 / 0.808 秒，持续焦点分别 2.890382 / 3.432124 / 2.988492 秒，均符合 <5 秒与 >=2 秒要求；未发现新增 ANR 文件或包名 trace/文本命中，三轮模拟器均确认离线

报告：`build/t07-privacy/runtime/t910-candidate31-api35-cold-gate-final3/summary.json`，SHA-256 `657cb2e1005bbb14187aa39513a8f9f1cbe7b21f611035e848e8fc26536fa825`；196 项证据快照 SHA-256 `85d0c6a51bb8ad03c4c6d1e9715bebc3985d68d544a7f00beebccca8647af3db`。该门禁只证明所述冷启动边界，不替代完整矩阵，也不是 T08 的真机候选延迟预算。

## candidate31 历史：完整设备验收矩阵

C31 当时要求结果绑定其冻结的 x86_64 应用 APK `ef03cb14…42620b` 和 AndroidTest APK `e301a073…923d8b`；此身份不是 C34 产物。失败准备、旧 APK 或其他候选均未计入通过证据。

| 设备/范围 | 结果 | 实际验证路径 |
| --- | --- | --- |
| Android 5.0 / API21 privacy | 通过；隐私专项与普通模式恢复 | 三类触发、3 个已启用方案、Lua 生命周期、用户词典/历史/建议/朗读/日志隔离及普通模式恢复 |
| Android 5.0 / API21 editors | 通过；输入框、浮层及连续 100 次显示/隐藏 | 输入框矩阵、统一回车动作、九键恢复、第三方主题回退、浮层生命周期、真实 WebView 与连续 100 次显示/隐藏 |
| Android 5.0 / API21 其余主探针 | 通过；8 项主探针均通过，含缺失共享文件恢复 | privacy、editors、engine、startupFailure、feedback、shutdown、setup、clipSave |
| Android 5.0 导航与生命周期 | 通过；13 条导航路由、进程恢复、旋转与应用切换 | 13 条导航路由、杀进程恢复、旋转、应用切换；API21 不含分屏 |
| Android 5.0 故障降级 | 通过；降级输入与恢复后重试 | 引擎/资源不可用时仍可受限输入、导航与删除，恢复资源后重试成功 |
| Android 15 / API35 privacy | 通过；API35 host 完整矩阵 final6 | 带敏感 ClipDescription 标记的剪贴板、三类触发、学习/历史/日志隔离及普通模式恢复 |
| Android 15 / API35 editors | 通过；API35 host 完整矩阵 final6 | 输入框矩阵、主题切换、九键恢复与浮层生命周期 |
| Android 15 生命周期/分屏 | 通过；API35 host 完整矩阵 final6 | 杀进程恢复、旋转、切换应用及上下两个分屏位置输入 |
| Android 15 其余主探针 | 通过；API35 host 完整矩阵 final6 | privacy、editors、setup、engine、startupFailure、feedback、shutdown、clipSave、saf、clip、t02 |
| Android 15 导航与故障降级 | 通过；API35 host 完整矩阵 final6 | 13 条导航路由、冷/暖 ACTION_RUN、资源不可用降级与重试 |

API21 矩阵摘要：`build/t07-privacy/runtime/t910-candidate31-api21-final1/summary.json`，SHA-256 `8be169d3902f2496f74f6a720863b8e1b7298d05e57479f0a92b4421bac10004`。该轮总用时 765.216 秒；主探针 359.021 秒、导航 153.272 秒、生命周期 121.606 秒、故障降级及恢复 33.004 秒；结束后已确认模拟器离线。故障降级报告 SHA-256 `e96ddeb2626e1a385f4b0acb98f7952ad8e9aedb5f64d1e728cbae1d86952c15`，其中主动故障注入与平台 warning 仍保留，须由最终日志裁决解释。API35 host GPU 矩阵摘要：`build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final6/summary.json`，SHA-256 `6a92939ac9526ff20fc347c6a32bdfb309860922ca041a866a4261a8944d90b6`。

API35 host 完整矩阵 `final6` 全部六阶段通过，11 项主探针的 functional/assertions 均为 true；自然 UI 11 阶段、13 条导航路由、九键、杀进程恢复、旋转、应用切换、上下分屏输入、连续 100 次显示/隐藏和不可用引擎/资源的降级及恢复重试均通过。摘要绑定同一 C31 源码及 APK，设备已关闭并确认离线。API35 unavailable 报告 SHA-256 `b86e7d62f6978d9c72d7575754501a0db2ab0738f8ba700894b84e860aa437c4`，identity SHA-256 `7cd60fb602d60cfc5b49f52b981f4e0086dcb581e77e88bd50b729bb7fd1d2e6`，runtime audit SHA-256 `678ba4a21e579a55924649f6eb4fab947aa8f773ce30ce807710bca9cf30563f`；报告保留 warning_count 75，完整目标日志审阅及最终裁决尚未完成，不预先将其归零。

正式自然持有条目 UI 报告 SHA-256 `63e8b7774b46c65c8da3a6d318d8a2ceff1cfee8b11043e39d8901a1c334ed65`，231 项完整文件 inventory SHA-256 `db988bcb21a7c83aa134a1365f6aa749b5b36894cfc0d27b1e562188949ebcca`，94 条设备命令全部保留。实际一次 Copy、一次 Back 隐藏、一次点回原编辑器后，由真实剪贴板历史的唯一持有条目进入 Edit，加载虚构标记并自然显示九键；应用与 service 同 PID 3243、同一 service 实例及 boot，Cancel 后原持有条目哈希不变。两张截图起点间隔 4.834782 秒，报告稳定时长 4.658384 秒，两者分别记录且均超过原一秒要求；并非先前仅作诊断的记录。

自然 UI 独立只读审阅 `build/t07-privacy/reviews/candidate31-api35-natural-owned-ui-independent-review.json`，SHA-256 `e1baac72ff280c742e56a12c78eac8ee51bc95f19f14577d14e40ed943844fa3`，在该门禁范围内未见 Trime 崩溃、ANR、BadToken 或虚构正文泄漏，P0/P1 为零。仍保留 4 条 BackDispatcher、2 条 SQLite、1 条 InteractionJankMonitor 权限诊断，以及 2 条 FrameTracker 动画超时诊断；它们不是零警告，`warning_free=false`。scope 标记前另有属于退出 editorfixture 窗口的 system_server DeadObjectException，未计入该自然 UI gate 范围，完整矩阵目标日志仍须独立审阅。审阅写作时其余矩阵阶段尚在进行，这份专项通过不独立构成稳定发布批准；完整六阶段通过由后续正式矩阵摘要证明。

candidate31 的验收尝试记录：API21 完整矩阵 `final1` 与真实系统 SAF `final1` 均完整通过；真实 SAF 的 roundtrip、restart、revoke 按同一候选、同一设备启动及授权目录的有序链执行。candidate30 的真实 SAF `final10` 失败后修复子项枚举并重新冻结 candidate31，未沿用 candidate30 的构建或任何运行通过结果。其他门禁按其独立章节记录。

API35 host GPU Direct Boot `final1` 的冷重启、锁定态启动与 Rime 就绪检查通过，未观察到 ANR；但第八阶段的隐私菜单观察脚本错误遍历了窗口包装，未找到实际可点击的 Frame 按钮（`got 0`），完整 gate 失败。该轮 PIN 已清除、模拟器已离线，部分通过不能计入发布验收。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final1/summary.json`，SHA-256 `dc1461869380a50e3ea7663a37936db9451fc3a1afb1e9496e9adadb5b39bc80`。随后仅修复 ignored 门禁的窗口包装遍历，原有 81 项加新增 11 项契约自测共 92/92 通过；门禁 SHA-256 `b7284fdc69f472c995c0db960d2b780a22cf96b045eae1e1f432ae431729d892`，产品源码、十阶段、ANR 扫描与阈值未变化。host GPU `final2` 的实际结果见后述失败记录。

API35 host GPU Direct Boot `final2` 在第七阶段再次证明锁定态输入法与 Rime 就绪，第八阶段仍以 `Fresh shell-window hierarchy unavailable: direct-boot-privacy-disabled` 失败。但只读调查确认 12 次观察命令均返回 0 并取得相同键盘窗口，实际并非观察器失效：密码编辑器仍处有效隐私状态，手动隐私开关变化触发正常的 `clearEditorBoundaryState` 并收起菜单，脚本却要求该菜单继续打开。PIN 已清除、adbd 已恢复 UID 2000，模拟器已确认离线；部分通过与清理不算完整验收。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final2/summary.json`，SHA-256 `cf43e6f950b3e41664c732290cbfe189d83f681c12183f2860088cc284cafcc1`。ignored 第八阶段已改为从新的键盘窗口重开菜单，再同时验证唯一的 Enable privacy 标签与设备保护首选项；104/104 契约自测、语法检查与独立复核通过，当时冻结门禁源码 SHA-256 `944d5eab33ec17ea100f3e09d071d23e379d4ede28966b79dcef897c8633cd7b`。tracked 产品源码仍干净且未变化。产品、十阶段、ANR 扫描与阈值未变化，host Direct Boot `final3` 的实际失败见下一段。

API35 host GPU Direct Boot `final3` 的前八阶段均通过，包含锁定态输入法/Rime 及修正后的隐私菜单切换检查；第九阶段因 `Fresh shell-window hierarchy unavailable: direct-boot-pin-entry` 失败，第十阶段未运行，整份 gate 仍不能计为通过。PIN 已清除、adbd 已恢复 UID 2000，模拟器已确认离线。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final3/summary.json`，SHA-256 `4163d7e49263a80f4dc812307b346e7bcf7a7e63f433ae91ba4d3f30209b2db9`。后续事实确认第八阶段的隐私菜单仍打开，第九阶段脚本发送的 `KEYCODE_BACK` 触发实际 `HIDE_SOFT_INPUT_BY_BACK_KEY`，隐藏输入法，导致无法取得预期的 PIN 键盘；此处不是观察器传输失败或产品崩溃。ignored 门禁已改为从新层级点击 Trime 唯一的返回按钮，并固定六项布局源码；135/135 契约自测及独立复核通过，当时冻结源码 SHA-256 `99f8d6c53aca5d11f92f4d920452a6bee8bb6722b6293ac67186dc73ef337937`；产品源码和十阶段、超时、ANR 扫描规则未改变。`final4` 的实际失败见下一段。

API35 host GPU Direct Boot `final4` 的前八阶段通过，修正后的 Trime 返回按钮与真实 PIN 数字输入路径均已执行，但第九阶段以 `TimeoutError: Trime PID handoff exceeded seven seconds` 失败，第十阶段未运行。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final4/summary.json`，SHA-256 `17ae491d4b659b89021356d7f0d9b5389828fcea5009bb2ccf91f64d0ce2cf08`；PIN 已清除、adbd 为 UID 2000、模拟器已确认离线。日志时序显示旧 Trime 收到 `USER_UNLOCKED` 后 0.661 秒正常退出，广播发出至应用接收约延迟 2.375 秒；脚本在起点后 2.545 秒已观察用户解锁，但串行输入法/fixture 查询直到 5.489 秒，AMS 又在旧进程退出后安排 1,000 ms 重启，最后进程列表虽见新 PID 2887，却已越过原七秒期限。未见 native 五秒或七秒兜底触发、Trime Fatal 或 ANR；该端到端失败不能称为单纯观察器误判、唯一产品故障或通过。ignored 门禁只优化三处独立只读查询：以双 `Popen` 并发执行，主线程保留原始记录；原七秒起点、单调用 1.25 秒、触摸 6,500 ms 上限、变更操作顺序与十阶段均保持。原 135 项与新增专项 34 项自测、root 及裁决工具独立复核通过，冻结 gate SHA-256 `03f6e6276281a98fe1570ee447502bbddd874fe07b6626f3b0a265fc9daefdd0`、测试 SHA-256 `34eb9a1f4fdc608657c5c813b27e6516df31a000111a38637d5a99d897281067`。`final5` 的实际失败见下一段，最终证据占位继续保留。

API35 host GPU Direct Boot `final5` 的前八阶段通过，第九阶段失败，第十阶段未执行，完整 gate 未通过。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-direct-boot-hostgpu-final5/summary.json`，SHA-256 `52b6446893ee2416269093d33cf987862f271b5ca5611823cf2cfdcab0f2072f`；实际末尾错误为在剩余约 0.257 秒预算内读取 `ps -A` 超时。PIN 已清除、adbd 为 UID 2000、模拟器已确认离线。最后七次进程采样在 5.228–6.692 秒均未找到替代 PID，失败后 9.384 秒的最终 PS 也没有替代 Trime 进程；旧 PID 1294 在收到 `USER_UNLOCKED` 回调后 0.205 秒正常退出。默认输入法仍是 Trime，但连接记录为 `mHaveConnection=true`、`mBound=false`、`mCurMethod=null`，未见 AMS 安排一秒重启，普通 fixture 的 focus/show 请求晚于原七秒期限。因此不能归因于唯一产品故障或纯观察器误判，也不能将旧进程正常退出视为交接成功。并发只读链的八组并行查询、16 条原始命令及 32 项输出哈希均通过核查，保留原七秒门禁失败结果。

该轮窗口移除后记录 1,099 次触摸注入失败。只读源码调查发现 Android 15 的 `sendSwipe` 没有 sleep 节流；ignored 门禁已将原无节流 `shell input swipe` 换为 60 Hz、6,500 ms 的明确触摸驱动，完整记录每个 event 的尝试、接受与拒绝。主 gate SHA-256 `f6e8982ea187a52ae89999a504708e3b59c97c931bdf81265120d7c46c33f640`、测试 SHA-256 `6f44bdc7ebbe800e74f9db362323f1cb95918fc086c99f794100fb6aedac30ec`；原 135 项及专项 68 项自测、独立 68 项复核通过。两个独立构建所得 JAR 与 identity 相同，JAR SHA-256 `11de4eda746baf587def6b9ee5bd7144cd8c4b575e6c6f5746b1bf7fa591202f`、identity SHA-256 `fb28946b315779052c904bd7215ace06826b02573194c926613cf3aa5cb3010f`；完整七文件驱动在运行首尾固定绑定。原七秒期限、十阶段和一次有序 Latin→Trime/fixture 变更规则保持。工具 freeze 仅证明宿主版本固定；压力设备 `final6` 随后失败，见本历史部分开头的终止记录。

独立自然 PIN wrapper SHA-256 `300a6409d904ef4920f5f16c7ae6180119f52cc4916d56197d06f25d9e4954b8`、测试 SHA-256 `8c78c6d63f387bf7970f2f28ccc8cc8e7fe99807da1c980591b637503e540ad5`；28 项自测及独立 28 项复核通过。该门禁不使用输入法重选、压力触摸、force-stop 或其他救援；保留十阶段、实际 PIN 提交前起算的七秒期限、用户解锁、一次普通编辑器启动及新 PID 证明。第十阶段还要求通过真实唯一 Delete 将 `UNLOCKED` 变为 `UNLOCKE`，检查普通连接、native、CE 与窗口状态。自然 PIN `final1` 随后实际失败，压力与自然两个门禁均未形成通过证据，`candidate31 未完成并已作废，不计 candidate35 通过` 继续保留。

API35 软件渲染三轮冷启动 `final1` 未形成通过证据：首轮点击 DONE 前初始部署已结束，因此未满足“部署仍运行时点击”的前提；这次失败未发现应用崩溃或包名 ANR。第二轮安装期间以 SIGINT 停止，三轮未完成；`round-01` 与 `round-02` 均已确认模拟器离线。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-cold-gate-final1/summary.json`，SHA-256 `58c809e7be26c7870363c5a701ef5ca4194f5a080f684a8c8deabef3bb28adaa`。这份未完成结果不可计入发布验收，冷启动门禁当时未作源码更改。

随后修正 ignored 软件冷启动驱动并完成独立复核，保留真实默认 Latin 输入法直到 Setup 第三步的系统输入法选择器，避免准备期过早激活 Trime；门禁源码 SHA-256 `9eaff9a44724ccfdefb6f2ab95456110f1f96340da866b6e194b6a6124fe0759`，原 20 项加新增 10 项契约自测共 30/30 通过，Python 编译通过。全新 wipe、DONE 时初始部署仍运行、MainActivity 小于 5 秒 top-resumed、连续 2 秒焦点及 ANR 检查要求均未放宽。`build/t07-privacy/runtime/t910-candidate31-api35-cold-gate-final2` 随后失败/中断，详见后述记录；该版 30/30 自测不能替代实际运行。

软件冷启动 `final2` 首轮在点击系统选择器选项后约 59 ms 立即读取设置，仍读到 Latin 而失败；实际系统日志表明受锁竞争影响，约 1.304 秒后才完成选择 Trime。这是观察时序问题，该轮未见产品崩溃或包名 ANR，仍不能算通过。第二轮启动期间以 SIGINT 停止，两个模拟器均已确认离线，三轮未完成。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-cold-gate-final2/summary.json`，SHA-256 `6d0d633d5c1f5ba83124da4a144223b55b6c335168dd3a501c3f9860bb0e098d`。

ignored 冷启动门禁随后增加最长 3 秒的实际选择状态确认，只等待系统选择器完成，不重按选项、不通过设置命令改默认输入法；36/36 契约自测、Python 编译及独立复核通过，源码冻结 SHA-256 `ab315b410aef3e44e4fd89aa709b9ac5f75aa83603e8c56fe313614ed9f792c5`。原有部署中点击 DONE、MainActivity 时限、持续焦点、全新 wipe 与 ANR 断言未放宽。软件冷启动 `final3` 随后完成三轮通过，实际结果及冻结摘要见前述冷启动章节；host Direct Boot `final3` 至 `final5` 均在第九阶段失败，新的触摸压力设备验收与自然解锁回归仍待完成。API35 host 完整矩阵 `final1` 至 `final5` 的实际失败见后述历史记录，正式 `final6` 已完整通过；Direct Boot 两门禁及最终裁决未知，相应占位继续保留。

API35 host 完整矩阵 `final1` 的 9 项其他主探针通过，但 `clip` 在 `ClipEditorProbe.kt:68` 失败，`t02` 及后续导航、生命周期和故障降级尚未运行，整份矩阵失败。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final1/summary.json`，SHA-256 `100852d14e51971b1f80e8b91ffd9d60b8a46d1566871f883408c98ab7a31536`。原始 `clip` 重复诊断一次仍失败，系统连接记录出现 `DEAD`，新目标 PID 没有输入法 Service，而实际 ClipEdit 编辑连接存在；未见 FATAL、ANR 或测试正文泄漏。C29 与 C31 的 Clip Activity、探针和 manifest 摘要相同，本次未修改产品源码或重建 APK，不能把部分探针或诊断结果当作完整矩阵通过。

同一 C31 的真实用户路径诊断从虚构 fixture 复制内容、打开剪贴板历史并长按该实际持有条目的 Edit 进入编辑器，加载精确标记；应用与输入法绑定同一 PID 1965，input shown/start/view 均为 true，边界稳定 2.189871 秒，两份截图各有 35 种量化颜色。通过 Back 隐藏输入法再 Cancel 丢弃未保存缓冲，原历史条目未修改。诊断文件 `build/t07-privacy/diagnostics/candidate31-clip-binding/real-owned-ui/natural-ui-diagnostic.json`，SHA-256 `9aaaa7816e93c7ff5e0f1dadd4e772d48a28902a63cc49a82acd07d71056049f`，其中明确 `diagnostic_only=true`、`gate_acceptance=false`；这条诊断不能替代发布验收。

ignored 受控绑定 wrapper 的 29 项契约自测和独立复核通过；wrapper SHA-256 `7251cba679b9ccbf22f52c8d1604ff6da13f3569839fb52e1af59e453f672496`，batch SHA-256 `ecf6aa41ca3f79f98ac429b468ac4c299cb90ad3ef1a202028b52ceda3c14479`。它在 instrumentation 后明确执行一次 Latin→Trime 的受控准备，不能称作自然首次显示。独立真实持有条目 UI 门禁须先于原五阶段及 11 项主探针在同一 fresh boot 运行；工具自测不等于设备通过，冻结 C31 产品源码与 APK 不变，尚无发布文档提交或资产 freeze。

## candidate31 历史：未计入 candidate31 发布的失败或历史结果

API35 host 完整矩阵 `final2` 在独立自然 UI 第一阶段之前的 `select_t9` 冷启动准备就失败：六次捕获仍是 `Preparing Chinese input` 数字降级键盘，正常 Theme KeyboardWindow 约在 helper 失败边界 13:17:22 才附着，比最后一次捕获晚约 2.2 秒。自然 UI、主探针及后续阶段均未运行，不计部分通过。摘要 `build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final2/summary.json`，SHA-256 `308d460b2e3bd18d008eede45af3801ccf8dc32881e9d7e299b3516e2d87119b`；模拟器已确认离线。随后两次 no-wipe 诊断的观察器均以 137 退出且 stdout 为空，未取得证明，不能计产品通过或失败；没有 guest 失败日志或 host OOM 记录，137 的直接原因未知。实际命令漏列 `uiautomator.jar`，源码所需类已核实，该 classpath 事实已修正，但不能据此断言 137 的原因。

自然 UI helper 增加明确的 framework classpath 路径绑定，原 30 项加新增 3 项自测共 33/33 通过，root 与独立复核通过；helper SHA-256 `61eff729dab3b22be17b3b1efee8d2d465a2b5f22e411ad89b93cfd6ea0bb70f`、测试 SHA-256 `e60afbb1d4a5e1dd97bea03860845ed9465465b47c863d4177114b3e0be029b7`，schema SHA-256 仍为 `c2f15708290c15de5f78531d0fe49a03204752ac5bf46e0901773adc84d06ae5`。

矩阵调用方新增最长 90 秒的独立冷启动准备，核对同 PID/boot、native 就绪事件晚于准备标记、真实普通键盘，并要求至少相隔一秒的两帧稳定窗口；不放宽独立软件冷启动性能门禁或原 `select_t9` helper 的期限，不重试。首版 17 项测试的独立复核发现末尾源码哈希检查可能跨过总期限后仍通过，已补末尾 deadline 复核与 timeout 原始证据保留；最终 19/19 自测、root 与独立复核通过，实际 525 项源码与 18 项固定路径 preflight 通过。当时冻结矩阵 SHA-256 `6db1e37135421e5fa9508b64de489896cc20238b18857fcd706d1b6d35e09c84`、测试 SHA-256 `e6540ae36362b3468d25cefb453717417a79e7cca2829a4eaf25c45696f32bac`；`final3` 的实际失败与后续工具最小修正见下一段。

API35 host 完整矩阵 `final3` 的调用方冷启动 UI 准备通过，用时 21.656 秒、两帧稳定 4.557 秒，九键前置也通过；这只属于准备期部分结果。冷准备报告 SHA-256 `9b8495e03877356a463a0d56dc5e37a98e58a461a2d4cad1d347223cdbe207c7`。首个自然 UI 门禁随后在 `copy-confirmed` 失败：fixture 源节点身份和文本正确，但复制成功状态被普通输入法裁剪，未取得所要求的可见状态。自然门禁报告 SHA-256 `552a81f347b1e3cae67a59a335468900044c1c29e83e24c5ecba2bbf0416ef9b`，完整失败摘要 `build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final3/summary.json`，SHA-256 `a785715e71becf6f1a226086f3e4073011aa899f2a9cd3a53697f5cf1896d0ae`；主探针及后续阶段未运行，模拟器已确认离线，不计矩阵通过。

自然 UI 工具最小修正为一次 Copy 后按一次普通 Back 隐藏输入法，从新真实层级读取成功状态，再点击一次原编辑器恢复九键，同时保持同 PID、token、boot、默认输入法与编辑焦点绑定；没有重复 Copy、重绑输入法、主动 show、instrumentation 或 fixture 修改。Cancel 状态不可见时返回 `None`，不伪造成功。原 33 项加新增 13 项共 46/46 自测、root 及矩阵独立复核通过；helper SHA-256 `76cdeb951f747563ec3d49a6842251fde0219d6ca80f3a14830ee3cba0a5309c`、测试 SHA-256 `2f717103a3f32ede699cf4b2efdaca3c6a2ae908bcfca2dc403bb5b36c88a8d3`，schema `c2f15708290c15de5f78531d0fe49a03204752ac5bf46e0901773adc84d06ae5` 与 fixture APK `dc75f5129585387188c3d42515c76054e8f7420fba250a20f0fd60de4a2228a9` 均保持。矩阵只更新两个对应的源码固定哈希，19/19 原契约测试及实际 525 项源码、18 项路径 preflight 通过；当时矩阵 SHA-256 `7d71e865b0b364d8dfcf848e5a773ce8077bce9316ccf2100347ea18cde78f9c`、测试 SHA-256 `e6540ae36362b3468d25cefb453717417a79e7cca2829a4eaf25c45696f32bac`。`final4` 的实际失败与后续修正见下一段；产品源码及 APK 未修改。

API35 host 完整矩阵 `final4` 冷启动 UI 准备用时 19.562 秒并通过，九键前置也通过；冷准备报告 SHA-256 `84426fbf621b8aeaa5dd0d0ed9cb23b9e6776b48ee796279a17da09d41ad6ed6`，仅作部分准备证据。首个自然 UI 阶段在 Copy 之前失败：脚本从 windows 子转储读取 `mCurrentFocus`，该输出没有此字段，但 activity 转储中的四项焦点证据均唯一且一致，windows 中唯一的对应窗口也正确。自然报告 SHA-256 `168b35867d44caba550bc544c991d8d541ef8deee94ea42fa6b03901761b8fd9`，失败摘要 `build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final4/summary.json`，SHA-256 `2e0e1f1744b413c641673448a1a591301e14304099910c225080a0adb6351dc9`；运行探针及后续阶段未执行，模拟器已确认离线，整份矩阵失败。

自然工具改为严格联合解析 activity 与 windows 的一致焦点身份，不放宽唯一性、PID/token、默认输入法或真实编辑窗口要求。52/52 root 与工具 owner 自测通过；helper SHA-256 `a1f5b6195e382b9d2f92e8502342532d94113ddeed411aabe49c779626abdb1c`、测试 SHA-256 `8fcf25a98964364d2126da923846b51eb08772ae95b5d72a4736c56dc658f724`，schema 保持 `c2f15708290c15de5f78531d0fe49a03204752ac5bf46e0901773adc84d06ae5`。矩阵更新相应源码绑定后，原 19/19 root 自测及实际 525 项源码、18 项固定路径 preflight 通过；当时 SHA-256 `437ee4ef24c61ef3728ba1290ae76b89a3fd32f9c8af887d103f6486fc4acbbc`、测试 SHA-256 `e6540ae36362b3468d25cefb453717417a79e7cca2829a4eaf25c45696f32bac`。`final5` 的实际失败见下一段；准备通过和工具自测不能替代完整矩阵或最终裁决。

API35 host 完整矩阵 `final5` 的冷启动 UI 准备用时 26.440184 秒、稳定窗口 4.997357 秒并通过，九键前置也通过；冷准备报告 SHA-256 `c6f6c84370f9788b3d34f720fe999c90f725b54905d3df65a553fe4cd34836fc`，仍仅作准备期部分证据。首个自然 UI 阶段在 `copy-confirmed-hidden` 失败，原报告不修改：Copy 已真实成功，manager 的 `mInputShown=false`；service 的七项实际状态 `mWindowVisible`、`mDecorViewVisible`、`mDecorViewWasVisible`、`mInputViewStarted`、`mCandidatesViewStarted`、`mShowInputRequested`、`mInShowWindow` 均为 false，输入法窗口为 GONE、无 surface、不可见。但私有缓存 `mIsInputViewShown=true`，旧脚本错误要求该缓存也为 false，实际失败为 `Trime service visibility differs: mIsInputViewShown`，不能解释为产品隐藏未完成。自然报告 SHA-256 `69006f2297c74511fbba32b67377c21b182d61be6d129d26f44758e85238d4ce`，完整失败摘要 `build/t07-privacy/runtime/t910-candidate31-api35-hostgpu-final5/summary.json`，SHA-256 `a4663fa99f056dd8a0e09c1f5f014f2ba3e6ed140bd294f58b219a5fae3ace56`；运行探针及后续阶段未执行，模拟器已确认离线，完整矩阵仍失败。

[Android 15 r1 的 InputMethodService 源码](https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/core/java/android/inputmethodservice/InputMethodService.java#2361)区分私有显示缓存与实际 decor 可见性：`updateInputViewShown` 在 decor 可见时更新缓存，`hideWindow` 不清理该缓存，而公有 `isInputViewShown` 返回 decor 可见状态。最小 helper 修正对隐藏态严格检查上述七项实际状态为 false，并把唯一且合法的私有缓存布尔值作为原始证据记录；显示态原严格规则保留，不添加动作、等待或重试。56/56 owner 与 root 自测通过；helper SHA-256 `9b8de6ed5c894576150471457a2b4cd015f91fb712ebdc9c3e9bef6a238c239e`、测试 SHA-256 `51391061e0cbdda3d771ed947d12b7c2a6d80825c1522782f424323c1162ac5e`。矩阵仅更新对应两个固定源码哈希，19/19 原自测及 525 项源码/18 项路径 preflight 通过；当时矩阵 SHA-256 `0bb17ce9e2e1febfcc89aebdf0affc9fd2e6b63fb932a3f24691d1fb7ca69bf9`、测试 SHA-256 `e6540ae36362b3468d25cefb453717417a79e7cca2829a4eaf25c45696f32bac`。完整 host 矩阵 `final6` 随后正式通过，见完整矩阵章节；Direct Boot 压力/自然 PIN 设备门禁随后均失败，C31 未形成最终发布裁决；不计当前 C35 通过。

candidate30（实现提交 `4ff9e1866bc36869f1bc7dd2f5ebd7ea4df5cf71`）的构建和静态验证通过，API21 `final2` 完整矩阵也通过；历史摘要为 `build/t07-privacy/runtime/t910-candidate30-api21-final2/summary.json`，SHA-256 `242f6431ecccf6c7231e001320e3a9e23c27d6f853f2f776be891eb6d2193515`。其 `final1` 因用户中断未完成。

但 `build/t07-privacy/runtime/t910-candidate30-api21-real-saf-final10` 的真实 DocumentsUI roundtrip 失败：生产 StorageAccess 文件替换、持久授权和读回先通过，导入历史保护随后在 `SafCompatibilityProbe.kt:286` 失败。定位发现 storage-kt 1.2.1 的 `list` 只返回首个子项，导致其余文件被漏掉。candidate30 因此作废，以上通过结果均仅作历史记录，不能计入 candidate31 发布。

candidate30 的 API35 Direct Boot `final1` 也未通过：`run-as` 尝试访问尚未挂载的凭据保护目录，无法完成锁定态资源读证据。门禁随后改为短时 root 只读设备保护路径并恢复 shell，仍须在 candidate31 重新运行。candidate29 及更早候选的矩阵、冷启动、SAF 和裁决证据同样不能计入 candidate31。

candidate31 的 API35 软件渲染 Direct Boot `final1` 未通过，真实启动 ANR 发生在 Android 创建应用类加载器期间，主线程位于 `DexFileVerifier.CheckHeader → libz.adler32`，尚未进入 Trime 调用栈。主线程 CPU 运行 1.432 秒、等待调度 28.274 秒，同期还出现 SystemUI ANR 与 WindowManager watchdog；这些证据支持模拟器冷启动调度受压的解释，但不能把失败改为通过。原失败、PIN 已清除和模拟器已离线的清理事实保留；恢复 trace 为 `build/t07-privacy/diagnostics/candidate31-directboot-anr-recovery/trime-startup-anr.txt`，SHA-256 `dfb4b22215569cc3dadbe848c397338bdfa3e701b14dacefa6c6a4af0341e3d2`。

Direct Boot 使用明确绑定的 host GPU 执行独立冷重启验收，前五次 host 尝试均失败，压力设备 `final6` 与独立自然 PIN `final1` 均已失败，本候选作废；summary、preflight 与实际模拟器命令必须同为 host。所有十阶段、原有超时、锁定状态及 ANR 扫描要求保持原样，诊断恢复启动不算验收。三轮冷启动门禁仍使用软件渲染；host 结果也不能代表软件渲染或物理设备已经通过。

## candidate31 历史：Direct Boot 与真实 SAF 发布门禁

API35 Direct Boot 压力与独立自然 PIN 两门禁：candidate31 未完成并已作废，不计 candidate35 通过

API21 真实 DocumentsUI SAF：通过；可见系统 DocumentsUI 创建并选择独立目录，roundtrip、进程重启后的授权/数据恢复和撤销授权后的安全降级三阶段均通过；`build/t07-privacy/runtime/t910-candidate31-api21-real-saf-final1/summary.json`，SHA-256 `08803c60abb549c9b319e03f704e04a652aefb5d76ea6bda9e85f024281bed85`。阶段报告 SHA-256：roundtrip `fbc45bd000d5215708bd546f58203a4b0ae1d783d07572afbb152828f0e19131`、restart `c32a3cf72df8b20e25935520932a18b537bb096769b9a582daa715bc593ecdbe`、revoke `6d23fbcb2704389af0e8ea436372eb5a9278f09cbbb5b95177cf731f81fefe5f`。roundtrip 与 restart 各保留 65 条目标进程诊断，warning_count 分别为 68 与 67；revoke 的目标诊断为 0，warning_count 为 3。独立专项日志审阅已通过且无功能发布阻断项；130 条项目诊断为受控的 blocked 目标目录冲突，其中全量导入 68 条、主题导入 62 条；另外 8 条为 6 条 SQLite WAL 恢复与 2 条 ART 暂停提示。三阶段 warning_count 合计 138，原始记录均保留，不能改写为零警告。审阅报告 `build/t07-privacy/reviews/candidate31-api21-real-saf-log-review.json`，SHA-256 `3f1848ff0da06f758da695053cd54fdc0b2a5fb38b1145fa7b202f0093159445`。

真实 SAF 完整设备 backlog 还记录一次非注入的 `WrappedRunnable` 排队 6,280 ms：系统选择器返回所选目录后，Trime 进入 `fullCheck` 配置部署与进度窗口，部署结束后 Rime 响应恢复。Latin 切回 Trime 早约 7.8 秒且当时立即响应；延迟时段未见 `onStartInputView` 或活跃按键/提交请求，也未见崩溃、ANR 或虚构测试正文进入目标进程日志。匿名 runnable 可能对应 `bindEditor` 只是按时间推断，精确任务及编辑器身份未被证实；本场景也未证明任意并发编辑器不受这段部署延迟影响。因此该 6,280 ms 观测作为已解释的性能限制保留，不能归入故障注入，也不代表候选延迟预算达标。

Direct Boot 的最终证据须同时包含压力和独立自然 PIN 两个通过摘要，并绑定 candidate31、门禁脚本、矩阵驱动与 shell 窗口观察器，覆盖真实凭据锁定的冷启动、锁定态输入法窗口、设备保护首选项、解锁时进程交接、首选项恢复、日志及凭据/模拟器清理。先前候选的 final1–final8 是失败或中断的验收尝试，均未计入 candidate31 的通过证据；具体归因保留在各次记录中。

API21 SAF 的最终证据需要通过可见 DocumentsUI 创建并选择独立目录，验证持久读写授权、往返读写、进程重启后恢复，并在撤销授权后安全降级。隔离 provider 探针不能替代这个真实系统选择器门禁。

上一契约的裁决器 44/44、发布工具 16/16 离线自测与独立复核通过，无复核阻断项；这不等于实际运行裁决通过。最新工具正在绑定更新后的 Direct Boot、自然 UI 与矩阵脚本，最终计数及冻结 SHA 尚待提供；上一契约的结果不能代替最新工具自测或实际设备验收。

## candidate31 历史：运行日志裁决

C31 原计划裁决须绑定其 API21 与 API35 host GPU 正式矩阵，但因后续失败未形成发布通过。其检查范围包括 `FATAL EXCEPTION`、`ANR in com.osfans.trime`、`BadTokenException`、`WindowLeaked`、`Fatal signal`、native tombstone，以及私密 fixture 在应用 PID 日志中的出现。这些历史候选的文件数、warning 数及裁决摘要不作为当前 C35 证据。

candidate31 未完成并已作废，不计 candidate35 通过

candidate31 全部运行 warning 未完成最终裁决，已有原始记录保留

平台与系统日志不能静默删除；主动故障注入诊断需要逐项分类，不能把“没有未解释阻断项”改写成“没有警告”。应用 APK 已保留 META-INF 提示，`warning_free=false`。

裁决报告及证据快照：candidate31 未完成并已作废，不计 candidate35 通过。

## candidate31 历史：兼容性与未覆盖范围

- Android 5.0–6.0 无法读取其他应用只在现代 `ClipDescription.extras` 中设置的敏感标记；Trime 自己创建的敏感剪贴板仍受保护。
- 已启用方案遍历 guard/历史路径，临时验收方案验证标准 Lua 组件与生命周期；这不构成任意自定义方案、第三方原生代码或绕过项目接口插件的独立沙箱保证。
- 应用沿用 `com.osfans.trime.debug`、debuggable 变体和既有 debug 证书。GitHub 稳定版状态不等于生产签名或 release/R8 变体。
- 设备验收使用 API21/API35 x86_64 模拟器；ARM64 只有构建、签名、资源和静态对齐证据，没有物理 ARM、OEM 编辑器或物理 16 KiB 页设备结果。
- 模拟器时延与日志是本次观测，不代表物理设备性能上限，也不能替代 T08 真机预算。

## candidate31 历史：发布附件

Release 只上传 ARM64 应用 APK、x86_64 应用 APK 和包含全部固定子模块的递归源码包，共 3 项手动附件。测试 APK、验证 ZIP、校验清单、身份 JSON、测试报告与运行日志不上传；GitHub 自动生成的 Source code (zip/tar.gz) 入口保留。

源码归档以发布标签为根，并包含全部固定子模块。归档摘要由发布工具在归档外记录并在上传前后复核，不写入归档自身包含的本文档。

C31 准备发布时曾再次核对远端固定上游 `e9b21960abd8c6f6aa3864991117f6a5adc48318` 已全部合并，当时源码比较为 ahead 80 / behind 0；fork develop 仍为 `a276c9775429b781fe2294a4c59aa1e858e062b8`，当时尚未推送候选。历史远端清单复核 `/tmp/trime-t910-candidate31-remote-inventory-recheck.json`（2026-10-08T06:19:28.462844Z），SHA-256 `ac279909e1d0d6e8ff8d039ca86067818dab75b5b5f9c89691e99f30b26cfbcd`：8 个既有 Release、16 项手动附件，12 项 APK 与 4 项源码包，没有需删除的辅助附件或 t9.10 草稿记录，Latest 仍为 `v3.3.13-t9.9`。完成全部 C35 验收并实际发布后，将重新枚举全部 Release，确认旧资产名称与摘要未变、新 Release 恰有 3 项手动附件，并验证 `v3.3.13-t9.10` 为非草稿、非预发布的 Latest 稳定版。


## C35 历史：发布边界与远端基线

本节保留 C35 原拟发布计划和当时远端核对；该计划未执行。C36 发布必须采用自身来源、APK 与全部新验收结果，见本文开头的 pending 表。

当前 C35 发布前已再次核对固定上游 `e9b21960abd8c6f6aa3864991117f6a5adc48318` 仍已整合；认证远端清单 `/tmp/trime-t910-candidate35-remote-inventory-recheck.json`（2026-10-08T09:37:47.453405Z），SHA-256 `618ce5f3c95bef46fe45a42b4c6dcb8037ca01327d133a284b6dbc40901fb4aa`：8 个既有 Release、16 项手动附件，12 APK/4 源码包，没有额外辅助附件，也没有 t9.10 草稿；Latest 仍为 `v3.3.13-t9.9`。最终发布前后还须重新核验；当前没有公开变更。

C35 只有全部全新设备门禁及最终裁决通过后，才会把恰好这六份文档写回仓库并完成文档提交、源码归档、推送和稳定版发布。APK 内实现 SHA 与包含最终文档提交的递归源码包根 SHA 分别记录。手动公开资产只允许两份应用 APK 和包含固定子模块的 source.tar.gz；测试 APK、工具、报告、日志与校验/身份清单保留本地，GitHub 自动 Source code 入口保留。
