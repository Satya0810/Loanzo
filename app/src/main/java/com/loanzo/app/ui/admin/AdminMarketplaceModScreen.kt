package com.loanzo.app.ui.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.loanzo.app.ui.components.LoanzoText as Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.ui.theme.*
import com.loanzo.app.util.LocalAdminRepository
import com.loanzo.app.util.LocalMarketplaceRepository
import com.loanzo.app.util.toInrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMarketplaceModScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val marketplaceRepository = LocalMarketplaceRepository.current
    val adminRepository = LocalAdminRepository.current

    val allPosts by marketplaceRepository.getAllPosts().collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("UNVERIFIED") }
    var searchQuery by remember { mutableStateOf("") }

    var postToDelete by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var postToNotify by remember { mutableStateOf<MarketplacePostEntity?>(null) }
    var notifyTitle by remember { mutableStateOf("") }
    var notifyMessage by remember { mutableStateOf("") }

    val unverifiedPosts = remember(allPosts) { allPosts.filter { !it.isAdminVerified } }
    val verifiedCount = remember(allPosts) { allPosts.count { it.isAdminVerified } }

    val filteredPosts = remember(allPosts, searchQuery, selectedFilter) {
        allPosts.filter { post ->
            val matchesQuery = searchQuery.isBlank() ||
                post.title.contains(searchQuery, ignoreCase = true) ||
                post.description.contains(searchQuery, ignoreCase = true) ||
                post.authorName.contains(searchQuery, ignoreCase = true) ||
                post.locationCity.contains(searchQuery, ignoreCase = true) ||
                post.postId.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "UNVERIFIED" -> !post.isAdminVerified
                "VERIFIED" -> post.isAdminVerified
                "SEEKING_LOAN" -> post.postType == "SEEKING_LOAN"
                "OFFER_TO_LEND" -> post.postType == "OFFER_TO_LEND"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Community & Marketplace Moderation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextNavyDark
                        )
                        Text(
                            text = "${unverifiedPosts.size} Pending Verification • ${allPosts.size} Total Posts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextNavyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Metrics KPI Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ModKpi(label = "Pending Verification", value = unverifiedPosts.size.toString(), color = if (unverifiedPosts.isNotEmpty()) Red400 else Emerald500)
                    ModKpi(label = "Verified Posts", value = verifiedCount.toString(), color = Emerald500)
                    ModKpi(label = "Total Feed", value = allPosts.size.toString(), color = Color(0xFF0F172A))
                    ModKpi(label = "Seeking Loans", value = allPosts.count { it.postType == "SEEKING_LOAN" }.toString(), color = BrandCobalt)
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search posts by title, author, city...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "UNVERIFIED" to "Pending Verification (${unverifiedPosts.size})",
                    "ALL" to "All Posts (${allPosts.size})",
                    "VERIFIED" to "Verified (${verifiedCount})",
                    "SEEKING_LOAN" to "Seeking Loan (${allPosts.count { it.postType == "SEEKING_LOAN" }})",
                    "OFFER_TO_LEND" to "Lending Offers (${allPosts.count { it.postType == "OFFER_TO_LEND" }})"
                )

                filters.forEach { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        onClick = { selectedFilter = key },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandNavyBlue else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) BrandNavyBlue else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Posts List
            if (filteredPosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedFilter == "UNVERIFIED") "All marketplace posts are verified" else "No posts found matching filter",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPosts, key = { it.postId }) { post ->
                        MarketplaceModCard(
                            post = post,
                            onVerify = { verifyStatus ->
                                scope.launch(Dispatchers.IO) {
                                    marketplaceRepository.verifyPost(post.postId, verifyStatus)
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (verifyStatus) "Post marked as Admin Verified" else "Verification revoked",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onDelete = { postToDelete = post },
                            onNotifyAuthor = {
                                postToNotify = post
                                notifyTitle = ""
                                notifyMessage = ""
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (postToDelete != null) {
        val target = postToDelete!!
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = {
                Text(
                    text = "Confirm Post Deletion",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextNavyDark
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove \"${target.title}\" posted by ${target.authorName}? This action cannot be undone.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            marketplaceRepository.deletePostByAdmin(target.postId)
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, "Post removed from marketplace", Toast.LENGTH_SHORT).show()
                                postToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red400)
                ) {
                    Text("Delete Post", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Notify Author Dialog
    if (postToNotify != null) {
        val target = postToNotify!!
        AlertDialog(
            onDismissRequest = { postToNotify = null },
            title = {
                Text(
                    text = "Send Moderation Notice to ${target.authorName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextNavyDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = notifyTitle,
                        onValueChange = { notifyTitle = it },
                        label = { Text("Notice Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = notifyMessage,
                        onValueChange = { notifyMessage = it },
                        label = { Text("Notice Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notifyTitle.isNotBlank() && notifyMessage.isNotBlank()) {
                            scope.launch(Dispatchers.IO) {
                                adminRepository.sendUserNotification(
                                    userId = target.authorId,
                                    title = notifyTitle,
                                    message = notifyMessage,
                                    actionRoute = "community"
                                )
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "Notice transmitted to author", Toast.LENGTH_SHORT).show()
                                    postToNotify = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavyBlue)
                ) {
                    Text("Send Notice", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToNotify = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModKpi(label: String, value: String, color: Color) {
    Column {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

@Composable
private fun MarketplaceModCard(
    post: MarketplacePostEntity,
    onVerify: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onNotifyAuthor: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (!post.isAdminVerified) Gold500.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Author & Verification Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandIceBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (post.authorName.firstOrNull() ?: 'P').uppercase(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandCobalt
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = post.authorName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${post.locationCity.ifBlank { "National" }} • Trust Score: ${post.authorTrustScore}/100",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (post.isAdminVerified) Emerald400.copy(alpha = 0.12f) else Gold500.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (post.isAdminVerified) "VERIFIED" else "UNVERIFIED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isAdminVerified) Emerald500 else Gold500,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post Title & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = post.purposeCategory,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (post.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.description,
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Terms Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Amount Range", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("${post.minAmount.toInrString()} - ${post.maxAmount.toInrString()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Column {
                    Text("Interest Rate", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("${post.interestRate}% p.a.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandRoyalBlue)
                }
                Column {
                    Text("Tenure", fontSize = 10.sp, color = Color(0xFF64748B))
                    Text("${post.tenureMonths} Months", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!post.isAdminVerified) {
                    Button(
                        onClick = { onVerify(true) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify Post", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onVerify(false) },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Gold500),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Text("Revoke", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Gold500)
                    }
                }

                OutlinedButton(
                    onClick = onNotifyAuthor,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandCobalt.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Notice", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BrandCobalt)
                }

                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Red400),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Red400)
                }
            }
        }
    }
}
