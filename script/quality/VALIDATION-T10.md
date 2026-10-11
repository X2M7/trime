# T10：个人词库与候选管理验证记录

状态：**本轮稳定版验收通过，线上状态以 Release 为准**。当前实现源码 `921f713266a497e29b2381d7d7471eae8ee5a974`，包含上游 `8545278b9efccaaf5b222dd499f5954ebcf87269`。版本 `3.3.13-t9.13`／versionCode `20261118`。本轮 Build17、Host13、Static13 及 API21-run13／API35-run7 的完整六阶段功能、Release GUI、正常停止、逐行日志与控制台审阅均已闭合；历史分项不继承。

## 实现范围

- 默认全拼和九键共享 `luna_pinyin` 学习库与 `custom_phrase.txt`；字母／数字短语编码独立，不自动转换。
- 原生用户词库提供导入、导出与备份恢复；常用短语支持编辑、导入导出与恢复，修改前精确备份。portable Rime导入合并记录，精确文件备份恢复替换受保护文件。SHA校验、事务journal与失败物理回滚，未完成操作恢复后才清journal；保留最近三备份并保护pending所需备份。
- 维护由单线程JNI与互斥会话生命周期管理，关闭缓存后操作，恢复schema/options/privacy。真实备份迭代器先于DB Close销毁，正常Release与Debug均重复三次原生备份；原生 checked restore／物理回滚由 Debug 验证，正常 Release GUI 另验证短语文件恢复。
- 置顶按schema／rawinput隔离，512规则／前144候选查找；恢复排序不删除学习，隐私模式不应用／修改置顶。忘记请求移除已学习记录；无可编辑学习记录时可能提示请求失败，不永久屏蔽系统词、不删pin，也不将native Boolean等同全部系统词持久删除保证。
- 首选突出、可配注释、默认关闭的横向候选滚动、长候选全文；触摸持有snapshot与索引，重绑或native旧snapshot拒绝，负索引双层拒绝。失败操作使用不含输入内容的输入法提示框；保留成功提示与无效window token的回退，并在有效新动作、重置及detach清理旧提示。提示窗口保留编辑器焦点，真实编辑器更换触发清理。
- 展开记忆同scope全局index与signedpixel，不缓存View／文本／PagingData；重建真实对象、partial首行、两次ordinary relayout、native exactonce标准保持。测量bind用peek，实际可见范围提示及missing锚点单次边界加载；postscroll验证真实pixel，actuallayout/page触发一次settled PreDraw，无timer／自arm／强制循环。PAGE48、initial48、prefetch16、maxSize目标144为Paging best-effort预算，非普遍硬峰值。
- 上游8545 Shift双击锁定整合保留显式DOUBLE／LAZY优先；无显式动作的hook-off二次tap仍CLICK。仅builtin+hook+SHIFT mask幂等锁定registered mShiftKey，已锁不toggle off，原modifier／longpress／隐私/editor行为保留。新增真实DOWN/UP测试源码及本轮编译通过，API21-run13 与 API35-run7 实际模拟器回归均通过，未代替物理设备验证。

## 本轮验收结果

| 范围 | 结果／实际证据 |
| --- | --- |
| Build17 frozen源码、before/after、fiveAPKs | 通过 — 842ccef97a8ca51c71271859a9122e8651f90837de4e93f35c7e90fad537d3be／579 个源码绑定／5 个实际 APK 逐字节核验（Debug 两 ABI、androidTest、Release 两 ABI；Release SHA 见发布说明） |
| fresh应用JVM | 通过 — 702 项本轮新执行应用 JVM 测试通过，该数量由本轮实际 JUnit 原文核验；build-logic 17 项＋compatibility 6 项为 UP-TO-DATE 缓存结果，未计入 702 fresh |
| Host13 | 通过 — 213 项本轮新执行宿主测试通过／6 suites：quality 128／T08 31／T04 13／T10 7／T05 5／virtual-text 29／579 源码／92 宿主输入，前后 SHA 完全相同 |
| Lint、build、签名／双ABI／min21／DEBUG=false | 通过 — Debug／Release Lint 均 0 issues，构建警告0／ARM64／x86_64 Release 签名检查通过，minSdk21、20261118／3.3.13-t9.13、DEBUG=false、签名警告0／既有证书 SHA-256 `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`（DN 为 Android Debug；Release DEBUG=false） |
| Static13所有新实际body | 通过 — 117 owners／ABI 配对，共 234 当前主体；Shift 2 个主体独立审阅／主体 1474 methods；Shift 42 methods 单列／主体 468 份 stdout／stderr 原文；补充原文单列；保14方面／17角色／29JNI／实际SDK28→API26guard及真实pages/settled callback链；本轮正式静态封存实际通过，未继承旧214／1360 |
| API21-run13与API35-run7 六阶段功能 | 通过 — 两轮正式Release旧9.12学习→新覆盖升级、UID／nativeStopped／三次备份、主6场景7检查、短语全2及配置树／元数据／IME恢复；单独Debug T10／privacy／T08／完整Editor均通过，原驱动wait0与owned stop0。两轮全部原始日志及控制台诊断均完成本轮审阅，实际门禁与发布执行收据保存在归档外 |
| 当前 API21 Release GUI 分项 | 通过 — 本轮 6 场景／7 检查及常用短语 2 检查；原始 XML／PNG、真实点击、失败提示确定／真实编辑器更换清理、展开可读锚点及全部配置／元数据／LatinIME 恢复均核验，child／outer wait0。原驱动六阶段功能、两主题候选、Shift真实DOWN／UP、40rapid／100cycles及owned stop0亦已闭合；4463条W／E／F逐行审阅完成，保留未知调用来源和环境限界。 |
| 当前 API35 Release 升级与 GUI 分项 | 通过 — 正式 Release 旧9.12覆盖升级及三次便携备份、候选6场景7检查、常用短语2检查；主界面与短语脚本均 child／outer wait0、signals为空，全部配置／受保护文件元数据／LatinIME恢复。通知保持禁用时失败提示可读，真实编辑器更换清理旧提示。短语实际32捕获／22操作／1447命令／10停机守卫／15树元数据快照；原短语文件不存在的状态与四个受保护树根（含原本不存在的备份目录）均恢复。Release和单独Debug功能已分别闭合，原始日志逐行审阅完成。 |
| Debug实际行为 | 两API本轮分项通过 — T10 14项／存储21项／privacy16项／T08 8项，两个主题真实汉字／展开全局index与signedpixel／两次relayout／native单次提交，以及Shift真实DOWN／UP、快速输入40轮与键盘显示隐藏100次均通过。原触摸、全局索引与带符号偏移、两次重布局和精确一次提交标准保持 |
| 同轮封存、正常wait0与全部日志 | 功能封存通过 — API21本轮2441文件、API35本轮2864文件均逐SHA／bytes核验，579源码及冻结输入不变、正常owned stop0；两轮各critical／LMK／kernel为0。4463／11620条原始W／E／F全部保留并逐行裁定；完整控制台另审。未裁定日志行0，本轮项目warning0；未知内部调用来源和环境风险继续列明，不等同全部平台已修复 |
| 最终门禁与发布记录 | 本报告冻结本轮已接受的构建、功能与日志范围；最终门禁及源码包装／发布收据保存在归档外 `build/t10/`，不作为本报告待填哈希。[版本Release页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.13)与发布后的Latest查询提供独立在线状态。 |

本轮只运行一个模拟器。API21 保留 1536MiB／1 核预算；API35 使用 3072MiB／4 核，CPU present 为 0–3。fatal、ANR、LMK、超时、内存下限及六阶段／GUI 标准保持，增加资源只是本轮实验配置，不代表已证明全部旧失败根因。

完整模拟器日志包含实际framework、图形与预期负例诊断；每个W/E/F原文行绑定SHA、PID、同轮上下文和原因证据。保留原始行，构建／签名零警告不代表设备所有警告消失。失败cohort不能作最终warning PASS。

## 历史失败与分项

| 记录 | 保留结果与准确范围 |
| --- | --- |
| Build1–4 | 无效负索引大量惰性读取而主动停止；旧容器 TextView 测试编译失败；最终 test APK 缺第二 runner；自有坏链接触发格式失败。各原失败保留，不能当接受 APK。 |
| Build5／6与早期 API21 | 采集 XML／PNG 不同时问题是测试兼容范围；可选横滑布局及展开重建丢锚点是分别证实的产品问题，改为合适 manager 和 snapshot／offset 绑定标量状态。 |
| Build7 | 设备测试冗余 cast 警告，构建失败，无接受 APK。 |
| Build8／run5–6 | modal 外部 editor XML 缺失是采集兼容问题；随后 Debug backup 真正 LevelDB SIGABRT，整轮失败。RAII 让 DbSource 的两个迭代器先于 DB Close 析构，并加入三次真实备份、checked restore 和失败回滚。 |
| Build9／10 | 原错误 CLI 与格式拒绝保留；parentFile 可空调用警告导致停止，停止期间源码变化的 inputs_unchanged=false 如实保存。 |
| Build11／API21-run7 | Build11／Host7／Static7 分项通过；FullEditor 旧 AutoScale 筛选未匹配真实 TextView，第二主题／100 cycles 未执行。4064 W/E/F pending，2193 封存文件，owned wait0。 |
| Build12／API21-run8 | 构建／Host／Static及升级、主 UI、短语和其他 Debug 分项通过，展开 helper 的原三条件合取失败但原 raw 未分清哪项。分开原子状态采样与 overlay 正常退出；4178 W/E/F pending，不改判原失败。 |
| Build13／API21-run9 | 构建／Host／Static分项通过；一次主 UI 外层143未补造wait0，重跑后实际004→003。部分首行必须同时保存全局index与signedpixel；全cohort失败。 |
| Build14／Host10／Static10 | 历史分项通过：702 fresh app JVM、213 fresh host，17 buildlogic +6compat缓存单列，双Lint0、build warning0、579sources；214主体／1360方法／14方面、17角色、29 JNI为当时CODE的静态事实，不能继承给新源码。 |
| API21-run10 | 普通 Release真实旧9.12升级／三备份、主7检查通过；短语2真实hostSIGTERM失败保留，短语3全2从零正常wait0。后续 FullEditor timeout，整轮失败；SettingsCmd PID16619 Binder SIGSEGV critical1 的真实原因未证实，不能豁免或称已修复。2852文件封存，4699 W/E/F pending，owned VM wait0。 |
| editor-stage诊断1 | 专用测试profile的APP_STORAGE配置错误导致RimeUnavailable，后续显式配置APP_STORAGE；不改变产品默认EXTERNAL_SYNC，原setup失败保留。 |
| editor-stage诊断2 | predicate成立与后续sample在不同MAIN dispatch，中间可发生正常layout更新；改为同MAIN满足条件后立即执行原collapse，原有断言及30s／40ms标准保持，不作已证产品bug判定。 |
| editor-stage诊断3 | 真正提前consume pending，但void scrollBy未达到signedoffset，之后再无pending纠正；产品补同scope postverify，未达则rearm等真实page／layout。原失败不改判。 |
| product诊断1–2 | 两主题定位／relayout分项通过后，第二主题preclick失败；同S下全局31→79、readable39→87，真实+48丢页。改测量bind为peek、真实可见访问hint与missing锚点边界自然加载，prefetch16；精确历史hint触发未追踪，不称已证全部内部因果。 |
| product诊断3 | 同scope目标31/-71，实际31/0、pending31/24保留，30s timeout，2002 W/E/F pending。 |
| product诊断4 | 仅3次parent callback，最后目标pixel240→0后pending仍在，29.5s无第4次；2013 W/E/F pending。新增只由真实layout／page事件arm的一次coalesced PreDraw，detach／scope／真实拖动取消，无timer／自arm／强制新帧。内部Flex clamp原因仍未追踪。 |
| product诊断5 | hardware candidate touch之后30s timeout，根因未证，不当已修产品问题；434文件封存、2005 W/E/F pending、critical／LMK／kernel0、owned wait0。该轮未提供展开修复结果。 |
| product诊断6 | 两主题原严格展开阶段通过后，独立private slide-delete fixture要求空editor而失败；2016 W/E/F pending。helper最终严格commit检查后经真实InputConnection仅删自身刚提交区间，恢复原有文字并核同editor状态，原严格条件保留。 |
| product诊断7 | 当前修正的Debug fullEditor诊断实际wait0／normalstop0通过：两主题8阶段、真实native exactonce、40rapid与100cycles；412文件封存、critical／LMK／kernel0。2119原始W/E/F仍pending；不含后续Shift修正与完整稳定版六phase，不能当新完整验收或逐行日志通过。 |
| Build15／API21-run11 | 候选修复已提交daab89d2，上游8545278b实际合并至121ae225，Shift修正提交29e14160。Build15／Host11／Static11分项及 API21-run11 原驱动整轮、owned stop0通过，均只绑定旧源码；不继承给当前 Build17／两 API／最终日志门禁。 |
| API35-run2／run3 | run2 在本轮安装／probe前因 Phone／SystemUI ANR拒绝启动；run3 native升级与三备份分项通过，但安装前已发生的 SystemUI ANR对话框遮住 GUI，窗焦点失败。原 ANR／栈／CPU上下文保留，不能按系统tag豁免或宣称已证全部原因。 |
| API35-run4 | 4 核／2048MiB 轮仍有 3 次实际 LMK，启动门禁失败，0 本轮安装／probe；后续单 VM 改为 3072MiB，不放宽原内存下限与 fatal／ANR／LMK标准。 |
| API35-run5 | 原生升级与三备份分项通过，Observer未声明生产包的精确查询可见性，首次 GUI transport JSON失败、0capture。只增加测试helper manifest精确query，Java／协议／包名／版本保持；原失败保留。 |
| API35-run6 | 查询修复后真实采集及前5项 GUI检查通过；通知禁用时未收到实际失败Toast，原通知抑制日志与失败保留，不以授予通知权限绕过。候选失败反馈改为输入法拥有的提示框。 |
| Build16／API21-run12 | 原前5项 GUI和可读004→004锚点通过，但实际编辑器更换后旧失败提示仍在；12次原XML／PNG／oracle确认新编辑器已出现而焦点仍被对话框占用。Main真实wait1，fulltext／Phrase未跑，配置恢复；整轮失败。 |
| API35-run7 首次短语启动 | 原release-phrase-ui2因预期通知说明遮住设置入口而失败，0检查／12次原捕获及恢复记录保留。记录普通Cancel操作后，同一冻结Phrase15脚本在新release-phrase-ui3完整2检查通过；通知仍禁用。取消后原捕获先显示设置入口，再进入常用短语和真实编辑器；未修改产品、脚本原断言或授予通知权限。 |
| 当前 Build17／source921f | 新失败提示保留编辑器焦点，利用既有重置／detach生命周期清理。Build17／Host13／Static13及两API本轮正式Release主7＋短语2分项已实际闭合；API21和API35原驱动六阶段、正常停止及4463／11620条同轮日志审阅均通过；不继承 Build15／16验收。 |

## 验收边界

历史SettingsCmd SIGSEGV原因仍unknown；新轮critical0只能说明新轮，不证明旧fault已修。真机候选P95、Java／native／graphics／总PSS、长期驻留／首次部署、物理ARM64、OEM／16KiB页面、真实TTS及SAF选择器权限／取消未测。不宣称全部第三方方案／插件、物理双ABI运行或Paging144普遍硬上限。

本轮日志接受范围保留具体环境风险：API21 的真实 Trime 启动 Binder invalid-reference／bad-handle 被平台拒绝，具体调用及引用生命周期未追踪；启动前 vdd FAT dirty 警告未执行 fsck。API35 真实 Trime PID4811 的缺失 Binder 目标及8条 -EINVAL 事务仍保留，目标对象与高层调用栈未知；kernel audit_lost1、模块签名 taint、SELinux 拒绝、KeyMint earlyBootEnded -68及5个启动前非零 oneshot退出均保留原文。模拟器 StrongBox、部分音频／sounddose／tracing capability 缺口保留，不据此宣称真实硬件服务通过。两轮末尾宿主 stop Not implemented 的具体发出组件仍未知，只接受实际 owned process 正常wait0，不能概括所有子系统都已停止。取回 primary 源码用于对应打印分支，未独立证明客体／宿主二进制逐字相同；逐行审阅不等于所有平台风险消除。API35 的实际 FrameTracker jank 与8条 finalizer 警告仍保留，具体触发根因未证明；WebView ERROR code -1发生在后续 isolated-not-needed 及正常退出之前，有限的退出上下文不能证明没有发生 renderer 错误。

手动发布附件仅两个正式ABI APK与完整递归子模块源码归档；GitHub自动source zip/tar保留。源码归档大小／SHA以GitHub Release资产大小／digest及发布下载复核为准；新tag、Latest、完整source绑定及清理结果由实际操作后的归档外收据记录。本报告的冻结验收结果与在线发布状态分开，不嵌入自身归档SHA或事后发布收据ID／SHA。正式notes按精确已提交字节包装及发布，不能包装后修改导致source／tag漂移。本报告与正式发布说明包含在源码归档中，事后包装及发布收据保留在归档外。
