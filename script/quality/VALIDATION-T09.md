# T09 混输实现与模拟器验收记录（2026-10-10）

当前编译提交为 `d49940262bf5dce4a120e053e54dd5c93e614291`：543个构建输入、25条审阅路径，构建8及独立字节审阅通过。两个API的14固定任务、8稳态、4快速顺序和4宏回归均已通过并封存，原生候选与编辑器额外功能回归也通过。**两API的语言功能、恢复、停止及完整日志证据已闭合；当前正常Release在Android 15的两主题六轮浮层回归也通过。T09功能实现与这些限定范围的模拟器验收完成，性能及warning-free验收仍未完成。** 严格成本比较已执行，结果为 false，原因和可比子集如下。

公开的 `v3.3.13-t9.11` 是T08稳定版，不含T09。T09源码和本记录同步到 `X2M7/trime:develop`，不新增稳定标签。文档提交晚于编译提交；当前APK的编译来源仍是 `d4994026`，不能记作文档HEAD。

## 实现与功能范围

九键界面提供明确的QWERTY英文入口及中文返回，仅显式语言动作持久化 UNSET/NATIVE/ASCII；强制编辑器模式和符号导航不写入手动记忆。方案或主题变化重新解析布局，普通UNSET方案切换不再沿用旧锁定九键。原Rime单线程FIFO保持输入、语言动作和模式宏顺序；符号往返保留组合，不把九键raw数字串莫名当英文提交。

虚拟可打印ASCII使用既有InputConnection文本提交路径，避免后发按键事件落在新中文组合之后；物理键、TYPE_NULL、非打印键和快捷修饰仍沿用事件路径。已知虚拟映射键沿用旧按键路径的内容副作用语义，不更新 `lastCommittedText` 或触发提交朗读；普通公开/native文本提交仍保留原上下文及朗读行为，隐私禁止时清空上下文并禁止朗读。标准主题英文页新增可见 `+`、`&`、`=` 长按路线；动态T9无障碍描述随当前模式及缓存/重建View刷新。未增加逐候选JNI查询或输入内容日志。

这些行为由下表限定的实际功能检查支持。跨进程语言记忆、临时编辑器覆盖、方案/主题组合及待提交64无Space切入英文的API21额外范围已通过；API35终态功能报告及独立源、身份、恢复停止和完整日志证据闭包已完成。普通警告仍未解决，证据封存不表示warning-free。

## 已封存结果

| 范围 | 实际结果 | 边界 |
| --- | --- | --- |
| 构建8 | host203＝既有174＋当前虚拟文本29；应用JVM663本轮执行，0失败/错误/跳过；build-logic9缓存；双Lint0、Kotlin警告0 | 旧IC18及草稿54未重跑、不重复计数；独立报告 `13a70272…fe92f8` |
| Release静态身份 | ARM64/x86_64、minAPI21、签名/ELF/R8/Provider/48资源和4940 DEX定义检查通过 | QA包为 `com.osfans.trime.debug`，nondebug、tracing=false；未在ARM64真机运行 |
| API21成本采集 | `implementation-api21-7`：14＋8＋4＋4全通过，原状态恢复及owned停止完成 | 逻辑KeyView动作、真实native/IC文本；不是实物触摸或延迟测试 |
| API35成本采集 | `implementation-api35-1`：同样30项通过，父整轮封存、owned停止完成 | 原Scope3参考恢复失败与独立恢复成功分别保留 |
| API21/35 T08额外回归 | API21 extra4、API35 extra1：原生8项、320顺序输入、49候选分页、编辑器100轮及新增动态描述检查通过 | API35 Debug窗口停顿未决，不由功能成功抵消 |
| 两API新参考 | 每API14个实际状态、42角色和3个主对话框，经Root实际查看原PNG/XML后本地冻结 | 原图/节点/RGBA、编译来源、APK、boot/PID及独立schema侧证绑定；不是语言额外验收 |
| API35原Scope3与Recovery3 | 原报告 `f6330b40…5700` 仍FAILED；独立恢复报告 `36197878…bf3b` 仅cleanup通过 | 原5个文件的字节/存在性及YAML原UID/GID/mode/FUSE元数据在停止后恢复，随后新PID7264恢复UNSET/Trime/T9；原错误不改写 |
| API21语言额外 | 完整4个case/24份proof、12次owned进程重建和2次待提交切换通过，原状态恢复及停止；独立闭包 `2d121416…52b268` | 真实可见键、observer2实际光标/组合范围和跨进程状态；普通警告影响UNKNOWN，非正常Release或物理预算验收 |
| API35语言额外 | `language-extra-api35-1`：24份proof、12次真实进程重建、2次过渡、原状态恢复、新Main、owned停止及完整日志证据闭包完成 | 完整闭包 `7ae094d0…47abb`；源/功能 `3a2570ae…0aa2e`，身份/observer/42角色 `6217b9d3…06efc`；普通W/E/F及GC仍UNKNOWN，machine/warning-free均false |
| 当前正常Release浮层 | `normal-popup-api35-2`：两主题简化字全拼各3轮通过；Popup 1→0→1，提交后0；同Main2199，每轮精确提交一次你；独立闭包 `8ea8a3d7…db8d` | 当前Build8 x86_64/API35正常Release，非公开9.11 APK；原文件在应用停止时恢复，未采集恢复后的新Main；normal1中断0轮记录仍未通过 |

语言额外的进程重建实际验证同包UID/cmdline、旧PID消失和新PID变化；未捕获guest Main starttime，不追加该断言。主题/方案通过实际UI切换，密码编辑器的强制模式不覆盖手动偏好，返回普通输入恢复原记忆。待提交64→英文检查先绑定实际selection末尾，不点候选或Space，切英文得到精确 `你`，再输入 `a` 得到 `你a`，延迟复读相同。API21实际inline与API35 display-only展示分别保留，不把inline预编辑或宿主精确文本冒充native/IC回调计数；固定任务的Android探针另有真实提交计数。

每Gradle构建阶段保留55条上游弃用提示（52 AGP、3 KSP），每个Release APK保留2条META-INF签名提示；不称warning-free。R8优化/缩减与资源压缩启用，`-dontobfuscate` 保留类名。

上述API35语言与Debug成本范围的外部 `user.yaml` 在精确限定路径上使用特权Root审计；其CE/DE仍使用已绑定Debug AppUID。该读取不能证明普通AppUID外部存储可读或native回调。恢复只对独占nonce临时文件修正原UID/GID，经全字节及元数据复读后才替换目标；原Scope3失败与独立cleanup成功不是同一状态。

## 严格成本比较

冻结比较器已对完整旧基线与新终态实际执行，环境匹配通过，正式结果 `false`（报告 `fea14c9b…746a2e7`）。每API均有7个起终态匹配的成功任务、3个Trime新增可达能力、4个邮件/URL状态不可比任务；可比任务没有导航或总动作退步。旧基线每API固定11 PASS/3 UNSUPPORTED、稳态4 PASS/3 UNSUPPORTED/1 FAIL保持原样，采集完成不是旧产品全部通过。

两API的实际逻辑动作结果相同：

| 可比任务 | 总动作：旧→新 | 导航动作（mode＋layout）：旧→新 |
| --- | --- | --- |
| 标准主题 Android 16 | 15→13 | 4→2（下降50%） |
| 标准主题 Wi-Fi | 13→9 | 6→2（下降66.7%） |
| 标准主题 B站 | 11→9 | 4→2（下降50%） |
| 两主题电话 | 14→14 | 不变 |
| 两主题金额 | 6→6 | 不变 |

同起终态的标准主题三个任务，每API总动作合计39→31（下降20.5%），导航动作14→6（下降57.1%）。这个子集的改善不能推广到全部14任务，也不是物理触摸性能。

Trime旧英文路线不可达，新Android 16/Wi-Fi/B站任务分别13/9/9动作，仅记能力增加，不能以旧部分3动作当0成本或算改善率。两主题邮件25→25、URL36→36仅是动作观察：旧 `__trime_compat_letter` 与新 `letter` 的真实起终键盘ID不同，四项每API都保留不可比、delta/百分比为null。未归一化ID或修改原比较合同；不能宣称全基准通过。快速顺序、宏、语言记忆、参考准备和浮层诊断不加入14任务成本。

## 全日志与性能限制

当前API35成本父/子全文分别保留4536/2557条W/E/F，独立Recovery3全文4520条；all-PID critical、实际LMK、TrimePerf及捕获console的kernel critical为0。普通警告仍为 **UNRESOLVED**，原文、行结束SHA和上下文保留，不能把机器critical0说成warning-free。ART/GC及跳帧诊断也保留，不推出延迟预算结论。

另一个已封存API35 Debug编辑器范围保留11条独立窗口记录，对应两次约4秒 `ViewRootImpl.doDie`/`dequeueBuffer -110` 停顿及240帧跳过。父/子重复不算22次；精确主题/子用例缺少时间标记，不能补推或归因于无障碍修复，状态仍 **PERF_UNRESOLVED**。当前成本范围没有相同字面签名，也不能清除这个不同范围的异常；本次正常Release六轮没有相同字面签名，仍保留Debug未决事实；两种构建的不同结果不证明根因已消除。

API21成本为1536MiB/1CPU/swangle、Vulkan关闭，实际720×1280/320dpi/360dp且三条WM查询证据匹配；API35成本为3072MiB/4CPU/host、Vulkan关闭，旧基线没有直接WM density/override采样，不从截图或AVD推断。API35未开启show-kernel，console结论仅限已捕获内容。

此前真实产品失败、旧参考描述错误、旧工具失败和四次dd SIGPIPE Fatal整轮失败均保留；独立恢复不抵消原FAILED，不补写未确认根因。语言额外和当前正常Release的原图批准、全日志、恢复及停止证据均已闭合，不覆盖这些历史记录。

真机候选P95≤100ms（明显更慢时先下降30%并确保准确率不退步）、Java/native/graphics/总PSS、连续输入和主题切换驻留、首次部署、ARM64/OEM运行及一般准确率仍未测。模拟器功能、固定文字与逻辑动作账本不代替这些指标。

正常Release首次范围的parent在会话中断后消失，owned QEMU由Root按原PID/exe/PGID/starttime精确停止；0轮产品检查、无已接受UI图，原状态恢复未宣称。原报告与独立中断收据 `e32d9409…efce`保留，新normal2不能把原范围改写为通过。辅助Root UI工具仅修正ART argv[0]末尾NUL填充校验，精确包名、UID/starttime、APK、备份及原图批准等门禁保持；9项纯测试原执行通过，独立42文件SHA及逆向全字节审阅 `9c63b171…1d2dd`通过，不是设备浮层验收。

API35语言范围父/子全文分别62887/62017行，W/E/F分别14545/14426条，GC/等待/跳帧各77条，全部保留UNKNOWN；环形缓冲区重复不合计为独立事件。all-PID critical、实际LMK、TrimePerf、捕获console内kernel critical及字面4秒窗口签名均0，完整guest kernel捕获未建立。`machine_log_scan_passed=false`、`warning_free=false`，不豁免普通警告，也不清除不同Debug范围的两次约4秒停顿。

本次仅将T09源码、README和本记录同步到fork的 `develop`，不替换公开T08稳定版的标签或附件。本次文档提交不改变当前543个构建输入；APK编译源码仍为 `d4994026`。实际推送后按远端分支提交核对同步结果。

## 当前正常Release的实际收据

正常APK SHA-256为 `18689c2fadcd2c7ab64a78e23a180199b3f01633ccc584ff30afd032b3708cbc`，nondebug、minAPI21、tracing=false。API35使用3072MiB/4CPU/host、关闭Vulkan；boot为 `dcad0a15-7744-4b29-882e-3d60660e37c2`。主题与 `luna_pinyin_simp` 通过当前实际UI设置；Root查看新原PNG/XML后批准n、i及真实空格键。六轮共30个实际按键/显示隐藏操作，始终Main2199/UID10148，输入ni、隐藏、恢复、空格提交准确一次 `你`。独立审阅核对3905份原件、918条父命令和36个轮次状态。

正常Release的CE929B、DE914B和YAML341B均由Root对精确路径审计，在所有所属应用进程停止后按原字节、存在性、stat/SELinux元数据恢复；三次DD计数、双读和独占nonce临时文件及诊断清理均有实际收据。这不证明普通AppUID读写权限，也没有恢复后新Main的验证。模拟器3517314正常停止并wait exit0；父验收进程exit0，结束于2026-10-10T00:08:36.739666Z（北京时间08:08）。

独立闭包完整SHA-256：`8ea8a3d7fe1ee47705432facd9b7685349488a41a0a2de288eef049b9effdb8d`。全日志4660465B、33726行，SHA-256 `dc780593689c4467144ed6b06f7e1522516ad7db91f3e067673d1061d88f31a9`；all-PID critical、实际LMK、约4秒浮层字面签名、TrimePerf及已捕获console严重事件均0。5968条普通W/E/F（其中owned34条）及318条GC/帧等待仍为UNKNOWN，machine/warning-free均false；未开启show-kernel，不声称完整guest kernel历史。原审阅的import及标记子串计数错误保留，最终按冻结的完整标记解析闭合，未改写原日志或生产源码。
