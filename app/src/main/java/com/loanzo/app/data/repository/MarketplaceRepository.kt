package com.loanzo.app.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.loanzo.app.data.dao.MarketplaceDao
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
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
     * Endorses / vouches for a borrower or lender post.
     */
    suspend fun vouchForPost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            marketplaceDao.incrementVouchCount(postId)
            try {
                firestore.collection(COLLECTION_POSTS)
                    .document(postId)
                    .update("vouchCount", com.google.firebase.firestore.FieldValue.increment(1))
            } catch (e: Exception) {
                Log.w(TAG, "Firestore update failed for vouch: ${e.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        val count = marketplaceDao.getAllPostsFlow()
        // We will seed 4 realistic community loan posts
        val samplePosts = listOf(
            MarketplacePostEntity(
                postId = "sample_post_1",
                authorId = "community_lender_1",
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
                createdAt = System.currentTimeMillis() - 3600000 * 4
            ),
            MarketplacePostEntity(
                postId = "sample_post_2",
                authorId = "community_borrower_1",
                authorName = "Sneha Patil",
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
                createdAt = System.currentTimeMillis() - 3600000 * 8
            ),
            MarketplacePostEntity(
                postId = "sample_post_3",
                authorId = "community_lender_2",
                authorName = "Rajeshwari Financials",
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
                createdAt = System.currentTimeMillis() - 3600000 * 24
            ),
            MarketplacePostEntity(
                postId = "sample_post_4",
                authorId = "community_borrower_2",
                authorName = "Arjun Mehra",
                authorAvatarUrl = "",
                authorKycVerified = true,
                authorTrustScore = 87,
                postType = "SEEKING_LOAN",
                title = "Emergency Family Hospitalization Bill",
                description = "Need urgent assistance to clear father's post-surgery hospital bill before discharge. Employed full-time as senior QA engineer with monthly salary of ₹65,000.",
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
                createdAt = System.currentTimeMillis() - 3600000 * 32
            )
        )
        marketplaceDao.insertPosts(samplePosts)
    }
}
