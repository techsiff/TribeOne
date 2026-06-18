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
import androidx.core.net.toUri
import com.siffmember.info.databinding.BottomSheetWhatsappGroupLayoutBinding

class WhatsAppGroupBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "WhatsAppGroupBottomSheetFragment"
    }

    private lateinit var binding: BottomSheetWhatsappGroupLayoutBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetWhatsappGroupLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {

            btnGroup1.setOnClickListener {
                openWhatsAppGroup(requireActivity(), "LaVQYNAjPI8Cg2KRvbUjZh")
            }
            btnGroup2.setOnClickListener {
                openTelegramGroup(requireActivity(), "BOJXh0ursVrifbpfEWLNuw")
            }
            btnGroup3.setOnClickListener {
                openWhatsAppGroup(requireActivity(), "Bx3k8Lfq8rEGyfv9qxGd0k")
            }
            btnGroup4.setOnClickListener {
                openWhatsAppGroup(requireActivity(), "GZc0Lc18fPFGg0SyKqnvuT")
            }
            btnGroup5.setOnClickListener {
                openWhatsAppGroup(requireActivity(), "F979zK33cm1K8ukbQcLTyD")
            }
            btnGroup6.setOnClickListener {
                openTelegramGroup(requireActivity(), "VEHivEmE-66cyKVs")
            }
        }
    }

    fun openWhatsAppGroup(context: Context, inviteCode: String) {
        val uri = "https://chat.whatsapp.com/$inviteCode".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.whatsapp")

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp not installed
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            Log.e(TAG, "WhatsApp not installed ${e.message}")
        }
    }
    fun openTelegramGroup(context: Context, inviteCode: String) {
        val uri = "https://t.me/joinchat/$inviteCode".toUri()
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
}
