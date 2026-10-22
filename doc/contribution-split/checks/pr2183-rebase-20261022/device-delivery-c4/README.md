# PR #2183 exact-c4 ARM64 physical-device delivery

状态：**HOST_PREPARED / PHYSICAL NOT_RUN**。本目录只交付 exact-c4 原始 ARM64 Debug APK、完整身份审计和真机清单；没有任何真机 PASS。

先在本目录运行 sha256sum -c SHA256SUMS。随后核对：

- HEAD c4b613f78835e7b914e02447955c6b89fe9839ab，tree d7639dfce3b6b5646bb004479a688148c1b19900，parent b14a2f1e0565cec4953f5a974bfcea023875369f，base 19b048494ffa55cae52d5385dfa6d0611461c27d
- 原始 ARM64 APK com.osfans.trime-c4b613f7-arm64-v8a-debug.apk
- APK SHA-256 12fd34dad063e76a0cdd132bbed83e802e02cd5f5ada80671f7f6935c09619c0，20603550 bytes
- 当前证书 SHA-256 f6409a2d9db2800f1acc9b2e614d73e4609df3652d265cf2c40eb8a64456deb2
- 完整审计 apk-audit-result.json SHA-256 58b3d5e7f60678c164269897764957053bacc47055eed69a1a657dfb3a53f336
- fork run 39806420824 / artifact 12438099455

ARM64 真机包与运行时使用的 x86_64 包来自同一 HEAD、run、artifact、完整审计和证书。x86_64 包 SHA-256 为 453c71544f67685db3b85fc91855ed27c5ff264d0a25671669642b0dc4b8732e；ABI 不同，所以两个 APK 字节摘要本来就不同。交付包没有重签或改包。

实体设备必须执行旧 17 项以及 R1、R2、R3、R4；device-result-template.json 中这 21 个顶层条目当前全部是 NOT_RUN。R4 必须包含 c4 forced-ASCII 回归：在全拼中文模式下依次打开整数框和邮箱框，中间不允许手动切 ASCII、布局或方案；邮箱输入一次小写 a 后 Editable 必须精确为 a，且不能停在 native preedit/candidates。还要覆盖 cached/same-target forced editor，并回到普通框证明保存的中文模式恢复、可用全拼选择精确你。两主题都做；trime 另做同 QWERTY 的 luna_pinyin 到 luna_pinyin_simp checked-label 路径。

优先使用测试机或独立用户。覆盖安装若因证书或版本受限，保留数据并停止；不得卸载、清空数据或允许降级来绕过。截图、视频和日志不得包含私人输入。旧 device-delivery/ 与 device-delivery-b14/ 已明确 superseded，原文件未修改；其 APK 或证书身份不得用于本候选。

这是 CI Debug 包，不是 stable/release。主机审计、静态源码评审、模拟器和空模板都不能代替真机结果，也不能据此将 PR 标记 Ready 或授权合并。
