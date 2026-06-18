package com.siffmember.info.ui.activity

import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityMemberSearchBinding
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.ExcelUtil
import com.siffmember.info.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.ss.util.WorkbookUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class MembershipSearchActivity : BaseActivity() {

    companion object {
        var TAG = "MembershipSearchActivity"
    }

    private lateinit var binding: ActivityMemberSearchBinding
    private lateinit var db: FirebaseFirestore
    private var mName = ""
    private var mCity = ""
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    private lateinit var folderPickerLauncher : ActivityResultLauncher<Intent>
    private var readExcelNew: MutableList<Map<Int, Any>> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.upDownMember) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore

        binding.apply {

            if(sharedPref.getBoolean(AppConstants.IS_ADMIN, false)){
                upDownMember.visibility = View.VISIBLE
            } else {
                upDownMember.visibility = View.GONE
            }

            btnAddMember.setOnClickListener {
                val intent = Intent(this@MembershipSearchActivity, AddMembershipDetailsActivity::class.java)
                intent.putExtra("memberId", "")
                startActivity(intent)
            }
            btnAddParam.setOnClickListener {
                startActivity(Intent(this@MembershipSearchActivity, AddMembershipParamsActivity::class.java))
            }
            btnUploadMember.setOnClickListener {
                if(Utils.isNetworkAvailable(this@MembershipSearchActivity)) {
                    openFilePicker()
                } else {
                    Toast.makeText(this@MembershipSearchActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }
            }
            downloadMember.setOnClickListener {
                if(Utils.isNetworkAvailable(this@MembershipSearchActivity)) {
                    getAllMembershipData()
                } else {
                    Toast.makeText(this@MembershipSearchActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }
            }
            searchNumber.setOnClickListener {
                if(Utils.isNetworkAvailable(this@MembershipSearchActivity)) {
                    if(numberEdit.text!!.toString().isNotEmpty()){
                        //getMembershipData(numberEdit.text!!.toString())
                        searchByParam(numberEdit.text!!.toString())
                    } else {
                        Toast.makeText(this@MembershipSearchActivity,"Please enter phone number last 4 digit.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@MembershipSearchActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }
            }

            searchNameCity.setOnClickListener {
                if(Utils.isNetworkAvailable(this@MembershipSearchActivity)) {
                    if(validateSearch()) {
                        getMembershipDataWithName(mName, mCity)
                    }
                } else {
                    Toast.makeText(this@MembershipSearchActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }
            }
        }

        fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    showProgDialog()
                    importExcelMembership(uri)
                }
            }
        }

        folderPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    showProgDialog()
                    writeExcelNew(this@MembershipSearchActivity, readExcelNew, uri)
                }
            }
        }
    }

    private fun validateSearch(): Boolean{
        if(binding.nameEdit.text!!.toString().isNotEmpty() ) {
            mName = binding.nameEdit.text!!.toString()
        } else {
            Toast.makeText(this@MembershipSearchActivity,"Please enter member name.", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.cityEdit.text!!.toString().isNotEmpty() ) {
            mCity = binding.cityEdit.text!!.toString()
        } else {
            Toast.makeText(this@MembershipSearchActivity,"Please enter member city.", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun getAllMembershipData() {
        try {
            showProgDialog()
            readExcelNew.clear()
            val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents == null || documents.isEmpty) {
                        dismissProgDialog()
                        return@addOnSuccessListener
                    }
                    val categoryFieldMap = linkedMapOf<String, MutableList<String>>()
                    for (document in documents) {
                        val membership = document.data
                        for ((categoryName, categoryValue) in membership) {
                            val categoryMap = categoryValue as? Map<*, *> ?: continue
                            val fieldList = categoryFieldMap.getOrPut(categoryName) {
                                mutableListOf()
                            }
                            for ((fieldKey, _) in categoryMap) {
                                val field = fieldKey.toString()
                                if (field.equals("PhoneLast4", true)) continue
                                if (!fieldList.contains(field)) {
                                    fieldList.add(field)
                                }
                            }
                        }
                    }
                    val orderedCategoryMap = linkedMapOf<String, MutableList<String>>()
                    // Add Personal Info first (if exists)
                    categoryFieldMap["Personal Info"]?.let {
                        orderedCategoryMap["Personal Info"] = it
                    }
                    // Add remaining categories
                    for ((key, value) in categoryFieldMap) {
                        if (key != "Personal Info") {
                            orderedCategoryMap[key] = value
                        }
                    }
                    val categoryRow = mutableMapOf<Int, Any>()
                    val headerRow = mutableMapOf<Int, Any>()
                    var columnIndex = 0
                    for ((categoryName, fieldList) in orderedCategoryMap) {
                        for (field in fieldList) {
                            categoryRow[columnIndex] = categoryName
                            headerRow[columnIndex] = field
                            columnIndex++
                        }
                    }
                    readExcelNew.add(categoryRow)
                    readExcelNew.add(headerRow)
                    for (document in documents) {
                        val membership = document.data
                        val valueRow = mutableMapOf<Int, Any>()
                        var dataColumn = 0
                        for ((categoryName, fieldList) in orderedCategoryMap) {
                            val categoryMap = membership[categoryName] as? Map<*, *>
                            for (field in fieldList) {
                                val value = categoryMap?.get(field) ?: ""
                                valueRow[dataColumn++] = value
                            }
                        }
                        readExcelNew.add(valueRow)
                    }
                    dismissProgDialog()
                    openFolderSelector()
                }
                .addOnFailureListener { exception ->
                    Log.e("getAllMembershipData", "Error", exception)
                    dismissProgDialog()
                }

        } catch (e: Exception) {
            e.printStackTrace()
            dismissProgDialog()
        }
    }

    private fun getMembershipDataWithName(name: String, city: String) {
        showProgDialog()
        val nameFieldPath = "Personal Info.Name"
        val cityFieldPath = "Personal Info.City"
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
            .whereEqualTo(nameFieldPath, name)
            .whereEqualTo(cityFieldPath, city)
            .get()
            .addOnSuccessListener { querySnapshot ->
                dismissProgDialog()
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Membership data not available.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val document = querySnapshot.documents[0]
                val data = document.data
                if (data != null) {
                    val intent = Intent(this, MembershipAllDetailsActivity::class.java)
                    intent.putExtra("memberId", document.id)
                    startActivity(intent)
                    binding.numberEdit.setText("")
                }
            }
            .addOnFailureListener {
                dismissProgDialog()
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun searchByParam(value: String) {
        showProgDialog()
        val fieldPath = "Personal Info.PhoneLast4"
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
            .whereEqualTo(fieldPath, value)
            .get()
            .addOnSuccessListener { querySnapshot ->
                dismissProgDialog()
                if (querySnapshot.isEmpty) {
                    Toast.makeText(this, "Membership data not available.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val document = querySnapshot.documents[0]
                val data = document.data
                if (data != null) {
                    val intent = Intent(this, MembershipAllDetailsActivity::class.java)
                    intent.putExtra("memberId", document.id)
                    startActivity(intent)
                    binding.numberEdit.setText("")
                }
            }
            .addOnFailureListener {
                dismissProgDialog()
                Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }

    // Function to launch the file picker
    private fun openFolderSelector() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/*"
            putExtra(Intent.EXTRA_TITLE, "${System.currentTimeMillis()}.xlsx")
        }
        folderPickerLauncher.launch(intent)
    }

    private fun importExcelMembership(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val excelData = ExcelUtil.readMembershipExcel(this@MembershipSearchActivity, uri)
                if (excelData.size < 3) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MembershipSearchActivity, "Invalid Excel format", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val categoryRow = excelData[0]
                val headerRow = excelData[1]
                var maxColumn = 0
                for (row in excelData) {
                    val rowMax = row.keys.maxOrNull() ?: 0
                    if (rowMax > maxColumn) {
                        maxColumn = rowMax
                    }
                }

                var batch = db.batch()
                var batchCount = 0

                for (i in 2 until excelData.size) {
                    val row = excelData[i]
                    val user = mutableMapOf<String, MutableMap<String, String>>()
                    var lastCategory = ""
                    for (col in 0..maxColumn) {
                        val rawCategory = categoryRow[col]?.toString()?.trim() ?: ""
                        val fieldName = headerRow[col]?.toString()?.trim() ?: ""
                        if (rawCategory.isNotEmpty()) {
                            lastCategory = rawCategory
                        }
                        val categoryName = lastCategory
                        if (categoryName.isEmpty() || fieldName.isEmpty())
                            continue
                        val value = row[col]?.toString()?.trim() ?: ""
                        val categoryMap = user.getOrPut(categoryName) {
                            mutableMapOf()
                        }
                        categoryMap[fieldName] = value
                        if (fieldName.equals("Phone Number", true)) {
                            val digitsOnly = value.replace(Regex("\\D"), "")
                            val lastFour = if (digitsOnly.length >= 4)
                                digitsOnly.takeLast(4)
                            else
                                digitsOnly
                            categoryMap["PhoneLast4"] = lastFour
                        }
                    }

                    val membershipNumber = user["Personal Info"]?.get("Phone Number")?.trim()
                    if (membershipNumber.isNullOrEmpty()) {
                        //Log.e(TAG, "Skipping row $i because Membership Number is empty")
                        continue
                    }

                    val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
                        .document(membershipNumber)
                    batch.set(docRef, user)
                    batchCount++
                    if (batchCount == 500) {
                        batch.commit().await()
                        batch = db.batch()
                        batchCount = 0
                    }
                }

                if (batchCount > 0) {
                    batch.commit().await()
                }

                withContext(Dispatchers.Main) {
                    dismissProgDialog()
                    Toast.makeText(this@MembershipSearchActivity, "Membership data imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    dismissProgDialog()
                    Toast.makeText(this@MembershipSearchActivity, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun writeExcelNew(context: Context, exportExcel: MutableList<Map<Int, Any>>, uri: Uri?) {
        try {
            if (exportExcel.isEmpty()) return
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName("Sheet1"))
            val columnCount = exportExcel[0].size
            for (i in 0 until columnCount) {
                sheet.setColumnWidth(i, 25 * 256)
            }
            for (i in exportExcel.indices) {
                val row = sheet.createRow(i)
                val rowMap = exportExcel[i]
                for (j in 0 until columnCount) {
                    val cell = row.createCell(j)
                    val value = rowMap[j]?.toString() ?: ""
                    cell.setCellValue(value)
                }
            }
            if (exportExcel.size > 1) {
                val categoryRow = exportExcel[0]
                var startColumn = 0
                var currentCategory = categoryRow[0]?.toString()
                for (col in 1 until columnCount) {
                    val categoryValue = categoryRow[col]?.toString()
                    if (categoryValue != currentCategory) {
                        // Merge previous group ONLY if more than 1 column
                        if (!currentCategory.isNullOrEmpty() && col - 1 > startColumn) {
                            sheet.addMergedRegion(
                                CellRangeAddress(0, 0, startColumn, col - 1)
                            )
                        }
                        startColumn = col
                        currentCategory = categoryValue
                    }
                }
                if (columnCount - 1 > startColumn) {
                    sheet.addMergedRegion(
                        CellRangeAddress(0, 0, startColumn, columnCount - 1)
                    )
                }
            }
            val outputStream = context.contentResolver.openOutputStream(uri!!)
            workbook.write(outputStream)
            outputStream?.flush()
            outputStream?.close()
            workbook.close()
            dismissProgDialog()
            Toast.makeText(context, "File downloaded successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "writeExcel error: $e")
            Toast.makeText(context, "Export error: $e", Toast.LENGTH_SHORT).show()
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