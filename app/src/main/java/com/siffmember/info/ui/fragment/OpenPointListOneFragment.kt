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
import com.siffmember.info.data.remote.model.openpoints.SheetResponse
import com.siffmember.info.data.remote.model.openpoints.SheetsData
import com.siffmember.info.databinding.FragmentOpenPointListOneBinding
import com.siffmember.info.ui.adapter.OpenPointsListAdapter
import com.siffmember.info.ui.interfaces.OPFragmentInteractionInterface
import com.siffmember.info.ui.model.OpenPointList
import com.siffmember.info.utils.OpenPoints
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.navigation.findNavController

class OpenPointListOneFragment : BaseFragment(), OpenPointsListAdapter.OpenPointsListener {

    companion object {
        private const val TAG = "OpenPointListOneFragment"
    }
    private lateinit var binding: FragmentOpenPointListOneBinding

    private var recyclerViewAdapter: OpenPointsListAdapter? = null
    private var opDetailsList: ArrayList<OpenPointList> = ArrayList()

    private var listener: OPFragmentInteractionInterface? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is OPFragmentInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        binding = FragmentOpenPointListOneBinding.inflate(layoutInflater)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(OPFragments.OPEN_POINTS_DETAILS_ONE_FRAGMENT, TAG)
        setupAdapter()
    }

    override fun onResume() {
        super.onResume()
        getAllOpenPoints()
    }

    private fun setupAdapter(){
        recyclerViewAdapter = OpenPointsListAdapter(opDetailsList, this)
        binding.opList.layoutManager = LinearLayoutManager(requireActivity())
        binding.opList.adapter = recyclerViewAdapter
    }

    private fun getAllOpenPoints(){
        try{
            showProgDialog()
            listSheets(OpenPoints.getDriveFolderId()) { files ->
                opDetailsList.clear()
                dismissProgDialog()
                files.forEach { file ->
                    //Log.e(TAG, "Sheets :: ${file.name}")
                    val openPoints = OpenPointList(file.id, file.name)
                    opDetailsList.add(openPoints)
                }
                if(opDetailsList.isEmpty()){
                    binding.opList.visibility = View.GONE
                    binding.noOp.visibility = View.VISIBLE
                } else {
                    binding.opList.visibility = View.VISIBLE
                    binding.noOp.visibility = View.GONE
                    setupAdapter()
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun listSheets(folderId: String, callback: (List<SheetsData>) -> Unit) {

        RetrofitInstanceFunction.api.listSheetsInFolder(folderId)
            .enqueue(object : Callback<SheetResponse> {
                override fun onResponse(call: Call<SheetResponse>, response: Response<SheetResponse>) {
                    if (response.isSuccessful) {
                        val sheets = response.body()
                        //Log.e(TAG, "Sheets :: ${sheets!!.files.size}")
                        callback(sheets!!.files)
                    } else {
                        Log.e(TAG, "Failed to list sheets")
                        Toast.makeText(requireActivity(),"Failed to list sheets", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                    }
                }

                override fun onFailure(call: Call<SheetResponse>, t: Throwable) {
                    Log.e(TAG, "Error: ${t.message}")
                    dismissProgDialog()
                }
            })

    }


    override fun onClickNext(opDetails: OpenPointList?) {
        OpenPoints.setOpenPointsList(opDetails!!)
        requireView().findNavController().navigate(R.id.action_sendOneFragment_to_twoFragment)
    }
}