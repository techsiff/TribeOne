package com.siffmember.info.utils

import android.content.Context
import android.net.ConnectivityManager
import android.content.SharedPreferences
import com.google.firebase.auth.PhoneAuthProvider
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * App util class
 */
object Utils {

    val category = arrayOf("Admin", "UserL1", "UserL2", "MemberL1", "MemberL2", "MemberL3")
    val ParamCategory = arrayOf("Personal Info", "Legal Parameters", "Non-Legal Parameters")
    val categoryFilter = arrayOf("All","Admin", "UserL1", "UserL2", "MemberL1", "MemberL2", "MemberL3")
    val categorySelect = arrayOf("Select category", "All", "Admin", "UserL1", "UserL2", "MemberL1", "MemberL2", "MemberL3")
    val allowedRoles: List<String> = listOf("Admin","UserL1","UserL2","MemberL1","MemberL2","MemberL3")
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val activeNetworkInfo = connectivityManager!!.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    // Function to save the resend token
    fun saveResendToken(context: Context, resendToken: PhoneAuthProvider.ForceResendingToken) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Serialize the resend token using Gson
        val gson = Gson()
        val tokenString = gson.toJson(resendToken)

        // Save the serialized token string in SharedPreferences
        editor.putString("RESEND_TOKEN", tokenString)
        editor.apply()
    }

    fun getResendToken(context: Context): PhoneAuthProvider.ForceResendingToken? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        // Retrieve the serialized token string
        val tokenString = sharedPreferences.getString("RESEND_TOKEN", null)

        // Deserialize the string back to ForceResendingToken
        return if (tokenString != null) {
            val gson = Gson()
            gson.fromJson(tokenString, PhoneAuthProvider.ForceResendingToken::class.java)
        } else {
            null // Return null if the token isn't stored
        }
    }

    fun clearResendToken(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("RESEND_TOKEN")
        editor.apply()
    }


    fun formatDynamicCount(number: Long): String {
        val suffixes = arrayOf("", "K", "M", "B", "T", "P", "E") // Supports up to Exa (E)
        var num = number.toDouble()
        var index = 0

        while (num >= 1000 && index < suffixes.size - 1) {
            num /= 1000
            index++
        }

        return String.format("%.1f%s", num, suffixes[index]).replace(".0", "") // Removes ".0" for whole numbers
    }
    /*fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} hours ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)} days ago"
            else -> "${diff / TimeUnit.DAYS.toMillis(7)} weeks ago"
        }
    }*/

    fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} hours ago"
            else -> {
                val date = Date(timestamp)
                val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()) // e.g., Jun 4, 10:20 AM
                sdf.format(date)
            }
        }
    }

    fun getTimeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)} min ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)} hours ago"
            else -> {
                val date = Date(timestamp)
                val sdf = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()) // e.g., Jun 4, 10:20 AM
                sdf.format(date)
            }
        }
    }
    fun getDateTime(timestampData: String): String {
        val timestamp = timestampData.toLong()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault()) // 12-hour format with AM/PM
        return dateFormat.format(Date(timestamp))
    }
    fun getMeetingDateTime(timestampData: String): String {
        val timestamp = timestampData.toLong()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) // 12-hour format with AM/PM
        return dateFormat.format(Date(timestamp))
    }
    fun getCallDuration(duration: String): String {
        val seconds = duration.toInt()
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            h > 0 -> "${h}h ${m}m ${s}s"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }
    fun formatCallDuration(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60

        return when {
            h > 0 -> "${h}h ${m}m ${s}s"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }


    fun getChatTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault()) // 12-hour format with AM/PM
        return dateFormat.format(Date(timestamp))
    }

    fun getGroupTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd/mm/yyy", Locale.getDefault()) // 12-hour format with AM/PM
        return dateFormat.format(Date(timestamp))
    }

    fun getChatLastTime(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return when {
            // If the timestamp is from today, show "hh:mm a" format
            today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) -> {
                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
            }
            // If the timestamp is from yesterday, show "Yesterday"
            yesterday.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    yesterday.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday"
            }
            // Otherwise, show "dd/MM/yyyy"
            else -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    fun generateRandomPass(
        length: Int = 12,
        includeUppercase: Boolean = true,
        includeDigits: Boolean = true,
        includeSymbols: Boolean = true
    ): String {
        val lowercase = ('a'..'z').toList()
        val uppercase = ('A'..'Z').toList()
        val digits = ('0'..'9').toList()
        val symbols = "!@#$%^&*()-_=+[]{};:,.<>?/".toList()

        // Build allowed character set
        val allowedChars = lowercase.toMutableList()
        if (includeUppercase) allowedChars += uppercase
        if (includeDigits) allowedChars += digits
        if (includeSymbols) allowedChars += symbols

        // Generate password
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    fun generateRandomMembershipNumber(): String {
        val numberPart = (10..99).random() // 2-digit number

        val textPart = (1..2)
            .map { ('A'..'Z').random() }   // random uppercase letters
            .joinToString("")

        val sequencePart = (1000..9999).random() // 4-digit number

        return "$numberPart$textPart$sequencePart"
    }

    fun getCurrentDate(): String{
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val dateOnlyTimestamp = calendar.timeInMillis
        return "$dateOnlyTimestamp"
    }
    fun getMeetingDuration(
        startTime: String?,
        endTime: String?
    ): String {
        if (startTime.isNullOrEmpty() || endTime.isNullOrEmpty()) {
            return "0 min"
        }
        return try {
            val start = startTime.toLong()
            val end = endTime.toLong()
            val diff = end - start
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60

            when {
                hours > 0 -> {
                    val remainingMinutes = minutes % 60
                    "${hours}h ${remainingMinutes}m"
                }
                minutes > 0 -> {
                    "${minutes}m"
                }
                else -> {
                    "${seconds}s"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "0 min"
        }
    }
    fun formatDuration(durationMillis: Long): String {
        val seconds = durationMillis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> {
                val remainingMinutes = minutes % 60
                "${hours}h ${remainingMinutes}m"
            }
            minutes > 0 -> {
                "${minutes}m"
            }
            else -> {
                "${seconds}s"
            }
        }
    }
}