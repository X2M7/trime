<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Trime

Rime IME for Android

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=feature/t9-pinyin)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

English | [简体中文](README_sc.md) | [繁體中文](README_tc.md)

## About

Trime is originally a frontend of open-source [Android Traditional Chinese IME], based on [RIME] input method framework and written in Java/Kotlin with JNI. It is designed to protect the native language of various local dialects of Chinese and is a universal shape-based and phonetic-based input method platform.

[Documentation](https://github.com/osfans/trime/wiki)

## T9 Pinyin Preview

This fork adds `Luna Pinyin T9` / `luna_pinyin_t9` on top of Trime:

- Adds a T9 pinyin schema based on the Luna Pinyin dictionary, using digits `2-9` for pinyin input.
- Adds a 3x3 main key area with side controls in both bundled themes, with `ABC=2`, `DEF=3`, `GHI=4`, `JKL=5`, `MNO=6`, `PQRS=7`, `TUV=8`, and `WXYZ=9`.
- Automatically switches to the T9 keyboard when the `luna_pinyin_t9` schema is selected.
- Improves early theme loading during IME startup to avoid first-run theme deployment crashes.

[Project page](https://x2m7.github.io/trime/) | [Download the T9 preview APK](https://github.com/X2M7/trime/releases/latest)

New prerelease: [t9.7-dev.2](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.7-dev.2), versionCode `20261111`, adds T04/T05 and runtime safety fixes. Known ARM-translation startup/slow-frame warnings remain; see [release notes](doc/releases/v3.3.13-t9.7-dev.2.md). The previous t9.6 remains Latest.

Preview `3.3.13-t9.6` (versionCode `20261108`) includes dictionary-backed pinyin selection, syllable locking, middle-segment editing, unlocking and undo. Selecting pinyin does not commit Hanzi. T03 adds an adaptive sidebar/horizontal pinyin list, left/right one-handed modes, adjustable key height, literal digits on long press and Space cursor sliding. See the [key contracts](script/quality/T03.md) and [development validation](script/quality/VALIDATION-t9.6-T03.md).

This version integrates upstream through `d9a1f424`, including scoped theme colors, in-place color refresh and installation-aware data sync. T9 colors now refresh with the keyboard, and sync backups are protected when the installation identity is unavailable. See the [merge validation record](script/t9/UPSTREAM-t9.3.zh-CN.md) for scope and test results.

The ARM64 preview retains Android 5.0+ support and the previous preview's package (`com.osfans.trime.debug`) and debug signing certificate for in-place upgrades. Its displayed name has no Debug suffix. This is a preview, not a production-signed/R8 build. Back up configuration and dictionaries before upgrading. Custom copies of the T9 schema or themes need the updated rules/layout and redeployment; do not reset personal dictionaries. See [release notes](doc/releases/v3.3.13-t9.6.md), [T9 design and tests](script/t9/README.md) and [startup ANR checks](script/t9/STARTUP-ANR.md).

T04 in the new prerelease adds a [fixed Chinese ranking benchmark](script/quality/t04/README.md), pinned dictionary-layer experiments, user-learning checks and APK resource checksum verification. Experiments use isolated data directories and do not overwrite personal dictionaries. No large dictionary or grammar model is enabled by default.
The [first evaluation](script/quality/VALIDATION-T04.md) found improvements and regressions, but no net holdout Top-1 gain, so Luna remains the default.

Prerelease [T05](script/quality/t05/README.md) separates six optional fuzzy-pinyin rules from three bounded numeric-key repair rules. All are off by default. Source-labeled pinyin suggestions preserve the original Hanzi ranking until explicitly selected; raw digits, unlocking and undo remain available. This does not replace the previous Latest release.

## Download

- T9 Preview [Download](https://github.com/X2M7/trime/releases/latest)

- Stable Channel <br>
  [<img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='80px'/>](https://f-droid.org/packages/com.osfans.trime)
  [<img alt='Google Play Download Now' src='https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png' height='80px'/>](https://play.google.com/store/apps/details?id=com.osfans.trime)

- Nightly Channel [Download](https://github.com/osfans/trime/releases/tag/nightly)

- Canary Channel [Download](https://github.com/osfans/trime/actions)

- Configurations [rimerc](https://github.com/Bambooin/rimerc)

## History

TRIME is the abbreviation of _Tongwen RIME_ or _ThaeRv Input Method_.

From the beginning, TRIME was written for TaeRv Pinyin, and named _TaeRv Input Method (泰如输入法)_.

Then, we created an input method platform with some code tables, such as Wu dialect (吴语). We renamed it to _Chinese Character Dialect Input Method (汉字方言输入法)_.

Later, it supports Wubi and Liangbi and other shape-based input method, we branded it [_Tongwen Input Method Platform 2.0 (同文输入法平台 2.0)_](https://github.com/osfans/trime-legacy), which implies that the phonetic-based and shape-based input method on one platform, while dialects and Mandrain share one kind of characters.

Benefit from the [librime](https://github.com/rime/librime) project by JNI, we are now in version 3.0 of TRIME aka _Tongwen Input Method (同文输入法)_.

Your are now welcome to [contribution](CONTRIBUTING.md) ~ !

## Getting Started for developer

### Prepare

#### Requirements:

- Android SDK and Android NDK
  * If you are new to Android development, please install [Android Studio](https://developer.android.com/studio).

- JDK (OpenJDK) 17
- Python 3 (required by OpenCC to generate dictionary text files)

#### Prerequisites for Windows

Symbolic links will be created according to current build configurations, developers need:

- Enable [Developer Mode](https://learn.microsoft.com/en-us/windows/apps/get-started/enable-your-device-for-development) so that symlinks can be created without administrator privilege.

- Enable symlink support for `git`:

  ```powershell
  git config --global core.symlinks true
  ```

If you cannot or wouldn't like to enable anything, it doesn't matter. Copying will be used instead when error on creating symbolic links.

### Build

#### 1. Clone this project and fetch all submodules:

```sh
git clone git@github.com:osfans/trime.git
git submodule update --init --recursive
# use partial clone to save time
git submodule update --init --recursive --filter=blob:none
```

#### 2. Debug version without signature:

```sh
# On Linux or macOS
make debug

# On Windows
.\gradlew assembleDebug
```

#### 3. Release version with signture:

Create `keystore.properties` file which contains following contents for [signing information](https://developer.android.com/studio/publish/app-signing.html):

```gradle.properties
storePassword=myStorePassword
keyPassword=mykeyPassword
keyAlias=myKeyAlias
storeFile=myStoreFileLocation
```

Then, you may run:

```sh
# On Linux or macOS
make release

# On Windows
.\gradlew assembleRelease
```

### Troubleshooting

```
Target "boost_log_setup" links to target "Boost::coroutine" but the target was not found.
```

Run `make clean` on Linux or macOS, or run `.\gradlew clean` on Windows.

Other issues:

1. Try `make clean`
2. Make sure your repo is up-to-date. If one or more submodules are modified, also make sure they are compatible with the current version.
3. If the problem still exists(very unlikely), try to make a new clone.
4. Check if this is there is an issue/PR related to your problem. If yes, try their solutions.
5. If none of them works, you may make an issue to ask for help.(optional)

## Acknowledgments

- Developer: [osfans](https://github.com/osfans)
- Contributors: [boboIqiqi](https://github.com/boboIqiqi)、[Bambooin](https://github.com/Bambooin)、[senchi96](https://github.com/senchi96)、[heiher](https://github.com/heiher)、[abay](https://github.com/a342191555)、[iovxw](https://github.com/iovxw)、[huyz-git](https://github.com/huyz-git)、[tumuyan](https://github.com/tumuyan)、[WhiredPlanck](https://github.com/WhiredPlanck)、[nopdan](https://github.com/nopdan)...
- [Wiki Editors](https://github.com/osfans/trime/wiki): [xiaoqun2016](https://github.com/xiaoqun2016)、[boboIqiqi](https://github.com/boboIqiqi)...
- Translators: 天真可爱的满满 (Chinese Traditional), 点解 (English) ...
- Keyboard Designers: 天真可爱的满满、皛筱晓小笨鱼、吴琛 11、熊猫阿 Bo、默默ㄇㄛ ˋ...
- Donations: See QR Code in [Releases](https://github.com/osfans/trime/releases)
- Community: Netizens who feedback in [Issues](https://github.com/osfans/trime/issues)、[QQ Group (811142286)](https://jq.qq.com/?_wv=1027&k=AXdR80HN)、[QQ Group (224230445)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=pg_q7UVumWYLq1Rk8kIAqkK1xGt64VnX&authKey=04m9l7OBO5H5vgrEL8IbpsmtnptWM60xy%2FUwYCfyvw9VcRhe8zRzAS1ezoemZdFr&noverify=0&group_code=224230445)、[Tieba](http://tieba.baidu.com/f?kw=rime)、[Google Play](https://play.google.com/store/apps/details?id=com.osfans.trime)、[Telegram](https://t.me/trime_dev)...
- Projects: [RIME]、[OpenCC]、[Android Traditional Chinese IME] and so on.

## Third Party Libraries

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
- [Android Traditional Chinese IME](https://code.google.com/p/android-traditional-chinese-ime/) (Apache License 2.0)

[Android Traditional Chinese IME]: https://code.google.com/p/android-traditional-chinese-ime/
[RIME]: http://rime.im
[OpenCC]: https://github.com/BYVoid/OpenCC
