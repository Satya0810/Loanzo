package com.loanzo.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a secured legal, financial, or regulatory document stored in the user's Document Vault.
 * Includes cryptographic SHA-256 hash for tamper evidence and auditability.
 */
@Entity(tableName = "vault_documents")
data class VaultDocumentEntity(
    @PrimaryKey
    val documentId: String,
    val userId: String,
    val loanId: String? = null,
    val title: String,
    val documentType: String, // FINANCIAL_DOSSIER, LOAN_AGREEMENT, SANCTION_LETTER, PAYMENT_RECEIPT, NOC_CERTIFICATE, ACCOUNT_STATEMENT
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val isEncrypted: Boolean = true,
    val description: String = "",
    val generatedAt: Long = System.currentTimeMillis()
)
