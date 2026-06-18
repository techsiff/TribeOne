package com.siffmember.info.socialFeeds.feed.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.siffmember.info.socialFeeds.feed.viewmodel.CreatePostViewModel
import com.siffmember.info.socialFeeds.feed.viewmodel.FeedViewModel
import com.siffmember.info.socialFeeds.feed.viewmodel.PostDetailViewModel

@Composable
fun SocialFeedApp(
    modifier: Modifier = Modifier,
    sharedText: String? = null,
    sharedLink: String? = null,
    sharedImages: List<android.net.Uri>? = null,
    sharedFromApp: String? = null,
    onSharedHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Initialize ViewModels using Compose viewModel() which handles lifecycles out-of-the-box
    val feedViewModel: FeedViewModel = viewModel()
    val createPostViewModel: CreatePostViewModel = viewModel()
    val postDetailViewModel: PostDetailViewModel = viewModel()

    // Trigger navigation and load content when a share is received
    androidx.compose.runtime.LaunchedEffect(sharedText, sharedLink, sharedImages, sharedFromApp) {
        if (sharedText != null || sharedLink != null || !sharedImages.isNullOrEmpty() || sharedFromApp != null) {
            createPostViewModel.setSharedContent(
                text = sharedText,
                link = sharedLink,
                images = sharedImages,
                fromApp = sharedFromApp
            )
            onSharedHandled()
            navController.navigate("create_post") {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "feed",
        modifier = modifier
    ) {
        // 1. Feed Screen Route
        composable("feed") {
            FeedScreen(
                viewModel = feedViewModel,
                onNavigateToCreatePost = { navController.navigate("create_post") },
                onNavigateToPostDetails = { postId -> navController.navigate("post_details/$postId") },
                onNavigateToUserProfile = { userId -> navController.navigate("user_profile/$userId") },
                onSharePostLink = { shareText -> shareTextNative(context, shareText) },
                onImageClick = { imageUrl -> navController.navigate("image_viewer/${Uri.encode(imageUrl)}") }

            )
        }

        // 2. Create Post Route
        composable("create_post") {
            CreatePostScreen(
                viewModel = createPostViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 3. Post Details Route
        composable(
            route = "post_details/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.StringType })
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            PostDetailScreen(
                viewModel = postDetailViewModel,
                postId = postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToUserProfile = { userId -> navController.navigate("user_profile/$userId") },
                onNavigateToCommentComposer = { pid, parentId ->
                    val dest =
                        if (parentId != null) "comments/$pid/$parentId" else "comments/$pid/null"
                    navController.navigate(dest)
                },
                onSharePostLink = { shareText -> shareTextNative(context, shareText) },
                onImageClick = { imageUrl -> navController.navigate("image_viewer/${Uri.encode(imageUrl)}") }
            )
        }

        // 4. Comments Route
        composable(
            route = "comments/{postId}/{parentCommentId}",
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("parentCommentId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val parentCommentIdRaw = backStackEntry.arguments?.getString("parentCommentId")
            val parentCommentId = if (parentCommentIdRaw == "null" || parentCommentIdRaw.isNullOrEmpty()) null else parentCommentIdRaw

            CommentsScreen(
                viewModel = postDetailViewModel,
                postId = postId,
                parentCommentId = parentCommentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. User Profile Route
        composable(
            route = "user_profile/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserPostsScreen(
                viewModel = feedViewModel,
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPostDetails = { postId -> navController.navigate("post_details/$postId") },
                onSharePostLink = { shareText -> shareTextNative(context, shareText) },
                onImageClick = { imageUrl -> navController.navigate("image_viewer/${Uri.encode(imageUrl)}") }

            )
        }
        composable(
            route = "image_viewer/{imageUrl}",
            arguments = listOf(navArgument("imageUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
            ImageViewerScreen(
                imageUrl = imageUrl,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

// Native share sheet helper to broadcast a link text seamlessly in Android
private fun shareTextNative(context: Context, text: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Post")
        context.startActivity(shareIntent)
    } catch (e: Exception) {
        // Safety catch
    }
}
