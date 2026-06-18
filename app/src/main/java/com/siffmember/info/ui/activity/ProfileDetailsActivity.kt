package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.databinding.ActivityProfileDetailsBinding
import com.siffmember.info.ui.backup.core.RoomBackup
import com.siffmember.info.ui.fragment.FragmentsAccounts
import com.siffmember.info.ui.interfaces.AccountInterface
import com.siffmember.info.ui.interfaces.FragmentAccountInteractionInterface

class ProfileDetailsActivity : BaseActivity(), FragmentAccountInteractionInterface{

    companion object {
        var TAG = "ProfileDetailsActivity"
    }
    lateinit var backup: RoomBackup
    private lateinit var binding: ActivityProfileDetailsBinding

    private var accountInterface: AccountInterface? = null

    fun setAccountInterface(callback: AccountInterface?) {
        accountInterface = callback
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        // Get ViewModel scoped to this Activity
        backup = RoomBackup(this)
        binding.apply {
            sharePlaylist.setOnClickListener {
                accountInterface!!.onShareClicked()
            }
            deletePlaylist.setOnClickListener {
                accountInterface!!.onDeleteClicked()
            }
            btnCreatePlayList.setOnClickListener {
                accountInterface!!.onCreateClicked()
            }
        }
    }

    override fun onFragmentInteraction(fragments: FragmentsAccounts, tag: String) {
        when(fragments){
            FragmentsAccounts.SETTINGS_FRAGMENT -> {
                binding.profileTitle.text = tag
                binding.sharePlaylist.visibility = View.GONE
                binding.deletePlaylist.visibility = View.GONE
                binding.btnCreatePlayList.visibility = View.GONE
            }
            FragmentsAccounts.PLAYLIST_ONE_FRAGMENT -> {
                binding.profileTitle.text = tag
                binding.sharePlaylist.visibility = View.GONE
                binding.deletePlaylist.visibility = View.GONE
                binding.btnCreatePlayList.visibility = View.VISIBLE
            }
            FragmentsAccounts.PLAYLIST_TWO_FRAGMENT -> {
                binding.profileTitle.text = tag
                binding.sharePlaylist.visibility = View.VISIBLE
                binding.deletePlaylist.visibility = View.VISIBLE
                binding.btnCreatePlayList.visibility = View.GONE
            }
            FragmentsAccounts.SHARED_PLAYLIST_ONE_FRAGMENT -> {
                binding.profileTitle.text = tag
                binding.sharePlaylist.visibility = View.GONE
                binding.deletePlaylist.visibility = View.GONE
                binding.btnCreatePlayList.visibility = View.GONE
            }
            FragmentsAccounts.SHARED_PLAYLIST_TWO_FRAGMENT -> {
                binding.profileTitle.text = tag
                binding.sharePlaylist.visibility = View.GONE
                binding.deletePlaylist.visibility = View.VISIBLE
                binding.btnCreatePlayList.visibility = View.GONE
            }
        }
    }

    /* override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
          if (keyCode == KEYCODE_BACK) {
             finish()
             return true
         }
         return super.onKeyDown(keyCode, event)
     }*/
}