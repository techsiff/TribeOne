package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.openpoints.CreateSheetRequest
import com.siffmember.info.data.remote.model.openpoints.CreateSheetResponse
import com.siffmember.info.databinding.ActivityOpenPointsAddBinding
import com.siffmember.info.utils.OpenPoints
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OpenPointsAddActivity : BaseActivity() {

    companion object {
        var TAG = "OpenPointsAddActivity"
    }

    private lateinit var binding: ActivityOpenPointsAddBinding
    private var title = ""
    private var description = ""
    private var whoDoIt = ""
    private var remarks = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenPointsAddBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        binding.apply {

            btnCreate.setOnClickListener {
                try{
                    if(validate()) {
                        createSheet()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

        }

    }

    private fun validate(): Boolean{
        if(binding.titleEdit.text.toString().isNotEmpty()){
            title = binding.titleEdit.text.toString()
        } else {
            Toast.makeText(applicationContext,"Please enter open point title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.descEdit.text.toString().isNotEmpty()){
            description = binding.descEdit.text.toString()
        } else {
            Toast.makeText(applicationContext,"Please enter description", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.wdEditLn.text.toString().isNotEmpty()){
            whoDoIt = binding.wdEditLn.text.toString()
        } else {
            Toast.makeText(applicationContext,"Please enter the name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.remarksEdit.text.toString().isNotEmpty()){
            remarks = binding.remarksEdit.text.toString()
        } else {
            Toast.makeText(applicationContext,"Please enter remarks", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun createSheet(){
        try{
            showProgDialog()
            val newData: List<List<String>> = listOf(
                listOf("SL", "Description", "Who will do it?", "Remarks", "Status"),
                listOf("1",  description, whoDoIt, remarks, "Open")
            )
            createSpreadsheet(OpenPoints.getDriveFolderId(), title, newData)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createSpreadsheet(folderId: String, title: String, newData: List<List<String>>) {
        val request = CreateSheetRequest(folderId, title, newData)

        RetrofitInstanceFunction.api.createNewSpreadsheetInFolder(request)
            .enqueue(object : Callback<CreateSheetResponse> {
                override fun onResponse(call: Call<CreateSheetResponse>, response: Response<CreateSheetResponse>) {
                    dismissProgDialog()
                    if (response.isSuccessful) {
                        Log.d("CreateSheet", "Spreadsheet Created: ${response.body()?.spreadsheetId}")
                        binding.titleEdit.setText("")
                        binding.descEdit.setText("")
                        binding.wdEditLn.setText("")
                        binding.remarksEdit.setText("")
                        Toast.makeText(applicationContext,"Open point sheet successfully created", Toast.LENGTH_LONG).show()
                    } else {
                        Log.e(TAG, "Failed to list sheets: ${response.errorBody()?.string()}")
                        Toast.makeText(this@OpenPointsAddActivity,"Open point sheet failed to create. Please try again later: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                    }
                }

                override fun onFailure(call: Call<CreateSheetResponse>, t: Throwable) {
                    dismissProgDialog()
                    Log.e("CreateSheet", "Error: ${t.message}")
                    Toast.makeText(this@OpenPointsAddActivity,"Open point sheet failed to create. Please try again later: ${t.message}", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
            })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
