# PrioSort ProGuard rules
-keep class com.priosort.app.data.local.entities.** { *; }
-keep class com.priosort.app.domain.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }
