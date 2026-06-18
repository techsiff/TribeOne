package com.siffmember.info.ui.activity

import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.siffmember.info.BuildConfig
import com.siffmember.info.data.remote.api.RetrofitInstanceAI
import com.siffmember.info.data.remote.model.geminiAI.GeminiApi
import com.siffmember.info.databinding.ActivityAiUserStoryCollectedBinding
import com.siffmember.info.ui.adapter.ChatAdapter
import com.siffmember.info.ui.repository.GeminiRepository
import com.siffmember.info.ui.viewmodel.ChatViewModel
import com.siffmember.info.utils.UserStory

class AIUserStoryCollectedActivity : BaseActivity() {

    companion object {
        var TAG = "AIUserStoryCollectedActivity"
    }
    private lateinit var binding: ActivityAiUserStoryCollectedBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var adapter: ChatAdapter

    private var base64File = ""
    private var mime = ""
    private var prompt = ""


    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiUserStoryCollectedBinding.inflate(layoutInflater)
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
            binding.chatRecyclerViewLL.setPadding(0, 0, 0, maxOf(ime, nav))
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

        base64File = UserStory.getStoryFile()
        mime = UserStory.getMime()
        prompt = UserStory.getQuestions()

        val prompt = """
                        Read the file and provide answers based on the following questions.
                 
                        Questions:
                        $prompt
                    """.trimIndent()

        viewModel.sendMessage(prompt, base64File, mime, apiKey)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}