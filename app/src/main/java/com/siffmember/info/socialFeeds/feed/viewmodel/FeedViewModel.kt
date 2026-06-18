package com.siffmember.info.socialFeeds.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siffmember.info.socialFeeds.feed.data.AuthManager
import com.siffmember.info.socialFeeds.feed.data.FeedUser
import com.siffmember.info.socialFeeds.feed.data.Inject
import com.siffmember.info.socialFeeds.feed.domain.usecase.GetPublicFeedUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.LikePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.ResharePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.ManagePostUseCase
import com.siffmember.info.socialFeeds.feed.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.delay

sealed interface FeedUiState {
    object Loading : FeedUiState
    data class Success(
        val posts: List<Post>,
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

class FeedViewModel(
    private val getPublicFeedUseCase: GetPublicFeedUseCase = Inject.getPublicFeedUseCase,
    private val likePostUseCase: LikePostUseCase = Inject.likePostUseCase,
    private val resharePostUseCase: ResharePostUseCase = Inject.resharePostUseCase,
    private val managePostUseCase: ManagePostUseCase = Inject.managePostUseCase
) : ViewModel() {

    private val currentUserFlow = AuthManager.currentUserFlow
    private val _isRefreshing = MutableStateFlow(false)
    private val _isLoadingMore = MutableStateFlow(false)
    
    // We filter by target userId if set. If null, we show the public feed.
    private val _filterUserId = MutableStateFlow<String?>(null)

    val currentFeedUser: FeedUser
        get() = AuthManager.getCurrentUser()

    // Real-time combine of public feed, user filter and like-state adjustments
    private val rawFeedFlow = getPublicFeedUseCase.execute(30)

    val uiState: StateFlow<FeedUiState> = combine(
        rawFeedFlow,
        _filterUserId,
        currentUserFlow,
        _isRefreshing,
        _isLoadingMore
    ) { rawPosts, filterId, currentUser, refreshing, loadingMore ->
        try {
            val filtered = if (filterId != null) {
                rawPosts.filter { it.userId == filterId }
            } else {
                rawPosts
            }
            // Map the likes and reshares based on the active user identity (optimistic check fallback)
            val updated = filtered.map { post ->
                post.copy(
                    isLikedByCurrentUser = post.userId == currentUser.userId || post.isLikedByCurrentUser, // Fallback/demo simulated
                    isResharedByCurrentUser = post.originalPostId != null && post.userId == currentUser.userId
                )
            }
            FeedUiState.Success(
                posts = updated,
                isRefreshing = refreshing,
                isLoadingMore = loadingMore
            )
        } catch (e: Exception) {
            FeedUiState.Error(e.localizedMessage ?: "Unknown error occurred")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeedUiState.Loading
    )

    fun filterByUser(userId: String?) {
        _filterUserId.value = userId
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Simulates pull to refresh reload
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun loadMore() {
        val state = uiState.value
        if (state is FeedUiState.Success && !state.isLoadingMore && state.posts.isNotEmpty()) {
            val lastPostId = state.posts.last().postId
            viewModelScope.launch {
                _isLoadingMore.value = true
                try {
                    val additional = getPublicFeedUseCase.loadMore(lastPostId, 15)
                    if (additional.isNotEmpty()) {
                        Log.d("FeedViewModel", "Loaded ${additional.size} more posts")
                        // Firestore triggers callback flow, so rawFeedFlow auto-updates if real Firestore connects,
                        // for fallback we simulate loading complete.
                    }
                } catch (e: Exception) {
                    Log.e("FeedViewModel", "Load more failed", e)
                } finally {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    fun performLike(postId: String) {
        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            val result = likePostUseCase.execute(postId, user.userId, user.displayName, user.profileImageUrl)
            if (result.isFailure) {
                Log.e("FeedViewModel", "Failed to like post: $postId", result.exceptionOrNull())
            }
        }
    }

    fun performReshare(postId: String) {
        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            val result = resharePostUseCase.execute(postId, user.userId, user.displayName, user.profileImageUrl)
            if (result.isFailure) {
                Log.e("FeedViewModel", "Failed to reshare post: $postId", result.exceptionOrNull())
            }
        }
    }

    fun performDelete(postId: String) {
        viewModelScope.launch {
            managePostUseCase.delete(postId)
        }
    }

    fun performEdit(postId: String, newContent: String) {
        viewModelScope.launch {
            // Find existing post to preserve content fields
            val state = uiState.value
            if (state is FeedUiState.Success) {
                val post = state.posts.find { it.postId == postId } ?: return@launch
                managePostUseCase.edit(
                    postId = postId,
                    newContent = newContent,
                    newPostType = post.postType.name,
                    imageUrls = post.imageUrls,
                    linkUrl = post.linkUrl,
                    linkTitle = post.linkTitle,
                    linkDescription = post.linkDescription,
                    linkThumbnail = post.linkThumbnail
                )
            }
        }
    }
}
