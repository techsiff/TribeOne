package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.auth.FirebaseAuth
import com.siffmember.info.R
import com.siffmember.info.databinding.FragmentAccountBinding
import com.siffmember.info.ui.activity.IntroActivity
import com.siffmember.info.ui.activity.UserCallHistoryActivity
import com.siffmember.info.ui.interfaces.FragmentAccountInteractionInterface
import com.siffmember.info.ui.interfaces.FragmentInteractionInterface
import com.siffmember.info.ui.services.BackupWorker
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.ui.viewmodel.UserDetailsViewModel
import com.siffmember.info.utils.AppConstants
import java.util.concurrent.TimeUnit

class AccountFragment : BaseFragment() {

    companion object {
        private const val TAG = "AccountFragment"
    }

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var userDetailViewModel: UserDetailsViewModel
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel
    private var emailId: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userDetailViewModel = ViewModelProvider(this)[UserDetailsViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
    }

    private var listener: FragmentAccountInteractionInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is FragmentAccountInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root

        binding.apply {
            emailId = sharedPref.getString(AppConstants.USER_EMAIL, "").toString()

            userName.text = sharedPref.getString(AppConstants.USER_NAME, "")
            userEmail.text = emailId
            phoneNumber.text = sharedPref.getString(AppConstants.USER_ID, "")

            if(sharedPref.getBoolean(AppConstants.IS_LOGGEDIN_EP, false)){
                acResetPassword.visibility = View.VISIBLE
            } else {
                acResetPassword.visibility = View.GONE
            }
            acPlayList.setOnClickListener {
                requireView().findNavController().navigate(R.id.action_account_oneFragment_to_twoFragment)
            }

            acSharedPlayList.setOnClickListener {
                requireView().findNavController().navigate(R.id.action_account_oneFragment_to_sharedPlaylistOneFragment)
            }

            acBackupSettings.setOnClickListener {
               // requireView().findNavController().navigate(R.id.action_account_oneFragment_to_settingsBackupFragment)
            }

            acCallHistory.setOnClickListener {
               /* val next = Intent(requireActivity(), UserCallHistoryActivity::class.java)
                next.putExtra("contact_name", sharedPref.getString(AppConstants.USER_NAME, ""))
                next.putExtra("contact_number", sharedPref.getString(AppConstants.USER_ID, ""))
                next.putExtra("screen", "2")
                startActivity(next)*/
            }

            acResetPassword.setOnClickListener {
                passReset()
            }
            acSignout.setOnClickListener {
                singOut()
            }

        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(FragmentsAccounts.SETTINGS_FRAGMENT,"Settings")
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().finish()
                }
            })
    }


    private fun passReset(){
        try {
            AlertDialog.Builder(context)
                .setTitle("Reset Password")
                .setMessage("Are you sure you want to reset your password?")
                .setPositiveButton("Reset") { _, _ ->
                    val auth = FirebaseAuth.getInstance()
                    val emailAddress = emailId
                    auth.sendPasswordResetEmail(emailAddress)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d(TAG, "✅ Password reset email sent to $emailAddress")
                                Toast.makeText(context, "Password reset link sent to your email.", Toast.LENGTH_LONG).show()
                            } else {
                                Log.e(TAG, "❌ Failed to send reset email: ${task.exception?.message}")
                                Toast.makeText(context, "Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun singOut(){
        try {
            AlertDialog.Builder(context)
                .setTitle("Sign Out")
                .setMessage("All locally stored community data will be deleted. Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN, false).commit()
                    sharedPrefEditor.clear()
                    sharedPrefEditor.apply()
                    viewModel.deleteAllCommunities()
                    viewModel.deleteAllCategoryTag()
                    postsViewModel.deleteAllPostMessages()
                    postsViewModel.deleteAllReplyPostMessages()
                    userDetailViewModel.deleteUserDetails()
                    startActivity(Intent(requireActivity(), IntroActivity::class.java))
                    requireActivity().finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }


    /*fun scheduleWeeklyBackup(context: Context, folderUri: Uri) {

        val data = workDataOf("backupFolderUri" to folderUri.toString())

        val weeklyWork = PeriodicWorkRequestBuilder<BackupWorker>(
            7, TimeUnit.DAYS
        )
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "WEEKLY_BACKUP",
                ExistingPeriodicWorkPolicy.UPDATE,
                weeklyWork
            )
    }

    fun scheduleMonthlyBackup(context: Context, folderUri: Uri) {

        val data = workDataOf("backupFolderUri" to folderUri.toString())

        val monthlyWork = PeriodicWorkRequestBuilder<BackupWorker>(
            30, TimeUnit.DAYS
        )
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "MONTHLY_BACKUP",
                ExistingPeriodicWorkPolicy.UPDATE,
                monthlyWork
            )
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}