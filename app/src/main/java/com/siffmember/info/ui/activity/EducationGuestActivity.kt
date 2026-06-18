package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityEducationBinding
import com.siffmember.info.ui.adapter.EducationGuestPagerAdapter
import com.siffmember.info.ui.adapter.EducationPagerAdapter
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.OpenPoints

class EducationGuestActivity : BaseActivity() {

    private lateinit var binding: ActivityEducationBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEducationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        EducationDetails.setPDFContentAdd(false)
        EducationDetails.setVideoContentAdd(false)

        val viewPager = binding.viewPager
        val tabs = binding.tabs

        val adapter = EducationGuestPagerAdapter(this)
        viewPager.adapter = adapter

        // Tab titles
        val tabTitles = arrayOf(resources.getString(R.string.tab_text_1), resources.getString(R.string.tab_text_2))
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        binding.apply {
            title.text = "Guest ${resources.getString(R.string.education_title)}"

            educationAdd.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            educationAdd.setOnClickListener {
                if (viewPager.currentItem == 0) {
                    startActivity(Intent(this@EducationGuestActivity, AddEducationVideoGuestActivity::class.java))
                } else {
                    startActivity(Intent(this@EducationGuestActivity, AddEducationPDFGuestActivity::class.java))
                }
            }
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