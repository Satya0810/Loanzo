package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE userId = :userId OR (:isAdmin = 1 AND (userId = 'ADMIN' OR userId = 'ALL_ADMINS')) ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: String, isAdmin: Boolean = false): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (userId = :userId OR (:isAdmin = 1 AND (userId = 'ADMIN' OR userId = 'ALL_ADMINS'))) AND isRead = 0")
    fun getUnreadCount(userId: String, isAdmin: Boolean = false): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE notificationId = :notificationId")
    suspend fun markAsRead(notificationId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId OR (:isAdmin = 1 AND (userId = 'ADMIN' OR userId = 'ALL_ADMINS'))")
    suspend fun markAllAsRead(userId: String, isAdmin: Boolean = false)

    @Query("DELETE FROM notifications WHERE notificationId = :notificationId")
    suspend fun deleteNotification(notificationId: String)

    @Query("DELETE FROM notifications WHERE userId = :userId OR (:isAdmin = 1 AND (userId = 'ADMIN' OR userId = 'ALL_ADMINS'))")
    suspend fun clearAllForUser(userId: String, isAdmin: Boolean = false)

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE userId = :userId AND type = :type AND relatedLoanId = :relatedLoanId AND dayKey = :dayKey)")
    suspend fun existsNotification(userId: String, type: String, relatedLoanId: String, dayKey: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE notificationId = :notificationId)")
    suspend fun existsNotificationById(notificationId: String): Boolean
}
