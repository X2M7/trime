<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# 同文 Android 輸入法平臺

![build](https://github.com/X2M7/trime/actions/workflows/commit-ci.yml/badge.svg?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub release](https://img.shields.io/github/release/X2M7/trime.svg)](https://github.com/X2M7/trime/releases)
[![F-Droid release](https://img.shields.io/f-droid/v/com.osfans.trime.svg)](https://f-droid.org/packages/com.osfans.trime)
[![Project page](https://img.shields.io/badge/GitHub%20Pages-Trime%20T9-2f6f5e)](https://x2m7.github.io/trime/)

[English](README.md) | [简体中文](README_sc.md) | 繁體中文

## v3.3.13-t9.18 — 上游整合

本候選合入上游 develop 截至 `c7f32b6c` 的六項提交，修復帶修飾鍵的 Return，並將資料管理整合至 `core.data.UserDataManager`。保留本專案已校驗的交易式匯入、復原、取消、原同步索引及 Direct Boot 行為；預設設定僅在缺失時建立，不覆寫使用者檔案。摘要緩衝增至 32 KiB，SAF 掛起讀取後重新核對本機內容。Gradle 9.8.1 在合併前已經具備。

`versionCode 20261123` 高於已實物核對 28 個公開 APK 的最高值 `20261122`。正式套件沿用 `com.osfans.trime.debug` 及原憑證，本候選實際 Release／不可偵錯身分、minSdk21、原憑證與每份 APK 的單一 native ABI 檢查已通過，簽章檢查無警告。升級前把詞庫與設定備份複製至獨立位置，再覆蓋安裝。

Debug／Release 實際建置通過，建置及正式 APK 簽章檢查警告為 0。820 項應用 JVM 測試（86 份 XML）、Spotless 和 268 項宿主檢查新執行通過；34 項建置邏輯結果為快取（28 + 6），單列。雙 Lint 均通過、0 問題，正式 APK 身分／簽章／ABI 檢查通過。Android 5.0 與 Android 15 各通過九項正式套件覆蓋升級、原 Debug 編輯器回歸及五種 Return、真實系統目錄授權和七項嚴格匯入；各完成兩主題 40 輪顯示與 100 次顯隱，未解除安裝或清除 Trime 資料。兩平台控制器和模擬器均正常結束。

Android 5.0／15 的 3,024／8,126 條 W／E 全部保留，部分診斷根因仍未知，不能歸為無害系統雜訊。這是限定範圍的模擬器驗收，不表示所有執行警告已經消除或真機驗收通過。

範圍見[驗證報告](script/quality/VALIDATION-UPSTREAM18.md)、[升級說明](script/release/README.md)及[版本 Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.18)。手動附件限定兩個正式 APK 與完整遞迴原始碼；線上發布狀態以版本 Release 為準。先前未定位的 ART finalizer 診斷也出現在舊版及新版 Trime 服務中，不能宣稱所有執行警告已消除。實體 ARM64／OEM／16 KiB 頁、真機 TalkBack／TTS、P95／PSS 與長期駐留仍未驗收。

下方 v3.3.13-t9.17 內容保留為該版本的歷史證據。

## 歷史 v3.3.13-t9.17 — 版本與覆蓋升級一致性

應用程式版本、APK 名稱、發布工具及 Pages 統一讀取版本中繼資料。`versionCode 20261122` 高於全部 26 個已核對歷史 APK 的最高值 `20261121`。繼續沿用歷史套件名稱 `com.osfans.trime.debug` 及原憑證，正式 APK 實際為不可偵錯的 Release 建置。升級前把設定與詞庫備份複製到獨立位置，再覆蓋安裝，不解除安裝、不清除資料。

本版建置的 812 項應用 JVM 測試及 268 項宿主檢查新執行通過，34 項建置邏輯測試為快取結果，雙 Lint 均為 0 問題。

Android 5.0 與 Android 15 x86_64 模擬器從 v3.3.13-t9.16 正式 APK 覆蓋升級到本版，各通過全部九項檢查，涵蓋設定、偏好、UID、已選及已啟用輸入法和真實原生使用者詞庫保留。檢查使用專用測試環境與內部空編輯器，不代表公開介面輸入或跨套件名稱遷移驗收。

範圍與線上狀態見 [T14 驗證報告](script/quality/VALIDATION-T14.md)、[升級說明](script/release/README.md)及[版本 Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.17)。計劃附件僅兩個正式 APK 與完整遞迴原始碼。實體 ARM64 手機、OEM、16 KiB 頁、可聽 TTS、P95／PSS 與長期駐留仍未驗收，執行警告、先前失敗嘗試及獨立輔助程式的未決警告繼續保留。

下方 v3.3.13-t9.16 內容保留為該版本的歷史證據。

## 歷史 v3.3.13-t9.16 — 無障礙與裝置適配

按鍵、候選、九鍵拼音篩選與預編輯控制項補齊可存取名稱、角色、選取狀態與操作。動態換行鍵名稱隨傳送、搜尋等動作更新；候選更新後的舊操作會被輸入身分檢查攔截。改善大字體、橫向與視窗邊界適配，短螢幕可透過垂直捲動存取內容。

v3.3.13-t9.16 建置的 812 項應用 JVM 測試與 227 項宿主檢查新執行通過。獨立 Debug／Release Lint 0 問題，建置及雙 ABI 正式 APK 簽章檢查無警告。384 項原生檢查按 31 項一致輸入綁定重用歷史結果，本輪未重跑；生產 JNI 工作為 UP-TO-DATE，不宣稱全部重新編譯。

已測模擬器範圍包括 Android 5.0 四組設定、Android 15 八組設定與額外一組 Steam 深色配色檢查。夜間模式矩陣仍用淺色配色，深色像素依據來自獨立 Steam 原圖。Android 15 正式 APK 上，實際 TalkBack FOSS 探索與雙擊完成九鍵輸入、篩選拼音、選詞及傳送。一般與兩倍字體、上下兩個分割畫面位置的 TalkBack 輸入均通過；整輪執行警告的分類與限制見驗證報告。

本歷史記錄凍結於該版本發布前，線上狀態以[版本 Release 頁面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.16)為準。系統／模擬器執行警告與歷史失敗全部保留，不表示所有警告或風險已經消除。[9.16 驗證範圍](script/quality/VALIDATION-t9.16.md) · [歷史 9.15 驗證](script/quality/VALIDATION-t9.15.md)。

保留 Android 5.0+、ARM64／x86_64 建置支援。實體手機、OEM、16 KiB 頁、真實語音／TTS、候選 P95／PSS 與長期駐留仍未驗收。v3.3.13-t9.16 當時計劃的手動附件僅兩個正式 APK 與包含主儲存庫及全部 21 個遞迴子模組的完整原始碼封存檔；GitHub 自動 source zip／tar 保留。

## 關於

源於開源的[注音倉頡輸入法]前端，基於著名的 [RIME] 輸入法框架，使用 JNI 的 C 語言和 Android 的 Java/Kotlin 語言書寫，旨在保護漢語各地方言母語，音碼、形碼通用的輸入法平臺。

[查看文檔](https://github.com/osfans/trime/wiki)

## 九鍵拼音

這個 fork 在 Trime 中加入基於方案詞典的 `朙月拼音九鍵` / `luna_pinyin_t9`，兩個內建主題均提供 3x3 主鍵區。使用 2–9 輸入拼音，支援逐段鎖定、中間編輯、解鎖與復原；選擇拼音不提交漢字。佈局支援拼音側欄或橫排、左右單手模式、鍵高設定、長按數字及空格滑動移游標。

[專案頁面](https://x2m7.github.io/trime/) | [下載九鍵穩定版 APK](https://github.com/X2M7/trime/releases/latest)

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
git clone git@github.com:X2M7/trime.git
cd trime
git submodule update --init --recursive
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
