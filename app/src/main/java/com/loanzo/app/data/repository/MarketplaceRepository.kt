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

    /**
     * Publishes a new loan post (Lender Capital Offer or Borrower Loan Request).
     * Saves to local Room immediately, then syncs to Cloud Firestore.
     */
    suspend fun publishPost(post: MarketplacePostEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            marketplaceDao.insertPost(post)
            try {
                firestore.collection(COLLECTION_POSTS)
                    .document(post.postId)
                    .set(post, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.w(TAG, "Firestore sync failed for post, saved locally: ${e.message}")
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

    /**
     * Pulls latest active posts from Cloud Firestore into Room, or populates sample community posts if empty.
     */
    suspend fun syncFeed(): Unit = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection(COLLECTION_POSTS)
                .whereEqualTo("status", "OPEN")
                .limit(40)
                .get()
                .await()

            if (!snapshot.isEmpty) {
                val remotePosts = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(MarketplacePostEntity::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (remotePosts.isNotEmpty()) {
                    marketplaceDao.insertPosts(remotePosts)
                    return@withContext
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch remote posts: ${e.message}")
        }

        // Seed initial vibrant community posts if local feed is empty
        populateSamplePostsIfEmpty()
    }

    private suspend fun populateSamplePostsIfEmpty() {
        val samplePosts = listOf(
            MarketplacePostEntity(
                postId = "sample_post_1",
                authorId = "demo_vikram_malhotra",
                authorName = "Vikram Aditya (Angel Investor)",
                authorAvatarUrl = "",
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
                authorAvatarUrl = "",
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
                authorAvatarUrl = "",
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
                authorAvatarUrl = "",
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
    }
}
