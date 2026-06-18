package com.siffmember.info.ui.activity

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
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.utils.AppConstants
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.Query
import com.siffmember.info.databinding.ActivityCallHistoryAllBinding
import com.siffmember.info.ui.adapter.AllCallHistoryAdapter
import com.siffmember.info.ui.model.UpdateUsersCallLog
import com.siffmember.info.utils.Utils
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class AllUsersCallHistoryActivity : BaseActivity() {

    companion object {
        var TAG = "AllUsersCallHistoryActivity"
    }

    private lateinit var binding: ActivityCallHistoryAllBinding
    private lateinit var db: FirebaseFirestore

    private var recyclerViewAdapter: AllCallHistoryAdapter? = null
    private var callHistoryList: ArrayList<UpdateUsersCallLog> = ArrayList()
    private var filteredItems: List<UpdateUsersCallLog> = emptyList()
    private lateinit var folderPickerLauncher : ActivityResultLauncher<Intent>
    private var readExcelNew: MutableList<Map<Int, Any>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallHistoryAllBinding.inflate(layoutInflater)
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
            binding.contentLayout.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        setupSearchView()
        setupAdapter()
        updateList(callHistoryList)
        db = Firebase.firestore
        binding.downloadHistory.setOnClickListener {
            downloadAllUsersHistoryDetails()
        }
        fetchAllCallHistory { historyList ->
            callHistoryList.clear()
            callHistoryList.addAll(historyList)
            if (callHistoryList.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.callHistoryList.visibility = View.GONE
            } else {
                binding.tvEmpty.visibility = View.GONE
                binding.callHistoryList.visibility = View.VISIBLE
               // setupAdapter()
                updateList(callHistoryList)
            }
        }

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    showProgDialog()
                    writeExcelNew(this@AllUsersCallHistoryActivity, readExcelNew, uri)
                }
            }
        }
    }

    private fun updateList(users: List<UpdateUsersCallLog>) {
        filteredItems = users
        recyclerViewAdapter!!.updateList(users)
    }

    private fun setupSearchView() {
        binding.searchViewHistory.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                val filtered = if (query.isEmpty()) {
                    callHistoryList
                } else {
                    callHistoryList.filter {
                        it.fromUserName!!.contains(query, ignoreCase = true) ||
                                it.fromUserPhoneNumber!!.contains(query) ||
                                it.toUserName!!.contains(query, ignoreCase = true) ||
                                it.toUserPhoneNumber!!.contains(query)
                    }
                }
                updateList(filtered)
            }

            override fun afterTextChanged(s: Editable?) {
                //
            }
        })
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.callHistoryList.layoutManager = layoutManager
        recyclerViewAdapter = AllCallHistoryAdapter(callHistoryList)
        binding.callHistoryList.adapter = recyclerViewAdapter
    }

    fun fetchAllCallHistory(onResult: (List<UpdateUsersCallLog>) -> Unit) {
        val postsRef = db.collection(AppConstants.TABLE_USERS_CALL_HISTORY_DETAILS)
        postsRef.orderBy("timestamp", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Read Firestore data safely
                        val model = doc.toObject(UpdateUsersCallLog::class.java) ?: return@mapNotNull null
                        UpdateUsersCallLog(
                            fromUserName = model.fromUserName?: "",
                            fromUserPhoneNumber = model.fromUserPhoneNumber?: "",
                            toUserName = model.toUserName?: "",
                            toUserPhoneNumber = model.toUserPhoneNumber?: "",
                            duration = model.duration?: "0",
                            timestamp = model.timestamp?: "0"
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    /*fun fetchLast30DaysCallHistory(
        onResult: (List<UpdateUsersCallLog>) -> Unit
    ) {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        db.collection(AppConstants.TABLE_USERS_CALL_HISTORY_DETAILS)
            .whereGreaterThanOrEqualTo("timestamp", thirtyDaysAgo)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Read Firestore data safely
                        val model = doc.toObject(UpdateUsersCallLog::class.java) ?: return@mapNotNull null
                        UpdateUsersCallLog(
                            fromUserName = model.fromUserName?: "",
                            fromUserPhoneNumber = model.fromUserPhoneNumber?: "",
                            toUserName = model.toUserName?: "",
                            toUserPhoneNumber = model.toUserPhoneNumber?: "",
                            duration = model.duration?: "0",
                            timestamp = model.timestamp?: "0"
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }*/
// Function to launch the file picker
    private fun openFolderSelector() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_TITLE, "${System.currentTimeMillis()}.xlsx")
        }
        folderPickerLauncher.launch(intent)
    }

    private fun downloadAllUsersHistoryDetails() {
        try {
            showProgDialog()
            readExcelNew.clear()
            val docRef = db.collection(AppConstants.TABLE_USERS_CALL_HISTORY_DETAILS)
            docRef.orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnSuccessListener { documents ->
                    try{
                        if (documents != null) {
                            Log.e(TAG, "${documents.size()}")
                            var isHeaderAdded = false
                            val fieldOrderHeader = listOf("FromUserName", "FromUserPhoneNumber","ToUserName", "ToUserPhoneNumber", "Duration", "DateTime")
                            val fieldOrder = listOf("fromUserName", "fromUserPhoneNumber", "toUserName", "toUserPhoneNumber", "duration", "timestamp")
                            for (document in documents) {
                                val callHistory = document.data
                                if (callHistory["name"] != "Name") {  // Skip header if it exists
                                    val headerMap: MutableMap<Int, Any> = HashMap()
                                    val valuesMap: MutableMap<Int, Any> = HashMap()
                                    for (i in fieldOrder.indices) {
                                        val field = fieldOrderHeader[i]
                                        val value = when (val fieldValue = fieldOrder[i]) {
                                            "timestamp" -> {
                                                val time = callHistory[fieldValue] ?: "0"
                                                Utils.getDateTime(time.toString())
                                            }
                                            "duration" -> {
                                                val time = callHistory[fieldValue] ?: "0"
                                                Utils.getCallDuration(time.toString())
                                            }
                                            else -> {
                                                callHistory[fieldValue] ?: ""
                                            }
                                        }
                                        //val value = callHistory[fieldValue] ?: ""
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
                    }catch (e: Exception){
                        e.printStackTrace()
                        dismissProgDialog()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("getAllUsersData", "get failed with ", exception)
                    dismissProgDialog()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            dismissProgDialog()
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