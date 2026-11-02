# Keep line numbers so release stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Library-specific keep rules (Retrofit/Gson/Room/WorkManager) are added in the phase that introduces each library.

# Gson/Retrofit reflect over these request/response classes; keep their field names.
-keep class com.asifqureshi.gitforge.data.remote.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
