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

# SLF4J (transitive of junrar / smbj) — missing provider is fine
-dontwarn org.slf4j.**

# Apache Commons Net (FTP) — uses reflection-free APIs but keep for safety
-keep class org.apache.commons.net.** { *; }
-dontwarn org.apache.commons.net.**

# JSch (SFTP) — reflective lookup of cipher/mac/kex providers
-keep class com.jcraft.jsch.** { *; }
-dontwarn com.jcraft.jsch.**

# smbj (SMB2/3) + Bouncy Castle dependency
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
