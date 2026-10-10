<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# Trime

Rime IME for Android

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

English | [简体中文](README_sc.md) | [繁體中文](README_tc.md)

## v3.3.13-t9.12

Stable release for Android 5.0+; ARM64 and x86_64 APKs.

This version adds an explicit QWERTY English route for T9, remembered manual language choices and temporary editor-forced modes. Number/symbol switching retains pending composition. It fixes preedit/candidate touch regions, own-scroll coordinates, repeated window attachment and lost cached views, combined modifier and SYM state, incomplete SAF listings and log-export threading/cancellation, and integrates upstream develop. Font directories are reused within each theme instance; fixed-size bars skip hidden-child measurement while flexible layouts retain their sizing behavior.

Validation: 663 application JVM tests and 206 host checks passed; both Lint runs reported zero issues and APK signature checks reported zero warnings; Android 5.0 and Android 15 emulator regressions passed with Debug and normal Release builds. Raw framework/graphics W/E/F remain retained: zero build or signature warnings does not mean a warning-free device. Physical P95/PSS, ARM64/OEM runtime and real enabled-speech engine teardown remain unverified.

[Stable release downloads](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.12) · [Validation and history](script/quality/VALIDATION-t9.12.md). Manual assets are two Release APKs and a complete recursive-submodule source archive.

## About

Trime is originally a frontend of open-source [Android Traditional Chinese IME], based on [RIME] input method framework and written in Java/Kotlin with JNI. It is designed to protect the native language of various local dialects of Chinese and is a universal shape-based and phonetic-based input method platform.

[Documentation](https://github.com/osfans/trime/wiki)

## T9 Pinyin

This fork adds dictionary-backed `Luna Pinyin T9` / `luna_pinyin_t9` to Trime, with a 3x3 main key area in both bundled themes. Digits 2–9 enter pinyin; syllable locking, middle editing, unlocking and undo preserve the original numeric input. Selecting pinyin does not commit Hanzi. The layout supports a pinyin sidebar or horizontal list, one-handed modes, adjustable key height, literal digits on long press and Space cursor sliding.

[Project page](https://x2m7.github.io/trime/) | [Download T9 stable APKs](https://github.com/X2M7/trime/releases/latest)

## Download

- T9 Stable [Download](https://github.com/X2M7/trime/releases/latest)

- Upstream Stable Channel <br>
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
git clone git@github.com:X2M7/trime.git
cd trime
git submodule update --init --recursive
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
