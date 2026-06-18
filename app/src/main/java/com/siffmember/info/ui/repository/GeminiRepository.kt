package com.siffmember.info.ui.repository

import android.util.Log
import com.siffmember.info.data.remote.model.geminiAI.Content
import com.siffmember.info.data.remote.model.geminiAI.GeminiApi
import com.siffmember.info.data.remote.model.geminiAI.GeminiRequest
import com.siffmember.info.data.remote.model.geminiAI.GeminiResponse
import com.siffmember.info.data.remote.model.geminiAI.InlineData
import com.siffmember.info.data.remote.model.geminiAI.Part
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.SocketTimeoutException

class GeminiRepository(private val api: GeminiApi) {

    fun generateContent(prompt: String, base64File: String, mime: String, apiKey: String, callback: (String?) -> Unit) {
        Log.e("GeminiRepository","MIME:: $mime")
        Log.e("GeminiRepository","MIME:: ${base64File.isEmpty()}")
        val request = if(base64File.isEmpty()){
            GeminiRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(Part(text = prompt))
                    )
                )
            )
        } else {
            if(mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document"){
                Log.e("GeminiRepository","generateContent Is Doc Attached")
                GeminiRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(Part(text = prompt + "\n\n" + base64File))
                        )
                    )
                )
            } else {
                GeminiRequest(
                    contents = listOf(
                        Content(
                            role = "user",
                            parts = listOf(
                                Part(text = prompt),
                                Part(
                                    inlineData = InlineData(
                                        mimeType = mime,
                                        data = base64File
                                    )
                                )
                            )
                        )
                    )
                )
            }
        }


        api.generateContent(apiKey, request).enqueue(object : Callback<GeminiResponse> {
            override fun onResponse(call: Call<GeminiResponse>, response: Response<GeminiResponse>) {
                if (response.isSuccessful) {
                    val output = response.body()
                        ?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text
                    callback(output)
                } else {
                    callback("Error: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                //callback("Failure: ${t.message}")
                if (t is SocketTimeoutException) {
                    callback("Request timed out. Please try again.")
                } else {
                    callback("Failure: ${t.message}")
                }

            }
        })
    }


    fun generateContentMultiple(
        prompt: String,
        files: List<Pair<String, String>>, // Pair<mimeType, base64Data>
        apiKey: String,
        callback: (String?) -> Unit
    ) {

        val parts = mutableListOf<Part>()

        // Add prompt first
        parts.add(Part(text = prompt))

        // Add all files
        files.forEach { file ->

            val mime = file.first
            val base64 = file.second

            if (mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document") {

                // DOCX as text
                parts.add(
                    Part(
                        text = "\n\n$base64"
                    )
                )

            } else {

                // Images / PDFs / audio etc
                parts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = mime,
                            data = base64
                        )
                    )
                )
            }
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = parts
                )
            )
        )

        api.generateContent(apiKey, request)
            .enqueue(object : Callback<GeminiResponse> {

                override fun onResponse(
                    call: Call<GeminiResponse>,
                    response: Response<GeminiResponse>
                ) {

                    if (response.isSuccessful) {

                        val output = response.body()
                            ?.candidates
                            ?.firstOrNull()
                            ?.content
                            ?.parts
                            ?.firstOrNull()
                            ?.text

                        callback(output)

                    } else {

                        callback("Error: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(
                    call: Call<GeminiResponse>,
                    t: Throwable
                ) {

                    if (t is SocketTimeoutException) {
                        callback("Request timed out. Please try again.")
                    } else {
                        callback("Failure: ${t.message}")
                    }
                }
            })
    }


    suspend fun validateAnswer(
        question: String,
        answer: String,
        apiKey: String
    ): Boolean {

        try {
            when (question) {

                "What is your name?" -> {
                    return answer.matches(Regex("^[a-zA-Z ]{2,50}$"))
                }

                "What is your age?" -> {
                    return answer.toIntOrNull()?.let { it in 1..120 } ?: false
                }

                "What is your date of birth?" -> {
                    // Accept formats like: 12/05/1995 or 12-05-1995
                    return answer.matches(Regex("^\\d{2}[-/]\\d{2}[-/]\\d{4}$"))
                }

                "What is your phone number?" -> {
                    return answer.matches(Regex("^\\+?[1-9]\\d{7,14}$"))
                }

                "What is your email?" -> {
                    return android.util.Patterns.EMAIL_ADDRESS.matcher(answer).matches()
                }

                "What is your problem?" -> {
                    // 🔥 Use AI ONLY for this (complex input)
                    return validateWithAI(question, answer, apiKey)
                }

                else -> return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    private suspend fun validateWithAI(
        question: String,
        answer: String,
        apiKey: String
    ): Boolean {

        val prompt = """
                You are validating a user response for a form.
                
                Question: "$question"
                User Answer: "$answer"
                
                Definition of "problem":
                - It can be ANY real-life issue
                - Includes personal, emotional, legal, medical, technical, financial problems
                
                Examples of VALID:
                - I have a headache
                - App is crashing
                - Payment failed
                - I lost my job
                - I have family issues
                - I received divorce papers
                - I am feeling stressed
                
                Examples of INVALID:
                - ok
                - nothing
                - hi
                - 12345
                - test
                
                Rules:
                - If answer describes a real issue → VALID
                - If answer is meaningless or too short → INVALID
                
                Reply ONLY:
                VALID
                or
                INVALID
                """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    role = "user",
                    parts = listOf(Part(text = prompt))
                )
            )
        )

        val response = api.collectUsersData(apiKey, request)
        // 🔥 FULL RESPONSE (optional - big log)
        Log.e("AI_VALIDATION", "Full Response: $response")
        val result = response.candidates.firstOrNull()
            ?.content?.parts?.firstOrNull()?.text ?: ""

        val raw = result.uppercase().trim()

        val isValid = Regex("\\bVALID\\b").containsMatchIn(raw)
        val isInvalid = Regex("\\bINVALID\\b").containsMatchIn(raw)

        return isValid && !isInvalid
    }
}