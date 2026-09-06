# T01：t9.2 双词库基线补验

日期：2026-09-06。本轮沿用已通过启动探针及九键界面测试的 APK，不修改运行时代码、不重新构建、不递增版本号。
启动和界面结果见 [启动阻塞回归](STARTUP-ANR.md)；本文件记录独立引擎的全新/固定学习后两组结果，不能替代 Android UI 或真机性能验收。

## 固定身份

| 项目 | 值 |
| --- | --- |
| 本轮 run | `8398cf96f2db-1e937795ce9e` |
| 包名 / versionName / versionCode | `com.osfans.trime.debug` / `3.3.13-t9.2` / `20261103` |
| APK 内嵌 Git SHA | `07717d0d31c8c05da6413a9f1282194b00f0f5bc` |
| APK SHA-256 | `8398cf96f2db9f0b4abff4739501b0d051357cf8b3a5b2444e47d837d7090286` |
| debug 签名证书 SHA-256 | `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2` |
| native 库 SHA-256 | `d0a44d7e4ac7c0656e633f786505905abfeacb813f8feb03e7f4410cbeb2dcf9` |
| 九键 schema SHA-256 | `9f04415df4bc0342dfa9a11212ef34d57445c60c6b234c9e73835c685308c41f` |
| 测试语料 SHA-256 | `85101958818eae924d72519e689091665f74b4447796b749a9eae28ab2742bce` |
| 测试驱动二进制 SHA-256 | `1fbbfb93d639eaf8a97a1c16f4f9d200df9c180aad78da53b3090f92e31002bb` |

本地目录为 `build/baseline/8398cf96f2db-1e937795ce9e/`，下文的结果路径均相对于它。
旧界面证据目录 `8398cf96f2db-ceeb3eaf5bcd` 不覆盖、不改写；其两组仍为 `not_run`，不能把不同 run 的记录混为一次执行。

## 环境与隔离

- `hk_api35` 临时只读实例，`emulator-5560`，Android 15 / API 35，2 GB、2 核，软件 GPU；未保存快照或清空原 AVD。
- 系统指纹：`google/sdk_gphone64_x86_64/emu64xa:15/AE3A.240806.043/12960925:userdebug/dev-keys`。
- 使用 APK 自带的 ARM64 库，经过 `libndk_translation.so` 执行；不是 Linux 系统 Rime，也不是手机原生 ARM64 性能数据。
- 引擎测试只在本轮 `/data/local/tmp/trime-baseline-1d69092175414632b159832c6ccbdb25/` 中工作，不安装 Trime 或修改其应用数据，不接触个人词库。
- 本轮 cold 从原始资源部署，没有导入旧 APK 的编译词典；learned 仅复用本轮 cold 生成的系统 YAML/BIN，不复制 userdb 或学习记录。
- 临时实例停用 Google Play 服务、Digital Wellbeing、Google Search、Android System Intelligence、Restore，减少后台负担；退出只读实例后不保留这些设置。开机有一次 System UI ANR，不能把独立引擎测试作为应用启动无 ANR 的证据。
- 主机没有并行运行 Gradle/native 库构建，测试驱动单独编译；各阶段顺序执行，阶段超时 1800 秒。

## 执行

以下为本轮执行命令。run 为一次性目录，已归档结果只运行 `verify`；复测需先 `prepare` 新目录，不重用此 run。

```sh
python3 script/baseline/baseline.py run \
  --run build/baseline/8398cf96f2db-1e937795ce9e \
  --serial emulator-5560 --ndk /path/to/android-sdk/ndk/28.0.13004108 \
  --phase-timeout 1800 --reuse-cold-build
python3 script/baseline/baseline.py verify \
  --run build/baseline/8398cf96f2db-1e937795ce9e
```

基线工具单元测试 26 项通过。两组均为 `measured`，`verify` 验证冻结输入、完整学习序列、用户目录和全部 9 项证据的身份/摘要通过。

| 阶段 | 结果 | 本次进程总耗时 |
| --- | --- | --- |
| cold/measure | 原始资源部署成功，8 个用例均有候选 | 945.2 秒 |
| learned/train | 独立空 userdb，按文字定位候选，“你”“你好”各提交 5 次，10 次内容均正确 | 31.8 秒 |
| learned/measure | 学习进程 finalize 后另起进程，8 个用例均有候选 | 15.2 秒 |

上述为转译环境下各阶段的进程总耗时，含初始化/部署/退出，不是按键延迟或 Android 应用启动时间。首次完整系统词典编译耗时很长；本轮主机可用内存采样约 6 GB，未通过增加模拟器 RAM 加速。原始阶段日志为 `execution.log`，模拟器已关闭。

| 原始结果 | SHA-256 |
| --- | --- |
| `cold/measure.json` | `3ccb254656dc380c3f336068a1d519e9022b4ed9634845d6ab0d75843a815334` |
| `learned/train.json` | `347bff49ae9e81cae7ea07cd60a18ba2502f0d9f0e182cf34f0be417df149169` |
| `learned/measure.json` | `12ad367ce8c7d33fe264ce28ea30ee844f46e1668fcf59faae2f076fbf7a9633` |

每项结果都有 `.identity.json`；运行时方案、词典、OpenCC 和 userdb 的文件摘要在各组的 `runtime-files.sha256.json`。`android-failure-events.txt` 仅记录本次开机的 System UI 事件，独立引擎测试未经过 Trime Android 服务，不能据此宣称应用没有 ANR。

## 对照版本

| 冻结版本 | run |
| --- | --- |
| 先前冻结的 Release `v3.3.11-t9.4`，包内 versionName 为 `3.3.11` | `93ae0dd43044-6a56d8f8e275` |
| 合并后 develop `d736cd2c`，包内 versionName 为 `3.3.13` | `206eec348ccb-1b6bb05485fd` |

两个旧 run 的完整性验证通过。本轮没有重新查询或变更 GitHub latest，Release 标签不替代包内版本号或 APK 摘要。
三个 run 的语料、主词典、词频语料、测试驱动二进制、Android 镜像指纹及执行 ABI 相同；schema/native 库随版本变化。
这里只比较相同学习状态下的候选，不以不同构建的单次转译耗时推断手机性能优劣。

## 候选比较

分别比较 cold 和 learned：每组 8 个用例的前 30 个候选文字、注释、顺序、原始输入、预编辑和提交预览，与两份对照版本完全一致。比较时仅排除 `engine_query_ms`，不忽略候选顺序或合并同音字；机器可读结果为 `candidate-comparison.json`。
本轮 cold 与 learned 的这些字段也一致；两个学习目标原本就是首选，因此完成学习不代表首选必然变化，不能把排序不变判断为学习失败。

以下展示每个输入的前 3 项；三个版本的两组结果相同：

| 输入 | 前 3 个汉字候选 |
| --- | --- |
| `64` | 你 / 米 / 迷 |
| `64426` | 你好 / 妳好 / 你敢 |
| `9426` | 小 / 先 / 找 |
| `94'26` | 一波 / 西安 / 议案 |
| `68` | 偶 / 女 / 木 |
| `58` | 路 / 据 / 哭 |
| `683` | 虐 / 疟 / 母鹅 |
| `583` | 觉 / 绝 / 略 |

结论：本轮补齐了最终 `t9.2` APK 的两组独立引擎基线，在这些用例中没有候选回归；**没有证明候选排序优于旧版或 Google 拼音**。
本驱动输入原始数字，不执行拼音锁定按钮。逐段消歧、删除、撤销、多音字和非九键方案的控制器/JNI/UI 验收，仍以 [T01 测试说明](README.md) 和 [启动及界面复测](STARTUP-ANR.md) 为准。
真机触摸延迟、长时间快速连打、字体缩放、第三方主题及 release 签名构建仍未验收。本轮不修改 GitHub Release/latest，也不把这次候选基线当作稳定版发布许可。
