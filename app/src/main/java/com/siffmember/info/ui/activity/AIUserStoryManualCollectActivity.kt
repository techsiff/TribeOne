package com.siffmember.info.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.siffmember.info.BuildConfig
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceAI
import com.siffmember.info.data.remote.model.geminiAI.GeminiApi
import com.siffmember.info.databinding.ActivityChatBinding
import com.siffmember.info.ui.adapter.ChatAdapter
import com.siffmember.info.ui.repository.GeminiRepository
import com.siffmember.info.ui.viewmodel.ChatViewModel
import java.io.File

class AIUserStoryManualCollectActivity : BaseActivity() {

    companion object {
        var TAG = "AIUserStoryManualCollectActivity"
    }
    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    private var base64File = ""
    private var mime = ""
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    private val selectedFiles = mutableListOf<Uri>()
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var cameraImageUri: Uri? = null

    private var currentQuestionIndex = 0
    private var retryCount = 0
    private val MAX_RETRY = 5

    private val answersMap = mutableMapOf<String, String>()

    private val questions = listOf(
        "What is your name?",
        "What is your age?",
        "What is your date of birth?",
        "What is your phone number?",
        "What is your email?",
        "What is your problem?"
    )

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
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
            binding.askGeminiLL.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val retrofit = RetrofitInstanceAI.getClient()
        val api = retrofit.create(GeminiApi::class.java)
        val repository = GeminiRepository(api)
       /* val model = Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel("gemini-3.1-pro-preview")*/
       // Log.e(TAG, "apiKey: $apiKey")

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(repository) as T
            }
        })[ChatViewModel::class.java]

        adapter = ChatAdapter(mutableListOf())
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = adapter

        viewModel.messages.observe(this) { messages ->
            adapter = ChatAdapter(messages)
            binding.chatRecyclerView.adapter = adapter
            binding.chatRecyclerView.scrollToPosition(messages.size - 1)
        }

       /* binding.sendButton.setOnClickListener {
            val text = binding.inputMessage.text.toString()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text.trimEnd(), base64File, mime, apiKey)
                //viewModel.generate(text.trimEnd())
                binding.inputMessage.setText("")
                selectedFiles.clear()
                binding.filePreviewContainer.visibility = View.GONE
            }
        }*/

        binding.sendButton.setOnClickListener {

            val answer = binding.inputMessage.text.toString().trim()
            if (answer.isEmpty()) return@setOnClickListener

            if(selectedFiles.isEmpty()){
                val question = questions[currentQuestionIndex]

                viewModel.addUserMessage(answer)
                binding.inputMessage.setText("")

                // 🔥 Call AI validation
                viewModel.validateAnswer(question, answer, apiKey) { isValid ->
                    runOnUiThread {
                        if (isValid) {
                            answersMap[question] = answer
                            retryCount = 0
                            currentQuestionIndex++
                            askNextQuestion()
                        } else {
                            retryCount++
                            viewModel.addBotMessage("Invalid answer. Please try again.")
                            if (retryCount < MAX_RETRY) {
                                viewModel.addBotMessage(question)
                            } else {
                                viewModel.addBotMessage("Skipping this question...")
                                retryCount = 0
                                currentQuestionIndex++
                                askNextQuestion()
                            }
                        }
                    }
                }
            } else {
                viewModel.sendMessage(answer, base64File, mime, apiKey)
                //viewModel.generate(text.trimEnd())
                binding.inputMessage.setText("")
                selectedFiles.clear()
                binding.filePreviewContainer.visibility = View.GONE
            }


        }
     //   binding.addFileButton.visibility = View.GONE

        binding.addFileButton.setOnClickListener {
            openFilePicker()
            //openCamera()
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
                showPreview()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success && cameraImageUri != null) {
                    selectedFiles.add(cameraImageUri!!)
                    processFile(cameraImageUri!!)
                    showPreview()
                }
            }
        viewModel.addBotMessage("Welcome to TribeOne")
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentQuestionIndex == 0 && answersMap.isEmpty()) {
                askNextQuestion()
            }
        }, 1000)

    }
    private fun askNextQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            viewModel.addBotMessage(question)
        } else {
            onFormCompleted()
        }
    }

    private fun onFormCompleted() {

        viewModel.addBotMessage("✅ Thank you! Here is your summary:")

        answersMap.forEach { (q, a) ->
            viewModel.addBotMessage("$q\n👉 $a")
        }

        // 👉 You can send to API / Firebase here
    }
    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "image/*",
                    "application/pdf",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        }
        fileSelectorLauncher.launch(intent)
    }

    private fun openCamera() {
        val file = File.createTempFile("camera_image", ".jpg", cacheDir)
        cameraImageUri = FileProvider.getUriForFile(this, "${packageName}.provider", file)
        cameraLauncher.launch(cameraImageUri!!)
    }

    fun uriToBase64(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream!!.readBytes()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri) ?: "application/octet-stream"
    }

    private fun showPreview() {
        binding.filePreviewContainer.visibility = View.VISIBLE
        binding.filePreviewList.removeAllViews()
        for (uri in selectedFiles) {
            val view = layoutInflater.inflate(
                R.layout.item_file_preview,
                binding.filePreviewList,
                false
            )
            val thumb = view.findViewById<ImageView>(R.id.fileThumb)
            val remove = view.findViewById<ImageView>(R.id.removeFile)
            val name = view.findViewById<TextView>(R.id.fileName)

            val fileName = getFileName(this, uri)
            name.text = fileName
            Log.e(TAG, "lastPathSegment: $fileName")
            val mime = contentResolver.getType(uri)

            when {
                mime?.startsWith("image") == true -> {
                    thumb.setImageURI(uri)
                }
                mime == "application/pdf" -> {
                    thumb.setImageResource(R.drawable.ic_pdf)
                }
                mime == "application/vnd.ms-excel" ||
                        mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> {
                    thumb.setImageResource(R.drawable.ic_excel)
                }
                else -> {
                    thumb.setImageResource(android.R.drawable.ic_menu_upload)
                }
            }

            remove.setOnClickListener {
                selectedFiles.remove(uri)
                showPreview()
            }
            binding.filePreviewList.addView(view)
        }
    }

    fun processFile(uri: Uri) {
         base64File = uriToBase64(this, uri)
         mime = getMimeType(this, uri)
        //Log.e(TAG, "base64File: $base64File")
        //Log.e(TAG, "mime: $mime")
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
}