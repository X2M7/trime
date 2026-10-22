# PR #2183 exact-c4 ARM64 device delivery — reviewed v2

状态：**HOST_PREPARED / PHYSICAL NOT_RUN**。这是实体设备填写入口；没有任何真机 PASS。

此前 `../device-delivery-c4/` v1 模板相对当前有效 b14 模板的受保护块有 49 处对象差异（legacy 32、R1 3、R2 5、R3 9），模板验证未过。本 reviewed v2 替代 v1 作为记录模板；v1 目录 18 个文件保持原字节，目录 inventory SHA-256 为 `926589a7f777ed71716c3b1f4095043fc7841c51df411e50e60836b58170e124`。不要修改或删除 v1 以掩盖差异。

先运行 `sha256sum -c SHA256SUMS`，再读 `template-preservation-validation.json`。`device-result-template.json` 的 `legacy_17_cases` 和 R1/R2/R3 对象与 `../device-delivery-b14/device-result-template.json`（SHA-256 `0a211e51d341d821c8294b1c64bf48f48d15fe4c51205e19eb21bcf4bb1c8ab4`）逐字段深等；旧 R4 字段保留，活动 source head 更新为 c4，并仅追加 `c4_forced_ascii_capability`。全部递归运行 `status` 当前都是 `NOT_RUN`。

候选身份：

- HEAD `c4b613f78835e7b914e02447955c6b89fe9839ab`；tree `d7639dfce3b6b5646bb004479a688148c1b19900`；parent `b14a2f1e0565cec4953f5a974bfcea023875369f`；base `19b048494ffa55cae52d5385dfa6d0611461c27d`
- 原始 ARM64 APK `com.osfans.trime-c4b613f7-arm64-v8a-debug.apk`，SHA-256 `12fd34dad063e76a0cdd132bbed83e802e02cd5f5ada80671f7f6935c09619c0`，20603550 bytes
- 完整审计 `apk-audit-result.json`，SHA-256 `58b3d5e7f60678c164269897764957053bacc47055eed69a1a657dfb3a53f336`
- 当前签名证书 SHA-256 `f6409a2d9db2800f1acc9b2e614d73e4609df3652d265cf2c40eb8a64456deb2`
- X2M7/trime run `39806420824` / artifact `12438099455`
- 同 producer x86_64 APK SHA-256 `453c71544f67685db3b85fc91855ed27c5ff264d0a25671669642b0dc4b8732e`，运行时绑定 SHA-256 `52919b0ce3ee58c654f73c80008d4ea4575362f11d08eba540eb9207c4781778`

旧 17 项、R1、R2、R3、R4 均需按 `REAL-DEVICE-ACCEPTANCE.md` 执行。保留的契约包括：C7 活动 `64` 切英文时精确提交 `你` 一次并继续 ASCII 得到 `你a`；T17 的完整 legacy id；R2 的三布局、显式正控预期精确 `AA` 及 `observed_editable_AA_aa` 槽；R3 只覆盖设备实际 available/enabled 的反馈与 TTS 边界；旧 R4 的 literal `a`、精确 `你`、同 QWERTY 和回 T9 `你好`。c4 附加路径还要求中文全拼 → integer → email 精确 `a` 且无 native preedit/candidates → cached/same-target forced editor → 普通框恢复中文并选精确 `你`，两主题都执行且中间不得手动 workaround。

这是原 CI Debug 包，不是 stable/release。APK、完整 audit 和 producer payload 复用已核原字节，没有重签。主机审计、静态评审、空模板、CI 或模拟器都不能替代实体设备结果。两条 META-INF 签名警告、Actions endpoint lag、缓存 native 与单设备不可泛化限制均保留。
