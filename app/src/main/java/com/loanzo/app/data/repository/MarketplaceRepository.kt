package com.loanzo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.loanzo.app.data.dao.MarketplaceDao
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import com.loanzo.app.data.entity.MarketplaceVouchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepository @Inject constructor(
    private val marketplaceDao: MarketplaceDao,
    private val firebaseManager: com.loanzo.app.data.firebase.FirebaseManager
) {
    companion object {
        private const val TAG = "MarketplaceRepo"
        private const val COLLECTION_POSTS = "marketplace_posts"
        private const val COLLECTION_BIDS = "marketplace_bids"
    }

    private val firestore: FirebaseFirestore
        get() = com.loanzo.app.data.firebase.FirestoreProvider.get()

    fun getAllPosts(): Flow<List<MarketplacePostEntity>> =
        marketplaceDao.getAllPostsFlow()

    fun getPostsByType(postType: String): Flow<List<MarketplacePostEntity>> =
        marketplaceDao.getPostsByTypeFlow(postType)

    fun getPostsByAuthor(authorId: String): Flow<List<MarketplacePostEntity>> =
        marketplaceDao.getPostsByAuthorFlow(authorId)

    fun getPostByIdFlow(postId: String): Flow<MarketplacePostEntity?> =
        marketplaceDao.getPostByIdFlow(postId)

    suspend fun getPostById(postId: String): MarketplacePostEntity? =
        marketplaceDao.getPostById(postId)

    fun getBidsForPost(postId: String): Flow<List<MarketplaceBidEntity>> =
        marketplaceDao.getBidsForPostFlow(postId)

    fun getBidsByBidder(bidderId: String): Flow<List<MarketplaceBidEntity>> =
        marketplaceDao.getBidsByBidderFlow(bidderId)

    fun postToFirestoreMap(post: MarketplacePostEntity): Map<String, Any?> {
        return hashMapOf(
            "postId" to post.postId,
            "authorId" to post.authorId,
            "authorName" to post.authorName,
            "authorAvatarUrl" to post.authorAvatarUrl,
            "authorKycVerified" to post.authorKycVerified,
            "authorTrustScore" to post.authorTrustScore,
            "postType" to post.postType,
            "title" to post.title,
            "description" to post.description,
            "minAmount" to post.minAmount,
            "maxAmount" to post.maxAmount,
            "interestRate" to post.interestRate,
            "interestModel" to post.interestModel,
            "tenureMonths" to post.tenureMonths,
            "repaymentFrequency" to post.repaymentFrequency,
            "purposeCategory" to post.purposeCategory,
            "locationCity" to post.locationCity,
            "collateralOffered" to post.collateralOffered,
            "incomeProofStatus" to post.incomeProofStatus,
            "vouchCount" to post.vouchCount,
            "bidsCount" to post.bidsCount,
            "status" to post.status,
            "createdAt" to post.createdAt,
            "coBorrowerName" to post.coBorrowerName,
            "coBorrowerRelationship" to post.coBorrowerRelationship,
            "coBorrowerAvatarUrl" to post.coBorrowerAvatarUrl,
            "coBorrowerKycVerified" to post.coBorrowerKycVerified,
            "coBorrowerTrustScore" to post.coBorrowerTrustScore
        )
    }

    fun bidToFirestoreMap(bid: MarketplaceBidEntity): Map<String, Any?> {
        return hashMapOf(
            "bidId" to bid.bidId,
            "postId" to bid.postId,
            "bidderId" to bid.bidderId,
            "bidderName" to bid.bidderName,
            "bidderAvatarUrl" to bid.bidderAvatarUrl,
            "bidderKycVerified" to bid.bidderKycVerified,
            "bidderTrustScore" to bid.bidderTrustScore,
            "proposedAmount" to bid.proposedAmount,
            "proposedInterestRate" to bid.proposedInterestRate,
            "proposedTenureMonths" to bid.proposedTenureMonths,
            "message" to bid.message,
            "status" to bid.status,
            "createdAt" to bid.createdAt
        )
    }

    fun vouchToFirestoreMap(vouch: MarketplaceVouchEntity): Map<String, Any?> {
        return hashMapOf(
            "vouchId" to vouch.vouchId,
            "postId" to vouch.postId,
            "voucherUserId" to vouch.voucherUserId,
            "voucherName" to vouch.voucherName,
            "vouchReason" to vouch.vouchReason,
            "comment" to vouch.comment,
            "createdAt" to vouch.createdAt
        )
    }

    suspend fun publishPost(post: MarketplacePostEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Immediately persist to local database for zero-latency, offline-capable UI responsiveness
            marketplaceDao.insertPost(post)
            Log.d(TAG, "Post ${post.postId} stored locally in Room database")

            // 2. Ensure authenticated Firebase session before cloud broadcast
            firebaseManager.ensureFirebaseAuthSession()

            // 3. Cloud Firestore broadcast with explicit Map payload
            val payload = postToFirestoreMap(post)
            var cloudUploaded = false
            try {
                withTimeoutOrNull(12000L) {
                    firestore.collection(COLLECTION_POSTS)
                        .document(post.postId)
                        .set(payload, SetOptions.merge())
                        .await()
                    cloudUploaded = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore sync error for post ${post.postId}: ${e.message}", e)
            }

            if (cloudUploaded) {
                Log.i(TAG, "Post ${post.postId} successfully broadcasted to Cloud Firestore Community Wall.")
            } else {
                Log.w(TAG, "Post ${post.postId} saved locally; cloud broadcast was delayed or offline.")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Submits a competitive bid/counter-offer on an existing loan post.
     */
    suspend fun submitBid(bid: MarketplaceBidEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val post = marketplaceDao.getPostById(bid.postId)
            if (post != null && post.authorId.isNotBlank() && post.authorId == bid.bidderId) {
                return@withContext Result.failure(IllegalStateException("You cannot submit a proposal or bid on your own post."))
            }

            marketplaceDao.insertBid(bid)
            marketplaceDao.incrementBidsCount(bid.postId)
            try {
                firebaseManager.ensureFirebaseAuthSession()
                val bidPayload = bidToFirestoreMap(bid)
                firestore.collection(COLLECTION_BIDS)
                    .document(bid.bidId)
                    .set(bidPayload, SetOptions.merge())
                    .await()
                
                firestore.collection(COLLECTION_POSTS)
                    .document(bid.postId)
                    .update("bidsCount", com.google.firebase.firestore.FieldValue.increment(1))

                val currentPost = marketplaceDao.getPostById(bid.postId)
                if (currentPost != null && currentPost.authorId.isNotBlank() && currentPost.authorId != bid.bidderId) {
                    val notifMap = hashMapOf(
                        "notificationId" to ("bid_notif_" + UUID.randomUUID().toString().take(8)),
                        "userId" to currentPost.authorId,
                        "title" to "New Proposal: ₹${bid.proposedAmount.toInt()}",
                        "message" to "${bid.bidderName} proposed ₹${bid.proposedAmount.toInt()} at ${bid.proposedInterestRate}% on '${currentPost.title}'.",
                        "type" to "BID_OFFER",
                        "actionRoute" to "marketplace",
                        "timestamp" to System.currentTimeMillis()
                    )
                    firestore.collection("notifications").document(notifMap["notificationId"] as String).set(notifMap, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync failed for bid, saved locally: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit bid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Endorses / vouches for a borrower or lender post with social collateral reasons.
     * Prevents self-vouching and toggles on/off idempotently (1 vouch per user).
     */
    suspend fun vouchForPost(
        postId: String,
        voucherUserId: String,
        voucherName: String,
        reason: String = "COMMERCIAL_PEER",
        comment: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val post = marketplaceDao.getPostById(postId)
            if (post != null && post.authorId == voucherUserId) {
                return@withContext Result.failure(IllegalStateException("You cannot vouch for your own post."))
            }

            val alreadyVouched = marketplaceDao.hasUserVouched(postId, voucherUserId)
            if (alreadyVouched) {
                // Toggle off / un-vouch
                marketplaceDao.deleteVouch(postId, voucherUserId)
                marketplaceDao.decrementVouchCount(postId)
                try {
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .update("vouchCount", com.google.firebase.firestore.FieldValue.increment(-1))
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .collection("vouches")
                        .document(voucherUserId)
                        .delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore update failed for un-vouch: ${e.message}")
                }
                Result.success(false)
            } else {
                val vouch = MarketplaceVouchEntity(
                    vouchId = java.util.UUID.randomUUID().toString(),
                    postId = postId,
                    voucherUserId = voucherUserId,
                    voucherName = voucherName,
                    vouchReason = reason,
                    comment = comment
                )
                marketplaceDao.insertVouch(vouch)
                marketplaceDao.incrementVouchCount(postId)
                try {
                    val vouchPayload = vouchToFirestoreMap(vouch)
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .update("vouchCount", com.google.firebase.firestore.FieldValue.increment(1))
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .collection("vouches")
                        .document(voucherUserId)
                        .set(vouchPayload)
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore update failed for vouch: ${e.message}")
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserVouchedPostIdsFlow(userId: String): Flow<List<String>> {
        return marketplaceDao.getUserVouchedPostIdsFlow(userId)
    }

    fun getVouchesForPostFlow(postId: String): Flow<List<MarketplaceVouchEntity>> {
        return marketplaceDao.getVouchesForPostFlow(postId)
    }

    /**
     * Accepts a bid, marks status as ACCEPTED, and marks post as IN_NEGOTIATION / FUNDED.
     */
    suspend fun acceptBid(bidId: String, postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            marketplaceDao.updateBidStatus(bidId, "ACCEPTED")
            marketplaceDao.updatePostStatus(postId, "IN_NEGOTIATION")
            try {
                firestore.collection(COLLECTION_BIDS).document(bidId).update("status", "ACCEPTED")
                firestore.collection(COLLECTION_POSTS).document(postId).update("status", "IN_NEGOTIATION")
            } catch (e: Exception) {
                Log.w(TAG, "Firestore update failed for acceptBid: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private var realtimeFeedRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /**
     * Safely parses a Firestore document into a MarketplacePostEntity,
     * converting Longs to Doubles and handling null values to avoid reflection crashes.
     */
    fun parseDocumentToPost(doc: com.google.firebase.firestore.DocumentSnapshot): MarketplacePostEntity? {
        return try {
            val data = doc.data ?: return null
            MarketplacePostEntity(
                postId = doc.getString("postId") ?: doc.id,
                authorId = doc.getString("authorId") ?: "",
                authorName = doc.getString("authorName") ?: "Community Member",
                authorAvatarUrl = doc.getString("authorAvatarUrl") ?: "",
                authorKycVerified = doc.getBoolean("authorKycVerified") ?: false,
                authorTrustScore = (doc.getLong("authorTrustScore") ?: doc.getDouble("authorTrustScore")?.toLong() ?: 85L).toInt(),
                postType = doc.getString("postType") ?: "OFFER_TO_LEND",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                minAmount = doc.getDouble("minAmount") ?: doc.getLong("minAmount")?.toDouble() ?: 0.0,
                maxAmount = doc.getDouble("maxAmount") ?: doc.getLong("maxAmount")?.toDouble() ?: 0.0,
                interestRate = doc.getDouble("interestRate") ?: doc.getLong("interestRate")?.toDouble() ?: 0.0,
                interestModel = doc.getString("interestModel") ?: "SIMPLE",
                tenureMonths = (doc.getLong("tenureMonths") ?: doc.getDouble("tenureMonths")?.toLong() ?: 6L).toInt(),
                repaymentFrequency = doc.getString("repaymentFrequency") ?: "MONTHLY",
                purposeCategory = doc.getString("purposeCategory") ?: "PERSONAL",
                locationCity = doc.getString("locationCity") ?: "Bengaluru",
                collateralOffered = doc.getString("collateralOffered") ?: "",
                incomeProofStatus = doc.getString("incomeProofStatus") ?: "VERIFIED",
                vouchCount = (doc.getLong("vouchCount") ?: doc.getDouble("vouchCount")?.toLong() ?: 0L).toInt(),
                bidsCount = (doc.getLong("bidsCount") ?: doc.getDouble("bidsCount")?.toLong() ?: 0L).toInt(),
                status = doc.getString("status") ?: "OPEN",
                createdAt = doc.getLong("createdAt") ?: doc.getDouble("createdAt")?.toLong() ?: System.currentTimeMillis(),
                coBorrowerName = doc.getString("coBorrowerName") ?: "",
                coBorrowerRelationship = doc.getString("coBorrowerRelationship") ?: "",
                coBorrowerAvatarUrl = doc.getString("coBorrowerAvatarUrl") ?: "",
                coBorrowerKycVerified = doc.getBoolean("coBorrowerKycVerified") ?: false,
                coBorrowerTrustScore = (doc.getLong("coBorrowerTrustScore") ?: doc.getDouble("coBorrowerTrustScore")?.toLong() ?: 88L).toInt()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse post document ${doc.id}: ${e.message}", e)
            null
        }
    }

    /**
     * Attaches a real-time Firestore snapshot listener for active community wall posts.
     * Whenever any other user publishes or updates a post, it is immediately synchronized
     * into the local Room database and emitted to the UI via Room Flow.
     */
    fun startRealtimeFeedListener(scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        if (realtimeFeedRegistration != null) return

        scope.launch {
            try {
                firebaseManager.ensureFirebaseAuthSession()
                realtimeFeedRegistration = firestore.collection(COLLECTION_POSTS)
                    .whereEqualTo("status", "OPEN")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.w(TAG, "Realtime feed listener notice: ${error.message}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null && !snapshot.isEmpty) {
                            val posts = snapshot.documents.mapNotNull { parseDocumentToPost(it) }
                            if (posts.isNotEmpty()) {
                                scope.launch {
                                    marketplaceDao.insertPosts(posts)
                                    Log.d(TAG, "Realtime feed: synchronized ${posts.size} posts into local Room")
                                }
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register realtime feed listener: ${e.message}")
            }
        }
    }

    fun stopRealtimeFeedListener() {
        realtimeFeedRegistration?.remove()
        realtimeFeedRegistration = null
    }

    /**
     * Pulls latest active posts from Cloud Firestore into Room, and pushes any pending local posts to cloud.
     */
    suspend fun syncFeed(): Unit = withContext(Dispatchers.IO) {
        try {
            firebaseManager.ensureFirebaseAuthSession()
            val snapshot = withTimeoutOrNull(10000L) {
                firestore.collection(COLLECTION_POSTS)
                    .whereEqualTo("status", "OPEN")
                    .limit(50)
                    .get()
                    .await()
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val remotePosts = snapshot.documents.mapNotNull { parseDocumentToPost(it) }
                if (remotePosts.isNotEmpty()) {
                    marketplaceDao.insertPosts(remotePosts)
                    Log.d(TAG, "syncFeed: Loaded ${remotePosts.size} posts from Cloud Firestore")
                }
            }

            // Bidirectional sync: Push any local user posts to Cloud Firestore if missing
            val localPosts = marketplaceDao.getAllPostsFlow().firstOrNull() ?: emptyList()
            for (local in localPosts) {
                if (local.status == "OPEN" && !local.postId.startsWith("demo_") && !local.postId.startsWith("post_demo_")) {
                    try {
                        firestore.collection(COLLECTION_POSTS)
                            .document(local.postId)
                            .set(postToFirestoreMap(local), SetOptions.merge())
                            .await()
                        Log.d(TAG, "Synchronized local post ${local.postId} to Cloud Firestore")
                    } catch (syncEx: Exception) {
                        Log.d(TAG, "Local post cloud push note: ${syncEx.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch remote posts: ${e.message}")
        }
    }

    private suspend fun populateSamplePostsIfEmpty() {
        // No-op: Demo data seeding is disabled completely. Only genuine user posts are shown.
    }
}
