# v3.3.13-t9.14 验收记录

**本轮稳定版验收通过，线上发布状态以 Release 为准。** 支持 Android 5.0+，versionCode `20261119`，产品源码 `06ef3dcafb3b64f170be39c7c3c0160211e5209a`，包含上游 `8545278b9efccaaf5b222dd499f5954ebcf87269`。Build3、host3、原生独立复测及两台 x86_64 模拟器最终功能验收已通过。最终文档提交与产品源码仅允许七个发布文档不同；APK 使用成功 Build3 的冻结字节，不因文档提交重写。

## 功能与构建结果

增加默认关闭的轻量离线下一词联想，随附全拼、简化字和九键方案在提交汉字后显示建议。只有明确点击当前候选快照才提交；数字键取消旧建议并开始新九键，回车、空格、删除、标点、物理 Shift 及宏继续正常输入。关闭设置、隐私、ASCII、编辑器／方案边界清理上下文，返回普通输入后不重放旧提交。拼音补全、整句排序和下一词联想保持独立状态。

CC0 示例模型仅 6,364 字节，34 个上下文、76 个条目，默认 5 项／1 轮，配置硬上限 8 项／2 轮；预测模型不从输入内容训练，普通 Rime 用户词库学习保持原有行为。已有 custom 配置不覆盖，第三方方案须接入预测组件。本轮仅支持随包 `trime_predict.db` 的完整 SHA 匹配，不同同名模型、缺失／损坏模型及非普通文件回退基础输入并保留用户文件；不宣称已支持通用第三方模型。模型与安全加载细节见 [T11 记录](VALIDATION-T11.md)。

保留 T10 的全拼／九键共享 `luna_pinyin` 学习库和 `custom_phrase.txt`、个人词库导入／导出／备份恢复、常用短语编辑和自动备份、候选置顶／恢复排序、可选横向滚动、注释及长候选全文阅读。忘词是请求移除学习记录，不等于永久屏蔽系统词；失败显示输入法提示框。隐私模式不应用或修改置顶，联想候选不参与置顶或忘词。普通候选在反复清理联想／切换设置后保持未提交输入，恢复排序回到完整原始顺序。支持既有 Shift 双击锁定与自定义主题动作。

| 检查 | 本轮实际结果 |
| --- | --- |
| 新执行应用 JVM | 715 项通过。 |
| 新执行宿主 | 227 项通过：215 项基线＋12 项 T11，包含真实默认 schema 部署；最终文档提交的同组复核属于重复核验，不叠加测试覆盖。 |
| 缓存结果 | 17 项 build-logic＋6 项 compatibility 为 UP-TO-DATE／CACHE 结果，未计入 715 fresh。 |
| 原生与独立复测 | host13 编译／运行实际退出 0，384 项检查通过；Root 在新目录运行同一冻结二进制再通过 384 项，独立证据不算成 768 项。原生编译峰值 464756 KiB。 |
| Build3／Lint／签名 | Debug／Release Lint 均 0 issues，构建警告 0，双 ABI Release 签名警告 0；minSdk21、20261119／3.3.13-t9.14、DEBUG=false，既有证书保持。 |
| 原生日志 | `native-build-logs4` 区分当前 ARM64 增量执行与 x86_64 Ninja no-work、AGP JSON cache-hit。原始编译日志复用只限严格匹配的源码、配置、子模块、模型、对象及当前 APK `.so` 字节；文本和结构化 warning 均 0，不把 no-work 或 task EXECUTED 算成新编译／configure 进程。 |
| 当前静态及链接 | Static2 双 ABI API21 ELF DT_NEEDED／强引用闭包通过，weak／unresolved 0；实际 JNI／DEX 接口、模型及签名核对通过。Build3 独立测试 APK 的 138 项必要接口直接链接扫描通过。采集器原始 `passed:false` 和 API／设备语义未测字段保留，由独立审阅结合设备运行判定，不改写为采集器已证明全部运行语义。 |

## 最终模拟器验收

两台均使用当前冻结 Release／Debug x86_64 APK，分别完成八阶段；Release 实际界面在两个主题上各三项检查，另做独立 Debug 回归。原驱动和拥有的模拟器均正常停止并等待退出 0；未把部分分项成功当作完整终态。

| 范围 | Android 5.0／API21 | Android 15／API35 |
| --- | --- | --- |
| 最终功能目录 | `build/t11/api21-build3-runtime2-clean-sdcard` | `build/t11/api35-build3-runtime1` |
| 最终正式版 GUI | `api21-build3-release-gui3-clean-sdcard` | `api35-build3-release-gui1` |
| 升级阶段 | 旧 9.13 Release 准备 2 项、9.14 覆盖升级核对 3 项；虚构学习词、UID／数据前缀与三次 portable 备份核对通过。 | 同范围通过。 |
| T11 实际运行 | Release 30 项＋Debug 30 项通过，包含真实 JNI／九键、内部 F4 切方案、隐私／编辑器边界、失效模型和普通置顶排序回归。 | 同范围通过。 |
| 其它 Debug 阶段 | T10 14 项、隐私 16 项、T08 8 项及完整编辑器回归通过；快速输入 40 轮、显示隐藏 100 次。 | 同范围通过。 |
| Release 两主题实点 | 6 项通过，44 次实际点击、50 次捕获；原始截图／XML／节点和新读编辑器结果绑定，Root 实际审阅 8 张截图。 | 同范围通过。 |
| GUI 备份恢复 | GUI 验收前备份范围内配置和文件的原 presence／bytes／元数据核对，9 个文件及输入法选择恢复。 | 同范围核对，11 个文件及输入法选择恢复；实际 CE／DE 形状分别检查，不强套 API21 数量。 |
| 原始功能 manifest | 366 个原文件逐 SHA／长度核验。 | 308 个原文件逐 SHA／长度核验。 |
| 本轮完整原文 | 3,903 条 W/E/F，完整 logcat 4,144,954 字节；控制台和全部附件保留。 | 8,198 条 W/E/F，完整 logcat 8,627,447 字节；控制台和全部附件保留。 |
| 终态 | 八阶段通过、GUI 恢复和 handoff／completion 核验、owned stop 退出 0。 | 同范围通过。 |

本轮 IME fatal／ANR、实际 LMK 和内核严重异常计数为 0。平台／测试环境 W/E/F 没有全部消失，原始行、上下文及接受限界均保留；功能验收通过不能概括为所有进程、服务和硬件都无风险。

## 历史失败与环境更换

早期原生模块注册、编译／运行失败，启用联想时 FIFO 模型阻塞，以及原生产 JNI 在 100 次清理后候选菜单嵌套／恢复排序失败的真实复现均保留。最小修复后 host13 及独立 replay 通过。Build1 为必要 JNI 修复受控停止，原退出 1 和内存守护记录保留，未发生 OOM。详见 [T11 完整历史](VALIDATION-T11.md)。

Build2 API21 Release 独立测试 APK 因宿主 R8 裁剪 `Intrinsics.checkNotNullParameter` 发生 `NoSuchMethodError`，原失败、全 PID 日志和部分升级结果保留。修复精确保留 62 个类型身份和 60 个成员，未新增通配规则，原 `assumenosideeffects` 不变；测试八处调用显式传原默认值，避免默认参数桥依赖。Build3 新 APK 链接扫描及两 API 的实际运行通过，旧失败不改判。

`native-build-logs2` 收集器误判精确的 Ninja glob 维护行，`native-build-logs3` 误把 AGP configureTask EXECUTED 等同实际 CMake 进程，两份 false 回执保留。新 V7 在 `native-build-logs4` 精确识别实际 no-work／三条 JSON cache-hit，仍绑定冻结历史原文并拒绝额外工作、异常和诊断，实际收集通过。

首次 API21 GUI 完成 6 项检查后，V2 的 `after-t11-gui` 收尾角色被冻结驱动正则拒绝，原整轮失败报告保留。V3 仅改为 `after-prediction-gui`，随后在新目录完成真实检查、GUI 验收前备份恢复和停止；没有修改 APK 或 GUI 判定条件。恢复范围不包含驱动整轮开始前所有全局状态：本轮专用 AVD 曾主动清理测试偏好，API21 测试 SD 也经明确的环境重建。

旧 `api21-build3-runtime1` 八阶段功能通过，但启动 FAT 检查发现 7 条丢失簇链、共 72 个簇，并有 LOST.DIR 空间不足及 FSInfo 异常；该环境已退休，不进入最终稳定验收。原报告、草稿、SD base 和 qcow2 保留。新 SD 由 SDK `mksdcard` 生成，最终 runtime2／GUI3 完成完整验收；启动 FSInfo 签名和 freecount 各随后显示 `FIXED`，继而 `Filesystem check completed OK`，完整本轮原文无 lost chain／LOST.DIR。此处是测试介质更换，未重建用户数据，也不是产品修复；不删除新介质修复诊断来宣称平台警告为零。

## 门禁与范围

GateV3 对已确认的原生 guest inline portable 备份核验精确 13 字段、`independent_serial_JNI_worker_not_Daemon_lifecycle`、两 API 限定 user／sync 目录和 UUID／`__t10_upgrade_32hex.userdb.txt` 路径、有界严格 base64 与 UTF-8／SHA／长度、导出头和词条、三次相同备份／返回 true／临时文件 0，并核对 full／T9 查词保持。普通宿主 path／bytes／SHA 仍逐 regular 文件核验；未知、损坏或额外字段的 guest 形状拒绝。它不是通用 guest 路径豁免，也不是继承 T10 发布批准；旧 GateV1／V2 和原始报告字段冻结不改。

API35 Debug 编辑器回归中的一次性 WebView Renderer（PID12371）出现 code -1，之后有 isolated-not-needed 和 Zygote 正常退出上下文，测试源码有主动 `destroy`。这些只支持时序关联，退出由销毁造成仍是推断；不能声称所有平台崩溃都为 0。Binder 引用／事务、模拟器硬件服务、文件系统／内核和宿主控制台诊断保留可见限界；逐项分类接受不证明所有风险已消除。API35 内核 EROFS 探测随后成功 ext4 挂载及恢复日志也不能推导为所有介质无损。

真机候选 P95、Java／native／graphics／总 PSS、长期驻留、首次部署、物理 ARM64／全部 OEM／16KiB 页、真实 TTS、SAF 选择器权限／取消及全部第三方方案／插件未验证。模拟器编辑器响应按既有 2000ms 条件检查，不是候选 P95≤100ms 的证明；性能采集关闭，不能把正式启动与 Debug 全量维护检查混为一谈。候选展开 PAGE48／prefetch16／maxSize144 是 best-effort 预算，不是普遍硬驻留上限。

## 冻结附件与在线发布

手动附件仅 ARM64 APK、x86_64 APK 和一个完整 `source-with-submodules.tar.gz`；源码包包含主仓库与 21 个递归子模块共 22 个 Git 仓库，以及模型、schema、原生覆盖源和 LICENSE。GitHub 自动 source zip／tar 保留，不上传 Debug APK、验收报告、日志、截图或额外校验清单。

发布说明以七个文档的最终提交字节冻结；精确远端 `x2m7` develop／tag、源码归档大小／SHA、draft 与公开附件下载复核、Latest 和旧附件清理结果由归档外发布回执证明。本文不嵌入包含它自身的源码归档 SHA，也不因包装后的回执反复改写。线上发布状态以[版本 Release 页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.14)为准。
