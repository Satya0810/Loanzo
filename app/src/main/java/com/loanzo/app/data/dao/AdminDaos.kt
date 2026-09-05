package com.loanzo.app.data.dao

import androidx.room.*
import com.loanzo.app.data.entity.CollateralVaultEntity
import com.loanzo.app.data.entity.ComplaintEntity
import com.loanzo.app.data.entity.MediationMeetingEntity
import com.loanzo.app.data.entity.NocCertificateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY createdAt DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE status = :status ORDER BY createdAt DESC")
    fun getComplaintsByStatus(status: String): Flow<List<ComplaintEntity>>

    @Query("SELECT * FROM complaints WHERE complaintId = :complaintId")
    suspend fun getComplaintById(complaintId: String): ComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaints(complaints: List<ComplaintEntity>)

    @Update
    suspend fun updateComplaint(complaint: ComplaintEntity)

    @Query("UPDATE complaints SET status = :status, resolutionNotes = :notes, resolvedAt = :resolvedAt WHERE complaintId = :complaintId")
    suspend fun updateComplaintStatus(complaintId: String, status: String, notes: String?, resolvedAt: Long?)

    @Query("DELETE FROM complaints WHERE complaintId LIKE 'comp_demo_%'")
    suspend fun deleteDemoComplaints()
}

@Dao
interface MediationMeetingDao {
    @Query("SELECT * FROM mediation_meetings ORDER BY scheduledDateTime ASC")
    fun getAllMeetings(): Flow<List<MediationMeetingEntity>>

    @Query("SELECT * FROM mediation_meetings WHERE status = 'SCHEDULED' ORDER BY scheduledDateTime ASC")
    fun getUpcomingMeetings(): Flow<List<MediationMeetingEntity>>

    @Query("SELECT * FROM mediation_meetings WHERE meetingId = :meetingId")
    suspend fun getMeetingById(meetingId: String): MediationMeetingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MediationMeetingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<MediationMeetingEntity>)

    @Update
    suspend fun updateMeeting(meeting: MediationMeetingEntity)

    @Query("UPDATE mediation_meetings SET status = :status, adminNotes = :notes WHERE meetingId = :meetingId")
    suspend fun updateMeetingStatus(meetingId: String, status: String, notes: String?)

    @Query("DELETE FROM mediation_meetings WHERE meetingId LIKE 'meet_demo_%'")
    suspend fun deleteDemoMeetings()
}

@Dao
interface CollateralVaultDao {
    @Query("SELECT * FROM collateral_vault ORDER BY createdAt DESC")
    fun getAllVaultItems(): Flow<List<CollateralVaultEntity>>

    @Query("SELECT * FROM collateral_vault WHERE vaultItemId = :vaultItemId")
    suspend fun getVaultItemById(vaultItemId: String): CollateralVaultEntity?

    @Query("SELECT * FROM collateral_vault WHERE loanId = :loanId")
    suspend fun getVaultItemByLoanId(loanId: String): CollateralVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: CollateralVaultEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItems(items: List<CollateralVaultEntity>)

    @Update
    suspend fun updateVaultItem(item: CollateralVaultEntity)

    @Query("UPDATE collateral_vault SET lockerNumber = :lockerNumber, barcodeTag = :barcodeTag, tamperSealNumber = :sealNumber, custodyStatus = :status WHERE vaultItemId = :vaultItemId")
    suspend fun assignLockerAndSeal(vaultItemId: String, lockerNumber: String, barcodeTag: String, sealNumber: String, status: String)

    @Query("UPDATE collateral_vault SET custodyStatus = :status, releaseDate = :releaseDate WHERE loanId = :loanId")
    suspend fun updateCustodyStatusByLoan(loanId: String, status: String, releaseDate: Long?)

    @Query("DELETE FROM collateral_vault WHERE vaultItemId LIKE 'vault_demo_%'")
    suspend fun deleteDemoVaultItems()
}

@Dao
interface NocCertificateDao {
    @Query("SELECT * FROM noc_certificates ORDER BY issuedAt DESC")
    fun getAllNocs(): Flow<List<NocCertificateEntity>>

    @Query("SELECT * FROM noc_certificates WHERE nocId = :nocId")
    suspend fun getNocById(nocId: String): NocCertificateEntity?

    @Query("SELECT * FROM noc_certificates WHERE loanId = :loanId")
    suspend fun getNocByLoanId(loanId: String): NocCertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNoc(noc: NocCertificateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNocs(nocs: List<NocCertificateEntity>)

    @Query("DELETE FROM noc_certificates WHERE nocId LIKE 'noc_demo_%'")
    suspend fun deleteDemoNocs()
}
