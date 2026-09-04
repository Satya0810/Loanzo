package com.loanzo.app.data.firebase

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.loanzo.app.data.entity.UserEntity
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor() {

    companion object {
        private const val TAG = "FirebaseManager"
        private const val USERS_COLLECTION = "users"
    }

    private val firestore: FirebaseFirestore by lazy {
        try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            FirebaseFirestore.getInstance(app, "default")
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val realtimeDb: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    /**
     * Creates or updates a user document in Cloud Firestore and Firebase Realtime Database.
     */
    suspend fun saveUserToFirestore(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val userMap = hashMapOf(
                "userId" to user.userId,
                "name" to user.name,
                "email" to user.email,
                "phone" to user.phone,
                "username" to user.username,
                "role" to user.role,
                "kycStatus" to user.kycStatus,
                "panNumber" to user.panNumber,
                "aadhaarNumber" to user.aadhaarNumber,
                "emailVerified" to user.emailVerified,
                "phoneVerified" to user.phoneVerified,
                "panVerified" to user.panVerified,
                "aadhaarVerified" to user.aadhaarVerified,
                "selfieVerified" to user.selfieVerified,
                "upiId" to user.upiId,
                "bankAccountNumber" to user.bankAccountNumber,
                "profilePhotoUri" to user.profilePhotoUri,
                "panImageUrl" to user.panImageUrl,
                "aadhaarImageUrl" to user.aadhaarImageUrl,
                "dateOfBirth" to user.dateOfBirth,
                "address" to user.address,
                "fcmToken" to user.fcmToken,
                "createdAt" to Timestamp(user.createdAt / 1000, ((user.createdAt % 1000) * 1000000).toInt()),
                "updatedAt" to Timestamp.now(),
                "password" to user.password,
                "app" to "Loanzo Android"
            )

            // 1. Save to primary document by userId
            Tasks.await(
                firestore.collection(USERS_COLLECTION)
                    .document(user.userId)
                    .set(userMap, SetOptions.merge())
            )
            // 3. Sync to Firebase Realtime Database
            try {
                val rtdbKey = if (user.email.isNotBlank()) {
                    user.email.trim().lowercase().replace(".", "_").replace("@", "_at_")
                } else {
                    user.userId
                }
                realtimeDb.getReference("users").child(rtdbKey).setValue(userMap)
            } catch (rtdbEx: Exception) {
                Log.w(TAG, "Realtime DB sync note: ${rtdbEx.message}")
            }

            Log.d(TAG, "Successfully synced user ${user.userId} (${user.email}) to Firestore & RTDB")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user to Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Looks up a user in Cloud Firestore by email, phone, or userId and returns a Result.
     */
    suspend fun fetchUserFromFirestoreResult(loginId: String): Result<UserEntity?> = withContext(Dispatchers.IO) {
        val cleanLoginId = loginId.trim()
        if (cleanLoginId.isBlank()) return@withContext Result.success(null)

        try {
            // 1. Try direct lookup by userId document
            val directDoc = Tasks.await(firestore.collection(USERS_COLLECTION).document(cleanLoginId).get())
            if (directDoc.exists()) {
                return@withContext Result.success(parseUserFromDoc(directDoc.data))
            }
            // 2. Query by email field
            if (cleanLoginId.contains("@")) {
                val queryEmail = Tasks.await(
                    firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("email", cleanLoginId)
                        .limit(1)
                        .get()
                )
                if (!queryEmail.isEmpty) {
                    return@withContext Result.success(parseUserFromDoc(queryEmail.documents[0].data))
                }
            } else {
                // 3. Query by phone field (with and without country code)
                val cleanPhone = cleanLoginId.replace(" ", "").replace("-", "")
                val phoneVariants = listOf(
                    cleanPhone,
                    if (cleanPhone.startsWith("+91")) cleanPhone else "+91$cleanPhone",
                    cleanPhone.removePrefix("+91")
                ).distinct()

                for (phoneVar in phoneVariants) {
                    val queryPhone = Tasks.await(
                        firestore.collection(USERS_COLLECTION)
                            .whereEqualTo("phone", phoneVar)
                            .limit(1)
                            .get()
                    )
                    if (!queryPhone.isEmpty) {
                        return@withContext Result.success(parseUserFromDoc(queryPhone.documents[0].data))
                    }
                }
            }

            // 4. Query by username field (checks both exact casing and lowercase)
            val usernamesToTry = listOf(cleanLoginId, cleanLoginId.lowercase()).distinct()
            for (uname in usernamesToTry) {
                val queryUsername = Tasks.await(
                    firestore.collection(USERS_COLLECTION)
                        .whereEqualTo("username", uname)
                        .limit(1)
                        .get()
                )
                if (!queryUsername.isEmpty) {
                    return@withContext Result.success(parseUserFromDoc(queryUsername.documents[0].data))
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user from Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Looks up a user in Cloud Firestore by email, phone, or userId.
     */
    suspend fun fetchUserFromFirestore(loginId: String): UserEntity? {
        return fetchUserFromFirestoreResult(loginId).getOrNull()
    }

    /**
     * Updates specific fields (e.g. KYC status, PAN, Bank/UPI) in Cloud Firestore.
     */
    suspend fun updateFieldsInFirestore(userId: String, updates: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            val fullUpdates = updates.toMutableMap()
            fullUpdates["updatedAt"] = Timestamp.now()

            Tasks.await(
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .set(fullUpdates, SetOptions.merge())
            )
            Log.d(TAG, "Updated fields for user $userId in Firestore")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating fields in Firestore: ${e.message}", e)
            false
        }
    }

    /**
     * Registers a user in Firebase Authentication.
     */
    suspend fun registerFirebaseAuthUser(email: String, pass: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = Tasks.await(auth.createUserWithEmailAndPassword(email.trim(), pass))
            Result.success(result.user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth registration notice: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Signs in a user with Firebase Authentication.
     */
    suspend fun signInFirebaseAuthUser(email: String, pass: String): Result<FirebaseUser?> = withContext(Dispatchers.IO) {
        try {
            val result = Tasks.await(auth.signInWithEmailAndPassword(email.trim(), pass))
            Result.success(result.user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth sign-in notice: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sends an official Firebase Authentication password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Tasks.await(auth.sendPasswordResetEmail(email.trim()))
            Result.success(true)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth reset email error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Uploads a document to Firebase Storage and returns the download URL.
     */
    suspend fun uploadDocument(uri: Uri, documentType: String, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("kyc_documents")
                .child(userId)
                .child("${documentType}_${System.currentTimeMillis()}.jpg")
            
            Tasks.await(storageRef.putFile(uri))
            val downloadUrl = Tasks.await(storageRef.downloadUrl)
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading document: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a bitmap image to Firebase Storage and returns the download URL.
     */
    suspend fun uploadBitmapImage(bitmap: android.graphics.Bitmap, documentType: String, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
            val data = baos.toByteArray()

            val storageRef = FirebaseStorage.getInstance().reference
                .child("kyc_documents")
                .child(userId)
                .child("${documentType}_${System.currentTimeMillis()}.jpg")
            
            Tasks.await(storageRef.putBytes(data))
            val downloadUrl = Tasks.await(storageRef.downloadUrl)
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading bitmap: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun parseUserFromDoc(data: Map<String, Any>?): UserEntity? {
        if (data == null) return null
        val userId = data["userId"] as? String ?: return null
        val name = data["name"] as? String ?: "User"
        val email = data["email"] as? String ?: ""
        val phone = data["phone"] as? String ?: ""
        val password = data["password"] as? String ?: ""  // Password hash is needed for online verification
        val username = data["username"] as? String ?: ""
        val role = data["role"] as? String ?: "BORROWER"
        val kycStatus = data["kycStatus"] as? String ?: "PENDING"
        val panNumber = data["panNumber"] as? String ?: ""
        val aadhaarNumber = data["aadhaarNumber"] as? String ?: ""
        val emailVerified = data["emailVerified"] as? Boolean ?: false
        val phoneVerified = data["phoneVerified"] as? Boolean ?: false
        val panVerified = data["panVerified"] as? Boolean ?: false
        val aadhaarVerified = data["aadhaarVerified"] as? Boolean ?: false
        val selfieVerified = data["selfieVerified"] as? Boolean ?: false
        val upiId = data["upiId"] as? String ?: ""
        val bankAccountNumber = data["bankAccountNumber"] as? String ?: ""
        val profilePhotoUri = data["profilePhotoUri"] as? String ?: ""
        val panImageUrl = data["panImageUrl"] as? String ?: ""
        val aadhaarImageUrl = data["aadhaarImageUrl"] as? String ?: ""
        val dateOfBirth = data["dateOfBirth"] as? String ?: ""
        val address = data["address"] as? String ?: ""
        val fcmToken = data["fcmToken"] as? String ?: ""
        val createdAt = when (val c = data["createdAt"]) {
            is Timestamp -> c.toDate().time
            is Number -> c.toLong()
            else -> System.currentTimeMillis()
        }

        return UserEntity(
            userId = userId,
            name = name,
            email = email,
            phone = phone,
            username = username,
            password = password,
            role = role,
            kycStatus = kycStatus,
            panNumber = panNumber,
            aadhaarNumber = aadhaarNumber,
            emailVerified = emailVerified,
            phoneVerified = phoneVerified,
            panVerified = panVerified,
            aadhaarVerified = aadhaarVerified,
            selfieVerified = selfieVerified,
            upiId = upiId,
            bankAccountNumber = bankAccountNumber,
            profilePhotoUri = profilePhotoUri,
            panImageUrl = panImageUrl,
            aadhaarImageUrl = aadhaarImageUrl,
            dateOfBirth = dateOfBirth,
            address = address,
            fcmToken = fcmToken,
            createdAt = createdAt
        )
    }
}
