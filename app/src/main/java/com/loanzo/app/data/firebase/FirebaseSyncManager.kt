package com.loanzo.app.data.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loanzo.app.data.entity.SyncQueueEntity
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSyncManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "FirebaseSyncManager"
    }

    suspend fun syncEntity(item: SyncQueueEntity) {
        val collectionName = getCollectionName(item.entityType)
        
        when (item.operation) {
            "CREATE", "UPDATE" -> {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val data: Map<String, Any> = gson.fromJson(item.payload, type)
                
                Log.d(TAG, "Uploading ${item.entityType}/${item.entityId} to $collectionName")
                firestore.collection(collectionName)
                    .document(item.entityId)
                    .set(data, SetOptions.merge())
                    .await()
            }
            "DELETE" -> {
                Log.d(TAG, "Deleting ${item.entityType}/${item.entityId} from $collectionName")
                firestore.collection(collectionName)
                    .document(item.entityId)
                    .delete()
                    .await()
            }
            else -> {
                Log.w(TAG, "Unknown operation: ${item.operation}")
            }
        }
    }

    private fun getCollectionName(entityType: String): String {
        return when (entityType) {
            "LOAN" -> "loans"
            "DISBURSEMENT" -> "disbursements"
            "REPAYMENT" -> "repayments"
            "PLEDGE" -> "pledges"
            "USER" -> "users"
            "AGREEMENT" -> "agreements"
            else -> entityType.lowercase() + "s"
        }
    }
}
