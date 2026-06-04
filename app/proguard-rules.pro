# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Apache Commons Compress (7z) — uses reflection for codec dispatch
-keep class org.apache.commons.compress.** { *; }
-dontwarn org.apache.commons.compress.**

# Tukaani XZ — LZMA2 codec used by 7z
-keep class org.tukaani.xz.** { *; }
-dontwarn org.tukaani.xz.**

# junrar (RAR4 archives)
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**

# SLF4J (transitive of junrar) — missing provider is fine
-dontwarn org.slf4j.**
