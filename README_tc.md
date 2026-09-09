<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# 同文 Android 輸入法平臺

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=feature/t9-pinyin)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

[English](README.md) | [简体中文](README_sc.md) | 繁體中文

## 關於

源於開源的[注音倉頡輸入法]前端，基於著名的 [RIME] 輸入法框架，使用 JNI 的 C 語言和 Android 的 Java/Kotlin 語言書寫，旨在保護漢語各地方言母語，音碼、形碼通用的輸入法平臺。

[查看文檔](https://github.com/osfans/trime/wiki)

## 九鍵拼音

這個 fork 在 Trime 中加入基於方案詞典的 `朙月拼音九鍵` / `luna_pinyin_t9`，兩個內建主題均提供 3x3 主鍵區。使用 2–9 輸入拼音，支援逐段鎖定、中間編輯、解鎖與復原；選擇拼音不提交漢字。佈局支援拼音側欄或橫排、左右單手模式、鍵高設定、長按數字及空格滑動移游標。

[專案頁面](https://x2m7.github.io/trime/) | [下載九鍵穩定版 APK](https://github.com/X2M7/trime/releases/latest)

穩定版 [3.3.13-t9.8](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.8)，versionCode `20261113`，包含 T06 輸入框適配、統一 Enter 動作、返回聊天框恢復九鍵、編輯器連線隔離、第三方主題回退及部署重試，並修復 Android 5.0 預編輯浮層當機。具體 APK 與驗證範圍見[發行說明](doc/releases/v3.3.13-t9.8.md)和 [T06 驗證報告](script/quality/VALIDATION-T06.md)。

ARM64 與 x86_64 APK 保留 Android 5.0+ 支援，延續此前 fork 的套件名稱 `com.osfans.trime.debug` 和簽章憑證，以支援覆蓋升級；顯示名稱為 `Trime`。附件仍是除錯簽章、可除錯建置，未啟用正式簽章或 R8。升級前備份設定與詞庫；自訂目錄中的同名方案或主題可能需要合併新規則、佈局並重新部署，請保留個人詞庫。

[T04](script/quality/t04/README.md) 在獨立目錄中提供固定排名與學習測試，預設繼續使用朙月詞典，不啟用大詞庫或語法模型。[T05](script/quality/t05/README.md) 將六組可選模糊音與三類有界數字鍵修正分開設定，預設全部關閉，建議標明來源。

仍保留三項 v1 META-INF 簽章覆蓋警告。模擬器驗收不代表 ARM64 實機效能、所有 OEM 編輯器或物理 16 KB 頁裝置已驗證；此前 ARM 轉譯啟動排隊與慢幀記錄見發行說明。另見[鍵位規則](script/quality/T03.md)、[九鍵設計](script/t9/README.md)和[上游合併記錄](script/t9/UPSTREAM-t9.3.zh-CN.md)。

## 下載

- 九鍵穩定版 [點選下載](https://github.com/X2M7/trime/releases/latest)

- 上游穩定版 <br>
  [<img alt='Get it on F-Droid' src='https://fdroid.gitlab.io/artwork/badge/get-it-on.png' height='80px'/>](https://f-droid.org/packages/com.osfans.trime)
  [<img alt='Google Play 立即下載' src='https://play.google.com/intl/en_us/badges/images/generic/zh-tw_badge_web_generic.png' height='80px'/>](https://play.google.com/store/apps/details?id=com.osfans.trime)

- 每夜版 [點選下載](https://github.com/osfans/trime/releases/tag/nightly)

- 測試版 [點選下載](https://github.com/osfans/trime/actions)

- 配置文件 [rimerc](https://github.com/Bambooin/rimerc)

## 沿革

TRIME 是 Tongwen RIME 或是 ThaeRvInputMEthod 的縮寫:

- 最初，輸入法是寫給[泰如拼音](http://taerv.nguyoeh.com/ime/)（tae5 rv2）的，中文名為“泰如輸入法”;
- 然後，添加了吳語等方言碼錶，做成了一個輸入法平臺，更名為“漢字方言輸入法”;
- 後來，相容了五筆、兩筆等形碼，在太空衛士、徵羽的建議下，更名為“[同文輸入法平臺 2.x](https://github.com/osfans/trime-legacy)”。寓意音碼形碼同臺，方言官話同文。
- 之後，藉助 JNI 技術，享受了 [librime](https://github.com/rime/librime) 的成果，升級為“同文輸入法平臺 3.x”，簡稱“同文輸入法”。

現在歡迎你前來[貢獻](CONTRIBUTING.md) ～！

## 開發入門

### 準備

#### 開發環境要求

- Android SDK 和 Android NDK
  - 如果還不熟悉 Android 開發，建議安裝 [Android Studio](https://developer.android.com/studio)，它會自動安裝並配置 Android 開發環境。

- JDK（OpenJDK）17
- Python 3 (用於給 OpenCC 生成詞典文字檔案)

#### Windows 上的前提條件

當前構建配置會使構建過程中建立符號連結，開發者需要：

- 啟用[開發者模式](https://learn.microsoft.com/zh-cn/windows/apps/get-started/enable-your-device-for-development) 以在無管理員許可權的情況下建立符號連結。

- 啟用 `git` 的符號連結支援：

  ```powershell
  git config --global core.symlinks true
  ```

如果無法或者不想啟用上述設定也沒關係。構建系統會自動在符號連結建立失敗時使用複製代替。

### 構建

#### 1. 克隆此專案並拉取所有子模組。

```sh
git clone git@github.com:osfans/trime.git
git submodule update --init --recursive
# 可以使用部分克隆節省時間
git submodule update --init --recursive --filter=blob:none
```

#### 2. 編譯除錯版本:

```sh
# On Linux or macOS
make debug

# On Windows
.\gradlew assembleDebug
```

#### 3. 編譯正式版本：

請建立 `keystore.properties` 檔案，包含以下內容，註明[簽名信息](https://developer.android.com/studio/publish/app-signing.html)：

```gradle.properties
storePassword=myStorePassword
keyPassword=mykeyPassword
keyAlias=myKeyAlias
storeFile=myStoreFileLocation
```

然後執行：

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

在 Linux 或 macOS 上執行 `make clean`，Windows 上執行 `.\gradlew clean`。

其他問題:

1. 首先嚐試 `make clean`
2. 確保你的倉庫與最新版本一致。如果你修改了一個或更多的子模組，請確保它們與當前倉庫版本相容。
3. 如果問題依然存在（不太可能）, 嘗試進行一次新的克隆。
4. 檢查是否有 PR/issue 與你的問題相關。 如果有的話，嘗試他們的解決方案。
5. 如果以上方法都不工作，你可以提一個 issue 來尋求幫助(可選)。

## 鳴謝

- 開發：[osfans](https://github.com/osfans)
- 貢獻：[boboIqiqi](https://github.com/boboIqiqi)、[Bambooin](https://github.com/Bambooin)、[senchi96](https://github.com/senchi96)、[heiher](https://github.com/heiher)、[abay](https://github.com/a342191555)、[iovxw](https://github.com/iovxw)、[huyz-git](https://github.com/huyz-git)、[tumuyan](https://github.com/tumuyan)、[WhiredPlanck](https://github.com/WhiredPlanck)、[nopdan](https://github.com/nopdan)......
- [維基](https://github.com/osfans/trime/wiki)：[xiaoqun2016](https://github.com/xiaoqun2016)、[boboIqiqi](https://github.com/boboIqiqi)......
- 翻譯：天真可愛的滿滿（繁體中文）、點解（英文）......
- 鍵盤：天真可愛的滿滿、皛筱曉小笨魚、吳琛 11、熊貓阿 Bo、默默ㄇㄛ ˋ......
- 捐贈：[Releases](https://github.com/osfans/trime/releases) 中的“打賞”實時更新
- 社群：在 [Issues](https://github.com/osfans/trime/issues)、[QQ 群 (811142286)](https://jq.qq.com/?_wv=1027&k=AXdR80HN)、[QQ 群 (224230445)](http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=pg_q7UVumWYLq1Rk8kIAqkK1xGt64VnX&authKey=04m9l7OBO5H5vgrEL8IbpsmtnptWM60xy%2FUwYCfyvw9VcRhe8zRzAS1ezoemZdFr&noverify=0&group_code=224230445)、[貼吧](http://tieba.baidu.com/f?kw=rime)、[Google Play](https://play.google.com/store/apps/details?id=com.osfans.trime)、[Telegram](https://t.me/trime_dev) 中反饋意見的網友
- 專案：[RIME]、[OpenCC]、[注音倉頡輸入法]等開源專案

## 第三方庫

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
- [注音倉頡輸入法](https://code.google.com/p/android-traditional-chinese-ime/) (Apache License 2.0)

[注音倉頡輸入法]: https://code.google.com/p/android-traditional-chinese-ime/
[RIME]: http://rime.im
[OpenCC]: https://github.com/BYVoid/OpenCC
