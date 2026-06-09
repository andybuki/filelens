package com.filelens.commander.ui.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Allowed authenticators: a strong biometric (Class 3 — fingerprint or
 * face on most devices) OR the device PIN / pattern / password. Either
 * works for the vault, matching the roadmap brief ("biometric / device-
 * credential gated").
 */
private const val ALLOWED = BiometricManager.Authenticators.BIOMETRIC_STRONG or
    BiometricManager.Authenticators.DEVICE_CREDENTIAL

enum class VaultAuthAvailability {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE
}

fun vaultAuthAvailability(context: Context): VaultAuthAvailability {
    val manager = BiometricManager.from(context)
    return when (manager.canAuthenticate(ALLOWED)) {
        BiometricManager.BIOMETRIC_SUCCESS -> VaultAuthAvailability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
            VaultAuthAvailability.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
            VaultAuthAvailability.NOT_ENROLLED
        else -> VaultAuthAvailability.UNAVAILABLE
    }
}

/**
 * Launches the system biometric / device-credential prompt. Calls
 * [onSuccess] when the user authenticates, [onFailure] with a
 * user-facing reason otherwise (cancel taps go through onFailure too).
 */
fun launchVaultAuth(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString.toString())
            }
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(ALLOWED)
        .build()
    prompt.authenticate(info)
}
