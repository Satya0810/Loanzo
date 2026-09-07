package com.loanzo.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.loanzo.app.data.dao.CollateralVaultDao
import com.loanzo.app.data.dao.LoanDao
import com.loanzo.app.data.dao.RepaymentDao
import com.loanzo.app.data.dao.UserDao
import com.loanzo.app.data.dao.VaultDocumentDao
import com.loanzo.app.data.entity.VaultDocumentEntity
import com.loanzo.app.util.pdf.UserDossierPdfGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository responsible for the User's Encrypted Document Vault.
 * Manages secure internal storage, SHA-256 integrity auditing,
 * PDF generation, and view/share intents.
 */
@Singleton
class DocumentVaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDocumentDao: VaultDocumentDao,
    private val userDao: UserDao,
    private val loanDao: LoanDao,
    private val repaymentDao: RepaymentDao,
    private val collateralVaultDao: CollateralVaultDao
) {

    private val TAG = "DocumentVaultRepo"

    val vaultDir: File by lazy {
        File(context.filesDir, "vault_documents").apply { mkdirs() }
    }

    fun getDocumentsForUser(userId: String): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getDocumentsForUser(userId)
    }

    fun getDocumentsForLoan(loanId: String): Flow<List<VaultDocumentEntity>> {
        return vaultDocumentDao.getDocumentsForLoan(loanId)
    }

    /**
     * Generates and archives the comprehensive Master Financial & Legal Dossier
     * documenting everything for the specified user.
     */
    suspend fun generateAndVaultMasterDossier(userId: String): VaultDocumentEntity? = withContext(Dispatchers.IO) {
        try {
            val user = userDao.getUserById(userId) ?: return@withContext null
            val loans = loanDao.getAllLoansForUser(userId).firstOrNull() ?: emptyList()
            val repayments = repaymentDao.getAllRepaymentsForBorrower(userId).firstOrNull() ?: emptyList()
            val collateralItems = collateralVaultDao.getAllVaultItems().firstOrNull()?.filter {
                it.borrowerId == userId
            } ?: emptyList()

            val fileName = "LZ_Financial_Dossier_${user.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val targetFile = File(vaultDir, fileName)

            val (file, checksum) = UserDossierPdfGenerator.generateUserDossier(
                context = context,
                user = user,
                loans = loans,
                repayments = repayments,
                collateralItems = collateralItems,
                outputFile = targetFile
            )

            val document = VaultDocumentEntity(
                documentId = UUID.randomUUID().toString(),
                userId = userId,
                loanId = null,
                title = "Master Financial & Legal Dossier",
                documentType = "FINANCIAL_DOSSIER",
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                checksumSha256 = checksum,
                isEncrypted = true,
                description = "Complete identity, KYC certifications, credit rating, active facilities, repayment trail, and collateral custody."
            )

            vaultDocumentDao.insertDocument(document)
            Log.d(TAG, "Master dossier generated and vaulted successfully: ${document.documentId}")
            document
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate and vault master dossier", e)
            null
        }
    }

    /**
     * Securely copies or archives an externally created document (e.g. Agreement, Sanction, Receipt, NOC)
     * into the protected Document Vault directory and registers it in the database.
     */
    suspend fun archiveDocument(
        userId: String,
        loanId: String?,
        title: String,
        documentType: String,
        sourceFile: File,
        description: String = ""
    ): VaultDocumentEntity? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext null

            val targetFile = File(vaultDir, "VAULT_${documentType}_${sourceFile.name}")
            sourceFile.copyTo(targetFile, overwrite = true)

            val checksum = calculateChecksum(targetFile)

            val entity = VaultDocumentEntity(
                documentId = UUID.randomUUID().toString(),
                userId = userId,
                loanId = loanId,
                title = title,
                documentType = documentType,
                fileName = targetFile.name,
                filePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                checksumSha256 = checksum,
                isEncrypted = true,
                description = description
            )

            vaultDocumentDao.insertDocument(entity)
            Log.d(TAG, "Document archived to vault: ${entity.title} (${entity.documentId})")
            entity
        } catch (e: Exception) {
            Log.e(TAG, "Failed to archive document to vault", e)
            null
        }
    }

    suspend fun deleteDocument(documentId: String) = withContext(Dispatchers.IO) {
        try {
            val doc = vaultDocumentDao.getDocumentById(documentId)
            if (doc != null) {
                val file = File(doc.filePath)
                if (file.exists()) file.delete()
                vaultDocumentDao.deleteDocument(documentId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete vault document", e)
        }
    }

    fun openDocument(context: Context, document: VaultDocumentEntity) {
        val file = File(document.filePath)
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${document.filePath}")
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Vault Document"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open document", e)
        }
    }

    fun shareDocument(context: Context, document: VaultDocumentEntity) {
        val file = File(document.filePath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, document.title)
                putExtra(Intent.EXTRA_TEXT, "Loanzo Secured Vault Document: ${document.title}\nChecksum (SHA-256): ${document.checksumSha256}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Vault Document"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share document", e)
        }
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
