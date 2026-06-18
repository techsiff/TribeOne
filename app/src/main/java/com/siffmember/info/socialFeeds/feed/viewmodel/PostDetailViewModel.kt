package com.siffmember.info.socialFeeds.feed.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siffmember.info.socialFeeds.feed.data.AuthManager
import com.siffmember.info.socialFeeds.feed.data.FeedUser
import com.siffmember.info.socialFeeds.feed.data.Inject
import com.siffmember.info.socialFeeds.feed.domain.usecase.GetPublicFeedUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.CommentUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.LikePostUseCase
import com.siffmember.info.socialFeeds.feed.domain.usecase.ResharePostUseCase
import com.siffmember.info.socialFeeds.feed.model.Comment
import com.siffmember.info.socialFeeds.feed.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

sealed interface PostDetailUiState {
    object Loading : PostDetailUiState
    data class Success(
        val post: Post,
        val comments: List<Comment>,
        val currentUserId: String,
        val isAddingComment: Boolean = false,
        val error: String? = null
    ) : PostDetailUiState
    object NotFound : PostDetailUiState
}

class PostDetailViewModel(
    private val getPublicFeedUseCase: GetPublicFeedUseCase = Inject.getPublicFeedUseCase,
    private val commentUseCase: CommentUseCase = Inject.commentUseCase,
    private val likePostUseCase: LikePostUseCase = Inject.likePostUseCase,
    private val resharePostUseCase: ResharePostUseCase = Inject.resharePostUseCase
) : ViewModel() {

    private val _postId = MutableStateFlow<String?>(null)
    private val _isAddingComment = MutableStateFlow(false)
    private val _commentFormError = MutableStateFlow<String?>(null)

    val currentFeedUser: FeedUser
        get() = AuthManager.getCurrentUser()

    // Dynamically fetch post and comments whenever the postId changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PostDetailUiState> = _postId.flatMapLatest { id ->
        if (id == null) {
            flowOf(PostDetailUiState.Loading)
        } else {
            val postFlow = getPublicFeedUseCase.getPostDetails(id)
            val commentsFlow = commentUseCase.getComments(id)
            val authFlow = AuthManager.currentUserFlow

            combine(postFlow, commentsFlow, authFlow, _isAddingComment, _commentFormError) { post, comments, currentUser, addition, error ->
                if (post == null) {
                    PostDetailUiState.NotFound
                } else {
                    // Update like status optimistically or mapped
                    val updatedPost = post.copy(
                        isLikedByCurrentUser = post.userId == currentUser.userId || post.isLikedByCurrentUser,
                        isResharedByCurrentUser = post.originalPostId != null && post.userId == currentUser.userId
                    )
                    PostDetailUiState.Success(
                        post = updatedPost,
                        comments = comments,
                        currentUserId = currentUser.userId,
                        isAddingComment = addition,
                        error = error
                    )
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PostDetailUiState.Loading
    )

    fun setPostId(postId: String) {
        _postId.value = postId
    }

    fun submitComment(content: String, parentCommentId: String? = null) {
        val id = _postId.value ?: return
        if (content.trim().isEmpty()) {
            _commentFormError.value = "Comment content cannot be empty"
            return
        }

        _isAddingComment.value = true
        _commentFormError.value = null

        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            val result = commentUseCase.addComment(
                postId = id,
                userId = user.userId,
                userName = user.displayName,
                userProfileImage = user.profileImageUrl,
                content = content,
                parentCommentId = parentCommentId
            )
            if (result.isFailure) {
                _commentFormError.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to post comment"
            }
            _isAddingComment.value = false
        }
    }

    fun performLike() {
        val id = _postId.value ?: return
        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            likePostUseCase.execute(id, user.userId, user.displayName, user.profileImageUrl)
        }
    }

    fun performReshare() {
        val id = _postId.value ?: return
        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            resharePostUseCase.execute(id, user.userId, user.displayName, user.profileImageUrl)
        }
    }

    fun editComment(commentId: String, newContent: String) {
        val id = _postId.value ?: return
        viewModelScope.launch {
            commentUseCase.editComment(id, commentId, newContent)
        }
    }

    fun deleteComment(commentId: String) {
        val id = _postId.value ?: return
        viewModelScope.launch {
            commentUseCase.deleteComment(id, commentId)
        }
    }
}
