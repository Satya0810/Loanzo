package com.loanzo.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loanzo.app.data.entity.VaultDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDocumentDao {

    @Query("SELECT * FROM vault_documents WHERE userId = :userId ORDER BY generatedAt DESC")
    fun getDocumentsForUser(userId: String): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE loanId = :loanId ORDER BY generatedAt DESC")
    fun getDocumentsForLoan(loanId: String): Flow<List<VaultDocumentEntity>>

    @Query("SELECT * FROM vault_documents WHERE documentId = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: String): VaultDocumentEntity?

    @Query("SELECT * FROM vault_documents ORDER BY generatedAt DESC")
    fun getAllVaultDocuments(): Flow<List<VaultDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: VaultDocumentEntity)

    @Query("DELETE FROM vault_documents WHERE documentId = :documentId")
    suspend fun deleteDocument(documentId: String)

    @Query("DELETE FROM vault_documents WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
