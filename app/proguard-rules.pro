# Keep BCrypt
-keep class org.mindrot.jBCrypt { *; }

# Keep Room entities
-keep class com.budgetapp.data.local.entity.** { *; }

# Keep Retrofit interfaces
-keep,allowobfuscation interface com.budgetapp.data.remote.** { *; }
