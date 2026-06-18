package com.siffmember.info.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.CreateUserAccountRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailResponse
import com.siffmember.info.databinding.ActivityAddUserBinding
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.Users
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.ExcelUtil
import com.siffmember.info.utils.Utils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AddUserActivity : BaseActivity() {

    companion object {
        var TAG = "AddUserActivity"
    }
    private lateinit var binding: ActivityAddUserBinding
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    //private lateinit var folderPickerLauncher : ActivityResultLauncher<Intent>
    private lateinit var db: FirebaseFirestore
    private var nameFN = ""
    private var nameLN = ""
    private var email = ""
    private var phone = ""
    private var category = "Admin"
    //private var readExcelNew: MutableList<Map<Int, Any>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        val adapter = ArrayAdapter(this, R.layout.spinner_list, Utils.category)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.apply {

            if(sharedPref.getBoolean(AppConstants.IS_ADMIN, false)){
                upDownUser.visibility = View.VISIBLE
            } else {
                upDownUser.visibility = View.GONE
            }
            btnUpload.setOnClickListener {
                openFilePicker()
            }
           /* downloadUser.setOnClickListener {
                downloadAllUsersDetails()
            }*/
            btnAdd.setOnClickListener {
                if(validate()) {
                    val nameFL = "$nameFN $nameLN"
                    val cc = ccp.selectedCountryCodeWithPlus
                    val user = Users(nameFL, email, cc, phone, category)
                    showProgDialog()
                    addUser(user)
                }
            }

            btnUserSettings.setOnClickListener {
                startActivity(Intent(this@AddUserActivity, ManageUserActivity::class.java))
            }

            btnUserApprove.setOnClickListener {
                startActivity(Intent(this@AddUserActivity, ApproveRegisteredUserActivity::class.java))
            }

            btnUsersCallHistory.setOnClickListener {
                startActivity(Intent(this@AddUserActivity, AllUsersCallHistoryActivity::class.java))
            }
            spinnerCategory.adapter = adapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        category = Utils.category[position]
                    } else {
                        // handle the case where the view parameter is null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // write code to perform some action
                }
            }
        }
        // Initialize the ActivityResultLauncher
        fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    //Log.e("AddUserActivity","${uri.path}")
                    showProgDialog()
                    importExcelUsers(uri)
                }

            }
        }
        /*folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    showProgDialog()
                    writeExcelNew(this@AddUserActivity, readExcelNew, uri)
                }
            }
        }*/
    }

    private fun validate(): Boolean{
        if(binding.nameEditFn.text.toString().trim().isNotEmpty()){
            nameFN = binding.nameEditFn.text.toString()
        } else {
            Toast.makeText(this@AddUserActivity,"Please enter first name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.nameEditLn.text.toString().trim().isNotEmpty()){
            nameLN = binding.nameEditLn.text.toString()
        } else {
            Toast.makeText(this@AddUserActivity,"Please enter last name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.emailEdit.text.toString().trim().isNotEmpty()){
            email = binding.emailEdit.text.toString()
        } else {
            Toast.makeText(this@AddUserActivity,"Please enter email id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.phoneEdit.text.toString().trim().isNotEmpty()){
            phone = binding.phoneEdit.text.toString()
        } else {
            Toast.makeText(this@AddUserActivity,"Please enter phone number", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addUser(user: Users){
        try{
            db.collection(AppConstants.TABLE_USER_DETAILS).document(user.phone_number!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    val member = MembersGroup(user.phone_number, user.name!!, user.phone_number, false, "")
                    addMemberToGroup(member) { isAdded ->
                        if (isAdded) {
                            updateUserGroupDocument(member.phoneNumber) {
                                if(user.country.equals("+91")){
                                   // sendApproveEmail(user.email_id!!, "", "", user.name)
                                } else {
                                    val tempPass = Utils.generateRandomPass(8)
                                    createUserAccount(user.email_id!!, tempPass, user.name)
                                }
                            }
                        } else {
                            dismissProgDialog()
                            //Toast.makeText(this, "Failed to add ${member.name}, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    binding.nameEditFn.setText("")
                    binding.nameEditLn.setText("")
                    binding.emailEdit.setText("")
                    binding.phoneEdit.setText("")
                    nameFN = ""
                    nameLN = ""
                    email = ""
                    phone = ""
                    category = "Admin"
                    binding.spinnerCategory.setSelection(0)
                    dismissProgDialog()
                    Toast.makeText(this@AddUserActivity,"User added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(this@AddUserActivity,"User failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createUserAccount(emailId: String, userTempPass: String, userName: String){
        try{
            val request = CreateUserAccountRequest(emailId, userTempPass)

            RetrofitInstanceFunction.api.createUserAccount(request)
                .enqueue(object : Callback<SendApprovedEmailResponse> {
                    override fun onResponse(call: Call<SendApprovedEmailResponse>, response: Response<SendApprovedEmailResponse>) {
                        dismissProgDialog()
                        if (response.isSuccessful) {
                            Log.e(TAG, "User account created successfully ${response.body()!!.message}")
                            sendApproveEmail(emailId, userTempPass, response.body()!!.message, userName)
                        } else {
                            Log.e(TAG, "User account creation failed")
                        }
                    }

                    override fun onFailure(call: Call<SendApprovedEmailResponse>, t: Throwable) {
                        Log.e(TAG, "Error: ${t.message}")
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun sendApproveEmail(emailId: String, userTempPass: String, verificationLink: String, userName: String){
        try{
            val subject = "Your TribeOne Registration Has Been Approved"
            val msg = if(userTempPass.isEmpty()){
                """
                    Hello $userName,
            
                    We’re excited to let you know that your TribeOne registration request has been approved.
            
                    You can now log in to the TribeOne app using your registered phone number and OTP verification.
            
                    Welcome to the TribeOne community!
            
                    Best regards,
                    The TribeOne Team
                """.trimIndent()
            } else {
                """
                    Hello $userName,
            
                    We’re excited to let you know that your TribeOne registration request has been approved.
            
                    To complete your registration, please verify your email address using the link below:
                    $verificationLink
            
                    Once your email is verified, you can log in to the TribeOne app using your registered email address and the temporary password provided below:
            
                    Temporary Password: $userTempPass
            
                    You can change your password anytime from the app settings.
            
                    Welcome to the TribeOne community — we’re thrilled to have you onboard!
            
                    Best regards,
                    The TribeOne Team
                """.trimIndent()
            }

            val request = SendApprovedEmailRequest(emailId, subject, msg)

            RetrofitInstanceFunction.api.sendApprovedEmail(request)
                .enqueue(object : Callback<SendApprovedEmailResponse> {
                    override fun onResponse(call: Call<SendApprovedEmailResponse>, response: Response<SendApprovedEmailResponse>) {
                        dismissProgDialog()
                        if (response.isSuccessful) {
                            Log.e(TAG, "Email sent successfully")
                        } else {
                            Log.e(TAG, "Email sent failed")
                        }
                    }

                    override fun onFailure(call: Call<SendApprovedEmailResponse>, t: Throwable) {
                        Log.e(TAG, "Error: ${t.message}")
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private var usersFromFileList: ArrayList<Users> = ArrayList()

    @Suppress("UNCHECKED_CAST")
    fun addMemberToGroup(newMember: MembersGroup, callback: (Boolean) -> Unit) {
        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
        userRef.whereEqualTo("id", AppConstants.EDUCATION_GROUP_ID).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val currentMembers = (document["members"] as? List<Map<String, Any>>)?.map { memberMap ->
                            MembersGroup(
                                id = memberMap["id"] as? String ?: "",
                                name = memberMap["name"] as? String ?: "",
                                phoneNumber = memberMap["phoneNumber"] as? String ?: "",
                                isAdmin = memberMap["admin"] as? Boolean == true,
                                fcmToken = memberMap["fcmToken"] as? String ?: ""
                            )
                        } ?: emptyList()
                        // Check if the member already exists
                       /* if (currentMembers.any { it.id == newMember.id }) {
                            Log.e("Firestore", "Member already exists")
                            callback(false)
                            return@addOnSuccessListener
                        }*/
                        // Add the new member to the list
                        val updatedMembers = currentMembers.toMutableList()
                        updatedMembers.add(newMember)
                        // Update Firestore
                        document.reference.update("members", updatedMembers)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Member added successfully")
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error adding member", exception)
                                callback(false)
                            }
                    }
                } else {
                    Log.e("Firestore", "Group not found")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error finding group", exception)
                callback(false)
            }
    }

    private fun updateUserGroupDocument(phoneNumber: String, onComplete: () -> Unit) {
        val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(phoneNumber)
        userGroupRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userGroupRef.update("groupsId", FieldValue.arrayUnion(AppConstants.EDUCATION_GROUP_ID))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Group ID added to existing document")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update group ID", e)
                        onComplete()
                    }
            } else {
                val newUserGroup = mapOf("groupsId" to listOf(AppConstants.EDUCATION_GROUP_ID))
                userGroupRef.set(newUserGroup)
                    .addOnSuccessListener {
                        Log.d("Firestore", "New user group document created")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to create document", e)
                        onComplete()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Failed to fetch document", e)
            onComplete()
        }
    }

   @Suppress("UNCHECKED_CAST")
   private fun addUsersBatch(users: List<Users>, groupId: String) {
       showProgDialog()

       val batch = db.batch()
       val groupRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
           .whereEqualTo("id", groupId)

       groupRef.get().addOnSuccessListener { querySnapshot ->
           if (querySnapshot.isEmpty) {
               dismissProgDialog()
               Toast.makeText(this, "Group not found!", Toast.LENGTH_LONG).show()
               return@addOnSuccessListener
           }

           val groupDoc = querySnapshot.documents.first()
           val currentMembers = (groupDoc["members"] as? List<Map<String, Any>>)?.map {
               MembersGroup(
                   id = it["id"] as? String ?: "",
                   name = it["name"] as? String ?: "",
                   phoneNumber = it["phoneNumber"] as? String ?: "",
                   isAdmin = it["admin"] as? Boolean == true,
                   fcmToken = it["fcmToken"] as? String ?: ""
               )
           }?.toMutableList() ?: mutableListOf()

           val membersMap = currentMembers
               .distinctBy { it.phoneNumber }  // removes duplicates if any
               .associateBy { it.phoneNumber } // converts to map (key = phoneNumber)
               .toMutableMap()
           users.forEach { user ->
               val userRef = db.collection(AppConstants.TABLE_USER_DETAILS)
                   .document(user.phone_number!!)

               // Add/update user document
               batch.set(userRef, user)

               // Update user's group reference
               val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS)
                   .document(user.phone_number)
               batch.set(
                   userGroupRef,
                   mapOf("groupsId" to FieldValue.arrayUnion(groupId)),
                   SetOptions.merge()
               )

               // Replace or add new member (avoids duplicates)
               membersMap[user.phone_number] = MembersGroup(
                   id = user.phone_number,
                   name = user.name ?: "",
                   phoneNumber = user.phone_number,
                   isAdmin = membersMap[user.phone_number]?.isAdmin ?: false, // keep admin flag if existed
                   fcmToken = membersMap[user.phone_number]?.fcmToken ?: ""
               )
           }

           // ✅ Convert back to list and update group document
           val updatedMembers = membersMap.values.toList()
           batch.update(groupDoc.reference, "members", updatedMembers)

           // 🔥 Commit all writes together
           batch.commit()
               .addOnSuccessListener {
                   dismissProgDialog()
                   Toast.makeText(this, "✅ All users added successfully!", Toast.LENGTH_LONG).show()
               }
               .addOnFailureListener { e ->
                   dismissProgDialog()
                   Log.e(TAG, "❌ Batch write failed", e)
                   Toast.makeText(this, "Failed to add users: ${e.message}", Toast.LENGTH_LONG).show()
               }
       }.addOnFailureListener { e ->
           dismissProgDialog()
           Log.e(TAG, "❌ Error finding group", e)
           Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
       }
   }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }

//    private fun openFolderSelector() {
//        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//            type = "application/*"
//            putExtra(Intent.EXTRA_TITLE, "${System.currentTimeMillis()}.xlsx")
//        }
//        //folderPickerLauncher.launch(intent)
//    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun importExcelUsers(uri: Uri) {
        try {
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@AddUserActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@AddUserActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@AddUserActivity, "successfully imported", Toast.LENGTH_SHORT).show()
                        }

                        for (i in readExcelNew.indices) {
                            val map = readExcelNew[i]
                            val name = map[0]?.toString() ?: ""
                            val email = map[1]?.toString() ?: ""
                            val country = map[2]?.toString() ?: ""
                            val phoneNumber = map[3]?.toString() ?: ""
                            val category = map[4]?.toString() ?: ""

                            if (name.isNotEmpty() && name != "Name" && phoneNumber.isNotEmpty()) {
                                val countryCode = if (country.contains("+")) {
                                    country.trim()
                                } else {
                                    country.toDouble().toLong().toString()
                                }
                                val cleanNumber = phoneNumber.filter { it.isDigit() }
                                val user = Users(name, email, countryCode, cleanNumber.toLong().toString(), category)
                                usersFromFileList.add(user)
                            }
                        }
                        if(usersFromFileList.isNotEmpty()){
                            Log.e(TAG, " Users list:: ${usersFromFileList.size}")
                            addUsersBatch(usersFromFileList, AppConstants.EDUCATION_GROUP_ID)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@AddUserActivity, "No data available", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dismissProgDialog()
        }
    }

   /* private fun downloadAllUsersDetails() {
        try {
            showProgDialog()
            readExcelNew.clear()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents != null) {
                        Log.e(TAG, "${documents.size()}")
                        var isHeaderAdded = false
                        val fieldOrderHeader = listOf("Name", "EmailId", "Country", "PhoneNumber", "Category")
                        val fieldOrder = listOf("name", "email_id", "country", "phone_number", "category")
                        for (document in documents) {
                            val membership = document.data
                            if (membership["name"] != "Name") {  // Skip header if it exists
                                val headerMap: MutableMap<Int, Any> = HashMap()
                                val valuesMap: MutableMap<Int, Any> = HashMap()
                                for (i in fieldOrder.indices) {
                                    val field = fieldOrderHeader[i]
                                    val fieldValue = fieldOrder[i]
                                    val value = membership[fieldValue] ?: ""
                                    if (!isHeaderAdded) {
                                        headerMap[i] = field  // Add header (field names) first
                                    }
                                    valuesMap[i] = value  // Add values corresponding to each field
                                }
                                if (!isHeaderAdded) {
                                    readExcelNew.add(headerMap)  // Add header only once
                                    isHeaderAdded = true
                                }
                                readExcelNew.add(valuesMap)  // Add values in the desired order
                            }
                        }
                        dismissProgDialog()
                        openFolderSelector()
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

    private fun writeExcelNew(context: Context, exportExcel: MutableList<Map<Int, Any>>, uri: Uri?) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"))
            val colums = exportExcel[0].size
            for (i in 0 until colums) {
                //set the cell default width to 15 characters
                sheet.setColumnWidth(i, 30 * 256)
            }
            for (i in exportExcel.indices) {
                val row: Row = sheet.createRow(i)
                val integerObjectMap = exportExcel[i]
                for (j in 0 until colums) {
                    val cell = row.createCell(j)
                    cell.setCellValue(integerObjectMap[j].toString())
                }
            }
            val outputStream = context.contentResolver.openOutputStream(uri!!)
            workbook.write(outputStream)
            outputStream!!.flush()
            outputStream.close()
            dismissProgDialog()
            Toast.makeText(context, "File downloaded successfully", Toast.LENGTH_SHORT).show()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e(TAG, "writeExcel: error$e")
            Toast.makeText(context, "export error$e", Toast.LENGTH_SHORT).show()
        }
    }*/

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}