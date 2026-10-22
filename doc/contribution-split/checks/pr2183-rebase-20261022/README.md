# PR #2183 当前整改与证据（c4b613f7）

当前源码 `c4b613f78835e7b914e02447955c6b89fe9839ab`，基线 `19b048494ffa55cae52d5385dfa6d0611461c27d`，五条规范提交、零 merge。原 PR 保持 **Draft，真机 NOT_RUN**。[当前限定总表](current-c4-qualified-scope.json)为本节入口。

- [云端资格](schema-layout-regression/c4-cloud-terminal1/qualification2.json)：fork／上游正式各 5/5 成功，新执行 360 JVM／115 JNI 通过。[原始四 ABI APK 审计](schema-layout-regression/c4-fork-run-39806420824-ubuntu1/apk-audit/result.json)与[真实终态补充](schema-layout-regression/c4-cloud-terminal1/terminal-status-addendum1.json)保留原字节；旧端点滞后句由后续终态覆盖。
- [Android 5.0 业务资格](runtime/api21-c4-independent1/api21-c4-business-aggregate-qualification1.json)及 [Android 15 业务资格](runtime/api35-c4-independent1/api35-c4-business-aggregate-qualification1.json)：每系统 12 实际成功会话／14 逻辑项／134 帧；K1 各嵌入两主题 R4 一次，没有额外 K1 会话或重复计数。当前原包由 [v7 绑定](runtime/original-apk-binding-c4-v7.json)及仅 Shift 使用的 [v8 绑定](runtime/original-apk-binding-c4-v8-shift.json)固定。精确提交文本、拼音状态、进程和真实触摸断言保留，原采集日志 false／空 checks 未改。
- Root 的 [API21 会话身份复核](runtime/api21-c4-root-session-index1.json)、[API35 会话身份复核](runtime/api35-c4-root-session-index1.json)、[选定原图映射](runtime/root-image-publication-map.json)和[最终冻结视觉记录](runtime/root-image-review-c4.json)单列。Root 实际复核 34 张 API21、26 张 API35 原 PNG；[选图目录](runtime/selected-images)保存这些原字节，不称全帧人工审核。旧资格内的 ledger SHA 是当时快照，不要求等于随后追加并冻结的最终 ledger。
- [API21 终态资格](runtime/api21-c4-independent1/api21-c4-terminal-qualification1.json)及 [API35 终态资格](runtime/api35-c4-independent1/api35-c4-terminal-qualification1.json)：模拟器／控制器均正常退出 0、SDK 10／25 项内容不变、输入法设置恢复且无残留。API35 采集器实际断开 255、前置 phone／SystemUI ANR、内核 IRQ #35 的 F 级事件均保留；两系统全部原 W/E/F 分别 2095/2435/0、11198/2464/1，Trime 目标 W/E 也没有删改或免责。终态边界核验不等于全系统零错误或因果证明。
- [ARM64 真机交付 v2](device-delivery-c4-reviewed/README.md)、[完整验收表](device-delivery-c4-reviewed/REAL-DEVICE-ACCEPTANCE.md)、[结果模板](device-delivery-c4-reviewed/device-result-template.json)及[Root 字节／契约核验](device-delivery-c4-reviewed-root-verification.json)：44 个递归状态全为 NOT_RUN。[后续 Shift 绑定说明](device-delivery-c4-reviewed-runtime-addendum1.json)不改变原 APK 或手机结果。原包在[Actions 附件](https://github.com/X2M7/trime/actions/runs/39806420824/artifacts/12438099455)，将其 ARM64 APK 放入 v2 目录后再运行 SHA256SUMS；APK 不存入此 Git 索引。

两项真实产品失败、旧时序拒收、当前 API21 v7 启动工具拒收及旧 v1 手机模板失败全部保留。v2 保留旧十七项和 R1–R3 契约，R4 只增加当前强制 ASCII 路径；v1 不再作为记录入口。本轮不是全部旧十七项、展开候选滚动恢复、全应用、快速焦点／全部缓存模式、物理设备、TalkBack、Release、升级或正式性能验收。APP_STORAGE 是应用专用外部存储，不是私有 /data 或 FUSE 绕过证明。没有零警告、Ready、合并或稳定发布声明。

`manifest.json` 只列实际复制到此索引的文件，并排除自身。APK／DEX JAR／完整原始设备流没有存入 Git；原文件路径、全量 SHA、官方 run/artifact 及有界原文用于追溯。嵌套交付 manifest 可能列出未进 Git 的 APK 身份，不表示该二进制已保存到索引。

# 历史 b14 修复与证据（已退出验收候选）

当前源码为 `b14a2f1e0565cec4953f5a974bfcea023875369f`，基线 `19b048494ffa55cae52d5385dfa6d0611461c27d`。四条 Conventional Commits、零 merge，原 PR 保持 Draft。修复方案切换恢复旧九键的真实缺陷，JNI、主题资产和递归子模块未改变。

- [修复与推送回执](schema-layout-regression/source-push/receipt.json)、[独立源码审阅](schema-layout-regression/source-independent-review1.json)。
- [fork 原始四 ABI APK 身份](schema-layout-regression/cloud-fork-run-39796208691-ubuntu1/apk-audit/result.json)已核验，原包未重签；[上游 synthetic merge 的单独审计](schema-layout-regression/cloud-pr-run-39796220972-ubuntu1/apk-audit/result.json)保留各自 BuildConfig 与证书差异。
- [首轮新源控制失败归因](schema-layout-regression/cloud-control-run-39796394613/failure-adjudication.json)：应用 JVM 360 项通过，JNI 因基准路径不存在未执行；[独立控制分支修正](schema-layout-regression/cloud-control-fix1/push-receipt.json)后的[新运行实际执行证据](schema-layout-regression/cloud-control-run-39798930637/artifact-execution-audit.json)通过 360 项 JVM 和 115 项独立 JNI 断言；[最新云端资格](schema-layout-regression/cloud-independent1/qualification1.json)确认正式两轮各 5/5 项与新控制均成功；此前终态滞后保留为历史事实。
- [新原始 APK／执行脚本绑定](runtime/original-apk-binding-b14.json)和[执行前静态／失败路径审阅](runtime/sequence-independent-review1.json)。Android 5.0／15 限定回归尚未闭合；清单或脚本不构成设备通过。

GitHub Actions Results 故障曾影响任务终态回报；已以恢复后的真实终态闭合云端资格。正式日志与签名 114 条警告、独立控制补充 58 条警告均保留各自范围，发射次数不等于独立缺陷数。所有真机用例仍 NOT_RUN；新 [ARM64 真机指导](device-delivery-b14/README.md)已完成 exact-b14 身份绑定与 [Root 独立字节核验](device-delivery-b14-root-verification.json)。旧包已知有方案恢复缺陷，只保留追溯。依赖／工具／META-INF 签名警告全部保留，不宣称零警告、Ready、合并或稳定发布。

本目录 manifest 只列实际保存到索引的文件；APK 和完整原始日志未存入 Git，原审计的本地路径及 Actions run/artifact 用于追溯。


# 历史初始 rebase 与交付（1754bb51，已退出验收候选）

本节历史源码 `1754bb51931407ac916549edc2ae57bba4c073bc`，基线 `19b048494ffa55cae52d5385dfa6d0611461c27d`。原 PR 已按明确 lease 更新，仍为 Draft。

- [三条规范提交和精确 tree 等价证明](linear-source-binding.json)
- [原分支推送与 Draft 读回](original-pr-update1/receipt.json)
- [360 项应用 JVM / 115 项宿主 JNI 断言](cloud-run-39787582756/machine-audit.json)
- [首个 Ubuntu 四 ABI 原包核验](cloud-run-39787303403/apk-audit/result.json)
- [真实设备验收清单](device-delivery/REAL-DEVICE-ACCEPTANCE.md)与[预填构建身份的结果模板](device-delivery/device-result-template.json)
- [原始 APK 构建附件](https://github.com/X2M7/trime/actions/runs/39787303403/artifacts/12430451882)：选 `com.osfans.trime-1754bb51-arm64-v8a-debug.apk`；[身份与 SHA-256](device-delivery/candidate-identity.json)。这是测试用 Debug 原包，不能据其显示名称认定为稳定 Release。覆盖安装失败时保持原数据，停止并报告，不卸载或清数据绕过。

本目录已记录 rebase、宿主测试、首个 APK 身份及正式两条 CI；[正式 fork 与上游 PR 流水线](cloud-final-audit/formal-cloud-audit.json)已各五项全部通过；新源 Android 5.0/15 限定回归等待完成。原临时分支只使用成功的 check／Ubuntu job，其他三项因正式同源验证完成而[取消](cancel-redundant-preflight/receipt.json)，不能称临时整轮成功。所有实体设备用例仍为 NOT_RUN，清单与空模板不构成真机结果。旧 head 的 Android 结果保留其原出处，不升级为新源或真机通过。[399 条扩展范围的警告行记录](cloud-final-audit/warning-summary.json)与[原行](cloud-final-audit/warning-records.jsonl)保留，包括 Windows Gradle cache 锁文件保存失败；该数字不是缺陷数；早期 161 行仅覆盖正式两轮及临时成功 job，本次增加控制运行、临时取消 job 与 APK 签名输出。依赖／工具／Debug 签名警告保留，不宣称全局零警告、Ready、合并或稳定发布。

`manifest.json` 只覆盖本索引实际保存的证据。原云端审计 JSON 中的 APK、ZIP、完整日志及其他本地原件引用用于追溯，未把其全部复制到此索引；云端 run/artifact 地址提供原始附件。`device-delivery/manifest.json` 是本地完整交付的清单，包含未存入 Git 的 APK 字节身份；不会把 APK 缺席索引误记为已保存。

本索引只存同进程触摸工具的源码、构建脚本与身份／审阅记录；身份文件中的DEX JAR保留在本地完整交付，不作为已存入Git的二进制。真实设备日志中的快速触摸与CLICK／DOUBLE_CLICK分发仍须分别验收，HOST审阅不构成设备通过。
