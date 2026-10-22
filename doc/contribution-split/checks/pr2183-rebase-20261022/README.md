# PR #2183 当前整改与证据

当前源码为 `b14a2f1e0565cec4953f5a974bfcea023875369f`，基线 `19b048494ffa55cae52d5385dfa6d0611461c27d`。四条 Conventional Commits、零 merge，原 PR 保持 Draft。修复方案切换恢复旧九键的真实缺陷，JNI、主题资产和递归子模块未改变。

- [修复与推送回执](schema-layout-regression/source-push/receipt.json)、[独立源码审阅](schema-layout-regression/source-independent-review1.json)。
- [fork 原始四 ABI APK 身份](schema-layout-regression/cloud-fork-run-39796208691-ubuntu1/apk-audit/result.json)已核验，原包未重签；[上游 synthetic merge 的单独审计](schema-layout-regression/cloud-pr-run-39796220972-ubuntu1/apk-audit/result.json)保留各自 BuildConfig 与证书差异。
- [首轮新源控制失败归因](schema-layout-regression/cloud-control-run-39796394613/failure-adjudication.json)：应用 JVM 360 项通过，JNI 因基准路径不存在未执行；[独立控制分支修正](schema-layout-regression/cloud-control-fix1/push-receipt.json)已触发新运行。
- [新原始 APK／执行脚本绑定](runtime/original-apk-binding-b14.json)和[执行前静态／失败路径审阅](runtime/sequence-independent-review1.json)。Android 5.0／15 限定回归尚未闭合；清单或脚本不构成设备通过。

GitHub Actions Results 故障影响任务终态回报。APK 字节身份通过不等于整轮 CI 成功。所有真机用例仍 NOT_RUN；新 ARM64 真机指导另行准备。旧包已知有方案恢复缺陷，只保留追溯。依赖／工具／META-INF 签名警告全部保留，不宣称零警告、Ready、合并或稳定发布。

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
