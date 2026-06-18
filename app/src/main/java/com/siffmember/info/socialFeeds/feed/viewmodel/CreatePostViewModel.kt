package com.siffmember.info.socialFeeds.feed.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.siffmember.info.socialFeeds.feed.data.AuthManager
import com.siffmember.info.socialFeeds.feed.data.Inject
import com.siffmember.info.socialFeeds.feed.domain.usecase.CreatePostUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.siffmember.info.socialFeeds.feed.domain.LinkPreviewHelper
import kotlinx.coroutines.Job

data class CreatePostUiState(
    val contentText: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val inputLinkUrl: String = "",
    val maxCharacterCount: Int = 280,
    val uploadProgress: Float = 0.0f,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    val isFetchingMetadata: Boolean = false,
    val linkTitle: String? = null,
    val linkDescription: String? = null,
    val linkThumbnail: String? = null,
    val sourceType: String = "",
    val originalSharedUrl: String? = null,
    val sharedFromApp: String? = null
)

sealed interface CreatePostEvent {
    object PostCreatedSuccess : CreatePostEvent
    data class PostCreatedError(val error: String) : CreatePostEvent
}

class CreatePostViewModel(
    private val createPostUseCase: CreatePostUseCase = Inject.createPostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<CreatePostEvent>()
    val eventFlow: SharedFlow<CreatePostEvent> = _eventFlow.asSharedFlow()

    private var metadataJob: Job? = null

    fun onContentTextChange(text: String) {
        if (text.length <= _uiState.value.maxCharacterCount) {
            _uiState.update { it.copy(contentText = text) }
        }
    }

    fun onLinkUrlChange(url: String) {
        _uiState.update { it.copy(inputLinkUrl = url) }
        fetchLinkMetadata(url)
    }

    fun fetchLinkMetadata(url: String) {
        metadataJob?.cancel()
        if (url.isBlank()) {
            _uiState.update { it.copy(
                linkTitle = null,
                linkDescription = null,
                linkThumbnail = null,
                isFetchingMetadata = false
            ) }
            return
        }

        metadataJob = viewModelScope.launch {
            _uiState.update { it.copy(isFetchingMetadata = true) }
            try {
                var formatted = url.trim()
                if (!formatted.startsWith("http://") && !formatted.startsWith("https://")) {
                    formatted = "https://$formatted"
                }
                val metadata = LinkPreviewHelper.fetchMetadata(formatted)
                _uiState.update { it.copy(
                    linkTitle = metadata.title,
                    linkDescription = metadata.description,
                    linkThumbnail = metadata.thumbnailUrl,
                    isFetchingMetadata = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetchingMetadata = false) }
            }
        }
    }

    fun setSharedContent(text: String?, link: String?, images: List<Uri>?, fromApp: String?) {
        _uiState.update { state ->
            state.copy(
                contentText = text ?: "",
                inputLinkUrl = link ?: "",
                selectedImages = images ?: emptyList(),
                sourceType = "SHARED",
                originalSharedUrl = link ?: (if (text != null) LinkPreviewHelper.extractUrl(text) else null),
                sharedFromApp = fromApp,
                isFetchingMetadata = false,
                linkTitle = null,
                linkDescription = null,
                linkThumbnail = null
            )
        }
        if (!link.isNullOrEmpty()) {
            fetchLinkMetadata(link)
        } else if (text != null) {
            val extracted = LinkPreviewHelper.extractUrl(text)
            if (extracted != null) {
                fetchLinkMetadata(extracted)
            }
        }
    }

    fun addImage(uri: Uri) {
        _uiState.update { state ->
            val current = state.selectedImages.toMutableList()
            if (!current.contains(uri)) {
                current.add(uri)
            }
            state.copy(selectedImages = current)
        }
    }

    fun removeImage(uri: Uri) {
        _uiState.update { state ->
            state.copy(selectedImages = state.selectedImages.filter { it != uri })
        }
    }

    fun submitPost() {
        val state = _uiState.value
        val hasText = state.contentText.trim().isNotBlank()
        val hasImages = state.selectedImages.isNotEmpty()
        val hasLink = state.inputLinkUrl.trim().isNotBlank()

        if (!hasText && !hasImages && !hasLink) {
            _uiState.update { it.copy(errorMessage = "Please enter some text, add an image, or share a link.") }
            return
        }

        _uiState.update {
            it.copy(
                isUploading = true,
                uploadProgress = 0.05f,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val user = AuthManager.getCurrentUser()
            
            // Map metadata to a String list or map if present
            val metadataMap = if (state.linkTitle != null) {
                mapOf(
                    "title" to (state.linkTitle ?: ""),
                    "description" to (state.linkDescription ?: ""),
                    "thumbnailUrl" to (state.linkThumbnail ?: "")
                )
            } else null

            val result = createPostUseCase.execute(
                userId = user.userId,
                userName = user.displayName,
                userProfileImage = user.profileImageUrl,
                content = state.contentText,
                imageUris = state.selectedImages,
                inputLinkUrl = state.inputLinkUrl.ifBlank { null },
                sourceType = state.sourceType,
                originalSharedUrl = state.originalSharedUrl,
                linkMetadata = metadataMap,
                sharedFromApp = state.sharedFromApp,
                onUploadProgress = { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                }
            )

            if (result.isSuccess) {
                _uiState.update { CreatePostUiState() } // Reset form
                _eventFlow.emit(CreatePostEvent.PostCreatedSuccess)
            } else {
                val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Unknown creation failure"
                _uiState.update { it.copy(isUploading = false, errorMessage = errorMsg) }
                _eventFlow.emit(CreatePostEvent.PostCreatedError(errorMsg))
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
