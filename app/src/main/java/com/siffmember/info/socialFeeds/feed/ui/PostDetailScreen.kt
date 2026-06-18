package com.siffmember.info.socialFeeds.feed.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.siffmember.info.R
import com.siffmember.info.socialFeeds.feed.model.Comment
import com.siffmember.info.socialFeeds.feed.ui.components.PostCard
import com.siffmember.info.socialFeeds.feed.viewmodel.PostDetailUiState
import com.siffmember.info.socialFeeds.feed.viewmodel.PostDetailViewModel
import com.siffmember.info.socialFeeds.ui.theme.ColorPrimary
import com.siffmember.info.socialFeeds.ui.theme.TribeBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToCommentComposer: (String, String?) -> Unit, // Navigate to specific dedicated Comment view
    onSharePostLink: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    var commentInputText by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<Comment?>(null) }

    val listState = rememberLazyListState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        viewModel.setPostId(postId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Post Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorResource(id = R.color.white)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back button",
                            tint = colorResource(id = R.color.white)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    //containerColor = MaterialTheme.colorScheme.surface
                    containerColor = ColorPrimary
                )
            )
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            PostDetailUiState.NotFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("This thread is no longer available.")
                }
            }

            is PostDetailUiState.Success -> {
                /*
                var previousCommentCount by remember { mutableIntStateOf(0) }
                LaunchedEffect(state.comments.size) {
                    if (state.comments.size > previousCommentCount) {
                        listState.animateScrollToItem(
                            index = listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1
                        )
                    }
                    previousCommentCount = state.comments.size
                }*/
                /*val topLevelComments = remember(state.comments) {
                    state.comments.filter { it.parentCommentId == null }
                }*/
                val listState = rememberLazyListState()
                val bringIntoViewRequester = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                var previousCommentCount by remember { mutableIntStateOf(0) }
                var shouldScrollToNewComment by remember { mutableStateOf(false) }

                val topLevelComments = state.comments
                    .filter { it.parentCommentId == null }
                    .sortedByDescending { it.createdAt }

                val repliesMap = remember(state.comments) {
                    state.comments.filter { it.parentCommentId != null }
                        .groupBy { it.parentCommentId!! }
                }
                LaunchedEffect(state.comments.size) {
                    if (
                        shouldScrollToNewComment &&
                        state.comments.size > previousCommentCount
                    ) {
                        //listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                        //listState.animateScrollToItem(0)

                        shouldScrollToNewComment = false
                    }

                    previousCommentCount = state.comments.size
                }
//                LaunchedEffect(state.comments.size) {
//                    if (state.comments.isNotEmpty()) {
//                        listState.animateScrollToItem(0)
//                    }
//                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Feed list including main post details and comments
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        item {
                            PostCard(
                                post = state.post,
                                currentUserId = state.currentUserId,
                                onPostClick = {},
                                onProfileClick = { onNavigateToUserProfile(state.post.userId) },
                                onLikeClick = { viewModel.performLike() },
                                onCommentClick = {
                                    // Navigate to dedicated Comment Screen
                                    onNavigateToCommentComposer(state.post.postId, null)
                                },
                                onReshareClick = { viewModel.performReshare() },
                                onShareClick = { onSharePostLink(state.post.commentUrlForShare()) },
                                onDeleteClick = {
                                    viewModel.deleteComment(state.post.postId) // deletes thread
                                    onNavigateBack()
                                },
                                onEditClick = { /* handeled in view */ },
                                onImageClick = {imageUrl -> onImageClick(imageUrl)}
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Text(
                                text = "Comments (${state.comments.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = TribeBlue
                            )
                        }

                        if (topLevelComments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("💬", fontSize = 32.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No comments yet",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Start the conversation by adding yours.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(topLevelComments, key = { it.commentId }) { comment ->
                                CommentBubble(
                                    comment = comment,
                                    currentUserId = state.currentUserId,
                                    hasReplies = repliesMap.containsKey(comment.commentId),
                                    onReplyClick = {
                                        replyingToComment = comment
                                        scope.launch {
                                            bringIntoViewRequester.bringIntoView()
                                        }
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteComment(comment.commentId)
                                    },
                                    onEditClick = { updated_text ->
                                        viewModel.editComment(comment.commentId, updated_text)
                                    },
                                    onNavigateToUserProfile = onNavigateToUserProfile
                                )

                                // Render indented replies underneath
                                val replies = repliesMap[comment.commentId] ?: emptyList()
                                replies.forEach { reply ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp) // Indentation for thread look
                                    ) {
                                        // Vertical thread connector line
                                        Box(
                                            modifier = Modifier
                                                .width(1.5.dp)
                                                .height(50.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant)
                                        )

                                        Box(modifier = Modifier.weight(1f)) {
                                            CommentBubble(
                                                comment = reply,
                                                currentUserId = state.currentUserId,
                                                hasReplies = false,
                                                onReplyClick = {
                                                    // Reply to top-level comment parent
                                                    replyingToComment = comment
                                                    scope.launch {
                                                        bringIntoViewRequester.bringIntoView()
                                                    }
                                                },
                                                onDeleteClick = {
                                                    viewModel.deleteComment(reply.commentId)
                                                },
                                                onEditClick = { updated_text ->
                                                    viewModel.editComment(reply.commentId, updated_text)
                                                },
                                                onNavigateToUserProfile = onNavigateToUserProfile
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Reply indicator box
                    AnimatedVisibility(
                        visible = replyingToComment != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Replying to @${replyingToComment?.userName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            IconButton(
                                onClick = { replyingToComment = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel reply button",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Comment compose text box
                   // Divider()
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 0.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = viewModel.currentFeedUser.profileImageUrl,
                            contentDescription = "User profile picture",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = commentInputText,
                            onValueChange = { commentInputText = it },
                            placeholder = { Text("Write a comment or reply...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("comment_text_input"),
                            shape = RoundedCornerShape(20.dp),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        viewModel.submitComment(
                                            content = commentInputText,
                                            parentCommentId = replyingToComment?.commentId
                                        )
                                        shouldScrollToNewComment = true
                                        commentInputText = ""
                                        replyingToComment = null
                                    },
                                    enabled = commentInputText.trim().isNotEmpty()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send comment button",
                                        tint = if (commentInputText.trim().isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentBubble(
    comment: Comment,
    currentUserId: String,
    hasReplies: Boolean,
    onReplyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(comment.content) }
    val profilePlaceHolder = "https://firebasestorage.googleapis.com/v0/b/siffmembershipinfo.firebasestorage.app/o/placeholder%2Favatar.png?alt=media&token=195900c8-92a2-4749-86dd-a8d5d18a5492"

    val formattedTime = remember(comment.createdAt) {
        val sdf = SimpleDateFormat("MMM d • h:mm a", Locale.getDefault())
        sdf.format(Date(comment.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(comment.userProfileImage.ifEmpty { profilePlaceHolder })
                    .crossfade(true)
                    .build(),
                contentDescription = "Commenter avatar",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNavigateToUserProfile(comment.userId) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = comment.userName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { onNavigateToUserProfile(comment.userId) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•  $formattedTime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    if (comment.userId == currentUserId) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Comment options",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Comment") },
                                    onClick = {
                                        showMenu = false
                                        isEditing = true
                                    },
                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Comment", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isEditing) {
                    Column {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    isEditing = false
                                    onEditClick(editText)
                                }
                            ) {
                                Text("Save")
                            }
                        }
                    }
                } else {
                    Text(
                        text = comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Reply action button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onReplyClick() }
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Reply icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reply",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
