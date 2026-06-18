package com.siffmember.info.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.FirebaseStorage
import com.siffmember.info.databinding.ActivityAiUserStoryCollectBinding
import com.siffmember.info.ui.fragment.DeleteStoryBottomSheet
import com.siffmember.info.ui.fragment.SearchUsersBottomSheetFragment
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.ui.model.UploadStoryFile
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UserStory
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.net.URL

class AIUserStoryCollectActivity : BaseActivity(), SearchUsersBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AIUserStoryCollectActivity"
    }

    private lateinit var binding: ActivityAiUserStoryCollectBinding
    private lateinit var db: FirebaseFirestore

    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    private val selectedFiles = mutableListOf<Uri>()
    private var base64File = ""
    private var mime = ""
    private var prompt = ""
    private val questionMap = HashMap<String, List<String>>()
    private var previousStoryList: ArrayList<UploadStoryFile> = ArrayList()
    private var userId = ""
    private var selectedUserID = ""
    private var selectedUserName = "Select User"

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiUserStoryCollectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnProceedLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        userId = sharedPref.getString(AppConstants.USER_ID, "")!!
        binding.selectedUserName.text = selectedUserName
        binding.oldStoryLL.visibility = View.GONE
        fetchAllQuestionsWithMap { map ->
            val keys = map.keys.toMutableList()
            keys.add(0, "Select Question")
            setupQuestionSpinner(keys)
        }

        fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                // MULTIPLE FILES
                data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        selectedFiles.add(uri)
                        processFile(uri)
                    }
                }
                // SINGLE FILE
                data?.data?.let { uri ->
                    selectedFiles.add(uri)
                    processFile(uri)
                }
               // showPreview()
            }
        }

        binding.spinnerOldStory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                Log.e(TAG, "Selected: $position")
                if (position == 0) {
                    return
                }
                val selectedStory = parent.getItemAtPosition(position) as UploadStoryFile
                Log.e(TAG, "Selected: ${selectedStory.fileName}")
                downloadFile( selectedStory.storyFile) { bytes ->
                    bytes?.let {
                        runOnUiThread {
                            val base64 = convertToBase64(it)
                            base64File = base64
                            mime = selectedStory.mimeType
                            binding.storyFileName.text = ""

                            binding.spinnerQuestion.setSelection(0, false) // 🔥 fix
                        }
                    }
                }
                // Use questions (e.g., show in RecyclerView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                //
            }
        }

        binding.spinnerQuestion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                Log.e(TAG, "Selected: $position")
                if (position == 0) {
                    return
                }
                val selectedKey = parent.getItemAtPosition(position) as String
                val questions = questionMap[selectedKey] ?: emptyList()
                Log.e(TAG, "Selected: $selectedKey")
                Log.e(TAG, "Questions: ${questions.size}")
                prompt = questions.joinToString()
                // Use questions (e.g., show in RecyclerView)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                //
            }
        }

        binding.btnUploadFile.setOnClickListener {
            binding.spinnerOldStory.setSelection(0)
            openFilePicker()
        }

        binding.btnProceed.setOnClickListener {
            if(validate()){
                UserStory.setQuestions(prompt)
                UserStory.setStoryFile(base64File)
                UserStory.setMime(mime)
                binding.storyFileName.text = ""
                startActivity(Intent(this@AIUserStoryCollectActivity, AIUserStoryCollectedActivity::class.java))
            }
        }

        binding.selectUserRL.setOnClickListener {
            val bottomSheetFragment = SearchUsersBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("adminId", userId)
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
    }
    private fun validate(): Boolean{
        if(selectedUserName == "Select User"){
            Toast.makeText(this@AIUserStoryCollectActivity,"Select one user", Toast.LENGTH_LONG).show()
            return false
        }
        if(prompt.isEmpty()){
            Toast.makeText(this@AIUserStoryCollectActivity,"Select one questions", Toast.LENGTH_LONG).show()
            return false
        }
        if(base64File.isEmpty()){
            Toast.makeText(this@AIUserStoryCollectActivity,"Upload file or select previous file", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }
    @Suppress("UNCHECKED_CAST")
    fun fetchAllQuestionsWithMap(onResult: (Map<String, List<String>>) -> Unit) {
        showProgDialog()
        db.collection(AppConstants.TABLE_QUESTIONNAIRE_DETAILS)
            .get()
            .addOnSuccessListener { snapshot ->
                val resultMap = HashMap<String, List<String>>()
                for (doc in snapshot.documents) {
                    val docId = doc.id
                    val questions = doc.get("questions") as? List<String> ?: emptyList()
                    resultMap[docId] = questions
                }
                questionMap.clear()
                questionMap.putAll(resultMap)
                onResult(resultMap)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching data: ${it.message}")
                onResult(emptyMap())
                dismissProgDialog()
            }
    }

    private fun setupQuestionSpinner(keys: List<String>) {
        val adapter = ArrayAdapter(this, com.siffmember.info.R.layout.spinner_list, keys)
        adapter.setDropDownViewResource(com.siffmember.info.R.layout.spinner_dropdown_item)
        binding.spinnerQuestion.adapter = adapter
    }

    private fun setupStorySpinner(stories: List<UploadStoryFile>) {
        val adapter = ArrayAdapter(this, com.siffmember.info.R.layout.spinner_list, stories)
        adapter.setDropDownViewResource(com.siffmember.info.R.layout.spinner_dropdown_item)
        binding.spinnerOldStory.adapter = adapter
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        }
        fileSelectorLauncher.launch(intent)
    }

    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream!!.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    fun processFile(uri: Uri) {
        mime = getMimeType(this, uri)
        val fileName = getFileName(this, uri)
        Log.e(TAG, "lastPathSegment: $fileName")
        binding.storyFileName.text = fileName
        when (mime) {
            "application/pdf" -> {
                base64File = uriToBase64(this, uri)
                Log.e(TAG, "base64File: $base64File")
                Log.e(TAG, "mime: $mime")
                showProgDialog()
                uploadFile(fileName, uri) { fileUrl ->
                    addStoryFile(fileName, fileUrl, mime)
                }
            }
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                base64File = readDocxFromUri(this@AIUserStoryCollectActivity, uri)
                showProgDialog()
                uploadFile(fileName, uri) { fileUrl ->
                    addStoryFile(fileName, fileUrl, mime)
                }
            }
        }
    }

    fun readDocxFromUri(context: Context, uri: Uri): String {
        val text = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val document = XWPFDocument(inputStream)
            for (para in document.paragraphs) {
                text.append(para.text).append("\n")
            }
            document.close()
        }
        return text.toString()
    }

    fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                name = it.getString(nameIndex)
            }
        }
        return name
    }
    fun convertToBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
    fun downloadFile(fileUrl: String, onResult: (ByteArray?) -> Unit) {
        Thread {
            try {
                val url = URL(fileUrl)
                val connection = url.openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                val bytes = inputStream.readBytes()
                onResult(bytes)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }.start()
    }

    private fun uploadFile(fileName: String, fileUri: Uri, onResult: (String) -> Unit){
        val fileRef = FirebaseStorage.getInstance().reference
            .child("stories/${selectedUserID}/${fileName.trim()}")
        fileRef.putFile(fileUri)
            .continueWithTask {
                fileRef.downloadUrl
            }
            .addOnSuccessListener { uri ->
                val fileUrl = uri.toString()
                onResult(fileUrl)

            }
            .addOnFailureListener {
                onResult("")
                dismissProgDialog()
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addStoryFile(
        fileName: String,
        storyFile: String,
        mimeType: String
    ) {
        val docRef = db.collection(AppConstants.TABLE_USER_STORIES)
            .document(selectedUserID)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                val existingQuestions = snapshot.get("questions")
                            as? ArrayList<HashMap<String, Any>>
                        ?: arrayListOf()
                // Check duplicate file name
                val isFileAlreadyExists = existingQuestions.any {
                    val existingFileName = it["fileName"] as? String ?: ""
                    existingFileName.equals(
                        fileName,
                        ignoreCase = true
                    )
                }

                if (isFileAlreadyExists) {
                    dismissProgDialog()
                    Log.e("addStoryFile", "File already exists: $fileName")
                    Toast.makeText(this@AIUserStoryCollectActivity,"File already exists: $fileName", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val storyID = java.util.UUID.randomUUID().toString()
                val params = hashMapOf(
                    "id" to storyID,
                    "fileName" to fileName,
                    "storyFile" to storyFile,
                    "mimeType" to mimeType,
                    "timestamp" to System.currentTimeMillis().toString()
                )
                val data = hashMapOf(
                    "userName" to selectedUserName,
                    "userId" to selectedUserID,
                    "questions" to com.google.firebase.firestore.FieldValue.arrayUnion(params)
                )

                docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        dismissProgDialog()
                        Log.e("addStoryFile", "Added StoryFile $fileName")
                        Toast.makeText(this@AIUserStoryCollectActivity,"Added StoryFile $fileName", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        dismissProgDialog()
                        Log.e("addStoryFile", "Error: ${it.localizedMessage}")
                    }
            }
            .addOnFailureListener {
                Log.e("addStoryFile", "Fetch Error: ${it.localizedMessage}")
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun fetchAllStoryFile(onResult: (List<UploadStoryFile>) -> Unit) {

        db.collection(AppConstants.TABLE_USER_STORIES)
            .document(selectedUserID)
            .get()
            .addOnSuccessListener { snapshot ->
                val data = snapshot.data
                val tempStories = ArrayList<UploadStoryFile>()
                if (data == null) {
                    onResult(tempStories)
                    return@addOnSuccessListener
                }
                val questions = data["questions"] as? ArrayList<HashMap<String, Any>>
                        ?: arrayListOf()
                questions.forEach { item ->
                    val story = UploadStoryFile(
                        id = item["id"] as? String ?: "",
                        fileName = item["fileName"] as? String ?: "",
                        storyFile = item["storyFile"] as? String ?: "",
                        mimeType = item["mimeType"] as? String ?: "",
                        timestamp = item["timestamp"].toString()
                    )
                    tempStories.add(story)
                }
                onResult(tempStories)
            }
            .addOnFailureListener {
                Log.e("fetchAllStoryFiles", "Fetch Error: ${it.localizedMessage}")
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSelectedUser(user: GetUsers) {
        selectedUserName = user.name!!
        selectedUserID = user.phone_number!!
        binding.selectedUserName.text = user.name
        fetchAllStoryFile { stories ->
            previousStoryList.clear()
            if(stories.isEmpty()){
                binding.oldStoryLL.visibility = View.GONE
            } else {
                binding.oldStoryLL.visibility = View.VISIBLE
                val updatedList = stories.toMutableList()
                updatedList.add(0, UploadStoryFile("","Select Previous Story","","",""))
                previousStoryList.addAll(updatedList)
                setupStorySpinner(updatedList)
            }
        }
    }
}