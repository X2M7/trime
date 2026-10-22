# PR #2183 exact-c4 ARM64 真机验收清单

> 当前状态：**NOT_RUN**。本文是操作清单，不是测试结果。所有结果写入 device-result-template.json。

## 1. 候选身份

| 项目 | 精确值 |
| --- | --- |
| HEAD | c4b613f78835e7b914e02447955c6b89fe9839ab |
| tree | d7639dfce3b6b5646bb004479a688148c1b19900 |
| parent | b14a2f1e0565cec4953f5a974bfcea023875369f |
| upstream base | 19b048494ffa55cae52d5385dfa6d0611461c27d |
| APK | com.osfans.trime-c4b613f7-arm64-v8a-debug.apk |
| APK SHA-256 / bytes | 12fd34dad063e76a0cdd132bbed83e802e02cd5f5ada80671f7f6935c09619c0 / 20603550 |
| certificate SHA-256 | f6409a2d9db2800f1acc9b2e614d73e4609df3652d265cf2c40eb8a64456deb2 |
| package / version | com.osfans.trime.debug / 3.3.13 (20261101) |
| build / ABI | CI Debug / arm64-v8a |
| producer | X2M7/trime run 39806420824, artifact 12438099455 |
| full audit SHA-256 | 58b3d5e7f60678c164269897764957053bacc47055eed69a1a657dfb3a53f336 |
| current behavior doc blob / SHA-256 | e5fe70dc917d3229efe3da3ca096a8c89132d735 / 2cb473b0bb404569030fb99e5d5beca4530d809e6ec09f9a6ed5ed3034599bf0 |
| current source review SHA-256 | e1a7ff1e68af5f0038f80c1a43722522977f34eb4e7efd1dfa2b0cecc42ab611 |

开始前运行 sha256sum -c SHA256SUMS。任一身份不符立即停止。本目录包与运行时 x86_64 包 453c71544f67685db3b85fc91855ed27c5ff264d0a25671669642b0dc4b8732e 来自同一 producer；不能把 x86_64 APK 装到 ARM64 真机，也不能用旧交付 APK 或证书替换本候选。

## 2. 真机、数据与安装

填写实体设备厂商、型号、Android/API、安全补丁、ABI 顺序、实际进程 ABI、build fingerprint、测试者、带时区的起止时间。只有真实设备可计入。

使用测试机、次要用户或无私人数据的环境。覆盖升级前备份用户目录、自定义配置、当前方案、主题、反馈设置及一个可识别测试用户词。若签名、版本或系统策略阻止覆盖安装，保存错误并记 BLOCKED；不得通过卸载、清数据或降级绕过。全新测试机应写明升级保留项不适用，不能填 PASS。测试结束后恢复原方案、主题与反馈设置。

每项记录实际按键、前后文本、可见方案勾选标签、布局、候选或预编辑、截图或短视频名。失败后先保存现场。崩溃、白屏、文本重复或丢失、私有 ~、旧编辑器状态串入新框均为 FAIL。

## 3. 通用准备

1. 通过公开方案选择器启用并选择朙月拼音九键 (luna_pinyin_t9)，等待实际部署成功和候选可用。
2. 准备空多行框、声明 Send/Search/Done 的单行框、chat、integer、email、password 测试框。每项用新空框，除非步骤明确要求连续切换。
3. trime 与 tongwenfeng 两个内置主题分别执行 C1-C8；T17 只在 tongwenfeng 执行。
4. 候选按屏幕实际内容记录。C7 冻结子路径要求 raw 64 切英文时精确提交你一次；tongwenfeng 全角叹号必须按实际 U+FF01 记录。

## 4. 旧 17 项（当前全部 NOT_RUN）

| ID | 主题 | 操作与验收 |
| --- | --- | --- |
| trime-C1 | trime | 拼音筛选、解锁、撤销、取消：64 后可选拼音；锁定/解锁/撤销一致；取消后文本、拼音栏和预编辑为空；无私有 ~ 标记；状态 NOT_RUN |
| trime-C2 | trime | 选汉字后立即新输入：候选只提交一次；后续 6 为新组合；取消只清新组合，已提交文本不变；状态 NOT_RUN |
| trime-C3 | trime | 空格路径：首次空格仅提交高亮候选一次；第二次为空组合字面空格；无残留锁定；状态 NOT_RUN |
| trime-C4 | trime | 删除、撤销边界：退格依次处理锁定和原始数字；撤销仅恢复紧邻一步；不得删除组合前文本；状态 NOT_RUN |
| trime-C5 | trime | Return 原始码：组合 Return 精确提交原始 64；多行换行；动作框执行声明动作且不重复；状态 NOT_RUN |
| trime-C6 | trime | 数字页与恢复：数字页 2 立即字面提交；返回当前 T9；待定 64 长按切换路径只提交规定原始输入一次；状态 NOT_RUN |
| trime-C7 | trime | ASCII、符号页与恢复：待定 64 切英文精确提交你一次并可继续 ASCII；字母/符号字面输入；恢复当前 T9；状态 NOT_RUN |
| trime-C8 | trime | 编辑器边界：旧 64/ni 锁定不串入新聊天框；新 6 可取消为空；无白屏或崩溃；状态 NOT_RUN |
| tongwenfeng-C1 | tongwenfeng | 拼音筛选、解锁、撤销、取消：64 后可选拼音；锁定/解锁/撤销一致；取消后文本、拼音栏和预编辑为空；无私有 ~ 标记；状态 NOT_RUN |
| tongwenfeng-C2 | tongwenfeng | 选汉字后立即新输入：候选只提交一次；后续 6 为新组合；取消只清新组合，已提交文本不变；状态 NOT_RUN |
| tongwenfeng-C3 | tongwenfeng | 空格路径：首次空格仅提交高亮候选一次；第二次为空组合字面空格；无残留锁定；状态 NOT_RUN |
| tongwenfeng-C4 | tongwenfeng | 删除、撤销边界：退格依次处理锁定和原始数字；撤销仅恢复紧邻一步；不得删除组合前文本；状态 NOT_RUN |
| tongwenfeng-C5 | tongwenfeng | Return 原始码：组合 Return 精确提交原始 64；多行换行；动作框执行声明动作且不重复；状态 NOT_RUN |
| tongwenfeng-C6 | tongwenfeng | 数字页与恢复：数字页 2 立即字面提交；返回当前 T9；待定 64 长按切换路径只提交规定原始输入一次；状态 NOT_RUN |
| tongwenfeng-C7 | tongwenfeng | ASCII、符号页与恢复：待定 64 切英文精确提交你一次并可继续 ASCII；字母/符号字面输入；恢复当前 T9；状态 NOT_RUN |
| tongwenfeng-C8 | tongwenfeng | 编辑器边界：旧 64/ni 锁定不串入新聊天框；新 6 可取消为空；无白屏或崩溃；状态 NOT_RUN |
| tongwenfeng-T17 | tongwenfeng | 符号类别宏、候选提交、数字页与 T9 恢复：类别产生实际标点候选；仅提交一次；返回后新组合正常；最终精确为所选标点+2；无宏/私有标记泄露；状态 NOT_RUN |

C1-C8 的详细操作遵循现有旧 17 项定义 android-smoke-minimal-v4.json（SHA-256 c6600665bed383da968c9ad7ed9dd9db571537abeb36eb757f904890bd806318）。不得省略 C7 的 active 64 到 English 到 你a 子路径或 C8 的新编辑器隔离。

## 5. R1 Return（NOT_RUN）

依次聚焦空多行框、至少一种声明动作的单行框、再回多行框。核对 Return 标签和实际动作同步，无 null、无重复。若 fixture 支持自定义 action id/label，也要记录。最后用活动 T9 64 按 Return，必须精确提交原始 64。

## 6. R2 Shift（NOT_RUN）

- trime T9 到 letter 与 tongwenfeng T9 到 letter 没有显式 double_click：分别验证单击和长按；真实快速双击不得额外产生全局 Caps Lock。
- tongwenfeng 全拼 default 有显式 double_click: Shift_L3：真实快速双击应执行主题锁定并可解除。
- 每条记录实际布局、大小写结果和状态；切框或返回 T9 后 Shift 不泄漏。不得用两次慢单击代替双击。

## 7. R3 反馈（NOT_RUN）

先记录系统音、振动、自定义音效、按键朗读、提交朗读、TTS 引擎或语言是否可用及是否启用。全部关闭做基线；每个可用能力开启后至少连续五键；检查首次初始化、重复反馈、资源切换、音量零或关闭，以及切框、隐藏键盘、切换输入法后的陈旧反馈。不可用能力记 BLOCKED 并说明原因，交维护者裁定覆盖范围，不得伪造 PASS。

## 8. R4 方案、布局与 c4 forced-ASCII 回归（NOT_RUN，核心）

方案身份必须通过重开公开选择器看到的 checked label 证明；布局身份通过指定新编辑器的实际键盘证明。固定等待时间、相同 QWERTY 外观或旧编辑器都不能替代。

对 tongwenfeng 与 trime 分别完整执行：

1. 新普通框从 luna_pinyin_t9 开始，通过公开选择器切到 luna_pinyin，重开选择器记录精确 checked label。
2. 打开独立新普通框，确认保持全拼 QWERTY，不恢复旧 T9。
3. 确保该全拼普通框当前处于中文模式，可开始 ni 组合；取消组合，保留这一正常模式作为恢复锚点。
4. 打开新 integer 框，记录强制数字布局。
5. 立即打开新 email 框。integer 到 email 之间禁止手动切 ASCII、布局、方案、输入法，禁止重启或清数据。输入一次小写 a：
   - Editable 必须精确为 a；
   - a 不得留在 Rime native preedit/candidates；
   - 不得出现中文组合、旧数字布局、重复或丢失。
6. 再打开相同目标布局的另一 email 框或 password 框，不做手动 workaround，输入 a；必须再次字面提交，覆盖 cached/same-target forced-ASCII 分支。
7. 打开新普通框，必须恢复先前保存的中文模式；输入 full-Pinyin ni 并选择精确你。若仍为强制 ASCII、恢复陈旧 T9 或无中文候选则 FAIL。
8. 通过公开选择器回 luna_pinyin_t9 并重开核对 checked label；新普通框锁定 ni、hao 并选择精确你好。

trime 主题另做：从 luna_pinyin 公开切到 luna_pinyin_simp，重开选择器必须精确勾选朙月拼音·简化字；即使仍是同一 QWERTY，也要重复 integer 到 email 字面 a 且无预编辑或候选，到 same-target forced ASCII，再到普通框恢复中文并选精确你。

每个 forced-ASCII 子路径至少保留 integer 布局、email 输入前、输入后 Editable=a 且无预编辑或候选、same-target 结果、普通框中文恢复的证据。任一关键帧缺失，R4 不得 PASS。

## 9. 判定与交付

核心范围为旧 17 项、R1、R2、R4。R3 覆盖实体设备实际可用能力，缺口需维护者书面裁定。顶层状态只可改为 PASS、FAIL 或 BLOCKED；当前全部保持 NOT_RUN。单机通过只代表记录的型号、Android/API、ABI、本 APK SHA 和证书，不能泛化到所有 OEM 或 Android。

最终结果必须包含身份核对、设备信息、安装、备份、恢复、每项证据、失败材料、测试者与复核者签名及带时区时间。空模板、CI、主机审计、静态评审和模拟器均不构成真机通过，也不授权 Ready 或 merge。

## 10. 本清单的只读依据

- 完整 exact-c4 APK 审计：apk-audit-result.json，SHA-256 58b3d5e7f60678c164269897764957053bacc47055eed69a1a657dfb3a53f336
- 当前行为文档：t9-input.md，blob e5fe70dc917d3229efe3da3ca096a8c89132d735，SHA-256 2cb473b0bb404569030fb99e5d5beca4530d809e6ec09f9a6ed5ed3034599bf0
- forced-ASCII 静态独立评审：source-independent-review.json，SHA-256 e1a7ff1e68af5f0038f80c1a43722522977f34eb4e7efd1dfa2b0cecc42ab611
- 精确评审 diff：source-fix-reviewed.diff，SHA-256 2b4d7d6171bf7a77daa8b564e194ead13a2250c7b79cd82dc2014b97e8be477e
- 当前线性历史检查：source-history-check.json，SHA-256 0301041da96197bf9c39dba0aad4bf0d77715da7604740cf327678470fb52f11
- 同 producer 运行时绑定：runtime-x86_64-binding.json，SHA-256 52919b0ce3ee58c654f73c80008d4ea4575362f11d08eba540eb9207c4781778

限制：Debug 原包身份与静态设计评审不等于运行验证；缓存 native 产物不重述为本地 fresh compile；实体设备仍为 NOT_RUN。
