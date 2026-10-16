# v3.3.13-t9.18 上游整合验证

状态（2026-10-16）：**本轮限定范围验收完成；本文冻结于实际推送和发布之前**。Debug／Release、双 Lint、APK 静态检查及 Android 5.0／15 的升级、编辑器与导入路径均通过。运行日志保留全部原始警告和未定位原因；公开附件、Latest 与 Pages 的后续核对由实际发布回执记录。旧版报告不作为本 APK 的新执行通过凭证。

## 源码与整合范围

构建候选 CODE：`d6d586fe8f9dcea28d10bd3e08213d1fc769cab5`；候选冻结时间戳 `1792110774886` 毫秒，Debug／Release／独立 Lint 共用该实际候选时间，阶段起止时间分别记录。之后仅文档变更的 PUB 以新 tag `v3.3.13-t9.18` 的实际目标和最终发布回执为准；本报告不自引用尚未产生的文档提交 SHA。

合并提交 `61df629e6cfbf911500e167e9c0ed85460560e96` 含上游 develop 截至 `c7f32b6cca4d86ec43d6782e7c70f6700e8baa07` 的全部六项待合入提交；本次固定上游范围内遗漏数为 0。这不表示上游以后新增提交也已合入。

| 上游提交 | 内容 | 本项目整合方式 |
| --- | --- | --- |
| `e4934abf` | Gradle 升级至 9.8.1 | 该版本及官方 SHA 校验值在本 fork 合并前已经具备，保留现有固定值。 |
| `ad5a14be` | 带修饰键的 Return 以组合键转发 | 无修饰 Return 保留编辑器动作；修饰键场景发送 modifier／Enter 的按下和释放。增加实际编辑器回归。 |
| `1e4b9939` | `UserDataManager` 外部目录同步 | 整合管理入口，保留本项目已校验的事务导入、失败回滚、取消边界、路径防护和 Direct Boot 隔离。 |
| `78613789` | 默认自定义配置初始化移至 `UserDataManager` | 仅缺失时创建，包含全拼／简体全拼／九键，已有用户文件按字节保留。 |
| `5edc1d65` | `DataManager` 移至 `core/data` | 更新生产、测试、反射入口与混淆保留规则；旧包升级测试反射兼容旧命名空间。 |
| `c7f32b6c` | 重用内容摘要并加宽摘要缓冲 | 流式 SHA-256 使用 32 KiB 缓冲，复用已读取的远端／暂存内容摘要；跨 SAF 挂起后重新读取目标内容摘要。 |

事务流程有意保留本项目的既有行为，未整段替换为上游实现。`rime_sync_index.json` 的文件名及 `treeUri`／`entries`／`size`／`lastModified`／可选 `localSha256` 数据格式保持兼容，保留原同步历史。保持便携词典导出与 `skipImport` 接口；打开的二进制用户词库、编译缓存和本机安装身份不直接当成普通共享配置同步。

后续修复 `6582dc3e` 在 SAF 挂起读取完成后重算当前目标文件的摘要，避免其他写入者在等待期间修改本地文件后仍使用旧摘要。格式整理提交 `36701a6d` 与内存构建配置提交 `d6d586fe` 均已包含在当前 CODE；不据源码审阅代替实际设备通过结果。

## 已执行的身份与宿主检查

历史 SDK 执行实际复核当前 16 个公开 Release 的 28 个 APK，包括公开 9.17 的 ARM64 和 x86_64 下载原件；签名、native 条目与包名／版本原件均保留。原 audit1 的最低 SDK 解析只识别 `sdkVersion`，漏掉实际输出的 `minSdkVersion`，因此错误记为 null，并导致第一轮升级输入准备失败；原 audit1 及 `upgrade-artifacts1/preparation-result.json` 失败原件保留。

`published-identity-audit2/identity-results.json` 已对上述 28 份实际绑定的 SDK 输出进行严格派生重解析，更正 28 项 minSdk 为 21；SDK 两种最低版本别名合计必须恰好一条完整数字，targetSdk 同样单值。派生过程实际退出 0，新 SDK 命令执行数为 **0**，不冒充重新验签；APK 字节、公开 digest、原 SDK 命令／时间戳／输出 bindings 和其余全部字段均保持并复核。相同包名历史最高 `versionCode=20261122`，新元数据为 `20261123`。

长期安装身份保持 `com.osfans.trime.debug`；签名证书 SHA-256 为 `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`。包名与证书 DN 含历史 Debug 字样；新 APK 的实际检查结果为 Release、`debuggable=false`、minSdk 21、版本 3.3.13-t9.18／20261123，各 APK 只包含对应 native ABI 且证书一致。`artifacts1/build-record.json` 的实际 `inspection_passed=true`，静态记录阶段实际退出 0，签名检查警告均为 0。这不代替物理设备验收。

修正历史 SDK 字段后，`upgrade-artifacts2/preparation-result.json` 实际通过，冻结旧公开包／新正式包／本候选测试包及其已检查身份，测试包的 AAPT／Manifest／签名检查实际执行通过。它证明升级输入准备完成；Android 5.0 与 Android 15 的九项实际覆盖升级检查随后均已通过；Android 15 的目录授权、严格导入和正常停机随后也已完成，全日志复核结果见下文。第一次失败未被改写为通过。

宿主 `build/upstream18/host2/result.json` 实际退出 0，**268 项新执行通过**，绑定当前 CODE。Debug 的 820 项应用 JVM 测试新执行通过，86 份 XML 无失败、错误和跳过，XML 时间戳均在本次 Debug 实际运行区间内；`:app:testDebugUnitTest` 与 Spotless 任务在实际日志中执行。构建逻辑 28 项 convention 和 6 项 compatibility 为 UP-TO-DATE 缓存结果，共 34 项单列，不计为本轮新执行。历史 384 项原生检查未重新运行。Debug／Release、ARM64／x86_64 四项 CMake 构建任务本轮均有调用，但保留日志无 Building／Linking 行，不能声称四组生产原生库已重新编译。

## 构建与内存

当前串行构建使用显式 `--compact-memory`：768 MiB JVM 堆、640 MiB Metaspace、单 worker／单活动处理器；启动可用内存至少 3.75 GiB，运行下限仍为 1 GiB。默认构建配置仍为 896 MiB 堆、4 GiB 启动。每阶段有原有 90 分钟上限，保留实际内存样本、命令、日志和退出／清理记录。

模拟器仍要求至少 4 GiB 可用内存才启动，运行下限 1 GiB；构建与模拟器串行，同一时间仅一个模拟器。文件索引、编辑器与浏览器保持运行。辅助 ktlint 独立格式化使用缓存 JAR 和较小 JVM，不能替代正式构建中的 Spotless 检查。

| 阶段 | 实际结果 | 警告／最低可用内存／退出清理 |
| --- | --- | --- |
| Debug、Spotless、应用与构建逻辑测试 | 实际退出 0；820 项应用 JVM 新执行通过，86 份 XML；Spotless 实际通过；34 项构建逻辑结果缓存 | 日志警告 0；最低 2,075,160 KiB（约 1.98 GiB）；源码未变、保护未触发、无清理信号或残留子进程 |
| ARM64／x86_64 Release | 实际退出 0；两份 APK 已冻结，实际身份／签名／单一 ABI 检查通过 | 日志警告 0；最低 2,177,164 KiB（约 2.08 GiB）；源码未变、保护未触发、无清理信号或残留子进程 |
| 独立 Debug／Release Lint | 实际退出 0；两个 Analyze／Report 阶段实际执行，两份 XML 均 0 问题 | 日志警告 0；最低 2,533,236 KiB（约 2.42 GiB）；源码未变、保护未触发、无清理信号或残留子进程 |
| 正式 APK 身份／签名／ABI／资源记录 | `artifacts1/build-record.json` 实际通过；两份 Release、minSdk21、debuggable=false、对应单一 native ABI 和原证书 | 签名检查警告 0；仅静态检查，不代表设备通过 |

## 双平台受影响路径验收

使用专用、初始未安装 Trime 的模拟器档案，先安装已核验的公开 9.17 x86_64 正式 APK，通过真实 JNI 学习虚构词条，再普通覆盖安装本候选正式 APK。禁止卸载、清除数据或降级参数。词库、备份、配置、偏好、UID、已选／已启用输入法与绑定到内部空编辑器的状态须分别核对。

随后覆盖安装同候选 Debug 包，运行受影响的编辑器和数据路径检查。Debug 检查不冒充正式包公开界面验收。

| 平台／检查 | 必须验证的内容 | 实际结果 |
| --- | --- | --- |
| Android 5.0 Release 升级 | 九项原生升级／数据／输入法保留断言 | `upgrade1/upgrade-report.json` 实际通过，9／9；UID、目录、输入法和虚构学习结果保留，三次便携备份调用及逻辑条目核对通过；未卸载或清数据，最低可用 2,526,464 KiB |
| Android 15 Release 升级 | 同上 | `upgrade1/upgrade-report.json` 实际通过，9／9；UID 10148、目录、输入法、配置、偏好和虚构学习结果保留，三次便携备份调用及逻辑条目核对通过；普通覆盖安装，未卸载或清数据，最低可用 2,094,268 KiB |
| Android 5.0 Debug 编辑器／Return | 普通 Return 执行编辑器动作；Shift／Control／Alt／Meta + Return 转发完整组合键，真实编辑器收到相同键序列 | `editors1/phase-report.json` 及原生报告实际通过，5／5 Return；普通覆盖安装、未重建初始夹具或卸载／清数据，最低可用 2,332,872 KiB |
| Android 15 Debug 编辑器／Return | 同上；保留虚构草稿及输入所有权断言 | `editors1/phase-report.json` 与原始 Bundle／原生报告实际通过，5／5 Return；API35 PID 3378 的五种 Return 与原编辑器回归通过，未重建初始夹具或卸载／清数据，最低可用 1,890,336 KiB |
| 两平台真实系统目录授权 | 在现有设置界面实际选择专用空 `trime-saf-UUID` 目录，确认当前 UID 持久化读写授权和实际记住的树 URI | Android 5.0 tree6 实际通过，当前 UID 10053 持久化读写授权与同一 URI 已复核；Android 15 tree1 实际通过，UID 10148 对所选 primary 树的持久化读写授权和设置页 URI 一致；Observer 正常 stop，宿主／远端均 0 |
| 两平台严格导入 | 七项：畸形 YAML 整批拒绝、有效导入后激活、相同内容不重复部署、纯资源变更不部署、失败替换保留旧文件、目录无效主题回滚、同 ID 单主题失败回滚 | Android 5.0 import1 实际通过，7／7、原生完成 code −1／passed=true、PID 17126／UID 10053；Android 15 import1 实际通过，7／7、原生 code −1／passed=true、PID 6113／starttime 71308／UID 10148；最低可用 1,919,920 KiB |

Android 5.0 的系统目录准备 tree1–5 均为工具流程失败，原件保留，不计为授权或导入通过；实际成功的 tree6 与随后七项严格导入已独立完成，前五轮不变更为通过；Android 15 的真实目录授权及严格导入已通过。此前观察到的 API21 根标签为 `UP18API21`，仅作为同 boot、同 Debug 目标的真实标签证据，不能替代成功授权。

严格导入须保留源码／编译缓存／当前主题模型／方案选项／同步索引／偏好／授权等原断言。辅助程序通过真实系统选择器取得授权，不注入 grant 或目录偏好。Observer 的正常停止和实际宿主／设备完成均须核对；宿主 adb 退出不单独证明远端 instrumentation 已结束。原始失败和超时证据保留。

新执行 JVM 结果包含 32 KiB 边界、短读和大文件流式摘要，以及默认配置初始化和已有文件保留。它们的主机测试通过不代替两系统真实 SAF 和原生部署路径验收。

## 发布与溯源

目标仅为 `X2M7/trime:develop`，新 tag `v3.3.13-t9.18`；不向 `osfans/trime` 推送。稳定版 Latest 只能在实际验收完成后设置。APK 的 CODE 与之后仅发布文档变更的 PUB 分别固定，记录主仓库、递归子模块、词典、签名及 ABI。

| 产物 | SHA-256／实际校验 |
| --- | --- |
| ARM64 正式 APK | 11,966,487 字节；`00dbc090ec746e572cc18dfeaca250d3651eb439a5070e142434e55b656ed2e7` |
| x86_64 正式 APK | 12,047,703 字节；`7b1c116d6480276be5cc0418525fe2dc76694aee391aafc8bedd367619a48ae8` |
| 完整递归源码包 | 文档提交冻结后生成；实际字节数／SHA-256 见产物 manifest 和公开附件 digest，逐文件核验见 source-verification.json；冻结本文时尚未执行 |
| 草稿及公开附件下载 | 发布工具随后执行双阶段实际下载比对，结果见发布回执；冻结本文时尚未执行 |
| Release／Latest／原历史附件保持 | 由实际发布回执和公开 Release 状态确认；冻结本文时尚未执行 |
| Pages 部署提交／HTML／release.json 实物一致 | 发布后单独核对部署提交与公开文件字节，结果另记构建回执和工作区计划；冻结本文时尚未执行 |

两份 APK 哈希已由本轮冻结文件流式重算，并与实际 Release 结果及通过的 Manifest／签名／ABI 静态记录绑定一致。源码包、产物 manifest、发布回执和 Pages 网络核验均晚于仅文档变更的 PUB 提交；实际结果保存在外部构建记录与工作区计划中，避免为补填本文而改变已冻结的源码包目标。

手动附件仅两个正式 APK 与完整离线源码，保留 GitHub 自动 source zip／tar 和既有标签／产物。GitHub 自动源码压缩包不递归包含子模块工作树；完整包应包含所需子模块和固定清单。在线对应源码获取：

```sh
git clone --recurse-submodules https://github.com/X2M7/trime.git
cd trime
git checkout v3.3.13-t9.18
git submodule update --init --recursive
```

Android 5.0 runtime1 已按正常 STOP 结束，Root 等待原控制器实际退出 0，QEMU／logcat 均退出 0，清理错误为空且无 owned VM 残留；整轮最低可用内存 2,085,208 KiB（约 1.99 GiB），1 GiB 保护未触发。真实目录选择仅准备授权，不替代完整首次设置或取消竞态验收。独立 API21 导入证据审阅核对 1,818 项文件绑定、720 条 GUI 宿主命令和 16 帧原件；Root 实际查看 5 张 tree6 原图。

Android 15 的四阶段均属于 runtime2，同一 boot 为 `e504fc2d-f9f3-4bc7-9844-38b12c9b8250`。全部通过后正常 STOP，Root 等待原控制器实际退出 0，QEMU 退出 0，清理错误为空、无残留、1 GiB 保护未触发；控制器采样最低可用 1,886,736 KiB，目录辅助程序独立采样最低 1,883,760 KiB（约 1.80 GiB）。日志采集真实退出 255，保留该非零值：原件包含全部业务开始／结束标记和结束后日志，正常关闭命令实际返回成功；不据此证明日志退出的精确瞬间或最后关机阶段无遗漏。独立 `api35-integrity-review2/report.json` 六组复核通过，核对 2,307 个文件绑定、579 条 GUI 宿主命令和 13 个接受帧；Root 实际查看五张目录原图。最初 reader 把启动预检的日志前缀误当成完整文件摘要而失败，原件保留；修正后验证原长度前缀 SHA，并独立绑定最终完整日志，未改写结果或放宽业务断言。两系统均通过两主题共 40 轮显示、精确候选定位恢复与 100 次显隐；这些测试墙钟耗时不代表真机候选 P95。两平台的三次便携备份调用记录核对相同逻辑条目与字节摘要；宿主仅从原生 receipt 解码一份快照，没有独立拉取三份备份文件。

## 警告与范围限制

Android 5.0 完整日志库存与独立复核已完成：21,017 行，保留全部 3,024 条 W／E（1,879／1,145），F 为 0；102 个 tag 组完整核对，真实 Trime 进程时段有 93 条 W／E。显式致命事件、ANR、JNI fatal、ART finalizer、OOM、LMK 和 kernel panic 模式本轮均未匹配；4 次 empty-cache 策略回收单列，不冒充没有进程回收。9 条 Binder 句柄诊断、inactive InputConnection 的调用者／时序、WebView／ART 本地引用诊断的具体原因尚未闭合，不能归入已证实的 Android-only 排除项。独立报告 `build/upstream18/api21-runtime-log-review1/report.json` 只确认限定四阶段证据一致，未作零警告或完整设备验收声明。Android 15 的完整采集日志及独立分类复核完成：52,170 行，8,126 条 W／E（6,138／1,988）、431 个 tag 组，全部原始成员与内容逐一核对；Trime 进程共 466 条，其中旧／新正式包各 19 条、Debug 428 条，数量相同不证明根因相同或无问题。原件未匹配 Java／native 致命事件、ANR、OOM 或 Trime JNI 链接错误；启动阶段有真实 `am_low_memory` 1 条及 LatinIME 被 SIGKILL，发生在首次安装 Trime 前，发送方和确切原因未确认；33 条 `am_kill` 中 20 条为 empty-cache 策略回收，不能称没有内存压力或进程回收。9 条 ART finalizer 诊断中，4 条来自旧正式版、新正式版、Debug 编辑器和 Debug 设置的真实 Trime 进程，其余 5 条来自 Android 进程，引用对象根因均未知。严格导入维护期间有一次生产 RimeDispatcher 任务等待 5,852 ms，不能计作候选 P95。WebView 的 renderer error 出现在同一测试控件 remove/destroy 后，随后有系统 isolated-not-needed 回收和 Zygote 正常退出记录；保留原错误，不宣称已修复根因。动画丢帧／超时、回调取消、Binder、依赖反射、审计丢失／限流及磁盘环境诊断均保留限定归因，不能一并排除为系统噪声。跨 logcat／模拟器控制台的 77 条 Binder undelivered 捕获可能重复，不计为 77 个独立故障。最终审阅见 `build/upstream18/api35-runtime-log-review1/report.json`（SHA-256 `7fe080ee10c8b8cb32c77ba3e68c40c267271a67aa58ba7fd1c11dc3ba45d999`）；其结论仅为受测四阶段证据一致，明确不认定全部警告已消除。此前 ART finalizer Reference 诊断出现在旧版和新版 Trime 服务，尚未定位具体引用或唯一根因；不能仅归为独立辅助程序或已证实的系统噪声，也不能称所有系统／应用／依赖运行警告已经消除。

本轮限定范围不等于公开界面完整功能矩阵、真实设备 TalkBack／TTS、物理 ARM64／OEM／16 KiB 页、候选 P95／PSS 或长期驻留验收。跨包名迁移需独立导出／导入和新授权；本版保持原包名与证书，不能建议通过卸载保留词库。32 位设备不在两份新 APK 的安装范围内。

首次 Android 15 启动等待的历史记录（2026-10-16）：20 分钟连续 4 GiB 启动内存等待实际退出 2；未启动 VM／控制器，该尝试未执行本平台任何验收。等待记录及 Root 实际退出回执保留于 `build/upstream18/api35-start1-memory-wait.jsonl` 与 `api35-start1-root-wait.json`。未降低门槛、未停止文件索引或用户应用；该次等待结束时公开 fork 与 Latest 未变，尚未推送或发布。后续 runtime1 在 Trime 安装或业务检查前因 adbd 重启时的 `closed`／`device not found` 连接窗口失败，控制器／VM 实际退出 1／0；原结果仍为失败，未写入通过凭证。经审阅的新控制器保留三次 root 请求、180 秒 boot 上限及 4 GiB 启动／1 GiB 运行保护，仅在已观察重连窗口内等待恢复。后续 runtime2 已完成四阶段及正常停机，结果见上文；runtime1 失败不作为 Trime 已崩溃的证据，也不能被改记为通过。
