<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# 同文 Android 输入法平台

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

[English](README.md) | 简体中文 | [繁體中文](README_tc.md)

## v3.3.13-t9.17 — 版本与覆盖升级一致性

应用版本、APK 名称、发布工具与 Pages 统一读取版本元数据。`versionCode 20261122` 高于全部 26 个已核对历史 APK 的最高值 `20261121`。继续沿用历史包名 `com.osfans.trime.debug` 和原证书，正式 APK 实际为不可调试的 Release 构建。升级前把配置与词库备份复制到独立位置，再覆盖安装，不卸载、不清除数据。

本版构建的 812 项应用 JVM 测试和 268 项宿主检查新执行通过，34 项构建逻辑测试为缓存结果，双 Lint 均为 0 问题。

Android 5.0 与 Android 15 x86_64 模拟器从 v3.3.13-t9.16 正式 APK 覆盖升级到本版，各通过全部九项检查，覆盖配置、偏好、UID、已选及已启用输入法和真实原生用户词库保留。检查使用专用测试环境与内部空编辑器，不代表公开界面输入或跨包名迁移验收。

范围与线上状态见 [T14 验证报告](script/quality/VALIDATION-T14.md)、[升级说明](script/release/README.md)和[版本 Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.17)。计划附件仅两个正式 APK 与完整递归源码。实物 ARM64 手机、OEM、16 KiB 页、可听 TTS、P95／PSS 与长期驻留仍未验收，运行警告、先前失败尝试及独立辅助程序的未决警告继续保留。

下方 v3.3.13-t9.16 内容保留为该版本的历史证据。

## 历史 v3.3.13-t9.16 — 无障碍与设备适配

按键、候选、九键拼音筛选和预编辑控件补齐可访问名称、角色、选中状态与操作。动态回车键名称随发送、搜索等动作更新；候选刷新后的旧操作会被输入身份检查拦截。改善大字体、横屏与窗口边界适配，短屏可通过垂直滚动访问内容。

v3.3.13-t9.16 构建的 812 项应用 JVM 测试与 227 项宿主检查新执行通过。独立 Debug／Release Lint 0 问题，构建和双 ABI 正式 APK 签名检查无警告。384 项原生检查按 31 项一致输入绑定复用历史结果，本轮未重跑；生产 JNI 任务为 UP-TO-DATE，不声称全部重新编译。

已测模拟器范围包括 Android 5.0 四组配置、Android 15 八组配置及额外一组 Steam 深色配色检查。夜间模式矩阵仍用浅色配色，深色像素依据来自独立 Steam 原图。Android 15 正式 APK 上，实际 TalkBack FOSS 探索与双击完成九键输入、筛拼音、选词和发送。常规与两倍字体、上下两个分屏位置的 TalkBack 输入均通过；整轮运行警告的分类与限制见验证报告。

本历史记录冻结于该版本发布前，线上状态以[版本 Release 页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.16)为准。系统／模拟器运行警告及历史失败全部保留，不表示所有警告或风险已经消除。[9.16 验证范围](script/quality/VALIDATION-t9.16.md) · [历史 9.15 验证](script/quality/VALIDATION-t9.15.md)。

保留 Android 5.0+、ARM64／x86_64 构建支持。实物手机、OEM、16 KiB 页、真实语音／TTS、候选 P95／PSS 与长期驻留仍未验收。v3.3.13-t9.16 当时计划的手动附件仅两个正式 APK 与包含主仓库及全部 21 个递归子模块的完整源码包；GitHub 自动 source zip／tar 保留。

## 关于

源于开源的[注音仓颉输入法]前端，基于著名的 [RIME] 输入法框架，使用 JNI 的 C 语言和 Android 的 Java/Kotlin 语言书写，旨在保护汉语各地方言母语，音码、形码通用的输入法平台。

[查看文档](https://github.com/osfans/trime/wiki)

## 九键拼音

这个 fork 在 Trime 中加入基于方案词典的 `朙月拼音九键` / `luna_pinyin_t9`，两个内置主题均提供 3x3 主键区。使用 2–9 输入拼音，支持逐段锁定、中间编辑、解锁与撤销；选择拼音不提交汉字。布局支持拼音侧栏或横排、左右单手模式、键高设置、长按数字和空格滑动移光标。

[项目页面](https://x2m7.github.io/trime/) | [下载九键稳定版 APK](https://github.com/X2M7/trime/releases/latest)

## 下载

- 九键稳定版 [点击下载](https://github.com/X2M7/trime/releases/latest)

- 上游稳定版 <br>
  [<img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='80px'/>](https://f-droid.org/packages/com.osfans.trime)
  [<img alt='Google Play 立即下载' src='https://play.google.com/intl/en_us/badges/images/generic/zh-cn_badge_web_generic.png' height='80px'/>](https://play.google.com/store/apps/details?id=com.osfans.trime)

- 每夜版 [点击下载](https://github.com/osfans/trime/releases/tag/nightly)

- 测试版 [点击下载](https://github.com/osfans/trime/actions)

- 配置文档 [rimerc](https://github.com/Bambooin/rimerc)

## 沿革

TRIME 是 Tongwen RIME 或是 ThaeRvInputMEthod 的缩写:

- 最初，输入法是写给[泰如拼音](http://taerv.nguyoeh.com/ime/)（tae5 rv2）的，中文名为“泰如输入法”;
- 然后，添加了吴语等方言码表，做成了一个输入法平台，更名为“汉字方言输入法”;
- 后来，兼容了五笔、两笔等形码，在太空卫士、征羽的建议下，更名为“[同文输入法平台 2.x](https://github.com/osfans/trime-legacy)”。寓意音码形码同台，方言官话同文。
- 之后，借助 JNI 技术，享受了 [librime](https://github.com/rime/librime) 的成果，升级为“同文输入法平台 3.x”，简称“同文输入法”。

现在欢迎你前来[贡献](CONTRIBUTING.md) ～！

## 开发入门

### 准备

#### 开发环境要求

- Android SDK 和 Android NDK
  - 如果还不熟悉 Android 开发，建议安装 [Android Studio](https://developer.android.google.cn/studio)，它会自动安装并配置 Android 开发环境。

- JDK（OpenJDK）17
- Python 3 (用于给 OpenCC 生成词典文本文件）

#### Windows 上的前提条件

当前构建配置会使构建过程中创建符号链接，开发者需要：

- 启用[开发者模式](https://learn.microsoft.com/zh-cn/windows/apps/get-started/enable-your-device-for-development) 以在无管理员权限的情况下创建符号链接。

- 启用 `git` 的符号链接支持：

  ```powershell
  git config --global core.symlinks true
  ```

如果无法或者不想启用上述设置也没关系。构建系统会自动在符号链接创建失败时使用复制代替。

### 构建

#### 1. 克隆此项目并拉取所有子模块。

```sh
git clone git@github.com:X2M7/trime.git
cd trime
git submodule update --init --recursive
```

#### 2. 编译调试版本:

```sh
# On Linux or macOS
make debug

# On Windows
.\gradlew assembleDebug
```

#### 3. 编译正式版本：

请创建 `keystore.properties` 文件，包含以下内容，注明[签名信息](https://developer.android.com/studio/publish/app-signing.html)：

```gradle.properties
storePassword=myStorePassword
keyPassword=mykeyPassword
keyAlias=myKeyAlias
storeFile=myStoreFileLocation
```

然后执行：

```sh
# On Linux or macOS
make release

# On Windows
.\gradlew assembleRelease
```

### 故障排除

```
Target "boost_log_setup" links to target "Boost::coroutine" but the target was not found.
```

在 Linux 或 macOS 上执行 `make clean`，Windows 上执行 `.\gradlew clean`。

其他问题:

1. 首先尝试 `make clean`
2. 确保你的仓库与最新版本一致。如果你修改了一个或更多的子模块，请确保它们与当前仓库版本兼容。
3. 如果问题依然存在（不太可能）, 尝试进行一次新的克隆。
4. 检查是否有 PR/issue 与你的问题相关。 如果有的话，尝试他们的解决方案。
5. 如果以上方法都不工作，你可以提一个 issue 来寻求帮助(可选)。

## 鸣谢

- 开发：[osfans](https://github.com/osfans)
- 贡献：[boboIqiqi](https://github.com/boboIqiqi)、[Bambooin](https://github.com/Bambooin)、[senchi96](https://github.com/senchi96)、[heiher](https://github.com/heiher)、[abay](https://github.com/a342191555)、[iovxw](https://github.com/iovxw)、[huyz-git](https://github.com/huyz-git)、[tumuyan](https://github.com/tumuyan)、[WhiredPlanck](https://github.com/WhiredPlanck)、[nopdan](https://github.com/nopdan)......
- [维基](https://github.com/osfans/trime/wiki)：[xiaoqun2016](https://github.com/xiaoqun2016)、[boboIqiqi](https://github.com/boboIqiqi)......
- 翻译：天真可爱的满满（繁体中文）、点解（英文）......
- 键盘：天真可爱的满满、皛筱晓小笨鱼、吴琛 11、熊猫阿 Bo、默默ㄇㄛ ˋ......
- 捐赠：[Releases](https://github.com/osfans/trime/releases) 中的“打赏”实时更新
- 社区：在 [Issues](https://github.com/osfans/trime/issues)、[QQ 群 (811142286)](https://jq.qq.com/?_wv=1027&k=AXdR80HN)、[QQ 群 (224230445)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=pg_q7UVumWYLq1Rk8kIAqkK1xGt64VnX&authKey=04m9l7OBO5H5vgrEL8IbpsmtnptWM60xy%2FUwYCfyvw9VcRhe8zRzAS1ezoemZdFr&noverify=0&group_code=224230445)、[Google Play](https://play.google.com/store/apps/details?id=com.osfans.trime)、[贴吧](http://tieba.baidu.com/f?kw=rime)、[Telegram](https://t.me/trime_dev) 中反馈意见的网友
- 项目：[RIME]、[OpenCC]、[注音仓颉输入法]等开源项目

## 第三方库

- [Boost C++ Libraries](https://www.boost.org/) (Boost Software License)
- [darts-clone](https://github.com/s-yata/darts-clone) (New BSD License)
- [LevelDB](https://github.com/google/leveldb) (New BSD License)
- [libiconv](https://www.gnu.org/software/libiconv/) (LGPL License)
- [marisa-trie](https://github.com/s-yata/marisa-trie) (BSD License)
- [glog](https://github.com/google/glog) (New BSD License)
- [OpenCC](https://github.com/BYVoid/OpenCC) (Apache License 2.0)
- [RIME](https://rime.im) (BSD License)
- [snappy](https://github.com/google/snappy)(BSD License)
- [utfcpp](https://github.com/nemtrif/utfcpp) (Boost Software License)
- [yaml-cpp](https://github.com/jbeder/yaml-cpp) (MIT License)
- [注音仓颉输入法](https://code.google.com/p/android-traditional-chinese-ime/) (Apache License 2.0)

[注音仓颉输入法]: https://code.google.com/p/android-traditional-chinese-ime/
[RIME]: http://rime.im
[OpenCC]: https://github.com/BYVoid/OpenCC
