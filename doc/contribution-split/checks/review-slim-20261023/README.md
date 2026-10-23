# 三项 PR 的审阅精简（2026-10-23）

修改直接推送到原贡献分支；没有另建 PR 或操作审阅状态。

| PR | 当前源码 | 本轮验证 |
|---|---|---|
| [#2188](https://github.com/osfans/trime/pull/2188) 延迟创建输入视图 | `e009dc0b` | [上游 CI 5/5](https://github.com/osfans/trime/actions/runs/39897773613)；[原始资格](pr2188/ci1/qualification.json) |
| [#2186](https://github.com/osfans/trime/pull/2186) JNI Unicode 转换 | `2ad1865f` | [上游 CI 5/5](https://github.com/osfans/trime/actions/runs/39897889043)；[原始资格](pr2186/ci1/qualification.json) |
| [#2182](https://github.com/osfans/trime/pull/2182) 日志进程与视图 | `9f95cb57` | [上游 CI 5/5](https://github.com/osfans/trime/actions/runs/39900491225)；[原始资格](pr2182/ci2/qualification.json) |

详细来源、当前与历史源码边界见 [限定总表](root-qualified-scope.json) 和 [最终源码审计](source-audit-final.json)。CI 是各自记录的上游合并产物，未称作完整 fork 原树 APK，也未借用旧 301／349 项测试数。

- #2188：删除销毁时重复状态重置并合并条件；待创建、待输入开始、finish 清理和仅补缺失树仍保留。
- #2186：删长度查询和描述结果的重复检查；有效 UTF-8 直接转换，非法文本才替换重试。115＋46 项 host 检查、六个 NDK 编译单元通过，资源释放及失败后停止副作用仍保留。
- #2182：精简重复 UI 条件、回退未使用工具接口、统一一次进程释放并移除额外固定部署超时。保留标准 reader.use、取消监听及经 -ea 回归需要的短取消异常处理。最终 LogView 使用默认生命周期启动，内部仍先订阅后开进程。

日志 host5 新执行 46 项（13 Process／17 Logcat／16 Buffer）；9f95 后续只改 LogView，六个 host 输入字节不变，因此按哈希复用这 46 项，不称重跑或 LogView 的 host 验收。旧 ec0 CI 即使 5/5 也单列为历史；最终 9f95 另有自己的正式 CI。所有早期失败及更正报告保留。

本轮未重跑模拟器或真机；不代表 TalkBack、Release、升级、性能、签名或物理 ABI 验收。CI 固定依赖及 SDK／JNA／Gradle／缓存警告原件保留，没有零警告结论。

原始 host 记录映射见 [静态清单](publication-map-static-final.json)，原始 CI 记录映射见 [CI 清单](publication-map-ci.json)。中间轮询快照和下载的官方源码／编译二进制仍留本地，正式引用与未截断日志保留。`.gitattributes` 保持原换行，统一 SHA 清单覆盖发布文件。

PR 正文将在本证据提交固定后同步，并另存实际逐字读回回执。
