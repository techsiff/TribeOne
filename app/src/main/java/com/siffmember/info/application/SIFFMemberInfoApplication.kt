package com.siffmember.info.application

import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.DelicateCoroutinesApi
import us.zoom.sdk.ZoomSDK


class SIFFMemberInfoApplication: MultiDexApplication() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseFirestore.setLoggingEnabled(true)
        val zoomSDK = ZoomSDK.getInstance()
        if (zoomSDK.meetingService != null) {
            zoomSDK.meetingService.addListener(GlobalMeetingListener)
        }
       /* GlobalScope.launch(Dispatchers.IO) {
            try{
              //  DatabaseMerger.autoMergeIfNeeded(this@SIFFMemberInfoApplication)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }*/
    }
}