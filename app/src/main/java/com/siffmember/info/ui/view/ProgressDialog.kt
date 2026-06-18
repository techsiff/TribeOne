package com.siffmember.info.ui.view

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.siffmember.info.R
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Showing progress dialog
 */
class ProgressDialog {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(THEME_LIGHT, THEME_DARK, THEME_FOLLOW_SYSTEM)
    annotation class ThemeConstant

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(MODE_INDETERMINATE, MODE_DETERMINATE)
    annotation class ModeConstant

    private val context: Context
    private var progressBarIndeterminate: ProgressBar? = null
    private var progressDialog: AlertDialog? = null
    private var dialogLayout: ConstraintLayout? = null
    private var vMode = 0
    private var vTheme = 0
    private var incrementAmt = 0
    private var cancelable = false
    private var autoThemeEnabled = false

    /**
     * Simple Constructor accepting only the Activity Level Context as Argument.
     * Theme is set as Light Theme by Default (which can be changed later using [.setTheme]).
     * Mode is set as Indeterminate by Default (which can be changed later using [.setMode]).
     */
    constructor(context: Context) {
        this.context = context
        initialiseDialog(THEME_LIGHT, MODE_INDETERMINATE)
    }

    /**
     * A Constructor accepting the Activity Level Context and Theme Constant as Arguments.
     * Theme is set as Light Theme if [.THEME_LIGHT] is passed (This can be changed later using [.setTheme]).
     * Theme is set as Dark Theme if [.THEME_DARK] is passed (This can be changed later using [.setTheme]).
     * Theme is automatically decided at runtime according to System's Theme if [.THEME_FOLLOW_SYSTEM] is passed (This can be changed later using [.setTheme]).
     * NOTE : [.THEME_FOLLOW_SYSTEM] can be used starting from Android API Level 31 only.
     * Mode is set as Indeterminate by Default (which can be changed later using [.setMode]).
     */
    constructor(context: Context, @ThemeConstant themeConstant: Int) {
        this.context = context
        initialiseDialog(themeConstant, MODE_INDETERMINATE)
    }

    /**
     * A Constructor accepting the Mode Constant, Activity Level Context and Theme Constant as Arguments.
     * Mode is set as Determinate if [] is passed (This can be changed later using [.setMode]).
     * Mode is set as Indeterminate if [.MODE_INDETERMINATE] is passed (This can be changed later using [.setMode]).
     * Theme is set as Light Theme if [.THEME_LIGHT] is passed (This can be changed later using [.setTheme]).
     * Theme is set as Dark Theme if [.THEME_DARK] is passed (This can be changed later using [.setTheme]).
     * Theme is automatically decided at runtime according to System's Theme if [.THEME_FOLLOW_SYSTEM] is passed (This can be changed later using [.setTheme]).
     * NOTE : [.THEME_FOLLOW_SYSTEM] can be used starting from Android API Level 31 only.
     */
    constructor(
        @ModeConstant modeConstant: Int,
        context: Context,
        @ThemeConstant themeConstant: Int
    ) {
        this.context = context
        initialiseDialog(themeConstant, modeConstant)
    }

    /**
     * A Constructor accepting the Mode Constant and Activity Level Context as Arguments.
     * Mode is set as Determinate if [.MODE_DETERMINATE] is passed (This can be changed later using [.setMode]).
     * Mode is set as Indeterminate if [.MODE_INDETERMINATE] is passed (This can be changed later using [.setMode]).
     * Theme is set as Light Theme by Default (which can be changed later using [.setTheme]).
     */
    constructor(@ModeConstant modeConstant: Int, context: Context) {
        this.context = context
        initialiseDialog(THEME_LIGHT, modeConstant)
    }

    private fun initialiseDialog(@ThemeConstant themeValue: Int, @ModeConstant modeValue: Int) {
        val builder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.layout_progressdialog, null)
        dialogLayout = view.findViewById(R.id.dialog_layout)
        progressBarIndeterminate = view.findViewById(R.id.progressbar_indeterminate)
        setTheme(themeValue)
        setMode(modeValue)
        builder.setView(view)
        progressDialog = builder.create()
        if (progressDialog!!.window != null) {
            progressDialog!!.window!!.setBackgroundDrawable(ColorDrawable(0))
        }
        setCancelable(false)
    }

    /**
     * Sets/Changes the mode of ProgressDialog which is [.MODE_INDETERMINATE] by Default.
     * If you're going to use only one Mode constantly, this method is not needed. Instead, use an appropriate Constructor to set the required Mode during Instantiation.
     *
     * @param modeConstant The Mode Constant to be passed as Argument ([.MODE_DETERMINATE] or [.MODE_INDETERMINATE]).
     * @return true if the passed modeConstant is valid and is set. false if the passed Mode is the current Mode or if modeConstant is invalid.
     */
    fun setMode(@ModeConstant modeConstant: Int): Boolean {
        return if (modeConstant == vMode) false else when (modeConstant) {
            MODE_DETERMINATE -> {
                progressBarIndeterminate!!.visibility = View.GONE
                incrementAmt = if (incrementAmt == 0) 1 else incrementAmt
                vMode = modeConstant
                true
            }
            MODE_INDETERMINATE -> {
                progressBarIndeterminate!!.visibility = View.VISIBLE
                vMode = modeConstant
                true
            }
            else -> false
        }
    }

    /**
     * Sets/Changes the Theme of ProgressDialog which is [.THEME_LIGHT] by Default.
     * If you're going to use only one Theme constantly, this method is not needed. Instead, use an appropriate Constructor to set the required Theme during Instantiation.
     *
     * @param themeConstant The Theme Constant to be passed.Use [.THEME_LIGHT] for Light Mode. Use [.THEME_DARK] for Dark Mode. Use [.THEME_FOLLOW_SYSTEM] for AutoTheming based on System theme (can be used starting from Android 11 (API Level 31) ONLY).
     * @return true if the passed themeConstant is valid and is set. false if the passed Theme is the current Theme or if themeConstant is invalid.
     * @throws IllegalArgumentException if [.THEME_FOLLOW_SYSTEM] is passed as Argument to this method in Android Versions lower than Android 11 (API Level 30)}.
     */
    @Throws(IllegalArgumentException::class)
    fun setTheme(@ThemeConstant themeConstant: Int): Boolean {
        return if (themeConstant == vTheme) false else when (themeConstant) {
            THEME_DARK, THEME_LIGHT -> {
                autoThemeEnabled = false
                setThemeInternal(themeConstant)
            }
            THEME_FOLLOW_SYSTEM -> {
                require(isAboveOrEqualToAnd11) { "THEME_FOLLOW_SYSTEM can be used starting from Android 11 (API Level 30) only !" }
                autoThemeEnabled = true
                true
            }
            else -> false
        }
    }

    /**
     * Starts the ProgressDialog and shows it on the Screen.
     */
    fun show() {
        if (autoThemeEnabled) {
            if (isSystemInNightMode && vTheme == THEME_LIGHT) {
                setThemeInternal(THEME_DARK)
            } else if (!isSystemInNightMode && vTheme == THEME_DARK) {
                setThemeInternal(THEME_LIGHT)
            }
        }
        progressDialog!!.show()
    }

    /**
     * Dismisses the ProgressDialog, removing it from the Screen.
     * To be used after the Task calling ProgressDialog is Over or if any Exception Occurs during Task execution.
     * In case of passing to Another Activity/Fragment, this method SHOULD be called before starting the next Activity/Fragment.
     * Else, it would cause WindowLeakedException.
     */
    fun dismiss() {
        progressDialog!!.dismiss()
    }

    /**
     * Sets the [DialogInterface.OnCancelListener] for ProgressDialog.
     * Should be used only if [.setCancelable] was passed with true earlier since cancel() cannot be called explicitly
     * and ProgressDialog is NOT cancelable by Default.
     *
     * @param onCancelListener [DialogInterface.OnCancelListener] listener object.
     * @return true if ProgressDialog is Cancelable. false otherwise.
     */
    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?): Boolean {
        return if (cancelable) {
            progressDialog!!.setOnCancelListener(onCancelListener)
            true
        } else {
            false
        }
    }

    /**
     * Toggles the Cancelable property of ProgressDialog which is false by Default.
     * If it is set to true, the User can cancel the ProgressDialog by pressing Back Button or by touching any other part of the screen.
     * It is NOT RECOMMENDED to set Cancelable to true.
     *
     * @param cancelable boolean value which toggles the Cancelable property of ProgressDialog.
     */
    fun setCancelable(cancelable: Boolean) {
        progressDialog!!.setCancelable(cancelable)
        this.cancelable = cancelable
    }

    /**
     * Applies a tint to Indeterminate Drawable if mode is [.MODE_INDETERMINATE].
     * Applies a tint to Determinate Drawable if mode is [.MODE_DETERMINATE].
     *
     * @param tintList The ColorStateList object used to apply tint to ProgressBar's Drawable.
     */
    fun setProgressTintList(tintList: ColorStateList?) {
        if (!isDeterminate) progressBarIndeterminate!!.indeterminateTintList = tintList
        //else
        //progressBarDeterminate.setProgressTintList(tintList);
    }

    private val isDeterminate: Boolean
        private get() = vMode == MODE_DETERMINATE

    @get:RequiresApi(api = Build.VERSION_CODES.R)
    private val isSystemInNightMode: Boolean
        private get() = context.resources.configuration.isNightModeActive
    private val isAboveOrEqualToAnd11: Boolean
        private get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private fun setThemeInternal(@ThemeConstant themeConstant: Int): Boolean {
        return when (themeConstant) {
            THEME_DARK -> {
                dialogLayout!!.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_dialog_dark
                )
                vTheme = themeConstant
                true
            }
            THEME_LIGHT -> {
                dialogLayout!!.background = ContextCompat.getDrawable(
                    context,
                    R.drawable.bg_dialog
                )
                vTheme = themeConstant
                true
            }
            else -> false
        }
    }

    companion object {
        /**
         * The default Theme for ProgressDialog (even if it is not passed in Constructor).
         * Suitable for apps having a Light Theme.
         * Theme can be changed later using [.setTheme].
         */
        const val THEME_LIGHT = 1

        /**
         * This theme is suitable for apps having a Dark Theme.
         * This Constant SHOULD be passed explicitly in the Constructor for setting Dark Theme for ProgressDialog.
         * Theme can be changed later using [.setTheme].
         */
        const val THEME_DARK = 2

        /**
         * When this ThemeConstant is used, ProgressDialog's theme is automatically changed to match the System's theme each time before [.show] is called.
         * This Constant can be used starting from Android API Level 31 (Android 11) ONLY.
         * [.setTheme] will throw [IllegalArgumentException] if this Constant is passed in method call in Android versions lower than Android 11.
         */
        @RequiresApi(api = Build.VERSION_CODES.R)
        const val THEME_FOLLOW_SYSTEM = -1

        /**
         * The default mode for ProgressDialog where an Indeterminate Spinner is shown for indicating Progress (even if it is not passed in Constructor).
         * Suitable for implementations where the exact progress of an operation is unknown to the Developer.
         */
        const val MODE_INDETERMINATE = 3

        /**
         * In this mode, a Determinate ProgressBar is shown inside the ProgressDialog for indicating Progress.
         * It also has a TextView for numerically showing the Progress Value either as Percentage or as Fraction.
         */
        const val MODE_DETERMINATE = 4
        private const val SHOW_AS_PERCENT = 6
    }
}