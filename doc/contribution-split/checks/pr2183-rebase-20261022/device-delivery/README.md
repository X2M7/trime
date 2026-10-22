# PR #2183 ARM64 真机测试交付

使用本目录 APK 与 `REAL-DEVICE-ACCEPTANCE.md`，构建身份已按原 CI 包填写。`candidate-identity.json`、`SHA256SUMS`、`manifest.json` 绑定本交付的实际字节。`device-result-template.json` 的实体设备信息和全部用例仍为 NOT_RUN，等待测试者填写，不能据此标 Ready。

`plan-only-manifest.json` 和 `correction-record.json` 是下载 APK 之前生成的历史计划原件，保留其原始字节。它们当时的 PENDING/待填描述已被本次交付身份取代，不能用来判断当前 APK 身份是否已核验；当前清单以 `manifest.json` 为准。`EMULATOR-FOCUS.md` 只定义模拟器范围，不是真机结果。

这是 CI Debug 测试包，若与日用安装的证书/版本不兼容，保持数据并停止覆盖安装；使用测试机或独立用户档案，不通过卸载或清数据绕过。
