package com.siffmember.info.socialFeeds.feed.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siffmember.info.socialFeeds.feed.ui.components.PostCard
import com.siffmember.info.socialFeeds.feed.viewmodel.FeedUiState
import com.siffmember.info.socialFeeds.feed.viewmodel.FeedViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.colorResource
import com.siffmember.info.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onNavigateToCreatePost: () -> Unit,
    onNavigateToPostDetails: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onSharePostLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    onImageClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Detect when scrolling near the bottom to trigger Firestore infinite scroll pagination
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= totalItems - 3 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "TribeStream",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorResource(id = R.color.white),
                        textAlign = TextAlign.Center,
                    )
                },
//                actions = {
//                    AsyncImage(
//                        model = viewModel.currentFeedUser.profileImageUrl,
//                        contentDescription = "My profile image",
//                        modifier = Modifier
//                            .padding(end = 16.dp)
//                            .size(36.dp)
//                            .clip(CircleShape)
//                            .background(MaterialTheme.colorScheme.secondaryContainer)
//                            .clickable { onNavigateToUserProfile(viewModel.currentFeedUser.userId) }
//                    )
//                },
                colors = TopAppBarDefaults.topAppBarColors(
                    //containerColor = Purple800
                    containerColor = colorResource(id = R.color.colorPrimary)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreatePost,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("create_post_fab"),
                containerColor = colorResource(id = R.color.colorAccent),
                contentColor = colorResource(id = R.color.white)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create post button"
                )
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is FeedUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is FeedUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Failed to load feed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is FeedUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refreshFeed() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (state.posts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 48.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Your public feed is waiting!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Become the first person to share a text, image, or link post with the community.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = state.posts,
                                key = { _, post -> post.postId }
                            ) { index, post ->
                                PostCard(
                                    post = post,
                                    currentUserId = viewModel.currentFeedUser.userId,
                                    onPostClick = { onNavigateToPostDetails(post.postId) },
                                    onProfileClick = { onNavigateToUserProfile(post.userId) },
                                    onLikeClick = { viewModel.performLike(post.postId) },
                                    onCommentClick = { onNavigateToPostDetails(post.postId) },
                                    onReshareClick = { viewModel.performReshare(post.postId) },
                                    onShareClick = { onSharePostLink(post.commentUrlForShare()) },
                                    onDeleteClick = { viewModel.performDelete(post.postId) },
                                    onEditClick = { content -> viewModel.performEdit(post.postId, content) },
                                    onImageClick = {imageUrl -> onImageClick(imageUrl)}
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
