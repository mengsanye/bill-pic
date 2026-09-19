# BillPic 上架包（release）混淆规则
#
# 原则：Compose 与 AndroidX 已自带 consumer rules，项目自身不使用反射、不接序列化框架，
# 因此这里只做最小保留——多余 keep 会让 R8 白跑，还会把包撑大。
#
# 关键收益：release 包里 TelemetryProvider 指向空实现，R8 会把整条埋点链路
# 判定为不可达并删除，包内不会残留任何埋点事件名字符串。

# 保留行号，线上收到的堆栈才可读（配合 -renamesourcefileattribute 隐藏原始文件名）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# FileProvider 的 authority 由 manifest 声明、系统按包名解析，需要保留 provider 子类名
-keep class androidx.core.content.FileProvider { *; }
