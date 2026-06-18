package com.siffmember.info.data.remote.api

import com.siffmember.info.data.remote.model.functions.CreateUserAccountRequest
import com.siffmember.info.data.remote.model.functions.ForgotPasswordRequest
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailResponse
import com.siffmember.info.data.remote.model.openpoints.AppendSheetResponse
import com.siffmember.info.data.remote.model.openpoints.AppendSheetRequest
import com.siffmember.info.data.remote.model.openpoints.CreateSheetRequest
import com.siffmember.info.data.remote.model.openpoints.CreateSheetResponse
import com.siffmember.info.data.remote.model.openpoints.ReadSheetData
import com.siffmember.info.data.remote.model.openpoints.SheetResponse
import com.siffmember.info.data.remote.model.openpoints.UpdateSheetResponse
import com.siffmember.info.data.remote.model.openpoints.UpdateSheetRequest
import com.siffmember.info.data.remote.model.zoom.SignatureRequest
import com.siffmember.info.data.remote.model.zoom.SignatureResponse
import com.siffmember.info.data.remote.model.zoom.ZakRequest
import com.siffmember.info.data.remote.model.zoom.ZakResponse
import com.siffmember.info.data.remote.model.zoom.ZoomMeetingDeleteRequest
import com.siffmember.info.data.remote.model.zoom.ZoomMeetingRequest
import com.siffmember.info.data.remote.model.zoom.meeting.ZoomMeetingResponse
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FirebaseFunctionApi {
    @POST("sendFCMNotifications")
    fun sendNotification(@Body request: NotificationRequest): Call<NotificationResponse>

    @GET("listSheetsInFolder")
    fun listSheetsInFolder(@Query("folderId") folderId: String): Call<SheetResponse>

    @GET("readSheetData")
    fun readSheetData(
        @Query("spreadsheetId") spreadsheetId: String,
        @Query("range") range: String
    ): Call<List<ReadSheetData>>

    @POST("createNewSpreadsheetInFolder")
    fun createNewSpreadsheetInFolder(@Body request: CreateSheetRequest): Call<CreateSheetResponse>

    @POST("appendDataToSheet")
    fun appendDataToSheet(@Body request: AppendSheetRequest): Call<AppendSheetResponse>

    @POST("updateDataToSheet")
    fun updateDataToSheet(@Body request: UpdateSheetRequest): Call<UpdateSheetResponse>

    @POST("sendApprovedEmail")
    fun sendApprovedEmail(@Body request: SendApprovedEmailRequest): Call<SendApprovedEmailResponse>

    @POST("createUserAccount")
    fun createUserAccount(@Body request: CreateUserAccountRequest): Call<SendApprovedEmailResponse>

    @POST("passwordReset")
    fun forgotPasswordRequest(@Body request: ForgotPasswordRequest): Call<SendApprovedEmailResponse>

    @POST("createZoomMeetingDynamic")
    fun createMeeting(@Body request: ZoomMeetingRequest): Call<ZoomMeetingResponse>

    @POST("getZoomSignatureDynamic")
    fun getSignature(@Body request: SignatureRequest): Call<SignatureResponse>

    @POST("getZoomZAKDynamic")
    fun getZAK(@Body request: ZakRequest): Call<ZakResponse>

    @POST("deleteZoomMeetingDynamic")
    fun deleteMeeting(@Body request: ZoomMeetingDeleteRequest): Call<ResponseBody>

}