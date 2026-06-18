package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityEducationBinding
import com.siffmember.info.ui.adapter.EducationPagerAdapter
import com.siffmember.info.ui.fragment.EducationFragments
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class EducationActivity : BaseActivity(), EducationFragmentInteractionInterface {

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
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.viewPager.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        EducationDetails.setPDFContentAdd(false)
        EducationDetails.setVideoContentAdd(false)

        val viewPager = binding.viewPager
        val tabs = binding.tabs

        val adapter = EducationPagerAdapter(this)
        viewPager.adapter = adapter

        // Tab titles
        val tabTitles = arrayOf(resources.getString(R.string.tab_text_1), resources.getString(R.string.tab_text_2))
        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()

        binding.apply {
            title.text = resources.getString(R.string.education_title)

            educationAdd.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) {
                View.VISIBLE
            } else {
                View.GONE
            }
            educationAdd.setOnClickListener {
                if (viewPager.currentItem == 0) {
                    startActivity(Intent(this@EducationActivity, AddEducationVideoActivity::class.java))
                } else {
                    startActivity(Intent(this@EducationActivity, AddEducationPDFActivity::class.java))
                }
            }
        }
    }

    override fun onFragmentInteraction(fragments: EducationFragments, tag: String) {
        when(fragments){
            EducationFragments.EDUCATION_DETAILS_ONE_FRAGMENT -> {
                binding.educationAdd.visibility = View.VISIBLE
            }
            EducationFragments.EDUCATION_DETAILS_TWO_FRAGMENT -> {
                binding.educationAdd.visibility = View.GONE
            }
            EducationFragments.EDUCATION_DETAILS_THREE_FRAGMENT -> {
                binding.educationAdd.visibility = View.GONE
            }
        }
    }

    /*override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }*/
}