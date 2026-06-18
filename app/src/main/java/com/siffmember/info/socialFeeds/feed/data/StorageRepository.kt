package com.siffmember.info.socialFeeds.feed.data

import android.net.Uri

interface StorageRepository {
    suspend fun uploadPostImage(imageUri: Uri): Result<String>
}
