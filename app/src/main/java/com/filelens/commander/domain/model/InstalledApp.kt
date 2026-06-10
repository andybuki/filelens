package com.filelens.commander.domain.model

data class InstalledApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isUpdatedSystemApp: Boolean,
    val sourceApkPath: String,
    val installerPackageName: String?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val apkSize: Long,
    val targetSdk: Int,
    val minSdk: Int
)
