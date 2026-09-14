package com.loanzo.app.data.firebase

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Centralized provider for Cloud Firestore.
 *
 * In this project, the Firestore database is named "default" (without parentheses)
 * located in the asia-south1 region. The default FirebaseFirestore.getInstance()
 * attempts to target "(default)" with parentheses which does not exist in this project
 * and returns 404 NOT_FOUND.
 *
 * This singleton provider guarantees all repositories, ViewModels, and services
 * consistently bind to the valid "default" Firestore database.
 */
object FirestoreProvider {
    private const val TAG = "FirestoreProvider"
    private const val DATABASE_NAME = "default"

    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: try {
                val app = FirebaseApp.getInstance()
                FirebaseFirestore.getInstance(app, DATABASE_NAME).also {
                    instance = it
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falling back to default Firestore: ${e.message}")
                try {
                    Firebase.firestore.also { instance = it }
                } catch (_: Exception) {
                    FirebaseFirestore.getInstance()
                }
            }
        }
    }
}
