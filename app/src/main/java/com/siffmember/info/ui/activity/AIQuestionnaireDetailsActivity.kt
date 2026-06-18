package com.siffmember.info.ui.activity

import android.app.AlertDialog
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAiQuestionsDetailsBinding
import com.siffmember.info.ui.adapter.BlockedUserAdapter
import com.siffmember.info.ui.fragment.AddTagsBottomSheetFragment
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails

class AIQuestionnaireDetailsActivity : BaseActivity(), BlockedUserAdapter.CommunityUserGroupListener, AddTagsBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AIQuestionnaireDetailsActivity"
    }

    private lateinit var binding: ActivityAiQuestionsDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var hostName = ""
    private var hostID = ""
    private var questionnaireName = ""

    private var userAdapter: BlockedUserAdapter? = null
    private var questionList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiQuestionsDetailsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        setupAdapter()
        binding.btnDeleteMeeting.visibility =
            if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE
        db = Firebase.firestore
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                questionnaireName = vBundle.getString("questionList", null)
            }
            binding.blTitle.text = questionnaireName
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.btnDeleteMeeting.setOnClickListener {
            deleteBlockedListDialog()
        }

        binding.btnAddQuestion.setOnClickListener {
            val bottomSheetFragment = AddTagsBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("addType", "3")
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
        fetchQuestions()
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchQuestions(){
        try{
            showProgDialog()
            db.collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS).document(questionnaireName)
                .get()
                .addOnSuccessListener { snapshot ->
                    val result = mutableListOf<String>()

                    val array = snapshot.get("questions") as? List<String>
                    array?.let { result.addAll(it) }

                    Log.e(TAG, "questions users count: ${result.size}")
                    questionList = result as ArrayList<String>
                    setupAdapter()
                    dismissProgDialog()
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error fetching meetings: ${it.message}")
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun setupAdapter(){
        try{
            userAdapter = BlockedUserAdapter(questionList, this)
            binding.meetingUsersRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.meetingUsersRv.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun removeQuestionFromList(meetingId: String, memberId: String, onComplete: (Boolean) -> Unit) {
        val meetingRef = db
            .collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS)
            .document(meetingId)

        meetingRef.update(
            "questions",
            FieldValue.arrayRemove(memberId)
        )
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to remove member: ${it.message}")
                onComplete(false)
                dismissProgDialog()
            }
    }

    override fun onUserSelectListener(users: String) {
        deleteMemberDialog(users)
    }

    private fun deleteMemberDialog(users: String){
        try{
            AlertDialog.Builder(this)
                .setTitle("Remove Question Alert")
                .setMessage("Are you sure you want to remove this $users question?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    showProgDialog()
                    removeQuestionFromList(questionnaireName, users) {
                        if(it){
                            Toast.makeText(this, "Question removed successfully", Toast.LENGTH_SHORT).show()
                            MeetingUserDetails.removeUserById(users)
                            fetchQuestions()
                        } else {
                            Toast.makeText(this, "Failed to remove question, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
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

    override fun onAddTagName(tagName: String) {
        showProgDialog()
        val meetingRef = db
            .collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS)
            .document(questionnaireName)
        meetingRef.update(
            "questions",
            FieldValue.arrayUnion(tagName)
        )
            .addOnSuccessListener {
                fetchQuestions()
                Log.e(TAG, "Members added successfully")
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Failed to add members: ${it.message}")
                Toast.makeText(this, "Failed to add questions", Toast.LENGTH_SHORT).show()

            }
    }

    private fun deleteBlockedListDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Questions List Alert")
                .setMessage("Are you sure you want to delete this question list?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteBlockListFireStore(questionnaireName) { isDeleted ->
                        if (isDeleted) {
                            finish()
                            dialogInterface.dismiss()
                        }
                    }
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteBlockListFireStore(meetingId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS).document(meetingId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user details", e)
                onComplete(false)
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