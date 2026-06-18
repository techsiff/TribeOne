package com.siffmember.info.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.utils.AppConstants
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.databinding.ActivityCallHistoryBinding
import com.siffmember.info.ui.adapter.CallHistoryAdapter
import com.siffmember.info.ui.model.UpdateCallLog

class UserCallHistoryActivity : BaseActivity() {

    companion object {
        var TAG = "UserCallHistoryActivity"
    }

    private lateinit var binding: ActivityCallHistoryBinding
    private lateinit var db: FirebaseFirestore

    private var recyclerViewAdapter: CallHistoryAdapter? = null
    private var callHistoryList: ArrayList<UpdateCallLog> = ArrayList()
    private var userName: String = ""
    private var userID: String = ""
    private var screen: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.contentLayout.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                userName = vBundle.getString("contact_name", null)
                userID = vBundle.getString("contact_number", null)
                screen = vBundle.getString("screen", null)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.apply {
            contactName.text = userName
            if(screen == "1") {
                binding.deleteContact.visibility =
                    if (sharedPref.getBoolean(
                            AppConstants.IS_ADMIN,
                            false
                        )
                    ) View.VISIBLE else View.GONE
            } else {
                binding.deleteContact.visibility = View.GONE
            }
            deleteContact.setOnClickListener {
                deleteContactus()
            }
        }

        if(userID.isNotEmpty()) {
            fetchAllCallHistory(userID) { historyList ->
                callHistoryList.clear()
                callHistoryList.addAll(historyList)
                if (callHistoryList.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.callHistoryList.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    binding.callHistoryList.visibility = View.VISIBLE
                    setupAdapter()
                }
            }
        }
    }

    private fun setupAdapter(){
        val sortedList = callHistoryList.sortedByDescending { it.timestamp }
        val layoutManager = LinearLayoutManager(this)
        binding.callHistoryList.layoutManager = layoutManager
        recyclerViewAdapter = CallHistoryAdapter(sortedList)
        binding.callHistoryList.adapter = recyclerViewAdapter
    }

    fun fetchAllCallHistory(userId: String, onResult: (List<UpdateCallLog>) -> Unit) {
        val postsRef = db.collection(AppConstants.TABLE_CALL_HISTORY_DETAILS).document(userId).collection(
            AppConstants.TABLE_HISTORY)
        postsRef.orderBy("timestamp").get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Read Firestore data safely
                        val model = doc.toObject(UpdateCallLog::class.java) ?: return@mapNotNull null
                        UpdateCallLog(
                            name = model.name?: "",
                            phoneNumber = model.phoneNumber?: "",
                            type = model.type?: "0",
                            duration = model.duration?: "0",
                            timestamp = model.timestamp?: "0"
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    private fun deleteContactus() {
        try{
            AlertDialog.Builder(this)
                .setTitle("Delete Contact us Alert")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteContact()
                    dialogInterface.dismiss()
                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteContact(){
        showProgDialog()
        db.collection(AppConstants.TABLE_CONTACT_US_DETAILS).document(userID)
            .delete()
            .addOnSuccessListener {
                Log.e(TAG, "DocumentSnapshot successfully deleted!")
                Toast.makeText(this@UserCallHistoryActivity,"Contact deleted successfully", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
                Toast.makeText(this@UserCallHistoryActivity,"Contact deleting failed try again!", Toast.LENGTH_LONG).show()
                dismissProgDialog()
            }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}