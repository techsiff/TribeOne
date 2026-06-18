package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.siffmember.info.databinding.ActivityAiUserStoryManageBinding
import com.siffmember.info.ui.adapter.UserStoryAdapter
import com.siffmember.info.ui.fragment.DeleteStoryBottomSheet
import com.siffmember.info.ui.fragment.SearchUsersBottomSheetFragment
import com.siffmember.info.ui.model.UploadStoryFile
import com.siffmember.info.ui.model.UserStoryModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UserStory

class AIUserStoryManageActivity : BaseActivity(), UserStoryAdapter.UserStoryListener, DeleteStoryBottomSheet.BottomSheetListener {

    companion object {
        const val TAG = "AIUserStoryManageActivity"
    }

    private lateinit var binding: ActivityAiUserStoryManageBinding
    private lateinit var db: FirebaseFirestore
    private var userStoryList: ArrayList<UserStoryModel> = ArrayList()
    private lateinit var adapter: UserStoryAdapter
    private var selectedUserID = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiUserStoryManageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        binding.blTitle.text = "Manage User Stories"
        setupRecyclerView()
        fetchUserStories()
    }

    private fun setupRecyclerView() {
        adapter = UserStoryAdapter(userStoryList,this)
        binding.meetingUsersRv.layoutManager = LinearLayoutManager(this)
        binding.meetingUsersRv.adapter = adapter
    }

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("NotifyDataSetChanged")
    private fun fetchUserStories() {
      showProgDialog()
        userStoryList.clear()
        db.collection(AppConstants.TABLE_USER_STORIES)
            .get()
            .addOnSuccessListener { snapshot ->
                dismissProgDialog()
                for (document in snapshot.documents) {
                    val data = document.data ?: continue
                    val userName = data["userName"] as? String ?: ""
                    val userId = document.id
                    val questions = data["questions"] as? ArrayList<HashMap<String, Any>>
                            ?: arrayListOf()
                    val stories = ArrayList<UploadStoryFile>()
                    questions.forEach { item ->
                        val story = UploadStoryFile(
                            id = item["id"] as? String ?: "",
                            fileName = item["fileName"] as? String ?: "",
                            storyFile = item["storyFile"] as? String ?: "",
                            mimeType = item["mimeType"] as? String ?: "",
                            timestamp = item["timestamp"].toString()
                        )
                        stories.add(story)
                    }
                    val model = UserStoryModel(
                        userName = userName,
                        userId = userId,
                        storyList = stories
                    )
                    userStoryList.add(model)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Fetch Error: ${it.message}")
            }
    }

    override fun onDeleteUser(userStoryModel: UserStoryModel) {
        AlertDialog.Builder(this)
            .setTitle("Remove User Stories")
            .setMessage("Are you sure you want to remove all stories?")
            .setPositiveButton("Yes") { dialog, _ ->
                deleteUserWithStory(userStoryModel)
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onSelectUser(userStoryModel: UserStoryModel) {
        selectedUserID = userStoryModel.userId
        UserStory.setUserStoryModel(userStoryModel)
        //Toast.makeText(this, "Selected: ${userStoryModel.userName}", Toast.LENGTH_SHORT).show()
        val bottomSheetFragment = DeleteStoryBottomSheet()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    private fun deleteUserWithStory(userStory: UserStoryModel) {
      showProgDialog()
        db.collection(AppConstants.TABLE_USER_STORIES)
            .document(userStory.userId)
            .delete()
            .addOnSuccessListener {
                if (userStory.storyList.isEmpty()) {
                    dismissProgDialog()
                    fetchUserStories()
                    return@addOnSuccessListener
                }
                var deletedCount = 0
                userStory.storyList.forEach { story ->
                    deleteAllStorageFile(userStory.userId, story.fileName) {
                        deletedCount++
                        if (deletedCount == userStory.storyList.size) {
                            dismissProgDialog()
                            fetchUserStories()
                        }
                    }
                }
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Delete Error: ${it.message}")
            }
    }

    private fun deleteAllStorageFile(
        userId: String,
        fileName: String,
        completion: () -> Unit
    ) {

        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("stories/$userId/$fileName")

        fileRef.delete()
            .addOnSuccessListener {
                Log.e(TAG, "Story Deleted")
                completion()
            }
            .addOnFailureListener {
                Log.e(TAG, "Delete Failed: ${it.message}")
                completion()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deleteStory(
        userId: String,
        story: UploadStoryFile
    ) {

      showProgDialog()
        val docRef = db.collection(AppConstants.TABLE_USER_STORIES).document(userId)

        docRef.get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.data
                if (data == null) {
                    dismissProgDialog()
                    return@addOnSuccessListener
                }

                val questions = data["questions"] as? ArrayList<HashMap<String, Any>>
                        ?: arrayListOf()

                questions.removeAll {
                    val id = it["id"] as? String ?: ""
                    id == story.id
                }
                docRef.update("questions", questions)
                    .addOnSuccessListener {
                        deleteStorageFile(story.fileName, userId)
                    }
                    .addOnFailureListener {
                        dismissProgDialog()
                        Log.e(TAG, "Update Failed: ${it.message}")
                    }
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Fetch Failed: ${it.message}")
            }
    }

    private fun deleteStorageFile(
        fileName: String,
        userId: String
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("stories/$userId/$fileName")

        fileRef.delete()
            .addOnSuccessListener {
                dismissProgDialog()
                Toast.makeText(this, "Story deleted successfully", Toast.LENGTH_SHORT).show()
                fetchUserStories()
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Storage Delete Failed: ${it.message}")
            }
    }

    override fun onSelectedQuestion(user: UploadStoryFile) {
        deleteStory(selectedUserID, user)
    }
}