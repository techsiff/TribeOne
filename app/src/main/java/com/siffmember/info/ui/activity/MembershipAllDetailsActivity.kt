package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityMembershipAllDetailsBinding
import com.siffmember.info.ui.adapter.MembershipAllDetailsPagerAdapter
import com.siffmember.info.ui.fragment.MembershipAllDetailsFragment
import com.siffmember.info.utils.AppConstants


class MembershipAllDetailsActivity : BaseActivity() {

    companion object {
        var TAG = "MembershipAllDetailsActivity"
    }

    private lateinit var binding: ActivityMembershipAllDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var memberId: String = ""
    private lateinit var categories: List<String>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembershipAllDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                memberId = vBundle.getString("memberId", null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.apply {
            updateDetails.setOnClickListener {
                val intent = Intent(this@MembershipAllDetailsActivity, AddMembershipDetailsActivity::class.java)
                intent.putExtra("memberId", memberId)
                startActivity(intent)
            }
            addNotes.setOnClickListener {
                addNotes.visibility = View.GONE
                saveNotes.visibility = View.VISIBLE
                val fragment = getCurrentFragment()
                fragment?.onAddNotesClicked()
            }
            saveNotes.setOnClickListener {
                val fragment = getCurrentFragment()
                fragment?.onSaveNotesClicked()
            }
            downloadReport.setOnClickListener {

            }
            deleteMember.setOnClickListener {
                deleteMemberDialog()
            }
        }
    }
    private fun deleteMemberDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Membership Data Alert")
                .setMessage("Are you sure you want to delete this membership data?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteMemberFireStore(memberId) { isDeleted ->
                        if (isDeleted) {
                            Toast.makeText(this, "Membership data deleted successfully", Toast.LENGTH_SHORT).show()
                            finish()
                            dialogInterface.dismiss()
                        }
                    }
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteMemberFireStore(memberId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(memberId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener { e ->
                Log.e(UserBlockListDetailsActivity.TAG, "Error deleting user details", e)
                onComplete(false)
                dismissProgDialog()
            }
    }
    private fun getCurrentFragment(): MembershipAllDetailsFragment? {
        val currentItem = binding.viewPager.currentItem
        val tag = "f$currentItem"
        return supportFragmentManager.findFragmentByTag(tag) as? MembershipAllDetailsFragment
    }

    override fun onStart() {
        super.onStart()
        fetchMemberData()
    }
    val fieldPriority = listOf(
        "Name",
        "Email Id",
        "Country Code",
        "Phone Number",
        "Address"
    )
    private fun fetchMemberData() {
        Log.e(TAG, "fetchMemberData: $memberId")
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
            .document(memberId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.data != null) {
                    Log.e(TAG, "fetchMemberData: $document")
                    val rawData = document.data!!
                    // Convert Firestore map safely
                    val formattedData = mutableMapOf<String, Map<String, String>>()
                    for ((key, value) in rawData) {
                        val innerMap = value as? Map<*, *>
                        if (innerMap != null) {
                            val stringMap = innerMap.mapNotNull {
                                val k = it.key as? String
                                val v = it.value as? String
                                //if (k != null && v != null) k to v else null
                                if (k != null && v != null && k != "PhoneLast4") {
                                    k to v
                                } else null
                            }.toMap()
                            formattedData[key] = stringMap
                        }
                    }
                    setupPager(formattedData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupPager(data: Map<String, Map<String, String>>) {
        categories = data.keys.toList()
        val sortedList = data
            .toList()
            .sortedWith(compareBy<Pair<String, Map<String, String>>> {
                when (it.first) {
                    "Personal Info" -> 0
                    else -> 1
                }
            }.thenBy { it.first })
            .map { (category, fields) ->
                category to fields
                    .toList()
                    .sortedWith(compareBy<Pair<String, String>> {
                        val index = fieldPriority.indexOf(it.first)
                        if (index == -1) Int.MAX_VALUE else index
                    }.thenBy { it.first.lowercase() })
                    .toMap()
            }

        val pagedList = sortedList
            .filter { it.first != "Notes" } // safety (if exists)
            .flatMap { (category, fields) ->
                fields.toList().chunked(10).map { chunk ->
                    category to chunk.toMap()
                }
            }
            .toMutableList()
        // ✅ Add dedicated empty Notes page at END
        pagedList.add("Notes" to emptyMap())
        val adapter = MembershipAllDetailsPagerAdapter(this, pagedList, memberId)
        binding.viewPager.adapter = adapter
        val totalPages = pagedList.size
        // Initial state
        binding.btnBack.visibility = View.GONE
        binding.btnNext.visibility = if (totalPages > 1) View.VISIBLE else View.GONE
        binding.btnBack.setOnClickListener {
            if (binding.viewPager.currentItem > 0) {
                binding.viewPager.currentItem--
            }
        }
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < totalPages - 1) {
                binding.viewPager.currentItem++
            }
        }
        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                binding.btnBack.visibility = if (position == 0) View.GONE else View.VISIBLE
                binding.deleteMember.visibility = if (position == 0) View.VISIBLE else View.GONE
                binding.btnNext.visibility = if (position == totalPages - 1) View.GONE else View.VISIBLE
                binding.saveNotes.visibility = View.GONE
                binding.addNotes.visibility = if (position == totalPages - 1) View.VISIBLE else View.GONE
            }
        })
    }

    fun onNotesAdded() {
        binding.saveNotes.visibility = View.GONE
        binding.addNotes.visibility = View.VISIBLE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
             if (binding.viewPager.currentItem > 0) {
                 binding.viewPager.currentItem--
             } else {
                 finish()
             }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}