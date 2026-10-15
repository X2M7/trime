# T13 无障碍、设备适配与运行警告验证

状态：本地代码候选，尚未推送或发布；T13最终验收未闭合。已执行范围只对应check30 Debug：构建／保护／冻结0、最低3,511,604 KiB；812项JVM／84XML沿用check29（非本轮新执行），无当前Lint／Release结果。API21 runtime6完成setup7原14＋缓存7项、矩阵4组各31项及24图、编辑器40轮／100次显隐和privacy／t08／t10／t11／feedback五项串行回归，原native终态和宿主均0；设置精确恢复，Root正常STOP后controller／VM0、cleanup空、无残留，最低3,253,812 KiB。日志2177W／1320E，项目149W／92E；本轮ThemeUtils／旧Session fatal／am_crash／ANR／真实LMK及内核critical未匹配，只是有限范围，不关闭历史风险。当前已应用10处同文风配色调整及候选3.3.13-t9.16／20261121，尚未构建，不能沿用check30作为新候选通过。后续从最终APK完成两平台、实际深色／TalkBack／分屏／SAF及独立Lint／Release；用户无真机，优先模拟器，文件索引继续运行。旧失败和原件保留，已发布基线仍为`v3.3.13-t9.15`／`e0b2fc22`。

## 当前执行记录

- `build/t13/main-check1` 退出 1：主源码的可空 RimeSession 缓存读取调用错误，随后修正；不是通过凭证。
- `build/t13/debug-check2` 退出 1：主应用编译通过，设备测试在悬停事件方法可见性和 Editable 可空类型上编译失败，随后修正。最低可用内存 3,623,732 KiB，保护未触发；原始输出保留。
- `build/t13/debug-check3` 实际退出 0：789 项应用 JVM 测试新执行通过（82 份 XML 已冻结），Debug Lint 实际执行且 0 问题，双 ABI Debug 与设备测试 APK 生成。该轮有 6 条设备测试弃用警告，前轮主源码另有 3 条同类警告，不能记作零警告；最低可用内存 2,297,552 KiB（约 2.19 GiB），保护未触发。源码清单、APK 哈希、测试 XML 和 Lint XML 保存于该目录。
- 随后修正了切页／模式键可访问名称丢失具体目标、几何测试跨窗口直接比较 root 坐标的问题，并添加两主题候选选择前后原始 PNG。API 36 及以上改用整型 checked 状态，旧系统使用其实际可用的布尔 API，仅对兼容分支作精确弃用标注；专用测试编辑器的旧 resize 行为也单独说明。参考 [Android checked 状态定义](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#getChecked())。
- `build/t13/debug-check4` 实际退出 0：791 项 JVM 新执行通过（82 份 XML），编译与设备测试 APK 生成，无构建警告；最低可用内存 2,873,428 KiB（约 2.74 GiB）。源码变化仅为编译前 spotless 调整 import 顺序；独立复核见 `debug-check4-review1`。本轮未运行 Lint，新源码最终仍须完整 Lint 和设备验证，不是发布构建。
- `build/t13/debug-check5` 实际退出 1：短屏布局修改在主源码编译时失败，未作为测试／Lint 通过凭证。两处原因分别是 `windowManager` 扩展 import 在 ConstraintLayout DSL 中遮住 BoardWindowManager，以及 Kotlin 不能引用 `ScrollView.LayoutParams`；Root 已仅改为扩展别名 `systemWindowManager` 和 `FrameLayout.LayoutParams`。最低可用内存 3,466,484 KiB，保护未触发；原始错误、退出记录及 `tracked_source_unchanged=false` 保留，本轮只是开发命令。
- `build/t13/debug-check6` 实际退出 1：主源码和设备测试编译通过，794 项 JVM 测试新执行通过（82 份 XML，失败／错误／跳过均为 0，冻结于 `jvm-tests.json`）；Lint 未完成。Kotlin 编译与 Lint 共用 JVM 后，896 MiB 堆接近满额并持续 Full GC，Root 以 SIGTERM 受控停止，`controlled-stop.json`、线程诊断与原输出保留，不作为完整构建通过。最低可用内存 3,047,360 KiB（约 2.91 GiB），宿主保护未触发。`tracked_source_unchanged=false` 对应 Spotless 将 `InputView` 的 `systemWindowManager` 别名 import 移至 import 列表末尾；已对照本轮与 package7 启动时的原始工作区补丁，未把它解释为未经核对的行为改动。
- `build/t13/debug-package7` 使用 check6 完全相同的 BUILD_TIMESTAMP／HEAD 环境仅执行打包，实际退出 0（1 分 50 秒），构建警告 0；主源码和设备测试 Kotlin 编译任务均为 UP-TO-DATE，未冒称新一轮编译或测试。最低可用内存 4,231,576 KiB，`tracked_source_unchanged=true`。APK、607 项源码清单和未跟踪源码 ZIP 已冻结，runtime2 只绑定这些产物；后续工作区修改不属于这轮 APK。最终 Lint 待代码收敛后在独立 JVM 串行执行，Release 与完整验收尚未完成。
- 独立 `observer1-build1` 已实际编译、签名校验退出 0，256 MiB JVM 上限，输出无编译／签名警告。APK SHA-256 `db631d56a91abace2b1f8e504a706d9575f28de68c0a83d00650ea31a558dde0`。新包在 API 35 使用非抑制无障碍服务连接；这不代表 TalkBack 已运行或验收。
- API35 `runtime1` 已使用全新隔离磁盘运行并正常退出，控制器／VM 实际退出 0、无清理错误或残留，最低可用内存 1,439,544 KiB（约 1.37 GiB），保护未触发，索引保持运行。API21 尚未启动。旧 AVD 和系统镜像未修改。

## API35 runtime2 已执行范围（冻结 package7 APK）

以下只对应 `debug-package7` 冻结产物：x86_64 Debug APK SHA-256 `2df8748e720e4bf657078ebbd3aaad71e6594b0a80ffceef9e1d7376ac0a4f42`，测试 APK `dd31d7d56462fb8beccf38cfbe5740b093be3a0ca356274f529f7b3b21a688f6`。不覆盖后续未构建的工作区修改，也不是整轮设备验收。

- [setup2](../../build/t13/api35/setup2/phase-report.json)／[t10-2](../../build/t13/api35/t10-2/phase-report.json) 各通过 14 项检查，宿主命令退出 0，原 instrumentation `passed=true`、完成码 `-1`。重建后的试打焦点恢复通过；对应两进程未复现 `PhoneWindow`／`RippleDrawable` W/E/F，但其他原始警告仍保留。
- [editors2](../../build/t13/api35/editors2/phase-report.json) 原 instrumentation `passed=true`、完成码 `-1`，宿主命令退出 0。两主题展开候选的全局索引均为 22，`trime` 精确偏移 −83px、`tongwenfeng.trime` −73px；收起／重开、两次重布局及原生文本唯一提交检查通过。两主题各 20 轮显示／清除共 40 轮、真实 WebView 邮箱输入及 100／100 次显隐通过。计时是包含队列、主线程与轮询的测试墙钟时间，不是候选 P95 或真机性能。证据见[原始 stdout](../../build/t13/api35/editors2/command-009.stdout)和[指标原件](../../build/t13/api35/editors2/native-artifacts/report.json)。
- [matrix1](../../build/t13/api35/matrix1/matrix-report.json) 在读取原状态时失败，尚未修改设置或执行用例：脚本错误假设必须启用一个导航 overlay；实际系统可在无启用 overlay 时使用基础三键导航。脚本已按实际导航 mode 识别并保留这种原状态，未把失败改为通过。
- [matrix2](../../build/t13/api35/matrix2/matrix-report.json) 的首个手机用例中，键盘 helper 的 13 项检查与状态恢复通过；随后未附着窗口的候选夹具读取节点说明时发生空指针。原生报告 `configurations`、截图均为空，主题配置／几何验收尚未开始，其余七个用例未运行。已确认远端 instrumentation 不再运行，恢复原设置、显示和导航状态，前后 APK 哈希一致；失败原件与空指针栈保留，不能称矩阵通过。

针对这些问题，以下修改已在 debug-check9 编译通过，尚未设备运行：候选／拼音／预编辑夹具先附着真实测试窗口，T9 夹具在附着前先设置非空状态；增加滚动父容器后 `KeyView` 弹窗坐标更新及返回原坐标的回归；两主题增加未提交九键输入切符号页再返回的组合、编辑器与整个输入面板几何检查，每个成功用例预期保存 6 张截图；极小窗口改由稳定父容器滚动整个面板，避免只滚动按键却仍由顶部控制区遮挡编辑器。原触摸、候选身份、精确提交与几何断言保留。这些修改均不在 package7 APK 中，后续必须重新构建、执行矩阵及实际交互。debug-check8 主应用编译通过，但设备测试在保存可空预编辑快照时发生类型不匹配，实际退出 1，后续 JVM／打包未完成；已将保存变量声明为可空类型，保留原快照精确比较、raw64 和锁定断言。最低可用内存 3,413,848 KiB，保护未触发，原失败日志保留。Lint 将独立串行运行。

- [talkback2](../../build/t13/api35/talkback2/root-scope-review.json) 以真实 TalkBack FOSS 完成九键 `64`→锁定 `mi`→选择“米”，只读编辑框从 `T13_FAKEn` 精确变为 `T13_FAKEn米`。实际主题读回是 `trime`；前述过程口头称为同文风已更正。原西文模式下 `n` 直接提交的错误预期，以及两次探索坐标／标签误判均保留，纠正后才激活对应控件。Root 实际核对 18 张关键 PNG；字体 2 横屏的空态面板 `[0,210][1184,720]` 与完整编辑框 `[0,48][1184,144]` 不重叠。旋转重建了夹具并清空组合，故第 49 帧文件名虽含 composing，也不能记为组合态／符号页通过；固定 48dp 编辑框自身的字体裁切另列，未归作 IME 遮挡。
- 本轮 50 帧观察后，Observer 正常 stop、宿主退出 0，原 instrumentation `passed=true`／完成码 `-1`，会话／boot／PID/starttime／APK 身份一致；字体、旋转、原无障碍设置均恢复并读回。runtime2 VM／控制器均实际退出 0，无清理错误或残留；连续 logcat 在设备正常关闭时返回 255，原件保留。最低可用内存 1,796,720 KiB（约 1.71 GiB），1 GiB 保护未触发；索引保持运行。本轮仍不是完整矩阵、Android 5.0、Release 或稳定版验收。

导入路径另完成一处窄优化：与普通主题加载共用现有 APK 内置主题的 lazy 缓存，避免每次成功导入重复读取／反序列化内置兼容布局；精确导入主题的部署、校验和解码保持，事务／取消边界与 2000ms 队列警告阈值未改。debug-check9 已实际退出 0，主应用与设备测试编译／Debug 打包通过，797 项 JVM 测试在本轮新执行，82 份 XML 已独立保存；无失败、错误或跳过。最低可用内存 3,190,460 KiB（约 3.04 GiB），构建日志未发现警告，Lint 尚未执行。此结果不等于导入性能或设备通过。其后源码复核发现预编辑 ACTION_CANCEL 误清除未变化节点的已发布身份，正在补最小修复与设备回归，需另轮构建。

debug-check10 已包含上述触摸取消修复，实际构建退出 0，797 项应用 JVM／82 份 XML 全部新执行通过，Debug 主应用与设备测试 APK 已冻结。构建期间已跟踪源码字节不变，最低可用内存 3,194,272 KiB（约 3.05 GiB），构建日志未发现警告；独立 Debug Lint 实际退出 1：短屏九键侧栏的 `notifyDataSetChanged` 触发一项性能错误，完整报告已另存；最低可用内存 3,457,832 KiB，未触发保护。正在将可选音节标题行纳入精确 DiffUtil 更新，不通过抑制警告绕过。快照记录主仓库 app 下 619 项文件／符号链接目标与未跟踪源码，未冒充递归原生子模块全文件清单；设备尚未运行此 APK。

debug-check11 已修复该整表刷新问题：标题与拼音统一精确 Diff，保留 revision／span 点击绑定，另覆盖标题增删、同版本内容／选中状态改变和旧节点拒绝。Root 与独立源码审阅均完成；编译、797 项新执行 JVM／82 份 XML 和 Debug 打包实际通过，构建日志未发现警告。最低可用内存 3,393,760 KiB；格式化后的 619 项 app 文件／符号链接目标与三份 APK 已冻结，最终 Lint 待重跑。Android 15 runtime3 在首次启动、安装 APK 前因 ADB root 连接瞬断失败（原 stderr 为 `adb: unable to connect for root: closed`）；VM 正常退出 0、控制器退出 1，无清理错误或残留，最低可用内存 3,757,160 KiB，保护未触发。未执行设备验收；正在修正工具的有限启动重试，保留原 boot 时限、实际 root 身份及内存保护。

debug-check12 包含固定面板 Insets、三阶段实际区域／Region 检查及预编辑稳定资源标识，实际构建退出 0（6 分 29 秒）。797 项 JVM／82 份 XML 全部新执行通过，应用及设备测试编译、双 ABI Debug 打包完成，日志未检出构建警告；源码期间未变，最低可用内存 3,136,720 KiB（约 2.99 GiB），保护未触发。619 项 app 输入、工作区补丁、XML 与三个 APK 已冻结；x86_64 APK SHA-256 `14a1127ca05ef0d52d82355d8b5af325c3311d96e91bf96b0e07ddaa0367ea97`，设备测试 APK `62896e91d046289d1ee2dff33273b874db0af97885028489f12b92c7071da31c`。这不是最终 Lint／Release 验收。其后 Android 15 runtime5 已结束，限定结果与失败见下节，旧原件均保留。

debug-check13 已包含符号按键的通用内容分区、六项纯 JVM 边界测试、附着窄键的真实字体检查，以及两次实际绘制／API29+ 提交回调与截图前后身份核对。构建实际退出 0（约 6 分 12 秒），803 项应用 JVM／83 份 XML 全部新执行通过，应用与设备测试编译及双 ABI Debug 打包通过，日志未检出警告。最低可用内存 3,410,084 KiB（约 3.25 GiB），保护未触发。tracked_source_unchanged 为 false：Root 已核对唯一已跟踪源差别是 Spotless 把 KeyView 四个参数拆行；未跟踪 helper 的格式化也经独立复核。格式化后的 622 项 app 输入、补丁、XML 和 APK 已冻结，不声称构建全程字节未变。x86_64 APK SHA-256 `e16f719569437c5baf36b6dcf08b088bb4998a6e645b33ece19c9f60ecca170b`，测试 APK `d4c2f4fa140bb74bf607db5d76781ed29b57f1ba5e10cecef1fc2986a524f05e`。最终 Lint／Release 未完成，Android 15 runtime6 已完成本节下方限定范围与失败诊断，完整设备验收仍未完成。

## API35 runtime6（check13）与横屏短诊断

- setup5 的 14 项首次设置通过。matrix5 的字体 1／2 两组竖屏均完成两主题原生节点、精确提交与三阶段实际 Region 检查；Root 实际查看 4 张关键原图，独立审阅实际查看全部 12 张。两倍字体符号文字保持在键内，before-candidate-click 原图确为九键，after 为唯一“米”。屏障每张记录两次真实绘制、两次硬件提交回调及两次后续帧；未把提交等同呈现。
- 第三组横屏在 editor attached/served 前等待 30 秒超时，0 项业务检查、0 张 native PNG；原窗口日志确认 Activity 首次创建后旋转重建，探针仍持有首个编辑框，旧引用导致等待失败是强证据推断，未记录单个 predicate 的实际值。矩阵宿主实际退出 1，原配置恢复成功，后五组未运行。原重建来源及旋转回转发起者未证明。
- 独立短诊断锁定传感器并启动系统 Settings，原 window dump 确认真实 1280×720／rotation1／USER_ROTATION_LOCKED。之后 landscape-diagnostic1／2 均通过编辑器就绪、InputView 可见、15 项键盘 helper、候选 helper 及数字 `64`，但等待“选择拼音”动作再次超时，两轮实际退出 1；仍没有 native 成功截图。第二轮在等待期间额外保留原 `screencap` 与窗口转储，Root 实际看到侧栏 `64 / mi / ni`、完整键盘与未遮挡编辑器；这是独立诊断图，不冒充成功阶段截图。正在增加失败前有界节点动作／delegate／RecyclerView pending 状态诊断，尚未确定侧栏可见而动作不可达的原因，不降低原断言。
- 短诊断恢复 user_rotation=0、accelerometer_rotation=1，并核对实际 720×1280／rotation0；runtime6 VM／控制器实际退出均为 0，清理无错误、无残留，最低可用内存 2,035,728 KiB（约 1.94 GiB），保护未触发。运行日志原件保留；整轮仍不是完整矩阵、TalkBack 分屏、Android 5.0 或发布验收。

## API35 runtime5（冻结 check12 APK）

- [setup4](../../build/t13/api35/setup4/phase-report.json) 的 14 项首次设置检查通过，实际宿主退出 0、原 instrumentation `passed=true`／完成码 `-1`。
- [matrix4](../../build/t13/api35/matrix4/matrix-report.json) 前两组竖屏配置（字体 1／2）均完成两主题的原生节点、组合保持、精确选词及三阶段几何断言。透明宿主与真实视口、触摸 Region 已分开测量；完整编辑器与输入区域不交。但原图复核发现字体 2 下两个主题符号页的按键文字重叠越界，选词前截图还停留在上一符号页；逻辑通过不代表视觉通过，独立原图复核报告明确为 false。
- 第三组横屏在启动测试页面前失败：脚本要求 Launcher 先旋转，但当前 Launcher 声明 NOSENSOR，故 30 秒内一直为竖屏；剩余六组未运行。矩阵真实退出 1，原配置恢复成功。正在改为核对目标配置设置，再由可旋转的测试页面及其三阶段几何／原图证明实际方向；不降低方向或区域断言。
- 原始 12 张 PNG 与失败记录保留。按键绘制修复将主文字、上下提示限制在独立区域，长标签最多两行后省略，完整无障碍名称保留；截图补实际附着模型、绘制及前后状态绑定。这些新源码尚未构建或设备验证。
- runtime5 正常关闭，VM／控制器退出均为 0，清理无错误、无残留；最低可用内存 2,193,932 KiB（约 2.09 GiB），1 GiB 保护未触发。logcat 随正常关闭返回 255。运行警告仍保留并单独复核，尚非完整矩阵、真实分屏、Android 5.0、Release 或稳定版验收。

## API35 runtime4（冻结 check11 APK）

- 启动工具仅对 `adb root` 的精确连接关闭错误作最多三次重试，保留原 180 秒启动上限及实际 root 身份检查。新隔离实例实际启动，API 35／boot／进程身份已核对，文件索引保持运行。
- [setup3](../../build/t13/api35/setup3/phase-report.json) 的 14 项首次设置检查通过，宿主退出 0、原 instrumentation `passed=true`／完成码 `-1`，安装后拉回 APK 与 check11 冻结哈希一致。
- [matrix3](../../build/t13/api35/matrix3/matrix-report.json) 在首个字体 1 竖屏的 trime 符号页几何断言失败，宿主退出 1，七个后续配置未运行。键盘 helper 14 项、候选 helper 48 项通过；其中包括标题精确 Diff 与预编辑 ACTION_CANCEL 后同一节点激活。raw `64`、锁定 `mi`、编辑器身份及切符号页组合保持检查通过，尚未完成本用例最终候选提交。原设置、显示和导航均已恢复，实际 instrumentation 已退出。
- Root 实际查看原始 `trime-symbols-composition.png`：完整编辑框位于 `[0,48][720,139]`，可见面板从约 400px 起，图中无可见遮挡。失败测量的 `[0,48][720,1280]` 是服务为浮层保留的透明 MATCH_PARENT 宿主，不能当成实际面板。源码复核另发现真正的滚动风险：`onComputeInsets` 使用会随滚动移动的内容 `keyboardView`，可能把触摸边界上移。现改为固定 `keyboardViewport`；测试保留原宿主矩形，并核对视口、所有可见预编辑区域和真实触摸 Region 与完整编辑框不交。新源码尚未构建／设备验证，不能改判本轮失败。
- 同一 check11 APK 的 [editors3](../../build/t13/api35/editors3/phase-report.json) 实际退出 0，原 instrumentation `passed=true`／完成码 `-1`；两主题精确候选定位、40 轮显示、输入框／隐私／WebView 回归及 100 次显隐通过。后续源码修改不属于该冻结包。runtime4 随后正常关闭，VM／控制器均退出 0、清理无错误、无残留；最低可用内存 1,784,624 KiB（约 1.70 GiB），保护未触发。实际分屏、Android 5.0、最终 Debug／Release Lint、Release 与发布仍未完成。

runtime2 仍有 IME 返回回调、系统安装 session rename 和 WebView renderer 错误；已核实的发生范围、源码推断与限制见 [T13 警告分诊](T13-WARNING-TRIAGE.md)，不以功能回归通过代替警告原因闭合。

## API35 首轮诊断（debug-check4 APK）

- `t13-phone1` 在准备测试配置前失败：Android 15 的包转储使用 `appId`，旧脚本只认 `userId`。已接受两个实际字段并额外核对 `run-as id -u`，原件保留。
- `t13-phone2` 实际节点／按键与真实空格提交运行后，清理阶段的 schema／ASCII 精确恢复断言失败。`setText("")` 触发编辑器重启；已改同一 Editable 清空并补 editorToken 不变断言，仍待重建。
- `setup1` 前 9 项通过，新焦点保存检查确认草稿、selection、composition、editorToken 和 checkpoint 不变；页面重建后试打框没有恢复焦点，整轮失败。已补异步状态启用和 Fragment 附着后的受控恢复重试，原 60 秒断言保留，仍待重建。
- `t10-1` 实际通过全部 14 项词库／候选／恢复检查；离屏测试仍有两条 RippleDrawable 软件 Canvas 错误。API29+ 已改为公开 RenderNode 的离屏绘制记录，API21 Bitmap 路径保留，不改变原触摸／布局／提交断言，仍待新轮验证。
- `editors1` 在 T9 无障碍标签回归超时，后续 WebView／40 轮／100 次显隐未运行。旧断言要求重复数字和旧标签顺序；新实际节点确认 `ABC 2`、`MNO 6`、`符号 方案`，已更新精确语义预期并添加实际标签诊断，未把旧失败改判通过。
- `talkback1`：真实 TalkBack FOSS 服务在全部 27 份观察帧保持绑定和触摸探索。通过真实内核触摸设备完成全拼 `n`→选择“你”、方案切换、九键 `64`→锁定 `mi`→选择“米”；独立只读编辑器 oracle 确认每步无误提交，最终为 `T13_FAKE你米`。Root 实际核对 12 张关键原图及节点 XML。曾误把全拼预编辑 `n` 预期为直接 Latin 提交，原两项 false 比较保留并更正解释，未修改原记录。只覆盖 trime 主题、竖屏字体 1；不代表当前 Google Play TalkBack、实际语音听感或真机。
- 同一真实会话将字体设为 2 并旋转，发现横屏输入法占满 `[0,48][1184,720]`，编辑框被遮挡。原第 25 张 PNG 保留，正在修复短屏高度与音节区布局；新增整个 InputView 与完整编辑框的跨窗口几何断言，不能只检查按键外框。
- 短屏修复已落盘，尚未设备复测：`T9DisambiguationView`、`InputView`、`KeyboardWindow`、`Keyboard`、`T9LayoutPolicy` 及其 JVM 测试调整空态行、侧栏和高度预算。短横屏的音节／来源注释进入可滚动侧栏，三项操作保留 48dp 目标；空态侧栏用 INVISIBLE 保持占位，避免首个输入结果改变按键宽度。高度预算包含窗口安全区、字体对应的输入栏、控制区和编辑器预留，并在真实键盘模型中约束用户高度设置；刷新等待未结束触摸／延后动作，不能因显示更新取消输入。低于四行 48dp 的预算保留完整模型并提供滚动回退，常规矩阵仍要求全部按键完整可见，不能用回退放宽验收。极端窗口中顶部控制区本身也装不下，以及回退的实际滚动／长按取消，仍须另行验证。
- 追加符号页复测尚未执行：同文风的 `symbols` 使用 `ascii_mode=0`，切页明确保留组合；未提交九键输入切到该页时，T9 状态可仍启用，但当前布局按非 T9 页面撤侧栏和高度限制，可能重新遮挡编辑框。这只是源码风险，不是设备复现。待核心矩阵后，以大字号横屏实际输入 `64`、点“符号”再返回，核对整个输入法／完整编辑框、原组合与编辑框内容；步骤保存在 `build/t13/T9-short-viewport-layout-fix1/auxiliary-followup.json`。
- 字体／旋转、无障碍服务设置均恢复并读回，原文字保留。Observer 正常 stop，宿主实际退出 0；原 stdout `passed=true`／`INSTRUMENTATION_CODE: -1` 与 ready／finished 的会话、boot、PID/starttime、APK 身份由 Root 一致核对。见 `build/t13/api35/talkback1/root-scope-review.json`。
- 原运行日志保留 7,117 条 W/E/F。初始化阶段（Trime 安装前）有一条真实低水位回收，涉及系统 cellbroadcast 进程；没有将其记作 Trime 崩溃或“全 PID 零事件”。系统残余警告与未执行的性能／WebView 路径仍须如实保留，本轮不是稳定版验收。

## 实现范围

- 按键以真实 View 暴露名称、Button 角色、修饰键状态和可执行动作；无障碍激活沿用输入事件顺序，探索不提交，动态回车名称对应当前编辑器动作。
- 候选完整文本、可见注释、选中状态、选择及菜单动作；拼音选择、音节编辑和分页动作保持原引擎 revision／snapshot 校验。重绑后未重新查询的无障碍节点拒绝点击；Android 动作没有旧节点标识，重新查询后无法区分更早排队的动作，不能宣称解决所有跨服务时序。
- 预编辑文字提供字符级光标移动和选区信息，保留 UTF-16／UTF-8 边界转换及旧文本检查。工具栏提供展开、收起、返回、隐藏等名称。
- 候选默认字号也按实际字体边界计算行高；拼音行保留系统字体缩放。横向安全区扣除系统已完成的布局补偿，键盘宽度回退使用当前窗口尺寸。
- 首次设置焦点恢复警告单独核查和修复，保留草稿、检查点与高级配置返回行为。

## 验证计划与界限

以单线程、896 MiB 堆／640 MiB Metaspace 执行编译和 JVM 测试；代码收敛后，最终 Lint 使用独立 JVM 串行执行。模拟器保持单个串行运行，宿主可用内存低于 1 GiB 则停止本次所有受控重进程。文件索引保持运行。

专用 `t13` instrumentation 入口检查 Android 实际 View 节点和动作，并在真实输入连接上通过九键、拼音选择与候选选择核对唯一提交结果。两主题覆盖字体放大、横屏、深色、平板尺寸与窗口缩窄，旧编辑器／候选回归继续执行。节点动作测试本身不等同 TalkBack 验收。

用户确认暂时没有真机，先做模拟器。准备在 API 35 上补充 [TalkBack FOSS 13.0-f02](https://github.com/talkback-foss-team/talkback-foss/releases/tag/v13.0.0.473912301-f02) 探索／激活测试；该分支不是当前 Google Play TalkBack，且 minSdk 26，不能用于 Android 5.0。已下载 APK 31,236,571 字节，SHA-256 `1c19a0dd08e3f33f5ed41a8b2e7dd607ff46c492cb3fb05001169646014f5ff1`，v2／v3 签名校验通过，包含 x86_64；首轮 API35 已实际安装并运行，限定结果见下文。证书 SHA-256 `42b1f7f159e2e17b48f8e9cb67100617728c74bb8a77000616e913cecd53daa0`。真机探索可达性、实际语音听感、OEM 和物理 ARM64 结果仍待实测。

遵循 Android 官方关于[自定义 View 无障碍](https://developer.android.com/guide/topics/ui/accessibility/views/custom-views)及[无障碍测试](https://developer.android.com/guide/topics/ui/accessibility/testing)的节点、动作和真实服务交互要求，不以按键 TTS 朗读替代无障碍交互。

## 警告处理

T12 保留的 API 21／35 原始运行警告分别为 4,233／16,591 条。本轮按发生进程和调用路径区分项目代码、系统应用及模拟器问题；不删除原件、不降低日志等级来制造零警告。构建／签名警告与整个 Android 系统日志必须分开报告。后续记录每项修复的前后证据及未解决限制。


## check14 / API35 runtime7：横屏节点缺失的实际诊断

check14 只执行格式检查调整、设备测试编译与测试 APK 打包，真实退出 0，最低可用内存 3,473,272 KiB，构建警告搜索 0。唯一 app 输入变化为 `T13AccessibilityProbe.kt` 的失败诊断；主应用双 ABI APK 与 check13 完全相同。803 项 JVM XML 来自 check13，本次未重跑，冻结报告明确 `all_xml_fresh=false`；Lint 未执行。新测试 APK SHA-256 `bb04c213a8aab279348086ccaf5a4f784171075fc7cb7491071fd28d83389e00`。

runtime7 的 setup6 14 项检查通过。系统 Settings 锚点使实际显示先稳定为 1280×720／r1；landscape-diagnostic3 仍真实失败，前七条阶段检查通过后，等待拼音动作超时，native false／完成码 0，外层实际退出 1。失败前取得 512 节点以内的原始语义诊断：实际侧栏 `mi / ni` 均显示、启用、附着，但 delegate 为 null，角色为 TextView，点击只有默认无标签动作；本地与预期字符串均为 `Choose pinyin`。唯一侧栏 RecyclerView 的 pending/computing/suppressed 均为 false，排除本次捕获时的更新繁忙及标签语言不一致。同期真实系统 `accessibility_enabled=0`，无已启用服务。

[原始节点与 Root 复核](../../build/t13/api35/landscape-diagnostic3-root-review1/report.json)保存失败原件绑定。精确 RecyclerView 1.4.0 官方源码显示：服务关闭时绑定不保存原 delegate，而进入回收池时仍可恢复为 null；当前条目只在构造器装委托，存在回收复用后丢语义、默认点击绕过已发布身份守卫的源码路径。该路径还须用真实同一 holder 入池／出池回归直接验证，不能把源码推断冒充已记录完整运行时调用链。

Root 恢复原 user_rotation=0、accelerometer_rotation=1，实际 720×1280／r0，正常关闭模拟器。VM／控制器实际退出 0，清理错误为空、无残留，最低可用内存 1,979,004 KiB（约 1.89 GiB），1 GiB 保护未触发。未跑完整矩阵，未生成通过凭证。


## 拼音回收条目修复（check15 构建通过，设备验证进行中）

新增 `BoundActionTextView`，将可访问 Button 角色、完整名称／动作和身份检查放在条目 View 的覆盖方法中；RecyclerView 原 ItemDelegate 和 collection 信息保留。无障碍点击先检查控件启用／附着／可见、列表没有 pending/layout/suppressed，再进入原点击回调；旧 revision／完整 span 身份在重绑时仍清除。非回收按钮继续使用原委托，原触摸处理与点击监听段不变。

设备候选 helper 原 48 项检查全部保留，新加 10 项：真实生产 ChoiceAdapter、同一 holder 明确入池／出池各一次、列表更新期间拒绝旧点击、重新绑定后未查询节点拒绝点击、重新查询后 Button／动作／选中态，以及只执行一次完整 `62/Lock/0/2/ni` 回调。实际 AccessibilityManager 状态、委托及 collection 信息写入每主题报告，测试不切换服务，也不把假引擎回调当真实 Rime 锁定。生产 diff、测试 diff 和独立源码审阅分别见 `build/t13/t9-choice-intrinsic-accessibility-fix1`、`build/t13/t9-choice-pool-reuse-regression1`；编译和实际运行结果尚待后续补充。


check15 真实构建及监护退出 0：应用／测试 Kotlin 编译、双 ABI Debug APK 与设备测试 APK 打包通过，803 项应用 JVM 本轮新执行通过（83 份 XML，失败／错误／跳过均为 0），构建警告搜索 0，最低可用内存 3,346,712 KiB（约 3.19 GiB）。未在本轮执行 Lint。新增未跟踪 Kotlin 文件的格式变化已逐项读完整 diff，仅换行／缩进／尾逗号；不能把 guard 的 tracked_source_unchanged=true 扩大为全源码从未变化。623 项 app 输入、工作区补丁和 APK 已冻结；本轮 x86_64 APK `903ce3969e4f54594391744d5100554611f49cb18fbe48b5d58b601fc4059db4`、ARM64 `2a2ab4d9315f4787d4e60770f64661ab3b27bc8f10a9f06bfeab957d84f99cbb`、测试 APK `48ad61cf3a1e3d431ca9e41d91a0f19472ebd2a76a3dcd90c1281898aac02d33`。Android 15 runtime8 正在运行，尚无完整设备通过结论。


API35 runtime8 已闭合：setup7 14 项通过；landscape-diagnostic4 在新增 `t9-pool-same-holder-put-and-get` 断言失败，尚未完成候选 helper 或实际拼音输入动作；native false／完成码 0、宿主退出 1。原件未保存失败时池中的 holder 身份，不能唯一归因。精确 RecyclerView 1.4.0 代码确认容量 1 的池满时会直接丢弃新入池 holder，前置 han→mi 更新可能留下其他旧 holder；仅清计数日志未清真实池不能保证下一次取回同一条目。后续将仅修正 fixture 的池／prefetch 准备并补失败详情，保留同一 holder 入池／出池各一次、拒旧点击和唯一正确回调的原门槛，生产修复不变。

本轮已恢复原旋转 0／传感器 1、实际 720×1280／r0；VM／控制器均实际退出 0，清理错误为空、无残留，最低可用内存 2,161,444 KiB（约 2.06 GiB），1 GiB 保护未触发。该轮仍不是设备通过凭证。

## check16：仅修正池回收测试夹具

check16 的唯一 app 输入变化为候选设备测试 helper：禁用其独立列表的预取，读回实际缓存容量为 0；在旧节点已发布且原 holder 仍附着时记录真实池计数、清空其他池条目并核对清后为 0。原 58 项断言保留，新增 3 项准备检查；同一 holder 入池／出池各一次、旧动作拒绝和唯一完整新版本回调均未放宽。失败异常直接带池计数、holder 身份、入／出池记录及布局状态，不依赖成功报告才保存。生产代码不变；源码差异见 `build/t13/t9-choice-pool-preparation-fix2`，Root 与独立源码审阅均已完成。上一轮因缺少实际池详情，仍不能唯一追认失败根因。

[check16 原始记录](../../build/t13/debug-check16/result.json)和宿主实际等待均退出 0，设备测试编译／打包完成，无格式变化或构建警告；最低可用内存 3,336,608 KiB（约 3.18 GiB），保护未触发。主应用双 ABI APK 与 check15 完全一致，新测试 APK SHA-256 为 `78801d8e00b62f7e967b5d00c0cf99fbeee43996093f681ce10cc80b3bfb44cf`。冻结的 803 项／83 份 JVM XML 沿用 check15，`all_xml_fresh=false`，本轮未执行 JVM 测试或 Lint；这不是设备通过或发布凭证。runtime9 将使用该主包／测试包组合继续验证。

## check16 / API35 runtime9：候选复用通过，符号页滚动仍失败

setup8 的14项首次设置检查通过，实际宿主退出0。landscape-diagnostic5实际退出1，native false/完成码0；首主题61项候选检查通过，真实同一holder入池/出池各一次，旧节点动作拒绝，新节点仅回调一次完整`62/Lock/0/2/ni`。该轮manager/touch exploration均false；复用前真实池有一个旧choice，清池后两type均0。实际键盘的64输入、拼音动作可达与锁定且未提交通过。

后续符号页返回键完整可见检查超时：134×100px按键只显示134×84px，底部裁16px；直属内容592px、视口576px。原件没有记录请求时的局部坐标、返回值和scrollY，不能唯一断言运行时根因。源码另发现按键用translation定位，Android上溯滚动请求使用layout left/top，正在修复实际布局并补原始滚动/SHOW_ON_SCREEN与父坐标触摸回归。没有改写原失败，第二主题、截图和完整矩阵未完成。

原旋转0/传感器1已恢复，实际显示720×1280/r0；runtime9控制器与模拟器正常退出0、无清理错误或残留，最低可用内存2,150,248 KiB，保护未触发。Root原件复核见`build/t13/api35/landscape-diagnostic5/root-scope-review.json`。

## check17：真实按键布局与独立 Lint

按键改用模型绝对坐标执行实际 layout，移除仅用于定位的 translation，保留测量、触摸区域和原点击链。设备测试保留原 15 项键盘检查，另加入附着 ScrollView 的下排按键滚动、API23+ 标准 `SHOW_ON_SCREEN` 不提交内容，以及父容器坐标触摸唯一正确回调；LTR／RTL 仅在独立夹具中验证，实际应用不新增 RTL 支持声明。符号页返回检查仍使用原按键的 `requestRectangleOnScreen`，在同一 30 秒内等两次实际帧的稳定布局，记录请求坐标、返回值和 scrollY；完整可见、组合与编辑器身份断言未放宽。

[check17 构建复核](../../build/t13/debug-check17/root-scope-review.json)及实际命令均退出 0，803 项应用 JVM／83 份 XML 本轮新执行通过，Debug 双 ABI 和设备测试 APK 已冻结，构建警告 0，最低可用内存 3,443,140 KiB（约 3.28 GiB）。本次三文件变化已复核；格式调整只涉及键盘测试 helper 的换行／尾逗号，生产文件与审阅稿一致。x86_64 主 APK SHA-256 为 `744f4fbca9bc13358a826113152cd8999f7fb8648c03e9be5d63bf2538c148fb`，测试 APK 为 `f56688dc0021c1c97f5aec0fd3ce8ec6c8e055db0ca680da60420f40984eb979`。

[独立 lint-check17](../../build/t13/lint-check17/root-scope-review.json) 实际等待退出 0，Debug／Release XML 均为本轮新生成、各 0 问题，命令日志警告 0；最低可用内存 3,094,524 KiB（约 2.95 GiB），保护未触发。上述构建和静态检查不代表新的 Release APK、运行日志零警告或设备通过；runtime10 将继续验证冻结产物，全部旧失败原件保留。

## check17 / API35 runtime10：短横屏通过，矩阵前中断

[landscape-diagnostic6 原件](../../build/t13/api35/landscape-diagnostic6/native-artifacts/report.json) 的 31 项阶段、双主题各 61 项候选 helper、23 项键盘 helper 均通过；Root 与[独立图像复核](../../build/t13/api35/landscape-diagnostic6-independent-image-review1/report.json)实际查看六张原始 PNG：两主题符号页 Return 完整可见，返回后的截图确为九键，锁定组合保持，候选提交后编辑器显示“米”，未见编辑器遮挡。这仅覆盖字体 1 的短横屏诊断，不代表完整矩阵或真实 TalkBack 通过。[setup9](../../build/t13/api35/setup9/phase-report.json) 另通过 14 项检查。runtime10 收到 SIGTERM 后正常清理，18:28:30 UTC 是结束时间，不能当作精确收信时间；VM 退出 0、控制器退出 1，最低可用内存 1,944,044 KiB，无 STOP，未触发 1 GiB 内存阈值。信号发送者未知，不能据此归为产品错误或 OOM。[matrix6](../../build/t13/api35/matrix6/matrix-report.json) 因控制器停止而完成 0 项业务检查，失败及原件保留，整轮未通过。其后另用 fresh-home11／runtime11 执行 matrix7，结果见下节。

## check17 / API35 runtime11：前三组通过，字体 2 横屏遮挡失败

[setup10](../../build/t13/api35/setup10/phase-report.json) 的 14 项检查通过。matrix7 的字体 1 竖屏、字体 2 竖屏、字体 1 横屏各通过 31 项检查，独立审阅者实际查看每组六张原始 PNG，记录见 [01](../../build/t13/api35/matrix7/01-phone-independent-image-review1/report.json)、[02](../../build/t13/api35/matrix7/02-phone-large-independent-image-review1/report.json)、[03](../../build/t13/api35/matrix7/03-landscape-independent-image-review1/report.json)；Root 本轮仅另实际查看第 02 组两主题的 before-candidate-click 图，不记为 Root 看过全部 18 图。第 04 组字体 2 横屏真实发现编辑器被遮挡 15px，后四组未执行；[矩阵原件](../../build/t13/api35/matrix7/matrix-report.json) 保留失败，`restored=true`、恢复错误为空。runtime11 的 VM／控制器均正常退出 0，无残留，最低可用内存 1,425,984 KiB，文件索引继续运行。后续三文件窄修使编辑器预留不小于可用窗口的 1/4，同时保留原字号最小预留，新增两项 JVM 回归；check18 构建进行中，尚无新设备或整体验收通过结论，未提交或发布。

## check18 / API35 runtime12：遮挡消失，返回候选仍裁切

check18 实际构建退出 0，805 项应用 JVM 新执行通过、构建警告 0；setup11 的 14 项检查通过。matrix8 前三组各通过 31 项检查，每组六张原图已独审，记录见 [01](../../build/t13/api35/matrix8/01-phone-independent-image-review1/report.json)、[02](../../build/t13/api35/matrix8/02-phone-large-independent-image-review1/report.json)、[03](../../build/t13/api35/matrix8/03-landscape-independent-image-review1/report.json)。第 04 组字体 2 横屏的编辑器遮挡已实际消失，但返回后的候选高 126px、仅 120px 可见，裁切 6px，且 `layoutRequested=false`；Root 实际查看该失败范围的一张 symbols 原图。整轮仍为 false，后四组未执行，恢复为 true；原失败与[矩阵报告](../../build/t13/api35/matrix8/matrix-report.json)保留。runtime12 的 VM／控制器均正常退出 0，无残留，最低可用内存 1,881,280 KiB。随后窄修在真实页切换广播时将视口归零并停止旧滚动，同页刷新保留位置；probe 加入标准向后滚动与 Return 交错及 `scrollY=0` 检查，原完整可见、组合与提交断言保留，不倒推每轮曾有活跃动画。check19 已完成编译／打包、805 项 JVM 新执行通过、构建警告 0，最低可用内存 3,269,540 KiB；本轮未运行 Spotless／Lint19，旧 Lint17 不覆盖新源码。runtime13／字体 2 横屏短测和 matrix9 仍待实际结果，没有提交、推送或发布。

## check19 / API35 runtime13：切页归零通过，提交后按键仍裁切

[字体 2 横屏短测](../../build/t13/api35/font2-landscape-diagnostic1/phase-report.json)实际失败，保留 14 项检查及 trime 三张原始 PNG；[独立图像复核](../../build/t13/api35/font2-landscape-diagnostic1-independent-image-review1/report.json)已实际查看这三图。符号页到九键的 scrollY 从 122 归零、候选完整和精确提交“米”通过，末尾全键完整可见检查仍报 `Key clipped 12`：按键区 384px 加候选 126px，共 510px，超过 504px 视口，底部裁切 6px。runtime13 的 VM／控制器均正常退出 0，cleanup 为空、无残留，最低可用内存 1,896,212 KiB。下一步拟在提交后标准滚动整个 KeyboardView，仍要求全部按键同时全尺寸可见；尚未构建、执行或验收，不将该拟议测试修正记为产品通过。

## check20–22：视口节点与滚动准备已构建，设备结果待测

check20 测试单独打包实际退出 0、构建警告 0，最低可用内存 3,651,088 KiB；check21 主应用及测试编译／打包退出 0、构建警告 0，最低 3,468,948 KiB；check22 测试单独打包退出 0、构建警告 0，最低 3,559,780 KiB。三轮引用的 805 项 JVM 结果与 XML 均沿用 check19 缓存，未重新执行；尚无当前源码的独立 Lint，check17 结果不能替代。生产视口新增 `importantForAccessibility=YES`；节点测试严格检查真实 range／action，不再用 `canScroll` 的末像素公式代替节点语义。whole-keypad probe 的标准滚动准备仍保留所有按键同时全尺寸可见、31 项阶段及每组六张 PNG 的原契约，没有设备通过结论。

当前冻结的 check22 产物 SHA-256：x86_64 主 APK `ccc5d9afd0b6ab4dd254a54a4818989ac9b35d72608a0ae4f0d283fd5a03ba0e`，ARM64 主 APK `7e26719bd981c37dff5d9e68b45897cace47da0205169cebfbec4636f8680446`，测试 APK `04ba6d0cb3d94c59d510a015aa6738c9dcc4c66fe71be41b729aa06f64f253ca`。runtime14 正在启动，尚未取得任何新设备通过结果；原失败、未提交／未推送／未发布状态均保留。

## API35 runtime14：七组通过，宽屏标题夹具阻断末组

setup13 的 14 项检查及字体 2 横屏短测的 31 项检查通过，短测六张原图实际复核已封存。matrix9 前七组各通过 31 项；第 08 组平板横屏仅完成两项、截图 0，`KeyboardAccessibilityAssertions:315` 的长标题在宽屏可完整显示，夹具却要求出现省略，整轮仍为 false。恢复为 true、错误为空；VM／控制器均正常退出 0，cleanup 为空、无残留，最低可用内存 1,312,952 KiB。Root 只将独立标题夹具宽度限定为不超过 320dp，完整 glyph／role／ellipsis 断言均保留，check23 测试单独构建中，主 APK 未改。夜间配置 `ui_mode=33` 下实际默认 palette 仍浅色，不能升级为视觉深色通过；公开选择 Steam 后的真实深色重跑尚未执行。原失败、待验范围和未提交／未发布状态保留。

## check23 / API35 runtime15：八组记录通过，专项实际验收继续

check23 仅编译／打包测试 APK，实际退出 0、构建警告 0，最低可用内存 3,516,428 KiB；805 项 JVM 结果和 XML 沿用 check19 缓存，本轮未重跑，主 APK 与 check21／22 相同。setup14 通过 14 项检查；matrix10 八组各通过 31 项、保存六张当前 PNG，原 instrumentation 宿主均实际退出 0／完成码 −1，矩阵命令实际等待退出 0（14769），全部系统设置和显示恢复、恢复错误为空。[独立记录复核](../../build/t13/api35/matrix10-independent-record-review1/report.json) SHA-256 为 `885ae3b2f69c080094696f99c3762c8b13d43f50d5bc84f6757918862f6b9869`，核对当前主／测试 APK、48 张 PNG 的字节／尺寸、原 Bundle 和恢复，不授予 Root、真实 TalkBack 或整体验收批准。Root 本轮仅新看第 08 组六张 PNG；第 01–07 组的历史 matrix9 同主 APK 图审不冒称本轮逐图复核。night 配置只覆盖几何，实际默认 palette 仍浅。

随后[runtime15 终态](../../build/t13/api35/runtime15/result.json)因可用内存低于 1 GiB 正常受控关闭：控制器实际退出 1（70837）、VM 退出 0，cleanup 为空、无残留，最低可用内存 1,022,648 KiB，不能把矩阵范围通过升级为整轮通过。公开 Steam 选择及读回已完成，但 steam-dark-case1 在初始 installed 查询时因 Probe 内存保护失败（48669 实际退出 1），0 case、无 native、未更改配置，`restoration_not_needed=true`；未执行真实深色检查。当前 TalkBack／分屏等范围仍待实际验收，旧失败保留，源码未提交、推送或发布。

## runtime15 关闭后：内存恢复与交错回调修正待测

内存恢复观察实际等待退出 0（14550）；`recovery-after-runtime15.jsonl` 最后三次样本均不低于 4,456,448 KiB，VM 停止后文件索引保持运行。静态复核发现候选／拼音项同 identity 的 DOWN→service click→UP 可重复回调，不据此宣称已发生设备双提交。Root 已修改 `CandidateGestureFrame`／`VersionedClickBinding`，为触摸、queuedUP、service、重入建立互斥并使用独立 pending token；真实附着 View 的框架交错检查正在补充。新源码未编译、未验收，check23／matrix10 仅属于修前主 APK，视觉布局未改也不能替代新源码验证。[runtime15 警告限定复核](../../build/t13/api35/runtime15-warning-review1/interpretation.json)保留 6,836 W／3,828 E，项目 255 W／9 E，其中 8 条 FrameTracker、1 条 ImeBack；全部原件保留，不授予警告消除或整体通过结论。

## check24：交错互斥已构建，当前设备回归待执行

实际构建等待退出 0（89031，2 分 39 秒），805 项 JVM／83 份 XML 全部本轮新执行，失败／错误／跳过为 0。原 `command.log` 无 warning 行、命令保留 `--warning-mode=fail`，编译／构建警告 0；最低可用内存 2,779,164 KiB，保护未触发，构建期间已跟踪源码不变。冻结结果的 623 项 app 输入清单 SHA-256 为 `9296ded22aaff368061879e8f14ceb576636d846beabb39e63b9e5c3badfd5f5`。新增候选／binding 互斥及 39 项真实 attached-view 交错回归已编译，尚未设备执行，不以旧 matrix10 布局结果覆盖新源码。

Spotless 实际退出 0（25145），最低可用内存 2,604,688 KiB，纯格式独审通过；早一次 format1 在启动前被 4 GiB 保护拒绝，未执行，不记通过。此前构建等待 94428 在尚未启动构建时收到 SIGTERM、退出 143；随后仅将额外 4.25 GiB 等待余量恢复为既有 4 GiB，check24 等待 2 后才实际构建，4 GiB 启动／1 GiB 停止保护均未降低。

[冻结 APK 身份](../../build/t13/debug-check24/frozen/apks/identity.json)记录 x86_64 主包 SHA-256 `c0c29e0ed66bbadaf9d52f706a4c2361fdbec77f7315a6cc34dca89e02715de5`，ARM64 主包 `22094675dfe0f9fc65b405fb89bd75a5b7a7be2e648c53a630e4b39709fd4b19`，测试包 `9f32506b18936469c33ee5d5287393d8cc640958459af7faa03e878fc968e8b6`。runtime16 随后的受保护中止见下节；文件索引始终运行，源码未提交、推送或发布。当前源码独立 Lint／Release 与剩余设备范围仍待完成。

## API35 runtime16：安装确认后受内存保护中止

[runtime16 原终态](../../build/t13/api35/runtime16/result.json)已闭合，Root 实际 reap 控制器 58010 退出 1，VM 退出 0，`cleanup=[]`、`owned_remaining=false`，最低可用内存 1,001,532 KiB，1 GiB 保护触发。[setup15 原件](../../build/t13/api35/setup15/phase-report.json)实际退出 1（67904）；两份 check24 APK 已安装并读回 SHA 一致，但 `command-025` 仅到 fresh launch／IME 标记，没有任何首次设置 14 项通过结果。helper 15209 实际退出 0，不等同设备检查通过；新增 39 项交错测试尚未在设备执行。check24 的 805 项新执行 JVM／零编译构建警告范围保持，不能升级为当前设备通过。Root 准备优先验证较轻的 API21 VM，尚无新执行结果；旧失败原件保留。

## API21 准备轮：旧 shell 兼容与缺失 SD，未执行功能验收

[runtime1](../../build/t13/api21/runtime1/result.json)在 VM 启动前因可用内存不足 4 GiB 被拒绝，无 VM。runtime2 在连续三次可用内存至少 4 GiB 后启动，实际 argv 为 1536 MiB／单核；Root 正常 STOP 后，[原终态](../../build/t13/api21/runtime2/result.json)确认控制器实际退出 0（31506）、VM 退出 0，cleanup 为空、无残留，最低可用内存 2,373,496 KiB。该终态仅证明启动／停止与已记录准备步骤，不能当功能验收。

[setup1](../../build/t13/api21/setup1/phase-report.json)安装前原 stdout 明确 `printf: not found`；[setup2](../../build/t13/api21/setup2/phase-report.json)安装前的 `id -u` 实际输出完整 `uid=0(root)...`，不是预期的单个数值。[setup3](../../build/t13/api21/setup3/phase-report.json)使用 echo 状态尾行、完整 UID 与 exec-out 精确 XML 检查，check24 主 APK 安装读回 SHA 一致，随后 `mkdir /sdcard/Android/...` 返回 `Read-only file system`／remote 255 而失败；测试 APK 未装，未执行任何 native 功能检查。原 `run_probe.py` 保留，API21 v2 副本只作兼容且仍严格检查命令 status／UID／精确 XML，matrix v3 仅调整绑定和存在性 echo，没有产品修改。

[home1 准备原件](../../build/t13/api21/fresh-home1/preparation.json)的 `files_created` 只有 config 与 registry，实际没有 `sdcard.img`；此前“已准备 512 MiB SD”的说明应据此纠正。home2 的 mksdcard 512 MiB 原输出 `fsck -n` 实际退出 1（非法 SDCARD 标签、空闲计数 1,044,456／实际 261,113），旧镜像保存在 `home2/sdcard-mksdcard-original.img`，原 stdout／stderr 不改。Root 另用 mkfs.fat 建立新空 512 MiB 镜像，`fsck -n` 实际退出 0，见 [preparation-fixed.json](../../build/t13/api21/fresh-home2/preparation-fixed.json)；这只是新测试磁盘准备，不是设备通过。runtime3 等待者 54112 正等待至少 4 GiB 启动 home2，当前尚无 ready；其余验收仍待实际结果。

## API21 runtime3：首次设置原输出与矩阵前置记录

runtime3 已在 home2 ready（boot `1f4d8558…`、PID 1566824／starttime 10666683）；Root 实际 FD／mount 控制退出 0（70913），确认原 SD FD 只读、overlay 可写，vfat／fuse 实际 rw。setup4 实际退出 0（1928）、14 项原检查输出通过，原 `passed=true`／完成码 −1，check24 主／测试两 APK 读回一致；helper 7486 实际退出 0。后续完整日志扫描发现同一 PID 的真实主线程崩溃（见下方纠正），因此这些原输出不得解释为首次设置验收通过。

matrix1（v3）因旧系统不支持 `am get-current-user` 在前置失败，0 case。matrix2（v4）首阶段仅 editor attached，input view 等待 30 秒超时／native false，实际退出 1（6400），`restored=true`／错误为空；matrix3（v4）加同 IME 重选 helper 后仍同样失败，实际退出 1（43958）、恢复 true／错误为空。helper 86823 实际退出 1：首 case control 返回 0 后等待第二阶段时矩阵已经失败，不记功能通过。

实际记录表明 API21 `am instrument` 会 force-stop 同包 Trime IME；T12 正式编辑器 runner 原先先选 Latin、等待编辑器稳定再选 Trime。新 API21 Probe v3 只在 t13 前置选 Latin 并读回 default；matrix v5 同时严格要求 pm 唯一 0 及 processes 五行完整 started-user 0，独立源码审阅指出旧 pm 单项检查不能排除被隐藏的 partial user 边界。当前 matrix4 实际命令 98581／helper 41599 正在运行，尚无通过结论。app／test 源码和 check24 APK 未改，runtime3 未结束，文件索引保持运行；原失败均保留。

## API21 matrix4 终态：服务进入通过，精确滚动夹具失败

[matrix4 原件](../../build/t13/api21/matrix4/matrix-report.json)实际退出 1（98581）、`restored=true`／错误列表为空；首阶段通过 editor attached 和 input view visible，确实由先选 Latin、实际编辑器 barrier、公开选择 Trime 进入同进程服务。随后 `KeyboardAccessibilityAssertions:196` 报 `Scrolled key retained stale popup coordinates`，精确 scroll13 检查失败，native false／完成码 0，仅两项阶段检查、截图 0，尚未执行 39 项候选交错回归。helper 41599 实际退出 1：首阶段 control 返回 0 后等待第二阶段时矩阵已失败，不当功能通过。Root 正常 STOP 后，[runtime3 终态](../../build/t13/api21/runtime3/result.json)控制器 54112／VM 均实际退出 0，cleanup 为空、无残留，最低可用内存 1,454,560 KiB，保护未触发。

Root 对比官方源码发现 API21 `getLocationInWindow` 使用 `(int)(pos + .5)`、API35 使用 `Math.round`，负坐标取整确有差异；本次失败原件未记录该夹具实际 bounds，不能声称已测出 −12px 或确定唯一根因。仅测试夹具 top 移至 14px，使 scroll13 前后坐标非负；原 exact13、原 Rect 恢复断言均保留，并补实值日志，产品源码／主 APK 未改。随后因发现 setup4 的真实崩溃，check25 等待者已在构建启动前停止，未执行 Spotless／测试编译打包；805 项新执行 JVM 仍为 check24，不重复计数。文件索引保持运行，旧原件与失败均保留。

## API21 setup4 结论纠正：同一进程真实崩溃尚未修复

Root 完整扫描 runtime3 连续日志第 7573–7601 行，实际看到 setup4 PID 2766 于 `04:27:01.852` 报 `FATAL EXCEPTION: main`，`Session setup-closed-loop-… not established`，调用链为 `Key.checkKeyAction → KeyView.accessibilityAction → refreshAppearance → KeyboardWindow.onRimeOptionUpdated`。14 项／`passed=true`／完成码 −1／宿主退出 0 的原件仍保留，但同一 PID 结束前的真实应用崩溃使首次设置验收不能通过；不将此 fatal 解释为 instrumentation force-stop。目前旧 session 引用问题未修复，须先修复产品生命周期再编译、复验。

Root 由 FD 精确识别尚未启动的 check25 等待者 PID 1581593／starttime 10745500，并停止它：工具 6259 实际退出 143，无 Gradle 启动；[停止记录](../../build/t13/debug-check25-memory-wait-stop.json)保留 `build_started=false`。当前无构建／VM，未产生 check25 验证结果，check24 的 805 项新 JVM 执行范围不变。


## typed 关闭边界完成源码审阅，check27 仍待启动

`launchOnReady` 保留 Unit 返回和原 owner scope，仅同步关闭时不调度、异步 typed 关闭转为带原 cause 的取消；普通非法状态／native 异常和原取消继续传播。Root 将 `ensureEstablished` 改为具体关闭异常、ThemeLoader 的 session 检索移入已有结构化失败边界。Root／Observer 源码复核无阻断，新 7 项 JVM 与 freeze3 设备 helper 均未执行，旧 fatal 仍须实际复验。check26 的[停止回执](../../build/t13/debug-check26-memory-wait-stop.json)绑定 PID1586589／starttime10868244、`build_started=false`，工具45485实际退出143；旧原件保留。check27脚本 `run_debug_check27_when_ready.py` 的91430只表示等待开始，尚无Gradle启动／构建通过结果，仍遵守原4GiB启动／1GiB停止门槛，索引不动。


## check27 等待结束：内存门槛未满足，未启动构建

[真实结束回执](../../build/t13/debug-check27-memory-wait-completion.json)记录91430的终态chunk `55a3ac`／退出124，原stdout为 `Memory wait expired without starting a build`。720个样本最低2,918,588／最高3,688,880 KiB，无样本达到4,194,304 KiB；没有构建命令文件、Gradle／VM或等待者残留。Root终核12份生产审阅输入SHA仍一致；新7项JVM及设备helper尚未执行，不产生构建成功、失败或fatal修复验证结论。索引PID1062／1014052继续运行，当前没有自动续跑任务；原4GiB启动／1GiB停止门槛未放宽。


## check28 构建通过，runtime4 两项设备验证仍失败

check28实际32374／终态 `604c79` 退出0，保护及冻结退出0；812项JVM／84份XML均本轮新执行，构建警告0，最低可用内存3,274,548 KiB。新增7项typed关闭JVM回归已执行通过；[独立记录报告](../../build/t13/debug-check28-independent-record-review1/report.json)7,897字节、SHA `bb96a371ca742e7816c29103192d8486a7fcbc556ad68b98242f8ac467bb65d5`，范围为闭合构建记录，不代替设备或最终Lint／Release验收。

[runtime4终态](../../build/t13/api21/runtime4/result.json)由Root正常STOP，27398／`a81d7c`实际控制器／VM退出0、cleanup为空、无残留，最低可用内存3,332,876 KiB。setup5实际71385退出1：原14项通过，新增缓存teardown前5项通过，随后已detach的KeyView调用createAccessibilityNodeInfo触发mAttachInfo NPE，被测试捕获，原native false／完成码0；不能把它称为未捕获进程fatal，也不能把原14项或ADB传输0当整体通过。matrix5实际75096退出1：4项阶段检查与keyboard21通过，原exact scroll13回归通过，但候选next-page-is-labelled-button断言失败，0张PNG，后续配置未运行；restored=true／恢复和失败清理错误均为空。helper3125实际退出1，只完成首case实际选择0，不能扩大为后续通过。

[本轮警告扫描](../../build/t13/api21/runtime4-warning-review1/report.json)实际3a6baa／子进程wait0，128MiB／nice10、峰值22,252 KiB、stderr0，原输入绑定前后相同；[有限解释](../../build/t13/api21/runtime4-warning-review1/interpretation.json)保留1656W／745E／0F和项目8W／14E，全PID未匹配fatal／am_crash／am_anr／旧Session异常、真实LMK kill及内核critical。lowmemorykiller的两条启动支持诊断与一条连接通知不记作kill；所有原文保留。这仅是有限未复现，缓存teardown第6／7及可选第8项、候选39与完整设备回归尚未完成，不能宣布旧fatal或系统问题消除。check29源码修正尚未构建，fresh-home4／API35home17仅准备未启动，索引继续运行。


## check29／API21 runtime5：编译通过，配置拒收与编辑器中断

check29 实际25513／`9dc3e2`退出0，保护及冻结0；812项JVM／84XML全部新执行、失败／错误／跳过0、构建警告0，最低3,480,308 KiB。[独立核对](../../build/t13/debug-check29-independent-record-review1/report.json) SHA `1eb5db4d186246b58a5bb6192afb110ccf33bc21237e93a5255b93e27c6e3c9d`绑定3APK全SHA、628项源／链接目标和29项未跟踪ZIP；4处Spotless差异仅换行／缩进，缓存helper与构建前原件字节相同。本轮未跑Lint／Release。

[setup6](../../build/t13/api21/setup6/phase-report.json)输出原14项及独立缓存7项通过；关闭probe自有session后仍有其他owner，第8项关闭后无session分支未覆盖。[matrix6](../../build/t13/api21/matrix6/matrix-report.json)实际12697退出1：首组字体1的31项、两主题各63候选／39交错通过，Root实际看该组6张原图；第二组native31为true但三阶段实际字体均1、请求2，宿主拒收，第二组未作视觉通过，横屏未跑。设置恢复true／错误为空，helper89845退出1；[独立记录](../../build/t13/api21/matrix6-independent-record-review1/report.json) SHA `2eebb65a2e4117493a801a2daba0c4b9cb44bca6e29ba5653776f441c5d7f84c`保留这些边界。

[editors-check29-1](../../build/t13/api21/editors-check29-1/phase-report.json)实际86212退出1，仅保留双主题部分原stdout；无native终态／报告pull，不计完整编辑器或100次显隐。[runtime5终态](../../build/t13/api21/runtime5/result.json)控制器72770／`824196`退出1、VM及logcat0、cleanup为空、无残留；记录signal15而无STOP，最低3,408,596 KiB，不是内存阈值停止，发送者／原因未知。[警告解释](../../build/t13/api21/runtime5-warning-review1/interpretation.json)保留1791W／897E／0F、项目15W／34E；本轮fatal／am_crash／ANR／旧Session异常／真实LMK和内核critical未匹配，不能据有限未复现关闭旧fatal或系统问题。

新增ThemeUtils12条与矩阵夹具的AppCompat构造相符；Root已在3个夹具构造点补`ContextThemeWrapper`及清晰类型名／注释，已随check30编译，尚未设备复验。API21字体设置helper实际`07c866`退出0，javac／D8均0、零警告，见[宿主编译记录](../../build/t13/api21-font-configuration-build1/result.json)；它不构成大字体配置生效或任何设备通过证据。

check30实际构建／保护／冻结均退出0，`tracked_source_unchanged=true`，最低3,511,604 KiB；[闭合摘要](../../build/t13/debug-check30/frozen/summary.json)明确812项／84XML非本轮新执行、`all_xml_fresh=false`，最新新执行结果仍为check29。测试APK中的3处主题构造修正已编译，当前[三APK身份](../../build/t13/debug-check30/frozen/apks/identity.json)与旧check29分开；本轮警告和产物独审待完成，没有新增设备、Lint或Release通过结论。


## API21 runtime6：check30限定回归完成，新候选仍待构建

[字体诊断](../../build/t13/api21/font-diagnostic1/font-diagnostic-report.json)实际76676c退出0，仅`diagnostic_passed=true`：AMS与DB读回2.0／1.0一致，精确恢复原AMS1.0及DB缺项，未改成整体验收通过。setup7原14项及缓存独立7项有真实通过输出；关闭自有owner后仍有其他session，可选第8项未覆盖。[matrix7](../../build/t13/api21/matrix7/matrix-report.json)实际d2b7b2退出0，四个竖／横屏字体1／2配置各31项及6张原图，实际字号与请求相符，设置恢复／错误为空；Root已实际查看本轮24图。

[editors-check30-1](../../build/t13/api21/editors-check30-1/phase-report.json)实际d8388f退出0；原`touch_region_metrics`与pull后的native JSON完全一致，两主题各20共40轮，show最大390ms／clear最大147ms是含队列和轮询的测试墙钟，不是候选P95。100次显隐有完整10个原里程碑及终态；两主题global index22分别精确恢复−83／−73px，重开／两次relayout及唯一提交、缓存KeyView标签更新、resident复用、最后锁定组合和取消后文本不变均有原检查输出。[独立记录](../../build/t13/api21/editors-check30-1-independent-record-review1/report.json) SHA `929a227ba6685fc1a3135e2973dc87c4d91cfc89fa85006fefc976cffac51e4d`不代替TalkBack服务或全日志批准。随后[五项串行回归](../../build/t13/api21/check30-regressions1/actual-exits.json)privacy／t08／t10／t11／feedback各实际退出0，原native true／完成码−1。

Root实际034cce写STOP后，[runtime6终态](../../build/t13/api21/runtime6/result.json)控制器79389／`7acee2`退出0、VM／logcat0、cleanup空、无残留；450样本最低3,253,812 KiB，保护未触发。五项串行回归session20051／`bbc23e`实际退出0。[完整警告扫描](../../build/t13/api21/runtime6-warning-review1/report.json)实际e0c8eb／子进程wait0，128MiB／nice10、VmHWM22,964 KiB，保留2177W／1320E／0F及项目149W／92E；本轮ThemeUtils、旧Session异常、fatal／am_crash／ANR、真实LMK及内核critical未匹配，不能删除或关闭runtime3 fatal、runtime5来源未知signal15和其他系统警告。[有限解释](../../build/t13/api21/runtime6-warning-review1/interpretation.json) SHA `10576036bcd8dc5b64fc2fddb5f163613da6f6252430506af390555196329894`保留剩余AudioTrack／GL／WAL等原文。

以上只绑定check30 APK。Root其后已应用10处同文风功能键配色，并将候选身份设为`3.3.13-t9.16`／`20261121`；新候选尚未构建，不把既有check30或历史t9.15升级为它的验收。代码候选提交不等于PUB或发布；后续compile／JVM／package与fresh JVM Lint串行分开，最终两平台和实际深色／TalkBack／分屏／SAF仍待闭合。
