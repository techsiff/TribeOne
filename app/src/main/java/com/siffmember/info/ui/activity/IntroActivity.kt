package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.KeyEvent.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityIntroBinding
import com.siffmember.info.ui.adapter.IntroAdapter
import com.siffmember.info.ui.model.IntroItem

class IntroActivity : BaseActivity() {

    companion object {
        //var TAG = "IntroActivity"
    }

    private lateinit var binding: ActivityIntroBinding
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var isUserScrolling = false
    private var delayMillis = 3000L
    private val introItems = listOf(
        IntroItem(
            title = "A Safe Space for Your Journey",
            description = "Welcome to Tribe One Men's Community. Enter to Get Support and Access our content",
            imageId = R.drawable.intro_one
        ),
        IntroItem(
            title = "You’re Held, You’re Heard",
            description = "You can call our helplines to seek support, guidance and trainings.",
            imageId = R.drawable.intro_two
        ),
        IntroItem(
            title = "Healing Begins With a Call",
            description = "Do not ignore when the problem is mild. Call our helplines and get trained.",
            imageId = R.drawable.intro_three
        ),
        IntroItem(
            title = "Wisdom Before the Journey",
            description = "Unmarried men can request special trainings about how to avoid troubles in relationships and marriage.",
            imageId = R.drawable.intro_four
        ),

        IntroItem(
            title = "A Brotherhood Without Walls",
            description = "Our Men's Community is all over India and also in US, UK, Europe, Australia, Middle East and South East Asia.",
            imageId = R.drawable.intro_five
        )
    )

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.introBottomLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        val adapter = IntroAdapter(introItems)
        binding.introViewPager.adapter = adapter

        binding.arIndicator.removeIndicators()
        binding.arIndicator.attachTo(binding.introViewPager)

        binding.btnLogin.setOnClickListener {
            startActivity(Intent(this@IntroActivity, LoginActivity::class.java))
            //finish()
        }

        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this@IntroActivity, RegisterUserActivity::class.java))
            //finish()
        }

        binding.btnEnter.setOnClickListener {
            startActivity(Intent(this@IntroActivity, HomeGuestActivity::class.java))
            //finish()
        }
        handler = Handler(Looper.getMainLooper())

        runnable = Runnable {
            if (!isUserScrolling) {
                val itemCount = adapter.itemCount
                val nextItem = (binding.introViewPager.currentItem + 1) % itemCount
                binding.introViewPager.setCurrentItem(nextItem, true)
            }
        }
        binding.introViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Reset the timer after every page scroll completes
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, delayMillis)
            }

            override fun onPageScrollStateChanged(state: Int) {
                isUserScrolling = state != ViewPager2.SCROLL_STATE_IDLE
            }
        })
        handler.postDelayed(runnable, delayMillis)

    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(runnable, delayMillis)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finishAffinity()
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}