# v3.3.13-t9.13 验收记录

**本轮稳定版验收通过，线上状态以 Release 为准。** versionCode `20261118`，当前实现 `921f713266a497e29b2381d7d7471eae8ee5a974`，已包含上游 `8545278b9efccaaf5b222dd499f5954ebcf87269`。本轮 Build17／Host13／Static13 已实际闭合；API21-run13／API35-run7 六阶段功能回归、完整GUI与正常停止均通过；两轮逐行日志及完整控制台审阅均已闭合，旧分项结果不继承。

| 验收项目 | 实际结果 |
| --- | --- |
| frozen源码与currentupstream | 通过：921f713266a497e29b2381d7d7471eae8ee5a974／579 个源码绑定／本轮冻结源码 before／after SHA 完全相同，当前 Git clean；当前 upstream8545 ancestor 与源码绑定已核，本轮模拟器范围已闭合 |
| fresh应用／宿主 | 通过：702 项本轮新执行应用 JVM 测试通过／213 项本轮新执行宿主测试通过；build-logic 17 项＋compatibility 6 项为 UP-TO-DATE 缓存结果，未计入 702 fresh |
| Build17／Lint／签名 | 通过：Debug／Release Lint 均 0 issues，构建警告0／ARM64／x86_64 Release 签名检查通过，minSdk21、20261118／3.3.13-t9.13、DEBUG=false、签名警告0；既有证书 SHA 见 T10 记录 |
| Static13 complete当前body | 通过：117 owners／ABI 配对，共 234 当前主体；Shift 2 个主体独立审阅／主体 1474 methods；Shift 42 methods 单列／主体 468 份 stdout／stderr 原文；补充原文单列、14方面／17角色／29JNI、必要Shift实际owner范围、当前focusguard与pages／after-layout callback |
| API21 Release GUI 分项 | 通过：当前源码实际主6场景7检查＋短语2检查，失败提示／编辑器更换／全局阅读位置及完整配置、元数据和LatinIME恢复；真实child／outer wait0；原六阶段功能与40rapid／100cycles、owned stop0已闭合，4463条同轮W／E／F已逐行审阅，未知调用来源与环境风险保留 |
| API21完整验收 | 六阶段功能／GUI7＋2／原驱动wait0与owned stop0已通过；4463条逐行日志及完整控制台审阅通过，范围限本轮模拟器 |
| API35 Release 升级与 GUI 分项 | 通过：正式Release旧9.12覆盖升级、三次便携备份、主6场景7检查和短语2检查；通知禁用时可读失败提示、真实编辑器更换清理及全部配置／元数据／LatinIME恢复，child／outer wait0。短语32捕获／10停机守卫／15元数据快照按本轮实际计数，原短语不存在状态亦恢复；单独Debug功能、逐行日志和完整控制台审阅均已闭合。 |
| API35同完整范围 | 六阶段功能／GUI7＋2／原驱动wait0与owned stop0已通过；11620条逐行日志及完整控制台审阅通过，范围限本轮模拟器 |
| 新Debug真实回归 | 两API本轮分项通过：T10 14＋存储21／privacy16／T08 8，两主题partial signedpixel／两次relayout／native单次提交、Shift真实DOWN／UP、快速输入40轮和键盘显示隐藏100次；全部原严格条件保持 |
| 原文日志与封存 | 通过：2441／2864个封存文件逐SHA／bytes核验，原驱动wait0与owned stop0；4463／11620条原始日志及完整控制台审阅通过，本轮项目warning及未裁定行0。原始框架／负例诊断仍保留，不豁免fatal／LMK或未知环境限界 |
| 冻结验收与在线发布 | 本报告冻结记录本轮实际验收结果；最终门禁、包装／发布收据保存在归档外的 `build/t10/`。在线发布状态见[版本Release页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.13)，Latest另按发布后实际查询核对，不作为正文待填ID或SHA。 |

## 历史与范围

历史Build14有702fresh应用JVM／213fresh宿主、17+6缓存、两Lint0、静态214body／1360methods通过；这些只绑定当时8469源码。API21-run10仍因FullEditortimeout失败，SettingsCmd真实critical1原因unknown、4699原W/E/F待裁定。product诊断5硬件tap超时原因未证；诊断6fixture空editor条件失败，之后只经真实InputConnection删除自身刚committedexpected。product诊断7限定Debug fullEditor实际通过，两主题8阶段、40rapid／100cycles、normalwait0、412封存；2119W/E/F pending，不是新完整稳定验收，更未执行新增Shift回归。

API35早期run2／run3的SystemUI／Phone ANR、run4三次LMK、run5测试helper查询可见性失败、run6通知禁用导致失败Toast缺失均保留原失败。run7首次短语启动被预期通知说明阻挡，原0检查／12次捕获失败保留；记录普通Cancel后，同一脚本在新目录完整2检查通过，通知保持禁用，不修改原验收条件。Build16／API21-run12真实更换编辑器后旧失败对话框未清理，整轮失败；当前Build17提示框保留编辑器焦点并沿既有生命周期清理。API21使用1536MiB／1核，API35当前3072MiB／4核，单VM且原fatal／ANR／LMK／超时／内存下限不变。

其余原失败、source／test／采集修正区别见[完整T10历史](VALIDATION-T10.md)。当前候选包含上游ASCII punct正确选项与Shift配置，保留fork输入队列、editor／privacy与themeDOUBLE／LAZY行为。本轮新实际 APK、静态审查及两API正式Release升级、候选和短语界面、单独Debug两主题与新增Shift功能回归均已闭合；两轮逐行日志与完整控制台审阅均已闭合。

## 未验证

真机P95／PSS／长期驻留／首次部署、物理ARM64／全部OEM／16KiB页、真实TTS及SAF选择器权限／取消未验证。PAGE48／prefetch16／maxSize144是best-effort预算，非普遍硬驻留上限。每个原始framework／预期负例W/E/F保留并逐行判定；新轮critical0不追溯证明历史SettingsCmd已修。

本轮日志接受范围保留具体环境风险：API21 的真实 Trime 启动 Binder invalid-reference／bad-handle 被平台拒绝，具体调用及引用生命周期未追踪；启动前 vdd FAT dirty 警告未执行 fsck。API35 真实 Trime PID4811 的缺失 Binder 目标及8条 -EINVAL 事务仍保留，目标对象与高层调用栈未知；kernel audit_lost1、模块签名 taint、SELinux 拒绝、KeyMint earlyBootEnded -68及5个启动前非零 oneshot退出均保留原文。模拟器 StrongBox、部分音频／sounddose／tracing capability 缺口保留，不据此宣称真实硬件服务通过。两轮末尾宿主 stop Not implemented 的具体发出组件仍未知，只接受实际 owned process 正常wait0，不能概括所有子系统都已停止。取回 primary 源码用于对应打印分支，未独立证明客体／宿主二进制逐字相同；逐行审阅不等于所有平台风险消除。API35 的实际 FrameTracker jank 与8条 finalizer 警告仍保留，具体触发根因未证明；WebView ERROR code -1发生在后续 isolated-not-needed 及正常退出之前，有限的退出上下文不能证明没有发生 renderer 错误。

手动assets仅ARM64 APK、x86_64 APK及一个含recursive submodules完整源码archive；GitHub自带zip/tar保留。线上发布状态以版本 Release 页面为准；归档大小／SHA以GitHub Release资产大小／digest及发布下载复核为准，附件完整性、Latest与清理结果写入归档外发布收据。本报告包含在源码归档中，不嵌入该归档自身SHA或事后收据ID／SHA；已提交的发布说明字节不能在包装后修改。
