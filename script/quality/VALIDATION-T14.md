# T14：更新安装和发布一致性

状态（2026-10-15）：T14 实现及本轮限定范围的 Android 5.0／15 覆盖升级验收完成。APK 构建提交为 `b71ae9e5e7d2e342895ab5374d533b5141410249`，目标版本 `v3.3.13-t9.17`。本报告在发布前冻结；实际推送、附件下载校验与 Latest 状态由独立发布回执及[版本 Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.17)记录。下列历史失败不改写为通过。

## 安装身份与版本

发布前已读取 `X2M7/trime` 的 15 个公开 Release、全部 26 个历史 APK，实际包名均为 `com.osfans.trime.debug`，单一签名证书 SHA-256 为 `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`，历史最高 versionCode 为 `20261121`。[原始审计](../../build/t14/published-identity-audit1/identity-results.json)逐 APK 保存实际结果；不是根据文件名推断身份。

本版 `release.properties` 固定 `3.3.13-t9.17 / 20261122`，统一 Gradle、展示版本、APK 名称、发布工具和 Pages。正式包沿用历史包名与证书，实际 `debuggable=false`；Debug 变体增加 `-debug`。证书 DN 是 Android Debug，本轮没有声称更换签名。这样保留同包名原证书的覆盖更新路径；不同包名需按[导出／导入说明](../release/README.md)迁移，不能通过卸载保留词库。

目标仓库限定 `X2M7/trime:develop`，本地推送 remote 为 `x2m7`。`origin` 指向上游，不能用于发布推送。正式包提供 ARM64 和 x86_64 两种 ABI；本轮没有新发布 32 位兼容包。

## 本轮构建与 APK 实物检查

三个阶段统一实际冻结时间 `1792074259473`，构建源码均为 `b71ae9e5`，源码前后不变。每阶段实际执行、退出和内存原件分别保存：

| 阶段 | 实际结果 | 最低主机可用内存 |
| --- | --- | --- |
| [Debug5](../../build/t14/debug5/result.json) | 退出 0，812 项应用 JVM 测试新执行通过 | 3,403,956 KiB |
| [Release5](../../build/t14/release5/result.json) | 退出 0，两个正式 ABI APK 完成 | 3,212,244 KiB |
| [Lint4](../../build/t14/lint4/result.json) | 退出 0，Debug／Release 分析新执行，均为 0 问题 | 3,431,016 KiB |
| [Host4](../../build/t14/host4/result.json) | 268 项宿主检查新执行通过，包括 19 项升级门槛测试 | 单独限额运行 |

使用 896 MiB 堆、640 MiB Metaspace、单 worker，保护未触发。28 项 build-logic 和 6 项 compatibility 测试为缓存结果，未计入新执行的 812 项；历史 384 项原生预测测试本轮未重跑。生产 JNI 工作复用未变产物，不能声称全部重新编译。构建日志及两正式 APK 的签名检查没有警告。

[本轮 APK 实物记录](../../build/t14/artifacts4/build-record.json)确认包名、`20261122`、`debuggable=false`、单一 ABI／ELF、v1/v2 签名、证书和 ZIP 对齐；固定主仓库及 21 个递归子模块提交、9 份词典来源摘要和两个 APK 完全一致的 53 项数据资源。[独立完成复核](../../build/t14/final-artifacts-review4/completion-review.json)检查 181 项绑定、22 个仓库和实际 APK ZIP／DEX／ELF。Luna 打包头部按既定逻辑变换，来源与打包摘要分别保存，正文一致；不是把变换后的整个文件误称源文件字节相同。

| Release APK | 字节数 | SHA-256 |
| --- | ---: | --- |
| ARM64 | 11,966,335 | `ff20dcd5b4b939843a1971dbc6487bb2c9a727680b27c30e43774b837e6c7b57` |
| x86_64 | 12,047,543 | `21a146350a4f9e042e1979c5fb223020dbc311e365beadea648d29fb2e372df6` |

## 本轮双平台升级

两平台使用同一已发布 9.16 x86_64 Release、同一新 Release 和同一冻结升级脚本。各九项检查全部通过，实际运行工具退出均为 0。

| 环境 | 实际结果与原件 | 最低主机可用内存 |
| --- | --- | --- |
| Android 5.0 / API21 | [upgrade4](../../build/t13/t14-api21-upgrade4/upgrade-report.json)、[runtime5](../../build/t13/t14-api21-runtime5/result.json)，1 核／1536 MiB／swangle | 控制器 3,603,180 KiB；升级工具 3,594,416 KiB |
| Android 15 / API35 | [upgrade2](../../build/t13/t14-api35-upgrade2/upgrade-report.json)、[runtime7](../../build/t13/t14-api35-runtime7/result.json)，4 核／2304 MiB／Intel 主机 GPU | 控制器 2,102,524 KiB；升级工具 2,102,408 KiB |

两个模拟器及控制器正常退出 0，无剩余模拟器、保护停止或清理错误。Android 5.0 logcat 退出 0；Android 15 logcat 在正常关闭设备后退出 255，该原始退出保留，不能写成全部子进程退出 0。构建 JVM 已退出，文件索引、编辑器和浏览器未由本任务关闭。

[Android 5.0 独立复核](../../build/t14/api21-final-review1/review.json)重算 383 项文件绑定与 140 个命令原件，确认八项非进程偏好、两份配置、17 个停用后的原生文件、两个词条各三次学习、全拼／九键、三份相同备份以及旧版／新版精确编辑框焦点和绑定服务。[Android 15 独立复核](../../build/t14/api35-final-review1/review.json)确认同一九项检查，重算 548 项本地绑定与 140 个命令原件、八项非进程偏好、两份配置和 18 个原生文件；两个词条各三次学习、全拼／九键、三份相同备份、旧版／新版编辑框焦点和服务绑定均有实际原件。

两平台保留完整原始日志。Android 5.0 的 7,322 行日志包含 1,601 条 W、700 条 E；Android 15 的 32,387 行包含 3,118 条 W、1,800 条 E。两轮捕获范围内未匹配到 ANR、致命异常、实际 LMK／OOM、JNI 加载失败或内核 critical 事件；这不是全部系统健康或零警告声明。

Android 5.0 保留首次激活 166／299／35 帧掉帧、最终页面显示 5.181 秒、旧进程 Binder 诊断，以及 ART 旧 API 类校验、WorkManager 可选调度器回退等观察。Android 15 的旧版／新版 Trime 服务进程（PID 2711／3065）各有一次 ART finalizer 诊断，具体 Reference 对象尚未定位；不把它直接等同于泄漏，也不排除为已证实系统噪声。另保留帧预测、渲染／监控权限、ART CPU／JDWP 和测试元数据诊断；部分输入法 show 请求失败或取消，随后独立请求实际达到 onShown，最终服务／编辑框状态另有严格断言。

Android 15 安装前的数据库损坏日志属于 `android.process.acore` PID 1552，具体数据库未确定，不能仅根据邻近日志称为 Contacts 数据库。系统安装会话 rename、审计丢失与限流、PSI 等限制继续保留。逐条分类及进程来源见 [API21 日志复核](../../build/t14/api21-final-review1/log-triage.json)与 [API35 日志复核](../../build/t14/api35-final-review1/log-triage.json)。本轮真实业务通过不替代公开 UI 性能、完整引擎功能矩阵或所有警告消除。

## 历史候选构建

以下结果仅对应旧源码 `66baa779fb796722500afc40b7620fec3c62afb0`，保留历史原件，不替代上方最终构建和双平台升级结果。

| 阶段 | 实际结果 | 最低主机可用内存 |
| --- | --- | --- |
| [Debug4](../../build/t14/debug4/result.json) | 实际退出 0，812 项应用 JVM 测试新执行通过 | 3,193,624 KiB |
| [Release4](../../build/t14/release4/result.json) | 实际退出 0，两个正式 ABI APK 完成 | 3,192,316 KiB |
| [Lint3](../../build/t14/lint3/result.json) | 实际退出 0，Debug／Release 分析新执行且均为 0 问题 | 3,322,756 KiB |
| [Host3](../../build/t14/host3/result.json) | 262 项宿主检查新执行通过 | 单独限额运行 |

三个 Gradle 阶段共享实际候选冻结时间，使用 896 MiB 堆、640 MiB Metaspace、单 worker。构建和签名检查没有警告；源码前后不变，保护未触发。28 项 build-logic 和 6 项 compatibility 测试为缓存结果，未计入新执行的 812 项。历史 384 项原生预测测试没有在本轮重跑。

[APK 实物记录](../../build/t14/artifacts3/build-record.json)核对 Manifest、单一 ABI／ELF、v1/v2 签名、证书和 ZIP 对齐，固定主仓库及 21 个递归子模块提交、9 份词典来源摘要与两 APK 相同的 53 项数据资源。[独立完成复核](../../build/t14/final-artifacts-review3/completion-review.json)核对 162 项证据绑定。静态 ABI／对齐检查不等于物理 ARM64 或 16 KiB 页设备验收。

## 升级测试范围

使用实际已发布的 9.16 x86_64 Release 与新 Release，普通安装、同包名同证书、严格递增 versionCode。没有卸载、清除数据或降级参数。专用、初始未安装 Trime 的模拟器档案内，旧 APK 的实际 JNI 学习两个虚构短语“墨羽燈”和“茶雲翎”，各三次；新 APK 验证停止后的用户词库字节、学习频次、全拼／九键读取及三次备份。

宿主准备五项非默认偏好（字号、高度、注释、横向滚动、单手模式）及两个配置文件。升级前后保持 UID、数据目录、选中／启用输入法和 subtype；所有非 PID 偏好严格比较，配置按字节比较。只有崩溃日志进程身份 `general__pid` 可变化，不能豁免主题或版本键。

服务激活使用 root ADB 启动私有的空 `ClipEditActivity`，不提供剪贴板记录参数、不输入内容、不保存；应用进程仍会执行正常初始化。这是内部编辑框激活检查，不是公开 UI、剪贴板交互或完整设备功能矩阵验收。

## 原始失败与诊断

- 早期 APK 安装的 API21 CRLF 成功行识别和 shell `printf` 兼容问题已修正；原失败记录保留。
- [API21 runtime3 升级](../../build/t13/t14-api21-upgrade2/upgrade-report.json)：普通覆盖安装成功，但旧版主题异步初始化补入 `selected_theme=trime`，偏好基线不稳定，整轮失败。随后补充真实服务绑定、主题持久化和两次稳定偏好观察，未放宽偏好断言。
- [API21 runtime4 升级](../../build/t13/t14-api21-upgrade3/upgrade-report.json)：旧版基线稳定，普通覆盖安装成功；测试错误要求在无新输入请求的桌面立即绑定升级后的服务，导致超时。后续[独立诊断](../../build/t13/t14-api21-upgrade3-continuation-diagnostic1/report.json)确认非 PID 偏好／配置、两个学习词条与各自频次、全拼／九键、三次备份及最终服务绑定通过。诊断不改写原失败，不作为完整升级通过。
- [API35 runtime1](../../build/t13/t14-api35-runtime1/result.json)：只完成旧版原生准备，未安装新版。系统启动阶段 SystemUI 和电话进程发生 ANR，SystemUI 无响应弹窗占住输入焦点；后续内部编辑框虽然启动，但没有获得焦点。实际原日志保留，不能据此认定 Trime 激活故障。随后主机可用内存降到 979,088 KiB，1 GiB 保护正常关闭模拟器；VM 退出 0、控制器退出 1，清理无错误。

本次修正移除无输入请求时的立即绑定假设，在旧版／最终激活前启动实际空编辑框并要求其精确窗口焦点。测试前和激活失败时保存完整窗口、Activity、进程与电源快照，拒绝当前 ANR／崩溃状态；19 项相关宿主测试通过。所有九项业务检查、60 秒激活时限与数据保留条件不变；已在上方最终双平台轮次执行。

Android 15 本轮系统准备还保留下列失败：`runtime2` 在安装任何 Trime APK 前超过 180 秒首次启动时限，正常关闭；`runtime3` 因复用目录含生成硬件文件而在 VM 启动前拒绝；`runtime4／5／6` 在安装 Trime 前出现 SystemUI 或电话进程的启动 ANR，健康检查均拒绝。各轮磁盘只在正常停止后复制到新独立目录，六项磁盘逐字节校验、独立 inode、相对 backing 和原文件状态均留存；未复制快照／RAM／生成硬件配置，也未清除原失败日志。

后续仅调整模拟器：API35 虚拟 CPU 从 1 到 2、4，再将软件 GPU 改为实际可用的 Intel 主机 GPU。API21 参数不变，API35 客体内存始终为 2304 MiB；180 秒系统启动、120 秒只读健康检查、60 秒服务激活、九项升级断言、4 GiB 启动门槛与 1 GiB 运行下限均不变。原日志支持 CPU 压力较高这一观察，不能证明唯一根因。最终 runtime7 通过同一个只读启动健康检查后才首次安装旧版 Trime，仍由升级脚本确认不存在既有测试包和专用数据。

## 发布冻结与一致性检查

本轮资格文件绑定同一最终源码的 Debug／Release／双 Lint／宿主结果、Android 5.0／15 升级结果及原始历史 APK 审计。发布工具只接受两个 Release APK 和完整递归源码包；源码从固定 Git blob 打包，包含逐文件清单、主仓库及递归子模块版本。GitHub 自动 Source code zip/tar 不包含子模块工作树，发布说明须给出递归克隆方式。

先提交发布文档、生成 Pages，再正常推送 `x2m7` 的 develop 和新注释标签。发布工具先建草稿并下载核对附件，再设为稳定版 Latest 并再次核对公开下载和旧 Release 不变。随后核对 Pages 部署提交与实际 HTML／release.json。本报告在这些外部操作前冻结，实际发布回执保存在源码归档外，避免自引用；后续以回执、Release 实际附件和 Pages 部署结果核对公开状态。

## 限定范围

不宣称全部系统运行警告消除。旧原始警告与失败日志继续保留；独立 eSpeak 辅助程序的 ART finalizer 诊断尚未闭合，该程序不包含在正式 APK 中；本轮 Android 15 也在旧版和新版 Trime 服务进程各观察到一次相同类型诊断，具体 Reference 对象仍未定位，不能仅归因于辅助程序或作为已证实系统噪声排除。真机 TalkBack／TTS、物理 ARM64／OEM／16 KiB 页、P95／PSS 和长期驻留仍待验收。T14 不替代此前完整 T12／T13 功能矩阵，也不宣称覆盖所有跨包名迁移组合或 SAF 授权迁移。

持续遵守单构建或单模拟器、启动至少 4 GiB 可用内存及运行中 1 GiB 下限；虚拟 CPU 数量调整没有增加 API35 客体内存。文件索引、编辑器与浏览器保持运行；内存未达到门槛时不启动重任务。
