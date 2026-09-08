# T04 Dictionary Evaluation

Run: `e40563859022`

## Identity

- APK: `92e0e56244559b091801e8748f25a7fb2d80313938147c0ca006290e1d7ad9f6`
- Build: 3.3.13-t9.6 / 20261108; `com.osfans.trime.debug`
- Git: `425e334803b93b0bff0111da289ccec542eda4b9`
- Certificate: `c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2`
- Corpus: `0bc8e326b8744321e53798683f225c892cc5a2f8d14faf8d0ac70fd666218a42`
- Engine: `1d5a6870960186bfe9ada329cca80f6451872f9b6a8035369c1cab423bf2a480`; arm64-v8a; native_bridge
- Android: 15 / API 35
- Dictionary overlays are experiments, not the installed APK's shipped configuration.
- Device fingerprint, source/seed/runtime checksums and effective configs: `report.json`.

## Accuracy

Top-1/Top-3 require exact text AND full input consumption; sentence exact uses sentence cases only.

| Profile | Cohort | Split/mode | N | Top-1 % | Top-3 % | Sentence % | Extra choices /100 resolved chars | Unresolved |
|---|---|---|---:|---:|---:|---:|---:|---:|
| luna | cold | dev/trad | 32 | 65.62 | 93.75 | 83.33 | 12.63 | 0 |
| luna | cold | dev/simp | 32 | 65.62 | 93.75 | 83.33 | 12.63 | 0 |
| luna | cold | holdout/trad | 32 | 68.75 | 81.25 | 71.43 | 11.4 | 1 |
| luna | cold | holdout/simp | 32 | 71.88 | 84.38 | 85.71 | 7.89 | 1 |
| luna | learned | dev/trad | 32 | 65.62 | 93.75 | 83.33 | 12.63 | 0 |
| luna | learned | dev/simp | 32 | 65.62 | 93.75 | 83.33 | 12.63 | 0 |
| luna | learned | holdout/trad | 32 | 68.75 | 84.38 | 71.43 | 11.4 | 1 |
| luna | learned | holdout/simp | 32 | 71.88 | 87.5 | 85.71 | 7.89 | 1 |
| common | cold | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| common | cold | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 9.68 | 1 |
| common | cold | holdout/trad | 32 | 68.75 | 78.12 | 71.43 | 13.16 | 1 |
| common | cold | holdout/simp | 32 | 71.88 | 81.25 | 85.71 | 9.65 | 1 |
| common | learned | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| common | learned | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 9.68 | 1 |
| common | learned | holdout/trad | 32 | 68.75 | 81.25 | 71.43 | 13.16 | 1 |
| common | learned | holdout/simp | 32 | 71.88 | 84.38 | 85.71 | 9.65 | 1 |
| extended | cold | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| extended | cold | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 9.68 | 1 |
| extended | cold | holdout/trad | 32 | 68.75 | 78.12 | 71.43 | 13.16 | 1 |
| extended | cold | holdout/simp | 32 | 71.88 | 81.25 | 85.71 | 9.65 | 1 |
| extended | learned | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| extended | learned | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 9.68 | 1 |
| extended | learned | holdout/trad | 32 | 68.75 | 81.25 | 71.43 | 13.16 | 1 |
| extended | learned | holdout/simp | 32 | 71.88 | 84.38 | 85.71 | 9.65 | 1 |
| balanced | cold | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| balanced | cold | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| balanced | cold | holdout/trad | 32 | 68.75 | 78.12 | 71.43 | 13.16 | 1 |
| balanced | cold | holdout/simp | 32 | 71.88 | 81.25 | 85.71 | 9.65 | 1 |
| balanced | learned | dev/trad | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| balanced | learned | dev/simp | 32 | 68.75 | 96.88 | 100.0 | 10.53 | 0 |
| balanced | learned | holdout/trad | 32 | 68.75 | 81.25 | 71.43 | 13.16 | 1 |
| balanced | learned | holdout/simp | 32 | 71.88 | 84.38 | 85.71 | 9.65 | 1 |

Extra choices are a bounded oracle replay, not a human usability measurement: longest matching target prefix among 80 candidates; each intermediate selection counts once; final confirmation counts only if non-first. Final commit is not executed. Scrolling, pinyin locking, retries and unresolved cases are NOT assigned zero cost. All totals here are recalculated from hashed step traces; early probe weighted-effort totals are not used. Different resolved coverage must not be compared without the paired subset below.

## Resources

| Profile | Fresh deploy seconds | Cold sampled engine PSS MiB | Learned sampled engine PSS MiB |
|---|---:|---:|---:|
| luna | 740.10 | 160.29 | 161.90 |
| common | 716.52 | 160.14 | 161.65 |
| extended | 720.46 | 160.19 | 161.37 |
| balanced | 744.02 | 160.18 | 161.65 |

PSS samples are from the standalone APK-engine process after queries, not the Android keyboard service and not deployment peak. Native-bridge timings cannot predict ARM phone performance. Learned groups reuse only cold compiled tables, never cold user DBs.

## Improved And Regressed

### common: improved

- name03 (dev): 欧阳, rank 3 -> 2; first 模样 -> 模样
- address04 (dev): 送到小区门口, rank None -> 1; first 送到陷入门口 -> 送到小区门口
- address08 (holdout): 快递放在门口, rank None -> 1; first 快地方在门口 -> 快递放在门口
- homo06 (holdout): 异议, rank 10 -> 5; first 意义 -> 意义

### common: regressed

- place04 (dev): 西安, rank 13 -> None; first 小 -> 小
- name06 (holdout): 李雷, rank 10 -> 12; first 积累 -> 积累
- sent08 (holdout): 请问地铁站在哪里, rank 1 -> None; first 请问地铁站在哪里 -> 请问地铁站在那里
- edge07 (holdout): 西安, rank 2 -> 4; first 一波 -> 一按
- edge08 (holdout): 先, rank 2 -> 3; first 小 -> 小

### extended: improved

- name03 (dev): 欧阳, rank 3 -> 2; first 模样 -> 模样
- address04 (dev): 送到小区门口, rank None -> 1; first 送到陷入门口 -> 送到小区门口
- address08 (holdout): 快递放在门口, rank None -> 1; first 快地方在门口 -> 快递放在门口
- homo06 (holdout): 异议, rank 10 -> 5; first 意义 -> 意义

### extended: regressed

- place04 (dev): 西安, rank 13 -> None; first 小 -> 小
- name06 (holdout): 李雷, rank 10 -> 12; first 积累 -> 积累
- sent08 (holdout): 请问地铁站在哪里, rank 1 -> None; first 请问地铁站在哪里 -> 请问地铁站在那里
- edge07 (holdout): 西安, rank 2 -> 4; first 一波 -> 一按
- edge08 (holdout): 先, rank 2 -> 3; first 小 -> 小

### balanced: improved

- name03 (dev): 欧阳, rank 3 -> 2; first 模样 -> 模样
- address04 (dev): 送到小区门口, rank None -> 1; first 送到陷入门口 -> 送到小区门口
- address08 (holdout): 快递放在门口, rank None -> 1; first 快地方在门口 -> 快递放在门口
- homo06 (holdout): 异议, rank 10 -> 5; first 意义 -> 意义

### balanced: regressed

- place04 (dev): 西安, rank 13 -> 44; first 小 -> 小
- name06 (holdout): 李雷, rank 10 -> 12; first 积累 -> 积累
- sent08 (holdout): 请问地铁站在哪里, rank 1 -> None; first 请问地铁站在哪里 -> 请问地铁站在那里
- edge07 (holdout): 西安, rank 2 -> 4; first 一波 -> 一按
- edge08 (holdout): 先, rank 2 -> 3; first 小 -> 小

## User Dictionary

Each training target is explicitly committed five times using exact pinyin; a new process then queries raw T9. Rank changes below are observable ranking effects, not merely the existence of a database file.

| Profile | Target | Cold rank | Learned rank |
|---|---|---:|---:|
| luna | 權利 | 1 | 1 |
| luna | 時節 | 3 | 1 |
| luna | 異議 | 10 | 1 |
| luna | 事實 | 1 | 1 |
| common | 權利 | 1 | 1 |
| common | 時節 | 3 | 1 |
| common | 異議 | 5 | 1 |
| common | 事實 | 1 | 1 |
| extended | 權利 | 1 | 1 |
| extended | 時節 | 3 | 1 |
| extended | 異議 | 5 | 1 |
| extended | 事實 | 1 | 1 |
| balanced | 權利 | 1 | 1 |
| balanced | 時節 | 3 | 1 |
| balanced | 異議 | 5 | 1 |
| balanced | 事實 | 1 | 1 |

## Decision

Development-only nominee: **common**. Gated default: **luna**.

- FAIL: cold/simp: Top-1 +6.25pp
- FAIL: cold/simp: Top-3 non-regression
- PASS: cold/simp: sentence non-regression
- PASS: cold/simp: resolved coverage non-regression
- PASS: cold/simp: measured PSS growth <=20 MiB
- FAIL: cold/trad: Top-1 +6.25pp
- FAIL: cold/trad: Top-3 non-regression
- PASS: cold/trad: sentence non-regression
- PASS: cold/trad: resolved coverage non-regression
- PASS: cold/trad: measured PSS growth <=20 MiB
- FAIL: learned/simp: Top-1 +6.25pp
- FAIL: learned/simp: Top-3 non-regression
- PASS: learned/simp: sentence non-regression
- PASS: learned/simp: resolved coverage non-regression
- PASS: learned/simp: measured PSS growth <=20 MiB
- FAIL: learned/trad: Top-1 +6.25pp
- FAIL: learned/trad: Top-3 non-regression
- PASS: learned/trad: sentence non-regression
- PASS: learned/trad: resolved coverage non-regression
- PASS: learned/trad: measured PSS growth <=20 MiB
- PASS: fresh deployment <=2x baseline

Full paired selection costs, all rank changes and gate outcomes are in `comparison.json`. This small authored corpus is a regression suite, not a population estimate; identical homophone codes have mutually exclusive goldens. No large dictionary or octagram model was enabled.
