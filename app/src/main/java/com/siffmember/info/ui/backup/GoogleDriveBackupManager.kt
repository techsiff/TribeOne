package com.siffmember.info.ui.backup

/*
import android.content.Context
import android.net.Uri
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream

class GoogleDriveBackupManager(private val context: Context) {

    private suspend fun createDriveService(): Drive = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
            ?: throw Exception("User not signed in to Google")

        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf("https://www.googleapis.com/auth/drive.file")
        )
        credential.selectedAccount = account.account

        Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        ).setApplicationName("Siff Backup")
            .build()
    }

    */
/** Upload file to Google Drive *//*

    suspend fun uploadToDrive(localFile: java.io.File, fileName: String) =
        withContext(Dispatchers.IO) {
            val drive = createDriveService()

            val metadata = File().apply {
                name = fileName
                mimeType = "application/octet-stream"
            }

            val contentStream = FileInputStream(localFile)

            drive.files().create(metadata, com.google.api.client.http.InputStreamContent(
                "application/octet-stream", contentStream
            )).setFields("id").execute()
        }
}
*/
