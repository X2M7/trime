# PR #2183 exact-b14 ARM64 device delivery

状态：**HOST_PREPARED / PHYSICAL NOT_RUN**。使用原始 `com.osfans.trime-b14a2f1e-arm64-v8a-debug.apk` 与 `REAL-DEVICE-ACCEPTANCE.md`；结果填入 `device-result-template.json`。旧 `device-delivery/` 已因已知方案布局回归废弃，本目录是独立的新候选交付，旧原件未修改。

先运行 `sha256sum -c SHA256SUMS`，再核对 `candidate-identity.json`：HEAD `b14a2f1e0565cec4953f5a974bfcea023875369f`、tree `3b40eef39ce8245096c74b1a3a5066d9b4009df1`、base `19b048494ffa55cae52d5385dfa6d0611461c27d`、APK `a393b74c52ba78dd5e28240b390a6686b0af47d76750c9a8aa561b94758b57b2`、当前证书 `c137e00f34fe583772632ee915cdd156e830746d6fbf1c9e5e2f7d3d2f6239e2`。全量身份权威文件是原字节 `apk-audit-result.json`；`t9-input.md` 的 Git blob 为 `e4fde0aa867291ae72527e00e5bb5334aa38a6d4`，SHA-256 为 `24d3a29209fe629b8a6198624ee0df43110736f70da527ac18ad6335000394aa`。

这是 fork Ubuntu 工作流的 CI **Debug** 包，不是 stable。原签名与两条 META-INF service-entry 警告均保留在 `apk-audit-arm64-signer.stdout`。旧证书 `0d50a6c4b5af14741b9307952de134bff81ebf38c81d8030d1528200873f5c38` 只用于防止误认，绝不能替代当前证书。

实体设备必须执行旧 17 项及 R1、R2、R3、R4，不能缩减。R4 核心覆盖两主题的 T9→全拼→新编辑器、整数→邮箱字面 `a`→普通全拼选 `你`；`trime` 还要验证 `luna_pinyin`→`luna_pinyin_simp` 共用 QWERTY 时的精确 checked label；最后回 T9 在新编辑器选 `你好`。所有条目当前均为 NOT_RUN。

使用无私人数据的测试文本和测试机/独立用户档案。覆盖安装若因证书或版本受限，保留日用数据并停止；不要卸载、清空数据或允许降级来绕过。测试后按清单恢复原方案、主题与反馈设置。主机审计和模拟器计划都不能代替实体设备结果。
