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

## 目前狀態：v3.3.13-t9.11 T08 驗收紀錄

**本次驗收通過；公開發佈狀態以 GitHub 標籤為準。** 本版實作 T08 候選快照關聯、有界分頁及可選數值延遲紀錄，維持輸入依序送達與既有 JNI 彙整回應。T09 中英、數字、符號混輸最佳化未進入此標籤。

凍結 APK 為不可除錯的 Release/R8，日常效能追蹤關閉，支援 Android 5.0+，提供 ARM64 與 x86_64。套件 `com.osfans.trime.debug` 及既有 fork 簽章憑證保持，以支援覆蓋升級。R8 最佳化、縮減程式並壓縮資源，目前規則保留類別名稱；不宣稱更換正式簽章金鑰或完成 ARM64 實機驗收。

編譯原始碼 `2e0ab64a3bd590376c1d15805235241c36e83d33` 凍結於 `build/t08/stable-candidate-4`，536 項輸入前後 SHA 相同。Debug 實際 542.599 秒、Release 653.891 秒；應用 JVM 598 項本輪執行通過，build-logic 9 項報告全 0、任務 UP-TO-DATE；線上 Debug/Release Lint 均 0 issue，本輪 host 174 項通過。

API21/API35 x86_64 新輪模擬器驗收通過：各 320 個 native FIFO 按鍵、49 候選 48+1 分頁、舊快照/編輯器拒絕及 100 次顯示隱藏。Debug 分別涵蓋兩個內建主題的 native Unicode、accessibility 與實際螢幕字形；日常 Release 各以一個目前主題完成 6 次實際點擊、兩次精確漢字提交，17/23 個生命週期檢查點、36 次點擊診斷追蹤，以及原正常 APK 的實際恢復。API35 另完成 T02 與完整 360 寬度 T03；其 Debug 與正常組保留各自實際 boot 身分。

候選 2 因實際候選字形空白拒絕發佈，本版恢復 TextView 框架文字、測量與版面配置契約並保留縮放；候選 3 的 API35 T03 幾何檢查失敗。候選 4 僅修正該探針自身 scroll 座標歸一化，並獨立重做全部建置與裝置階段。舊候選及 T07 證據保留歷史範圍。

實機候選 P95、Java/native/graphics/總 PSS、準確率對照及首次部署仍未測，不宣稱達到效能預算。三個建置階段各保留 55 條上游棄用提示（AGP 52/KSP 3），正常 APK 各保留兩條 META-INF 服務簽章提示，完整裝置警告保留；不稱 warning-free 或流暢度達標。

附件命名為兩個 APK 與含子模組原始碼封存包；實際上傳、下載核驗及 Latest 以 GitHub 為準。詳見 [9.11 驗收紀錄](doc/releases/v3.3.13-t9.11.md)與 [T08 驗證報告](script/quality/VALIDATION-T08.md)。

首 API35 Debug 的三次約 4 秒浮層 buffer 銷毀 stall 單列保留。追加正常 Release 於獨立核對的 3 GiB boot，兩主題各完成三輪全拼，透過實際 fixture IMM 隱藏/顯示按鈕點擊；各主題的三輪在各自同一 owned PID 中核對浮層 1/0/1、組合保留與一次精確 Space 提交；追加 scope 未見同類 stall，不等於實機延遲預算通過。

## 關於

源於開源的[注音倉頡輸入法]前端，基於著名的 [RIME] 輸入法框架，使用 JNI 的 C 語言和 Android 的 Java/Kotlin 語言書寫，旨在保護漢語各地方言母語，音碼、形碼通用的輸入法平臺。

[查看文檔](https://github.com/osfans/trime/wiki)

## 九鍵拼音

這個 fork 在 Trime 中加入基於方案詞典的 `朙月拼音九鍵` / `luna_pinyin_t9`，兩個內建主題均提供 3x3 主鍵區。使用 2–9 輸入拼音，支援逐段鎖定、中間編輯、解鎖與復原；選擇拼音不提交漢字。佈局支援拼音側欄或橫排、左右單手模式、鍵高設定、長按數字及空格滑動移游標。

[專案頁面](https://x2m7.github.io/trime/) | [下載九鍵穩定版 APK](https://github.com/X2M7/trime/releases/latest)

<details>
<summary>T07 / v3.3.13-t9.10 與更早候選歷史紀錄</summary>

以下保留各紀錄當時的建置與驗收範圍。其中「目前」「pending」「Latest」、APK 摘要與可除錯建置說明均為歷史原文，不代表 9.11 候選，也不提供本次驗收。

穩定版目標 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 已完成C37建置/靜態、API21/API35完整矩陣、真實SAF、Direct Boot壓力、自然PIN及三輪冷啟動驗收。實際建置提交 `a5309c0256e325a0f042d2d9062ddbd4f1326c28`，凍結目錄 `build/t07-privacy/stable-candidate37-final1`，時間戳1791464154404（2026-10-08T12:55:54.404Z）；完整建置708.260124秒，應用JVM582、build-logic9、Spotless與Lint0通過，編譯/build warning行0，527項clean原始碼前後不變。

C37以自有副本固定列出的FD，優先完整fstat裝置/inode身分；僅真實EACCES可在兩份聲音fixture全部校準後使用有界fdinfo，嚴格唯一inode與全部fixture不同才證明非fixture。僅取得副本时直接原因EBADF可記錄原槽位消失，固定後所有錯誤均失敗。兩矩陣完成原200次掃描、同檔案0→1→2→1→0、9無關控制代碼、隱私/TTS/SoundPool/播放/釋放與30秒預算。API21僅8探针、未用fdinfo fallback；API35有11探针及實際自有副本中繼資料證明。實際APK審計保留19 appDEX/6 testDEX、21類/148完整body及catch/603引用、未解析0/高於21為0，56反例全拒絕、兩fresh replay的31輸出逐位元組相同、51檔案/553依賴；ARM64仍只作建置/靜態。

QA3繪製身分歸一化和精確舊PID INFO clean-exit分類保留實際focused112及完整raw/PID/context嚴格守衛；QA4只允許expected regular file且解析size0时的可信Toybox regular-empty别名，實際focused61保留原8欄位、数值/inode/权限/mtime守衛、raw kind和stat命令，不編造numeric S_IFREG。QA5在父脚本内以lazy工厂隔离四核心DeviceRun子類，完整Matrix startup方法僅core2→4 literal不同，boot/storage/readiness/cleanup行為保持，Matrix脚本逐位元組不變；其實際focused32和獨立source peers通過。QA5 live98+139+56=293項一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`；目前受影响publication23項一次PASS，host END2026-10-08T16:49:26.873240Z，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`。QA3發佈68+23僅保留历史一次实测，END2026-10-08T14:58:52.115495Z，`build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`；未變的原adjudicator68與actual matrix-only adju2保持自身證據，QA5没有重跑。每次host运行绑定自身QA13及15/17/10依賴，舊全域QAepoch僅作歷史快照；目前適用性peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee`保留矩陣/SAF/byteaudit實際執行範圍；527項候選原始碼及三APK仍是已驗收C37建置。 QA6將继承的唯一handoff後user-state读取保留為完整stdout/stderr及精確command index；重放要求該相邻读取唯一、rc0、stderr空且獨立解析RUNNING_UNLOCKED，然後normal input仍须立即衔接，所有原actions/deadlines保持。目前live Natural56一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`，受影响publication23一次PASS；parent與Matrix仍逐位元組不變，目前適用性peer通過獨立15項執行依賴保留pressure final5實際範圍。QA5 host證據與更早全域QAepoch僅保留各自历史原始碼範圍。

矩陣、真實SAF三阶段、fresh pressure final5、獨立自然PIN及三輪獨立冷啟動均以自身實際報告和獨立peer通過。壓力保留原7秒/1.25秒/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PIDbootbinding/一次真實Delete/資源和清理協定；Natural无controlled rebind或stress injection。僅涵蓋兩個矩陣的actual adjudication通過，validation errors0/unexplained blockers0，仍保留warning記錄1558、known project diagnostics480，warning_free=false；該裁決不替代獨立SAF/pressure/Natural/cold門禁。六門禁獨立peer SHA依次為`7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`。 Fresh pressure final5與Natural final2只涵蓋明確記錄的4-vCPU/2048MiB host-GPU环境，兩個矩陣和冷啟動仍涵蓋原2-vCPU/2048MiB环境；不聲稱兩核心环境7秒保證，也不聲稱已證实或修複CPU特定产品缺陷。 外部資源位元組證明来自UID2000输入动作结束後的有界privileged read-only lease，随後還原UID2000；不證明AppUID直接访问。

應用APK：ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c`（22,442,517位元組）、x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e`（22,539,554位元組）；test `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db`（1,480,837位元組）。應用各保留2條META-INF v1服务提示、test0，min21/target37/versionCode20261115與原debug包/憑證保持。API21的105條AudioTrack FAST提示保留而SoundPool功能完成；不聲稱實體ARM/16KiB页/OEM全涵蓋或全部bug/warning消除。C37舊失敗保留：P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`；局部handoff/Delete不計whole或資源通過，P2資源審計body從未開始。發佈目標為已授權稳定tag/連結，六文件提交後繼續遞迴原始碼封存、兩應用APK staging及11項實際evidence驗證，不編造此前已published時間或receipt。 P3原inventory87檔案、81份四步證明完成，#82 LOCK僅stat-before；其cat/device hash/stat-after和剩余5檔案未到达。P4耗尽未放宽的7秒观察預算，接近边界的新進程fork未提供完整預算内進程或ready-input證明；獨立时序/原始碼诊断 `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7`没有建立可支持的产品延迟修複，也未把LatinIME主动自杀解释為fatal/OOM。兩次原失敗只留历史；目前whole驗收僅来自明確四核心範圍的新pressure final5。 Natural1原whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c`保持：bounded handoff及Delete/UID2000局部输入證明完成，但固定尾部读取的span bookkeeping失敗，資源審計未到达；獨立诊断 `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93`及diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050`不重分類舊run，目前自然路径驗收只来自新Natural final2。 首次publication23 fixture尝試因audit推导前缺少locked-log输入而失敗，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`，該失敗host run保持历史。第二次尝試22個方法通過，但舊反例重排記錄後未重新封存命令索引而有1個失敗，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`，仍保留為失敗host尝試；最终23驗收只采用修正fixture/反例自身完成且绑定實際原始碼的報告。

### C36/C35 歷史紀錄（不借用為 C37 驗收）

以下段落保留當時原紀錄，C37 目前狀態以上方 pending 為準。

擬發佈穩定版 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 目前 **candidate36：建置/靜態及 Android 5.0 完整矩陣通過，Android 15 完整矩陣整體 FAIL，穩定發佈停止**。C36 提交 `97d38863a4353fbdde95eca3592a60e4a653887e` 僅修改 AndroidTest 的 `InputFeedbackProbe.kt`，正式程式碼未改；凍結目錄 `build/t07-privacy/stable-candidate36-final1`，時間戳記 `1791460043820`（2026-10-08T11:47:23.820Z）。完整建置 458.793 秒、應用 JVM 582 項、build-logic 9 項、Spotless、Lint 0、編譯/build warning 行 0，527 原始碼不變；C36 新 APK 身分/簽章/ABI/48 資源/16 KiB ZIP/ELF 靜態通過。應用各有 2 條 META-INF v1 服務提示、測試 APK 0，min21/target37 與原 debug 憑證/套件保留。415 項裝置 QA 宿主與獨立原始碼複核通過，11 QA 摘要凍結。C36 Android 5.0 實際完成 8 探針、13 路線、17 生命週期、100 次顯示隱藏、52 個 glyph/4 次點號提交及降級輸入/重試；獨立複核 925 項通過。Android 15 隨後在聲音探針 PID5442 首次 FD 快照的 `Os.stat` 收到 EACCES，整體失敗；具體 FD 槽位未記錄，不能宣稱已定位或修復原因。其真實持有剪貼簿 UI 與此前五項探針僅作局部通過。專項位元碼、發佈工具最終綁定與其餘門禁仍待新完整證據：C36整體執行驗收FAIL：API21 whole PASS、API35 whole FAIL，其餘未執行門禁不借用舊/新候選；穩定發佈已停止。聲音探針修正後的後續候選須重新建置並完成全部自身驗收，不能借用 C36 API21 或 C35 通過。未推送、建立標籤或發佈，Latest 仍為 `v3.3.13-t9.9`。

C35 目前 QA 的 Android 5.0 final2 矩陣已整體 FAIL：`InputFeedbackProbe.kt:103` 在 200 次同步讀取後觸發全程序 FD 增量超過 8 的斷言，實際 before/after 數值與 FD 目標未記錄，來源仍未證實，不能稱已定位聲音讀取器洩漏或已證明假陽性；後續 SoundPool/旋律/播放檢查未執行，依序安排的 Android 15 正式矩陣未啟動。C36 將該測試改為聲音 fixture 的 `(st_dev, st_ino)` 逐 FD 槽位歸屬計數，保留全程序總數作診斷；同一檔案開啟兩次的 1→2→1→0 控制與 9 個無關句柄控制已完成宿主複核，API21 實際校準已通過，API35 在首個快照過程中 EACCES 失敗；修正後的新候選仍須重新校準並完成全部門禁。原 200 次讀取、30 秒預算及隱私/TTS/SoundPool/播放/清理要求保持；不增加 GC、等待或容差，也不注入損壞 YAML 警告。

C35 歷史紀錄（以下當時狀態以上方 C36 紀錄為準）：candidate35 建置通過，完整裝置驗收未完成。統一隱私與固定上游 `e9b21960` 的 30 項整合提交保留；C35 使用 WorkManager 2.10.5 官方 Configuration.Provider 和穩定 lazy 設定，統一 getInstance 初始化路徑，修復普通程序恢復時的雙重初始化競態，並由 SDK21 短路及實際 DPS/舊 DE/替換狀態守衛拒絕不安全程序初始化。實作提交 `dc69d73baa0c06e5a103000d1cd13f106f280afd`，527 項原始碼，時間戳 `1791449483471`（2026-10-08T08:51:23.471Z）；正式凍結目錄為 `stable-candidate35-final1`，先前中斷建置沒有形成身分鏈。完整建置 519.403 秒通過，應用 JVM 582 項、build-logic 9 項均通過，Spotless 通過，Lint 0，編譯/build warning 行 0；APK 身分、靜態與專項 API21 DEX 檢查通過。應用 APK 各保留 2 條 META-INF v1 提示（test 0）；舊 QA 的 C35 Android 5.0 矩陣與獨立審閱真實通過，但目前 QA phase4 的 Matrix/綁定工具已變，API21 矩陣須全新重跑；未改的真實 SAF 三階段與歸屬審閱經目前校驗器複核後保留通過；首次 SAF 嘗試因準備末尾保留 Latin、Main 重定向 Setup3 而在任何選擇器動作前停止，不計 SAF 通過或產品缺陷，新的 gate 前準備恢復並核驗 Trime，不寫 SAF tree/首選項/grants；SAF 按實際程序歸屬的獨立日誌審閱亦通過，其餘裝置門禁與最終裁決仍待證據，C35 整體執行驗收未完成，API21 final2 整體 FAIL（歷史）。C35 Android 15 matrix final1 整體失敗：真實持有剪貼板 UI 與九項探針通過，ClipEditorProbe 未確認穩定鍵盤，後續導航/生命週期/降級未執行，失敗保留，後續解析診斷不改寫該矩陣結論。C35 壓力 final1 整體失敗：原 7 秒交接與完整 6501 ms 手勢通過，但第十階段在既定一次 focus 前要求鍵盤，因此未完成真實 Delete/資源稽核；失敗證據保留，壓力 QA 順序校正通過宿主及獨立 peer，但 final2 隨後在 mutation-3 超過原 1.25 秒而整體失敗，6503 ms 手勢只作單項。epoch3 完整視窗短診斷仍是整體 FAIL。唯讀重放確認 75 份 Latin 幀中，第一份過渡幀仍被目前綁定守衛拒絕；其餘 74 份完整顯示且目前連線有效的幀帶有 `mRestartOnNextWindowFocus=true`。Android 15 將其定義為未來視窗取得焦點時的重新啟動請求，舊 false-only 謂詞錯誤拒絕了這些有效幀。phase4 已協調套用嚴格唯一的 true|false 解析，保留目前 PID、序列、有效連線、方法、EditorInfo 與可見性檢查；未來標記原值繼續記錄，但不進入穩定綁定身分。獨立複核亦發現初次 /tmp 普通輸入提案會漏掉空值、等號前後空白或裸欄位名稱的重複欄位，已在任何 live 套用前修正，兩套普通輸入測試均拒絕這些重複欄位。隨後全新 phase4 Android 15 剪貼板短診斷及完整原探針斷言實際通過，只執行一次 Latin→fresh Trime；獨立執行複核仍待完成，短診斷不計完整矩陣或發佈驗收。精確套用後工具的 415 項宿主檢查全數通過；宿主與原始碼複核只證明解析修正，不把舊診斷改成通過，也不代替新裝置結果。目前 API21/API35 矩陣、壓力與自然 PIN、冷啟動及最終裁決均待全新證據。C31–C34 結果只作歷史；C34 自然交接及一次 Delete 的部分證明不能代替其整體失敗。原 7 秒/1.25 秒/6500 ms/60 Hz 規則保留，特權唯讀檔案稽核獨立標記，不偽稱應用 UID 存取證明。現有 debug 憑證/套件和 C31 歷史 SAF 部署 6,280 ms 排隊限制保留；C35 本次目錄選擇/完整部署亦記錄一次非注入的 5,434 ms Rime 排隊，早於 instrumentation，三份快照為同一事件，殘餘效能限制保留；最終 SAF 歸屬審閱保留一次 root 系統 input.Input 命令退出時的 native SIGSEGV，按 PID 確認為系統命令而非 Trime，不能稱完整裝置日誌無 fatal；ARM64 僅建置/靜態檢查，無 ARM64 實體裝置或原生 ARM 映像驗收。目前未推送或發佈，Latest 仍為 `v3.3.13-t9.9`。詳見[發行草稿](doc/releases/v3.3.13-t9.10.md)及[T07 驗證報告](script/quality/VALIDATION-T07-PRIVACY.md)。

ARM64 與 x86_64 APK 保留 Android 5.0+ 支援，延續此前 fork 的套件名稱 `com.osfans.trime.debug` 和簽章憑證，以支援覆蓋升級；預設及非中文語系的顯示名稱為 `Trime`，簡體中文為「同文输入法」，香港／臺灣繁體中文為「同文輸入法」。附件仍是除錯簽章、可除錯建置，未啟用正式簽章或 R8。升級前備份設定與詞庫；自訂目錄中的同名方案或主題可能需要合併新規則、佈局並重新部署，請保留個人詞庫。

[T04](script/quality/t04/README.md) 在獨立目錄中提供固定排名與學習測試，預設繼續使用朙月詞典，不啟用大詞庫或語法模型。[T05](script/quality/t05/README.md) 將六組可選模糊音與三類有界數字鍵修正分開設定，預設全部關閉，建議標明來源。最佳化計畫 T08–T10 仍是後續工作：本版不宣稱已完成其要求的實機正式建置候選延遲/PSS 預算、固定混輸切換次數驗收，或擴充的個人詞庫與候選管理驗收。

C35 每份應用 APK 保留兩項必要協程服務檔案的 v1 META-INF 簽章覆蓋警告（測試 APK 為 0）；C36 新 APK 靜態檢查也實際確認兩份應用各 2 條提示、測試 APK 為 0；必要資源繼續保留，不能宣稱 warning_free。模擬器驗收不代表 ARM64 實機效能、所有 OEM 編輯器或物理 16 KB 頁裝置已驗證。Release 僅上傳兩個應用 APK 和一個遞迴原始碼套件，共三項手動附件；GitHub 自動產生的原始碼入口保留。另見[鍵位規則](script/quality/T03.md)、[九鍵設計](script/t9/README.md)和[上游合併記錄](script/t9/UPSTREAM-t9.3.zh-CN.md)。

</details>

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
