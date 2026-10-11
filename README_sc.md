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

## v3.3.13-t9.13 — 本轮稳定版验收通过

支持 Android 5.0+，提供 ARM64 和 x86_64 Release APK。本轮稳定版验收通过，线上状态以[版本 Release 页面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.13)为准。

默认全拼与九键共享 `luna_pinyin` 学习库及 `custom_phrase.txt`。个人词库可导入、导出和备份恢复；常用短语可编辑、导入导出，修改前自动备份供恢复。Rime portable 快照导入合并记录，精确文件备份恢复替换受保护文件。置顶按当前方案及原始输入生效，恢复排序保留学习；忘记此词请求移除已学习记录，未学习词可能提示无法忘记；不是永久屏蔽系统词，也不删除置顶规则。操作失败时显示输入法提示框。候选注释、可选横向滚动和全文阅读方便查看长候选，并拒绝刷新后的旧手势。支持 Shift 双击锁定并保留自定义主题动作。

置顶最多 512 条规则，仅在前 144 候选内查找，隐私模式不应用或修改置顶；字母和数字短语编码不自动互转。展开分页每页 48 项、预取距离 16、`maxSize` 目标 144 为 best-effort 预算，不是普遍硬内存上限。

本轮 702 项新执行应用 JVM 测试、213 项新执行宿主测试通过；17 项 build-logic＋6 项 compatibility 为缓存结果，单列统计。Debug／Release Lint、构建及 Release 签名警告均为 0。Android 5.0 和 Android 15 模拟器完成正式版覆盖升级、候选和常用短语界面验收及单独 Debug 两主题回归，包含快速输入40轮与键盘显示隐藏100次。两轮4,463／11,620条原始 W/E/F 和完整控制台诊断均保留并逐行审阅；这不表示平台所有诊断已消失或全部环境风险已排除。

真机 P95/PSS、长期驻留、首次部署、物理 ARM64／OEM／16KiB 页、真实 TTS 及 SAF 选择器权限／取消未测。[验收范围与历史](script/quality/VALIDATION-t9.13.md)。手动附件仅两个 APK 与含递归子模块的完整源码包；发布状态及附件完整性以 Release 页面和归档外发布记录为准。

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
