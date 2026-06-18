package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityAiTribeoneHomeBinding
import com.siffmember.info.databinding.ItemStoryOptionBinding
import androidx.core.graphics.toColorInt

class AITribeOneHomeActivity : BaseActivity() {

    companion object {
        var TAG = "AITribeOneHomeActivity"
    }

    private lateinit var binding: ActivityAiTribeoneHomeBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAiTribeoneHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.headerLayout) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }

        setupOptions()
    }

    private fun setupOptions() {

        setupItem(
            binding.createQuestionnaire,
            "Create Questionnaire",
            "Create custom questionnaires",
            R.drawable.ic_questionnaire,
            "#3F7CF7"
        ){
            startActivity(Intent(this, AIQuestionnaireActivity::class.java))
        }

        setupItem(
            binding.uploadStory,
            "Upload Story",
            "Upload your story from a file",
            R.drawable.ic_story_upload,
            "#22C55E"
        ){
            startActivity(Intent(this, AIUserStoryCollectActivity::class.java))
        }

        setupItem(
            binding.manageStory,
            "Manage Story",
            "Manage your previous uploaded story",
            R.drawable.ic_manage_story,
            "#EE7103"
        ){
            startActivity(Intent(this, AIUserStoryManageActivity::class.java))
        }

        setupItem(
            binding.generalTalk,
            "General Talk",
            "Start a general conversation",
            R.drawable.ic_talk,
            "#A855F7"
        ){
            startActivity(Intent(this, AIGeneralTalkActivity::class.java))
        }

        /*setupItem(
            binding.manualEntry,
            "Manually Enter Story",
            "Write and enter your story manually",
            R.drawable.ic_edit,
            "#F97316"
        )*/
    }
    private fun setupItem(
        itemBinding: ItemStoryOptionBinding,
        title: String,
        subtitle: String,
        iconRes: Int,
        bgColor: String,
        onClick: () -> Unit
    ) {
        itemBinding.title.text = title
        itemBinding.subtitle.text = subtitle
        itemBinding.icon.setImageResource(iconRes)

        val drawable = itemBinding.iconContainer.background.mutate()
        drawable.setTint(bgColor.toColorInt())

        itemBinding.root.setOnClickListener {
            onClick.invoke()
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