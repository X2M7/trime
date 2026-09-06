# T01 验证记录：3.3.13-t9.1

日期：2026-09-06。结论：拼音消歧功能已完成引擎和 APK 界面验证；**仍有启动 ANR，不能视为稳定发布验收通过**。

以上为 `t9.1` 历史构建的结论。后续 `t9.2` 的修复及复测见 [启动 ANR 回归](STARTUP-ANR.md)，不要将两个 APK 的结果混用。

## 源码和构建身份

- T00 基线 `4d934becedc2e7814f37cbea02f078843ea597c3` 已推送至 `X2M7/trime` 的 `develop`；未推送原始库。
- T01 是该提交之上的本地未提交修改。APK 内嵌 Git SHA 仍为基线 SHA，不能单凭它还原 T01。
- APK：`app/build/outputs/apk/debug/com.osfans.trime-3.3.13-t9.1-dev-arm64-v8a-debug.apk`。
- 包名 `com.osfans.trime.debug`，显示名称 `Trime`；这是测试构建，不是 release 签名包。
- versionName `3.3.13-t9.1`，versionCode `20261102`，ABI `arm64-v8a`。
- APK SHA-256：`ef76e63cc6ecc54ebcdd24c26f6a12a87b033c6737c8914397a9f7c2cb1c581b`。
- 签名证书 SHA-256：`c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`。
- 冻结目录：`build/baseline/ef76e63cc6ec-f57041353e7c/`，下文路径均相对仓库根目录。
- 冻结目录保留 `T01-tracked.patch`、`T01-new-source.tar.gz` 及各自产物摘要；配合基线提交还原本次源码。文档收尾晚于 APK 构建，运行时代码没有再次修改。
- 没有创建 T01 tag、推送 T01 或修改 GitHub Release/latest。

## 环境及数据隔离

只运行一个 `hk_api35` 临时只读模拟器，2 GB 内存、2 核，不保存快照；Android 15 / API 35，序列号 `emulator-5554`。系统构建：

`google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys`

模拟器为 x86_64，通过 `libndk_translation.so` 执行 arm64 库，图形使用软件渲染。这些结果不能说明手机性能。
竖屏 720x1600、横屏 1600x720、密度 280 dpi；横屏分别测试了默认关闭横屏模式，以及手动启用 `Landscape only`。

原生测试使用每次新建的 `/data/local/tmp/trime-t9-RUN_ID/user/`。真实 UI 使用临时 AVD 的应用专属目录，没有清空真实手机或个人词库。
UI 部署复用了旧 T00 冻结资源中内容相同的系统 table/reverse 二进制文件以减少编译负担；没有导入个人 userdb，也没有复用旧九键 prism。原子拼写 prism 由应用重新部署。
UI 测试中已产生学习，因此截图属于 `installed_uncontrolled`，不能拿来比较冷词库排序。为了绕开初始化期间的方案选择阻塞，仅在临时 AVD 的 `user.yaml` 中预设九键方案。

## 验证范围

| 层次 | 结果和证据 |
| --- | --- |
| 方案比较实验 | `build/t9/2a06421d834d/results.txt`：验证字母重查、实验约束过滤器及 `xian` 分词反例，最终采用原子精确拼写，不发布实验过滤器 |
| 原生控制器 | `build/t9/007df06b04ee/results.txt`：185 项通过，另起进程的 3 项学习持久化检查通过 |
| 库与 APK 对应 | 上述驱动链接从 APK 提取的库，其 SHA-256 为 `5e1d6abc27b9d8a75b606c9610670ef6b2c9a176332ec6d3aa3feaf90d55fd11`，与最终 APK 内库逐字节相同；驱动不是 Android View 测试 |
| Android 单元测试 | 最终构建运行 `testDebugUnitTest`，53 项通过，XML 位于 `app/build/test-results/testDebugUnitTest/` |
| T00 工具单测 | `python3 -m unittest discover -s script/baseline -p 'test*.py'`，26 项通过 |
| APK/JNI/触摸 | 最终 APK 已安装且经 T00 capture 校验安装包身份，详情见以下截图和 UI XML |

原生覆盖：同码 `ni/mi`、锁定后保留后续编码、逐段锁定、修改中段、解锁、撤销、继续输入、前删/后删、光标及 UTF-8 预编辑位置、分隔符、`xian` 与 `xi an`、补全 `646 -> ming`、多音字 `xing/hang`、特殊韵母、直接/部分汉字选择、学习边界及重启后学习、回车和英文切换不泄露私有编码。
全拼 `nihao` 和自然码双拼 `nihk` 的实际引擎输入通过回归；测试替换为小词典并关闭过滤器，不代表所有第三方方案兼容性，也不是全拼/双拼 Android 界面的完整回归。

## 界面证据

最终冻结目录中的截图都有 `screenshot.png.identity.json`，记录 APK、设备、采集时间和哈希；不要只转发脱离这些身份文件的截图。

| 采集目录/文件 | 实测结果 |
| --- | --- |
| `capture-b8521f9e1145` / `ui-mi.xml` | 竖屏输入 `64426` 后点 `mi`，显示锁定 `mi` 和后续 `426`，候选含“米高”，编辑器仍为空 |
| `capture-afd3177e7c06` / `ui-mi-commit.xml` | 点汉字“米高”后编辑器文本确为“米高”，拼音区清空 |
| `capture-3d1a1bdf41f9` | 横屏原始 `64426`，独立拼音区和汉字候选共存；默认未启用横屏模式 |
| `capture-0e4144ef7797` / `ui-land-ni.xml` | 横屏点 `ni`，保留 `426`、候选含“你好”，补全项单独标记，拼音区与按键无重叠 |
| `capture-a5ca561fd070` | 启用横屏模式后，内置九键高度为 200 dp，拼音区、候选区和所有按键可见 |

此前 UI 修复包 `0c26b58f35eb`（完整身份在 `build/baseline/0c26b58f35eb-ab739087a388/`）还验证了：竖屏选 `ni` 不上屏、点“你好”才上屏；`ni hao ma` 的中间段改成 `gao` 后仍保留两侧锁定，撤销回 `hao`，解锁恢复 `426`。对应 `capture-4fc39596d389`、`capture-83b93e81248e`、`capture-9278e62ae7b7`、`ui-unlocked-middle.xml`。最终包仅在此基础上调整横屏排布及主题横屏高度，原生库和竖屏交互代码未变。

## 资源校验

最终 APK 内九键方案 SHA-256：`9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f`。
资源集合摘要：`05df6f1e0cd576a4955b23452ef18b0bda60095341c3cd149f840dd0a39cc413`。
真实 UI 的系统词典、已部署方案、prism、table、主题及 OpenCC 校验和在最终冻结目录的 `runtime-sha256.txt`；测试词典和全部 fixture 校验和在原生测试 `identity.json`。

这次使用 T00 的 prepare/capture 固定身份，**没有为 T01 APK 重跑 T00 的 cold/learned 性能比较**。因此基础 `report.md` 中这两组保持 `not_run`；其汇总设备栏也是未执行引擎基准时的默认值，APK 界面所用设备信息在各 capture 的身份文件中。原生控制器的独立新词库/学习持久化测试不能冒充这两组比较结果。

## 已修复及未解决项

- 已修复 T01 中间构建的 `Unknown color` 崩溃，使用主题现有的 `candidate_background` 回退逻辑。旧 `f69de7b7606c` 包不能用于最终验收。
- 已修复横屏新增两行拼音控件与键盘重叠，横屏改为一行并排区域。若启用横屏模式，还会使用较低键盘高度；不自动修改用户偏好。
- **最终 APK 仍有一次启动 ANR**：模拟器时间 `07:33:41`、PID `6379`，主线程在 `ThemeLoader.loadTheme -> Rime.deployRimeConfigFile`。保存于最终目录 `startup-anr.txt`。这是尚未处理的主题部署同步阻塞，不能归结为已经解决的颜色崩溃，也不能只用“模拟器慢”掩盖。
- 较早 `0c26` 包部署期间的方案选择 ANR 位于 `schema-picker-anr.txt`，涉及 `KeyAction.getLabel -> RimeDaemon.runBlocking`；同样没有在 T01 中重构该路径。
- 最终 PID 的 crash buffer 无致命崩溃记录，后续输入实测没有新增 ANR 事件，但这不等于长期稳定性保证。完整事件文件包含旧版本历史，必须按 PID/时间区分。
- Trime 自带测试输入面板在矮横屏窗口中仍会裁切编辑框/类型选择区；不能用键盘区无重叠来宣称整个测试面板已适配。旋转时该测试面板会重建，不作为跨 Activity 重建的组合保留验收。
- 真机、release 构建、长时间快速连打、不同字体缩放和第三方主题仍需后续验证。建议先修复启动/部署阻塞再发布稳定版。

## 复测入口

安装本次测试 APK，启用 Trime，等待部署结束后选择朙月拼音九键，在测试输入面板执行 README 的操作步骤。
旧用户目录如果有同名 schema 会覆盖内置版本，需要合并新增规则并重新部署，不能删除个人词库来“修复”。横屏高度测试需在“虚拟键盘 / 启用横屏模式”选“仅横屏”。
原生驱动命令、方案决策与边界说明见同目录 `README.md`。本轮构建/测试使用单 worker、单 native 编译任务和受限堆，结束后关闭临时模拟器；不要求用户长期保留占用内存的测试进程。
