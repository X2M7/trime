# v3.3.13-t9.12 验收报告

当前APK的构建、静态及四组正式模拟器验收均已闭合通过，各组功能、完整warning裁决、独立peer与Root接受分别绑定。公开发布及Latest由外部发布记录验证，文档不预填源码归档自身SHA或DOCHEAD。

编译CODE为 `1d78a9dce1eab27a092c2f6ac0c12e9c38036860`，普通parent为a956，包含已合入的osfans/trime develop提交 `af23c4a631274d118ed76ae77e6dc8d8f7024c56`。552项源输入相对父提交548项保持、四项修改，FullBuild12 before／after精确相同。设备使用同一批冻结APK，早期CODE的PASS不替代本轮接受。

| 检查 | 当前实际结果 |
| --- | --- |
| FullBuild12 Debug／Release | Debug1061.797秒、Release1022.266秒，均exit0；controller2090.375秒实际wait0，2026-10-10 13:25:35.192489 UTC正常闭合 |
| 应用JVM／convention／compatibility | 663项应用测试本轮实际执行；17＋6项为UP-TO-DATE缓存报告，failure／error／skipped均0，不称686项fresh |
| host15 | 128＋31＋13＋5＋29＝206项实际通过；552源与86宿主输入前后相同，owned40326自然wait0 |
| Spotless／Lint／构建警告 | Spotless通过，双Lint零问题，两份raw日志按冻结builder的同一警告策略重核warnings=[] |
| 双Release静态与签名 | static13及独立peer通过；minSdk21、非debuggable、同证书，v1／v2签名零警告，实际DEX及协程语义接受 |
| Android 5.0 Debug10 | editors／T08／privacy／T09及真实SAF roundtrip／restart／revoke七项原探针通过；generic9007ae88…04fb00、warning2 d6bdf947…09ef06、peer3a61591a…43d7ba、Root5cec2fb0…abfb1f正式接受 |
| Android 5.0 Release10 | 两主题各三轮、54组原始PNG／XML捕获，真实候选及准确一次提交、显隐和覆盖升级通过；generic18609c7d…091fb1、warningcdd522fc…2d9ca、peerd528bf00…359fc2、Rootf16dc973…5d4127正式接受 |
| Android 15 Debug5 | 五项原探针通过，generic c0b38635…761a7、修正warning2 bc8951f0…f3fa5、peer2 09085205…1d24c、Rootc1a2523b…bc0c正式接受；四个selected帧1517／1133／746／795ms低于原2000ms门槛 |
| Android 15 Release2 | 两主题各三轮及覆盖升级通过，54组原捕获另加三组startup观察；原Tong-three1失败保留、Tong-three2成功。完整warning层：generic8766ebfa…dc26d、完整warning裁决v3 fd1c1ac6…d5f21e／419885字节、独立peer59fd4b9d…60e1d9及Root9b750691…11e66b4正式接受；Main2079的1105／856ms帧低于原2000ms门槛 |

## 本轮实现及实际验收范围

九键明确QWERTY英文入口、手动语言记忆、临时编辑器强制布局及未提交组合保留均已实现。预编辑／候选使用实际可见Region及自身滚动坐标，空白区域事件路由恢复；BoardManager同一窗口不重复挂接，缓存视图保留。组合modifier逐位更新、SYM掩码及only-Shift判断修正保留。SAF不接受未完整列举的目录，支持失败降级和重试；日志导出移至IO并传播取消。上游中文resetShift在实际接受中文动作后生效，旧中文通知不能清除尚在队列的英文动作Shift。

ThemeFonts按主题实例lazy复用成功解析的字体目录，文件存在性、mtime／size和Typeface缓存仍动态检查。固定栏位跳过隐藏子项测量；右侧栏只有两个解析尺寸严格大于零时才使用该策略，WRAP_CONTENT／MATCH_PARENT等自适应尺寸保留原all-child测量。两组当前Debug均实际通过三项新增回归：真实production输入栏遍历；实际自适应工具栏隐藏项尺寸及移除恢复；唯一自有目录的字体缺失→添加→mtime改变→删除及同选择／强制主题重载。witness和原视图、边界、显示与自有目录均在finally恢复。新增回归排在原100次显隐和modifier回归之后，未替代或污染原冷启动／快速切换测量。

原FINGER触摸、native候选／caret／准确一次提交、whole32×32区域、两主题40次rapid及100次显隐断言均保留。38次实际KeyView普通click／long-release分发验证临时modifier消耗和锁定状态；隔离listener不向native或编辑器提交38个字母，不能称OS物理指针覆盖。Board隔离transition只统计实际回调，不提供动画性能结论。Debug T08各8轮／320事件FIFO，privacy各16项覆盖三个方案（不称48项），T09各30项；Release六轮原生提交并不等于重跑全部Debug矩阵。

当前最终源peer e00bed88…931a8c／6900字节绑定四文件、全部552源与548项逆比不变。Editor SHA30396cdd…f9374e；源审、实际生成代码和设备接受分别记录，不能互相替代。

## 完整warning与环境边界

系统／框架／图形W/E/F逐行保留，warning-free=false。Android 15 Debug5共5134条W/E（W3876、E1258），其中678条框架在项目进程内、4456条其他进程／启动记录；项目发出或未解释的项目类警告0。50112条解析行加五个标准buffer headers完整核对，40个raw命令、282项raw文件、当前五APK及552源均绑定。

Android 5.0 Debug10共2738条W/E（W1817、E921）：115条项目进程内框架、2492条其他进程／启动、131条项目发出。131条包括130条受控SAF失败诊断及一条先前Debug重新部署维护队列6717ms提示；它不是正常候选P95、首次部署或2秒帧门槛豁免。七个selected Main的原框架停顿门槛通过，未解释项目警告0；433项raw文件、48个raw命令、实际三阶段SAF、当前图形profile及五APK均绑定。

Android 5.0 Release10共3053条W/E（W1653、E1400），其中项目进程内框架8条、其他进程／启动3045条，项目发出与未解释项目警告0；3830项raw文件、1668个raw命令与54组原生捕获接受。Android 15 Release2共7089条W/E（W5687、E1402），其中项目Main进程内框架35条、其他进程／启动7054条，项目发出与未解释项目类警告0。37067条解析行及main／events／kernel／system／radio五个实际buffer headers完整核对，4210项raw文件、1836个raw命令、54组原生捕获与另三组startup观察及45次成功输入动作绑定；七条fixture-client自动show拒绝及14组binderID／kernel关联原文保留，具体callback对象与ART／compiler原因未证明。覆盖升级保留UID10148、两个原preferences文件hash与虚构sentinel，不能宣称覆盖全部用户数据。

四组原producer均正常关闭owned VM并实际wait0，all-PID critical／actual-LMK／kernel critical均0；这不等于没有系统诊断。

Android 5.0实际配置swangle／1536MiB／单核／720×1280@320，并使用SDK支持的GLDMA／GLDMA2关闭标志。原Root图形observer在同一boot把EGL扩展行误作为GLES行拒绝，原失败保留；修正observer按官方EGL／GLES命名空间分别核对，保留GL_OES_EGL_image与禁止DMA扩展的原门槛及负控制，没有修复guest kernel或证明旧DMA失败唯一原因。Android 15实际host GPU／3072MiB／四核；不将不同环境或boot的帧时长作同口径性能对照。

两组当前Release的speak_on_keypress和speak_on_commit均实际存在，stored=false／effective=false／default=false。原preferences XML和前后快照绑定；本次没有通过UI关闭或恢复朗读的动作。旧API35 Release1 absent／stored=null事实仅属于旧组，不能套用本轮。默认关闭路径不提供真实开启TTS、引擎释放或历史Pico native崩溃已修复的结论。

## 当前验收工具失败与后续成功的限定

API35 Debug5完整warning第一版的BackDispatcher说明被peer拒绝：warning实际在条件不满足的else，原243条isInProgress=false及计数／2秒守卫不变。原warning1、peer失败及warning2-peer1检查失败都保留；独立接受使用修正warning2与新peer2。API21 Debug10三个offline writer错误、旧warning1 peer失败及同boot图形observer分类失败同样保留，最终使用warning2与新peer；这些分析工具失败未改写为产品崩溃。

API35 Release2第一次Tong启动的自动show请求在CLIENT_VIEW_SERVED失败，比window获得焦点早33ms；tong-three1原报告false、actions=[]、rounds=[]保留。Root另以真实Show按钮诊断，并使用只改变fixture启动的companion：先核对fresh焦点／served input connection，再每轮单次显式Show，不重试。原输入、候选、两秒显隐及准确一次提交guard未修改，产品APK、fixture APK与原helper均未改；实际companion373f6b84…fc98c源码副本存于同cohort sibling，独立source peer另行绑定。Tong-three2在相同Main2079／UID10148／start7654／boot7d6ce2e6下完成三轮、原27组捕获及三组startup观察。只修正验收启动流程，不宣称自动显示的产品缺陷已经修复。

API35 Release2两次离线裁决writer因错误假设失败：第一版要求必有crash buffer header，第二版继承manual_language_mode必须存为native的假设。本轮实际有上述五个buffer headers、没有crash header；两个speech键已存储false，但manual_language_mode键实际不存在。新v3按原日志字节与两次原preferences XML的各18项核对修正，原两次FAIL收据仍保留；没有修改换行、产品、设备或原验收门槛。

## 构建、静态与公开附件的独立绑定

FullBuild12 summary8a49ceaf…df011／2681字节、两raw logs ac322edd…b37234／4d6852db…1256671及实际wait0冻结当前五APK；552源before／after SHA68147489…87c8c3逐字节相同。host15 summary6a5f700d…d4ce2e1／3773字节、五套原commands与552／86输入清单接受；host检查不提供APK／设备通过。两个17＋6 JVM报告为缓存，不重复计作fresh。

static13 verification1f2cb000…2f57ee／58005字节及final peer20c4687c…9d5936／88491字节读取两ABI实际APK：完整classes.dex逐字节相同（4137172字节），4936类定义／21797方法引用。18份fresh输出覆盖实际Main dispatcher、Handler API分支、API26–27 preHandler、R8合并调用者、延迟恢复和取消；完整调用者逆证据接受。Debug classes16正控制包含EditorProbeActivity，Release manifest／DEX／mapping无测试夹具，三项纯内存损坏控制被拒绝。原inspection／mapped collector的passed=false与语义pending保持，独立语义接受单独提供，不改写原collector。

手动发布只包含两份正常Release APK及一个完整递归子模块源码包；三份Debug／test APK只用于验收。编译CODE、仅七文档DOCHEAD、递归源码SHA／bytes及GitHub发布／Latest由外部manifest和发布记录分别绑定，避免归档自引用。发布前只读公开inventory-final4在2026-10-10 14:28:47 GMT通过列表／Latest HTTP200及9.12公开tag HTTP404确认：十个release共22手动附件（16 APK、6 source），额外附件为零，Latest为9.11，9.12尚未公开发布。与final3的规范化附件及Latest逐项相同，未进行附件或release修改；该观察收据SHA6dbe4212…664f5fb／19872字节，final3原记录保留。稳定版本当前状态以 [GitHub Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.12) 与外部发布记录为准。

| Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | 11786759 | `801faa7462dc5ee1da992d5089478ddd9d22689c6443c1f47c9ced757f282ef0` |
| x86_64 | 11865920 | `1ba60933edae1a420b85f2bdef7784f18d0054104873d2e38efa8c0b854ee6dd` |

## 尚未由本轮模拟器验证的性能与设备范围

同一真机的正常Release候选P95≤100ms、Java／native／graphics／总PSS、持续输入与主题切换后的驻留、首次部署、ARM64真机／全部OEM、一般准确率和物理16KiB页设备未测。1517ms首IME帧及40次rapid墙钟不是候选P95；旧2462ms与新帧跨boot比较不能证明唯一因果或30%候选延迟改善。Debug fullCheck是维护检查，不能描述为每次弹键盘必定全量重编译。历史Perfetto／ART／SIGQUIT限定及所有失败原文保留如下。

## 附录：旧FullBuild11冷帧诊断与三次ART失败

全部属于a956/FullBuild11；正式API35 Debug4五功能通过而独立首2462ms失败不豁免。cold-trace1编辑器子测通过、owned4189530正常wait0，但整体passed=false；保留selected2089的4403/2211/2220ms HWUI。Perfetto请求120秒实际77.303890秒，未覆盖约260秒编辑器流程，trace_complete=false，有丢包和解析问题。首帧18127的IMS.showWindow wall2186.646ms、Running2007.898ms及三段非重叠measure总1634.769ms/draw395.745ms仅为有限相关观察，不证明具体项目方法、JIT、宿主GPU或唯一原因，嵌套耗时不直接相加。独立correspondence5b8dc1b7…5ee3c98/206813字节保留完整限定。

cold-stacks1三次SIGQUIT在另boot/PID2138取到Key.SchemeColorCache／Key／Keyboard构造、ConstraintLayout求解、BoringLayout／TextView测量堆栈；4826/3332ms HWUI仍保留。编辑器子测通过、诊断producer整体false；owned22874及controller正常wait0。三次点采样有扰动，不代表方法耗时或频率；跨boot/PID不能把4403与4826比较成改善。准确sample2为5处ConstraintLayout.onMeasure、6处FrameLayout.onMeasure、1处TextView.onMeasure、0处AutoScaleTextView，旧错误计数收据不改写。

ART三次尝试均失败：第一次1000us/30秒采样后pull为0字节并AssertionError；第二次SystemUI keyguard约20140ms ANR触发启动门禁，未开始ART或探针；第三次停止后17次大小检查仍0、30秒flush超时。三次源码不变、owned正常关闭、producer/publication=false，没有可接受的方法样本，不能描述为Trime产品崩溃或性能改善。原报告／controls的完整路径、SHA和bytes在facts历史区绑定；不重跑、不删除失败。

<details>
<summary>字体／隐藏测量修正前a956／FullBuild11及全部更早原记录（历史，不供当前候选验收）</summary>

以下完整旧文属于各自原CODE；旧未填占位仅转换为历史标记。所有实际PASS/FAIL、W/E/F、SIGTERM、Pico与guest DMA/kernel失败原文保留，不能填入当前验收表。

# v3.3.13-t9.12 验收报告

（字体/隐藏测量修正前原时点未填，非本轮结论）

（字体/隐藏测量修正前原时点未填，非本轮结论） 新CODE `a9563344de1f9f6d56470e5588beaf219c7d86fb`（普通parentdb5）已提交；552源快照与FullBuild11/source-before一致。host14实际206项、FullBuild11整体构建及static12实际双APK签名/DEX协程审阅通过；API35 Debug4五探针功能通过但独立2462ms首帧门槛失败，另外三组未运行，正式四组接受0P。七文档DOCHEAD、递归源码与发布均未接受。先前db5/host13P与完整Full10F及全部旧P/F保留历史，不供本轮验收。

| 检查 | 本轮最终事实 |
| --- | --- |
| 新CODE / 上游整合 / 源输入 | a956普通parentdb5，包含上游af23c4a6；相对db5仅Keyboard与EditorLifecycleProbe，147新增/8删除，当前552源精确匹配Build11before |
| FullBuild11 Debug / Release | Debug648.564秒、Release722.793秒/均exit0；整体正常finally闭合，controller1375.682秒/wait0，2026-10-10 11:44:34.599298 UTC结束 |
| 应用JVM / convention / compatibility | 应用663项本轮实际执行；convention17及compatibility6为UP-TO-DATE缓存报告，全部failure/error/skipped0，不称686项fresh |
| 宿主host14与源输入前后 | host14实际128＋31＋13＋5＋29=206项通过，552源与86宿主前后相同；当前CODE与strictrecursiveclean保持，owned4152020自然wait0 |
| Spotless / Lint / 构建警告 | Spotless通过（UP-TO-DATE）；Debug/Release Lint均0，两原始构建日志按原builder警告规则重核均warnings=[]，不包含尚未接受的APK签名/设备警告 |
| 双APK静态 / v1v2签名 / 实际协程语义 | static12当前双Release实际通过：min21/non-debuggable、v1/v2均验证且warnings=[]；双完整DEX字节相等、4936类定义/21796 method IDs、18份新smali协程调用语义及Release无debug夹具通过；不提供设备通过 |
| API21 Release | （字体/隐藏测量修正前原时点未填，非本轮结论） |
| API21 Debug与真实SAF三阶段 | （字体/隐藏测量修正前原时点未填，非本轮结论） |
| API35 Release | （字体/隐藏测量修正前原时点未填，非本轮结论） |
| API35 Debug全探针 | API35 Debug4 closed：editors/t08/privacy/t09/saf功能5P，独立验收FAIL（Main2087 HWUI2462ms≥2000ms）；critical/LMK/kernel0不消除此失败 |
| 全W/E/F及逐类归属 | （字体/隐藏测量修正前原时点未填，非本轮结论） |

（字体/隐藏测量修正前原时点未填，非本轮结论）。

## 实现与验收边界

已有九键英文入口、手动语言记忆与临时编辑器强制模式、组合保留、可见IME Region/own-scroll、SAF完整性、日志IO/取消、af23上游resetShift及Board同实例Skip/cache保留实现保持。新a956将refreshModifier各个清除调用置于短路运算前以处理全部临时状态；Sym使用META_SYM_ON；only-Shift以其他支持modifier掩码交集为零判定，避免要求所有其他位同时存在的误判。

新隔离fixture构造实际第三方Keyboard/KeyboardView/KeyViews及recording listener，通过普通click与long-release callback记录38次a/b动作和真实modifier/锁定状态。它检查一次性多modifier消耗、各modifier临时/锁定/解锁、Shift与其他modifier组合、已有Shift锁定与临时其他位清除。listener不调用native/editor/prefs，onKey/onText路径为错误守卫；这是实际KeyView普通分发控制，不是OS物理指针注入、向Rime提交38字母或一般第三方App文本验收。API35 Debug4已实际产生38分发功能PASS终端，但完整cohort独立失败；不能由该子测推出完整设备或发布通过。Board隔离真实manager的transition也只统计回调（返回null），没有动画性能测量。

当前新源Editor SHA e36522b3…e4f267a/151298字节、Keyboard a25d7340…15e424/19427字节与格式化报告绑定。独立源码peer实际通过（modifier-source-peer1/review.json，58fb0386…5776c/8372字节），当前552源精确匹配且无剩余P0源码发现；它只提供源码审阅，不代替编译、实际APK/DEX、设备或发布验收。真实FINGER/native caret/准确一次提交、whole32×32、40rapid/100循环、FIFO、隐私、混输与SAF原断言保留，旧2763ms停顿不据此认定唯一原因。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率与物理16KiB设备未测。Debug fullCheck为维护，不等于每次弹键盘全量编译；rapid墙钟回归不代表P95≤100ms。系统/框架/图形W/E/F及全部FAIL原文保留，critical0不代表warning-free。

## 专用环境、附件与提交边界

（字体/隐藏测量修正前原时点未填，非本轮结论）。API35缺省key保持不存在、stored=null与声明默认false，不写成存储false/本次UI恢复。默认关闭或图形环境准备不提供真实启用TTS/engine teardown/Pico修复、guest内核修复或泄漏结论。

手动公开附件仅两个Release APK与完整递归子模块源码包；编译CODE、仅七文档DOCHEAD、source SHA/bytes及发布/Latest外部分别绑定，避免归档自引用。

| 本轮Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | 11786769 | `a6c9cf517a0c95cebfe1d3639d7152fd0c2bf86df9231bbdcde6ebcf864f3fff` |
| x86_64 | 11865934 | `dbd98b7c7d1ed1a761b84dccf7da8595f5e84e84b53bac98ca3dc397fc0f4023` |

## 本轮已闭合事实

### FullBuild11实际闭合

原summary671d7234…fbfb3b/2679字节绑定当前a956、两phaseexit0及五APK；controller自然wait0，root闭合复核c1a1cfd0…554936/441字节记录heavy0、fiveAPKs重hash与552源前后相同。两个原raw log SHA分别557b7d4e…f0365/8068f972…969b，复用原builder实际正则重核warnings=[]，两Lint原XML均0。命令、BuildConfig、JUnit原XML及五APK SHA/字节数详见facts，未重跑测试或构建。应用663项的task本轮执行，17+6只消费缓存报告。

552源before/after SHA2bc20587…ee0bf7完全一致，当前源逐项与CODE/递归clean匹配。实际Release双ABI SHA和字节数已填入上表；三Debug/test APK仅为验收输入，不是手动发布附件。static12现已另行实际闭合，完整DEX/签名语义接受见下节；四组设备及publication仍未接受。

### host14实际闭合

host14从冻结host13仅替换OUT、CODE及三个guard文件名，Mixed SHA7dbd…a663885不变；逆替换后原字节完全相同，206/552/86与五套检查守卫未降低，独立只读派生peer无P0。实际纯Python进程4152020（同PGID/start27393802）自然wait0、stderr为空；五套原commands/logs、virtual29 guard、当前552源码和86宿主逐项重核。源before/after SHA2bc20587…ee0bf7相同且精确匹配Build11source-before。它不提供新38记录Android/manager设备回归、完整构建/static/四组或发布通过。

### API35 Debug4功能通过、独立门槛失败

本轮a956/FullBuild11正式Debug4 producer五探针全部功能P，owned VM4168075于2026-10-10 12:02:38.525942 UTC正常stop/wait0；原controller739.208秒/wait0、12:02:42.185218 UTC闭合。原allPIDcritical、actualLMK及guestkernelcritical均0，但独立review7fe1a9bd…98f5d1/624842字节明确FAIL：完整logcat第23977行、19:51:58.614、MainPID2087/TID2098、I HWUI Davey duration2462ms/Flags1，超过既有2000ms门槛。该信息级停顿保留在完整raw，不能因不计W/E而漏掉；此前截断片段不足以宣称本轮首帧低于2秒。

editors原terminal实际通过100次show/hide、resident窗口缓存/重复attach回归及38条真实隔离KeyView callback记录；40rapid两theme各20，trime最大373ms/216ms，Tong最大177ms/69ms，只为含队列/main/polling的instrumentation墙钟，非候选P95或动画耗时。T08原8项/320 FIFO、privacy16项/3schema及T09功能通过；API35 SAF29项为隔离provider fixture，不能替代API21真实系统tree grant/restart/revoke。38分发不会native/editor提交或写prefs，不是一般第三方App/OS物理触摸验收。

全4784 W/E（W3596/E1188）及完整停顿原文保留，selected项目诊断0不消除独立F。原review/raw绑定已读取重hash；完整warning失败裁决97b7156e…57202/738280字节现已闭合，656条应用进程框架W/E、4128条其它/boot、项目发出诊断0均原样保留。40条commands/282raw/五probe原件与当前五APK、552源冻结绑定，完整cohort与publication保持false，另外三组尚未运行。后续同Build11冷采样trace仅诊断校准，不豁免失败、不靠随机重试挑P；准确CPU/GPU瓶颈及唯一原因尚未证明。本轮Full11/static12/host14仍各自通过，旧Full10/9失败及旧2763ms失败保留。

### Debug4完整警告失败与源快照范围

首帧2462ms仍是实际独立FAIL；正常的一次attach及两次Skip早退上下文未出现原重复detach/attach，但不能由此声称冷停顿被消除。HWUI字段分段为wall intervals，没有采样CPU/GPU工作或唯一瓶颈证明；真实WebView创建晚于首冷帧，不能把后加载WebView当作此首帧原因。八份Android15官方源仅解释消息分支，未证明与此emulator build12368160二进制等价。全部框架/其它警告、信息级stall和原F保留，critical/LMK/kernel0不豁免。

source-snapshot2/receipt.json（602a32a1…84aca4/350678字节）实际复制552源与compiled before/after完全一致；其中15个tracked asset/wrapper symlink分别记录原link target、实际resolved源路径和复制字节SHA，没有假装它们是原始普通文件。首次通用raw binder严格拒绝symlink，attempt1原exit1/false（4daec900…ae457a/938字节）、44个部分复制文件及当时未生成裁决保留。后续单独解析源目标只修正离线源快照绑定，不改cohort/verdict/log或放宽通用raw文件守卫。当前同Full11冷采样trace为诊断，不纳入publication、四组通过或首停顿原因结论。

### static12实际双APK闭合

verification e04e82b6…1fe02/58005字节与manual7827caf7…90b24/11637、fixture controls6cb8a680…9d89d/26575及实际模板绑定b42d33f3…88312/24235精确绑定当前a956/FullBuild11五APK/552源。双完整classes.dex SHA c60aa462…2eedb/4136996字节相等，4936类定义与21796 method IDs为本轮实际数，不借旧4936/21796结果。18份新smali重读Main、API21/35 Handler、26/27 prehandler、合并InlineList异常传播及delay/resume/cancellation分支；全部ServiceLoader方法引用与SPI服务资源为0。

双Release实际min21、non-debuggable、v1/v2签名验证且warnings=[]；Release manifest/完整DEX/R8mapping无EditorProbeActivity，当前Debug正例实际classes16.dex，三个内存损坏负例均拒绝且不改APK。原inspection11/mapped12 collector的passed=false/coroutine_semantics_reviewed=false保留；独立static12接受不改写原collector，不提供四组设备、物理性能或发布通过。

### 本轮源码与工具准备

源码peer绑定提交a956、两修改文件、552源清单与实际依赖/SDK证据；38条KeyView记录在API35 Debug4实际功能通过，完整设备终态仍被首帧停顿门槛拒绝。该源码peer收据内编译/static/runtime/四组和发布标志均为false，独立的后续构建/static接受分别绑定各自实际收据。static12原准备时收据eef5ba7f…627c32/9058字节保留不可通过的class=-1、method=-2与未绑定SHA守卫，未执行的模板原字节保持；该准备收据不提供接受，新实际接受另由后续static12收据绑定。

runtime12派生收据c8cc7281…2199/1913字节仅将默认build10路径改为build11，功能、冻结输入、全警告、owned停止及selected stall守卫不变，没有新cohort结果。Build11正常设置UI派生399960f8…4334/2206字节绑定新helper487f9684…ab06a/14303字节，保留完整owned/context/crop/observer/Release守卫；它尚未实际执行，不提供默认状态恢复、启用朗读或Pico修复验收。

（字体/隐藏测量修正前原时点未填，非本轮结论）

<details>
<summary>a956 modifier修正前doc-draft10/db5/FullBuild10失败及更早完整原记录（历史，不供新候选验收）</summary>

以下旧原文中的“本轮/当前/待闭合”属于旧时点，全部PASS/FAIL/raw保留。原未填占位只转换为历史标记，不填入新验收表。

# v3.3.13-t9.12 验收报告

（modifier修正前原时点未填，非新候选结论）

（modifier修正前原时点未填，非新候选结论） 同实例Board Skip与缓存保留修正CODE为 `db5ba06830c4d7eb68b5e407b3adc4678f53f0d0`（普通parent9c21），552源已快照，host13实际206项闭合通过但只属于旧db5；FullBuild10已闭合FAIL、不可发布，下一modifier修正CODE尚未冻结；static11、全部四组正式模拟器、七文档DOCHEAD、源码归档及发布绑定均待实际闭合；旧CODE9c21/host12及FullBuild9的Debug先行通过仅保留历史，完整FullBuild9为失败，不供新候选验收。

| 检查 | 本轮最终事实 |
| --- | --- |
| 新CODE / 上游整合 / 源输入 | db5普通parent9c21，包含已合入af23c4a6；552源快照与Build10原source-before一致，相对9c21仅BoardWindowManager及EditorLifecycleProbe变化，共151行新增 |
| FullBuild10 Debug / Release | 旧db5 FullBuild10完整FAIL：Debug393.266秒/exit1，Root因新增已确认modifier问题停止owned daemon；无APK，未执行Release或形成静态/设备通过；源前后相同 |
| 应用JVM / convention / compatibility | （modifier修正前原时点未填，非新候选结论） |
| 宿主host13与源输入前后 | host13五套原有Python检查128＋31＋13＋5＋29=206实际通过；552源与86宿主前后相同，CODE及递归clean保持，owned4143134自然wait0 |
| Spotless / Lint / 构建警告 | （modifier修正前原时点未填，非新候选结论） |
| 双APK静态 / v1v2签名 / 实际协程语义 | （modifier修正前原时点未填，非新候选结论） |
| API21 Release | （modifier修正前原时点未填，非新候选结论） |
| API21 Debug与真实SAF三阶段 | （modifier修正前原时点未填，非新候选结论） |
| API35 Release | （modifier修正前原时点未填，非新候选结论） |
| API35 Debug全探针 | （modifier修正前原时点未填，非新候选结论） |
| 全W/E/F及逐类归属 | （modifier修正前原时点未填，非新候选结论） |

（modifier修正前原时点未填，非新候选结论）。

## 实现与验收边界

已有实现涵盖九键英文入口、手动语言记忆与临时输入框模式、组合保留、主IME可见触摸Region与自身滚动坐标、SAF不完整列举拒绝、日志导出IO/取消传播；9c21已合入上游af23c4a6并将resetShift放入可达的已接受中文分支。新候选仍须在新APK保留真实FINGER/native caret/准确一次提交、whole32×32目标、40rapid/100循环、FIFO、隐私、混输与真实SAF全部断言。

db5在同一resident对象重复cache与同一current窗口重复attach两处立即return，保留原缓存View，避免重复detach/attach、颜色/transition回调及广播。EditorLifecycle新增149行，调用真实BoardWindowManager及一次性Android FrameLayout，核对原View身份/parent、生命周期/颜色/transition/广播计数、lazy创建、不同窗口真实切换、显式颜色刷新、缓存复用及错误key占用拒绝。该隔离fixture运行于Android主线程；transition返回null只统计调用，不是实际动画耗时或物理性能测试。db5源码已提交/格式化，但该新回归仍未设备运行；后续FullBuild10因另行modifier修正中断，不能提供构建/static/四组通过。旧HWUI2763ms停顿失败保留，不把窗口路径问题证明成其唯一原因。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率及物理16KiB设备未测。Debug fullCheck属于维护，不代表每次弹键盘全量编译；rapid墙钟回归不代表P95≤100ms。全部系统/框架/图形W/E/F及原FAIL保留，critical0不表示warning-free。

## 专用环境、附件与提交边界

（modifier修正前原时点未填，非新候选结论）。API35缺失key必须记录不存在、stored=null与声明默认false，不写成两项实际存储false或本次UI恢复。默认关闭及图形环境准备不提供真实启用朗读/engine teardown/Pico修复、guest内核修复或泄漏结论。

手动公开资产仅两个Release APK及一个完整递归子模块源码包；编译CODE、仅七文档DOCHEAD、source SHA/bytes及发布/Latest外部分别绑定，不引入归档自引用。

| 本轮Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | （modifier修正前原时点未填，非新候选结论） | `（modifier修正前原时点未填，非新候选结论）` |
| x86_64 | （modifier修正前原时点未填，非新候选结论） | `（modifier修正前原时点未填，非新候选结论）` |

## 本轮已闭合事实

### host13实际闭合

host13仅从host12替换OUT、CODE与三个guard文件名，Mixed源SHA保持7dbd0a85…a663885；逆替换后原字节完全相同，206/552/86和五套门禁未改。独立派生源审无P0，实际纯Python子进程4143134（同PGID/start27321304）自然waitexit0、stderr为空。五套原commands/logs、virtual29、当前552源码及86宿主逐项重核。source before/after SHA aa2233b0…203977相同且精确匹配FullBuild10/source-before；本结果不提供新增manager Android回归、实际动画性能、构建或发布验收。

（modifier修正前原时点未填，非新候选结论）

## 附录：旧db5 FullBuild10中断失败与下一modifier修正

FullBuild10原summary于2026-10-10 11:16:03.703799 UTC闭合FAIL，SHA29bba54c…52049d/411字节。仅Debug阶段393.266秒/exit1，artifacts=[]，未产生新APK/Release、完整JVM/Lint或静态验收。builder的552源before/after均为aa2233b0…203977；原outer controller实际394.441秒/waitexit1、finished11:16:03.861994 UTC和正常failedfinally闭合均保留。

Root于11:16:02.149637 UTC核实owned daemon4141992的exe、cmdline/start和四级live ancestry后SIGTERM，先停止重任务再改源。stop a935121e…4399d2/1781字节与root-closure-review1绑定原summary、source、实际childwait；闭合时heavy0。先前过严NUL cmdline断言在发送signal/写receipt前拒绝的历史也保留。该停止针对已新审出的第三方modifier问题，不写成OOM、Trime崩溃或完整构建通过。

旧db5/host13实际206P与static11尚未接受、新四组未启动的状态冻结为历史。下一源码拟补齐多临时modifier清除、Sym meta/keycode分工及only-Shift组合判断，并加入真实第三方Keyboard隔离回归；尚未冻结新CODE或实际设备通过。不得据此认定旧HWUI2763ms唯一原因；本轮不派生新文档目录或host14，等待Root新的实际CODE通知。

## 附录：FullBuild9 Debug通过、Release被明确中断

旧CODE9c21/FullBuild9于2026-10-10 11:03:12.035798 UTC闭合FAIL，source before/after相同。Debug641.053秒/exit0：应用663项JVM实际执行、convention17及compatibility6缓存报告均无failure/error/skipped，Spotless通过、DebugLint0、warnings=[]；三Debug/test APK只作原产物留存。

Release229.252秒/exit1，未产生本轮Release APK、签名/静态或四组通过。Root于2026-10-10 11:03:11.187347 UTC核实exe/cmdline/start/parent链后对owned GradleDaemon4135422发SIGTERM，先终止活动构建再改源；原stop收据和daemon disappeared日志保留。不能把它写成OOM、Trime崩溃或完整构建通过。此次停止为另行修复已经审出的窗口路径问题，不证明旧2763ms的唯一因果。normal-finally闭合复核c9e84b19…c212e4a/1483字节绑定原summary、实际controller childwait、root-stop及两源清单，仍full_build_acceptance=false。

<details>
<summary>Board管理修正前CODE9c21/host12及更早完整原记录（历史，不供新候选验收）</summary>

以下原文保持旧时点范围；“本轮/当前/待闭合”指各自旧CODE。所有旧APK/PASS/FAIL/raw保留，未填占位只转换为历史标记，不填入新候选验收表。

# v3.3.13-t9.12 验收报告

（窗口管理修正前原时点未填，非新候选结论）

（窗口管理修正前原时点未填，非新候选结论） 新上游整合CODE为 `9c21e82ba6450140aa9fc74f4cf0bc28540d57be`，552源已快照；host12实际206项闭合通过，FullBuild9进行中尚未接受。static10、全部四组正式模拟器、七文档DOCHEAD、源码归档及发布绑定均待实际闭合；旧CODE3bfb/FullBuild8和先行单组通过完整保留为历史，不能提供新候选验收。

| 检查 | 本轮最终事实 |
| --- | --- |
| 新CODE / 上游整合 / 源输入 | 普通merge9c21的父提交为本地3bfb与上游af23c4a631274d118ed76ae77e6dc8d8f7024c56；552源快照与Build9原source-before一致，相对Full8仅main两文件及Mixed探针变化 |
| FullBuild9 Debug / Release | （窗口管理修正前原时点未填，非新候选结论） |
| 应用JVM / convention / compatibility | （窗口管理修正前原时点未填，非新候选结论） |
| 宿主host12与源输入前后 | host12五套原有Python检查128＋31＋13＋5＋29=206实际通过；552源码和86宿主前后相同，递归clean与当前CODE保持，owned4128976自然wait0 |
| Spotless / Lint / 构建警告 | （窗口管理修正前原时点未填，非新候选结论） |
| 双APK静态 / v1v2签名 / 实际协程语义 | （窗口管理修正前原时点未填，非新候选结论） |
| API21 Release | （窗口管理修正前原时点未填，非新候选结论） |
| API21 Debug与真实SAF三阶段 | （窗口管理修正前原时点未填，非新候选结论） |
| API35 Release | （窗口管理修正前原时点未填，非新候选结论） |
| API35 Debug全探针 | （窗口管理修正前原时点未填，非新候选结论） |
| 全W/E/F及逐类归属 | （窗口管理修正前原时点未填，非新候选结论） |

（窗口管理修正前原时点未填，非新候选结论）。

## 实现与验收边界

既有实现包括九键明确英文入口、手动语言记忆与临时输入框模式、组合保留、主IME可见触摸Region与自身滚动坐标、SAF不完整列举拒绝及日志导出IO/取消传播。此前测试源明确legacy UNSET隔离及原值恢复、真实FINGER到达、API35系统栏安全区；新候选仍须用新APK完成原native caret/准确一次提交、whole32×32应用目标、40rapid/100循环、FIFO、隐私、混输与SAF断言。

上游af23c4a6的resetShift已合入9c21：已接受中文语言切换会复位Shift；通知同步复用现有可达ascii_mode分支，仅接受到中文结果后复位，不让旧中文通知清除尚在队列中的英文Shift。Mixed探针新增受控旧通知及宏返回的原生锁定Shift检查，实际设备终端结果仍待新APK；其fixture modifier setup/direct option callback部分不得说成全部真实外部触摸。源码审阅和完整生成/设备验证分别记录，不能由merge本身推出通过。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率及物理16KiB设备未测。Debug fullCheck属于维护，不代表每次弹键盘全量编译；rapid墙钟回归不代表P95≤100ms。所有系统/框架/图形W/E/F及历史失败按原文保留，critical0不代表warning-free。

## 专用环境、附件与提交边界

（窗口管理修正前原时点未填，非新候选结论）。API35原key缺失必须记录不存在、stored=null与声明默认false；不能写成两项实际存储false或本次UI恢复。默认关闭和图形环境准备不提供启用真实朗读/engine teardown/Pico修复、guest内核修复或泄漏结论。

手动公开资产仅两个Release APK及一个完整递归子模块源码包；APK编译CODE、仅七文档DOCHEAD、source SHA/bytes和发布/Latest由外部manifest/source/publication receipts分开绑定，避免文档归档自引用。

| 本轮Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | （窗口管理修正前原时点未填，非新候选结论） | `（窗口管理修正前原时点未填，非新候选结论）` |
| x86_64 | （窗口管理修正前原时点未填，非新候选结论） | `（窗口管理修正前原时点未填，非新候选结论）` |

## 本轮已闭合事实

### host12实际闭合

host12为run_host11的受限派生，仅替换OUT、CODE、三个guard文件名及新增Mixed源SHA；逆替换字节完全相同，206/86/552和五套原检查守卫未降低。独立source peer核对派生一致；实际纯Python子进程4128976（同PGID/start27197679）正常自然waitexit0、stderr为空。summary、五份原commands/logs、virtual29 guard及before/after清单逐项重核。552源清单SHA ff3ad901…076fa8相同；所有当前552源和86宿主文件逐项一致。本次未运行Java/Gradle/ADB/模拟器，仅提供宿主通过。

（窗口管理修正前原时点未填，非新候选结论）

## 合并前API35 Debug3：功能通过、独立停顿门禁失败

旧CODE3bfb/FullBuild8的API35 Debug3于2026-10-10 10:43:14.069482 UTC闭合，producer五探针功能PASS，owned VM4104490正常停止并wait0，Androidcritical/实际LMK/kernel0。但独立review9保持FAIL：selected Main PID2103/TID2114的原HWUI记录`Davey! duration=2763ms; Flags=1`超过2000ms停顿门禁，review_error为Independent selected Main process stall gate failed。不能写成独立正式组PASS，也不因功能/critical0予以豁免。

完整50243条parsed记录保留；其中5004 W/E（W3816/E1188）按原文留存，上述HWUI I级停顿另外仍在完整raw中，不挤入W/E计数。原producer、独立false、源码合并前快照和停顿原行ending SHA绑定，未删除日志或推断唯一原因、生产bug。其原日志SHA757f4db3…871beb、independent review86eb4dab…779de/623868字节及source snapshot4f1c8e75…ba50ad保留；新上游合并后必须新完整构建与全部四组正式验收。失败warning裁决115e0ecb…75a623/712515字节随后闭合，原项目警告0/selected框架652和全部5004W/E保留，282原文件及40父commands绑定；该停顿没有豁免，CPU/GPU瓶颈未证明。

<details>
<summary>上游新整合前CODE3bfb/FullBuild8及更早完整原记录（历史，不供新候选验收）</summary>

以下原文保持合并前范围；其“本轮/当前/待闭合”属于各自原时点。旧APK摘要与所有PASS/FAIL/raw保留，原未填占位只转成历史标记；它们不能填入新CODE的验收表。

# v3.3.13-t9.12 验收报告

（合并前原时点未填，非新候选结论）

（合并前原时点未填，非新候选结论） APK编译候选CODE为 `3bfb2258f82b501b467c60a3b8f7bfa0a5795fce`，552源输入；当前FullBuild8完整构建、host11实际206项和测试源审阅通过，双Release静态完整性、v1/v2签名零警告及实际生成协程语义审阅亦通过；Debug校准构建及API35五探针校准另在限定范围闭合。四组正式设备和发布仍未提供本轮通过。后续七份文档的DOCHEAD与APK编译CODE、完整递归源码包分别绑定。

| 检查 | 本轮最终事实 |
| --- | --- |
| FullBuild8 Debug / Release | FullBuild8于2026-10-10 10:25:17.096810 UTC闭合PASS；Debug554.898秒、Release722.489秒，552源前后相同 |
| 应用JVM / convention / compatibility | 应用663项本轮实际执行；convention17项和compatibility6项为缓存报告，failures/errors/skipped均0 |
| 宿主检查与源输入前后 | host11实际206项通过（128＋31＋13＋5＋29）；552源和86宿主前后相同，CODE/递归clean前后保持 |
| Spotless / Lint / 构建警告 | Spotless通过；Debug/Release Lint均0问题，两份构建warnings均为空 |
| 双APK静态 / v1v2签名 / 实际协程语义 | 新FullBuild8双Release APK静态完整性、v1/v2签名零警告及实际生成协程语义独立审阅通过；精确绑定当前CODE/552源及两APK，原raw collector的false标志保留 |
| API21 Release | （合并前原时点未填，非新候选结论） |
| API21 Debug与真实SAF三阶段 | （合并前原时点未填，非新候选结论） |
| API35 Release | （合并前原时点未填，非新候选结论） |
| API35 Debug全探针 | （合并前原时点未填，非新候选结论） |
| 全W/E/F及逐类归属 | （合并前原时点未填，非新候选结论） |

（合并前原时点未填，非新候选结论）。

## 实现与验收边界

T09提供明确QWERTY英文入口、手动语言记忆和临时输入框强制模式，数字/符号往返保持组合。预编辑/候选触摸使用主IME可见Region，正确换算自身滚动坐标，保留空白应用区域路由；SAF列举末端检查loading/error完整性，日志导出在IO线程写出并继续传播取消异常。以上生产实现未在本次测试源修正中改变。

legacy T06矩阵快照手动语言偏好的原key存在性、raw和typed值，在新session/编辑器创建前建立UNSET；独立finally精确恢复，不能声称Application尚未初始化。先前CCC已明确两个外部指针路径的id=0/FINGER、实际host DOWN/UP观察及finally清理。当前3bfb只在API35+调试宿主内容容器应用实际systemBars/displayCutout padding，并新增初始完整编辑框位于系统栏安全区的守卫；API21路径及其余FINGER、虚拟键盘恢复、文本重启token、布局、native caret、准确一次提交、100循环和30秒超时断言保持。两测试文件共31行新增，格式化前后字节相同；新source peer仅提供源审，不提供实际校准、完整构建或正式设备通过。

新R8已知协程SPI资源仅在优化后、打包签名前移除；原输入服务与DEX/其它资源不改、保留v1。新APK必须通过版本/原始输入/未知服务/实际加载引用守卫和实际生成协程语义审阅，旧static7不提供新APK通过。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率与物理16KiB设备未测。Debug fullCheck属于维护，不代表每次弹键盘全量编译；rapid墙钟回归不代表P95≤100ms。系统/框架/图形W/E/F及失败原文保留，critical0不代表warning-free。

## 本轮朗读、图形环境与公开附件

（合并前原时点未填，非新候选结论）。API35缺失key必须分别记录key不存在、stored=null与声明默认false，不写成两项实际存储false或本次UI恢复。默认关闭不提供启用真实朗读/Pico/engine teardown验收；历史Pico和guest DMA/kernel失败完整保留，环境调整不宣称内核或内存泄漏修复。

手动公开资产仅两个Release APK及一个完整递归子模块源码包；APK编译CODE、仅七文档DOCHEAD、source SHA/bytes和发布/Latest由外部manifest/source/publication receipts分开绑定，避免源码归档文档自引用。

| 本轮Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | 11786795 | `f57e726488e26c10c22f43a90cb2836dde374979ecf434a8d979a34c3713ea2b` |
| x86_64 | 11865958 | `2dedb705ecc3862d5394161116e160bc114ce45d449e230a79cf778ba2cad5f8` |

## FullBuild8与五个APK冻结

当前3bfb的FullBuild8于2026-10-10 10:25:17.096810 UTC闭合PASS，Debug554.898秒（119 tasks/31 executed/88 up-to-date），Release722.489秒（69/17/52）。应用663项JVM本轮实际执行，convention17和compatibility6是UP-TO-DATE缓存报告，不写686项全部fresh。66份JUnit XML（应用62/convention3/compatibility1）逐项重核tests/failure/error/skipped及实际testcase均一致。SpotlessCheck通过、两Lint均0问题、两原warnings=[]；Release日志实际执行post-R8已优化服务资源移除任务，但签名/DEX/协程语义仍由新静态收据另行验收。

五个产物（二Debug/一test/二Release）的原文件SHA/字节逐项重核，公开表仅列两个Release附件；Debug与test只用于本地正式验收。两BuildConfig均绑定当前CODE/20261117，performance tracing=false，552源before/after SHA dae13251…ce70e2且当前逐项相同，闭合后递归gitclean。完整command、log、BuildConfig、warnings、Lint、JUnit和output metadata已在ignored facts绑定；校准构建8的Debug SHA与新FullBuild8 Debug SHA不同，不能把旧校准APK验收直接算作新正式组。静态9随后已独立闭合通过，四组fresh正式及发布仍未闭合。

## 本轮双Release静态、签名与实际协程审阅

当前3bfb/FullBuild8的独立静态9已闭合PASS，verification.json SHA afd675e3…8585b7/57860字节；manual-semantics.json 951d2ff3…40c6af/11614字节与fixture-inventory-controls.json 9fe2e812…9dfa42/26566字节绑定本轮实际文件。双Release APK v1/v2签名均通过、签名警告为空、minSdk21、非debuggable，证书一致。实际Release manifest、完整DEX及R8 mapping没有Debug-only EditorProbeActivity，当前Debug正控制在classes16.dex确实存在该夹具；三项纯内存越界负控制保留。

两ABI完整classes.dex逐字节相同，SHA a071aff3…293780/4136916字节，4936定义、21796 method IDs；没有SPI残留或ServiceLoader方法引用。新18份实际smali与完整caller覆盖Main Dispatcher、API21/35 Handler、API26–27异常preHandler、R8合并caller、延迟resume/取消等路径。原inspection8/mapped9 collector的passed=false与coroutine_semantics_reviewed=false不改；通过由独立审阅提供。552源、FullBuild8五个APK、原命令与输出绑定重核，未重签/修改APK或执行新的Java/设备命令。

这是静态和生成代码语义通过，device_acceptance=false；不能据此代替API21/API35 Debug/Release四组正式运行、真实SAF持久授权三阶段、物理P95/PSS或真实开启朗读验收。

## 本轮宿主与限定Debug校准构建

当前3bfb的host11于2026-10-10 09:52:29.849697 UTC正常闭合PASS；独立PID/PGID4069341自然wait0，stderr为空。五套原Python检查128＋31＋13＋5＋29=206实际通过，552源码与86宿主清单前后相同且逐项重核，CODE及递归clean保持。summary SHA cc1c61a7…93ad5/3773字节；552源清单前后SHA dae13251…ce70e2。它不提供APK/设备或发布验收。

仅Debug校准构建8于2026-10-10 09:50:52.664789 UTC闭合限定PASS，259.78秒；实际SpotlessCheck、663项应用JVM及两Debug/测试APK三附件，failures/errors/skipped为0、warnings=[]、552源前后相同。命令没有Release或Lint，full_build_passed=false/release_built=false；这不是完整FullBuild8或静态/设备通过。API35校准3随后以限定范围闭合，详细结果如下；其他正式四组仍须分别闭合。

## API35五探针校准3：限定范围闭合

当前3bfb/Debug校准构建8的API35校准3于2026-10-10 10:03:32.931106 UTC闭合PASS，owned VM4068850在10:03:28.228084 UTC正常停止并wait0；它是calibration_only，FullBuild8/Release/Lint/四组正式/发布接受标志均false。独立仅文件审阅收据api35-calibration3-independent-peer1/review.json（SHA50307d8f…2e5a24/89163字节）重核26冻结输入、552当前源码、43父命令及86输出、287原文件、五子探针终端/身份/本轮Debug与test APK。同boot/API35/x86_64，五探针PID2106/2417/2493/2632/3107均UID10148；未执行新的设备或构建命令。run_runtime内部ADB调用未逐项记入这43条父命令，不能把父命令数写成全部ADB操作数。

编辑器初始完整输入框系统栏安全守卫实际通过；两主题真实preedit caret、floating候选准确一次提交（400ms重复检查）、实际chat FINGER DOWN/UP、在重置文本前恢复虚拟键盘均通过。两主题各20轮，共40 rapid，overall show最大165ms/clear95ms，均低于2000ms回归门槛；不是正常Release候选P95。144个touch阶段逐条同序匹配父/子raw与PID2106，保存metric与terminal嵌入JSON完全一致。100次show-hide的10…100进度及之后组合/取消保留检查均终结。

T08通过8项实际快照/分页检查、49候选菜单和320个并发FIFO字母/退格按键。隐私通过16项检查，覆盖3个启用方案，不称48项独立检查。混输14固定任务＋8普通回归＋4rapid＋4macro共30项通过，实际最终文本与各target一致，manual偏好恢复/新编辑器重新绑定及cleanup_errors=[]确认；成本改善和物理触摸性能不是本校准结论。SAF通过29项隔离provider检查（24故障/4本项目provider安全守卫/1create-replace-readback），包括production末端late-loading/error拒绝；它不提供真实系统picker目录持久授权、重启、撤销三阶段接受。

全设备50589条原记录及5个buffer标题中5238 W/E（W4037/E1201）逐条PID/TID/tag/message/rawline与原audit一致，0删除、warning_free=false；五selected probe的645条W/E也保留，不将框架或受控provider异常统一过滤。AndroidallPIDcritical、实际LMK和保存kernel审计critical均0，仅为该cohort观察。没有正式警告裁决、启用真实朗读/engine teardown/Pico修复或真机P95/PSS验收。先前CCC校准1/2及更早所有FAIL/raw继续保留；FullBuild8和static9随后已独立闭合通过，四组fresh正式仍须分别通过；本校准不提供它们的通过。

## 附录：本轮API35 Debug2启动失败

当前3bfb/FullBuild8的首次API35 Debug2启动保持FAIL。实际父命令先devices、wait-for-device返回0，随后第2号adb root返回1，stderr为`adb: unable to connect for root: closed`；没有boot-ready、APK安装或任何输入探针。owned VM4103233在2026-10-10 10:29:38.010522 UTC精确停止并wait0。独立闭合失败收据api35-debug2-startup-failure1.json SHA39b4b1dc…4c67dae/14763字节保留原false、全部三命令与原输出。

没有Android full log，allPIDcritical与实际LMK未知（null）；原host kernel scanner计数0不证明guest kernel日志覆盖。保留实际emulator host警告和未知连接关闭原因，不归为Trime崩溃，也不提供内存/设备健康通过。后续同CODE/同APK的新运行仅在其完整闭合及独立裁决后接受；当前仍无四组正式通过。

## 附录：CCC校准失败与API35系统栏几何前置

CCC/source552的host10于2026-10-10 09:32:10.803808 UTC实际206项通过，仅为该旧CODE宿主检查。API35校准1仍闭合FAIL：trime20 rapid最大503ms/160ms及hardware候选提交先行通过，但真实chat FINGER DOWN/UP到达守卫30秒超时；后续主题、100循环及其它探针未完成。3146条W/E（W2103/E1043）完整保留，Androidcritical/LMK/kernel0不改整体失败；限定失败收据c16fb8d7…3ce476/26132字节保留旧三APK与原false。

同CCC/API35校准2的窄诊断也保持FAIL。原输入最近队列记录DOWN/UP(540,36)，Activity/chat与StatusBar的实际屏幕区域均为[0,0][1080,72]，窗口带EDGE_TO_EDGE_ENFORCED；这确认点击几何前置未避开状态栏。仍未采样实际收件窗口/唯一超时原因，不宣称生产bug。八条宿主观测处于before_host_virtual_return到停止之间，非原子快照。PNG先于uiautomator，后者输出8字节Killed\n而无可用XML；独立诊断进程2136因UiAutomation already registered记录fatal，不能因非IME PID过滤为0。全部2725 W/E（W1927/E798）保留；kernel审计字段缺失为null，不能写kernel0。原失败收据2d8e2408…da6ef9/44155字节与所有原命令保留。

Android15官方PhoneWindow及文档说明target35+默认edge-to-edge，NoActionBar不豁免；强制时decorFits=false，setDecorFitsSystemWindows(true)被忽略。旧宿主首chat固定72px且无inset处理。官方URL/hash/关键行与原cal1证据见api35-edge-insets-review1/review.json；它结合cal2证实测试几何问题，但官方源审本身不提供设备通过；随后cal3在限定范围完成实际FINGER/virtual恢复。当前3bfb的完整构建、静态和宿主分别已通过，四组正式仍须独立验收，不能借旧F的先行子测晋级。

## 附录：API35 Debug1失败与官方指针元数据契约

旧CODE2895/FullBuild7的API21 Debug9、API21 Release9和API35 Release1三个正式单组分别通过，但API35 Debug1于2026-10-10 09:14:39.921 UTC闭合FAIL；整体稳定晋级停止。这些通过和失败仅保留历史，当前CODE3bfb须新完整构建/static/宿主/全部四组重验。

API35 Debug1主进程2131完成trime首主题20 rapid（show最大226ms/clear65ms）及真实hardware候选准确一次提交、400ms稳定检查。到达before_host_virtual_return后等待30秒超时；文本reset与restart-token阶段在该predicate之后，未执行。71条先行PASS和68个phase仅子集，Tong、100循环及之后T08/privacy/T09/SAF未验收。原producer false、owned4038180 wait0及Androidcritical/实际LMK/kernel0保留；这些零值不升级整体失败。失败限定收据api35-debug1-failure-context.json SHA1645d18b…3b7ee0/204655字节绑定112原文件和40条实际命令，原raw不删改。

官方Android15源码确证：simplified MotionEvent.obtain清空PointerProperties为TOOL_TYPE_UNKNOWN，再仅设置id0；setSource(TOUCHSCREEN)不改变toolType。ViewRoot在ACTION_UP保存toolType，IMM show传给server；IMMS在useHandwritingListenerForTooltype=false分支过滤UNKNOWN，true分支有不同设备路径。标准TextView和session仍有viewClicked转发，不能因deprecated断言本次必定没有旧回调。候选mode偏好恢复本身不调用输入设备重算；失败复合predicate未记录各分量、guest实际toolType/回调或flag值，故不证明唯一运行原因或生产bug。

官方完整源码URL/hash/关键行及原失败/source绑定在api35-pointer-metadata-review1/review.json SHAb1a11d2f…2bc48c/90036字节；API21源码确证同public PointerProperties重载可用。它说明测试事件元数据需要修正，不把旧F改成P，也不证明新注入已在设备通过。新commit ccc53433只改EditorLifecycleProbe（43新增/4删除，文件9786e8a9…6df9fe9/131945字节），source peer bd346bf6…c4d064/5383字节保留生产源码无变、更强实际到达断言与原清理/时限。

### 旧API35 Release1单组的准确范围

同旧CODE2895/FullBuild7的API35 Release1于2026-10-10 09:11:46.288 UTC闭合单组PASS；两主题6轮/54原PNG/XML，1754条实际命令及4033个raw文件、runtime/警告裁决/单组companion各自通过。全部6561条W/E（W5248/E1313）保留；选中进程项目0/框架25，其余6536。框架25为ART8/HWUI3/Back2/Binder12；Binder与六次实际am start -S的独立fixture冷启动边界按原command/上下文记录，调用者未知，不泛化为一般输入无警告。

只读快照16项，speak_on_keypress、speak_on_commit及manual_language_mode均不存在，stored=null；朗读声明默认false生效，不能写两项已存储false或手动native。这次没有恢复switch动作，不复用旧API21恢复。它仅为旧单组通过，新CODE仍需fresh四组，不供发布整体通过。

<details>
<summary>CODE2895/FullBuild7及更早完整原记录（历史范围，不供当前CODE3bfb验收）</summary>

以下保存doc-draft6全部验收原文。原时点占位仅在历史展示中展开为“原时点未填”；原doc-draft6文件/摘要与所有PASS/FAIL/raw保持。原“本轮/当前/待闭合/Latest”均指各自旧时点，旧APK摘要不供新候选。

# v3.3.13-t9.12 验收报告

（原时点未填，非本轮结论）

（原时点未填，非本轮结论） APK编译CODE为 `28953396cc09d224b07015f83fd52c37254ff428`，552源输入；后续仅七份文档形成DOCHEAD，APK、文档提交和完整源码归档身份分别由外部收据绑定。

| 检查 | 本轮最终事实 |
| --- | --- |
| FullBuild7 Debug / Release | FullBuild7 Debug/Release已闭合通过，Debug588.716秒、Release730.806秒；应用663项JVM测试本轮实际执行，convention17项及compatibility6项为缓存报告；host9共206项实际通过；Spotless通过，两Lint0问题，两份构建warnings均为空 |
| 应用JVM / convention / compatibility | 应用663项本轮实际执行；convention17项和compatibility6项为缓存，报告failures/errors/skipped均0 |
| 宿主host9 | 206项实际通过（128＋31＋13＋5＋29）；552源与86宿主前后相同；CODE及递归clean前后保持 |
| Spotless / Lint / 构建警告 | Spotless通过；Debug/Release Lint均0问题，两份原warnings.json均为空 |
| 新双APK静态 / v1v2签名 / 协程语义 | 本轮双Release APK的静态完整性、v1/v2签名零警告及实际生成协程代码独立审阅通过；精确绑定CODE2895/552源及新ARM64/x86_64附件，原raw collector的passed=false保持 |
| API21 Release9 | 本轮Release9正式单组PASS：真实9.11覆盖升级、两主题6轮/54原PNGXML；独立runtime、警告裁决及单组companion通过 |
| API21 Debug9与实际SAF三阶段 | 本轮Debug9正式单组PASS：七探针与真实SAF roundtrip/restart/revoke全部终结；独立runtime、警告裁决及单组companion通过 |
| API35 Release1 | （原时点未填，非本轮结论） |
| API35 Debug1全探针 | （原时点未填，非本轮结论） |
| 全W/E/F及逐类归属 | （原时点未填，非本轮结论） |

（原时点未填，非本轮结论）。

## 实现与验收边界

T09提供明确英文入口、手动语言记忆和临时编辑器强制模式，数字/符号往返保持组合。预编辑/候选触摸使用主IME可见Region，换算自身滚动坐标并要求32×32应用空白目标可达、真实指针和native caret/准确一次候选提交；没有空触摸Popup。SAF在目录列举末端检查loading/error完整性；日志导出在IO线程写出、取消异常继续传播。

旧T06矩阵现在先快照手动语言偏好的原key存在性、raw和typed值，在新session/编辑器创建前建立UNSET；独立finally即使其他清理失败也精确恢复原状态。原生ASCII、布局、真实触摸和100循环断言保留；真实用户语言记忆/FIFO由既有MixedInputProbe验证，不把native-only setter称为用户显式选择。

新R8产物的已知协程SPI资源仅在优化之后、打包签名之前移除；原输入服务不提前过滤，DEX/其它资源不改，版本/输入/未知服务/实际加载引用守卫及完整实际生成语义须由本轮静态收据证明，保持v1签名。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率与物理16KiB设备未测。Debug fullCheck属于维护，不代表每次弹键盘全量编译；rapid墙钟回归门槛不代表P95≤100ms。系统/框架/图形W/E/F及失败按原文保留，critical0不代表warning-free。

手动公开资产只包含两个Release APK和递归子模块源码包。公开下载、tag/Latest、DOCHEAD、完整source SHA/bytes及发布收据在外部manifest/source-receipt/publication-receipt记录，避免归档文档自引用。

## 专用AVD朗读与图形环境范围

（原时点未填，非本轮结论）。恢复默认false/false仅为环境准备，不是Pico修复或开启真实朗读/引擎teardown验收；先前Pico SIGSEGV原trace保留。GLDMA/GLDMA2环境调整也不声明guest kernel修复、Trime泄漏修复或物理性能通过。新四组cohort须绑定各自原设置、图形配置和完整W/E/F，旧范围不供本轮通过。

## FullBuild7与APK文件冻结

本轮完整构建于2026-10-10 08:21:04.640579 UTC闭合PASS。Debug588.716秒、119 tasks中31实际执行/88缓存；Release730.806秒、69 tasks中17实际执行/52缓存。应用663项实际执行，convention17项及compatibility6项为UP-TO-DATE报告；各报告failures/errors/skipped均0。两phase各自的完整command、log、BuildConfig、空warnings和Lint XML已绑定；Debug66份JUnit XML逐项重核，实际Release执行post-R8服务资源任务。

五个产物包括两Debug APK、一个测试APK和下列两Release APK，均按原文件重核SHA/bytes；Debug/test APK仅用于本地验收。性能tracing=false，552源before/after与host9逐项一致；闭合后递归gitclean。签名、DEX及实际协程语义由本轮独立静态收据另行绑定，正式设备范围见四组验收表。

| Build7 Release APK | 字节数 | SHA-256 |
| --- | --- | --- |
| arm64-v8a | 11786783 | `b17d17a52742120a5b01f8fac7da22f2282ec7b0d7f37403e82dc59b4545a393` |
| x86_64 | 11865947 | `c969a6a1bb5cc922e3dc9ea2d7e23cb5da902c2d557b8ed16d31e0ca34f8cedd` |

Build7/summary.json SHA为`3d45bf6a…f70a9d`，Debug/release原log分别`0d522ad5…3c11b1`/`621a56f6…bf036`；完整绑定与552源摘要记录于ignored facts，旧FullBuild6附件保留在历史范围。

## 新双Release静态、签名和实际协程审阅

新static7收据verification.json（SHA05999e1b…87ffa，57860字节）及manual-semantics/fixture-controls精确绑定CODE2895、552源、Build7与当前两APK。两ABI的完整classes.dex为4136916字节、4936类/21796方法ID，字节等价摘要9825848f…edc87a4。18份fresh mapped代码和两ABI六核心输出覆盖实际Main/provider初始化、API21/35 Handler分支、API26/27 preHandler、R8合并caller、延迟resume与cancel；实际method IDs没有ServiceLoader或Class/ClassLoader资源查询，正常AssetManager.open仍存在，已知SPI服务条目为空，签名v1/v2各0警告。

实际新Debug的classes16.dex存在专用EditorProbeActivity；两Release的manifest、全DEX类定义与R8 alias均无该夹具。三项实际字节的内存越界负控被拒绝，未修改原APK/source。原inspection/mapped collector的passed=false与semantics=false保持，由独立实际代码审阅提供静态通过；不借此计设备或发布通过。收据必要字段及所有嵌套原文件摘要已另外重核，完整列表在ignored facts。

## 已闭合的本轮宿主检查

host9于2026-10-10 08:00:19.876244 UTC闭合PASS，206项实际执行；source-before/after SHA均为`e9496316…b256f9`。host9/summary.json SHA为`7d333608…d715fb`；五套原日志、命令和完整输入摘要见ignored facts。未运行Java、Gradle或设备，不能替代正式APK/四组验收。

## API21 Debug9正式单组验收

新CODE2895/FullBuild7的Debug9于2026-10-10 08:40:59.567568 UTC闭合PASS，owned3974168由原runner停止并wait exit0，Androidcritical/实际LMK/kernel均0。七个探针进程3585/3852/3931/4053/4599/4707/4813的身份、原terminal/report和本轮Debug/test APK逐项绑定。独立runtime、逐条警告裁决与单组companion均通过；该收据仅提供本组正式验收，各组及整体发布的最终范围见报告首表。

editors完成真实指针、native caret/准确一次候选提交、输入框/硬件/WebView矩阵、两主题40 rapid和100次show-hide；rapid show最大353ms、clear251ms为包含队列/main/polling的功能墙钟，门槛2000ms，不是候选P95。T08完成8项快照/分页及320个FIFO按键；T07完成16项持久化/学习/剪贴板/日志/恢复检查、3已启用方案；T09两主题14固定任务、8普通/4rapid/4macro回归全部PASS，真实用户语言记忆与legacy UNSET矩阵分别覆盖。

SAF通过真实DocumentsUI选定新UUID目录并验证持久读写授权，随后roundtrip/restart/revoke三阶段实际终结；撤销后拒绝访问。两个导入失败负例进程4599/4707各保留65条预期阻塞目标目录诊断，共130条，原断言确认目录、无关sentinel和sync-history保护及清理。它们按UUID/真实异常/terminal范围裁决，未删日志或泛化到未知异常。

全部2763条W/E（W1862/E901）保留，warning_free=false；434个closed原文件和父50条命令以及全部输出摘要已重核。框架诊断总120=实际probe进程95＋preprobe25，项目诊断总131=SAF受控负例130＋preprobe维护1。七条inactive原文保留：四条T09启动诊断位于Trime activation前1758/1398/1388/1356ms；finishComposingText位于activation后758ms及onStartInputView后88ms，单独记录；两条showStatusIcon位于onFinishInputView(false)后5/6ms，缓存API21源码的对应标签为reportFullscreenMode。未捕获实际Binder调用栈，调用者和唯一原因仍未知，不写成已消除。

preprobe PID2462的4371ms WrappedRunnable等待跟在SAF改树后的fullCheck=true维护redeploy之后，距editors父启动前8407.943ms；更早已有fullCheck=false native启动，native目录仍为app-scoped mirror。因此不称全程首次部署、连续输入候选延迟、全部队列无等待或真机P95。早期进程APK未逐个采样，不猜精确旧APK身份。

当前图形原profile c643f1aa…55f52同boot绑定1536MiB/1核/720×1280、仅-GLDMA/-GLDMA2环境及34个原GL扩展无DMA广告；完整本轮kernel fatal0只证明此cohort观察，不证明二进制与官方源码等价或所有DMA路径修复。SAF picker前后原XML均为speak_on_keypress=false、speak_on_commit=false，manual native保持；默认关闭不提供启用真实朗读/引擎teardown或Pico修复验收，旧native失败保留。

独立收据：runtime review SHA337a17e3…05fea，warning SHA4e800baa…0fbb，single-cohort check SHA82cbc6c1…501c；限定时序source/context peer SHA82e9b6da…c3183。第一次离线warning writer因Chromium完整前缀守卫过严而未生成裁决JSON，原失败与writer字节保留；随后正确匹配原前缀的审阅不改producer/任何raw。此工具历史仅QA记录，不是App失败。

## API21 Release9正式单组验收

同CODE2895/FullBuild7的Release9于2026-10-10 08:56:26.843637 UTC闭合PASS，owned3993693由原runner停止并wait exit0。两个主题各3轮共6轮/54组原PNG/XML绑定同boot、Main2479、UID10053/start5689及本轮x86_64 Release APK；实际n/i按键、native caret、hide/show后的组合恢复、准确一次候选提交和late-stable均通过。真实9.11覆盖升级前后UID、四份原偏好文件和专用sentinel一致，旧/新versionCode20261116→20261117及APK身份分别绑定；升级保留在后续UI配置前完成。

独立审阅与单组companion核验1766条命令/4057个原文件，全部3125 W/E（W1643/E1482）保留。选中Main2479项目tag诊断0、框架6条（SQLite WAL恢复2、EGL fallback1/参数3），其余进程或boot3119；早期进程精确APK身份未单采，不猜旧SHA。Androidcritical/实际LMK/kernel均0，warning_free=false。收据review SHA1e9cb799…635e83、warning SHAcff1f221…cd01b、single-cohort check SHAd53e731c…158b0；仅API21两正式组已通过，API35两组和发布由各自闭合收据另行绑定。

本轮只读defaults-before1快照有48个实际存储项：两SPEAK布尔key真实存在且false，manual_language_mode真实存在且native；helper805213d1…0f68f5没有执行任何switch动作，未发生本次UI默认恢复，不复用旧Release8恢复操作。当前图形profile b7ced69c…183b1e同boot记录1536MiB/1核/720×1280及无DMA广告范围；closed kernel0不泛化为内核修复。默认关闭不提供开启真实TTS/Pico或engine teardown验收。

## 附录：完整实际记录与失败历史

CODE3bb的FullBuild6/static6与API21 Release8单组确实通过，随后Debug6/7/8失败，已停止稳定晋级；它们都不提供本轮CODE2895验收。

### Debug8原生模式与手动记忆前提失败

FullBuild6 Debug8于2026-10-10 07:48:43.805661 UTC正常闭合FAIL；EditorLifecycle原1605断言前只有native `ascii_mode=true`设置，而真实UI/两份SAF偏好快照为manual native，该操作没有建立标签所称的用户显式语言选择。273条先行PASS状态和双主题40 rapid（show最大303ms/clear206ms）只作已到达子测；100循环未完成，之后T08/privacy/T09/SAF阶段未执行。

完整2385条W/E（W1693/E692）、148条命令和468个原文件保留；Androidcritical/LMK/kernel0不升级整体FAIL。独立失败收据runtime-independent-review6/api21-debug8-explicit-mode-failure.json（SHA5f27a66b…87cdb，379803字节）保留原false，不证明生产bug或唯一运行原因。此前中文UI前置限定P不等于完整cohortP。新夹具修复须用新CODE/完整构建/static/四组fresh实际重验。

<details>
<summary>CODE3bb/FullBuild6及更早完整原记录（历史范围，不供本轮验收）</summary>

以下为doc-draft5验收报告的完整原文本；“本轮”“当前”“待闭合”均指旧时点，原PASS/FAIL和所有失败raw保持。旧附件SHA不得用于CODE2895公开候选。

# v3.3.13-t9.12 验收报告

（原时点待填，非本轮结论）

（原时点待填，非本轮结论） APK编译CODE为 `3bb852b6ed02090459a58b3bcd2a58745b42ee53`，552源输入；后续仅七份文档形成DOCHEAD，APK、文档提交和完整源码归档身份分别由外部收据绑定。

| 检查 | 本轮最终事实 |
| --- | --- |
| FullBuild6 Debug / Release | 已闭合PASS；Debug 547.985秒，Release 695.687秒；完成2026-10-10 06:51:41.637990 UTC；552源前后相同 |
| 应用JVM / convention / compatibility | 应用663项本轮实际执行；convention 17项、compatibility 6项报告为缓存，均0 failures/errors/skipped |
| 宿主host8 | 206项实际通过；552源与86宿主前后相同 |
| Spotless / Lint / 构建警告 | Spotless通过，Debug/Release Lint均0问题，两份原warnings.json均为[] |
| 新双APK静态 / v1v2签名 / 协程语义 | 新双ABI静态完整性/签名0警告/实际生成协程代码独立审阅PASS，verification6 SHA b18c37f9…9d72df；raw collector原passed=false仍保留 |
| API21 Release | 本轮Release8正式单组PASS；2主题×3轮、54原PNG/XML，真实9.11覆盖升级；独立runtime/warning裁决及单组companion门禁通过 |
| API21 Debug与实际SAF三阶段 | （原时点待填，非本轮结论） |
| API35 Release | （原时点待填，非本轮结论） |
| API35 Debug全探针 | （原时点待填，非本轮结论） |
| 全W/E/F及逐类归属 | （原时点待填，非本轮结论） |

（原时点待填，非本轮结论）。

## 实现与验收边界

T09提供明确英文入口、手动语言记忆和临时编辑器强制模式，数字/符号往返保持组合。预编辑/候选触摸使用主IME可见Region，换算自身滚动坐标并要求32×32应用空白目标可达、真实指针和native caret/准确一次候选提交；没有空触摸Popup。SAF在目录列举末端检查loading/error完整性；日志导出在IO线程写出、取消异常继续传播。

新R8产物的已知协程SPI资源仅在优化之后、打包签名之前移除；原输入服务不提前过滤，DEX/其它资源不改，版本/输入/未知服务/实际加载引用守卫及完整实际生成语义须由本轮静态收据证明，保持v1签名。

真机正常Release候选P95、Java/native/graphics/总PSS、驻留/首次部署、ARM64/OEM运行、一般准确率与物理16KiB设备未测。Debug fullCheck属于维护，不代表每次弹键盘全量编译；rapid墙钟回归门槛不代表P95≤100ms。所有普通系统/框架/图形诊断及失败按原文保留，critical0不代表warning-free。

手动公开资产只包含两个Release APK和递归子模块源码包。公开下载、tag/Latest、DOCHEAD、完整source SHA/bytes及发布收据在外部manifest/source-receipt/publication-receipt记录，避免归档文档自引用。

## 专用AVD朗读设置与Pico范围

onlyT09期间的只读tts-state1原XML显示speak_on_keypress=true、speak_on_commit=true，默认定义均false；首次开启来源未知。（原时点待填，非本轮结论）。

API21 Release8已通过正常MainActivity启动、真实设置页点击、四次经查看后审核的有界滚动及两个真实开关点击恢复默认false/false。限定独立审阅核对11个闭合操作、456条原命令/912个stdout/stderr文件及18组原PNG/XML/上下文；最终原截图与XML中Voice on key press/Voice on commit均OFF。所有操作绑定同一boot、MainPID2525、UID10053及本轮Release APK，不写私有偏好文件、不clear/uninstall/force-stop或改IME。

偏好XML从16项变为47项：2个既有朗读布尔值true→false，设置页加载时新增31个声明默认项，其余14项既有内容保持不变，无删除；不能写“总共只有两个键改变”。这是升级保留检查完成后的专用AVD环境操作，与升级数据保留范围分开。限定证据在ignored actual-ui-speech-default-restoration-peer.json；其余三个正式cohort的环境/验收仍待绑定，不提供设备或发布整体通过。

这只是测试环境准备，不是Pico修复，也不证明开启真实朗读及引擎teardown通过。校准5系统com.svox.pico的shutdown/deallocate SIGSEGV原trace保留；不猜产品doubleShutdown，不因后续窄scope critical0宣称此前native崩溃已消除。隐私探针保留原内容抑制/恢复与持久化检查。

## API21 Release8正式单组验收

本轮原执行器于2026-10-10 07:17:32.530408 UTC闭合PASS，owned PID3879519停止/等待exit0。独立runtime审阅、逐条警告裁决与发布companion的单组门禁均通过。两个主题各3轮共6轮、54组实际PNG/XML绑定同一boot、MainPID2525、UID10053及FullBuild6 x86_64 APK；真实native预编辑caret移动、hide/show后组合恢复和准确一次候选提交均按原断言通过。

真实9.11→9.12覆盖升级的UID、原偏好摘要及专用sentinel保持；升级检查在后续正常设置UI恢复朗读默认之前完成，两种操作范围分开。独立审阅核验2219条命令的stdout/stderr及5118个原文件；完整日志解析12443条记录，另4行为实际buffer标题，3466条W/E（W1671/E1795）全部保留。选中PID2525项目tag诊断0，框架诊断6条（SQLite WAL恢复2、EGL4）；安装前PID1702另有ART8和WAL2，未单独采样其进程APK身份，不能猜其精确旧APK。critical/实际LMK/kernel均0，warning_free=false。

独立收据为runtime-independent-review6/api21-release8-review1/review.json（SHA345a6b81…90fde）、api21-release8-warning-adjudication.json（SHA2443806d…2bf67）和api21-release8-publication-check.json（SHA41fbd80a…71a5c4）。仅正式API21 Release这一个cohort通过；其余三个本轮正式cohort、四组整体门禁和稳定发布仍待完成，真机P95/PSS范围仍未验收。

## 已闭合的T09限定诊断

CODE3bb在calibration-build6 Debug APK上的仅T09诊断与独立审阅通过：两主题14个固定任务、8项普通回归、4项rapid和4项macro全部PASS，253个原文件、44条命令及3条只读设置命令重验，owned停止/等待成功。全2110条W/E（W1588/E522）保留，critical/LMK/kernel均0。

选中MainPID2584有getTextBeforeCursor和showStatusIcon两条inactive InputConnection框架警告。上下文/官方源码审阅通过仅表示事实绑定，warning_acceptance=false；没有实际调用栈或guest框架字节等价证据，不能指认调用者，也不能宣称警告消除。该诊断使用校准APK，不提供FullBuild6四组正式验收；旧Pico native失败仍保留。

独立范围收据：runtime-independent-review6/api21-t09-diagnostic1-verification.json（SHA dd81c850…19068，95396字节）和api21-t09-diagnostic1-inactive-context.json（SHA f3587d17…3f21，46355字节）；完整摘要及原附件绑定见ignored facts/evidence。

## 附录：完整实际记录与失败历史

以下按其原CODE/APK/scope保留历史；“当前”“新”均指当时范围，不替代本轮完整构建或四组验收。

### 本轮API21 Debug6 / Debug7失败记录

FullBuild6同CODE的Debug6整体失败：editors的40 rapid/100 show-hide、T08的320个FIFO按键和privacy的16项/3方案只是已完成子测；T09有28条局部PASS状态但没有最终terminal/report，SAF三阶段未执行。完整2551条W/E（W1759/E792）保留；Android全PIDcritical0、实际LMK0仍不能抵消guest kernel的order10连续DMA内存分配失败、goldfish_pipe_v2.c:854 BUG和kernel panic，kernel fatal2。独立失败收据runtime-independent-review6/api21-debug6-kernel-failure.json（SHA477c7045…570c4）绑定43条命令和265个原文件。trace不证明host OOM、Trime内存泄漏或确切图形请求客户端，失败不豁免。

后续Debug7使用独立绑定的GLDMA/GLDMA2关闭环境，07:36:51.490180 UTC正常闭合FAIL；本轮kernel/Android critical/实际LMK均0。独立失败审阅绑定43条命令、221个原文件及全2320条W/E（W1722/E598），7项早期状态检查和fallback glyph只作子集P。失败收据runtime-independent-review6/api21-debug7-precondition-failure.json（SHAae3d91a8…82e17a）保留原false。editors的EngineUnavailableGlyph与PopupColor先行子测通过，随后真实九键布局前置等待30秒超时，后续触摸矩阵及T08/privacy/T09/SAF三阶段未执行。本轮失败前两份原偏好XML均保存manual_language_mode=ascii；真实日志15:36:13.372有SchemaMessage，不能声称方案通知未到达。

源码契约中USER编辑器优先按手动语言记忆选布局，而夹具只调用native selectSchema与setRuntimeOption(false)，没有显式用户中文切换。这证明当前九键矩阵前置条件未充分建立；未独立采样失败当下内存键盘/模式，也未证明既存英文偏好的首次来源，因此不把源分析扩大为产品九键失效。下一次同APK重试必须经原截图/XML审核的真实中文切换建立前置状态、保留记忆行为及原九键/native/触摸断言，重新完成正式整个cohort；GLDMA环境调整和此前子测都不替代该通过。

Debug8前的第一次正常UI准备操作也保留其独立失败：旧helper冻结完整owned_host_identity，把父执行器刷新后的volatile proc_stat误判为身份改变，操作报告false；独立fixture已正常启动，但首个capture前即拒绝，原PNG/XML捕获0、没有中文指针动作。另存v2只比较父六项稳定信息并继续双重验证PID/PGID/可执行文件/argv/命令行SHA/启动时间，保留旧helper和失败字节；v2源码审阅通过仅证明此守卫调整，没有代替真实UI基线或设备验收。

随后同APK Debug8的中文基线已由真实UI建立并独立限定审阅通过：原截图/XML的En节点经原完整节点/像素裁剪/上下文审核后实际tap(320,644)，新的原截图/XML显示“中”，完整原偏好仅manual_language_mode由ascii→native，两项朗读值原本与操作后均false。105条命令及210个stdout/stderr、3组原PNG/XML绑定同boot/Main2462/UID10053/start7774及Debug APK；只有前置准备P，不直接证明当前Rime ASCII option、九键布局或后续探针P。原准备F与此前两次正式失败均保留。独立限定收据api21-debug8-chinese-precondition-peer.json（SHA2dda45c7…5b7ca8）不替代正式整个cohort。

Debug8另保存与同一boot绑定的图形环境：仅ANDROID_EMULATOR_FEATURES=-GLDMA,-GLDMA2，仍1536MiB/1核/720×1280；实际启动GL行有34个有效非空扩展且未广告DMA1/2。源机制审阅source-review.json（SHA90998358…5f585d）及本次root-graphics-profile1.json的原boot行/摘要已核验。所引官方源码与guest/SDK二进制未证明精确等价，扩展缺失也不证明整场会话没有任何DMA调用；环境调整不计kernel修复、内存泄漏修复或物理性能通过，Debug8正式全探针仍待闭合。

<details>
<summary>本轮前置与FullBuild2–5等原始验收记录</summary>

# v3.3.13-t9.12 验收报告草稿

**新CODE3bb的source peer与host8=206通过；限定Debug构建calibration-build6通过，设备校准5整体失败、仅隐私诊断原执行器与独立限定审阅通过；完整Build6、static6、四组fresh设备及稳定发布仍待完成。**

CODE `3bb852b6ed02090459a58b3bcd2a58745b42ee53` 的parent是b32，仅更改Debug的EditorProbeActivity和androidTest的PrivacyModeProbe；552源输入键集不变，两个变化均为夹具。privateLogStart之后全部学习/native/plugin/persistence/log/cleanup字节与parent相同。可聚焦TextView窗口加ALT_FOCUSABLE_IM及AlwaysHidden；无extra的原编辑器路径、manifest与资源保持。

host8五套真实轻量检查206＝128＋31＋13＋5＋29通过，552源/86宿主前后相同，CODE/递归clean保持，未运行Java/Gradle或设备。summary SHA `f4f2ab6b9b45e1877a5e3289ebaa5249743b4315bd9f0fa6657e980e21a8495b`；source-before/after SHA `fc502e739cc9b7a88eff458844fb8a5720e39510279056d803f6deab4cfe66b5`。source peer SHA `a10e3d213867bb34ee8de38e7458b480b3369212cb91faf1166c3aa9a66a9fbf`。calibration-build6已限定Debug构建闭合通过，完整Build6及正式设备验收仍待完成。

| 新CODE必需范围 | 实际结果 |
| --- | --- |
| API21 Release | （原时点待填，非本轮结论） |
| API21 Debug（含真实SAF三阶段） | （原时点待填，非本轮结论） |
| API35 Release | （原时点待填，非本轮结论） |
| API35 Debug（完整editors/t08/privacy/t09/saf） | （原时点待填，非本轮结论） |

完整Build6双APK的字节/签名/实际协程语义须重新冻结并审阅。四组独立review与warning裁决均需新CODE/新完整构建APK/boot/MainPID/UID/owned停止及全部原raw绑定；不得沿用Build5或校准APK。CODE与后续仅七份文档的DOCHEAD分开记录，手动公开资产严格两个Release APK加递归源码包。发布、source摘要、tag和Latest仍待实际外部收据。

## 当前限定构建、失败校准及仅隐私诊断

calibration-build6于06:07:58.378495Z闭合通过，278.392秒，663项应用JVM本轮实际执行、Spotless通过、warnings为空。552源前后仍为host8的fc502e73摘要。三个QA APK只供限定校准，未运行Lint或Release；不提供完整Build6通过。

api21-fixture-calibration5于06:13:57.911106Z正常闭合FAIL，原runner停止owned3837360并wait exit0。editors的40 rapid/100 cycles和T08通过；privacy在15秒窗口焦点等待处超时，checks0，具体超时原因未确立。T09和SAF未运行。全raw2116条W/E/F（W1604/E511/F1）保持，SIGSEGV来自系统com.svox.pico的PID/TID2399，14:13:18.856；虽不归为Trime崩溃，all-PID critical1门禁仍失败，不能删掉或改整体P。失败producer闭合未产出最终kernel-audit；后续独立原日志审阅报告kernel0，all-PID SIGSEGV critical1仍使整体失败，不改写原producer。

api21-privacy-focus-diagnostic1随后仅执行privacy，16项/3已启用方案通过；原焦点、ALT_FOCUSABLE_IM、非编辑器和daemon空断言均通过，原学习/native/plugin/persistence/log/cleanup断言保持。原runner于06:19:05.994629Z闭合P，owned3847456停止wait exit0，critical/实际LMK/kernel0；raw1978条W/E（W1570/E408）全部保留，warning_free=false。独立限定scope收据已通过；该结果不修复校准5失败、不提供editors/T08/T09/SAF或完整四组验收。

root-focus-captures1只执行了五条原只读命令（windows/activity/input/input_method/cmdline），各exit0且原stdout/stderr SHA重核。main cmdline第一段确为com.osfans.trime.debug，但99字节含77个NUL；采集helper因整体cmdline等式过严自行失败，原receipt passed=false保留。没有PNG截图或截图证明，不能声称已采到焦点截图。

| 本次已闭合原件 | SHA-256 |
| --- | --- |
| calibration-build6/summary.json | `a3eebfd03e529f52aa16c3a7588889594c3349651e023ee89a19bc92ba3a1d8c` |
| api21-fixture-calibration5/report.json | `bf345c9c81a6d9f39d7d62183475424b094a93994d5c244a3867a843f30e330a` |
| api21-privacy-focus-diagnostic1/report.json | `934b077c6c3edc0effce0dc9d5d9aabb2b4a6f30c9e3474f98ce0b38558ec2c5` |

## 新增独立限定审阅与最新上游核验

仅privacy诊断独立收据 `runtime-independent-review6/api21-privacy-focus-diagnostic1-verification.json` SHA `c07cec6abcef28cbaa28baae481bb6c96eb07d379a93818333ac00b823032c2b`、41967字节，06:25:37.225220Z闭合P：当前CODE3bb、116个raw/42条命令与五只读capture输出全部哈希核验。实际16项/3方案、Main2540、owned关闭、焦点/ALT/非编辑器/daemon空断言及1978 W/E原文均保留；诊断/校准限定scope之外的full/Release/Lint/four-probe/four-VM/publication全部false，也不证明此前Pico native崩溃已消除。

校准5详细独立失败收据 `runtime-independent-review6/api21-fixture-calibration5-failure.json` SHA `67463554d0a7bc128dbb5af8c87e0e7ebf38eaf4b639a1f88a9a0e3753081263`、88087字节，原false、privacy checks0和Pico shutdown/deallocate SIGSEGV保留，163个raw/44条命令核验；焦点超时原因仍未知，没有当时live window/input/截图证据证明是哪一窗口阻塞焦点。T09only新诊断正在运行，不冻结报告SHA，不提前计T09通过或四组正式验收。

06:24:36.206310Z的真实远端核验 `upstream-lineage7/report.json` SHA `a195bc5629d05bd1995867dfe38e3b480af8e0b219098b17218d088e554c9307`：fork develop仍22678183，osfans develop仍fda120f7，上游为当前3bb祖先。lineage6先用了不存在的本地remote名upstream而查询失败，原stderr/stdout保留；lineage7明确用https://github.com/osfans/trime.git重新查询成功。该时点不表示新CODE已推送、打tag或发布；最终仍须实际GitHub分支/tag/三资产核验。

## FullBuild5停止晋级与Debug4失败边界

Build5及static5确实通过，API21 Release7单组producer/独立review/警告裁决通过；随后API21 Debug4正常闭合FAIL（05:58:55.490334Z，owned3818690由原runnerwait exit0）。editors/T08功能子测通过；DocumentsUI真实持久读写授权只完成准备，T09和SAF roundtrip/restart/revoke未到达。privacy检查在前置Activity启动后daemon不为空处失败，checks=0；旧ClipEditActivity主动聚焦EditText并post显示IME，与夹具要求无Rime会话冲突。没有由此建立隐私泄露或隐私sink失败结论，全部原断言仍要求后续新候选实际通过。

独立失败review SHA `cb205eaa794c8574d185bc2e172da361473b725d6bbb327b306d38c940a3fe8f`，原producer SHA `c2b30424c71e7e1a248f38f127eeaebe42053802429634bf3e75efec97bf59c3`。2394 W/E（W1717/E677）保留，warning_free=false；三项critical/实际LMK/kernel为0仍不计整体P。Full5 raw在13:54:33.484的当前Debug PID2461 WrappedRunnable7062ms等待发生于fullCheck启动/部署阶段、正式editors启动13:54:40.500之前；不是旧APK、稳态候选P95或每次弹键盘全量编译证据。真机P95/PSS、ARM64/OEM运行等未测范围保持。

<details>
<summary>CODEb32/FullBuild5完整历史（停止稳定晋级，不供新CODE验收）</summary>

以下“新”“当前”“本候选”仅指当时b32/FullBuild5或更早记录；原pass/fail与警告不改写为新CODE通过。

**完整Build5、host7、新双Release静态/签名与独立协程审阅及限定API21编辑器校准4通过；四组fresh正式设备验收和稳定发布仍待完成。**

编译CODE为 `b32f8b68145747de27db5b664fc671d2ea0e4f1c`，552项app/build输入在完整Build5、host7与calibration-build5各自前后逐项SHA一致。后续仅七份发布文档形成DOCHEAD，与CODE分开记录，APK内BUILD_COMMIT_HASH仍指编译CODE。旧Build4/Release5的通过与Debug3失败在历史区保留，不提供本候选验收。

## FullBuild5历史已执行范围

| 检查 | 实际结果 | 边界 |
| --- | --- | --- |
| fresh host7 | 206＝128＋31＋13＋5＋29，通过 | 源552和宿主86项输入不变；旧IC18未重跑 |
| calibration-build5 | Debug校准280.698秒，exit0 | app663fresh、fail/error/skipped各0，SpotlessCheck通过 |
| 校准构建警告 | warnings.json为空，0条 | 完整构建日志保留；未运行Lint或Release |
| 校准产物 | 两ABI Debug APK和测试APK冻结 | 三个APK仅本地QA使用，不是正式公开附件 |
| api21-fixture-calibration4 | producer及独立审阅scope PASS，Main2585；真实触摸、两主题symbols隐私、40 rapid/100cycles | owned3763935关闭wait exit0；candidate/publication/four-cohort acceptance均false |
| 完整Build5 Debug / Release | 519.642 / 666.339秒，退出0，552源映射不变 | Debug119 tasks/31执行/88缓存；Release69 tasks/17执行/52缓存 |
| 完整Build5 JVM | app663fresh、fail/error/skipped各0 | convention17/compatibility6报告通过但任务UP-TO-DATE |
| 完整Build5 Lint / 构建警告 | Debug/Release各0 issue、warnings.json均为空 | 两Lint XML和完整构建日志分别保留 |
| static5 | 新双Release静态/签名及独立协程语义PASS | 新CODE/精确APK/完整DEX/新mapped6绑定，device_acceptance=false |
| 四组正式设备 | 待完成 | 不沿用旧候选或校准APK |

calibration-build4先在SpotlessKotlinCheck失败，仅有两处格式差异；其CODE为512da2d6，108.497秒exit1、没有产物。两处普通格式修正提交后，calibration-build5在本CODE通过（完整Gradle日志4m40s），552项源映射不变。该范围不提供Lint、Release、签名或convention/compatibility套件的新通过。

| 当前原件 | SHA-256 |
| --- | --- |
| host7/summary.json | `b829e132568d886cfe6aa752b1ee8248e624a0618971ccd0082db6d1a0fe577f` |
| host7与calibration-build5的source-before/after.json | `f99bba9cbb273ac88c26639e0363f917a1fd6e3be64d146a26bd3492c47266c0` |
| calibration-build5/summary.json | `5625e5501334b76386458d837631c5430f922305b9f7190f65a24a1a5a2af31f` |
| calibration-build5/build.log | `801bfdb2cf216425e152e5c758f8657316c17b9fa136a167d95b8f71eae17773` |
| calibration-build4/summary.json（格式失败） | `f47faf492b3ed2db4b8ae61dab54902885535493f9b1997c594c1585a26c44f0` |

这些路径以 `build/stable-warning-debug/` 为根；29项host收据另在 `build/t09/t912-current-guard7.json`。校准4producer结束于2026-10-10T05:08:50.784202Z，owned于05:08:49.421646Z关闭。原报告SHA `04b4f65d56ee934d11107290be7672079b7332556a48719f59b16e8630b26308`，原touch指标SHA `4891431d49bc4581d3b66dd585ae768b33bcc428c4e4f214d921bedc54f4c866`；最大show384ms/clear185ms是instrumentation墙钟，不是候选P95。独立审阅保留2078条W/E（W1596/E482）、warning_free=false，critical/实际LMK/kernel0。所有观察到的PID项目tag为0，选中Main框架63条，包含两条inactive InputConnection（创建时finishComposingText、测试关闭时showStatusIcon），原上下文保留；不声称所有框架诊断消除。

完整Build5结束于 `2026-10-10T05:28:55.426463Z`；独立校准审阅结束于05:12:38.899894Z，只绑定calibration-build5的Debug/test APK，不能用于完整Build5的新APK身份。

| 完整构建与校准原件 | SHA-256 |
| --- | --- |
| build5/summary.json | `f4498d70102c3715b58be254688e44f6d19ae8e4ecfde2992c607c45382d2a44` |
| build5/debug/build.log | `14e9217cff73b17494cfdf7668110f38dcd90d0fc4ba1de9a8c9daa1926fee58` |
| build5/release/build.log | `0eb8e82657d7a4e0ec081516881044c9d0b27a45afc18a855420270f87293825` |
| runtime-independent-review5/api21-fixture-calibration4-verification.json | `da2b87efc03864ed4fe8a8d45a41037d1094b35232f774c565898f53e9e8d61e` |
| upstream-lineage5.json | `07d2d00faf9b3dcae43839501a71fb1d211a5757366ac3ff0b17c8a57e862240` |

上游归属在收据记录的05:27:39 UTC核验：osfans/trime:develop为fda120f7，fork develop为22678183，两实际远端ref与预期相同且均为b32f8b68祖先。该时点核验不表示当前候选已推送或已发布；发布前还须核对最终远端分支/tag。

| FullBuild5历史APK（不发布） | 字节数 | Build5 SHA-256 |
| --- | --- | --- |
| trime-v3.3.13-t9.12-arm64-v8a.apk | 11,786,772 | `8f2f1c561635ad344681b1f4c4fe319d576c5a8ddc62c36116501497cfff3faa` |
| trime-v3.3.13-t9.12-x86_64.apk | 11,865,939 | `ef5962e7f3a39e3dfdc79f8b6e155f226421df1222c8f5ab401852a65dfb4706` |
| trime-v3.3.13-t9.12-source-with-submodules.tar.gz | 归档后外部记录 | 完整递归源码摘要由外部收据核验 |

## 新双Release静态/签名与协程审阅

独立 `coroutine-independent-review5/verification.json` SHA-256为 `b1ba72b04aa0ee860d662d738d321604833486d6c449de4e451dd6ba0d2885e4`。两ABI都精确绑定完整Build5、新CODE与上表APK，minAPI21、nondebug、tracing=false、v1/v2及原证书保持；签名警告各0，META-INF/services条目为空。

双ABI实际完整classes.dex字节相同：SHA `95443dc6f3f2b76d7c8db3efeaaf3f822ea2bc3fd7b6451a72729e8555733f8e`、4,136,916字节、4936类/21796方法ID。全method ID无ServiceLoader与Class/ClassLoader资源查询，SPI/META-INF字面量为0，正常AssetManager.open仍在。Release实际manifest池129字符串/23组件、全DEX定义与R8 alias都没有Debug专用EditorProbeActivity；新Debug APK正例在classes16.dex实际存在，三项内存损坏AXML/DEX负控被拒绝。

新mapped6的18份输出、6个core类、InlineList实际caller及合并synthetic方法已审阅，包含直接factory/MainLooper/Handler、异常provider、API26–27反射prehandler、SDK21/35 Handler、post/postDelayed/取消/removeCallbacks和拒绝后IO路径。原inspection5整体passed=false/semantics=false保留，由新独立收据承担语义结论；manual→夹具正反控及所有原collector/tool/命令输出哈希链完整。此收据device_acceptance=false，不提供四组正式设备P。

## 新候选四组正式验收（增量记录，整体待完成）

| API / 构建 | 计划目录 | 实际结果 |
| --- | --- | --- |
| API21 Release | api21-release7 | Build5单组producer/独立review/裁决PASS；不供新CODE验收 |
| API21 Debug | api21-debug4 | 正常闭合FAIL：privacy前置checks0；editors/T08及授权准备仅子测P |
| API35 Release | api35-release1 | 未执行，Build5停止晋级 |
| API35 Debug | api35-debug1 | 未执行，Build5停止晋级 |

每组须绑定新完整构建的APK字节、CODE、boot/Main PID/UID、实际owned关闭、独立审阅与警告裁决。API21 Debug须真实DocumentsUI持久授权和SAF roundtrip/restart/revoke，API35 Debug须完整editors/t08/privacy/t09/saf。Release每主题27组PNG/XML、3轮，总54组/6轮。新正式全raw W/E/F及归属：Release7的3028及Debug4的2394 W/E原文全部保留，API35未执行。系统框架与图形诊断不归零，critical0不表示warning-free。

最终公开附件仍恰好两个Release APK和递归子模块源码包；新APK的bytes/SHA按FullBuild5冻结、static5已通过，不能使用下面历史Build4摘要。发布链新增companion只消费已有producer/独立review/警告裁决，在准备和公开PATCH前复验；冻结的原verifier/runner不改。18项离线controls通过只验证发布守卫，不提供新候选设备通过。

## 改动与行为

T09提供九键到明确QWERTY英文页的入口、中文返回与手动语言记忆，输入框强制模式只临时生效，符号往返保留待提交组合。Rime单线程FIFO保持输入、语言动作与宏顺序；虚拟可打印ASCII走既有InputConnection文本提交路径，物理键、非打印键与快捷修饰保留事件路径。既有合并JNI快照与有界候选读取保持。

预编辑/浮动候选移除空触摸Popup，主IME的TOUCHABLE_INSETS_REGION只覆盖同token、已附着且可见的区域；旁边空白需要可触达应用。getLocalVisibleRect包含View自身滚动，转换为window/screen坐标时扣own scroll，祖先滚动不重复扣除。测试保留整块32×32应用目标在Region外、真实DOWN/UP、native caret和候选一次准确提交断言，并临时滚动IME root `(11,7)` 检查物理区域后finally恢复。

仅Debug提供非floating EditorProbeActivity。测试从硬件候选回到虚拟键盘使用真实host点击；重置文本后等待源代码限定的新editor token、当前编辑器/fieldId/IMM/layout条件，不直接调用service模拟点击。Build4 Release实际manifest和DEX没有该Debug专用Activity；新CODE须由新的静态审阅复验。

SAF遍历前后拒绝loading/error不完整列表，防止部分列举触发orphan删除；StorageAccess2 namespace适配保持。日志导出在Main捕获文本、IO写出并传播取消。

隐私夹具按两主题配置分别验证Trime symbols native ASCII=true、Tong=false，在手动隐私进入/退出前后检查原native状态和同一KeyboardWindow/键盘对象。先前硬编码true的失败保留，不降低真实触摸、native caret或32×32目标断言。

<details>
<summary>Build4 / CODE7eae历史通过、失败与完整警告记录</summary>

历史CODE为 `7eae3a5bdd1e6b49d1d79ff7efdec2369108810e`；以下“新”“当前”仅指该旧范围，Debug3失败后停止晋级，不能用于b32f8b68。

## 完整Build4与host5

| 检查 | 实际结果 | 执行边界 |
| --- | --- | --- |
| Debug | 440.287秒、退出0，SpotlessCheck通过 | app663项实际执行，fail/error/skipped各0 |
| Release | 663.447秒、退出0，正常R8/双ABI打包 | 实际执行post-R8 guard后再package/sign |
| convention / compatibility | 17 / 6项报告通过 | 两任务UP-TO-DATE，不称本轮重跑 |
| Debug / Release Lint | 各0 issue | 两份实际XML报告 |
| Debug / Release构建警告 | 两个warnings.json均为空 | 完整日志保留，分别核验 |
| 输入与构建身份 | 552项前后SHA相同，CODE一致、tracing=false | BuildConfig的release、versionCode20261117与3.3.13-t9.12绑定 |
| fresh host5 | 206＝128＋31＋13＋5＋29，通过 | 最后29为tracked virtual text guards；旧IC18未重跑 |

完整构建结束于 `2026-10-10T04:35:00.674381Z`。host5的源码及宿主脚本前后不变、CODE不变，结束时Git干净。

关键原件如下，完整命令、测试XML、源码映射与原始日志保留在本地ignored目录，不作为Release附件：

| 原件 | SHA-256 |
| --- | --- |
| build4/summary.json | `2cdd06208b8b0ea248a46eb06e06f0c6177ebb8c08dc74ba9afdd21c080c6f19` |
| build4/source-before.json与source-after.json | `65ee4b5b41576f663edf4a7edcd4f0a40e34882b3e15679d6d1876ff7ad18519` |
| build4/debug/build.log | `5354f26ad4123f79c5e83b7cda3ef3a3d2c09e752114c12b40f5eb6c43e1bc5b` |
| build4/release/build.log | `946e9862391b60227b757df94ae3a54486515f5f28ceb8a7ecc8c75fd649d19e` |
| host5/summary.json | `c119467ae5ee23d637933ae6d7fc60696e16209f41523757a69c2270c49cc657` |

上述路径以 `build/stable-warning-debug/` 为根；29项收据另在 `build/t09/t912-current-guard5.json`，源字节绑定、actual tracked=true、inputs unchanged=true，device/build executed=false。

## 新Release APK静态、签名与协程

| 项目 | 实际结果 |
| --- | --- |
| 两ABI身份 | ARM64/x86_64、minAPI21、nondebug、tracing=false、包名com.osfans.trime.debug |
| 覆盖升级证书 | 原证书保持，SHA前缀c6fb875b；没有新生产密钥声明 |
| 签名与资源 | v1/v2通过，签名警告各0，META-INF/services条目为空 |
| 全DEX | 双ABI完整classes.dex字节相同，4936类定义、21796 method IDs |
| Release排除Debug夹具 | AXML129字符串/23组件无EditorProbeActivity，actual DEX定义无该类，R8 alias记录0 |
| 协程独立语义 | 新18份mapped输出、6个core类、InlineList及合并synthetic调用路径实际审阅通过 |

独立审阅绑定新CODE与精确新APK。全DEX没有ServiceLoader方法owner、Class/ClassLoader资源查找或SPI/META-INF字面量；Main实际直接创建AndroidDispatcherFactory/MainLooper/HandlerContext，异常路径直接提供AndroidExceptionPreHandler，并由全局InlineList调用。SDK26–27反射pre-handler、SDK21/35 Handler路径、post/postDelayed/取消/拒绝后IO与回调移除仍在实际字节码中。

原始inspection collector的整体passed=false/semantics=false原样保留；静态分项true，新独立verification负责语义结论。R8 mapping/config/usage/BuildConfig均明确绑定Build4对应原件。此独立收据device_acceptance=false，不代替设备验收。

新独立收据 `coroutine-independent-review4/verification.json` SHA-256：`ad6e03a3569e5376b793c7e981c4b920367473cee1f9c6f2134dd5fcd69dcf5e`。构建兼容与SPI变换的版本/原始输入/DEX/其它资源守卫详见 [构建插件说明](BUILD-PLUGIN-COMPAT.md)。实际顺序为R8→守卫及资源移除→package→正常sign；不改DEX或签名后APK，不关闭v1。

| Build4历史APK（不发布） | 字节数 | Build4 SHA-256 |
| --- | --- | --- |
| trime-v3.3.13-t9.12-arm64-v8a.apk | 11,786,781 | `cc557885d86144c65b51c3dab2836b1af12dbe82f86b21ffe6686590dd0cda9b` |
| trime-v3.3.13-t9.12-x86_64.apk | 11,865,954 | `dca7949ef2ac9586015bbefc03faf99c8f05a9a2a55deb86c25db65f3f40225b` |
| trime-v3.3.13-t9.12-source-with-submodules.tar.gz | 归档后外部记录 | 完整递归子模块源码，外部归档/公开下载收据核验 |

## API21前置校准（scope限定）

calibration-build3的Debug构建198.381秒，app663fresh/Spotless通过、构建警告0，552项输入不变。该构建没跑Lint或Release。wrapper误接收`--output`的位置参数导致目录先写到repo根，完整构建后仅行政迁移至ignored目录；原summary、全部原文件hash/bytes及directory-relocation保存，仅artifacts[*].file重定位，没有重跑构建或改写结果。

api21-fixture-calibration3及独立文本审阅实际通过：真实host点击→虚拟键盘恢复→reset后源代码限定的changed-token条件；ownScroll17换算后的32×32目标、native caret/候选触摸、40 rapid和100显示隐藏完成。最大show301ms、clear154ms为instrumentation墙钟（含队列/Main/polling），不是候选P95。raw没输出数值editorToken，不额外声称已测具体token值。

owned模拟器3707518停止并waited exit0。全raw2098条W/E（W1616、E482）全部保留，inactive cursor warning0，all-PID critical/LMK/kernel critical0；probe框架记录63，选中新候选PID的项目tag记录0。安装前旧PID1643有15条项目警告，其启动/诊断早于新APK实际安装，原文与归属证据保留；旧APK摘要未测，不补推具体旧构建身份。warning_free=false。

校准独立收据 `runtime-independent-review4/api21-fixture-calibration3-verification.json` SHA-256：`bb19bf160e31282ea58a8a5a28f02be8efe300f265dde9273853ecd800c90aec`。scope仅debug-fixture-calibration，candidate/publication/four-fresh-VM acceptance均false；正式新Debug APK时间戳/字节不同，不能沿用校准为正式设备P。

## Build4正式fresh设备记录（该候选已停止晋级）

| 新组 | 结果 | 必需范围 |
| --- | --- | --- |
| API21 Release | Release5实际PASS，owned关闭；仅本候选范围 | 实际9.11→新APK覆盖升级；UID/偏好/虚构哨兵；两主题各3轮native预编辑/caret、隐藏恢复、一次准确候选提交 |
| API21 Debug | Debug3实际FAIL：符号页ASCII夹具预期；原失败保留 | editors/T08/privacy/T09；40 rapid与100 show-hide；DocumentsUI真实持久授权、SAF roundtrip/restart/revoke |
| API35 Release | 未执行；该候选已停止晋级 | 新boot、实际升级、两主题各3轮真实UI；不借旧API21或Build8结果 |
| API35 Debug | 未执行；该候选已停止晋级 | editors/T08/privacy/T09及真实SAF provider，含遍历末端loading/error |

每组填写报告/独立审阅、Build4 APK SHA、boot/Main PID、真实停止及完整raw统计。Release每主题27 capture/3轮，共54 capture/6轮；Debug端实际隐私内容/候选关联与terminal指标绑定。API21运行须加`--saf21`；API35须包含`--probes editors t08 privacy t09 saf`，默认列表缺saf不能发布。普通Release没有空触摸Popup，不能沿用旧Popup1→0→1门禁。

正式API21 Release5在同一Main PID2526、UID10053、starttime6173完成两主题各3轮，共54组PNG/XML；真实caret `ni‸→‸ni→ni‸`、隐藏恢复和候选一次准确提交通过。9.11（20261116）实际覆盖到Build4新APK（20261117），UID、原偏好与虚构哨兵保持。1810条命令的stdout/stderr哈希核验通过；owned模拟器退出0并关闭。

完整原始W/E为3175（W1667/E1508），新PID项目tag为0、框架7条（2 SQLite WAL恢复、1 GL配置回退、3 eglCodecCommon、1 ART线程暂停）；全部保留，warning_free=false。安装前PID1702的15条项目诊断发生在12:37:14–15，早于实际旧版安装12:37:30.313与新候选安装12:37:37.346，不能归给新PID2526；当时boot APK字节未采集，不猜其SHA。全PID critical/实际LMK/kernel critical均0。

| Release5原件 | SHA-256 |
| --- | --- |
| api21-release5/report.json | `d0ef7c9ab6269626b0bc0c7eabbf3e3cc0180cc1b90dd6c6f108021286ed6f8a` |
| runtime-independent-review4/api21-release5-review1/review.json | `da3dd3e0fbac9c62dafc90f752edafe855e605c5e685c1f339da7265228fe079` |
| runtime-independent-review4/api21-release5-warning-adjudication.json | `9a9851569974fb90ec3483eef2cdfef96f46d7ff370f3d2eda35f2ff6e980233` |

其他正式组未完成；后续新候选必须重新采集raw W/E/F及逐类归属。系统框架、图形、受控fixture诊断全部保留；critical0不清空warning。API21早期PID0 capability只按精确原始boot前缀/相邻errno/后续同TID唯一连接分类，真实kill、未知critical或运行中新事件仍阻断。


</details>

## 本候选的主题配置前置失败（保留）

API21 Release6首个 `release-ui/trime-three1` 在正式轮次前因“Actual unambiguous saved Rime schema required”拒绝，原报告passed=false、rounds为空，结束于05:35:41.849837Z。SHA为 `efcae52eaabb8bc898835889e4abb9e3594889a608ef3bb42e63ae05f2fdcabe`。默认luna_pinyin没有明确持久化简化字方案；Root按真实UI选择“朙月拼音·简化字”，新的trime-three2及另一主题共六轮真实UI/54组PNG/XML子测通过，升级子测通过，原前置false保持。

随后原runner执行层exit143，在消费六轮root acceptance及执行finally之前中断，原producer仍false且没有finished或最终owned wait证据。Root核对原PID/starttime/cmdline/executable/PGID后关闭孤立模拟器，外部关闭不能替代原runner的正式闭合。信号来源未知，不据此归因为PTY、OOM或Trime。独立历史审阅 `runtime-independent-review5/api21-release6-interruption-review1/review.json` SHA-256为 `f2e216d3ec2b39066c624b73b7735d97aedf1c6cd790569026910e52f5d07bde`，passed、candidate/device/publication acceptance均false；1829条原命令及全部raw核验，3184条W/E（W1662/E1522）保留，warning_free=false。

正式API21 Release改为新api21-release7，原runner已独立闭合通过（见当前正式记录），不借Release6子测供通过。该Build5单组独立review和裁决已通过；Debug4随后整体FAIL，另外两组未执行，Build5停止晋级。

## API21 Release7当前原执行器结果（独立审阅待完成）

原runner于2026-10-10T05:51:34.973728Z闭合passed=true、functional_passed=true且552源输入不变。owned模拟器3804598由原runner停止并wait exit0，关闭时间05:51:33.556042Z。Main2520、UID10053、starttime6437、boot `a259861b-6fc9-4dd4-8263-dbb104370b8c` 绑定Build5新x86 APK；两主题各3轮、共54组原PNG/XML，真实预编辑caret/候选触摸、隐藏恢复及一次准确提交通过。

实际9.11（20261116）覆盖升级到Build5（20261117），UID10053、原偏好SHA和虚构哨兵SHA保持。原full-device audit记录3028条W/E（W1652/E1376）完整保留，warning_free=false；原producer的all-PID critical/实际LMK/kernel critical均0。Release7独立runtime审阅及裁决已通过（review SHA557b7c2341da19067ea0025ed9a3f740a4ad691a196da1aa446a29e9ee9fa494；warning SHAa3bff21eb55b4263871b24978519a91c77e908c22ec0d1362da9b19183948fc0），1631命令/3743raw文件核验，Main2520项目tag0/框架6；仅本Build5单组通过，不能供新CODE3bb或四组整体验收。Debug4随后正常闭合FAIL。

| Release7已闭合原件 | SHA-256 |
| --- | --- |
| api21-release7/report.json | `8f0e408d7df0cf8476218fb8c79c603b3ee5a968e9bc178cb8906003f7a028b3` |
| api21-release7/release-root-acceptance.json | `0055e200276e6f3ccc5f6b84c2dfa6126c91625e29ff36f04197b48499d3ba58` |
| api21-release7/logcat-full.txt | `a596ed5f988cc7fa10fc210c47c239c1a1ddc2910519dc56926369d261cbfc51` |

## 历史与未测范围

Build2/Build3旧构建、静态及设备收据保留历史，不提供新CODE设备通过。旧Release4的6轮/54 capture和3245 W/E（W1687/E1558）、Main框架6、warning_free=false均保留。旧Debug1小Dialog几何失败、Debug2仅11px host交集导致32×32目标未创建、calibration1未扣own scroll的17px坐标差、calibration2全部断言通过但两inactive cursor诊断整体FAIL均不改写。此前printf哨兵、PID0分类、SPI警告、host输出路径失败也保留。

原Build8 T09严格成本比较仍false；7匹配任务未退步，仅标准主题Android16/Wi-Fi/B站子集导航14→6、总39→31，email/URL不同起终键盘不可比。旧API35 Debug约4秒停顿原件保留，新的2秒rapid上限只验该回归，不是100ms P95，不能借普通Release范围清除旧失败。见 [T09历史记录](VALIDATION-T09.md)。

同一真机正常Release的P95、Java/native/graphics/总PSS、连续输入/切换主题驻留、首次部署、ARM64/OEM运行、一般准确率与物理16KiB页设备仍未测。ARM64仅构建/静态；Debug fullCheck属维护检查，不代表正常Release启动或每次弹键盘全量重编译。串行构建/模拟器与受限worker/heap避免验收OOM，不是产品PSS达标证据。

新CODE完整构建、host/静态与四组全部通过后，只更新允许七份文档形成DOCHEAD，冻结两个APK，以DOCHEAD归档完整递归子模块源码，推送fork、稳定tag并公开核验三附件。QA/testAPK/JSON/log/报告不上传，GitHub自动Source code链接保留。最终DOCHEAD与完整源码包摘要在外部收据记录，避免写进自身待归档文档形成自引用。本草稿不声明已发布或替换Latest。

</details>

</details>

<details>
<summary>原 README.md 历史记录 1（按原范围保留）</summary>

<details>
<summary>Historical Build8/T09 and v3.3.13-t9.11 T08 records</summary>

The following records retain their original builds, failures and warning counts. Their “current” wording is historical and supplies no t9.12 acceptance.

## Build8 historical T09 validation

Compiled commit `d4994026` passed Build8: 203 host checks, 663 freshly executed app JVM tests, 9 cached build-logic tests and zero Lint issues in both variants. Android 5.0/API21 and Android 15/API35 each passed 30 mixed-input cases and two-theme language functional extras: 24 proofs, 12 real process recreations and 2 pending-composition-to-English transitions per API, followed by restoration and owned emulator shutdown. Both APIs' complete language-log evidence is closed, while ordinary warnings remain UNKNOWN; the current normal Release passed three popup rounds per theme on Android 15: visibility 1→0→1, the same process, one exact commit and stopped-file restoration. Full logs show no critical crash or four-second popup signature in this scope.

The strict comparison returned false. Seven matching tasks did not regress. Only the standard theme's Android 16, Wi-Fi and B站 subset reduced navigation from 14 to 6 actions (57.1%) and total actions from 39 to 31 (20.5%). Email/URL keyboard IDs differ and remain incomparable; previously unreachable Trime routes are recorded as added capabilities. Two approximately four-second API35 Debug window stalls remain unresolved. The 55 upstream deprecations, two META-INF warnings per Release APK and ordinary device warnings remain recorded; physical P95/PSS, ARM64 runtime and general accuracy are unmeasured.

Public [v3.3.13-t9.11](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.11) is the T08 stable release and contains no T09 changes. T09 source and this record are synchronized on the fork’s develop branch; this work does not create another stable tag. See the [T09 validation record](script/quality/VALIDATION-T09.md). A later documentation HEAD does not change the APK's compiled source.

## Previously published v3.3.13-t9.11 T08 acceptance record

**Acceptance completed; publication status follows the GitHub tag.** This version contains T08 candidate snapshot guards, bounded paging and optional numeric latency diagnostics. Input delivery remains ordered and the existing aggregate JNI response is retained. T09 mixed-input changes are outside this tag.

The frozen APKs are non-debuggable Release/R8 builds with performance tracing disabled, Android 5.0+ support, and ARM64 / x86_64 variants. Package `com.osfans.trime.debug` and the fork's previous signing certificate are retained for upgrades. R8 optimizes and shrinks code/resources while the current rules preserve class names; no new production signing key or ARM64 device testing is claimed.

Compiled source `2e0ab64a3bd590376c1d15805235241c36e83d33` is frozen in `build/t08/stable-candidate-4`; all 536 selected input hashes match before/after the build. Debug took 542.599 seconds and Release 653.891 seconds. The 598 application JVM tests actually ran and passed; 9 build-logic reports have zero failures/errors/skips, with that task UP-TO-DATE. Both online Lint reports contain zero issues; the 174 host checks passed in this build's scope.

Fresh API21/API35 x86_64 emulator acceptance passed: 320 native FIFO keys, a 49-candidate 48+1 paging fixture, stale snapshot/editor rejection, and 100 show/hide cycles per API. Debug covers both shipped themes' native candidate Unicode, accessibility and actual screen glyphs. Normal Release separately completed 6 actual taps and 2 exact Hanzi commits in one current theme per API, 17/23 lifecycle checkpoints, a 36-tap diagnostic trace, and restoration of the exact trace-disabled normal APK. API35 also ran T02 and the full 360-width T03 case. Debug and normal API35 stages have separately recorded boot identities.

The prior candidate2 was rejected for actual blank candidate glyphs; this version restores the TextView text/measurement/layout contract while retaining scaling. Candidate3 failed its API35 T03 geometry check. Candidate4 corrects only that test's own-scroll normalization and uses entirely fresh build/device evidence. Earlier candidates and T07 results remain historical.

Physical-device candidate P95, Java/native/graphics/total PSS, accuracy comparison and first deployment remain unmeasured. The T08 performance budget is not declared passed. Each build phase retains 55 upstream AGP/KSP deprecations (52/3), each normal APK retains two META-INF service-signature warnings, and full device warnings remain recorded. No warning-free or smoothness claim is made.

The release asset names are the ARM64 APK, x86_64 APK and a source archive with submodules. Actual upload, downloads and Latest are determined by GitHub; see the [9.11 acceptance record](doc/releases/v3.3.13-t9.11.md) and [T08 validation report](script/quality/VALIDATION-T08.md).

The first API35 Debug boot retains three approximately 4-second popup buffer destruction stalls as explicit findings. Additional normal Release checks use the separately verified 3 GiB boot: both themes complete three full-pinyin rounds with actual fixture IMM hide/show taps, owned popup visibility 1/0/1, retained preedit and one exact Space submission. Within each theme, all three rounds use one unchanged owned IME process. Those extra scopes show no matching stall; this is not a physical latency budget result.

</details>

</details>

<details>
<summary>原 README.md 历史记录 2（按原范围保留）</summary>

<details>
<summary>Historical T07 / v3.3.13-t9.10 and earlier candidate records</summary>

These retained records describe their original time, build and test scope. Their “current”, “pending” and “Latest” wording, APK hashes and debuggable-build descriptions are historical; they do not describe the 9.12 candidate or supply its acceptance.

Stable release target [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) has completed C37 build/static, API21/API35 whole matrices, real SAF, Direct Boot pressure, natural PIN and three cold-start acceptance. The accepted build is `a5309c0256e325a0f042d2d9062ddbd4f1326c28`, frozen at `build/t07-privacy/stable-candidate37-final1`, timestamp1791464154404 (2026-10-08T12:55:54.404Z). The full708.260124-second build passed582 application JVM tests,9 build-logic tests, Spotless and Lint0, with zero compiler/build warning lines and527 unchanged clean source inputs.

C37 pins each listed descriptor with an owned duplicate; full fstat device/inode identity comes first. Only real EACCES may use bounded metadata after both sound fixtures calibrate, and only a unique strict inode differing from every fixture proves a nonfixture. Acquisition-only direct-cause EBADF records vanished slots; all post-pin errors fail. The original200 scans,0→1→2→1→0 control,9 unrelated descriptors, privacy/TTS/SoundPool/playback/release and30-second budget completed on both matrices. API21 has8 probes and used no fdinfo fallback; API35 has11 probes and verified owned-copy metadata. Actual APK audit retains19 appDEX/6 testDEX,21 probe classes/148 bodies and catch tables/603 references,0 unresolved or above21;56 negatives rejected, two fresh replays had31 byte-equal outputs with51 files/553 dependencies. ARM64 remains build/static only.

QA3 drawing-state canonicalization and exact old-PID INFO clean-exit classification retain their focused112 evidence and strict raw/PID/context guards. QA4 adds only the trusted Toybox regular-empty alias for expected regular files with parsed size0; its actual61 focused cases retain all eight fields, integer/inode/permission/mtime predicates, raw kind and stat commands, without a numeric S_IFREG claim. QA5 isolates a lazy parent-owned four-core DeviceRun subclass whose complete Matrix startup method differs only in the core2→4 literal; boot/storage/readiness/cleanup behavior and the byte-immutable Matrix driver remain intact. Its focused32 cases and independent source peers passed. QA5 live98+139+56=293 passed once, `build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`; current affected publication23 passed once, host END2026-10-08T16:49:26.873240Z, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`. QA3 release68+23 remains historical once-only evidence, END2026-10-08T14:58:52.115495Z, `build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`. The unchanged original adjudicator68 and actual matrix-only adjudication final2 retain their own evidence; neither was rerun for QA5. Each host run binds its own QA13 and dependency15/17/10 closures; older global QA epochs remain historical snapshots. Current applicability peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee` preserves actual matrix/SAF/byte-audit execution scope. All527 candidate sources and three APKs remain the accepted C37 build. QA6 retains the one inherited post-handoff user-state read as full raw stdout/stderr with its exact command index. Replay requires this unique adjacent read, rc0, empty stderr and independently RUNNING_UNLOCKED before normal input; all original actions and deadlines remain intact. Current live Natural56 passed once, `build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`, and affected publication23 passed once. The parent and Matrix remain byte-immutable; the applicability peer also preserves pressure final5 through its separate15-input execution closure. QA5 host evidence and older global QA epochs retain their own historical source scopes.

Separate finished reports and independent peers accept both matrices, the three real SAF phases, fresh pressure final5, the real natural PIN path and three independent cold starts. Pressure keeps the original7s/1.25s/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PID-boot-binding/one Delete/resource-cleanup protocol. Natural uses no controlled rebind or stress injection. Matrix-only adjudication passed with0 validation errors and0 unexplained blockers, while retaining1558 scoped warning records and480 known project diagnostics, warning_free=false; this report does not adjudicate the separate SAF/pressure/Natural/cold gates. Independent gate peers: `7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`. Fresh pressure final5 and Natural final2 acceptance applies to the explicitly recorded4-vCPU/2048-MiB host-GPU profile; both matrices and cold-start acceptance keep their original2-vCPU/2048-MiB scope. No seven-second guarantee on a two-core environment or CPU-specific product fix is claimed. External-resource byte proofs use a scoped privileged read-only lease after UID2000 input actions, followed by restoration to UID2000; they do not establish direct AppUID access.

Application APKs: ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c` (22,442,517 bytes), x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e` (22,539,554 bytes); testAPK `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db` (1,480,837 bytes). Both appAPKs retain two META-INF v1 service warnings, test zero, min21/target37/versionCode20261115 and the existing debug package/certificate. API21 retains105 AudioTrack FAST warnings with successful SoundPool behavior. Physical ARM/16KiB-page/OEM coverage and universal bug or warning elimination are not claimed. Historical C37 failures remain unchanged: P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`. Their partial handoff/Delete facts are not whole or resource acceptance; P2 resource-audit body never started. Publication targets the authorized stable tag/link after the six-document commit, recursive source archive, APK staging and eleven-evidence verification; no earlier published timestamp or receipt is asserted. P3 inventoried87 files and completed81 four-step proofs, then stopped at file82 LOCK stat-before; its cat/device-hash/stat-after and remaining5 files were not reached. P4 exhausted the unchanged seven-second observation deadline: a replacement fork near the boundary did not supply complete in-budget process or ready-input evidence. Its separate timing/source diagnoses `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7` establish no supported product latency fix or fatal/OOM explanation for LatinIME deliberate self-kill. Both failures remain historical; current whole acceptance comes only from fresh pressure final5 on its stated four-core profile. Natural1 whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c` remains unchanged: its bounded handoff and Delete/UID2000 input proof completed, but fixed-read span bookkeeping failed before the resource audit. Independent diagnosis `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93` and diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050` do not reclassify that run; current natural acceptance comes only from fresh Natural final2. The first publication23 fixture attempt failed before its audit derivation because the locked-log input was missing, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`; that failed test run remains historical. The second attempt passed22 methods and failed one old counterexample whose reordered rows lacked resealed command indices, `build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`; it also remains a failed host attempt. Final23 acceptance uses the corrected fixture/counterexample and its own finished source-bound report.

### Historical C36/C35 record — retained without borrowing acceptance

The following paragraphs preserve their recorded C36/C35 scope; C37 status is the pending record above.

Planned stable release [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) is **candidate36: the build/static checks and complete Android 5.0 matrix passed, but the complete Android 15 matrix failed; stable publication is stopped**. Commit `97d38863a4353fbdde95eca3592a60e4a653887e` changes only AndroidTest `InputFeedbackProbe.kt`, with no production change. The frozen directory is `build/t07-privacy/stable-candidate36-final1`, timestamp `1791460043820` (2026-10-08T11:47:23.820Z). The 458.793-second build passed 582 application JVM tests, 9 build-logic tests, Spotless and Lint 0, with no compiler/build warning lines and unchanged 527 source inputs. New C36 APK identity/signature/ABI/48-resource/16 KiB ZIP/ELF checks passed; each app retains two META-INF v1 service warnings (test zero), min21/target37 and the existing debug certificate/package. The 415 device-QA host checks and independent source review passed, with eleven frozen QA hashes. Android 5.0 completed eight probes, thirteen routes, seventeen lifecycle checkpoints, 100 visibility cycles, 52 glyphs/four dot commits and unavailable-input/retry; independent review passed 925 checks. Android 15 then failed at PID5442's first FD snapshot when `Os.stat` returned EACCES. The numeric FD slot was not recorded, and its precise cause is not established or fixed. The real owned-clipboard UI and preceding five probes are partial passes only. Targeted bytecode, final publication-tool binding and other gates still require full new evidence: C36 overall acceptance FAIL: API21 whole PASS, API35 whole FAIL; remaining gates NOT RUN and stable publication stopped. The next corrected candidate must rebuild and complete every gate itself; C36 API21 and earlier C35 passes cannot supply its acceptance. Nothing has been pushed, tagged or released; Latest remains `v3.3.13-t9.9`.

The C35 Android 5.0 final2 matrix under current QA failed overall: after 200 synchronous reads, `InputFeedbackProbe.kt:103` asserted that the total process FD increase exceeded eight. Actual before/after counts and FD targets were not recorded, so the origin remains unknown; neither a sound-reader leak nor a false positive has been established. Later SoundPool/melody/playback checks were not reached, and the sequential formal Android 15 matrix did not start. C36 scopes the test to each FD slot with the sound fixtures' `(st_dev, st_ino)` identities while retaining process totals as diagnostics. Host review covers opening the same fixture twice (1→2→1→0) and holding nine unrelated descriptors; API21 calibration passed; API35 failed during its first snapshot with EACCES, so the next candidate must supply fresh calibration and complete gates. The original 200 reads, 30-second budget, privacy/TTS/SoundPool/playback/cleanup checks remain, with no GC, added waits, raised tolerance or malformed-YAML warning injection.

C35 historical record (superseded by the C36 status above): the candidate35 build passed but complete device acceptance was not achieved. Unified privacy and 30 upstream commits through `e9b21960` remain included. C35 fixes the normal-process WorkManager initialization race with the official WorkManager 2.10.5 Configuration.Provider, a stable lazy configuration and one getInstance path; actual SDK21 guards and DPS/old-DE/replacement-state checks prevent initialization in unsafe process states. Implementation commit `dc69d73baa0c06e5a103000d1cd13f106f280afd` binds 527 source inputs and timestamp `1791449483471` (2026-10-08T08:51:23.471Z). The frozen build is `stable-candidate35-final1`; the earlier interrupted build did not produce an identity chain. The full build passed in 519.403 seconds with 582 application JVM tests, 9 build-logic tests, Spotless, Lint 0 and no compiler/build warning lines. APK identity, static checks and targeted API21 DEX review passed. Each app APK retains two META-INF v1 warnings (test APK 0). The earlier C35 Android 5.0 matrix and independent review genuinely passed under the previous QA, but QA phase4 changed the Matrix/binding tools; the fresh API21 final2 replay subsequently failed overall at the process-FD assertion. The unchanged real SAF phases and process-scoped review remain accepted after current-verifier revalidation; all other current device gates and final adjudication are pending. The first SAF attempt stopped before any picker action because preparation left Latin selected and Main redirected to Setup3; it proves neither a product defect nor successful SAF acceptance. The corrected preparation restores and verifies Trime before the gate without SAF tree/preferences/grant edits; its independent review and the subsequent real DocumentsUI roundtrip/restart/revoke gates passed; C35 complete runtime acceptance was not achieved; API21 final2 failed overall (history). The first C35 Android 15 matrix failed overall: the real owned-clipboard UI and nine probes passed, but ClipEditorProbe could not establish a stable keyboard; later navigation/lifecycle/fallback stages did not run. The failed evidence is retained; the subsequent parser diagnosis does not reclassify this matrix. C35 pressure final1 also failed overall: the original 7-second handoff and full 6501-ms gesture passed, but stage 10 required an IME before its planned single focus, so no real Delete or resource audit completed. Its evidence is retained; the pressure QA ordering correction passed host and independent peer checks. Pressure final2 then failed mutation-3 at the original 1.25-second command limit; its completed 6503-ms gesture is partial evidence only. The epoch3 full-window diagnostic remains an overall FAIL. Read-only replay found 75 Latin frames: its first transitional frame still fails current binding guards, while 74 fully shown frames have a live current connection and `mRestartOnNextWindowFocus=true`. Android 15 defines this as a future window-focus restart request, so requiring false incorrectly rejected those otherwise valid frames. Phase4 has applied a strict unique true|false parser while retaining all current PID, sequence, live-connection, method, EditorInfo and visibility checks; the exact future flag remains recorded outside stable binding identity. Independent review also found malformed duplicate-field gaps (empty values, whitespace before or after equals, or a bare key) in the initial /tmp normal-input proposals; it was corrected before any live application, and both normal-input suites now reject those duplicates. The fresh phase4 short API35 clipboard diagnostic passed with the complete original probe and its assertions after one Latin-to-fresh-Trime sequence; independent runtime review is pending, and this short diagnostic is not a complete matrix or release acceptance. All 415 checks of the exact applied host tools passed; host/source review proves the parser correction only; it does not turn the old diagnostic into PASS or supply a new device result. Current API21/API35 matrices, pressure and natural PIN, cold starts and final adjudication remain pending fresh evidence. C31–C34 results are historical only: C34's successful natural handoff and one real Delete are partial evidence within an overall failed gate. The original 7-second, 1.25-second command, 6500-ms and 60-Hz requirements remain. Privileged read-only file auditing is identified separately and does not prove app-UID file access. The debug certificate/package and historical C31 6,280-ms SAF deployment queue wait remain documented. The current C35 SAF deployment also records one non-injected 5,434-ms Rime queue wait before instrumentation; repeated snapshots show the same event, not three waits. This measured performance limit remains. The final process-scoped SAF review passed and retains one actual native SIGSEGV from the root system input.Input command, identified by PID; the complete device logs are not fatal-free. ARM64 has build/static checks only, with no physical ARM64 or native ARM image run. Nothing has been pushed or published; Latest remains `v3.3.13-t9.9`. See the [release draft](doc/releases/v3.3.13-t9.10.md) and [validation report](script/quality/VALIDATION-T07-PRIVACY.md).

ARM64 and x86_64 APKs retain Android 5.0+ support, package `com.osfans.trime.debug`, and the previous fork signing certificate for in-place upgrades. The default and non-Chinese display name is `Trime`; Simplified Chinese uses `同文输入法`, while Hong Kong and Taiwan Traditional Chinese use `同文輸入法`. These are debug-signed, debuggable builds; production signing and R8 are not enabled. Back up configuration and dictionaries before upgrading. Custom copies of the T9 schema or themes may need the updated rules/layout and redeployment; retain personal dictionaries.

[T04](script/quality/t04/README.md) provides fixed ranking and learning benchmarks in isolated data directories. Luna remains the default; no large dictionary or grammar model is enabled. [T05](script/quality/t05/README.md) separates six optional fuzzy-pinyin rules from three bounded numeric-key repair rules. All are off by default; suggestions display their source. Optimization-plan T08–T10 remain follow-up work: this release does not claim the required physical-device candidate-latency/PSS budget, fixed mixed-input switch-count acceptance, or expanded personal-dictionary and candidate-management acceptance.

Each C35 application APK has two required coroutine service descriptors with v1 META-INF signature-coverage warnings (test APK zero). The C36 static checks independently confirmed the same two v1 META-INF warnings per app APK (test APK zero); these required resources remain, and this draft makes no warning-free claim. Native phone performance, OEM editor coverage, and physical 16 KB-page devices are outside this emulator acceptance. The release has three manual assets: two application APKs and one recursive source archive; GitHub also provides its automatic source links. See also the [key contracts](script/quality/T03.md), [T9 design](script/t9/README.md), and [upstream merge record](script/t9/UPSTREAM-t9.3.zh-CN.md).

</details>

</details>

<details>
<summary>原 README_sc.md 历史记录 1（按原范围保留）</summary>

<details>
<summary>Build8/T09与v3.3.13-t9.11 T08历史记录</summary>

以下保留原构建、失败与警告数量；其中“当前”仅指当时范围，不提供9.12验收。

## Build8 T09历史验收

当前编译提交 `d4994026` 的构建8通过：203项host、663项应用JVM本轮执行、9项build-logic缓存，双Lint0。Android 5.0/API21与Android 15/API35各30项混输检查及两主题语言额外功能通过：每API24份proof、12次真实进程重建、2次待提交64切英文，恢复和owned停止完成。两API语言完整日志证据闭包完成，普通警告仍UNKNOWN；当前正常Release在Android 15两主题各3轮通过：浮层1→0→1、同进程、准确一次提交，原文件停止后还原；完整日志未检出该范围的崩溃或约4秒浮层签名。

严格成本比较实际为false；7项匹配任务未退步。仅标准主题Android 16、Wi-Fi、B站三项子集导航14→6（下降57.1%）、总动作39→31（下降20.5%）。邮件/URL起终键盘ID不同保留不可比，旧Trime不可达路线只记新增能力。API35 Debug两次约4秒窗口停顿仍未决；上游55条弃用提示、每Release APK两条META-INF提示及普通设备警告保留。真机P95/PSS、ARM64运行与一般准确率未测。

公开 [v3.3.13-t9.11](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.11) 是T08稳定版，不含T09；T09源码及本记录同步在fork的develop，不新增稳定标签。详见 [T09验收记录](script/quality/VALIDATION-T09.md)。最终文档HEAD不改变当前APK的编译来源。

## 已发布v3.3.13-t9.11 T08验收记录

**本次验收通过；公开发布状态以 GitHub 标签为准。** 本版实现 T08 候选快照关联、有界分页与可选数值延迟记录，维持输入按序送达和既有 JNI 聚合响应。T09 中英、数字、符号混输优化未进入该标签。

冻结 APK 为不可调试的 Release/R8，日常性能追踪关闭，支持 Android 5.0+，提供 ARM64 与 x86_64。包名 `com.osfans.trime.debug` 和既有 fork 签名证书保持，以支持覆盖升级。R8 优化、缩减代码并压缩资源，当前规则保留类名；不宣称更换生产签名密钥或完成 ARM64 真机验收。

编译源码 `2e0ab64a3bd590376c1d15805235241c36e83d33` 冻结于 `build/t08/stable-candidate-4`，536 项输入前后 SHA 一致。Debug 实际 542.599 秒、Release 653.891 秒；应用 JVM 598 项本轮执行通过，build-logic 9 项报告全 0、任务 UP-TO-DATE；在线 Debug/Release Lint 均 0 issue，本轮 host 174 项通过。

API21/API35 x86_64 新轮模拟器验收通过：各 320 个 native FIFO 按键、49 候选 48+1 分页、旧快照/编辑器拒绝及 100 次显示隐藏。Debug 分别覆盖两个内置主题的 native Unicode、accessibility 与实际屏幕字形；日常 Release 各用一个当前主题完成 6 次真实点击、两次精确汉字提交，17/23 个生命周期检查点、36 次点击的诊断追踪，以及原正常 APK 的实际恢复。API35 另完成 T02 与完整 360 宽度 T03；其 Debug 和正常组保留各自实际 boot 身份。

候选 2 因实际候选字形空白拒绝发布，本版恢复 TextView 框架文本、测量和布局契约并保留缩放；候选 3 的 API35 T03 几何检查失败。候选 4 只修正该探针自身 scroll 坐标归一化，并独立重做全部构建和设备阶段。旧候选与 T07 证据保留历史范围。

真机候选 P95、Java/native/graphics/总 PSS、准确率对照与首次部署仍未测，不宣称达到性能预算。三个构建阶段各保留 55 条上游弃用提示（AGP 52/KSP 3），正常 APK 各保留两条 META-INF 服务签名提示，完整设备警告保留；不称 warning-free 或流畅度达标。

附件命名为两个 APK 与含子模块源码包；实际上传、下载核验和 Latest 以 GitHub 为准。详见 [9.11 验收记录](doc/releases/v3.3.13-t9.11.md)与 [T08 验证报告](script/quality/VALIDATION-T08.md)。

首 API35 Debug 的三次约 4 秒浮层 buffer 销毁 stall 单列保留。追加正常 Release 在独立核对的 3 GiB boot 中，两主题各完成三轮全拼，通过实际 fixture IMM 隐藏/显示按钮点击；各主题的三轮在各自同一 owned PID 中核对浮层 1/0/1、组合保留和一次精确 Space 提交；追加 scope 未见同类 stall，不等于真机延迟预算通过。

</details>

</details>

<details>
<summary>原 README_sc.md 历史记录 2（按原范围保留）</summary>

<details>
<summary>T07 / v3.3.13-t9.10 及更早候选历史记录</summary>

以下保留各记录当时的构建与验收范围。其中“当前”“pending”“Latest”、APK 摘要和可调试构建说明均为历史原文，不代表9.12候选，也不提供本次验收。

稳定版目标 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 已完成C37构建/静态、API21/API35完整矩阵、真实SAF、Direct Boot压力、自然PIN及三轮冷启动验收。实际构建提交 `a5309c0256e325a0f042d2d9062ddbd4f1326c28`，冻结目录 `build/t07-privacy/stable-candidate37-final1`，时间戳1791464154404（2026-10-08T12:55:54.404Z）；完整构建708.260124秒，应用JVM582、build-logic9、Spotless与Lint0通过，编译/build warning行0，527项clean源码前后不变。

C37以自有副本固定列出的FD，优先完整fstat设备/inode身份；仅真实EACCES可在两份声音fixture全部校准后使用有界fdinfo，严格唯一inode与全部fixture不同才证明非fixture。仅取得副本时直接原因EBADF可记录原槽位消失，固定后所有错误均失败。两矩阵完成原200次扫描、同文件0→1→2→1→0、9无关句柄、隐私/TTS/SoundPool/播放/释放与30秒预算。API21仅8探针、未用fdinfo fallback；API35有11探针及实际自有副本元数据证明。实际APK审计保留19 appDEX/6 testDEX、21类/148完整body及catch/603引用、未解析0/高于21为0，56反例全拒绝、两fresh replay的31输出逐字节相同、51文件/553依赖；ARM64仍只作构建/静态。

QA3绘制身份归一化和精确旧PID INFO clean-exit分类保留实际focused112及完整raw/PID/context严格守卫；QA4只允许expected regular file且解析size0时的可信Toybox regular-empty别名，实际focused61保留原8字段、数值/inode/权限/mtime守卫、raw kind和stat命令，不编造numeric S_IFREG。QA5在父脚本内以lazy工厂隔离四核心DeviceRun子类，完整Matrix startup方法仅core2→4 literal不同，boot/storage/readiness/cleanup行为保持，Matrix脚本逐字节不变；其实际focused32和独立source peers通过。QA5 live98+139+56=293项一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json` SHA `cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48`；当前受影响publication23项一次PASS，host END2026-10-08T16:49:26.873240Z，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json` SHA `b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0`。QA3发布68+23仅保留历史一次实测，END2026-10-08T14:58:52.115495Z，`build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json` SHA `89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9`；未变的原adjudicator68与actual matrix-only adju2保持自身证据，QA5没有重跑。每次host运行绑定自身QA13及15/17/10依赖，旧全局QAepoch仅作历史快照；当前适用性peer `4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee`保留矩阵/SAF/byteaudit实际执行范围；527项候选源码及三APK仍是已验收C37构建。 QA6将继承的唯一handoff后user-state读取保留为完整stdout/stderr及精确command index；重放要求该相邻读取唯一、rc0、stderr空且独立解析RUNNING_UNLOCKED，然后normal input仍须立即衔接，所有原actions/deadlines保持。当前live Natural56一次PASS，`build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json` SHA `fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8`，受影响publication23一次PASS；parent与Matrix仍逐字节不变，当前适用性peer通过独立15项执行依赖保留pressure final5实际范围。QA5 host证据与更早全局QAepoch仅保留各自历史源码范围。

矩阵、真实SAF三阶段、fresh pressure final5、独立自然PIN及三轮独立冷启动均以自身实际报告和独立peer通过。压力保留原7秒/1.25秒/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PIDbootbinding/一次真实Delete/资源和清理协议；Natural无controlled rebind或stress injection。仅覆盖两个矩阵的actual adjudication通过，validation errors0/unexplained blockers0，仍保留warning记录1558、known project diagnostics480，warning_free=false；该裁决不替代独立SAF/pressure/Natural/cold门禁。六门禁独立peer SHA依次为`7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2`, `ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf`, `ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0`, `964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2`, `5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5`, `89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5`。 Fresh pressure final5与Natural final2只覆盖明确记录的4-vCPU/2048MiB host-GPU环境，两个矩阵和冷启动仍覆盖原2-vCPU/2048MiB环境；不声称两核心环境7秒保证，也不声称已证实或修复CPU特定产品缺陷。 外部资源字节证明来自UID2000输入动作结束后的有界privileged read-only lease，随后还原UID2000；不证明AppUID直接访问。

应用APK：ARM64 `d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c`（22,442,517字节）、x86_64 `2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e`（22,539,554字节）；test `c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db`（1,480,837字节）。应用各保留2条META-INF v1服务提示、test0，min21/target37/versionCode20261115与原debug包/证书保持。API21的105条AudioTrack FAST提示保留而SoundPool功能完成；不声称物理ARM/16KiB页/OEM全覆盖或全部bug/warning消除。C37旧失败保留：P1 whole FAIL `be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7`; P2 whole FAIL `e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d`; P3 whole FAIL `92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7`; P4 two-core whole FAIL `29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71`; first matrix adjudication FAIL17 `b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21`；局部handoff/Delete不计whole或资源通过，P2资源审计body从未开始。发布目标为已授权稳定tag/链接，六文档提交后继续递归源码归档、两应用APK staging及11项实际evidence验证，不编造此前已published时间或receipt。 P3原inventory87文件、81份四步证明完成，#82 LOCK仅stat-before；其cat/device hash/stat-after和剩余5文件未到达。P4耗尽未放宽的7秒观察预算，接近边界的新进程fork未提供完整预算内进程或ready-input证明；独立时序/源码诊断 `939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6`/`c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7`没有建立可支持的产品延迟修复，也未把LatinIME主动自杀解释为fatal/OOM。两次原失败只留历史；当前whole验收仅来自明确四核心范围的新pressure final5。 Natural1原whole FAIL `ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c`保持：bounded handoff及Delete/UID2000局部输入证明完成，但固定尾部读取的span bookkeeping失败，资源审计未到达；独立诊断 `e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93`及diagnostic replay `0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050`不重分类旧run，当前自然路径验收只来自新Natural final2。 首次publication23 fixture尝试因audit推导前缺少locked-log输入而失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json` SHA `43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a`，该失败host run保持历史。第二次尝试22个方法通过，但旧反例重排记录后未重新封存命令索引而有1个失败，`build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json` SHA `c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367`，仍保留为失败host尝试；最终23验收只采用修正fixture/反例自身完成且绑定实际源码的报告。

### C36/C35 历史记录（不借用为 C37 验收）

以下段落保留当时原记录，C37 当前状态以上方 pending 为准。

拟发布稳定版 [3.3.13-t9.10](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10) 当前 **candidate36：构建/静态及 Android 5.0 完整矩阵通过，Android 15 完整矩阵整体 FAIL，稳定发布停止**。C36 提交 `97d38863a4353fbdde95eca3592a60e4a653887e` 仅修改 AndroidTest 的 `InputFeedbackProbe.kt`，生产代码未改；冻结目录 `build/t07-privacy/stable-candidate36-final1`，时间戳 `1791460043820`（2026-10-08T11:47:23.820Z）。完整构建 458.793 秒、应用 JVM 582 项、build-logic 9 项、Spotless、Lint 0、编译/build warning 行 0，527 源码不变；C36 新 APK 身份/签名/ABI/48 资源/16 KiB ZIP/ELF 静态通过。应用各有 2 条 META-INF v1 服务提示、测试 APK 0，min21/target37 和原 debug 证书/包保留。415 项设备 QA 宿主及独立源码复核通过，11 QA 摘要冻结。C36 Android 5.0 实际完成 8 探针、13 路线、17 生命周期、100 次显示隐藏、52 个 glyph/4 次点号提交及降级输入/重试；独立复核 925 项通过。Android 15 随后在声音探针 PID5442 首次 FD 快照的 `Os.stat` 收到 EACCES，整体失败；具体 FD 槽位未记录，不能宣称已定位或修复原因。其真实持有剪贴板 UI 和此前五项探针仅作局部通过。专项字节码、发布工具最终绑定及其余门禁仍待新完整证据：C36整体运行验收FAIL：API21 whole PASS、API35 whole FAIL，其余未运行门禁不借用旧/新候选；稳定发布已停止。声音探针修正后的后续候选须重新构建并完成全部自身验收，不能借用 C36 API21 或 C35 通过。未推送、打标签或发布，Latest 仍为 `v3.3.13-t9.9`。

C35 当前 QA 的 Android 5.0 final2 矩阵已整体 FAIL：`InputFeedbackProbe.kt:103` 在 200 次同步读取后触发全进程 FD 增量超过 8 的断言，实际 before/after 数值与 FD 目标未记录，来源仍未证实，不能称已定位声音读取器泄漏或已证明假阳性；后续 SoundPool/旋律/播放检查未执行，顺序安排的 Android 15 正式矩阵未启动。C36 将该测试改为声音 fixture 的 `(st_dev, st_ino)` 逐 FD 槽位归属计数，保留全进程总数作诊断；同一文件打开两次的 1→2→1→0 控制与 9 个无关句柄控制已完成宿主复核，API21 实际校准已通过，API35 在首个快照过程中 EACCES 失败；修正后的新候选仍须重新校准并完成全部门禁。原 200 次读取、30 秒预算及隐私/TTS/SoundPool/播放/清理要求保持；不增加 GC、等待或容差，也不注入损坏 YAML 警告。

C35 历史记录（以下当时状态以本节上方 C36 记录为准）：candidate35 构建通过，完整设备验收未完成。统一隐私与固定上游 `e9b21960` 的 30 项整合提交保留；C35 使用 WorkManager 2.10.5 官方 Configuration.Provider 和稳定 lazy 配置，统一 getInstance 初始化路径，修复普通进程恢复时的双重初始化竞态，并由 SDK21 短路及实际 DPS/旧 DE/替换状态守卫拒绝不安全进程初始化。实现提交 `dc69d73baa0c06e5a103000d1cd13f106f280afd`，527 项源码，时间戳 `1791449483471`（2026-10-08T08:51:23.471Z）；正式冻结目录为 `stable-candidate35-final1`，早前中断构建没有形成身份链。完整构建 519.403 秒通过，应用 JVM 582 项、build-logic 9 项均通过，Spotless 通过，Lint 0，编译/build warning 行 0；APK 身份、静态与专项 API21 DEX 检查通过。应用 APK 各保留 2 条 META-INF v1 提示（test 0）；旧 QA 的 C35 Android 5.0 矩阵与独立审阅真实通过，但当前 QA phase4 的 Matrix/绑定工具已变，API21 矩阵须全新重跑；未改的真实 SAF 三阶段与归属审阅经当前校验器复核后保留通过；首次 SAF 尝试因准备末尾保留 Latin、Main 重定向 Setup3 而在任何选择器动作前停止，不计 SAF 通过或产品缺陷，新的 gate 前准备恢复并核验 Trime，不写 SAF tree/首选项/grants；SAF 按实际进程归属的独立日志审阅亦通过，其余设备门禁与最终裁决仍待证据，C35 整体运行验收未完成，API21 final2 整体 FAIL（历史）。C35 Android 15 matrix final1 整体失败：真实持有剪贴板 UI 与九项探针通过，ClipEditorProbe 未确认稳定键盘，后续导航/生命周期/降级未执行，失败保留，后续解析诊断不改写该矩阵结论。C35 压力 final1 整体失败：原 7 秒交接与完整 6501 ms 手势通过，但第十阶段在既定一次 focus 前要求键盘，因此未完成真实 Delete/资源审计；失败证据保留，压力 QA 顺序校正通过宿主及独立 peer，但 final2 随后在 mutation-3 超过原 1.25 秒而整体失败，6503 ms 手势只作单项。epoch3 完整窗口短诊断仍是整体 FAIL。只读重放确认 75 份 Latin 帧中，第一份过渡帧仍被当前绑定守卫拒绝；其余 74 份完整显示且当前连接有效的帧带有 `mRestartOnNextWindowFocus=true`。Android 15 将其定义为未来窗口获得焦点时的重启请求，旧 false-only 谓词错误拒绝了这些有效帧。phase4 已协调应用严格唯一的 true|false 解析，保留当前 PID、序列、有效连接、方法、EditorInfo 与可见性检查；未来标记原值继续记录，但不进入稳定绑定身份。独立复核还发现初次 /tmp 普通输入提案会漏掉空值、等号前后空白或裸字段名的重复字段，已在任何 live 应用前修正，两套普通输入测试均拒绝这些重复字段。随后全新 phase4 Android 15 剪贴板短诊断及完整原探针断言实际通过，只执行一次 Latin→fresh Trime；独立运行复核仍待完成，短诊断不计完整矩阵或发布验收。精确应用后工具的 415 项宿主检查全部通过；宿主与源码复核只证明解析修正，不把旧诊断改成通过，也不代替新设备结果。当前 API21/API35 矩阵、压力与自然 PIN、冷启动及最终裁决均待全新证据。C31–C34 结果只作历史；C34 自然交接及一次 Delete 部分证明不能代替其整体失败。原 7 秒/1.25 秒/6500 ms/60 Hz 规则保留，特权只读文件审计单独标记，不伪称应用 UID 访问证明。现有 debug 证书/包和 C31 历史 SAF 部署 6,280 ms 排队限制保留；C35 本次目录选择/完整部署也记录一次非注入的 5,434 ms Rime 排队，早于 instrumentation，三份快照为同一事件，残余性能限制保留；最终 SAF 归属审阅保留一次 root 系统 input.Input 命令退出时的 native SIGSEGV，按 PID 确认为系统命令而非 Trime，不能称完整设备日志无 fatal；ARM64 仅构建/静态检查，无 ARM64 真机或原生 ARM 镜像验收。当前未推送或发布，Latest 仍为 `v3.3.13-t9.9`。详见[发布草稿](doc/releases/v3.3.13-t9.10.md)及[T07 验证报告](script/quality/VALIDATION-T07-PRIVACY.md)。

ARM64 与 x86_64 APK 保留 Android 5.0+ 支持，延续此前 fork 的包名 `com.osfans.trime.debug` 和签名证书，以支持覆盖升级；默认及非中文语言环境的显示名称为 `Trime`，简体中文为“同文输入法”，香港/台湾繁体中文为“同文輸入法”。附件仍是调试签名、可调试构建，未启用生产签名或 R8。升级前备份配置和词库；自定义目录中的同名方案或主题可能需要合并新规则、布局并重新部署，请保留个人词库。

[T04](script/quality/t04/README.md) 在独立目录中提供固定排名与学习测试，默认继续使用朙月词典，不启用大词库或语法模型。[T05](script/quality/t05/README.md) 将六组可选模糊音与三类有界数字键纠错分开设置，默认全部关闭，建议标明来源。优化计划 T08–T10 仍是后续工作：本版不宣称已完成其要求的真机正式构建候选延迟/PSS 预算、固定混输切换次数验收，或扩展的个人词库与候选管理验收。

C35 每份应用 APK 保留两项必要协程服务文件的 v1 META-INF 签名覆盖警告（测试 APK 为 0）；C36 新 APK 静态检查也实际确认两份应用各 2 条提示、测试 APK 为 0；必要资源继续保留，不能宣称 warning_free。模拟器验收不代表 ARM64 真机性能、所有 OEM 编辑器或物理 16 KB 页设备已验证。Release 仅上传两个应用 APK 和一个递归源码包，共三项手动附件；GitHub 自动生成的源码入口保留。另见[键位规则](script/quality/T03.md)、[九键设计](script/t9/README.md)和[上游合并记录](script/t9/UPSTREAM-t9.3.zh-CN.md)。

</details>

</details>

<details>
<summary>原 README_tc.md 历史记录 1（按原范围保留）</summary>

<details>
<summary>v3.3.13-t9.11 T08歷史紀錄</summary>

以下保留原建置、失敗與警告數量；其中「目前」僅指當時範圍，不提供9.12驗收。T09的Build8歷史比較另見 [T09紀錄](script/quality/VALIDATION-T09.md)。

## 已發佈v3.3.13-t9.11 T08驗收紀錄

**本次驗收通過；公開發佈狀態以 GitHub 標籤為準。** 本版實作 T08 候選快照關聯、有界分頁及可選數值延遲紀錄，維持輸入依序送達與既有 JNI 彙整回應。T09 中英、數字、符號混輸最佳化未進入此標籤。

凍結 APK 為不可除錯的 Release/R8，日常效能追蹤關閉，支援 Android 5.0+，提供 ARM64 與 x86_64。套件 `com.osfans.trime.debug` 及既有 fork 簽章憑證保持，以支援覆蓋升級。R8 最佳化、縮減程式並壓縮資源，目前規則保留類別名稱；不宣稱更換正式簽章金鑰或完成 ARM64 實機驗收。

編譯原始碼 `2e0ab64a3bd590376c1d15805235241c36e83d33` 凍結於 `build/t08/stable-candidate-4`，536 項輸入前後 SHA 相同。Debug 實際 542.599 秒、Release 653.891 秒；應用 JVM 598 項本輪執行通過，build-logic 9 項報告全 0、任務 UP-TO-DATE；線上 Debug/Release Lint 均 0 issue，本輪 host 174 項通過。

API21/API35 x86_64 新輪模擬器驗收通過：各 320 個 native FIFO 按鍵、49 候選 48+1 分頁、舊快照/編輯器拒絕及 100 次顯示隱藏。Debug 分別涵蓋兩個內建主題的 native Unicode、accessibility 與實際螢幕字形；日常 Release 各以一個目前主題完成 6 次實際點擊、兩次精確漢字提交，17/23 個生命週期檢查點、36 次點擊診斷追蹤，以及原正常 APK 的實際恢復。API35 另完成 T02 與完整 360 寬度 T03；其 Debug 與正常組保留各自實際 boot 身分。

候選 2 因實際候選字形空白拒絕發佈，本版恢復 TextView 框架文字、測量與版面配置契約並保留縮放；候選 3 的 API35 T03 幾何檢查失敗。候選 4 僅修正該探針自身 scroll 座標歸一化，並獨立重做全部建置與裝置階段。舊候選及 T07 證據保留歷史範圍。

實機候選 P95、Java/native/graphics/總 PSS、準確率對照及首次部署仍未測，不宣稱達到效能預算。三個建置階段各保留 55 條上游棄用提示（AGP 52/KSP 3），正常 APK 各保留兩條 META-INF 服務簽章提示，完整裝置警告保留；不稱 warning-free 或流暢度達標。

附件命名為兩個 APK 與含子模組原始碼封存包；實際上傳、下載核驗及 Latest 以 GitHub 為準。詳見 [9.11 驗收紀錄](doc/releases/v3.3.13-t9.11.md)與 [T08 驗證報告](script/quality/VALIDATION-T08.md)。

首 API35 Debug 的三次約 4 秒浮層 buffer 銷毀 stall 單列保留。追加正常 Release 於獨立核對的 3 GiB boot，兩主題各完成三輪全拼，透過實際 fixture IMM 隱藏/顯示按鈕點擊；各主題的三輪在各自同一 owned PID 中核對浮層 1/0/1、組合保留與一次精確 Space 提交；追加 scope 未見同類 stall，不等於實機延遲預算通過。

</details>

</details>

<details>
<summary>原 README_tc.md 历史记录 2（按原范围保留）</summary>

<details>
<summary>T07 / v3.3.13-t9.10 與更早候選歷史紀錄</summary>

以下保留各紀錄當時的建置與驗收範圍。其中「目前」「pending」「Latest」、APK 摘要與可除錯建置說明均為歷史原文，不代表9.12候選，也不提供本次驗收。

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

</details>

<details>
<summary>原 docs/index.html 历史记录 1（按原范围保留）</summary>

<details>
          <summary>已发布9.11 T08历史验收记录</summary>
          <p>以下“本次”与“当前”仅指9.11原范围，不提供9.12设备验收。</p>
        <p><strong>v3.3.13-t9.11：本次验收通过；公开发布状态以 GitHub 标签为准。</strong>T08 保持输入顺序和 JNI 聚合响应，增加候选关联、有界分页与可选数值诊断；T09 不在该标签内。</p>
        <p>冻结 APK 为非 debuggable Release/R8，关闭日常性能日志，支持 Android 5.0+，提供 ARM64/x86_64，保持包名与既有签名证书。R8 优化/缩减并保留类名，ARM64 没有真机运行验收。</p>
        <p>编译源码 <code>2e0ab64a3bd590376c1d15805235241c36e83d33</code> 的 536 项输入前后 SHA 相同。Debug 542.599 秒、Release 653.891 秒；应用 JVM 598 项本轮通过，build-logic 9 项报告全 0且 UP-TO-DATE，在线两 Lint 均 0 issue，host174 项通过。</p>
        <p>API21/API35 新轮 x86_64 验收通过：各 320 native FIFO 按键、49 候选 48+1 分页、100 次显示隐藏。Debug 两主题实际候选字形通过；正常 Release 各一个当前主题用 6 次真实点击提交两次汉字，生命周期 17/23 点，36 点击诊断以及原正常包恢复通过。API35 另完成 T02/完整 360 宽度 T03，其 Debug 与正常组保留不同 boot 的真实边界。</p>
        <p>候选 2 的实际空白字形和候选 3 的 API35 T03 几何失败均保留历史；本轮恢复 TextView 契约、归一化探针自身 scroll 坐标，并重新完成全部阶段，不借旧通过。</p>
        <p>真机 P95、Java/native/graphics/总 PSS、准确率和首次部署仍未测。三个构建阶段各 55 条 AGP/KSP 弃用、正常 APK 各两条 META-INF 服务签名提示，以及完整设备警告均保留；不称性能预算或流畅度达标。</p>
        <p>首 API35 Debug 的三次约4秒浮层 buffer 销毁stall单列保留。追加正常 Release 在独立3GiB boot中，两主题各三轮全拼ni，实际fixture IMM隐藏/显示按钮；各主题的三轮保持各自同一 owned PID、浮层1/0/1、组合保留与一次Space提交；追加scope未见同类stall，不宣称延迟预算通过。</p>
        <p>附件命名为两个应用 APK 与含子模块源码包。实际公开发布、下载核验及 Latest 以 GitHub 标签和 Releases 为准。</p>
        </details>

</details>

<details>
<summary>原 docs/index.html 历史记录 2（按原范围保留）</summary>

<details>
          <summary>T07 / v3.3.13-t9.10 与更早候选历史记录</summary>
          <p>以下原文中的当前、PENDING、Latest、旧 APK 身份与可调试构建说明只属于当时记录，不是9.12的当前状态或验收证据。</p>
        <p>稳定版目标 <a href="https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10">3.3.13-t9.10</a> 已完成C37构建/静态、API21/API35完整矩阵、真实SAF、Direct Boot压力、自然PIN及三轮冷启动验收。实际构建提交 <code>a5309c0256e325a0f042d2d9062ddbd4f1326c28</code>，冻结目录 <code>build/t07-privacy/stable-candidate37-final1</code>，时间戳1791464154404（2026-10-08T12:55:54.404Z）；完整构建708.260124秒，应用JVM582、build-logic9、Spotless与Lint0通过，编译/build warning行0，527项clean源码前后不变。</p>
        <p>C37以自有副本固定列出的FD，优先完整fstat设备/inode身份；仅真实EACCES可在两份声音fixture全部校准后使用有界fdinfo，严格唯一inode与全部fixture不同才证明非fixture。仅取得副本时直接原因EBADF可记录原槽位消失，固定后所有错误均失败。两矩阵完成原200次扫描、同文件0→1→2→1→0、9无关句柄、隐私/TTS/SoundPool/播放/释放与30秒预算。API21仅8探针、未用fdinfo fallback；API35有11探针及实际自有副本元数据证明。实际APK审计保留19 appDEX/6 testDEX、21类/148完整body及catch/603引用、未解析0/高于21为0，56反例全拒绝、两fresh replay的31输出逐字节相同、51文件/553依赖；ARM64仍只作构建/静态。</p>
        <p>QA3绘制身份归一化和精确旧PID INFO clean-exit分类保留实际focused112及完整raw/PID/context严格守卫；QA4只允许expected regular file且解析size0时的可信Toybox regular-empty别名，实际focused61保留原8字段、数值/inode/权限/mtime守卫、raw kind和stat命令，不编造numeric S_IFREG。QA5在父脚本内以lazy工厂隔离四核心DeviceRun子类，完整Matrix startup方法仅core2→4 literal不同，boot/storage/readiness/cleanup行为保持，Matrix脚本逐字节不变；其实际focused32和独立source peers通过。QA5 live98+139+56=293项一次PASS，<code>build/t07-privacy/reviews/candidate37-QA-epoch5-host-final1/final-review.json</code> SHA <code>cc3c12a760ff6c5637403d1ec3165c8ac0f690df743c19604e359774d622ff48</code>；当前受影响publication23项一次PASS，host END2026-10-08T16:49:26.873240Z，<code>build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final3/final-review.json</code> SHA <code>b21fbe762ac2804864ee2d552e3829bec2baada0c45665dadac701d9aa7894b0</code>。QA3发布68+23仅保留历史一次实测，END2026-10-08T14:58:52.115495Z，<code>build/t07-privacy/reviews/candidate37-release-QAepoch3-host-final1/final-review.json</code> SHA <code>89fbbdf892e9e21303d826c43d3fe1a620e7949bbadf3eb453408f14145db3c9</code>；未变的原adjudicator68与actual matrix-only adju2保持自身证据，QA5没有重跑。每次host运行绑定自身QA13及15/17/10依赖，旧全局QAepoch仅作历史快照；当前适用性peer <code>4faeddee008fb439bd7b0de07845d0d12084ab51b22bfbd87e0c509c682b52ee</code>保留矩阵/SAF/byteaudit实际执行范围；527项候选源码及三APK仍是已验收C37构建。 QA6将继承的唯一handoff后user-state读取保留为完整stdout/stderr及精确command index；重放要求该相邻读取唯一、rc0、stderr空且独立解析RUNNING_UNLOCKED，然后normal input仍须立即衔接，所有原actions/deadlines保持。当前live Natural56一次PASS，<code>build/t07-privacy/reviews/candidate37-QA-epoch6-host-final1/final-review.json</code> SHA <code>fe53a24b017ee6137010e858847800657e117871f0d845f901f7934bd7b3bec8</code>，受影响publication23一次PASS；parent与Matrix仍逐字节不变，当前适用性peer通过独立15项执行依赖保留pressure final5实际范围。QA5 host证据与更早全局QAepoch仅保留各自历史源码范围。</p>
        <p>矩阵、真实SAF三阶段、fresh pressure final5、独立自然PIN及三轮独立冷启动均以自身实际报告和独立peer通过。压力保留原7秒/1.25秒/6500ms/≤60Hz/DOWN→START+EOF→UP/UID2000/PIDbootbinding/一次真实Delete/资源和清理协议；Natural无controlled rebind或stress injection。仅覆盖两个矩阵的actual adjudication通过，validation errors0/unexplained blockers0，仍保留warning记录1558、known project diagnostics480，warning_free=false；该裁决不替代独立SAF/pressure/Natural/cold门禁。六门禁独立peer SHA依次为<code>7982a8d4d1b67d06e61577e1e1e1838a4c7bc3c7b03ad59539b7c6aa29cdf5e2</code>, <code>ffab0aba9a6d2bdcc9e584580dd2a8687e767ff51a561f4ceba32380789ffdbf</code>, <code>ff0e8cf843c0db09e2c06278bb2abb6c29a081557c49a552d5be65ad225b0eb0</code>, <code>964802c914e28a6589f1a819b1cb85b143553062550ed36077fe566acc3d85c2</code>, <code>5cd94ae1f2ac6817d64a73f6234d13351fa415cd5d2ab28d5b28f02def6199c5</code>, <code>89aa05b31f0a7cd76528a29b7b81689cb3e8435a17d5a7e74e30a2d2c8a186b5</code>。 Fresh pressure final5与Natural final2只覆盖明确记录的4-vCPU/2048MiB host-GPU环境，两个矩阵和冷启动仍覆盖原2-vCPU/2048MiB环境；不声称两核心环境7秒保证，也不声称已证实或修复CPU特定产品缺陷。 外部资源字节证明来自UID2000输入动作结束后的有界privileged read-only lease，随后还原UID2000；不证明AppUID直接访问。 应用APK：ARM64 <code>d36241f2e7e61145d7f01148732b44632c46149cc1f571bb3c5aeab66689e17c</code>（22,442,517字节）、x86_64 <code>2a6b7375d1c80595c736c0b64e9e9f34272d768f06fb78352767a5a97c10b05e</code>（22,539,554字节）；test <code>c1b41ba4149585d275c01f3c1b7be2289a71e6ce8fd024f6f5306188b60f28db</code>（1,480,837字节）。应用各保留2条META-INF v1服务提示、test0，min21/target37/versionCode20261115与原debug包/证书保持。API21的105条AudioTrack FAST提示保留而SoundPool功能完成；不声称物理ARM/16KiB页/OEM全覆盖或全部bug/warning消除。C37旧失败保留：P1 whole FAIL <code>be5d3c579b3e63498f0e6829c01836ae35947f321646ee899e7c72d1c40b45f7</code>; P2 whole FAIL <code>e1c9ca93eab763ee79a11a8ce02429081eec697fb24de96e4ae9f8469445424d</code>; P3 whole FAIL <code>92105ccfe5d210ff16804525cf35221e5e3fb20ae0ac62a4241e8b184899d9c7</code>; P4 two-core whole FAIL <code>29740315c373a5173f6a945295652912ff7348e04fb5586758b3baf131432d71</code>; first matrix adjudication FAIL17 <code>b468ea8f97cc2d4f74cc11805843268c00fd7e048db0ea8d23d630142b0bdd21</code>；局部handoff/Delete不计whole或资源通过，P2资源审计body从未开始。发布目标为已授权稳定tag/链接，六文档提交后继续递归源码归档、两应用APK staging及11项实际evidence验证，不编造此前已published时间或receipt。 P3原inventory87文件、81份四步证明完成，#82 LOCK仅stat-before；其cat/device hash/stat-after和剩余5文件未到达。P4耗尽未放宽的7秒观察预算，接近边界的新进程fork未提供完整预算内进程或ready-input证明；独立时序/源码诊断 <code>939b16394b5bc4a727390ba834c9b36dd7038d7bbaf4d609d30715fde2cbf2f6</code>/<code>c8b9b4de1a2c8efa25318a36610d9997537aa23a12bb2ce8bc20e612206488a7</code>没有建立可支持的产品延迟修复，也未把LatinIME主动自杀解释为fatal/OOM。两次原失败只留历史；当前whole验收仅来自明确四核心范围的新pressure final5。 Natural1原whole FAIL <code>ace1a169196e44828639a99da52476a948af0207ed5401386fdb596f817bc73c</code>保持：bounded handoff及Delete/UID2000局部输入证明完成，但固定尾部读取的span bookkeeping失败，资源审计未到达；独立诊断 <code>e020a11ed125e8cd759706f59b1b01022ddfc4901f5528e09efdf260128a1c93</code>及diagnostic replay <code>0cddccef95d8df463433b6913e615958e00c887ad9301479ee2dcfe05ee1d050</code>不重分类旧run，当前自然路径验收只来自新Natural final2。 首次publication23 fixture尝试因audit推导前缺少locked-log输入而失败，<code>build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final1/failure-summary.json</code> SHA <code>43fe8d2516df7b13e12f10875ffccaeb24babbf5f57d8300388d7fdd7174424a</code>，该失败host run保持历史。第二次尝试22个方法通过，但旧反例重排记录后未重新封存命令索引而有1个失败，<code>build/t07-privacy/reviews/candidate37-release-Natural2-publisher-correction-host-final2/failure-summary.json</code> SHA <code>c863cc1d87827e145cedd8c91d1148bdd409937c6ad69f2c1ad81ffd6aac3367</code>，仍保留为失败host尝试；最终23验收只采用修正fixture/反例自身完成且绑定实际源码的报告。</p>
        <p><strong>C36/C35 历史记录：</strong>以下保留当时原文及失败范围，不能借用为 C37 验收；本次状态以上方 PENDING 记录为准。</p>
        <p>拟发布稳定版 <a href="https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.10">3.3.13-t9.10</a> 当前 <strong>candidate36：构建/静态及 Android 5.0 完整矩阵通过，Android 15 完整矩阵整体 FAIL，稳定发布停止</strong>。C36 提交 <code>97d38863a4353fbdde95eca3592a60e4a653887e</code> 仅修改 AndroidTest 的 <code>InputFeedbackProbe.kt</code>，生产代码未改；冻结目录 <code>build/t07-privacy/stable-candidate36-final1</code>，时间戳 <code>1791460043820</code>（2026-10-08T11:47:23.820Z）。完整构建 458.793 秒、应用 JVM 582 项、build-logic 9 项、Spotless、Lint 0、编译/build warning 行 0，527 源码不变；C36 新 APK 身份/签名/ABI/48 资源/16 KiB ZIP/ELF 静态通过。应用各有 2 条 META-INF v1 服务提示、测试 APK 0，min21/target37 和原 debug 证书/包保留。415 项设备 QA 宿主及独立源码复核通过，11 QA 摘要冻结。C36 Android 5.0 实际完成 8 探针、13 路线、17 生命周期、100 次显示隐藏、52 个 glyph/4 次点号提交及降级输入/重试；独立复核 925 项通过。Android 15 随后在声音探针 PID5442 首次 FD 快照的 <code>Os.stat</code> 收到 EACCES，整体失败；具体 FD 槽位未记录，不能宣称已定位或修复原因。其真实持有剪贴板 UI 和此前五项探针仅作局部通过。专项字节码、发布工具最终绑定及其余门禁仍待新完整证据：C36整体运行验收FAIL：API21 whole PASS、API35 whole FAIL，其余未运行门禁不借用旧/新候选；稳定发布已停止。声音探针修正后的后续候选须重新构建并完成全部自身验收，不能借用 C36 API21 或 C35 通过。未推送、打标签或发布，Latest 仍为 <code>v3.3.13-t9.9</code>。</p>
        <p>C35 当前 QA 的 Android 5.0 final2 矩阵已整体 FAIL：<code>InputFeedbackProbe.kt:103</code> 在 200 次同步读取后触发全进程 FD 增量超过 8 的断言，实际 before/after 数值与 FD 目标未记录，来源仍未证实，不能称已定位声音读取器泄漏或已证明假阳性；后续 SoundPool/旋律/播放检查未执行，顺序安排的 Android 15 正式矩阵未启动。C36 将该测试改为声音 fixture 的 <code>(st_dev, st_ino)</code> 逐 FD 槽位归属计数，保留全进程总数作诊断；同一文件打开两次的 1→2→1→0 控制与 9 个无关句柄控制已完成宿主复核，API21 实际校准已通过，API35 在首个快照过程中 EACCES 失败；修正后的新候选仍须重新校准并完成全部门禁。原 200 次读取、30 秒预算及隐私/TTS/SoundPool/播放/清理要求保持；不增加 GC、等待或容差，也不注入损坏 YAML 警告。</p>
        <p>C35 历史记录（以下当时状态以上方 C36 记录为准）：candidate35 构建通过，完整设备验收未完成。统一隐私与固定上游 <code>e9b21960</code> 的 30 项整合提交保留；C35 使用 WorkManager 2.10.5 官方 Configuration.Provider 和稳定 lazy 配置，统一 getInstance 初始化路径，修复普通进程恢复时的双重初始化竞态，并由 SDK21 短路及实际 DPS/旧 DE/替换状态守卫拒绝不安全进程初始化。实现提交 <code>dc69d73baa0c06e5a103000d1cd13f106f280afd</code>，527 项源码，时间戳 <code>1791449483471</code>（2026-10-08T08:51:23.471Z）；正式冻结目录为 <code>stable-candidate35-final1</code>，早前中断构建没有形成身份链。完整构建 519.403 秒通过，应用 JVM 582 项、build-logic 9 项均通过，Spotless 通过，Lint 0，编译/build warning 行 0；APK 身份、静态与专项 API21 DEX 检查通过。应用 APK 各保留 2 条 META-INF v1 提示（test 0）；旧 QA 的 C35 Android 5.0 矩阵与独立审阅真实通过，但当前 QA phase4 的 Matrix/绑定工具已变，API21 矩阵须全新重跑；未改的真实 SAF 三阶段与归属审阅经当前校验器复核后保留通过；首次 SAF 尝试因准备末尾保留 Latin、Main 重定向 Setup3 而在任何选择器动作前停止，不计 SAF 通过或产品缺陷，新的 gate 前准备恢复并核验 Trime，不写 SAF tree/首选项/grants；SAF 按实际进程归属的独立日志审阅亦通过，其余设备门禁与最终裁决仍待证据，C35 整体运行验收未完成，API21 final2 整体 FAIL（历史）。C35 Android 15 matrix final1 整体失败：真实持有剪贴板 UI 与九项探针通过，ClipEditorProbe 未确认稳定键盘，后续导航/生命周期/降级未执行，失败保留，后续解析诊断不改写该矩阵结论。C35 压力 final1 整体失败：原 7 秒交接与完整 6501 ms 手势通过，但第十阶段在既定一次 focus 前要求键盘，因此未完成真实 Delete/资源审计；失败证据保留，压力 QA 顺序校正通过宿主及独立 peer，但 final2 随后在 mutation-3 超过原 1.25 秒而整体失败，6503 ms 手势只作单项。epoch3 完整窗口短诊断仍是整体 FAIL。只读重放确认 75 份 Latin 帧中，第一份过渡帧仍被当前绑定守卫拒绝；其余 74 份完整显示且当前连接有效的帧带有 <code>mRestartOnNextWindowFocus=true</code>。Android 15 将其定义为未来窗口获得焦点时的重启请求，旧 false-only 谓词错误拒绝了这些有效帧。phase4 已协调应用严格唯一的 true|false 解析，保留当前 PID、序列、有效连接、方法、EditorInfo 与可见性检查；未来标记原值继续记录，但不进入稳定绑定身份。独立复核还发现初次 /tmp 普通输入提案会漏掉空值、等号前后空白或裸字段名的重复字段，已在任何 live 应用前修正，两套普通输入测试均拒绝这些重复字段。随后全新 phase4 Android 15 剪贴板短诊断及完整原探针断言实际通过，只执行一次 Latin→fresh Trime；独立运行复核仍待完成，短诊断不计完整矩阵或发布验收。精确应用后工具的 415 项宿主检查全部通过；宿主与源码复核只证明解析修正，不把旧诊断改成通过，也不代替新设备结果。当前 API21/API35 矩阵、压力与自然 PIN、冷启动及最终裁决均待全新证据。C31–C34 结果只作历史；C34 自然交接及一次 Delete 部分证明不能代替其整体失败。原 7 秒/1.25 秒/6500 ms/60 Hz 规则保留，特权只读文件审计单独标记，不伪称应用 UID 访问证明。现有 debug 证书/包和 C31 历史 SAF 部署 6,280 ms 排队限制保留；C35 本次目录选择/完整部署也记录一次非注入的 5,434 ms Rime 排队，早于 instrumentation，三份快照为同一事件，残余性能限制保留；最终 SAF 归属审阅保留一次 root 系统 input.Input 命令退出时的 native SIGSEGV，按 PID 确认为系统命令而非 Trime，不能称完整设备日志无 fatal；ARM64 仅构建/静态检查，无 ARM64 真机或原生 ARM 镜像验收。当前未推送或发布，Latest 仍为 <code>v3.3.13-t9.9</code>。详见<a href="https://github.com/X2M7/trime/blob/develop/doc/releases/v3.3.13-t9.10.md">发布草稿</a>及<a href="https://github.com/X2M7/trime/blob/develop/script/quality/VALIDATION-T07-PRIVACY.md">T07 验证报告</a>。</p>
        </details>

</details>

<details>
<summary>原 doc/releases/v3.3.13-t9.12.md 记录（未完成发布，仅历史范围）</summary>

# v3.3.13-t9.12 发布说明草稿

当前CODE `3bb852b6` 仅修复Debug隐私剪贴板前台夹具：可聚焦的非编辑器窗口保持剪贴板可读，明确禁止连接输入法；原学习、插件、持久化、日志和清理断言保持。552项源输入中的变更仅这两个测试文件。source peer及新host8的206项真实检查通过，前后源码/宿主输入不变。

calibration-build6限定Debug构建278.392秒通过，app663fresh/Spotless/构建警告0；未跑Lint、Release或完整构建。校准5的editors（40 rapid/100显示隐藏）和T08子测通过，但privacy等待窗口焦点15秒超时、checks0，且全PID记录系统Pico SIGSEGV1，整体失败保留；T09/SAF未执行。后续仅privacy的诊断1原执行器通过16项/3方案、owned正常关闭且critical/LMK/kernel0，焦点/ALT/非编辑器/daemon空断言通过，独立限定审阅已通过；不代替多探针校准或四组正式验收。五条只读焦点命令原文存在，采集helper因cmdline额外NUL padding自行失败、没有截图。

旧CODE `b32f8b68` 的FullBuild5完整构建与静态通过，API21 Release7单组独立通过；随后Debug4在隐私前置条件失败，checks=0，稳定晋级停止。editors/T08及真实持久授权准备仅子测通过，T09与SAF三阶段未到达。Debug4原2394条W/E和7062ms启动部署队列等待保留，不声明隐私泄露、稳态P95或警告全部消除。

新CODE的限定校准、完整Build6、双Release静态与四组fresh正式验收仍待完成；旧候选和校准结果不替代。发布及Latest未宣称变更，手动附件仅两个新Release APK和递归源码包，CODE与后续DOCHEAD分别绑定。真机P95/PSS、驻留/首次部署、ARM64/OEM运行及一般准确率未测。详见 [9.12验收报告](../../script/quality/VALIDATION-t9.12.md)。

<details>
<summary>FullBuild5发布说明草稿历史（未发布）</summary>

以下“当前”仅指原b32/FullBuild5或更早范围，不供新CODE3bb验收。

本版纳入T09中英、数字与符号混输：九键有明确QWERTY英文入口、手动语言记忆与临时输入框强制模式，符号往返保留待提交组合。预编辑/浮动候选使用主IME可见触摸Region，修正自身滚动坐标；补齐SAF不完整列举保护、StorageAccess2适配及日志导出IO/取消处理。

API21 Release6六轮UI/升级子测通过但执行层exit143、未闭合，原producer仍false；信号来源未知。原件保留历史，API21 Release7原runner验收已闭合通过：两主题各3轮、共54组PNG/XML及实际升级，UID10053/偏好/虚构哨兵保持；owned3804598正常wait exit0。该Build5单组独立审阅及警告裁决已通过，其余三组和发布仍待完成，原raw3028条W/E保留。

编译CODE `b32f8b68` 的完整Build5通过，552输入不变：Debug519.642秒、Release666.339秒，app663与host7的206项fresh通过；convention17/compatibility6缓存，双Lint与构建警告记录0。API21编辑器校准4及独立审阅通过真实触摸、两主题隐私、40 rapid/100显示隐藏且owned关闭，2078条W/E全部保留；只提供限定校准范围，不替代正式四组验收。

新双Release APK静态/签名及独立协程审阅通过，v1/v2、签名警告0、完整DEX等价与实际排除Debug夹具已核验。**四组API21/API35×Debug/Release正式验收及稳定发布仍待完成：该Build5已因Debug4失败停止晋级。** 后续DOCHEAD与APK编译CODE分别绑定。旧Build4/Release5范围P、Debug3夹具F和格式失败保留历史，不供新CODE验收。

手动附件仅两个新Release APK和完整递归子模块源码包；测试APK、QA包、身份/日志/报告不上传，GitHub自动Source code链接保留。

| 附件 | 字节数 | Build5 SHA-256 |
| --- | --- | --- |
| trime-v3.3.13-t9.12-arm64-v8a.apk | 11,786,772 | `8f2f1c561635ad344681b1f4c4fe319d576c5a8ddc62c36116501497cfff3faa` |
| trime-v3.3.13-t9.12-x86_64.apk | 11,865,939 | `ef5962e7f3a39e3dfdc79f8b6e155f226421df1222c8f5ab401852a65dfb4706` |
| trime-v3.3.13-t9.12-source-with-submodules.tar.gz | 归档后外部记录 | 完整递归源码，外部归档/公开下载收据核验 |

新静态收据已核验正常Release/R8、minAPI21、日常tracing=false和原证书；实际覆盖升级仍须本轮设备证据。新正式系统框架/图形W/E/F及归属：（原草稿此项待填，未完成发布）。旧Release5的3175条W/E完整保留，不能借旧项目tag0清除本轮诊断；零构建警告不表示运行环境warning-free。

真机P95/PSS、驻留、首次部署、ARM64/OEM运行、一般准确率与物理16KiB页设备未测；2秒停顿门槛不是P95≤100ms。T09改善仅限定义过的三项可比子集。详细证据及历史见 [验收报告](../../script/quality/VALIDATION-t9.12.md)。分支/tag、三附件公开下载与Latest以实际GitHub发布收据为准。

</details>


</details>

<details>
<summary>原 script/quality/VALIDATION-T09.md 记录（未完成发布，仅历史范围）</summary>

# T09 混输实现与验收记录

当前CODE `3bb852b6` 仅修复Debug隐私剪贴板前台夹具：可聚焦的非编辑器窗口保持剪贴板可读，明确禁止连接输入法；原学习、插件、持久化、日志和清理断言保持。552项源输入中的变更仅这两个测试文件。source peer及新host8的206项真实检查通过，前后源码/宿主输入不变。

calibration-build6限定Debug构建278.392秒通过，app663fresh/Spotless/构建警告0；未跑Lint、Release或完整构建。校准5的editors（40 rapid/100显示隐藏）和T08子测通过，但privacy等待窗口焦点15秒超时、checks0，且全PID记录系统Pico SIGSEGV1，整体失败保留；T09/SAF未执行。后续仅privacy的诊断1原执行器通过16项/3方案、owned正常关闭且critical/LMK/kernel0，焦点/ALT/非编辑器/daemon空断言通过，独立限定审阅已通过；不代替多探针校准或四组正式验收。五条只读焦点命令原文存在，采集helper因cmdline额外NUL padding自行失败、没有截图。

旧CODE `b32f8b68` 的FullBuild5完整构建与静态通过，API21 Release7单组独立通过；随后Debug4在隐私前置条件失败，checks=0，稳定晋级停止。editors/T08及真实持久授权准备仅子测通过，T09与SAF三阶段未到达。Debug4原2394条W/E和7062ms启动部署队列等待保留，不声明隐私泄露、稳态P95或警告全部消除。

新CODE的限定校准、完整Build6、双Release静态与四组fresh正式验收仍待完成；旧候选和校准结果不替代。发布及Latest未宣称变更，手动附件仅两个新Release APK和递归源码包，CODE与后续DOCHEAD分别绑定。真机P95/PSS、驻留/首次部署、ARM64/OEM运行及一般准确率未测。详见 [9.12验收报告](VALIDATION-t9.12.md)。

<details>
<summary>FullBuild5及更早T09原记录</summary>

以下“当前”仅指原b32/FullBuild5或更早范围，不供新CODE3bb验收。

API21 Release6六轮UI/升级仅子测通过；执行层exit143、producer原false/未finished，不能供正式验收。原件保留，信号来源未知；API21 Release7原runner验收已闭合通过：两主题各3轮、共54组PNG/XML及实际升级，UID10053/偏好/虚构哨兵保持；owned3804598正常wait exit0。该Build5单组独立审阅及警告裁决已通过，其余三组和发布仍待完成，原raw3028条W/E保留。

当前T09随 `v3.3.13-t9.12` 候选纳入，编译CODE为 `b32f8b68`。完整Build5（552输入、Debug519.642秒/Release666.339秒、app663fresh、host7=206fresh、双Lint/构建警告0）通过；convention17/compatibility6缓存。API21编辑器校准4及独立审阅通过40 rapid/100显示隐藏并owned关闭，2078条W/E保留；新双Release静态/签名/协程审阅通过，签名警告0；四组正式验收和发布仍待完成。旧Build4/Release5范围通过及Debug3夹具失败保留历史，不提供新CODE验收，详见 [9.12验收报告](VALIDATION-t9.12.md)。后续文档HEAD与APK编译CODE分别记录。

历史严格成本比较仍为false；7个匹配任务未退步，只有标准主题Android16/Wi-Fi/B站子集导航14→6、总动作39→31，email/URL起终键盘不同不可比。不把此结果推广到全部任务、物理触摸性能或一般准确率。真机P95/PSS、首次部署、驻留与ARM64/OEM运行仍未测。

## Build8历史原件

以下原文全部属于 `d4994026` / Build8及当时boot。“当前”、55弃用、2META-INF、旧Popup门禁与未决停顿均是历史范围，不提供9.12通过证据；原SHA、失败与限制保持。

### 原报告：T09混输实现与模拟器验收（2026-10-10）

当前编译提交为 `d49940262bf5dce4a120e053e54dd5c93e614291`：543个构建输入、25条审阅路径，构建8及独立字节审阅通过。两个API的14固定任务、8稳态、4快速顺序和4宏回归均已通过并封存，原生候选与编辑器额外功能回归也通过。**两API的语言功能、恢复、停止及完整日志证据已闭合；当前正常Release在Android 15的两主题六轮浮层回归也通过。T09功能实现与这些限定范围的模拟器验收完成，性能及warning-free验收仍未完成。** 严格成本比较已执行，结果为 false，原因和可比子集如下。

公开的 `v3.3.13-t9.11` 是T08稳定版，不含T09。T09源码和本记录同步到 `X2M7/trime:develop`，不新增稳定标签。文档提交晚于编译提交；当前APK的编译来源仍是 `d4994026`，不能记作文档HEAD。

## 实现与功能范围

九键界面提供明确的QWERTY英文入口及中文返回，仅显式语言动作持久化 UNSET/NATIVE/ASCII；强制编辑器模式和符号导航不写入手动记忆。方案或主题变化重新解析布局，普通UNSET方案切换不再沿用旧锁定九键。原Rime单线程FIFO保持输入、语言动作和模式宏顺序；符号往返保留组合，不把九键raw数字串莫名当英文提交。

虚拟可打印ASCII使用既有InputConnection文本提交路径，避免后发按键事件落在新中文组合之后；物理键、TYPE_NULL、非打印键和快捷修饰仍沿用事件路径。已知虚拟映射键沿用旧按键路径的内容副作用语义，不更新 `lastCommittedText` 或触发提交朗读；普通公开/native文本提交仍保留原上下文及朗读行为，隐私禁止时清空上下文并禁止朗读。标准主题英文页新增可见 `+`、`&`、`=` 长按路线；动态T9无障碍描述随当前模式及缓存/重建View刷新。未增加逐候选JNI查询或输入内容日志。

这些行为由下表限定的实际功能检查支持。跨进程语言记忆、临时编辑器覆盖、方案/主题组合及待提交64无Space切入英文的API21额外范围已通过；API35终态功能报告及独立源、身份、恢复停止和完整日志证据闭包已完成。普通警告仍未解决，证据封存不表示warning-free。

## 已封存结果

| 范围 | 实际结果 | 边界 |
| --- | --- | --- |
| 构建8 | host203＝既有174＋当前虚拟文本29；应用JVM663本轮执行，0失败/错误/跳过；build-logic9缓存；双Lint0、Kotlin警告0 | 旧IC18及草稿54未重跑、不重复计数；独立报告 `13a70272…fe92f8` |
| Release静态身份 | ARM64/x86_64、minAPI21、签名/ELF/R8/Provider/48资源和4940 DEX定义检查通过 | QA包为 `com.osfans.trime.debug`，nondebug、tracing=false；未在ARM64真机运行 |
| API21成本采集 | `implementation-api21-7`：14＋8＋4＋4全通过，原状态恢复及owned停止完成 | 逻辑KeyView动作、真实native/IC文本；不是实物触摸或延迟测试 |
| API35成本采集 | `implementation-api35-1`：同样30项通过，父整轮封存、owned停止完成 | 原Scope3参考恢复失败与独立恢复成功分别保留 |
| API21/35 T08额外回归 | API21 extra4、API35 extra1：原生8项、320顺序输入、49候选分页、编辑器100轮及新增动态描述检查通过 | API35 Debug窗口停顿未决，不由功能成功抵消 |
| 两API新参考 | 每API14个实际状态、42角色和3个主对话框，经Root实际查看原PNG/XML后本地冻结 | 原图/节点/RGBA、编译来源、APK、boot/PID及独立schema侧证绑定；不是语言额外验收 |
| API35原Scope3与Recovery3 | 原报告 `f6330b40…5700` 仍FAILED；独立恢复报告 `36197878…bf3b` 仅cleanup通过 | 原5个文件的字节/存在性及YAML原UID/GID/mode/FUSE元数据在停止后恢复，随后新PID7264恢复UNSET/Trime/T9；原错误不改写 |
| API21语言额外 | 完整4个case/24份proof、12次owned进程重建和2次待提交切换通过，原状态恢复及停止；独立闭包 `2d121416…52b268` | 真实可见键、observer2实际光标/组合范围和跨进程状态；普通警告影响UNKNOWN，非正常Release或物理预算验收 |
| API35语言额外 | `language-extra-api35-1`：24份proof、12次真实进程重建、2次过渡、原状态恢复、新Main、owned停止及完整日志证据闭包完成 | 完整闭包 `7ae094d0…47abb`；源/功能 `3a2570ae…0aa2e`，身份/observer/42角色 `6217b9d3…06efc`；普通W/E/F及GC仍UNKNOWN，machine/warning-free均false |
| 当前正常Release浮层 | `normal-popup-api35-2`：两主题简化字全拼各3轮通过；Popup 1→0→1，提交后0；同Main2199，每轮精确提交一次你；独立闭包 `8ea8a3d7…db8d` | 当前Build8 x86_64/API35正常Release，非公开9.11 APK；原文件在应用停止时恢复，未采集恢复后的新Main；normal1中断0轮记录仍未通过 |

语言额外的进程重建实际验证同包UID/cmdline、旧PID消失和新PID变化；未捕获guest Main starttime，不追加该断言。主题/方案通过实际UI切换，密码编辑器的强制模式不覆盖手动偏好，返回普通输入恢复原记忆。待提交64→英文检查先绑定实际selection末尾，不点候选或Space，切英文得到精确 `你`，再输入 `a` 得到 `你a`，延迟复读相同。API21实际inline与API35 display-only展示分别保留，不把inline预编辑或宿主精确文本冒充native/IC回调计数；固定任务的Android探针另有真实提交计数。

每Gradle构建阶段保留55条上游弃用提示（52 AGP、3 KSP），每个Release APK保留2条META-INF签名提示；不称warning-free。R8优化/缩减与资源压缩启用，`-dontobfuscate` 保留类名。

上述API35语言与Debug成本范围的外部 `user.yaml` 在精确限定路径上使用特权Root审计；其CE/DE仍使用已绑定Debug AppUID。该读取不能证明普通AppUID外部存储可读或native回调。恢复只对独占nonce临时文件修正原UID/GID，经全字节及元数据复读后才替换目标；原Scope3失败与独立cleanup成功不是同一状态。

## 严格成本比较

冻结比较器已对完整旧基线与新终态实际执行，环境匹配通过，正式结果 `false`（报告 `fea14c9b…746a2e7`）。每API均有7个起终态匹配的成功任务、3个Trime新增可达能力、4个邮件/URL状态不可比任务；可比任务没有导航或总动作退步。旧基线每API固定11 PASS/3 UNSUPPORTED、稳态4 PASS/3 UNSUPPORTED/1 FAIL保持原样，采集完成不是旧产品全部通过。

两API的实际逻辑动作结果相同：

| 可比任务 | 总动作：旧→新 | 导航动作（mode＋layout）：旧→新 |
| --- | --- | --- |
| 标准主题 Android 16 | 15→13 | 4→2（下降50%） |
| 标准主题 Wi-Fi | 13→9 | 6→2（下降66.7%） |
| 标准主题 B站 | 11→9 | 4→2（下降50%） |
| 两主题电话 | 14→14 | 不变 |
| 两主题金额 | 6→6 | 不变 |

同起终态的标准主题三个任务，每API总动作合计39→31（下降20.5%），导航动作14→6（下降57.1%）。这个子集的改善不能推广到全部14任务，也不是物理触摸性能。

Trime旧英文路线不可达，新Android 16/Wi-Fi/B站任务分别13/9/9动作，仅记能力增加，不能以旧部分3动作当0成本或算改善率。两主题邮件25→25、URL36→36仅是动作观察：旧 `__trime_compat_letter` 与新 `letter` 的真实起终键盘ID不同，四项每API都保留不可比、delta/百分比为null。未归一化ID或修改原比较合同；不能宣称全基准通过。快速顺序、宏、语言记忆、参考准备和浮层诊断不加入14任务成本。

## 全日志与性能限制

当前API35成本父/子全文分别保留4536/2557条W/E/F，独立Recovery3全文4520条；all-PID critical、实际LMK、TrimePerf及捕获console的kernel critical为0。普通警告仍为 **UNRESOLVED**，原文、行结束SHA和上下文保留，不能把机器critical0说成warning-free。ART/GC及跳帧诊断也保留，不推出延迟预算结论。

另一个已封存API35 Debug编辑器范围保留11条独立窗口记录，对应两次约4秒 `ViewRootImpl.doDie`/`dequeueBuffer -110` 停顿及240帧跳过。父/子重复不算22次；精确主题/子用例缺少时间标记，不能补推或归因于无障碍修复，状态仍 **PERF_UNRESOLVED**。当前成本范围没有相同字面签名，也不能清除这个不同范围的异常；本次正常Release六轮没有相同字面签名，仍保留Debug未决事实；两种构建的不同结果不证明根因已消除。

API21成本为1536MiB/1CPU/swangle、Vulkan关闭，实际720×1280/320dpi/360dp且三条WM查询证据匹配；API35成本为3072MiB/4CPU/host、Vulkan关闭，旧基线没有直接WM density/override采样，不从截图或AVD推断。API35未开启show-kernel，console结论仅限已捕获内容。

此前真实产品失败、旧参考描述错误、旧工具失败和四次dd SIGPIPE Fatal整轮失败均保留；独立恢复不抵消原FAILED，不补写未确认根因。语言额外和当前正常Release的原图批准、全日志、恢复及停止证据均已闭合，不覆盖这些历史记录。

真机候选P95≤100ms（明显更慢时先下降30%并确保准确率不退步）、Java/native/graphics/总PSS、连续输入和主题切换驻留、首次部署、ARM64/OEM运行及一般准确率仍未测。模拟器功能、固定文字与逻辑动作账本不代替这些指标。

正常Release首次范围的parent在会话中断后消失，owned QEMU由Root按原PID/exe/PGID/starttime精确停止；0轮产品检查、无已接受UI图，原状态恢复未宣称。原报告与独立中断收据 `e32d9409…efce`保留，新normal2不能把原范围改写为通过。辅助Root UI工具仅修正ART argv[0]末尾NUL填充校验，精确包名、UID/starttime、APK、备份及原图批准等门禁保持；9项纯测试原执行通过，独立42文件SHA及逆向全字节审阅 `9c63b171…1d2dd`通过，不是设备浮层验收。

API35语言范围父/子全文分别62887/62017行，W/E/F分别14545/14426条，GC/等待/跳帧各77条，全部保留UNKNOWN；环形缓冲区重复不合计为独立事件。all-PID critical、实际LMK、TrimePerf、捕获console内kernel critical及字面4秒窗口签名均0，完整guest kernel捕获未建立。`machine_log_scan_passed=false`、`warning_free=false`，不豁免普通警告，也不清除不同Debug范围的两次约4秒停顿。

本次仅将T09源码、README和本记录同步到fork的 `develop`，不替换公开T08稳定版的标签或附件。本次文档提交不改变当前543个构建输入；APK编译源码仍为 `d4994026`。实际推送后按远端分支提交核对同步结果。

## Build8正常Release的历史收据

正常APK SHA-256为 `18689c2fadcd2c7ab64a78e23a180199b3f01633ccc584ff30afd032b3708cbc`，nondebug、minAPI21、tracing=false。API35使用3072MiB/4CPU/host、关闭Vulkan；boot为 `dcad0a15-7744-4b29-882e-3d60660e37c2`。主题与 `luna_pinyin_simp` 通过当前实际UI设置；Root查看新原PNG/XML后批准n、i及真实空格键。六轮共30个实际按键/显示隐藏操作，始终Main2199/UID10148，输入ni、隐藏、恢复、空格提交准确一次 `你`。独立审阅核对3905份原件、918条父命令和36个轮次状态。

正常Release的CE929B、DE914B和YAML341B均由Root对精确路径审计，在所有所属应用进程停止后按原字节、存在性、stat/SELinux元数据恢复；三次DD计数、双读和独占nonce临时文件及诊断清理均有实际收据。这不证明普通AppUID读写权限，也没有恢复后新Main的验证。模拟器3517314正常停止并wait exit0；父验收进程exit0，结束于2026-10-10T00:08:36.739666Z（北京时间08:08）。

独立闭包完整SHA-256：`8ea8a3d7fe1ee47705432facd9b7685349488a41a0a2de288eef049b9effdb8d`。全日志4660465B、33726行，SHA-256 `dc780593689c4467144ed6b06f7e1522516ad7db91f3e067673d1061d88f31a9`；all-PID critical、实际LMK、约4秒浮层字面签名、TrimePerf及已捕获console严重事件均0。5968条普通W/E/F（其中owned34条）及318条GC/帧等待仍为UNKNOWN，machine/warning-free均false；未开启show-kernel，不声称完整guest kernel历史。原审阅的import及标记子串计数错误保留，最终按冻结的完整标记解析闭合，未改写原日志或生产源码。

</details>


</details>

</details>

</details>

</details>

</details>

</details>


</details>
