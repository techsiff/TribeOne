package com.siffmember.info.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.siffmember.info.databinding.ActivityContactUsBinding
import com.siffmember.info.ui.adapter.ContactusAdapter
import com.siffmember.info.ui.model.ContactListItem
import com.siffmember.info.ui.model.ContactusDetails
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.ui.fragment.GuestBottomSheetFragment
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CallLogDetails
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.OpenPoints
import com.siffmember.info.utils.PermissionUtils

class ContactUsActivity : BaseActivity(), ContactusAdapter.ContactListener, GuestBottomSheetFragment.GuestUserBottomSheetListener {

    companion object {
        var TAG = "ContactUsActivity"
    }

    private lateinit var binding: ActivityContactUsBinding
    private var recyclerViewAdapter: ContactusAdapter? = null
    private lateinit var db: FirebaseFirestore
    private var allContacts: ArrayList<ContactusDetails> = ArrayList()
    private var filteredItems: List<ContactListItem> = emptyList()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var contactDetail: ContactusDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater).apply {
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
            binding.contactusLl.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        EducationDetails.setContactUsAdd(false)
        db = Firebase.firestore
        if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) {
            binding.contactusAdd.visibility = View.VISIBLE
        } else {
            binding.contactusAdd.visibility = View.GONE
        }
        binding.contactusAdd.setOnClickListener {
            startActivity(Intent(this, AddContactUsActivity::class.java))
        }

        setupSearchView()
        setupRecyclerView()
        updateList(allContacts)
        fetchAllContactusDetails()

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                //var allGranted = true
                permissions.entries.forEach { entry ->
                    val permission = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        //allGranted = false
                        if (PermissionUtils.isPermissionDeniedForever(this, permission)) {
                            showGoToSettingsDialog()
                        } else {
                            showPermissionDeniedDialog()
                        }
                    }
                }
                /*if (allGranted) {
                    onAllPermissionsGranted()
                }*/
            }
        requestPermissions()
        CallLogDetails.setCallInitiated(false)
        CallLogDetails.setUserName("")
        CallLogDetails.setUserPhoneNumber("")
    }

    override fun onResume() {
        super.onResume()
        if(EducationDetails.getContactUsAdd()){
            EducationDetails.setContactUsAdd(false)
            fetchAllContactusDetails()
        }
    }

    private fun requestPermissions() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
        } /*else {
            onAllPermissionsGranted()
        }*/
    }


    /*private fun onAllPermissionsGranted() {
        //Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
        // Start call monitoring service here
        //startCallMonitoringService()
    }*/


    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("These permissions are required for call monitoring. Please allow them.")
            .setPositiveButton("Retry") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("You have permanently denied permissions. Please enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchAllContactusDetails() {
        try {
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_CONTACT_US_DETAILS)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents != null) {
                        if(documents.size() != 0){
                            allContacts.clear()
                            val data = documents.toObjects<ContactusDetails>()
                            //Log.e(TAG, "${data.size}")
                            allContacts.addAll(data)
                            updateList(allContacts)
                        }
                        dismissProgDialog()
                    } else {
                        Log.e("getAllUsersData", "No such document")
                        dismissProgDialog()
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

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val filtered = if (query.isEmpty()) {
                    allContacts
                } else {
                    allContacts.filter {
                        it.name!!.contains(query, ignoreCase = true) ||
                                it.phoneNumber!!.contains(query) || it.state!!.contains(query, ignoreCase = true)
                    }
                }
                updateList(filtered)
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = ContactusAdapter(emptyList(), this, sharedPref.getBoolean(AppConstants.IS_ADMIN, false))
        binding.contactusList.layoutManager = LinearLayoutManager(this)
        binding.contactusList.adapter = recyclerViewAdapter
    }

    private fun updateList(contacts: List<ContactusDetails>) {
        val grouped = contacts
            .groupBy { it.state.orEmpty() }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        val items = mutableListOf<ContactListItem>()
        grouped.forEach { (state, stateContacts) ->
            items.add(ContactListItem.Header(state))
            val sortedContacts = stateContacts.sortedBy { it.name }
            items.addAll(sortedContacts.map { ContactListItem.ContactItem(it) })
        }
        filteredItems = items
        recyclerViewAdapter!!.updateList(items)
    }

    override fun onContact(contactDetails: ContactusDetails?) {
        if (!PermissionUtils.hasAllPermissions(this@ContactUsActivity)) {
            permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
        } else {
            contactDetail = contactDetails
            if(OpenPoints.getIsGuestUser()){
                val bottomSheetFragment = GuestBottomSheetFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            } else {
                contactDetails.let {
                    CallLogDetails.setCallInitiated(true)
                    CallLogDetails.setUserName(it!!.name.toString())
                    CallLogDetails.setUserPhoneNumber(it.phoneNumber.toString())
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = "tel:${it.phoneNumber}".toUri()
                    startActivity(intent)
                }
            }
        }
    }
    override fun onGuestUserDetails(userName: String, phoneNumber: String) {
        CallLogDetails.setGuestUserName(userName)
        CallLogDetails.setGuestUserPhoneNumber(phoneNumber)
        contactDetail.let {
            CallLogDetails.setCallInitiated(true)
            CallLogDetails.setUserName(it!!.name.toString())
            CallLogDetails.setUserPhoneNumber(it.phoneNumber.toString())
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = "tel:${it.phoneNumber}".toUri()
            startActivity(intent)
        }
    }
    override fun onDeleteContact(contactDetails: ContactusDetails?) {
        deleteContact(contactDetails)
        /*val next = Intent(this@ContactUsActivity, UserCallHistoryActivity::class.java)
        next.putExtra("contact_name", contactDetails!!.name)
        next.putExtra("contact_number", contactDetails.phoneNumber)
        next.putExtra("screen", "1")
        startActivity(next)*/
    }



    private fun deleteContact(contactDetails: ContactusDetails?) {
        try{
           AlertDialog.Builder(this)
                .setTitle("Delete Contact us Alert")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteNotes(contactDetails)
                    dialogInterface.dismiss()
                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteNotes(contactDetails: ContactusDetails?){
        showProgDialog()
        db.collection(AppConstants.TABLE_CONTACT_US_DETAILS).document(contactDetails!!.phoneNumber!!)
            .delete()
            .addOnSuccessListener {
                Log.e(TAG, "DocumentSnapshot successfully deleted!")
                Toast.makeText(this@ContactUsActivity,"Contact deleted successfully", Toast.LENGTH_LONG).show()
                fetchAllContactusDetails()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
                Toast.makeText(this@ContactUsActivity,"Contact deleting failed try again!", Toast.LENGTH_LONG).show()
                dismissProgDialog()
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            CallLogDetails.setCallInitiated(false)
            CallLogDetails.setUserName("")
            CallLogDetails.setUserPhoneNumber("")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}