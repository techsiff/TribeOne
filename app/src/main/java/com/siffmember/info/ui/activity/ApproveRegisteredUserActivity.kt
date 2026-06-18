package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.databinding.ActivityApproveRegisteredUsersBinding
import com.siffmember.info.ui.adapter.RegisteredUsersAdapter
import com.siffmember.info.ui.model.UsersRegistration
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UsersDetails
import kotlin.collections.filter

class ApproveRegisteredUserActivity : BaseActivity(), RegisteredUsersAdapter.UsersRegistrationListener {

    companion object {
        var TAG = "ApproveRegisteredUserActivity"
    }

    private lateinit var binding: ActivityApproveRegisteredUsersBinding
    private var recyclerViewAdapter: RegisteredUsersAdapter? = null
    private lateinit var db: FirebaseFirestore
    private var allUsers: ArrayList<UsersRegistration> = ArrayList()
    private var filteredItems: List<UsersRegistration> = emptyList()
    private var adminId: String = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApproveRegisteredUsersBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeaderGcp) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.registeredUserLl.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        try{
            adminId = sharedPref.getString(AppConstants.USER_ID, null)!!
        }catch (e: Exception){
            e.printStackTrace()
        }
        setupSearchView()
        setupRecyclerView()
        updateList(allUsers)
    }

    override fun onResume() {
        super.onResume()
        fetchAllContactusDetails()
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAllContactusDetails() {
        try {
            val usersList: ArrayList<UsersRegistration> = ArrayList()
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_REGISTRATION_DETAILS)
            docRef.get()
                .addOnSuccessListener { documents ->
                    allUsers.clear()
                    usersList.clear()
                    if (documents != null) {
                        if(documents.size() != 0){
                            val data = documents.toObjects<UsersRegistration>()
                            Log.e(TAG, "${data.size}")
                            usersList.addAll(data)
                            val allUser = usersList
                                .filter { it.phone_number != adminId }
                                .sortedBy { it.name }
                            allUsers.addAll(allUser)
                            updateList(allUsers)
                            binding.usersCount.text = "Total Registered Users: ${allUsers.size}"
                            binding.searchView.visibility = View.VISIBLE
                        } else {
                            updateList(allUsers)
                            binding.usersCount.text = "Total Registered Users: 0"
                            binding.searchView.visibility = View.GONE
                        }
                        dismissProgDialog()
                    } else {
                        Log.e("getAllUsersData", "No such document")
                        dismissProgDialog()
                        updateList(allUsers)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("getAllUsersData", "get failed with ", exception)
                    dismissProgDialog()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupSearchView() {
        binding.searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val filtered = if (query.isEmpty()) {
                    allUsers
                } else {
                    allUsers.filter {
                        it.name!!.contains(query, ignoreCase = true) ||
                                it.phone_number!!.contains(query)
                    }
                }
                binding.usersCount.text = "Total Registered Users: ${filtered.size}"

                updateList(filtered)
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = RegisteredUsersAdapter(
            allUsers,
            this
        )
        binding.allUsersList.layoutManager = LinearLayoutManager(this)
        binding.allUsersList.adapter = recyclerViewAdapter
    }

    private fun updateList(users: List<UsersRegistration>) {
        filteredItems = users
        recyclerViewAdapter!!.updateList(users)
    }

    override fun onSelectUser(user: UsersRegistration?) {
        binding.searchView.setText("")
        UsersDetails.setUsersRegisteredDetails(user!!)
        startActivity(Intent(this@ApproveRegisteredUserActivity, ApproveUserDetailsActivity::class.java))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}