package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.MarketplaceBidEntity
import com.loanzo.app.data.entity.MarketplacePostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {

    @Query("SELECT * FROM marketplace_posts ORDER BY createdAt DESC")
    fun getAllPostsFlow(): Flow<List<MarketplacePostEntity>>

    @Query("SELECT * FROM marketplace_posts WHERE postType = :postType ORDER BY createdAt DESC")
    fun getPostsByTypeFlow(postType: String): Flow<List<MarketplacePostEntity>>

    @Query("SELECT * FROM marketplace_posts WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getPostsByAuthorFlow(authorId: String): Flow<List<MarketplacePostEntity>>

    @Query("SELECT * FROM marketplace_posts WHERE postId = :postId LIMIT 1")
    suspend fun getPostById(postId: String): MarketplacePostEntity?

    @Query("SELECT * FROM marketplace_posts WHERE postId = :postId LIMIT 1")
    fun getPostByIdFlow(postId: String): Flow<MarketplacePostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: MarketplacePostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<MarketplacePostEntity>)

    @Query("UPDATE marketplace_posts SET status = :status WHERE postId = :postId")
    suspend fun updatePostStatus(postId: String, status: String)

    @Query("UPDATE marketplace_posts SET vouchCount = vouchCount + 1 WHERE postId = :postId")
    suspend fun incrementVouchCount(postId: String)

    @Query("UPDATE marketplace_posts SET bidsCount = bidsCount + 1 WHERE postId = :postId")
    suspend fun incrementBidsCount(postId: String)

    @Query("DELETE FROM marketplace_posts WHERE postId = :postId")
    suspend fun deletePost(postId: String)

    // Bids / Proposals
    @Query("SELECT * FROM marketplace_bids WHERE postId = :postId ORDER BY createdAt DESC")
    fun getBidsForPostFlow(postId: String): Flow<List<MarketplaceBidEntity>>

    @Query("SELECT * FROM marketplace_bids WHERE bidderId = :bidderId ORDER BY createdAt DESC")
    fun getBidsByBidderFlow(bidderId: String): Flow<List<MarketplaceBidEntity>>

    @Query("SELECT * FROM marketplace_bids WHERE bidId = :bidId LIMIT 1")
    suspend fun getBidById(bidId: String): MarketplaceBidEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBid(bid: MarketplaceBidEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBids(bids: List<MarketplaceBidEntity>)

    @Query("UPDATE marketplace_bids SET status = :status WHERE bidId = :bidId")
    suspend fun updateBidStatus(bidId: String, status: String)

    @Query("DELETE FROM marketplace_bids WHERE bidId = :bidId")
    suspend fun deleteBid(bidId: String)
}
