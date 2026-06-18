package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAddMembershipParamsBinding
import com.siffmember.info.ui.adapter.MembershipParamListAdapter
import com.siffmember.info.ui.fragment.AddMembershipParamsBottomSheetFragment
import com.siffmember.info.ui.model.MembershipParamField
import com.siffmember.info.ui.model.MembershipParamListItem
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.Utils

class AddMembershipParamsActivity : BaseActivity(), MembershipParamListAdapter.ParameterListener, AddMembershipParamsBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AddMembershipParamsActivity"
    }

    private lateinit var binding: ActivityAddMembershipParamsBinding
    private lateinit var db: FirebaseFirestore
    private var paramsList: ArrayList<MembershipParamField> = ArrayList()
    private var recyclerViewAdapter: MembershipParamListAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMembershipParamsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.paramListLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        setupAdapter()

        binding.addParam.visibility =
            if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE
        binding.addParam.setOnClickListener {
            if(Utils.isNetworkAvailable(this@AddMembershipParamsActivity)) {
                val bottomSheetFragment = AddMembershipParamsBottomSheetFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            } else {
                Toast.makeText(this@AddMembershipParamsActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        fetchAllParams()
    }

    private fun fetchAllParams(){
        showProgDialog()
        fetchAllParameters{ params ->
            paramsList.clear()
            paramsList.addAll(params)
            updateList(params)
        }
    }
    private fun updateList(contacts: List<MembershipParamField>) {

        val grouped = contacts.groupBy { it.category }

        // ✅ Sort categories: Personal Info first, then others alphabetically
        val sortedGrouped = grouped.toList()
            .sortedWith(compareBy<Pair<String, List<MembershipParamField>>> {
                if (it.first == "Personal Info") 0 else 1
            }.thenBy { it.first.lowercase() })

        val items = mutableListOf<MembershipParamListItem>()

        sortedGrouped.forEach { (category, paramsCategory) ->

            items.add(MembershipParamListItem.Header(category))

            val sortedParams = paramsCategory.sortedBy { it.paramName }

            items.addAll(sortedParams.map {
                MembershipParamListItem.ParamsItem(it)
            })
        }

        recyclerViewAdapter?.updateList(items)
    }
   /* private fun updateList(contacts: List<MembershipParamField>) {
        val grouped = contacts
            .groupBy { it.category }
            //.toSortedMap(String.CASE_INSENSITIVE_ORDER)
        val items = mutableListOf<MembershipParamListItem>()
        grouped.forEach { (state, paramsCategory) ->
            items.add(MembershipParamListItem.Header(state))
            val sortedParams = paramsCategory.sortedBy { it.paramName }
            items.addAll(sortedParams.map { MembershipParamListItem.ParamsItem(it) })
        }
        recyclerViewAdapter!!.updateList(items)
    }*/

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.paramList.layoutManager = layoutManager
        recyclerViewAdapter = MembershipParamListAdapter(emptyList(), this)
        binding.paramList.adapter = recyclerViewAdapter
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

    private fun addParams(paramName: String, isMandatory: Boolean, category: String){
        try{
            showProgDialog()
           val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_PARAMS).document()
            val params = MembershipParamField(docRef.id, paramName, category, isMandatory)
            docRef.set(params)
                .addOnSuccessListener {
                    Toast.makeText(this@AddMembershipParamsActivity,"Parameter added successfully", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                    fetchAllParams()
                    updateNewParamForAllMembers(paramName, category)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@AddMembershipParamsActivity,"Failed to add parameter. Please try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }

        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

   /* private fun updateParamInAllMembers(paramName: String) {
        val memberCollection = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
        memberCollection.get().addOnSuccessListener { snapshot ->
            val documents = snapshot.documents
            val chunkedList = documents.chunked(500)
            chunkedList.forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { document ->
                    val docRef = memberCollection.document(document.id)
                    batch.update(docRef, paramName, "")
                }
                batch.commit()
            }
        }
    }*/

    private fun updateNewParamForAllMembers(newParamName: String, category: String) {
        val membersRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
        membersRef.get().addOnSuccessListener { querySnapshot ->
            val documents = querySnapshot.documents
            var batch = db.batch()
            var count = 0
            for (document in documents) {
                val memberRef = document.reference
                // If params stored as Map<String, String>
                batch.update(memberRef, "$category.$newParamName", "")
                count++
                // Commit every 500
                if (count == 500) {
                    batch.commit()
                    batch = db.batch()
                    count = 0
                }
            }
            // Commit remaining
            if (count > 0) {
                batch.commit()
            }
            Toast.makeText(this, "All members updated successfully", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteParamFromAllMembers(paramName: String, category: String) {

        val membersRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
        membersRef.get().addOnSuccessListener { querySnapshot ->

            val documents = querySnapshot.documents
            var batch = db.batch()
            var count = 0
            for (document in documents) {
                val memberRef = document.reference
                // Delete nested field
                batch.update(
                    memberRef,
                    "$category.$paramName",
                    FieldValue.delete()
                )
                count++
                // Commit every 500 (Firestore limit)
                if (count == 500) {
                    batch.commit()
                    batch = db.batch()
                    count = 0
                }
            }
            // Commit remaining
            if (count > 0) {
                batch.commit()
            }
            Toast.makeText(this, "Parameter deleted from all members", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteParameter(paramId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_MEMBERSHIP_PARAMS).document(paramId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user details", e)
                onComplete(false)
                dismissProgDialog()
            }
    }

    private fun deleteParamDialog(paramId: String, paramName: String, category: String) {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Parameter Alert")
                .setMessage("Are you sure you want to delete this $paramName parameter?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    showProgDialog()
                    deleteParameter(paramId) { isDeleted ->
                        if (isDeleted) {
                            Toast.makeText(this@AddMembershipParamsActivity, "Parameter deleted successfully", Toast.LENGTH_LONG).show()
                            dialogInterface.dismiss()
                            fetchAllParams()
                            deleteParamFromAllMembers(paramName, category)
                        }
                    }
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onAddParamName(paramName: String, isMandatory: Boolean, category: String) {
        addParams(paramName, isMandatory, category)
    }

    override fun onDeleteParameter(paramDetails: MembershipParamField?) {
        deleteParamDialog(paramDetails!!.paramId, paramDetails.paramName, paramDetails.category)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }


}