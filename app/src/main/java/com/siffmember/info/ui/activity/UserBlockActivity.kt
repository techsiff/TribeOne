package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityUserBlockBinding
import com.siffmember.info.ui.adapter.UserBlockAdapter
import com.siffmember.info.utils.AppConstants

class UserBlockActivity : BaseActivity(), UserBlockAdapter.UserBlockingListener {

    companion object {
        var TAG = "UserBlockActivity"
    }
    private lateinit var binding: ActivityUserBlockBinding
    private lateinit var db: FirebaseFirestore
    private var meetingsList: ArrayList<String> = ArrayList()
    private var recyclerViewAdapter: UserBlockAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBlockBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        setupAdapter()
    }

    override fun onStart() {
        super.onStart()
        fetchAllBlockingList { historyList ->
            meetingsList.clear()
            meetingsList.addAll(historyList)
            //meetingsList.sortByDescending { it }
            recyclerViewAdapter!!.updateList(meetingsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = UserBlockAdapter(meetingsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchAllBlockingList(onResult: (List<String>) -> Unit) {
        showProgDialog()
        db.collection(AppConstants.TABLE_USER_BLOCK)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.e(TAG, "All meetings: ${snapshot.documents.size}")
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.id
                }
                onResult(result)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching meetings: ${it.message}")
                onResult(emptyList())
                dismissProgDialog()
            }
    }


    override fun onUserBlock(blockTopic: String?) {
        val next = Intent(this@UserBlockActivity, UserBlockListDetailsActivity::class.java)
        next.putExtra("blockListTitle", blockTopic)
        startActivity(next)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}