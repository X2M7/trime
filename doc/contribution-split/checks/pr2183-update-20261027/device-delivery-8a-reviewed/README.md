# PR #2183 ARM64 真机交付资料

状态：**HOST_PREPARED / PHYSICAL NOT_RUN**。本目录提供原 CI Debug APK、已核实际身份、固定清单和空结果模板；没有进行安装、覆盖升级或真机业务验收。本轮模拟器验收准备时仍为 PENDING，Ubuntu producer 成功也不等于整个五项 CI 已通过。

| 身份 | 实际值 |
| --- | --- |
| PR 源码 HEAD | `8a1ee0ea6538e220fcddf31ed6fd491f5bc3cca7` |
| 实际构建 checkout | `0ef0eb551a6f8d000266d4446348b2d2ab2823e9`（上游 PR synthetic merge） |
| 两者完整 Git tree | `b022a1e810e231af9f9776c2b5414faf6b5bd615` |
| 上游 base | `a047f74757fae0d7b5ef64e54f0b957dee2bf8f3` |
| Producer | `osfans/trime` / run `40527129836` / Ubuntu job `122272383005` / artifact `12745502662` |
| 原始 APK 文件名 | `com.osfans.trime-v3.3.12-110-g0ef0eb55-arm64-v8a-debug.apk` |
| APK SHA-256 / 字节数 | `7cb4e071373ef7506010120c845e243dfc00ac54e7b5e24860f90c1730743cde` / 20265180 |
| 签名证书 SHA-256 | `9188374217845103a7ca723f430ae446223ffbb7b48115b4cf65c6e7c59af2f4` |
| Package | `com.osfans.trime.debug` |
| 实际 manifest versionName / versionCode | `3.3.13` / `20261101` |
| ABI / minSdk / build | `arm64-v8a` / `21` / Debug、debuggable |

文件名中的 `v3.3.12-110-g0ef0eb55` 来自实际构建的 Git describe 元数据（BuildConfig `BUILD_VERSION_NAME`），不是 manifest 的 versionName。实际 manifest 为 `3.3.13`／`20261101`，原输出见 `apk-audit-arm64-badging.stdout`，Git 元数据见 `apk-audit-arm64-buildconfig.stdout`。源码与构建 commit 不同，完整源文件树相同；不据此声称 APK 字节或 Git 元数据相同。

先执行 `sha256sum -c SHA256SUMS`，然后核对 `candidate-identity.json` 和 `apk-audit-result.json`。本目录 APK 从[原附件](https://github.com/osfans/trime/actions/runs/40527129836/artifacts/12745502662)原字节复制，没有重新签名，也不是 stable/release 包。两条 META-INF service-entry 签名警告原样保留在 `apk-audit-arm64-signer.stdout`，不能宣称零警告。

安装前核对设备 ABI、已安装 package／证书／版本。日常安装与本包证书或包名不匹配时，先使用现有导出／备份功能保全词库和配置并核对备份，另选测试机或独立测试用户档案。**不要卸载、清除数据或允许降级来绕过安装限制；不改动用户日常安装。** 包名不同不属于覆盖升级；证书不同不能通过原地覆盖。不能把导出后新档案测试说成升级保留通过。

阅读 `REAL-DEVICE-ACCEPTANCE.md` 并填写 `device-result-template.json`。全部 44 个递归 `status` 字段都是 `NOT_RUN`，设备、操作者和测试时间留空。17 项旧业务、R1／R2／R3／R4 定义均保留，模板比对见 `template-preservation-validation.json`。`c4_forced_ascii_capability` 是保留的用例字段名，当前待测 APK 身份为本目录 8a／0ef，不能替换成旧 c4 包。

主机审计只确认本地原包身份与结构，不能替代真实设备、真实 TTS、OEM／ARM64 行为或升级测试。结果只能用于实际记录的设备／系统／ABI／APK／证书；PR 继续保持 Draft，等待真实结果。
