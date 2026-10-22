# rebase 后模拟器重点检查

> 状态：**计划，未执行**。这些检查用于尽早发现 rebase 交互回归，不能替代 `REAL-DEVICE-ACCEPTANCE.md` 中的真机验收。

## 风险依据

| 上游提交 | 行为变化 | 与 T9 的交叉风险 |
| --- | --- | --- |
| `a45f6d65` Return | 同一策略同时决定 Return 标签和实际 editor action；编辑器切换时使缓存键盘失效并刷新标签 | T9 对活动组合有“提交完整原始码”路径；无组合时仍须走新 editor action 策略，且缓存 T9 键盘不能显示旧标签 |
| `a39859c1` Shift revert | 撤销无主题声明时的自动双击 Shift 锁定及其设置项，保留既有动作 | 两主题从 T9 进入的 `letter` 都无显式双击，不应凭双击自动锁定；只有 `tongwenfeng.default` 显式定义 `double_click: Shift_L3`，该控制动作仍应生效 |
| `b60c3299` feedback | TTS 与自定义 SoundPool 改为首次需要时创建；worker 请求转主线程；输入/销毁边界丢弃旧排队请求；失败后避免逐键重试 | T9 的候选提交可来自 Rime worker，频繁切换编辑器/布局可能撞上初始化、回调和销毁边界 |

## 建议矩阵

- P0：API 21 与 API 35 各跑 E01–E07；`trime`、`tongwenfeng` 都覆盖 Return，Shift 严格按当时实际布局字段覆盖两个无显式双击的 T9 `letter` 和一个 `tongwenfeng.default` 显式正控。
- P0：API 35 跑 E08–E12，分别覆盖有 TTS 引擎和无/禁用 TTS 引擎环境。
- P1：API 21 复跑反馈基本开关和生命周期 E08–E10，重点观察旧 API 的 TTS、SoundPool 与振动兼容。
- 所有结果绑定 rebase 后确切 HEAD、APK SHA、证书、ABI、系统镜像/API、Trime PID/starttime，并保留目标 PID 的异常日志。模拟器结果字段使用 `EMULATOR_PASS/FAIL/BLOCKED`，避免与真机 PASS 混用。
- E08–E13 是受控模拟器上的反馈回归范围；它们不把实体设备缺少 TTS 引擎或不可感知某项反馈自动升级为全部九键失败。真机按实际可用/启用能力记录 R3，并由维护者裁定覆盖缺口。

## P0 用例

| 编号 | 操作 | 期望 / 判定重点 |
| --- | --- | --- |
| E01 Return 缓存刷新 | 同一 Trime 进程内依次聚焦多行、Send、Search/Done、再回多行测试框；每次截图 Return 标签并触发一次 | 标签在不重建进程的情况下随 editor 更新；实际 action 与显示/EditorInfo 一致；回到多行后是换行路径，无上一个 action 残留 |
| E02 自定义 action | fixture 提供非空 `actionLabel` 和非 UNSPECIFIED `actionId`，再提供 actionId 但空 label 的字段 | 第一种执行自定义 id；第二种按主题/标准回退显示，绝不显示 `null`；日志/fixture oracle 只记录一次 action |
| E03 T9 组合 Return | 两主题各输入 `64`、锁定 `ni`、点 Return；随后在空编辑器点 Return | 活动组合只提交原始 `64` 一次，无 `ni/~ni~/pending tail`；空组合才执行该编辑器的 Return action |
| E04 NO_ENTER_ACTION / NULL | fixture 分别声明 multiline/NO_ENTER_ACTION 和 TYPE_NULL，触发 Return | 走 key event 路径；多行产生一次换行，不误发 Send/Done；标签使用主题默认 |
| E05 两个 T9 `letter` 的 Shift | 分别由两主题 T9 长按 `123/EN` 进入 `letter`，单击 Shift 输入字母；在同一连续动作中快速真实双击后输入；长按后输入；切换编辑器再输入 | 两个 `letter` 均无显式 `double_click`，单击/长按保持各主题原语义，双击不会获得已回退的全局自动锁定；Editable 大小写结果和状态不跨编辑器异常泄漏。源码：`trime.yaml:861,1119-1123` 继承第 1083 行 Shift；`tongwenfeng.trime.yaml:4007,1167-1198` |
| E06 `tongwenfeng.default` 显式控制 | 切换到全拼/`default` 布局，在第 1149 行显式双击键上单击、快速真实双击、解除并输入字母 | `double_click: Shift_L3` 的真实 DOUBLE_CLICK 路径仍锁定并产生预期 `AA/aa`；第 4042 行 `shift_lock: click` 不被 revert 吞掉；解除后恢复。两次触摸之间不得插入 PID/截图等探测，也不能用两次慢单击代替。此结果只属于 `default`，不能代替 T9 `letter` |
| E07 Return/Shift 快速边界 | 在 T9/字母页间切换，同时在 multiline 与 action editor 间快速切换，重复 20 次 | 无旧 Return 标签、意外 Caps 状态、键盘白屏、异常布局或重复提交 |
| E08 反馈全关冷启动 | 清空进程后保持声音、振动、按键/提交朗读全关，首次调出键盘并输入 | 无反馈；功能输入正常；不出现 TTS bind/stop/shutdown 噪声或自定义 SoundPool 初始化。日志只能证明观测到的行为，不能单凭无日志证明未分配资源 |
| E09 反馈逐项首次启用 | 逐项开启系统音、振动、按键朗读、提交朗读、自定义音效；记录首次请求及稳定后的 5 次请求 | 首次按需初始化不崩溃/卡死；稳定后一键一次。SoundPool 异步装载前的首键是否静音要如实记录，不能事后补写成已播放 |
| E10 worker 朗读与边界 | T9 提交候选触发提交朗读；初始化期间快速键入多个键；立刻 finish input、换 fixture 或切 IME | worker 请求安全落到主线程；初始化阶段最多保留最新请求；边界后不朗读旧文本；返回后新请求正常 |
| E11 反馈销毁/重建 | 在 TTS/SoundPool 初始化回调可能未完成时连续隐藏/显示键盘、切换 IME、重开编辑器 20 次 | 无 stale callback 复活旧资源、重复播放、`not bound` teardown 告警、崩溃或每次按键反复初始化 |
| E12 后端失败 | 在无 TTS 引擎或被禁用的模拟器上开启朗读并连续按键；随后重启输入生命周期 | 首次失败后输入仍工作；同一生命周期不逐键产生初始化风暴；新生命周期可重新尝试。失败不应被记为朗读功能 PASS |

## P1 补充

| 编号 | 操作 | 期望 / 判定重点 |
| --- | --- | --- |
| E13 自定义音效重载 | 选音效 A，输入；切换 B，输入；设音量 0；关闭自定义音效并启用系统音 | 只播放当前选择；无旧池重复声音；音量 0 静音；系统音回退可用 |
| E14 启动与旧数据 | 使用有旧 `default.custom`、用户词和反馈设置的快照覆盖升级 | `luna_pinyin_t9` 的启用选择不覆盖旧配置；用户词仍可用；新反馈资源行为服从保留的开关 |
| E15 完整旧 17 项抽查 | API21/API35 至少各跑两主题 C5、C7、C8 和 T17；C7 必须包含活动原始 `64` 直接切英文后严格得到 `你` 一次、无私有标记并继续正常 ASCII；完整 17 项仍由正式矩阵决定 | 优先覆盖与 Return、布局切换、编辑器生命周期、符号宏重叠的路径；抽查不能被描述成完整 17 项通过 |

## 失败材料

失败时保留首个原始现场：操作前后 fixture oracle、屏幕 PNG/XML 或录像、目标 PID/starttime、完整动作顺序、相关日志前后文、候选 APK 与系统镜像身份。重试结果另列，不能覆盖首次失败。

