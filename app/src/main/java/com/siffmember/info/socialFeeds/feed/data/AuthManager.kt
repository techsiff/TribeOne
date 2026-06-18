package com.siffmember.info.socialFeeds.feed.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.siffmember.info.utils.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class FeedUser(
    val userId: String,
    val displayName: String,
    val profileImageUrl: String
)

object AuthManager {
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    private val _currentUserFlow = MutableStateFlow<FeedUser>(getFallbackUser())
    val currentUserFlow: StateFlow<FeedUser> = _currentUserFlow.asStateFlow()
    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
        updateFlow()
    }

//    init {
//        updateFlow()
//        firebaseAuth?.addAuthStateListener {
//            updateFlow()
//        }
//    }

    fun getCurrentUser(): FeedUser {
        val userId = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(12)
        val profilePlaceHolder = "https://firebasestorage.googleapis.com/v0/b/siffmembershipinfo.firebasestorage.app/o/placeholder%2Favatar.png?alt=media&token=195900c8-92a2-4749-86dd-a8d5d18a5492"
        return FeedUser(
                userId = prefs.getString(AppConstants.USER_ID, "") ?: userId,
                displayName = prefs.getString(AppConstants.USER_NAME, "GuestUser") ?: "GuestUser",
                profileImageUrl = profilePlaceHolder
            )

        /*val fbUser = firebaseAuth?.currentUser
        return if (fbUser != null) {
            FeedUser(
                userId = fbUser.uid,
                displayName = fbUser.displayName ?: "User_${fbUser.uid.take(4)}",
                profileImageUrl = fbUser.photoUrl?.toString() ?: "https://api.dicebear.com/7.x/adventurer/svg?seed=${fbUser.uid}"
            )
        } else {
            _currentUserFlow.value
        }*/
    }

    fun setSimulatedUser(name: String, avatarSeed: String) {
        val simulatedId = "simulated_user_123"
        _currentUserFlow.value = FeedUser(
            userId = simulatedId,
            displayName = name,
            profileImageUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=$avatarSeed"
        )
    }

    private fun updateFlow() {
        val profilePlaceHolder = "https://firebasestorage.googleapis.com/v0/b/siffmembershipinfo.firebasestorage.app/o/placeholder%2Favatar.png?alt=media&token=195900c8-92a2-4749-86dd-a8d5d18a5492"

        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null) {
//            _currentUserFlow.value = FeedUser(
//                userId = fbUser.uid,
//                displayName = fbUser.displayName ?: "User_${fbUser.uid.take(4)}",
//                profileImageUrl = fbUser.photoUrl?.toString() ?: "https://api.dicebear.com/7.x/adventurer/png?seed=${fbUser.uid}"
//            )
            _currentUserFlow.value = FeedUser(
                userId = prefs.getString(AppConstants.USER_ID, "")!!,
                displayName = prefs.getString(AppConstants.USER_NAME, "")!!,
                profileImageUrl = profilePlaceHolder
            )
        }
    }

    private fun getFallbackUser(): FeedUser {
        return FeedUser(
            userId = "demo_user_1",
            displayName = "Alex Carter",
            profileImageUrl = "https://api.dicebear.com/7.x/adventurer/png?seed=Alex"
        )
    }
}
