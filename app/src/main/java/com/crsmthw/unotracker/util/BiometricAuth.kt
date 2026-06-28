package com.crsmthw.unotracker.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Gates a destructive action behind the device's biometric / screen-lock credential, so a competitive
 * friend can't quietly erase records. Falls back to running [onSuccess] directly when the device has no
 * biometric or screen lock enrolled (nothing to enforce). [onFailure] fires on cancel / auth error.
 */
object BiometricAuth {

    private val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {},
    ) {
        if (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            onSuccess() // no lock set up — can't enforce, so proceed
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onFailure()
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }
}

/** Walks the Compose [Context] chain to the hosting [FragmentActivity]. */
fun Context.findFragmentActivity(): FragmentActivity {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    error("No FragmentActivity found in context chain")
}
