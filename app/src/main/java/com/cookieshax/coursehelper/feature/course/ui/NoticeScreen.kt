package com.cookieshax.coursehelper.feature.course.ui

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cookieshax.coursehelper.app.navigation.WebViewRoute
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.course.model.NoticeElement
import com.cookieshax.coursehelper.feature.course.ui.items.AttachmentItem
import com.cookieshax.coursehelper.feature.course.viewmodel.NoticeState
import com.cookieshax.coursehelper.feature.course.viewmodel.NoticeViewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeScreen(
    url: String,
    navController: NavController,
    viewModel: NoticeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(url) {
        viewModel.loadNotice(url)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知详情") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is NoticeState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is NoticeState.Success -> {
                    val notice = state.content
                    SelectionContainer {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            item {
                                Text(
                                    text = notice.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "${notice.sender}  ${notice.time}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            items(notice.elements) { element ->
                                when (element) {
                                    is NoticeElement.Text -> {
                                        Text(
                                            text = element.text,
                                            style = MaterialTheme.typography.bodyLarge,
                                            lineHeight = 24.sp,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }

                                    is NoticeElement.Image -> {
                                        AsyncImage(
                                            model = element.imageData ?: element.url,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentScale = ContentScale.FillWidth
                                        )
                                    }

                                    is NoticeElement.Attachment -> {
                                        AttachmentItem(
                                            name = element.name,
                                            size = element.size,
                                            onPreview = {
                                                viewModel.openAttachment(
                                                    element,
                                                    download = false
                                                ) { finalUrl ->
                                                    navController.navigate(
                                                        WebViewRoute(
                                                            finalUrl
                                                        )
                                                    )
                                                }
                                            },
                                            onDownload = {
                                                viewModel.openAttachment(
                                                    element,
                                                    download = true
                                                ) { finalUrl ->
                                                    try {
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            finalUrl.toUri()
                                                        )
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        "下载失败: ${e.message}".showToast()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                is NoticeState.Error -> {
                    Text(
                        text = "加载失败: ${state.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
