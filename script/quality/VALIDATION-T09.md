# T09 混输实现与验收

实现及限定范围的模拟器回归通过。APK编译源码为 `1d78a9dce1eab27a092c2f6ac0c12e9c38036860`，验收使用同一FullBuild12冻结产物；稳定发布状态以 [GitHub Release](https://github.com/X2M7/trime/releases/tag/v3.3.13-t9.12) 为准。

两个内置主题提供九键到明确QWERTY英文页的入口、中文返回及手动语言记忆；输入框的邮箱／网址／数字强制布局只临时生效。主布局与符号页往返保留未提交组合，九键数字串不隐式作为英文提交。按键、语言动作与宏沿Rime单线程FIFO保持顺序；可打印虚拟ASCII使用既有InputConnection文本提交路径。

FullBuild12的663项应用JVM与host15的206项检查通过。Android 5.0 Debug10及Android 15 Debug5各实际运行30项混输场景，包括上游中文Shift复位、旧通知与排队英文动作隔离、宏返回和编辑器强制布局；原FINGER、native候选／提交和终端断言保留。受控modifier设置及直接callback不称为全部外部OS触摸。两版正常Release另以真实界面在两个主题各运行三轮输入、候选提交及显隐，并验证覆盖升级；这些六轮原生验证不等同于Release重跑全部30项混输矩阵。

切换成本的旧严格比较为false；7项相同任务未退步，只有标准主题Android 16、Wi-Fi、B站子集导航14→6（约57%）、总动作39→31（约21%）。邮件／URL起终键盘ID不同，不能作同口径改善。一般准确率对照、真机正常Release候选P95≤100ms、Java／native／graphics／总PSS、驻留与首次部署仍未测；两秒停顿门槛及40次rapid墙钟回归不代表候选P95达标。

完整结果、系统W/E/F和失败历史见 [9.12验收报告](VALIDATION-t9.12.md)。旧CODE的通过结果不供当前APK接受：保留FullBuild11首2462ms门槛失败、有限Perfetto采样、三次ART失败及SIGQUIT点采样；旧Pico native崩溃和guest DMA／kernel失败也不宣称已修复。Debug fullCheck属于维护检查，首次部署须单独测量，不能描述为每次弹键盘必定全量重编译。
