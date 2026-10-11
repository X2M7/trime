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

## v3.3.13-t9.13 — 本輪穩定版驗收通過

支援 Android 5.0+，提供 ARM64 與 x86_64 Release APK。本輪穩定版驗收通過，線上狀態以[版本 Release 頁面](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.13)為準。

預設全拼與九鍵共用 `luna_pinyin` 學習庫及 `custom_phrase.txt`。個人詞庫可匯入、匯出及備份恢復；常用短語可編輯、匯入匯出，修改前自動備份供恢復。Rime portable 快照匯入合併記錄，精確檔案備份恢復替換受保護檔案。置頂依目前方案與原始輸入生效，恢復排序保留學習；忘記此詞請求移除已學習記錄，未學習詞可能提示無法忘記；不是永久封鎖系統詞，也不刪除置頂規則。操作失敗時顯示輸入法提示框。候選註釋、可選橫向捲動與全文閱讀方便查看長候選，並拒絕刷新後的舊手勢。支援 Shift 雙擊鎖定並保留自訂主題動作。

置頂最多 512 條規則，只在前 144 候選內查找，隱私模式不套用或修改置頂；字母與數字短語編碼不自動互轉。展開分頁每頁 48 項、預取距離 16、`maxSize` 目標 144 是 best-effort 預算，不是普遍硬記憶體上限。

本輪 702 項新執行應用 JVM 測試、213 項新執行宿主測試通過；17 項 build-logic＋6 項 compatibility 為快取結果，單列統計。Debug／Release Lint、建置及 Release 簽章警告均為 0。Android 5.0 和 Android 15 模擬器完成正式版覆蓋升級、候選和常用短語介面驗收及個別 Debug 兩主題回歸，包含快速輸入40輪與鍵盤顯示隱藏100次。兩輪4,463／11,620條原始 W/E/F 和完整控制台診斷均保留並逐行審閱；這不表示平台所有診斷已消失或全部環境風險已排除。

真機 P95/PSS、長期駐留、首次部署、物理 ARM64／OEM／16KiB 頁、真實 TTS 與 SAF 選擇器權限／取消未測。[驗收範圍與歷史](script/quality/VALIDATION-t9.13.md)。手動附件僅兩個 APK 與含遞迴子模組的完整原始碼包；發布狀態及附件完整性以 Release 頁面和歸檔外發布記錄為準。

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
