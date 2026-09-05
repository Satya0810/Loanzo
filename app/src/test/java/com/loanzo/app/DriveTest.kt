package com.loanzo.app

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import org.junit.Test
import java.io.FileInputStream
import java.io.File as JavaFile

class DriveTest {

    @Test
    fun testDriveUpload() {
        try {
            val transport = NetHttpTransport()
            val jsonFactory = GsonFactory.getDefaultInstance()

            val serviceAccountPath = "src/main/res/raw/service_account.json"
            val credential = GoogleCredential.fromStream(
                FileInputStream(JavaFile(serviceAccountPath))
            ).createScoped(listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE))

            val driveService = Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("Loanzo Test")
                .build()

            val fileMetadata = File()
            fileMetadata.name = "test_upload.txt"
            fileMetadata.parents = listOf("1IxUB_MXirxDGgoeZnAQXvSvwYVSXdvtD")

            val mediaContent = ByteArrayContent("text/plain", "hello world".toByteArray())

            println("Attempting to upload...")
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute()

            println("Success! File ID: ${file.id}")

        } catch (e: Exception) {
            println("FAILED WITH ERROR:")
            e.printStackTrace()
            if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                println("JSON Error: " + e.details.toPrettyString())
            }
        }
    }
}
