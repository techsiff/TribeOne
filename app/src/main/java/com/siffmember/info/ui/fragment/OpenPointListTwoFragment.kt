package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.databinding.FragmentOpenPointListTwoBinding
import com.siffmember.info.ui.adapter.OpenPointDetailsAdapter
import com.siffmember.info.ui.interfaces.OPFragmentInteractionInterface
import com.siffmember.info.ui.model.OpenPointDetails
import com.siffmember.info.utils.OpenPoints
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.navigation.findNavController
import com.siffmember.info.data.remote.model.openpoints.ReadSheetData

class OpenPointListTwoFragment : BaseFragment(), OpenPointDetailsAdapter.OpenPointDetailListener {

    companion object {
        private const val TAG = "OpenPointListTwoFragment"
    }

    private lateinit var binding: FragmentOpenPointListTwoBinding

    private var recyclerViewAdapter: OpenPointDetailsAdapter? = null
    private var opDetailsList: ArrayList<OpenPointDetails> = ArrayList()
    private var listener: OPFragmentInteractionInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OPFragmentInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }
    private var lastSL = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentOpenPointListTwoBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(OPFragments.OPEN_POINTS_DETAILS_TWO_FRAGMENT, TAG)
        setupAdapter()
        binding.apply {
            try{
                sheetTitle.text = OpenPoints.getOpenPointsList().opName
            }catch (e: Exception){
                e.printStackTrace()
            }
            addOpenPointsBtn.setOnClickListener {
                OpenPoints.setIsEditOPDetails(false)
                requireView().findNavController().navigate(R.id.action_twoFragment_to_threeFragment)
            }
        }
    }

    private fun setupAdapter(){
        recyclerViewAdapter = OpenPointDetailsAdapter(opDetailsList, this)
        binding.opListDetail.layoutManager = LinearLayoutManager(requireActivity())
        binding.opListDetail.adapter = recyclerViewAdapter
    }

    override fun onResume() {
        super.onResume()
        readOpenPoints()
    }

    private fun readOpenPoints(){
        try{
            showProgDialog()
            readSheet(OpenPoints.getOpenPointsList().opId!!,"Sheet1")
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun readSheet(spreadsheetId: String, range: String) {
        RetrofitInstanceFunction.api.readSheetData(spreadsheetId, range)
            .enqueue(object : Callback<List<ReadSheetData>> {
                override fun onResponse(call: Call<List<ReadSheetData>>, response: Response<List<ReadSheetData>>) {
                    if (response.isSuccessful) {
                        try{
                            opDetailsList.clear()
                            val data = response.body()
                            data!!.forEach {
                                if(it.status != "Closed") {
                                    val opDetails = OpenPointDetails(
                                        it.rowIndex,
                                        it.sl,
                                        it.description,
                                        it.who,
                                        it.remarks,
                                        it.status
                                    )
                                    opDetailsList.add(opDetails)
                                }
                                Log.e("SheetData", "${it.sl}")
                                lastSL = it.sl
                            }
                            if(lastSL.isNotEmpty()) {
                                val nextSL = lastSL.toInt() + 1
                                OpenPoints.setNextSL("$nextSL")
                            } else {
                                OpenPoints.setNextSL("1")
                            }
                            if(opDetailsList.isEmpty()){
                                binding.opListDetail.visibility = View.GONE
                                binding.noOp.visibility = View.VISIBLE
                            } else {
                                binding.opListDetail.visibility = View.VISIBLE
                                binding.noOp.visibility = View.GONE
                                setupAdapter()
                            }
                        }catch (e: Exception){
                            e.printStackTrace()
                        }

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

    override fun onClickNext(opDetails: OpenPointDetails?) {
        OpenPoints.setOpenPointsDetails(opDetails!!)
        OpenPoints.setIsEditOPDetails(true)
        requireView().findNavController().navigate(R.id.action_twoFragment_to_threeFragment)
    }
}