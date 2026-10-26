# Keep line numbers so release stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Library-specific keep rules (Retrofit/Gson/Room/WorkManager) are added in the phase that introduces each library.
