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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketplaceRepository @Inject constructor(
    private val marketplaceDao: MarketplaceDao
) {
    companion object {
        private const val TAG = "MarketplaceRepo"
        private const val COLLECTION_POSTS = "marketplace_posts"
        private const val COLLECTION_BIDS = "marketplace_bids"
    }

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

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

    suspend fun publishPost(post: MarketplacePostEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Immediately persist to local database for zero-latency, offline-capable UI responsiveness
            marketplaceDao.insertPost(post)
            Log.d(TAG, "Post ${post.postId} stored locally in Room database")

            // 2. Cloud Firestore broadcast with 10-second timeout to allow network connection & handshake
            var cloudUploaded = false
            try {
                withTimeoutOrNull(10000L) {
                    firestore.collection(COLLECTION_POSTS)
                        .document(post.postId)
                        .set(post, SetOptions.merge())
                        .await()
                    cloudUploaded = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync error for post ${post.postId}: ${e.message}")
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
                firestore.collection(COLLECTION_BIDS)
                    .document(bid.bidId)
                    .set(bid, SetOptions.merge())
                    .await()
                
                firestore.collection(COLLECTION_POSTS)
                    .document(bid.postId)
                    .update("bidsCount", com.google.firebase.firestore.FieldValue.increment(1))

                val post = marketplaceDao.getPostById(bid.postId)
                if (post != null && post.authorId.isNotBlank() && post.authorId != bid.bidderId) {
                    val notif = com.loanzo.app.data.entity.NotificationEntity(
                        notificationId = "bid_notif_" + UUID.randomUUID().toString().take(8),
                        userId = post.authorId,
                        title = "New Proposal: ₹${bid.proposedAmount.toInt()}",
                        message = "${bid.bidderName} proposed ₹${bid.proposedAmount.toInt()} at ${bid.proposedInterestRate}% on '${post.title}'.",
                        type = "BID_OFFER",
                        actionRoute = "marketplace"
                    )
                    firestore.collection("notifications").document(notif.notificationId).set(notif)
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
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .update("vouchCount", com.google.firebase.firestore.FieldValue.increment(1))
                    firestore.collection(COLLECTION_POSTS)
                        .document(postId)
                        .collection("vouches")
                        .document(voucherUserId)
                        .set(vouch)
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
                authorTrustScore = (doc.getLong("authorTrustScore") ?: 85L).toInt(),
                postType = doc.getString("postType") ?: "OFFER_TO_LEND",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                minAmount = doc.getDouble("minAmount") ?: doc.getLong("minAmount")?.toDouble() ?: 0.0,
                maxAmount = doc.getDouble("maxAmount") ?: doc.getLong("maxAmount")?.toDouble() ?: 0.0,
                interestRate = doc.getDouble("interestRate") ?: doc.getLong("interestRate")?.toDouble() ?: 0.0,
                interestModel = doc.getString("interestModel") ?: "SIMPLE",
                tenureMonths = (doc.getLong("tenureMonths") ?: 6L).toInt(),
                repaymentFrequency = doc.getString("repaymentFrequency") ?: "MONTHLY",
                purposeCategory = doc.getString("purposeCategory") ?: "PERSONAL",
                locationCity = doc.getString("locationCity") ?: "Bengaluru",
                collateralOffered = doc.getString("collateralOffered") ?: "",
                incomeProofStatus = doc.getString("incomeProofStatus") ?: "VERIFIED",
                vouchCount = (doc.getLong("vouchCount") ?: 0L).toInt(),
                bidsCount = (doc.getLong("bidsCount") ?: 0L).toInt(),
                status = doc.getString("status") ?: "OPEN",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                coBorrowerName = doc.getString("coBorrowerName") ?: "",
                coBorrowerRelationship = doc.getString("coBorrowerRelationship") ?: "",
                coBorrowerAvatarUrl = doc.getString("coBorrowerAvatarUrl") ?: "",
                coBorrowerKycVerified = doc.getBoolean("coBorrowerKycVerified") ?: false,
                coBorrowerTrustScore = (doc.getLong("coBorrowerTrustScore") ?: 88L).toInt()
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

        try {
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

    fun stopRealtimeFeedListener() {
        realtimeFeedRegistration?.remove()
        realtimeFeedRegistration = null
    }

    /**
     * Pulls latest active posts from Cloud Firestore into Room, or populates sample community posts if empty.
     */
    suspend fun syncFeed(): Unit = withContext(Dispatchers.IO) {
        try {
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
                    return@withContext
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch remote posts: ${e.message}")
        }

        // Seed initial vibrant community posts ONLY if local feed is genuinely empty
        populateSamplePostsIfEmpty()
    }

    private suspend fun populateSamplePostsIfEmpty() {
        if (marketplaceDao.getPostCount() > 0) return
        val samplePosts = listOf(
            MarketplacePostEntity(
                postId = "sample_post_1",
                authorId = "demo_vikram_malhotra",
                authorName = "Vikram Aditya (Angel Investor)",
                authorAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Vikram Aditya"),
                authorKycVerified = true,
                authorTrustScore = 98,
                postType = "OFFER_TO_LEND",
                title = "Capital Pool for Education & Tech Certifications",
                description = "Available to finance college fees, coding bootcamps, and certification exams. Transparent flat interest rate with flexible repayment tenure and zero prepayment charges.",
                minAmount = 25000.0,
                maxAmount = 150000.0,
                interestRate = 9.5,
                interestModel = "SIMPLE",
                tenureMonths = 12,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "EDUCATION",
                locationCity = "Bengaluru",
                collateralOffered = "DigiLocker Student ID / Aadhaar verified",
                vouchCount = 24,
                bidsCount = 5,
                status = "OPEN",
                createdAt = System.currentTimeMillis() - 3600000 * 4,
                coBorrowerName = "",
                coBorrowerRelationship = "",
                coBorrowerKycVerified = false,
                coBorrowerTrustScore = 85
            ),
            MarketplacePostEntity(
                postId = "sample_post_2",
                authorId = "demo_sneha_roy",
                authorName = "Sneha Roy",
                authorAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Sneha Roy"),
                authorKycVerified = true,
                authorTrustScore = 91,
                postType = "SEEKING_LOAN",
                title = "Medical Clinic Equipment & Diagnostic Tools",
                description = "Seeking funds to purchase an ECG monitor and sterilization equipment for my newly established clinic in Pune. Regular OPD cash flow guaranteed with verified medical registration.",
                minAmount = 50000.0,
                maxAmount = 50000.0,
                interestRate = 11.0,
                interestModel = "SIMPLE",
                tenureMonths = 6,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "MEDICAL",
                locationCity = "Pune",
                collateralOffered = "Clinic lease agreement + Medical License",
                vouchCount = 19,
                bidsCount = 3,
                status = "OPEN",
                createdAt = System.currentTimeMillis() - 3600000 * 8,
                coBorrowerName = "Dr. Rohan Patil",
                coBorrowerRelationship = "Spouse (Clinic Partner)",
                coBorrowerKycVerified = true,
                coBorrowerTrustScore = 94
            ),
            MarketplacePostEntity(
                postId = "sample_post_3",
                authorId = "demo_rajesh_gupta",
                authorName = "Rajesh Gupta",
                authorAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Rajesh Gupta"),
                authorKycVerified = true,
                authorTrustScore = 95,
                postType = "OFFER_TO_LEND",
                title = "MSME Working Capital & Inventory Deployment",
                description = "Offering working capital loans for small grocery, apparel, and manufacturing businesses. Same-day approval upon GSTIN or Udyam certificate verification.",
                minAmount = 50000.0,
                maxAmount = 250000.0,
                interestRate = 10.5,
                interestModel = "SIMPLE",
                tenureMonths = 18,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "BUSINESS",
                locationCity = "Mumbai",
                collateralOffered = "Udyam Aadhaar / GSTIN verified",
                vouchCount = 32,
                bidsCount = 8,
                status = "OPEN",
                createdAt = System.currentTimeMillis() - 3600000 * 24,
                coBorrowerName = "",
                coBorrowerRelationship = "",
                coBorrowerKycVerified = false,
                coBorrowerTrustScore = 85
            ),
            MarketplacePostEntity(
                postId = "sample_post_4",
                authorId = "demo_user_arjun",
                authorName = "Arjun Mehta",
                authorAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Arjun Mehta"),
                authorKycVerified = true,
                authorTrustScore = 87,
                postType = "SEEKING_LOAN",
                title = "Emergency Family Hospitalization Bill",
                description = "Need urgent assistance to clear father's post-surgery hospital bill before discharge. Employed full-time as senior QA engineer with monthly salary of Rs 65,000.",
                minAmount = 35000.0,
                maxAmount = 35000.0,
                interestRate = 12.0,
                interestModel = "SIMPLE",
                tenureMonths = 5,
                repaymentFrequency = "MONTHLY",
                purposeCategory = "EMERGENCY",
                locationCity = "Delhi NCR",
                collateralOffered = "Salary slips (3 months) + PAN/Aadhaar",
                vouchCount = 14,
                bidsCount = 4,
                status = "OPEN",
                createdAt = System.currentTimeMillis() - 3600000 * 32,
                coBorrowerName = "Sunita Mehra",
                coBorrowerRelationship = "Spouse (Co-Signer)",
                coBorrowerKycVerified = true,
                coBorrowerTrustScore = 89
            )
        )
        marketplaceDao.insertPosts(samplePosts)

        // Seed initial vibrant vouchers for community posts
        val sampleVouches = listOf(
            MarketplaceVouchEntity(
                vouchId = "vouch_1_1",
                postId = "sample_post_1",
                voucherUserId = "demo_lender_priya",
                voucherName = "Priya Patel",
                vouchReason = "PAST_REPAYMENT",
                comment = "Known Vikram for 4 years in Bangalore angel syndicates. Exceptional track record.",
                voucherKycVerified = true,
                voucherTrustScore = 96,
                voucherRole = "P2P Lender & MSME Capital Deployer"
            ),
            MarketplaceVouchEntity(
                vouchId = "vouch_1_2",
                postId = "sample_post_1",
                voucherUserId = "demo_amit_verma",
                voucherName = "Amit Verma",
                vouchReason = "BUSINESS_PEER",
                comment = "Highly professional capital deployer. Clear terms, no hidden fees.",
                voucherKycVerified = true,
                voucherTrustScore = 93,
                voucherRole = "MSME Retailer & CNC Operator"
            ),
            MarketplaceVouchEntity(
                vouchId = "vouch_2_1",
                postId = "sample_post_2",
                voucherUserId = "demo_coborrower_rohan",
                voucherName = "Dr. Rohan Patil",
                vouchReason = "COMMUNITY_REFERENCE",
                comment = "Dr. Sneha is a respected medical professional in Pune. Clinic serves 40+ patients daily.",
                voucherKycVerified = true,
                voucherTrustScore = 97,
                voucherRole = "Clinic Partner & Co-Borrower"
            ),
            MarketplaceVouchEntity(
                vouchId = "vouch_2_2",
                postId = "sample_post_2",
                voucherUserId = "demo_guarantor_nirmala",
                voucherName = "Nirmala Devi",
                vouchReason = "PAST_REPAYMENT",
                comment = "Honored all previous vendor credits on time. Highly recommended borrower.",
                voucherKycVerified = true,
                voucherTrustScore = 91,
                voucherRole = "Guarantor & Community Elder"
            ),
            MarketplaceVouchEntity(
                vouchId = "vouch_4_1",
                postId = "sample_post_4",
                voucherUserId = "demo_vikram_malhotra",
                voucherName = "Vikram Malhotra",
                vouchReason = "BUSINESS_PEER",
                comment = "Arjun is a reliable senior colleague at our IT firm. Genuine medical emergency.",
                voucherKycVerified = true,
                voucherTrustScore = 92,
                voucherRole = "VP of Engineering"
            )
        )
        for (v in sampleVouches) {
            marketplaceDao.insertVouch(v)
        }

        // Seed initial vibrant bids with 2D cartoon avatars
        val sampleBids = listOf(
            com.loanzo.app.data.entity.MarketplaceBidEntity(
                bidId = "sample_bid_1_1",
                postId = "sample_post_1",
                bidderId = "demo_amit_verma",
                bidderName = "Amit Verma",
                bidderAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Amit Verma"),
                bidderKycVerified = true,
                bidderTrustScore = 93,
                proposedAmount = 50000.0,
                proposedInterestRate = 9.5,
                proposedTenureMonths = 12,
                message = "Need ₹50,000 for technical tooling expansion. Fast turnaround and 100% on-time repayment.",
                status = "PENDING",
                createdAt = System.currentTimeMillis() - 3600000 * 2
            ),
            com.loanzo.app.data.entity.MarketplaceBidEntity(
                bidId = "sample_bid_2_1",
                postId = "sample_post_2",
                bidderId = "demo_lender_priya",
                bidderName = "Priya Patel",
                bidderAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Priya Patel"),
                bidderKycVerified = true,
                bidderTrustScore = 96,
                proposedAmount = 50000.0,
                proposedInterestRate = 10.5,
                proposedTenureMonths = 6,
                message = "Happy to fund clinic sterilization equipment. Ready for instant digital agreement signing.",
                status = "PENDING",
                createdAt = System.currentTimeMillis() - 3600000 * 5
            ),
            com.loanzo.app.data.entity.MarketplaceBidEntity(
                bidId = "sample_bid_3_1",
                postId = "sample_post_3",
                bidderId = "demo_kunal_rawat",
                bidderName = "Kunal Rawat",
                bidderAvatarUrl = com.loanzo.app.util.CartoonAvatarHelper.getCartoonAvatarUrl("Kunal Rawat"),
                bidderKycVerified = true,
                bidderTrustScore = 88,
                proposedAmount = 100000.0,
                proposedInterestRate = 10.5,
                proposedTenureMonths = 12,
                message = "Seeking working capital for transport logistics maintenance. Invoices attached.",
                status = "PENDING",
                createdAt = System.currentTimeMillis() - 3600000 * 18
            )
        )
        for (b in sampleBids) {
            marketplaceDao.insertBid(b)
        }
    }
}
