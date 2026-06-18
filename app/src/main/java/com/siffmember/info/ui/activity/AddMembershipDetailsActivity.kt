package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAddMembershipDetailsBinding
import com.siffmember.info.ui.fragment.SearchUsersBottomSheetFragment
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.ui.model.MembershipField
import com.siffmember.info.ui.model.MembershipParamField
import com.siffmember.info.ui.view.DynamicFormBuilder
import com.siffmember.info.utils.AppConstants

class AddMembershipDetailsActivity : BaseActivity(), SearchUsersBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AddMembershipDetailsActivity"
    }

    private lateinit var binding: ActivityAddMembershipDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var memberId: String = ""
    private lateinit var formBuilder: DynamicFormBuilder
    private var adminID = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMembershipDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->

            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // Push entire layout up
            view.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                memberId = vBundle.getString("memberId", null)
            }
            adminID = sharedPref.getString(AppConstants.USER_ID, null)!!
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if(memberId.isEmpty()){
            binding.addMembershipTitle.text = "Add Membership Details"
            binding.btnAdd.text = "Add"
            binding.search.visibility = View.VISIBLE
        } else {
            binding.addMembershipTitle.text = "Update Membership Details"
            binding.btnAdd.text = "Update"
            binding.search.visibility = View.GONE
        }

        db = Firebase.firestore
        val fields = mutableListOf<MembershipField>()
        formBuilder = DynamicFormBuilder(this@AddMembershipDetailsActivity, binding.dynamicContainer)
        fetchAllParameters{ params ->
            params.forEach { param ->
                val inputType = if(param.paramName.contentEquals("Email")){
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                } else if(param.paramName.contentEquals("Phone Number")){
                    InputType.TYPE_CLASS_PHONE
                } else{
                    InputType.TYPE_CLASS_TEXT
                }
                //Log.e(TAG, "Field Name: ${param.paramName} Mandatory::${param.mandatory}")
                val field = MembershipField(param.paramName, param.paramName, param.mandatory, param.category, inputType)
                fields.add(field)
            }
            formBuilder.build(fields, memberId.isNotEmpty())
            if(memberId.isNotEmpty()) {
                fetchMemberData()
            }
        }

        binding.apply {
            btnAdd.setOnClickListener {
                try{
                    if (formBuilder.validate(fields)) {
                        showProgDialog()
                        val data = formBuilder.getValues()
                        // Find Membership Number inside all categories
                        var membershipNumber: String? = null
                        data.forEach { (_, fieldsMap) ->
                            if (fieldsMap.containsKey("Phone Number")) {
                                membershipNumber = fieldsMap["Phone Number"]
                            }
                        }
                        if (membershipNumber!!.isNotBlank()) {
                            if(memberId.isEmpty()) {
                                addMembershipMembers(data, membershipNumber) { success ->
                                    if (success) {
                                        formBuilder.clearAllFields()
                                    }
                                }
                            } else {
                                updateMembershipMembers(data, memberId) { success ->
                                    if (success) {
                                        formBuilder.clearAllFields()
                                        finish()
                                    }
                                }
                            }
                        } else {
                            dismissProgDialog()
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                    dismissProgDialog()
                }
            }

            search.setOnClickListener {
                val bottomSheetFragment = SearchUsersBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }

    }

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
                                if (k != null && v != null) k to v else null
                            }.toMap()
                            formattedData[key] = stringMap
                        }
                    }
                    formBuilder.setValues(formattedData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }

    fun fetchAllParameters(onResult: (List<MembershipParamField>) -> Unit) {
        db.collection(AppConstants.TABLE_MEMBERSHIP_PARAMS)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.e(TAG, "All meetings: ${snapshot.documents.size}")
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MembershipParamField::class.java)
                }
                onResult(result)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching meetings: ${it.message}")
                onResult(emptyList())
                dismissProgDialog()
            }
    }

    private fun addMembershipMembers(user: Map<String, Map<String, String>>, membershipNumber: String, onResult: (Boolean) -> Unit) {
        val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(membershipNumber)
        docRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                Toast.makeText(this, "Member already exists!", Toast.LENGTH_LONG).show()
                dismissProgDialog()
                onResult(false)
            } else {
                docRef.set(user)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Member details added successfully", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                        onResult(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error writing document", e)
                        Toast.makeText(this, "Failed to add member details", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                        onResult(false)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking document", e)
            dismissProgDialog()
            onResult(false)
        }
    }

    private fun updateMembershipMembers(user: Map<String, Map<String, String>>, membershipNumber: String, onResult: (Boolean) -> Unit) {
        val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(membershipNumber)
        docRef.set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Member details updated successfully", Toast.LENGTH_LONG).show()
                dismissProgDialog()
                onResult(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error writing document", e)
                Toast.makeText(this, "Failed to update member details", Toast.LENGTH_LONG).show()
                dismissProgDialog()
                onResult(false)
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSelectedUser(user: GetUsers) {
        val data = mutableMapOf<String, MutableMap<String, String>>()

        data.getOrPut("Personal Info") { mutableMapOf() }["Name"] = user.name as String
        data.getOrPut("Personal Info") { mutableMapOf() }["Email Id"] = user.email_id as String
        data.getOrPut("Personal Info") { mutableMapOf() }["Country Code"] = user.country as String
        data.getOrPut("Personal Info") { mutableMapOf() }["Phone Number"] = user.phone_number as String
        formBuilder.setValuesFromUserSelect(data)
    }
}