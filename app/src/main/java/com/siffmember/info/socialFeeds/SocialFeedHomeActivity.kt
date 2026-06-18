package com.siffmember.info.socialFeeds

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.siffmember.info.socialFeeds.feed.data.AuthManager
import com.siffmember.info.socialFeeds.ui.theme.MyApplicationTheme
import com.siffmember.info.socialFeeds.feed.ui.SocialFeedApp
import com.siffmember.info.ui.activity.BaseActivity

class SocialFeedHomeActivity : BaseActivity() {

    private var sharedText by mutableStateOf<String?>(null)
    private var sharedLink by mutableStateOf<String?>(null)
    private var sharedImages by mutableStateOf<List<Uri>?>(null)
    private var sharedFromApp by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        AuthManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
           // MyApplicationTheme {
               // Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SocialFeedApp(
                       // modifier = Modifier.padding(innerPadding),
                        sharedText = sharedText,
                        sharedLink = sharedLink,
                        sharedImages = sharedImages,
                        sharedFromApp = sharedFromApp,
                        onSharedHandled = {
                            sharedText = null
                            sharedLink = null
                            sharedImages = null
                            sharedFromApp = null
                        }
                    )
                //}
           // }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("is_shared_intent", false)) {
            sharedText = intent.getStringExtra("shared_text")
            sharedLink = intent.getStringExtra("shared_link")
            sharedImages = intent.getParcelableArrayListExtra<Uri>("shared_images")
            sharedFromApp = intent.getStringExtra("shared_from_app")
        }
    }
}
