# T14：更新安装和发布一致性

状态（2026-10-15）：实现已完成，最终双平台升级、推送与稳定版发布尚未完成。当前公开稳定版仍为 `v3.3.13-t9.16`；`3.3.13-t9.17` 是待验收候选。不能用下列旧候选构建或后续局部诊断替代最终升级通过记录。

## 安装身份与版本

已读取 `X2M7/trime` 当前 15 个公开 Release 的全部 26 个 APK，实际包名均为 `com.osfans.trime.debug`，单一签名证书 SHA-256 为 `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`，历史最高 versionCode 为 `20261121`。[原始审计](../../build/t14/published-identity-audit1/identity-results.json)逐 APK 保存实际结果；不是根据文件名推断身份。

本版 `release.properties` 固定 `3.3.13-t9.17 / 20261122`，统一 Gradle、展示版本、APK 名称、发布工具和 Pages。正式包沿用历史包名与证书，实际 `debuggable=false`；Debug 变体增加 `-debug`。证书 DN 是 Android Debug，本轮没有声称更换签名。这样保留同包名原证书的覆盖更新路径；不同包名需按[导出／导入说明](../release/README.md)迁移，不能通过卸载保留词库。

目标仓库限定 `X2M7/trime:develop`，本地推送 remote 为 `x2m7`。`origin` 指向上游，不能用于发布推送。正式包提供 ARM64 和 x86_64 两种 ABI；本轮没有新发布 32 位兼容包。

## 已完成的构建与静态核验

以下结果对应源码 `66baa779fb796722500afc40b7620fec3c62afb0`。之后升级测试脚本仍有修改，最终源码必须重新冻结构建。

| 阶段 | 实际结果 | 最低主机可用内存 |
| --- | --- | --- |
| [Debug4](../../build/t14/debug4/result.json) | 实际退出 0，812 项应用 JVM 测试新执行通过 | 3,193,624 KiB |
| [Release4](../../build/t14/release4/result.json) | 实际退出 0，两个正式 ABI APK 完成 | 3,192,316 KiB |
| [Lint3](../../build/t14/lint3/result.json) | 实际退出 0，Debug／Release 分析新执行且均为 0 问题 | 3,322,756 KiB |
| [Host3](../../build/t14/host3/result.json) | 262 项宿主检查新执行通过 | 单独限额运行 |

三个 Gradle 阶段共享实际候选冻结时间，使用 896 MiB 堆、640 MiB Metaspace、单 worker。构建和签名检查没有警告；源码前后不变，保护未触发。28 项 build-logic 和 6 项 compatibility 测试为缓存结果，未计入新执行的 812 项。历史 384 项原生预测测试没有在本轮重跑。

[APK 实物记录](../../build/t14/artifacts3/build-record.json)核对 Manifest、单一 ABI／ELF、v1/v2 签名、证书和 ZIP 对齐，固定主仓库及 21 个递归子模块提交、9 份词典来源摘要与两 APK 相同的 53 项数据资源。[独立完成复核](../../build/t14/final-artifacts-review3/completion-review.json)核对 162 项证据绑定。静态 ABI／对齐检查不等于物理 ARM64 或 16 KiB 页设备验收。

## 升级测试范围

使用实际已发布的 9.16 x86_64 Release 与新 Release，普通安装、同包名同证书、严格递增 versionCode。没有卸载、清除数据或降级参数。专用空白模拟器档案内，旧 APK 的实际 JNI 学习两个虚构短语“墨羽燈”和“茶雲翎”，各三次；新 APK 验证停止后的用户词库字节、学习频次、全拼／九键读取及三次备份。

宿主准备五项非默认偏好（字号、高度、注释、横向滚动、单手模式）及两个配置文件。升级前后保持 UID、数据目录、选中／启用输入法和 subtype；所有非 PID 偏好严格比较，配置按字节比较。只有崩溃日志进程身份 `general__pid` 可变化，不能豁免主题或版本键。

服务激活使用 root ADB 启动私有的空 `ClipEditActivity`，不提供剪贴板记录参数、不输入内容、不保存；应用进程仍会执行正常初始化。这是内部编辑框激活检查，不是公开 UI、剪贴板交互或完整设备功能矩阵验收。

## 原始失败与诊断

- 早期 APK 安装的 API21 CRLF 成功行识别和 shell `printf` 兼容问题已修正；原失败记录保留。
- [API21 runtime3 升级](../../build/t13/t14-api21-upgrade2/upgrade-report.json)：普通覆盖安装成功，但旧版主题异步初始化补入 `selected_theme=trime`，偏好基线不稳定，整轮失败。随后补充真实服务绑定、主题持久化和两次稳定偏好观察，未放宽偏好断言。
- [API21 runtime4 升级](../../build/t13/t14-api21-upgrade3/upgrade-report.json)：旧版基线稳定，普通覆盖安装成功；测试错误要求在无新输入请求的桌面立即绑定升级后的服务，导致超时。后续[独立诊断](../../build/t13/t14-api21-upgrade3-continuation-diagnostic1/report.json)确认非 PID 偏好／配置、两个学习词条与各自频次、全拼／九键、三次备份及最终服务绑定通过。诊断不改写原失败，不作为完整升级通过。
- [API35 runtime1](../../build/t13/t14-api35-runtime1/result.json)：只完成旧版原生准备，未安装新版。系统启动阶段 SystemUI 和电话进程发生 ANR，SystemUI 无响应弹窗占住输入焦点；后续内部编辑框虽然启动，但没有获得焦点。实际原日志保留，不能据此认定 Trime 激活故障。随后主机可用内存降到 979,088 KiB，1 GiB 保护正常关闭模拟器；VM 退出 0、控制器退出 1，清理无错误。

本次修正移除无输入请求时的立即绑定假设，在旧版／最终激活前启动实际空编辑框并要求其精确窗口焦点。测试前和激活失败时保存完整窗口、Activity、进程与电源快照，拒绝当前 ANR／崩溃状态；19 项相关宿主测试通过。所有九项业务检查、60 秒激活时限与数据保留条件不变；尚需实际设备重跑。

## 发布门槛与待完成项

同一最终源码的 Debug／Release／双 Lint／宿主结果与 Android 5.0、15 升级结果通过后，才能形成资格文件。发布工具只接受两个 Release APK 和完整递归源码包；源码从固定 Git blob 打包，包含逐文件清单、主仓库及递归子模块版本。GitHub 自动 Source code zip/tar 不包含子模块工作树，发布说明须给出递归克隆方式。

先提交发布文档、生成 Pages，再正常推送 `x2m7` 的 develop 和新注释标签。发布工具先建草稿并下载核对附件，再设为稳定版 Latest 并再次核对公开下载和旧 Release 不变。随后核对 Pages 部署提交与实际 HTML／release.json。当前尚未执行这些推送或发布操作。

## 限定范围

不宣称全部系统运行警告消除。旧原始警告与失败日志继续保留；独立 eSpeak 辅助程序的一类 ART finalizer 诊断尚未闭合，该程序不包含在正式 APK 中。真机 TalkBack／TTS、物理 ARM64／OEM／16 KiB 页、P95／PSS 和长期驻留仍待验收。T14 不替代此前完整 T12／T13 功能矩阵，也不宣称覆盖所有跨包名迁移组合或 SAF 授权迁移。

持续遵守单构建或单模拟器、启动至少 4 GiB 可用内存及运行中 1 GiB 下限。文件索引、编辑器与浏览器保持运行；内存未达到门槛时不启动重任务。
