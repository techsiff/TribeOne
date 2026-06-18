package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.databinding.ActivityOpenPointsListBinding
import com.siffmember.info.ui.fragment.OPFragments
import com.siffmember.info.ui.interfaces.OPFragmentInteractionInterface

class OpenPointsListActivity : BaseActivity(), OPFragmentInteractionInterface {

    companion object {
        var TAG = "OpenPointsListActivity"
    }
    private lateinit var binding: ActivityOpenPointsListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenPointsListBinding.inflate(layoutInflater)
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
            binding.fragmentContainerView.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        binding.apply {
            addNewPoints.setOnClickListener {
                val nextIntent = Intent(this@OpenPointsListActivity, OpenPointsAddActivity::class.java)
                startActivity(nextIntent)
            }
        }
    }

    override fun onFragmentInteraction(fragments: OPFragments, tag: String) {
       // Log.e(TAG,tag)
        when(fragments){
            OPFragments.OPEN_POINTS_DETAILS_ONE_FRAGMENT -> {
                binding.addNewPoints.visibility = View.VISIBLE
            }
            OPFragments.OPEN_POINTS_DETAILS_TWO_FRAGMENT -> {
                binding.addNewPoints.visibility = View.GONE
            }
            OPFragments.OPEN_POINTS_DETAILS_THREE_FRAGMENT -> {
                binding.addNewPoints.visibility = View.GONE
            }
        }
    }

    /*override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            //finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }*/

}