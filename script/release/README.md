# 发布与升级

本 fork 的正式安装身份是 `com.osfans.trime.debug`。名字源于早期包名；正式 APK 必须是 `release`、`debuggable=false`，不能靠更改界面文字伪装。2026-10-15 实物核对了 `X2M7/trime` 当前全部 15 个 Release 的 26 个 APK：均沿用证书 SHA-256 `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`。最高已发布 versionCode 为 `20261121`，新版本从 `20261122` 递增，与日期无关。证书 DN 是 Android Debug；本轮为保留现有用户的覆盖升级路径继续沿用该签名，并未声称换成了其他签名。

## 唯一版本来源

根目录 `release.properties` 定义展示版本、versionCode、安装包名、发布仓库、分支、签名证书和 ABI。Gradle、APK 检查、Release 名称和 Pages 都读取它。Debug 的 Manifest/展示版本追加 `-debug`。环境变量可以确认版本，不能覆盖它；Git SHA 独立记录。

```sh
python3 script/release/release.py pages
python3 script/release/release.py metadata --check-pages
python3 -m unittest discover -s script/release -p 'test_*.py'
```

Pages 是静态生成文件；模板、元数据或生成结果不一致会使 CI 失败。源码导航指向 `develop`；具体版本源码使用对应 tag。页面只描述目标版本，线上发布状态以对应 Release 和 Latest 为准。

## 已有用户覆盖安装

1. 保留旧输入法，先在用户词典页面对各个词典执行导出或备份。备份按钮首先写入应用数据目录的 `sync/<installation_id>`，还需通过浏览应用数据目录把备份复制到独立共享目录；点击备份并不代表文件已离开应用数据区。
2. 手机优先下载 `arm64-v8a`；x86_64 为对应设备／模拟器使用。32 位设备不应安装这两种包，需继续使用匹配的旧包或等待单独验证的兼容包。
3. 同包名、同证书、较大 versionCode 的新 Release 直接覆盖安装。不卸载、不清除数据、不使用降级安装参数。
4. 更新后确认原输入法仍启用／选中、方案和自定义配置仍在，并试用已学习短语。若系统拒绝更新，先核对安装身份和签名；不要通过卸载绕过签名不匹配。

本轮包名和签名均不改变。早期 Debug APK 也必须按实际 Manifest 判断身份，文件名不能证明它是正式构建。高于新包的其他来源 versionCode 不在本轮覆盖升级承诺内。

## 不同包名或签名的迁移

`com.osfans.trime.debug` 与 `com.osfans.trime` 是两个应用，不能称原地升级。保留旧应用，先从旧应用用户词典页面逐个导出文本；对于 Rime 同步快照，可保留 `sync/<installation_id>/*.userdb.txt`，在目标应用的用户词典页面通过合并快照入口导入。常用短语通过该页的短语导出／导入操作单独迁移。

配置通过旧应用的数据／同步目录保存 `.custom.yaml`、自定义方案、主题和引用资源，复制到独立保留的目录；目标应用重新选择该目录，查看导入校验结果，成功后再激活。不要把正在使用的 LevelDB 目录当作可跨包直接复制的文本备份。目标应用需要自己的目录授权、输入法启用和选择。

共享目录不包含全部 AppPrefs；字号、高度、触感、隐私等偏好需在新应用重新设置。候选置顶记录和应用私有恢复备份也不在 YAML／文本词库同步的自动迁移范围内。目录授权不随复制文件迁移。先在新应用检查词条、配置和输入结果，旧应用及备份保留至确认完成；卸载本身不保留词库。本轮保持原身份，因此不宣称已实测所有跨包名迁移组合。

## 构建记录与发布门槛

`build.py` 串行运行 Debug／Release／独立 Lint，单 worker、896 MiB 堆、640 MiB Metaspace；≥4 GiB 可用内存才启动，低于 1 GiB 停止。它保存实际命令、日志、Git/submodule SHA、生成的 BuildConfig、冻结 APK 和内存／退出记录。正式签名沿用既有证书必须显式启用 `trime.forkReleaseIdentity=true`；其他签名会被发布验签拒绝。

`release.py record` 绑定真实 Release 构建结果，检查 Manifest、单一 native ABI/ELF machine、v1/v2 签名、证书和 ZIP 对齐，并记录全部 APK 数据资源摘要、词典版本／摘要、主仓库及递归子模块提交。静态对齐检查不等于物理设备页大小验收。

`stage.py` 需要同一源码的构建、单元测试、宿主检查、双 Lint、Android 5.0／15 实际升级报告及原始已发布 APK 身份审计。它只打包两份 Release APK 和完整离线源码。`publish.py plan` 不读取凭据；`publish` 只针对 `X2M7/trime`，先创建草稿，再下载校验全部附件，最后发布稳定版 Latest，并再次下载比对。实验版本不能走此稳定发布路径，必须标为 prerelease 且不替换 Latest。CI 不自动发布或移动标签。已有标签和产物不会被覆盖。

## 完整源码

GitHub 自动 Source code zip/tar 不包含递归子模块工作树。在线获取请使用：

```sh
git clone --recurse-submodules https://github.com/X2M7/trime.git
cd trime
git checkout <release-tag>
git submodule update --init --recursive
```

`source-with-submodules.tar.gz` 包含主仓库及递归子模块的固定 Git 文件、`BUILD-MANIFEST.json` 和逐文件 `SOURCE-FILES.json`。从 Git blob 直接打包，保留已提交符号链接和可执行位，忽略规则不会删减源码。构建提交与之后只改发布文档的 tag 提交分别记录；离线源码并不包含 Android SDK、JDK 或依赖下载缓存。
