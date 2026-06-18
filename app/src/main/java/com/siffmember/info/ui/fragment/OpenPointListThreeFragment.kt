package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.openpoints.AppendSheetRequest
import com.siffmember.info.data.remote.model.openpoints.AppendSheetResponse
import com.siffmember.info.data.remote.model.openpoints.ReadSheetData
import com.siffmember.info.data.remote.model.openpoints.UpdateSheetResponse
import com.siffmember.info.data.remote.model.openpoints.UpdateSheetRequest
import com.siffmember.info.databinding.FragmentOpenPointListThreeBinding
import com.siffmember.info.ui.interfaces.OPFragmentInteractionInterface
import com.siffmember.info.utils.OpenPoints
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OpenPointListThreeFragment : BaseFragment() {

    companion object {
        private const val TAG = "OpenPointListThreeFragment"
    }

    private lateinit var binding: FragmentOpenPointListThreeBinding
    private var listener: OPFragmentInteractionInterface? = null

    private var description = ""
    private var whoDoIt = ""
    private var remarks = ""
    private var lastSL = "0"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OPFragmentInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOpenPointListThreeBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(OPFragments.OPEN_POINTS_DETAILS_THREE_FRAGMENT, TAG)

        binding.apply {

            try{
                addOpTitle.text = OpenPoints.getOpenPointsList().opName
                if(OpenPoints.getIsEditOPDetails()){
                    btnOpAdd.visibility = View.GONE
                    btnOpClose.visibility = View.VISIBLE
                    binding.descEdit.setText(OpenPoints.getOpenPointsDetails().description)
                    binding.wdEditLn.setText(OpenPoints.getOpenPointsDetails().whoWillDoIt)
                    binding.remarksEdit.setText(OpenPoints.getOpenPointsDetails().remarks)

                    binding.descEdit.isEnabled = false
                    binding.wdEditLn.isEnabled = false
                    binding.remarksEdit.isEnabled = false

                } else {
                    btnOpAdd.visibility = View.VISIBLE
                    btnOpClose.visibility = View.GONE
                }
            }catch (e: Exception){
                e.printStackTrace()
            }

            btnOpAdd.setOnClickListener {
                appendNewData()
            }

            btnOpClose.setOnClickListener {
               updateStatus()
            }
        }
    }

    private fun validate(): Boolean{
        if(binding.descEdit.text.toString().isNotEmpty()){
            description = binding.descEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(),"Please enter description", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.wdEditLn.text.toString().isNotEmpty()){
            whoDoIt = binding.wdEditLn.text.toString()
        } else {
            Toast.makeText(requireActivity(),"Please enter the name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.remarksEdit.text.toString().isNotEmpty()){
            remarks = binding.remarksEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(),"Please enter remarks", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun appendNewData(){
        try {
            if(validate()) {
                val newData: List<List<String>> = listOf(
                    listOf(OpenPoints.getNextSL(), description, whoDoIt, remarks, "Open")
                )
                val sheetID = OpenPoints.getOpenPointsList().opId
                showProgDialog()
                appendDataToSheet(sheetID!!, "Sheet1", newData)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun appendDataToSheet(spreadsheetId: String, sheetName: String, newData: List<List<String>>) {
        val request = AppendSheetRequest(spreadsheetId, sheetName, newData)

        RetrofitInstanceFunction.api.appendDataToSheet(request)
            .enqueue(object : Callback<AppendSheetResponse> {
                override fun onResponse(call: Call<AppendSheetResponse>, response: Response<AppendSheetResponse>) {
                    if (response.isSuccessful) {
                        binding.descEdit.setText("")
                        binding.wdEditLn.setText("")
                        binding.remarksEdit.setText("")
                        Toast.makeText(requireActivity(),"Open point successfully added", Toast.LENGTH_LONG).show()
                        readSheetData(OpenPoints.getOpenPointsList().opId!!,"Sheet1")
                    } else {
                        Log.e(TAG, "Failed to close data: ${response.errorBody()?.string()}")
                        dismissProgDialog()
                        Toast.makeText(requireActivity(),"Open point Failed to add", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<AppendSheetResponse>, t: Throwable) {
                    Log.e("AppendData", "Error: ${t.message}")
                    dismissProgDialog()
                    Toast.makeText(requireActivity(),"Open point Failed to add", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun updateStatus(){
        try {
            val spreadsheetId = OpenPoints.getOpenPointsList().opId
            val row = OpenPoints.getOpenPointsDetails().slNo!!.toInt() + 1
            val rowToUpdate = "Sheet1!A$row"  // Range of the row you want to update
            val newData: List<List<String>> = listOf(
                listOf("${OpenPoints.getOpenPointsDetails().slNo}",
                    "${OpenPoints.getOpenPointsDetails().description}",
                    "${OpenPoints.getOpenPointsDetails().whoWillDoIt}",
                    "${OpenPoints.getOpenPointsDetails().remarks}",
                    "Closed"))
            showProgDialog()
            updateData(spreadsheetId!!, rowToUpdate, newData)

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun updateData(spreadsheetId: String, sheetName: String, newData: List<List<String>>) {
        val request = UpdateSheetRequest(spreadsheetId, sheetName, newData)

        RetrofitInstanceFunction.api.updateDataToSheet(request)
            .enqueue(object : Callback<UpdateSheetResponse> {
                override fun onResponse(call: Call<UpdateSheetResponse>, response: Response<UpdateSheetResponse>) {
                    dismissProgDialog()
                    if (response.isSuccessful) {
                        binding.btnOpClose.visibility = View.GONE
                        Toast.makeText(requireActivity(),"Open point successfully closed", Toast.LENGTH_LONG).show()
                    } else {
                        binding.btnOpClose.visibility = View.VISIBLE
                        Toast.makeText(requireActivity(),"Open point Failed to close", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<UpdateSheetResponse>, t: Throwable) {
                   // Log.e("UpdateData", "Error: ${t.message}")
                    dismissProgDialog()
                    Toast.makeText(requireActivity(),"Open point Failed to close", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun readSheetData(spreadsheetId: String, range: String) {
        RetrofitInstanceFunction.api.readSheetData(spreadsheetId, range)
            .enqueue(object : Callback<List<ReadSheetData>> {
                override fun onResponse(call: Call<List<ReadSheetData>>, response: Response<List<ReadSheetData>>) {
                    if (response.isSuccessful) {
                        val data = response.body()
                        data!!.forEach {
                            lastSL = it.sl
                        }
                        val nextSL = lastSL.toInt() + 1
                        OpenPoints.setNextSL("$nextSL")
                    } else {
                        Log.e("SheetData", "Failed to read sheet data")
                        Toast.makeText(requireActivity(),"Failed to read sheets: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                    dismissProgDialog()
                }

                override fun onFailure(call: Call<List<ReadSheetData>>, t: Throwable) {
                    Log.e("SheetData", "Error: ${t.message}")
                    dismissProgDialog()
                }
            })
    }
}