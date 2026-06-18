package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityUpdateMembershipDetailsBinding
import com.siffmember.info.ui.model.Membership
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MembershipDetails

class UpdateMembershipDetailsActivity : BaseActivity() {

    companion object {
        var TAG = "UpdateMembershipDetailsActivity"
    }

    private lateinit var binding: ActivityUpdateMembershipDetailsBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateMembershipDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnUpdateLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        binding.apply {

            try {
                val membership = MembershipDetails.getMembershipDetails()

                membershipNumber.text = membership.membershipNumber
                muNameEdit.setText(membership.name)
                muEmailEdit.setText(membership.emailId)
                muAddressEdit.setText(membership.address)
                muPhoneEdit.setText(membership.phoneNumber)
                muCityEdit.setText(membership.city)
                muDOJEdit.setText(membership.joiningDate)
                muCs1Edit.setText(membership.cs1Parameter)
                muCs2Edit.setText(membership.cs2)
                muCs3Edit.setText(membership.cs3)
                muPr1Edit.setText(membership.pr1Parameter)
                muPr2Edit.setText(membership.pr2)
                muPr3Edit.setText(membership.pr3)
                muLD1Edit.setText(membership.ld1)

                btnUpdate.setOnClickListener {
                    showProgDialog()
                    val members = Membership(membership.membershipNumber,
                        muNameEdit.text.toString(),
                        muEmailEdit.text.toString(),
                        muAddressEdit.text.toString(),
                        membership.countryCode,
                        muPhoneEdit.text.toString(),
                        muCityEdit.text.toString(),
                        muDOJEdit.text.toString(),
                        muCs1Edit.text.toString(),
                        muCs2Edit.text.toString(),
                        muCs3Edit.text.toString(),
                        muPr1Edit.text.toString(),
                        muPr2Edit.text.toString(),
                        muPr3Edit.text.toString(),
                        muLD1Edit.text.toString())
                    updateMembers(members)
                }

            }catch (e: Exception){
                e.printStackTrace()
            }

        }
    }

    private fun updateMembers(user: Membership){
        try{
            db.collection(AppConstants.TABLE_MEMBERSHIP_DETAILS).document(user.membershipNumber!!)
                .set(user)
                .addOnSuccessListener {
                    MembershipDetails.setMembershipDetails(user)
                    Toast.makeText(this@UpdateMembershipDetailsActivity,"Member details updated successfully", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@UpdateMembershipDetailsActivity,"Failed to update try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }

        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
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