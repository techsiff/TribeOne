package com.siffmember.info.ui.fragment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.BottomSheetSocialMediaLayoutBinding
import androidx.core.net.toUri

class SocialMediaBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "SocialMediaBottomSheetFragment"
    }

    private lateinit var binding: BottomSheetSocialMediaLayoutBinding
    private var listener: SocialMediaBottomSheetListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent activity implements the interface
        if (context is SocialMediaBottomSheetListener) {
            listener = context
        } else {
            Log.e(TAG, "Parent Fragment must implement BottomSheetListener")
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetSocialMediaLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            btnTwitter.setOnClickListener {
                openTwitterProfile(requireActivity(), "realsiff")
                //dismiss()
            }
            btnYoutube.setOnClickListener {
                openYouTubeChannel(requireActivity(), "https://www.youtube.com/@RealSIFF")
                //dismiss()
            }
            btnWhatsapp.setOnClickListener {
                listener!!.onOpenWhatsApp()
                dismiss()
            }
            btnTelegram.setOnClickListener {
                openTelegramGroup(requireActivity(),"SaveIndianFamily")
                //dismiss()
            }
        }
    }

    fun openTwitterProfile(context: Context, username: String) {
        val twitterAppUri = "twitter://user?screen_name=$username".toUri()
        val browserUri = "https://twitter.com/$username".toUri()

        val intent = Intent(Intent.ACTION_VIEW, twitterAppUri)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Twitter app not installed, open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
            Log.d(TAG, "openTwitterProfile: ${e.message}")
        }
    }

    fun openYouTubeChannel(context: Context, channelUrl: String) {
        val uri = channelUrl.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.youtube")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // YouTube app not installed
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            Log.d(TAG, "openYouTubeChannel: ${e.message}")
        }
    }

    fun openTelegramGroup(context: Context, groupName: String) {
        val uri = "https://t.me/$groupName".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("org.telegram.messenger")

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Telegram not installed
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            Log.d(TAG, "openYouTubeChannel: ${e.message}")
        }
    }

    interface SocialMediaBottomSheetListener {
        fun onOpenWhatsApp()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
