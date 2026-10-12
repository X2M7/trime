# T11：轻量离线下一词联想验证记录

状态：**代码已实现，本轮稳定版验收通过**。版本 `3.3.13-t9.14`／versionCode `20261119`，产品源码 `06ef3dcafb3b64f170be39c7c3c0160211e5209a`。Build3、最终宿主及原生检查、Android 5.0／15 的八阶段功能和两主题正式版实点均已通过。线上发布及 Latest 状态以[版本 Release 页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.14)和归档外发布回执为准；本记录不把设备验收通过写成已经推送或发布。

## 实现范围

- 默认关闭的全局“下一词联想”设置。随附全拼、简化字及九键方案接入 predictor、predict_translator 和匹配模型，保留原有拼音补全、整句排序。用户已有 custom 配置不覆盖；未接入组件的第三方方案不会因应用开关自动获得预测。
- 原创 CC0 离线示例模型：34 个上下文、76 个条目、6,364 字节，SHA-256 `2bc7ee4fe184c433b82a0650625e0fc4ecbc1c64352ea9aa36342d2c7a612add`。不是全面语言模型，也不从输入内容更新模型。
- 默认显示 5 项、连续点击 1 轮；非正配置回退默认值，正值硬上限分别 8 项、2 轮。只有当前候选快照的显式点击可提交联想。候选标记随同原 JNI 响应快照传递，不为每项额外查询 JNI，不读出全部候选。
- 数字、编辑键、标点、物理 Shift 按下／释放及宏先清联想，再处理实际按键；联想不作为编辑器预编辑文本。隐私、关闭设置、ASCII、输入框／方案切换及隐藏键盘清理预测上下文，不重放旧提交；正常未提交拼音保留。
- 模型仅在启用后的新提交中按需加载。采用同一 FD 非阻塞打开、普通文件检查、有限读取及私有字节快照 SHA 校验；缺失、损坏、管道、目录或设备文件保持基础输入可用。生成的原生覆盖文件保留上游 BSD 声明和 LICENSE，不修改子模块。
- 本轮仅支持随包 `trime_predict.db` 的完整 SHA 匹配，模型硬预算 64KiB、单查询数组最多 32 项；不同字节的同名第三方模型回退基础输入并保留原文件，不宣称已支持通用第三方预测数据库。
- 清理预测上下文和切换联想设置时保留普通候选的原始／置顶菜单关联，避免在未提交拼音上反复嵌套缓存；恢复默认排序仍能回到完整原始顺序。

## 当前实际证据

| 检查 | 当前结果 |
| --- | --- |
| Native host13 | 384 项实际原生检查通过，编译／运行退出码 0，编译日志为空，峰值 464756 KiB；31 个输入前后 SHA 一致，包括 5 个真实预测编译单元及抽取的 JNI／Session 生产方法。 |
| Root 独立原生复测 | 新运行目录执行同一冻结二进制，384 项通过，子进程退出码 0，峰值 21264 KiB；未重新编译，未重复计为 384 项新的测试用例。证据 `build/t11/native-root-replay3/`。 |
| 原生 CMake 检查 | 8 项通过，记录 `build/t11/privacy-cmake3.log`。 |
| 宿主／真实 schema 部署 | host3 新执行 227 项通过（215 项基线＋12 项 T11），包含数据和真实方案部署；最终文档提交的同组宿主复核不叠加为新覆盖，原生复测也单列。 |
| JVM、Lint、正式双 ABI APK | Build3 新执行 715 项应用 JVM 测试通过；17 项 build-logic 和 6 项 compatibility 为缓存结果，未算入 fresh。Debug／Release Lint 0 问题、构建警告 0，ARM64／x86_64 Release 签名警告 0。正式 APK 均 minSdk21、versionCode20261119、DEBUG=false，沿用既有证书。 |
| 当前原生构建日志 | `native-build-logs4` 绑定当前 ARM64 增量任务及 x86_64 Ninja no-work／AGP JSON 缓存命中；后者关联冻结的 Build2 原始编译日志及相同源码、配置、模型、对象和 APK `.so` 字节。当前及复用原文和结构化诊断警告 0，不把 task EXECUTED 当作新的编译或 configure 进程。 |
| 静态及 Release 测试链接 | Static2 双 ABI API21 ELF 强引用与 DT_NEEDED 闭包通过，weak／unresolved 均 0；两个新 JNI 和实际 DEX 接口、模型字节及签名核对通过。Build3 测试 APK 的 138 项必要宿主接口重新扫描通过；采集器原始 `passed:false`／设备语义未测字段保留，静态结果不替代实际运行。 |
| Android 5.0／Android 15 | 各完成旧 9.13 学习数据准备、9.14 Release 覆盖升级与三次便携备份，Release／Debug 各 30 项 T11，以及原有 T10／隐私／T08／编辑器检查；每台快速输入 40 轮、显示隐藏 100 次。最终 API21 使用新的 clean-sdcard cohort，旧环境退休。 |
| 正式版联想界面 | 两台 x86_64 模拟器各实点两个主题、每主题 3 项检查，共各 6 项；验证联想标记、点击准确提交、数字取消后开始新九键。截图、XML、动作及 Root 实际图审阅绑定，GUI 验收前备份范围内的配置／元数据／输入法选择恢复；原驱动和 owned process 正常退出 0。 |
| 原始日志 | 最终 API21 3,903、API35 8,198 条 W/E/F 及完整控制台原文保留。本轮 IME fatal／ANR、实际 LMK 和内核严重异常计数为 0；这不表示所有平台或测试环境诊断为 0。 |
| 推送／稳定版发布 | 本记录冻结验收结论；实际远端提交、tag、附件下载核对及 Latest 由归档外回执证明，线上状态以 Release 页面为准。 |

## 已保留的失败记录

早期 native-host1–5 的编译或运行失败保留；宿主安装的旧预测模块覆盖注册曾导致错误组件被使用，最终测试核验模块初始化来自当前二进制。`enabled-fifo-block1` 使用启用联想且无写端的管道模型路径，实际阻塞并被受控超时终止（子进程 -9）；host10 已加入该真实提交路径及后续数字输入回归，并修复文件打开方式。旧失败不改判为通过。

`native-ranking-repro11` 的测试适配层包含位置错误导致编译失败，记录保留。修正适配层后，`native-ranking-repro12` 在尚未修复的生产 JNI 上实际复现 100/100 次清理后菜单重包裹及恢复排序失败（编译 0、运行 1、未超时）。host13 在最小 JNI 修复后加入这一路径及 100 次设置切换回归并通过。Build1 因该必要修复受控停止，原始失败总结不改写；715 项 JVM／Lint 部分证据、停止记录及 RAM 守护日志保留，未触发内存底线，不是 OOM。

`api21-build2-runtime1` 实际 Release T11 在 `NextWordPredictionProbe.run` 抛出 `NoSuchMethodError`：独立测试 APK 调用的 `kotlin.jvm.internal.Intrinsics.checkNotNullParameter(Object,String)` 已被宿主 R8 裁剪。驱动退出 1，原始 instrumentation／全 PID 崩溃日志及部分升级证据保留；拥有的模拟器正常停止并等待退出 0，实际 LMK 为 0。Build2 内存守护最低可用 1,816,124 KiB，未触发停止。该轮失败不改判为通过；修复后使用新 APK 重新验收。

修复仅精确保留该 Release 验收及其清理回调实际依赖的 62 个类型身份和 60 个成员，未新增通配规则，原 `assumenosideeffects` 保持不变。测试中的八处调用显式传递原默认值，消除对 R8 可移动／裁剪的默认参数桥的依赖。先用 Build2 实际 DEX 逐项核对调用、声明、异常类型、继承和访问标志，再扫描 Build3 新 APK；Android 5.0／15 的实际 Release 及 Debug 运行通过后才闭合这项验收。

`native-build-logs2` 的原失败源于收集器误把真实单行 `[0/2] Re-checking globbed directories...` 当成编译工作；`native-build-logs3` 又把 AGP configureTask EXECUTED 当成 CMake 实际执行，拒绝了真实 JSON cache-hit。两份 `passed:false` 保留；V7 在新 `native-build-logs4` 目录精确接受已观察到的单次 glob 维护与三条 JSON cache-hit，并仍拒绝重复进度、其它编译行、警告或异常消息，实际收集通过。

API21 首次 GUI 已完成 6 项检查，却在收尾使用 `after-t11-gui` 角色名时触发冻结驱动的角色正则拒绝，原失败报告保留。新 GUI V3 仅把该字面量改为 `after-prediction-gui`；后续实际执行完整检查、恢复和正常停止，未更改 APK 或原 GUI 验收条件。当前两个 API 的成功均绑定 V3，而非把 V2 的部分成功冒充整轮通过。

旧 `api21-build3-runtime1` 八阶段功能通过，但启动 FAT 检查发现 7 条丢失簇链、共 72 个簇，且有 LOST.DIR 空间不足及 FSInfo 异常；它已退出最终稳定版 cohort。原报告、功能草稿、SD base 和 qcow2 都保留。使用 SDK `mksdcard` 创建的新介质在最终 `api21-build3-runtime2-clean-sdcard`／`api21-build3-release-gui3-clean-sdcard` 完成完整验收；首次挂载的 FSInfo 签名及 freecount 诊断后均为 `FIXED`，随后 `Filesystem check completed OK`，本轮完整原文无丢失簇链／LOST.DIR。该环境更换不是产品修复，也不把旧功能通过改成新功能失败；新介质的修复诊断仍原样保留。

GateV3 对已确认的原生 guest 便携备份 inline 快照严格核验精确 13 字段、独立串行 JNI worker 上下文、两 API 限定路径／UUID、严格有界 base64／UTF-8 字节、SHA／长度、Rime 导出头和词条、三次相同结果／返回 true／临时文件 0，以及 full／T9 查词保持。普通宿主绑定仍要求真实 regular 文件及逐字 SHA；未知或添加字段的 guest 形状拒绝。这不是通用 guest 路径豁免，旧 GateV1／V2 和原始采集器的 false 字段不改写。

## 验收边界

本轮实际 JNI、真实九键、内部 F4 方案切换、编辑器与隐私边界、缺失／损坏模型降级及普通置顶候选 100 次清理／100 次设置切换已在两个 x86_64 模拟器验证；联想与拼音补全、整句排序保持独立，不能以 `enable_sentence` 代替下一词联想。模拟器的编辑器响应门限不是候选 P95 测量。

API35 的一次性 Debug WebView Renderer（PID12371）出现 code -1，随后有 isolated-not-needed 及 Zygote 正常退出上下文；编辑器回归源码包含主动 `destroy`。目前只支持与测试销毁的时序关联，因果仍是推断，不能声称平台所有崩溃或错误均为 0。其余 Binder、文件系统、硬件服务和内核／宿主环境诊断均保留可见限界，分类接受不等于平台风险消失。

真机候选 P95、Java／native／graphics／总 PSS、长期驻留、首次部署、物理 ARM64、OEM／16KiB 页面、真实 TTS、SAF 选择器权限／取消及全部第三方方案／插件仍未验证。构建、Lint 和签名零问题不表示平台警告及风险全部消除。完整版本验收及附件范围见 [v3.3.13-t9.14 记录](VALIDATION-t9.14.md)；手动发布仅两个 APK 和含递归子模块的完整源码包。
