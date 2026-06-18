package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.siffmember.info.databinding.FragmentSettingsBackupBinding
import com.siffmember.info.ui.activity.ProfileDetailsActivity
import com.siffmember.info.ui.backup.BackupScheduler
import com.siffmember.info.ui.backup.DatabaseBackupManager
import com.siffmember.info.ui.backup.NotificationHelper
import com.siffmember.info.ui.backup.core.RoomBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SettingsBackupFragment : BaseFragment() {

    companion object {
        private const val TAG = "SettingsBackupFragment"
        const val SECRET_PASSWORD = "siff3d++"
    }

    private var _binding: FragmentSettingsBackupBinding? = null
    private val binding get() = _binding!!

    private lateinit var manager: DatabaseBackupManager
    //private lateinit var firebaseBackup : FirebaseBackupManager

    private fun fixTreeUri(treeUri: Uri): Uri {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
    }

    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let { folderUri ->
                val fixedUri = fixTreeUri(folderUri)
                // run backup encrypted zip and save to chosen folder
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        requireActivity().runOnUiThread { showProgress(true, "Starting backup...") }
                        manager.backupEncryptedZipToFolder(fixedUri)
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Backup completed", Toast.LENGTH_LONG).show()
                            NotificationHelper.showSuccess(requireActivity(), "Backup", "Local backup created")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                            NotificationHelper.showFailure(requireActivity(), "Backup failed", e.message ?: "error")
                        }
                    }
                }
            }
        }

   /* private val pickRestoreFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            uri?.let { folderUri ->
                val fixedUri = fixTreeUri(folderUri)
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        requireActivity().runOnUiThread { showProgress(true, "Starting restore...") }
                        val encName = findLatestEncFileName(fixedUri)
                            ?: throw IllegalStateException("No .enc backup file found")
                        manager.restoreEncryptedZipFromFolder(fixedUri, encName)
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Restore completed", Toast.LENGTH_LONG).show()
                            NotificationHelper.showSuccess(requireActivity(), "Restore", "Restore completed")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                            NotificationHelper.showFailure(requireActivity(), "Restore failed", e.message ?: "error")
                        }
                    }
                }
            }
        }*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(requireActivity())
        manager = DatabaseBackupManager(requireActivity())
      //  firebaseBackup = FirebaseBackupManager(requireActivity())
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBackupBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val fragmentActivity = (activity as ProfileDetailsActivity)
        val backup = fragmentActivity.backup
        binding.apply {

            var encryptBackup = true
            var storageLocation =  RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG
            var enableLog = true
            var useMaxFileCount = false

            btnBackupNow.setOnClickListener {
                pickFolder.launch(null)

                /*backup.backupLocation(storageLocation)
                    .backupLocationCustomFile(
                        File("${root.context.filesDir}/databasebackup/tribeOneBackup.sqlite3")
                    )
                    .database(AppDatabase.getDatabase(root.context))
                    .enableLogDebug(enableLog)
                    .backupIsEncrypted(encryptBackup)
                    .customEncryptPassword(SECRET_PASSWORD)
                    // maxFileCount: else 1000 because i cannot surround it with if condition
                    .maxFileCount(if (useMaxFileCount) 5 else 1000)
                    .apply {
                        onCompleteListener { success, message, exitCode ->
                            Log.d(TAG, "success: $success, message: $message, exitCode: $exitCode")
                            Toast.makeText(
                                root.context,
                                "success: $success, message: $message, exitCode: $exitCode",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            if (success)
                                restartApp(Intent(root.context, ProfileDetailsActivity::class.java))
                        }
                    }
                    .backup()*/
            }

            btnRestoreNow.setOnClickListener {
                //pickRestoreFolder.launch(null)
               /* backup.backupLocation(storageLocation)
                    .backupLocationCustomFile(
                        File("${root.context.filesDir}/databasebackup/tribeOneBackup.sqlite3")
                    )
                    .database(AppDatabase.getDatabase(root.context))
                    .enableLogDebug(enableLog)
                    .backupIsEncrypted(encryptBackup)
                    .customEncryptPassword(SECRET_PASSWORD)
                    .apply {
                        onCompleteListener { success, message, exitCode ->
                            Log.d(TAG, "success: $success, message: $message, exitCode: $exitCode")
                            Toast.makeText(
                                root.context,
                                "success: $success, message: $message, exitCode: $exitCode",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            if (success)
                                restartApp(Intent(root.context, ProfileDetailsActivity::class.java))
                        }
                    }
                    .restore()*/

            }

            switchWeekly.setOnCheckedChangeListener { _, checked ->
                if (checked) BackupScheduler.scheduleWeekly(requireActivity())
                else BackupScheduler.cancelWeekly(requireActivity())
            }

            switchMonthly.setOnCheckedChangeListener { _, checked ->
                if (checked) BackupScheduler.scheduleMonthly(requireActivity())
                else BackupScheduler.cancelMonthly(requireActivity())
            }

            btnBackupDrive.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        requireActivity().runOnUiThread { showProgress(true, "Creating encrypted backup...") }
                        val zipName = "tribeOneBackup.enc"
                        File( requireActivity().cacheDir, zipName)
                        val uri = Uri.fromFile( requireActivity().cacheDir)
                        manager.backupEncryptedZipToFolder(uri) // writes to cache
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Drive backup created (see logs).", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText(requireActivity(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            btnBackupFirebase.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        requireActivity().runOnUiThread {
                            showProgress(true, "Creating encrypted backup for Firebase...")
                        }
                        val zipName = "tribeOneBackup.enc"
                        File( requireActivity().cacheDir, zipName)
                        val uri = Uri.fromFile( requireActivity().cacheDir)
                        val fixedUri = fixTreeUri(uri)
                        lifecycleScope.launch {
                            val temp = File(requireContext().cacheDir, "tribeOneBackup.zip")
                            manager.backupEncryptedZipToFolder(fixedUri) // will create .enc in cache
                          //  firebaseBackup.uploadBackup(temp, "tribeOneBackup.enc")
                        }
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText( requireActivity(), "Firebase backup created (see logs).", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG,"${e.message}")
                        requireActivity().runOnUiThread {
                            showProgress(false)
                            Toast.makeText( requireActivity(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }


    private fun showProgress(show: Boolean, text: String = "") {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvProgress.text = text
    }

    // find newest .enc file in folder (simple implementation)
    private fun findLatestEncFileName(folderUri: Uri): String? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri, DocumentsContract.getDocumentId(folderUri)
        )
        val cursor = requireActivity().contentResolver.query(childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null, null, "lastModified DESC")
            ?: return null
        cursor.use {
            var best: String? = null
            while (it.moveToNext()) {
                val name = it.getString(1)
                if (name.endsWith(".enc")) {
                    best = name
                    break
                }
            }
            return best
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}