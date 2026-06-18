package com.siffmember.info.ui.model

import androidx.annotation.Keep

@Keep
data class GetAppVersion(
    val versionName_android: String? = null,
    val versionName_ios: String? = null
)
