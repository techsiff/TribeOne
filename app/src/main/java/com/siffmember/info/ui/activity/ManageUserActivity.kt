package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityManageUserBinding
import com.siffmember.info.ui.adapter.ManageUsersAdapter
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UsersDetails
import com.siffmember.info.utils.Utils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlin.collections.filter

class ManageUserActivity : BaseActivity(), ManageUsersAdapter.ManageUserListener {

    companion object {
        var TAG = "ManageUserActivity"
    }

    private lateinit var binding: ActivityManageUserBinding
    private var recyclerViewAdapter: ManageUsersAdapter? = null
    private lateinit var db: FirebaseFirestore
    private var allUsers: ArrayList<GetUsers> = ArrayList()
    private var adminId: String = ""
    private var category = "All"
    private var readExcelNew: MutableList<Map<Int, Any>> = ArrayList()
    private lateinit var folderPickerLauncher : ActivityResultLauncher<Intent>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageUserBinding.inflate(layoutInflater).apply {
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
            binding.allUsersList.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        val adapter = ArrayAdapter(this, R.layout.spinner_list, Utils.categoryFilter)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        try {
            adminId = sharedPref.getString(AppConstants.USER_ID, null)!!
        }catch (e: Exception){
            e.printStackTrace()
        }
        binding.spinnerCategory.adapter = adapter
        binding.spinnerCategory.setSelection(Utils.categoryFilter.indexOf(category))
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (view != null) {
                    category = Utils.categoryFilter[position]
                    val filtered = if (category == "All") {
                        allUsers
                    } else {
                        allUsers.filter {
                             it.category!!.contains(category, ignoreCase = true)
                        }
                    }
                    binding.usersCount.text = "Total Users: ${filtered.size}"

                    updateList(filtered)
                } else {
                    // handle the case where the view parameter is null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // write code to perform some action
            }
        }
        binding.downloadUser.setOnClickListener {
            downloadAllUsersDetails()
        }
        setupSearchView()
        setupRecyclerView()
        updateList(allUsers)
        fetchAllUsersDetails()
        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    showProgDialog()
                    writeExcelNew(this@ManageUserActivity, readExcelNew, uri)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if(UsersDetails.getIsEditUserDetails()) {
            UsersDetails.setIsEditUserDetails(false)
            category = "All"
            binding.searchView.setText("")
            fetchAllUsersDetails()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAllUsersDetails() {
        try {
            val usersList: ArrayList<GetUsers> = ArrayList()
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents != null) {
                        if(documents.size() != 0){
                            allUsers.clear()
                            usersList.clear()
                            val data = documents.toObjects<GetUsers>()
                            Log.e(TAG, "${data.size}")
                            usersList.addAll(data)
                            val allUser = usersList
                                //.filter { it.phone_number != adminId }
                                .sortedBy { it.name }
                            allUsers.addAll(allUser)
                            updateList(allUsers)
                            binding.usersCount.text = "Total Users: ${allUsers.size}"
                            binding.searchView.visibility = View.VISIBLE
                        } else {
                            binding.usersCount.text = "Total Users: 0"
                            binding.searchView.visibility = View.GONE
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

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val filtered = if (query.isEmpty()) {
                    allUsers
                } else {
                    allUsers.filter {
                        it.name!!.contains(query, ignoreCase = true) ||
                                it.phone_number!!.contains(query) || it.category!!.contains(query, ignoreCase = true)
                    }
                }
                binding.usersCount.text = "Total Users: ${filtered.size}"

                updateList(filtered)
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }
        })
    }

    private fun setupRecyclerView() {
        recyclerViewAdapter = ManageUsersAdapter(
            allUsers,
            this
        )
        binding.allUsersList.layoutManager = LinearLayoutManager(this)
        binding.allUsersList.adapter = recyclerViewAdapter
    }

    private fun updateList(users: List<GetUsers>) {
        recyclerViewAdapter!!.updateList(users)
    }

    override fun onSelectUser(user: GetUsers?) {
        //binding.searchView.setText("")
        UsersDetails.setUsersDetails(user!!)
        startActivity(Intent(this@ManageUserActivity, UpdateUserDetailsActivity::class.java))
    }
    // Function to launch the file picker
    private fun openFolderSelector() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_TITLE, "${System.currentTimeMillis()}.xlsx")
        }
        folderPickerLauncher.launch(intent)
    }

    private fun downloadAllUsersDetails() {
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}