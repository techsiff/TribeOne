package com.siffmember.info.socialFeeds

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.siffmember.info.ui.activity.BaseActivity
import java.util.regex.Pattern

import java.io.File
import java.io.FileOutputStream

class ShareReceiverActivity : BaseActivity() {

    private fun copyUriToCache(uri: Uri): Uri {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempFile = File(cacheDir, "shared_img_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                return Uri.fromFile(tempFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent.action
        val type = intent.type

        var sharedText: String? = null
        var sharedLink: String? = null
        val sharedImages = mutableListOf<Uri>()

        // 1. Resolve referrer app package
        val referrerUri = referrer
        val referrerPackage = callingActivity?.packageName
            ?: referrerUri?.authority
            ?: referrerUri?.host
            ?: intent.getStringExtra("android.intent.extra.REFERRER_NAME")

        val sharedFromApp = referrerPackage?.let { getAppNameFromPackage(it) } ?: "External Application"

        if (Intent.ACTION_SEND == action && type != null) {
            if ("text/plain" == type || type.startsWith("text/")) {
                val incomingText = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (incomingText != null) {
                    val extractedUrl = extractUrl(incomingText)
                    if (extractedUrl != null) {
                        sharedLink = extractedUrl
                        // Remove URL from the text to avoid duplicate inputs in the text area
                        val cleanText = incomingText.replace(extractedUrl, "").trim()
                        sharedText = cleanText.ifEmpty { null }
                    } else {
                        sharedText = incomingText
                    }
                }
            } else if (type.startsWith("image/")) {
                // If text is shared alongside the image
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { incomingText ->
                    val extractedUrl = extractUrl(incomingText)
                    if (extractedUrl != null) {
                        sharedLink = extractedUrl
                        val cleanText = incomingText.replace(extractedUrl, "").trim()
                        sharedText = cleanText.ifEmpty { null }
                    } else {
                        sharedText = incomingText
                    }
                }

                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) as? Uri)?.let { uri ->
                    sharedImages.add(copyUriToCache(uri))
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null) {
            if (type.startsWith("image/")) {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText = it }
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris ->
                    val safeUris = uris.filterNotNull().map { copyUriToCache(it) }
                    sharedImages.addAll(safeUris)
                }
            }
        }

        // 2. Launch MainActivity with organized data
        val mainIntent = Intent(this, SocialFeedHomeActivity::class.java).apply {
            putExtra("shared_text", sharedText)
            putExtra("shared_link", sharedLink)
            putParcelableArrayListExtra("shared_images", ArrayList(sharedImages))
            putExtra("shared_from_app", sharedFromApp)
            putExtra("is_shared_intent", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(mainIntent)
        finish()
    }

    private fun extractUrl(text: String): String? {
        val urlPattern = Pattern.compile(
            "(?:^|[\\s倾])(https?://(?:www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b(?:[-a-zA-Z0-9()@:%_+.~#?&/=]*))",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = urlPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    private fun getAppNameFromPackage(packageName: String): String {
        val cleanLabel = packageName.lowercase()
        return when {
            cleanLabel.contains("youtube") -> "YouTube"
            cleanLabel.contains("chrome") -> "Google Chrome"
            cleanLabel.contains("twitter") || cleanLabel.contains("x.android") -> "X"
            cleanLabel.contains("instagram") -> "Instagram"
            cleanLabel.contains("facebook") -> "Facebook"
            cleanLabel.contains("linkedin") -> "LinkedIn"
            cleanLabel.contains("whatsapp") -> "WhatsApp"
            cleanLabel.contains("telegram") -> "Telegram"
            cleanLabel.contains("photos") -> "Google Photos"
            cleanLabel.contains("gallery") -> "Gallery app"
            cleanLabel.contains("news") -> "News app"
            else -> {
                try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    "Shared Origin ($packageName)"
                }
            }
        }
    }
}
