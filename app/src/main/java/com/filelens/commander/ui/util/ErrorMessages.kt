package com.filelens.commander.ui.util

import android.content.Context
import app.com.filelens.R
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.util.zip.ZipException

/**
 * Map raw exception or low-level error text into a short, user-readable
 * sentence. Used by snackbars / toasts across the app so we don't surface
 * Java stack-trace fragments to people.
 */
fun friendlyErrorMessage(context: Context, throwable: Throwable?): String {
    if (throwable == null) return context.getString(R.string.error_generic)
    val msg = throwable.message.orEmpty()
    return when {
        throwable is SecurityException -> context.getString(R.string.error_permission_denied)
        throwable is AccessDeniedException -> context.getString(R.string.error_permission_denied)
        throwable is FileNotFoundException -> context.getString(R.string.error_file_not_found)
        msg.contains("permission", ignoreCase = true) ||
            msg.contains("EACCES", ignoreCase = true) ->
            context.getString(R.string.error_permission_denied)
        msg.contains("ENOENT", ignoreCase = true) ||
            msg.contains("No such file", ignoreCase = true) ->
            context.getString(R.string.error_file_not_found)
        msg.contains("ENOSPC", ignoreCase = true) ||
            msg.contains("No space", ignoreCase = true) ->
            context.getString(R.string.error_out_of_space)
        throwable is ZipException -> context.getString(R.string.error_corrupt_file)
        throwable is UnsupportedOperationException -> context.getString(R.string.error_unsupported_file)
        throwable is IOException && msg.isNotBlank() -> context.getString(R.string.error_generic)
        else -> context.getString(R.string.error_generic)
    }
}

/**
 * String overload — for paths where we only have a captured error message
 * (e.g. surfaced through a Flow as a String) and not the Throwable itself.
 */
fun friendlyErrorMessage(context: Context, rawMessage: String?): String {
    if (rawMessage.isNullOrBlank()) return context.getString(R.string.error_generic)
    val lc = rawMessage.lowercase()
    return when {
        "permission" in lc || "eacces" in lc || "denied" in lc ->
            context.getString(R.string.error_permission_denied)
        "no such file" in lc || "enoent" in lc || "not found" in lc ->
            context.getString(R.string.error_file_not_found)
        "no space" in lc || "enospc" in lc ->
            context.getString(R.string.error_out_of_space)
        "unsupported" in lc -> context.getString(R.string.error_unsupported_file)
        "corrupt" in lc || "malformed" in lc -> context.getString(R.string.error_corrupt_file)
        else -> rawMessage
    }
}
