package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityMeetingAnalyticDetailsBinding
import com.siffmember.info.ui.adapter.MeetingsAnalyticsNonUsersAdapter
import com.siffmember.info.ui.adapter.MeetingsAnalyticsUsersAdapter
import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.ui.model.MembersZoomMeeting
import com.siffmember.info.ui.model.ParticipantModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails
import com.siffmember.info.utils.Utils
import androidx.core.graphics.toColorInt
import com.siffmember.info.R

class MeetingsAnalyticsDetailsActivity : BaseActivity() {

    companion object {
        var TAG = "MeetingsAnalyticsDetailsActivity"
    }
    private lateinit var binding: ActivityMeetingAnalyticDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewNonUsersAdapter: MeetingsAnalyticsNonUsersAdapter? = null
    private var recyclerViewUsersAdapter: MeetingsAnalyticsUsersAdapter? = null
    private var hostName = ""
    private var hostID = ""

    private var meetingId = ""
    private var meetingTitle = ""
    private var meetingHostId = ""
    private var meetingStartTimestamp = ""
    private var roomName = ""

    private var meetingConfigDetails: MeetingHomeDetailsModel? = null
    private var meetingDetails: MeetingDetailsModel? = null
    private var meetingAnalyticsDetails: MeetingAnalyticsModel? = null
    private var nonJoinedUsersList: ArrayList<MembersZoomMeeting> = ArrayList()
    private var joinedUsersList: ArrayList<ParticipantModel> = ArrayList()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingAnalyticDetailsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.analyticLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()

        try {
            meetingConfigDetails = MeetingUserDetails.getMeetingConfigDetails()
            roomName = meetingConfigDetails!!.roomName!!

            meetingDetails = MeetingUserDetails.getMeetingDetails()
            meetingTitle = meetingDetails!!.topicName!!
            meetingId = meetingDetails!!.meetingNumber!!
            meetingHostId = meetingDetails!!.hostNumber!!

            meetingAnalyticsDetails = MeetingUserDetails.getMeetingAnalyticsDetails()
            meetingStartTimestamp = meetingAnalyticsDetails!!.startTime!!

            val allUsersList = MeetingUserDetails.getUsers() as ArrayList<MembersZoomMeeting>

            joinedUsersList = meetingAnalyticsDetails!!.participants

            val joinedUserIds = joinedUsersList.mapNotNull { it.userId }.toSet()

            val filteredNonJoinedUsers = ArrayList(
                allUsersList.filter { member ->
                    member.id !in joinedUserIds
                }
            )
            nonJoinedUsersList = filteredNonJoinedUsers

            setupAdapterNonJoinedUsers()
            setupAdapterJoinedUsers()

            binding.meetingTitle.text = meetingTitle
            binding.meetingAgenda.text = "${Utils.getMeetingDateTime(meetingAnalyticsDetails!!.startTime!!)}  |  TotalDuration: ${Utils.getMeetingDuration(meetingAnalyticsDetails!!.startTime!!, meetingAnalyticsDetails!!.endTime!!)}"
            binding.totalUsers.text = "Total Participants: ${allUsersList.size}"
            binding.joinedUser.text = "Joined Participants: ${joinedUsersList.size}"
            binding.nonJoinedUser.text = "Non Joined Participants: ${nonJoinedUsersList.size}"
            setupPieChart(allUsersList.size, joinedUsersList.size, nonJoinedUsersList.size)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.btnDeleteAnalytics.setOnClickListener {
            deleteMeetingAnalyticsDialog()
        }

    }
    private fun setupPieChart(totalUsers: Int, joinedUsers: Int, nonJoinedUsers: Int) {
        val entries = ArrayList<PieEntry>()
        entries.add(PieEntry(joinedUsers.toFloat(), ""))
        entries.add(PieEntry(nonJoinedUsers.toFloat(), ""))
        val dataSet = PieDataSet(entries, "")

        dataSet.colors = listOf(
            "#079A4A".toColorInt(), // Green
            "#EE7103".toColorInt()  // Red
        )

        dataSet.valueTextSize = 0f
        //dataSet.valueTextColor = "#FFFFFF".toColorInt()
        dataSet.setValueTextColors(listOf(
            "#079A4A".toColorInt(), // Green
            "#EE7103".toColorInt()  // Red
        ))

        val data = PieData(dataSet)

        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = "Total\n$totalUsers"
        binding.pieChart.setCenterTextSize(resources.getDimension(R.dimen.font8))
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    private fun setupAdapterNonJoinedUsers(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingNonJoinedUser.layoutManager = layoutManager
        recyclerViewNonUsersAdapter = MeetingsAnalyticsNonUsersAdapter(nonJoinedUsersList)
        binding.meetingNonJoinedUser.adapter = recyclerViewNonUsersAdapter
    }

    private fun setupAdapterJoinedUsers(){
        joinedUsersList.sortWith(compareByDescending<ParticipantModel> { it.userId == hostID }
            .thenBy { it.userName!!.lowercase() })
        val layoutManager = LinearLayoutManager(this)
        binding.meetingJoinedUser.layoutManager = layoutManager
        recyclerViewUsersAdapter = MeetingsAnalyticsUsersAdapter(hostID,joinedUsersList)
        binding.meetingJoinedUser.adapter = recyclerViewUsersAdapter
    }

    private fun deleteMeetingAnalyticsDialog() {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Meeting Analytics Alert")
                .setMessage("Are you sure you want to delete this analytics?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    showProgDialog()
                    deleteMeetingAnalyticsFireStore(meetingId) { isDeleted ->
                        dismissProgDialog()
                        if (isDeleted) {
                            finish()
                            dialogInterface.dismiss()
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
  /*  private fun deleteMeetingAnalyticsFireStore(meetingId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName)
            .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetingId)
            .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS).document(meetingStartTimestamp)
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
    }*/
  fun deleteMeetingAnalyticsFireStore(
      meetingId: String,
      onComplete: (Boolean) -> Unit
  ) {

      val analyticsDocRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
          .document(roomName)
          .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
          .document(meetingId)
          .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
          .document(meetingStartTimestamp)

      analyticsDocRef.collection("participants")
          .get()
          .addOnSuccessListener { participantSnapshot ->

              if (participantSnapshot.isEmpty) {
                  analyticsDocRef.delete()
                      .addOnSuccessListener { onComplete(true) }
                      .addOnFailureListener { onComplete(false) }
                  return@addOnSuccessListener
              }

              var processed = 0
              val total = participantSnapshot.size()

              participantSnapshot.documents.forEach { participantDoc ->

                  participantDoc.reference
                      .collection("sessions")
                      .get()
                      .addOnSuccessListener { sessionSnapshot ->

                          val batch = db.batch()

                          sessionSnapshot.documents.forEach { sessionDoc ->
                              batch.delete(sessionDoc.reference)
                          }

                          batch.commit()
                              .addOnSuccessListener {

                                  participantDoc.reference.delete()
                                      .addOnSuccessListener {

                                          processed++

                                          if (processed == total) {
                                              analyticsDocRef.delete()
                                                  .addOnSuccessListener {
                                                      onComplete(true)
                                                  }
                                                  .addOnFailureListener {
                                                      onComplete(false)
                                                  }
                                          }
                                      }
                              }
                      }
              }
          }
          .addOnFailureListener {
              onComplete(false)
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