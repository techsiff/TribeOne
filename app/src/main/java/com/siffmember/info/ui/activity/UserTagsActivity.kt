package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityUserBlockBinding
import com.siffmember.info.ui.adapter.UserBlockAdapter
import com.siffmember.info.ui.fragment.AddTagsBottomSheetFragment
import com.siffmember.info.utils.AppConstants
import kotlin.collections.mapOf

class UserTagsActivity : BaseActivity(), UserBlockAdapter.UserBlockingListener, AddTagsBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "UserTagsActivity"
    }
    private lateinit var binding: ActivityUserBlockBinding
    private lateinit var db: FirebaseFirestore
    private var tagsList: ArrayList<String> = ArrayList()
    private var recyclerViewAdapter: UserBlockAdapter? = null
    private var adminName = ""
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
        adminName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        binding.btnAddTags.visibility = View.VISIBLE
        binding.blListTitle.text = "Tags"
        db = Firebase.firestore
        setupAdapter()
        binding.btnAddTags.setOnClickListener {
            val bottomSheetFragment = AddTagsBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("addType", "1")
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
    }

    override fun onStart() {
        super.onStart()
        fetchAllTagsList { historyList ->
            tagsList.clear()
            tagsList.addAll(historyList)
            //meetingsList.sortByDescending { it }
            recyclerViewAdapter!!.updateList(tagsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = UserBlockAdapter(tagsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchAllTagsList(onResult: (List<String>) -> Unit) {
        showProgDialog()
        db.collection(AppConstants.TABLE_USER_TAGS)
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
        val next = Intent(this@UserTagsActivity, UserTagDetailsActivity::class.java)
        next.putExtra("tagListTitle", blockTopic)
        startActivity(next)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onAddTagName(tagName: String) {
        addTags(tagName){ isAdded ->
            if (isAdded){
                val next = Intent(this@UserTagsActivity, UserTagDetailsActivity::class.java)
                next.putExtra("tagListTitle", tagName)
                startActivity(next)
            }
        }
    }

    private fun addTags(tagName: String, onComplete: (Boolean) -> Unit){
           try{
               showProgDialog()
               db.collection(AppConstants.TABLE_USER_TAGS)
                   .document(tagName)
                   .set(mapOf("createdBy" to adminName))
                   .addOnSuccessListener {
                       Log.e(TAG, "DocumentSnapshot successfully written!")
                       dismissProgDialog()
                       onComplete(true)
                   }
                   .addOnFailureListener { e ->
                       Log.e(TAG, "Error writing document", e)
                       Toast.makeText(this@UserTagsActivity,"Failed to create tag",Toast.LENGTH_SHORT).show()
                       dismissProgDialog()
                       onComplete(false)
                   }
           }catch (e: Exception){
               dismissProgDialog()
               e.printStackTrace()
               onComplete(false)
           }
       }
}