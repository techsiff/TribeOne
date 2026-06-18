package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.R
import com.siffmember.info.ui.activity.MembershipAllDetailsActivity
import com.siffmember.info.ui.adapter.NotesDetailsAdapter
import com.siffmember.info.ui.model.NotesDetails
import com.siffmember.info.utils.AppConstants
import kotlin.collections.sortedWith

class MembershipAllDetailsFragment : BaseFragment(), NotesDetailsAdapter.DeleteListener {

    companion object {
        var TAG = "MembershipAllDetailsFragment"
        fun newInstance(categoryName: String, memberId: String, fields: HashMap<String, String>): MembershipAllDetailsFragment {
            val fragment = MembershipAllDetailsFragment()
            val bundle = Bundle()
            bundle.putString("categoryName", categoryName)
            bundle.putString("memberId", memberId)
            bundle.putSerializable("fields", fields)
            fragment.arguments = bundle
            return fragment
        }
    }
    private lateinit var db: FirebaseFirestore
    private lateinit var notesList: RecyclerView
    private lateinit var noNotesData: TextView
    private lateinit var notesEdit: AppCompatEditText
    private var memberID = ""
    private var recyclerViewAdapter: NotesDetailsAdapter? = null
    private var notesDetailsList: ArrayList<NotesDetails> = ArrayList()
    val fieldPriority = listOf(
        "Name",
        "Email Id",
        "Country Code",
        "Phone Number",
        "Address"
    )
    @Suppress("UNCHECKED_CAST")
    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val categoryName = arguments?.getString("categoryName") ?: ""
        memberID = arguments?.getString("memberId") ?: ""
        db = Firebase.firestore
        val view = if(categoryName == "Notes") {
            inflater.inflate(R.layout.fragment_membership_details_three, container, false)
        } else {
            inflater.inflate(R.layout.fragment_membership_all_details, container, false)

        }
        if(categoryName == "Notes") {
            Log.e(TAG, "categoryName $categoryName")
            notesList = view.findViewById(R.id.notes_list)
            noNotesData = view.findViewById(R.id.no_notes_data)
            notesEdit = view.findViewById(R.id.notes_edit)

            setupAdapter()
            getNotes()
        } else {
            Log.e(TAG, "categoryName $categoryName")
            val layout = view.findViewById<LinearLayout>(R.id.categoryContainer)
            val header = view.findViewById<TextView>(R.id.txtHeader)
            val fields = arguments?.getSerializable("fields") as HashMap<String, String>
            val title = TextView(requireContext())
            title.text = categoryName
            title.textSize = 20f
            title.setTypeface(null, Typeface.BOLD)
            layout.addView(title)
             Log.e(TAG, "fields $fields")
            //Log.e(TAG, "categoryName $categoryName")
            header.text = categoryName

            val sortedFields = fields
                .toList()
                .sortedWith(compareBy<Pair<String, String>> {
                    val index = fieldPriority.indexOf(it.first)
                    if (index == -1) Int.MAX_VALUE else index
                }.thenBy { it.first.lowercase() })
                .toMap()


            sortedFields.forEach { (key, value) ->
                val row = inflater.inflate(R.layout.item_membership_field_row, layout, false)
                row.findViewById<TextView>(R.id.txtLabel).text = key
                row.findViewById<TextView>(R.id.txtValue).text = value
                layout.addView(row)
            }
        }

        return view
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.setPadding(0, 0, 0, imeHeight)
            insets
        }
    }

    private fun setupAdapter(){
        val sortedList = notesDetailsList.sortedByDescending { it.dateTime?.toLong() }
        recyclerViewAdapter = NotesDetailsAdapter(sortedList, this)
        notesList.layoutManager = LinearLayoutManager(requireActivity())
        notesList.adapter = recyclerViewAdapter
    }

    private fun getNotes(){
        try{
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(memberID).collection(AppConstants.TABLE_NOTES_DETAILS)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0){
                            //Toast.makeText(requireActivity(),"Membership notes not available.", Toast.LENGTH_LONG).show()
                            notesList.visibility = View.GONE
                            noNotesData.visibility = View.VISIBLE
                        } else {
                            notesList.visibility = View.VISIBLE
                            noNotesData.visibility = View.GONE
                            val noteDetail = document.toObjects<NotesDetails>()
                            notesDetailsList = noteDetail as ArrayList<NotesDetails>
                            setupAdapter()
                            notesEdit.visibility = View.GONE
                        }
                        dismissProgDialog()
                    } else {
                        Log.e(TAG, "No such document")
                       // Toast.makeText(requireActivity(),"Membership notes not available.", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                        notesList.visibility = View.GONE
                        noNotesData.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "get failed with ", exception)
                    dismissProgDialog()
                }
        } catch(e: Exception){
            e.printStackTrace()
        }
    }

    private fun addNotes(notesDetails: NotesDetails){
        try{
            db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(memberID).collection(AppConstants.TABLE_NOTES_DETAILS).document(notesDetails.dateTime!!)
                .set(notesDetails)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    dismissProgDialog()
                    Toast.makeText(requireActivity(),"Notes added successfully", Toast.LENGTH_LONG).show()
                    notesEdit.setText("")
                    (requireActivity() as? MembershipAllDetailsActivity)?.onNotesAdded()
                    if(notesDetailsList.size >= 20){
                        val sortedList = notesDetailsList.sortedByDescending { it.dateTime?.toLong() }
                        val notes = sortedList[sortedList.size - 1]
                        Log.e(TAG, "$notes")
                        deleteLastNotes(notes)
                    } else {
                        getNotes()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(requireActivity(),"Failed to add notes please try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteLastNotes(notesDetails: NotesDetails?){
        showProgDialog()
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(memberID).collection(AppConstants.TABLE_NOTES_DETAILS).document(notesDetails!!.dateTime!!)
            .delete()
            .addOnSuccessListener {
                Log.e(TAG, "DocumentSnapshot successfully deleted!")
                getNotes()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
            }
    }

    fun onAddNotesClicked() {
        if (::notesEdit.isInitialized) {
            notesEdit.visibility = View.VISIBLE
        }
    }

    fun onSaveNotesClicked() {
        if (::notesEdit.isInitialized) {
            if (notesEdit.text.toString().isNotEmpty()) {
                showProgDialog()
                val currentTimestamp = System.currentTimeMillis()
                val notesDetails = NotesDetails("$currentTimestamp", notesEdit.text.toString())
                addNotes(notesDetails)
            } else {
                Toast.makeText(requireActivity(), "Please enter notes.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDeleteDevice(notesDetails: NotesDetails?) {
        deleteNotesDialog(notesDetails)
    }

    private fun deleteNotesDialog(notesDetails: NotesDetails?){
        try{
            android.app.AlertDialog.Builder(requireActivity())
                .setTitle("Delete Notes Alert")
                .setMessage("Are you sure you want to delete this notes?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteNotes(notesDetails)
                    dialogInterface.dismiss()
                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteNotes(notesDetails: NotesDetails?){
        showProgDialog()
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS).document(memberID).collection(AppConstants.TABLE_NOTES_DETAILS).document(notesDetails!!.dateTime!!)
            .delete()
            .addOnSuccessListener {
                Log.e(TAG, "DocumentSnapshot successfully deleted!")
                Toast.makeText(requireActivity(),"Notes deleted successfully", Toast.LENGTH_LONG).show()
                getNotes()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
                Toast.makeText(requireActivity(),"Notes deleting failed try again!", Toast.LENGTH_LONG).show()
            }
    }

}
