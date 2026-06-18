package com.siffmember.info.ui.fragment

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.AppConstants

open class BaseFragment: Fragment() {

    private var progressDialog: ProgressDialog? = null
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor: SharedPreferences.Editor
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        progressDialog = ProgressDialog(requireActivity())
        sharedPref = requireActivity().getSharedPreferences(AppConstants.PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            progressDialog!!.setTheme(ProgressDialog.THEME_FOLLOW_SYSTEM)
        }
    }

    /**
     * Showing progress dialog
     */
    fun showProgDialog() {
        try {
            progressDialog!!.setMode(ProgressDialog.MODE_INDETERMINATE)
            progressDialog!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dismiss progress dialog
     */
    fun dismissProgDialog() {
        progressDialog!!.dismiss()
    }
}