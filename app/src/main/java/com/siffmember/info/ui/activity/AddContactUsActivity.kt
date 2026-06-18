package com.siffmember.info.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAddContactUsBinding
import com.siffmember.info.ui.model.ContactusDetails
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.ExcelUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddContactUsActivity : BaseActivity() {

    companion object {
        var TAG = "AddContactUsActivity"
    }
    private lateinit var binding: ActivityAddContactUsBinding
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    private lateinit var db: FirebaseFirestore
    private var nameFN = ""
    private var nameLN = ""
    private var location = ""
    private var phone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore

        binding.apply {

            if(sharedPref.getBoolean(AppConstants.IS_ADMIN, false)){
                upDownUser.visibility = View.VISIBLE
            } else {
                upDownUser.visibility = View.GONE
            }
            btnUpload.setOnClickListener {
                openFilePicker()
            }

            btnAdd.setOnClickListener {
                if(validate()) {
                    val nameFL = "$nameFN $nameLN"
                    val user = ContactusDetails(nameFL, phone, location)
                    showProgDialog()
                    addUser(user)
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
    }

    private fun validate(): Boolean{
        if(binding.nameEditFn.text.toString().trim().isNotEmpty()){
            nameFN = binding.nameEditFn.text.toString()
        } else {
            Toast.makeText(this@AddContactUsActivity,"Please enter first name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.nameEditLn.text.toString().trim().isNotEmpty()){
            nameLN = binding.nameEditLn.text.toString()
        } else {
            Toast.makeText(this@AddContactUsActivity,"Please enter last name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.cityEdit.text.toString().trim().isNotEmpty()){
            location = binding.cityEdit.text.toString()
        } else {
            Toast.makeText(this@AddContactUsActivity,"Please enter location state/city", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.phoneEdit.text.toString().trim().isNotEmpty()){
            phone = binding.phoneEdit.text.toString()
        } else {
            Toast.makeText(this@AddContactUsActivity,"Please enter phone number", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addUser(user: ContactusDetails){
        try{
            db.collection(AppConstants.TABLE_CONTACT_US_DETAILS).document(user.phoneNumber!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    binding.nameEditFn.setText("")
                    binding.nameEditLn.setText("")
                    binding.cityEdit.setText("")
                    binding.phoneEdit.setText("")
                    nameFN = ""
                    nameLN = ""
                    location = ""
                    phone = ""
                    dismissProgDialog()
                    EducationDetails.setContactUsAdd(true)
                    Toast.makeText(this@AddContactUsActivity,"Contact us details added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    EducationDetails.setContactUsAdd(false)
                    Toast.makeText(this@AddContactUsActivity,"Contact us failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private var usersFromFileList: ArrayList<ContactusDetails> = ArrayList()
    private var sendDataListIndex: Int = 0

    private fun addUsersFromFile(user: ContactusDetails){
        try{
            db.collection(AppConstants.TABLE_CONTACT_US_DETAILS).document(user.phoneNumber!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    if (usersFromFileList.isNotEmpty()) {
                        ++sendDataListIndex
                        if (usersFromFileList.size == sendDataListIndex) {
                            usersFromFileList.clear()
                            sendDataListIndex = 0
                            dismissProgDialog()
                            EducationDetails.setContactUsAdd(true)
                            Toast.makeText(this@AddContactUsActivity,"File uploaded successfully", Toast.LENGTH_LONG).show()
                        } else {
                            addUsersList(sendDataListIndex)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    EducationDetails.setContactUsAdd(false)
                    Toast.makeText(this@AddContactUsActivity,"Failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun addUsersList(index: Int){
        addUsersFromFile(usersFromFileList[index])
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            setType("application/*")
        }
        fileSelectorLauncher.launch(intent)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun importExcelUsers(uri: Uri) {
        try {
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@AddContactUsActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@AddContactUsActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@AddContactUsActivity, "successfully imported", Toast.LENGTH_SHORT).show()
                        }
                        for (i in readExcelNew.indices) {
                            val map = readExcelNew[i]
                            val name = map[0]?.toString() ?: ""
                            val phoneNumber = map[1]?.toString() ?: ""
                            val state = map[2]?.toString() ?: ""
                            if(name.isNotEmpty() && name != "Name") {
                                val user = ContactusDetails(name, phoneNumber.trim(), state)
                                usersFromFileList.add(user)
                            }
                        }
                        if(usersFromFileList.isNotEmpty()){
                            Log.e(TAG, " Users list:: ${usersFromFileList.size}")
                            addUsersList(sendDataListIndex)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@AddContactUsActivity, "No data available", Toast.LENGTH_SHORT).show()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}