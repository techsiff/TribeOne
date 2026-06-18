package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAiUserStoryQuestionnaireBinding
import com.siffmember.info.ui.adapter.UserBlockAdapter
import com.siffmember.info.ui.fragment.AddTagsBottomSheetFragment
import com.siffmember.info.utils.AppConstants

class AIQuestionnaireActivity : BaseActivity(), UserBlockAdapter.UserBlockingListener, AddTagsBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AIUserStoryCollectActivity"
    }
    private lateinit var binding: ActivityAiUserStoryQuestionnaireBinding
    private lateinit var db: FirebaseFirestore
    private var questionsList: ArrayList<String> = ArrayList()
    private var recyclerViewAdapter: UserBlockAdapter? = null
    private var adminName = ""
    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiUserStoryQuestionnaireBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        adminName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        db = Firebase.firestore
        setupAdapter()
        binding.btnAddQuestion.setOnClickListener {
            val bottomSheetFragment = AddTagsBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("addType", "2")
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
    }

    override fun onStart() {
        super.onStart()
        fetchAllQuestionList { historyList ->
            questionsList.clear()
            questionsList.addAll(historyList)
            recyclerViewAdapter!!.updateList(questionsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.questRv.layoutManager = layoutManager
        recyclerViewAdapter = UserBlockAdapter(questionsList, this)
        binding.questRv.adapter = recyclerViewAdapter
    }

    fun fetchAllQuestionList(onResult: (List<String>) -> Unit) {
        showProgDialog()
        db.collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS)
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
        val next = Intent(this@AIQuestionnaireActivity, AIQuestionnaireDetailsActivity::class.java)
        next.putExtra("questionList", blockTopic)
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
                val next = Intent(this@AIQuestionnaireActivity, AIQuestionnaireDetailsActivity::class.java)
                next.putExtra("questionList", tagName)
                startActivity(next)
            }
        }
    }

    private fun addTags(tagName: String, onComplete: (Boolean) -> Unit){
        try{
            showProgDialog()
            db.collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS)
                .document(tagName)
                .set(mapOf("createdBy" to adminName))
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                    dismissProgDialog()
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@AIQuestionnaireActivity,"Failed to add questionnaire",Toast.LENGTH_SHORT).show()
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