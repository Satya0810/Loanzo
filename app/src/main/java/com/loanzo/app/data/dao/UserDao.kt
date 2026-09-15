package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId LIKE '%' || :query || '%' OR name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' LIMIT 20")
    fun searchUsers(query: String): Flow<List<UserEntity>>

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId LIKE 'demo_%' OR userId LIKE 'demo-%' OR userId = 'user_demo' OR username LIKE 'demo_%' OR userId IN ('kumar', 'prince25', 'demo_user_arjun', 'demo_lender_priya', 'demo_borrower_rahul', 'demo_agent_abhisi', 'demo_agent_sunil', 'demo_admin_satyam', 'demo_amit_verma', 'demo_sneha_roy', 'demo_rajesh_gupta', 'demo_vikram_malhotra', 'demo_guarantor_nirmala', 'demo_coborrower_rohan', 'demo_meera_sen', 'demo_kunal_rawat', 'demo_alok_trivedi', 'demo_staff_deepak', 'demo_staff_neha') OR username IN ('kumar', 'prince25', 'user_demo', 'demo_user_arjun', 'demo_lender_priya', 'demo_borrower_rahul', 'demo_agent_abhisi', 'demo_agent_sunil', 'demo_admin_satyam', 'demo_amit_verma', 'demo_sneha_roy', 'demo_rajesh_gupta', 'demo_vikram_malhotra', 'demo_guarantor_nirmala', 'demo_coborrower_rohan', 'demo_meera_sen', 'demo_kunal_rawat', 'demo_alok_trivedi', 'demo_staff_deepak', 'demo_staff_neha')")
    suspend fun deleteDemoUsers()

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)
}
