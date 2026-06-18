package com.siffmember.info.ui.view

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.hbb20.CountryCodePicker
import com.siffmember.info.R
import com.siffmember.info.ui.model.MembershipField

class DynamicFormBuilder(private val context: Context, private val container: LinearLayout) {
    // ✅ Correct mapping
    private val fieldMap = mutableMapOf<MembershipField, AppCompatEditText>()
    private val ccpMap = mutableMapOf<MembershipField, CountryCodePicker>()
    private val fieldPriority = listOf(
        "Name",
        "Email Id",
        "Country Code",
        "Phone Number",
        "Address"
    )
    fun build(fields: List<MembershipField>, isUpdate: Boolean) {
        container.removeAllViews()
        fieldMap.clear()
        ccpMap.clear()
        //val groupedFields = fields.groupBy { it.category }
        val groupedFields = fields
            .groupBy { it.category }
            .toSortedMap(compareBy { category ->
                if (category == "Personal Info") 0 else 1
            })
        groupedFields.forEach { (category, categoryFields) ->
            // 🔥 Category Header
            val header = TextView(context).apply {
                text = category
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.intro_title))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimension(R.dimen.font16))
                setPadding(16, 32, 16, 16)
            }
            container.addView(header)
            // 🔹 Fields inside category
            val sortedFields = categoryFields.sortedWith(
                compareBy<MembershipField> {
                    val index = fieldPriority.indexOf(it.key)
                    if (index == -1) Int.MAX_VALUE else index
                }.thenBy { it.key }

            )
            //categoryFields.forEach { field ->
            sortedFields.forEach { field ->
                // Label
                val label = TextView(context).apply {
                    text = field.label
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(context, R.color.blue))
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        context.resources.getDimension(R.dimen.font14)
                    )
                    setPadding(16, 16, 16, 10)
                }
                container.addView(label)
                // 🔹 Country Code Field
                if (isUpdate) {
                    val editText = AppCompatEditText(context).apply {
                        hint = field.label
                        inputType = field.inputType
                        setBackgroundResource(R.drawable.notes_edit_text_border)
                        setPadding(
                            context.resources.getDimension(R.dimen.margin10).toInt(),
                            context.resources.getDimension(R.dimen.margin10).toInt(),
                            context.resources.getDimension(R.dimen.margin5).toInt(),
                            context.resources.getDimension(R.dimen.margin10).toInt()
                        )
                        setHintTextColor(ContextCompat.getColor(context, R.color.grey))
                        setTextSize(
                            TypedValue.COMPLEX_UNIT_PX,
                            context.resources.getDimension(R.dimen.font14)
                        )
                        if (field.key == "Phone Number" || field.key == "Country Code") {
                            isEnabled = false          // disables editing
                            isFocusable = false       // removes focus
                            isClickable = false       // prevents click
                            setTextColor(ContextCompat.getColor(context, R.color.grey))
                        } else {
                            setTextColor(ContextCompat.getColor(context, R.color.blue))
                        }
                    }

                    container.addView(editText)
                    fieldMap[field] = editText
                } else {
                    if (field.key == "Country Code") {
                        val ccp = CountryCodePicker(context).apply {
                            setAutoDetectedCountry(true)
                            setCcpClickable(true)
                            showFullName(false)
                            showNameCode(false)
                            setDialogBackgroundColor(ContextCompat.getColor(context, R.color.white))
                            setContentColor(ContextCompat.getColor(context, R.color.blue))
                            setTextSize(context.resources.getDimension(R.dimen.font14).toInt())
                        }

                        container.addView(ccp)
                        ccpMap[field] = ccp
                        // Dummy EditText to store value internally
                        val dummyEditText = AppCompatEditText(context)
                        fieldMap[field] = dummyEditText
                        dummyEditText.setText(ccp.selectedCountryCodeWithPlus)
                        ccp.setOnCountryChangeListener {
                            dummyEditText.setText(ccp.selectedCountryCodeWithPlus)
                        }
                    } else {
                        val editText = AppCompatEditText(context).apply {
                            hint = field.label
                            inputType = field.inputType
                            setBackgroundResource(R.drawable.notes_edit_text_border)
                            setPadding(
                                context.resources.getDimension(R.dimen.margin10).toInt(),
                                context.resources.getDimension(R.dimen.margin10).toInt(),
                                context.resources.getDimension(R.dimen.margin5).toInt(),
                                context.resources.getDimension(R.dimen.margin10).toInt()
                            )
                            setHintTextColor(ContextCompat.getColor(context, R.color.grey))
                            setTextSize(
                                TypedValue.COMPLEX_UNIT_PX,
                                context.resources.getDimension(R.dimen.font14)
                            )
                            setTextColor(ContextCompat.getColor(context, R.color.blue))
                        }

                        container.addView(editText)
                        fieldMap[field] = editText
                    }
                }
            }
        }
    }

    // ✅ Clear all fields
    fun clearAllFields() {
        fieldMap.forEach { (_, editText) ->
            editText.setText("")
            editText.error = null
        }
        ccpMap.forEach { (_, ccp) ->
            ccp.setCountryForNameCode("IN") // Default country
        }
    }

    // ✅ Validate Required Fields
    fun validate(fields: List<MembershipField>): Boolean {
        var isValid = true
        fields.forEach { field ->
            val value = fieldMap[field]?.text?.toString()?.trim() ?: ""
            if (field.isRequired && value.isEmpty()) {
                fieldMap[field]?.error = "${field.label} is required"
                isValid = false
            }
        }
        return isValid
    }

    // ✅ Get values grouped by category
    fun getValues(): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, MutableMap<String, String>>()
        fieldMap.forEach { (field, editText) ->
            val value = editText.text?.toString()?.trim() ?: ""
            val category = field.category
            val key = field.key
            if (!result.containsKey(category)) {
                result[category] = mutableMapOf()
            }

            if (value.isNotEmpty()) {
                // Special logic for Phone Number
                if (key == "Phone Number") {
                    val lastFour = value.takeLast(4)
                    result[category]?.put("PhoneLast4", lastFour)
                }
                result[category]?.put(key, value)
            } else {
                result[category]?.put(key, "")
            }
        }
        return result
    }

    // ✅ Set values from Firebase
    fun setValues(data: Map<String, Map<String, String>>) {
        fieldMap.forEach { (field, editText) ->
            val categoryMap = data[field.category] ?: return@forEach
            val value = categoryMap[field.key]
            if (!value.isNullOrEmpty()) {
                editText.setText(value)
            }
//            if (field.key == "Country Code") {
//                val ccp = ccpMap[field]
//                if (!value.isNullOrEmpty() && ccp != null) {
//                    try {
//                        val cleanCode = value.replace("+", "") // "+91" -> "91"
//                        ccp.setCountryForPhoneCode(cleanCode.toInt())
//                        // also update hidden editText (your dummy field)
//                        editText.setText(value)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
//
//            } else {
//                // Normal EditText fields
//                if (!value.isNullOrEmpty()) {
//                    editText.setText(value)
//                }
//            }
        }
    }

    fun setValuesFromUserSelect(data: Map<String, Map<String, String>>) {

        fieldMap.forEach { (field, editText) ->
            val categoryMap = data[field.category] ?: return@forEach
            val value = categoryMap[field.key]
            if (field.key == "Country Code") {
                val ccp = ccpMap[field]
                if (!value.isNullOrEmpty() && ccp != null) {
                    try {
                        val cleanCode = value.replace("+", "") // "+91" -> "91"
                        ccp.setCountryForPhoneCode(cleanCode.toInt())
                        // also update hidden editText (your dummy field)
                        editText.setText(value)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

            } else {
                // Normal EditText fields
                if (!value.isNullOrEmpty()) {
                    editText.setText(value)
                }
            }
        }
    }


}
