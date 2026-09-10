package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.SupportTicketEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupportTicketDao {

    // --- Admin Queries ---

    @Query("SELECT * FROM support_tickets ORDER BY CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, createdAt DESC")
    fun getAllTickets(): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE status = :status ORDER BY CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 3 END, createdAt DESC")
    fun getTicketsByStatus(status: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT COUNT(*) FROM support_tickets WHERE status = 'OPEN'")
    fun getOpenTicketCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM support_tickets WHERE status = 'CALLBACK_SCHEDULED' AND scheduledCallbackAt >= :todayStart AND scheduledCallbackAt < :todayEnd")
    fun getCallbacksTodayCount(todayStart: Long, todayEnd: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM support_tickets WHERE status IN ('OPEN', 'UNDER_REVIEW', 'IN_PROGRESS', 'ESCALATED') AND createdAt < :threshold")
    fun getUnresolvedOlderThanCount(threshold: Long): Flow<Int>

    // --- User Queries ---

    @Query("SELECT * FROM support_tickets WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTicketsForUser(userId: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT * FROM support_tickets WHERE userId = :userId AND status = 'RESOLVED' AND feedbackRating IS NULL")
    fun getPendingFeedbackTickets(userId: String): Flow<List<SupportTicketEntity>>

    @Query("SELECT COUNT(*) FROM support_tickets WHERE userId = :userId AND status = 'RESOLVED' AND feedbackRating IS NULL")
    fun getPendingFeedbackCount(userId: String): Flow<Int>

    // --- Single Ticket ---

    @Query("SELECT * FROM support_tickets WHERE ticketId = :ticketId")
    suspend fun getTicketById(ticketId: String): SupportTicketEntity?

    @Query("SELECT * FROM support_tickets WHERE ticketId = :ticketId")
    fun observeTicketById(ticketId: String): Flow<SupportTicketEntity?>

    // --- Mutations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: SupportTicketEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<SupportTicketEntity>)

    @Update
    suspend fun updateTicket(ticket: SupportTicketEntity)

    @Query("UPDATE support_tickets SET status = :status, adminNotes = :adminNotes, updatedAt = :updatedAt WHERE ticketId = :ticketId")
    suspend fun updateTicketStatus(ticketId: String, status: String, adminNotes: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE support_tickets SET status = 'CALLBACK_SCHEDULED', scheduledCallbackAt = :callbackAt, adminNotes = :adminNotes, updatedAt = :updatedAt WHERE ticketId = :ticketId")
    suspend fun scheduleCallback(ticketId: String, callbackAt: Long, adminNotes: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE support_tickets SET status = 'RESOLVED', resolutionNotes = :resolutionNotes, resolvedAt = :resolvedAt, updatedAt = :updatedAt WHERE ticketId = :ticketId")
    suspend fun resolveTicket(ticketId: String, resolutionNotes: String, resolvedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE support_tickets SET feedbackRating = :rating, feedbackComment = :comment, feedbackSubmittedAt = :submittedAt, status = 'CLOSED', updatedAt = :updatedAt WHERE ticketId = :ticketId")
    suspend fun submitFeedback(ticketId: String, rating: Int, comment: String?, submittedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    // --- Demo Cleanup ---

    @Query("DELETE FROM support_tickets WHERE ticketId LIKE 'tkt_demo_%'")
    suspend fun deleteDemoTickets()
}
