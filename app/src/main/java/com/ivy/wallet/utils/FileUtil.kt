package com.ivy.wallet.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.*
import java.nio.charset.Charset
import java.util.*

@Deprecated("useless")
fun saveFile(
    context: Context,
    directoryType: String,
    fileName: String,
    content: String
) {
    val dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        ?: return
    val newFile = File("${dirPath}/$fileName")
    newFile.createNewFile()
    newFile.writeText(content)
}

fun writeToFile(context: Context, uri: Uri, content: String) {
    try {
        val contentResolver = context.contentResolver

        contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { fOut ->
                val writer = fOut.writer(charset = Charsets.UTF_16)
                writer.write(content)
                writer.close()
            }
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun readFile(
    context: Context,
    uri: Uri,
    charset: Charset
): String? {
    return try {
        val contentResolver = context.contentResolver

        var fileContent: String? = null

        contentResolver.openFileDescriptor(uri, "r")?.use {
            FileInputStream(it.fileDescriptor).use { fileInputStream ->
                fileContent = readFileContent(
                    fileInputStream = fileInputStream,
                    charset = charset
                )
            }
        }

        fileContent
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
        null
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

@Throws(IOException::class)
private fun readFileContent(
    fileInputStream: FileInputStream,
    charset: Charset
): String {
    BufferedReader(InputStreamReader(fileInputStream, charset)).use { br ->
        val sb = StringBuilder()
        var line: String?
        while (br.readLine().also { line = it } != null) {
            sb.append(line)
            sb.append('\n')
        }
        return sb.toString()
    }
}

fun Context.getFileName(uri: Uri, defaultFileName: String): String = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> getContentFileName(uri, defaultFileName)
    else -> uri.path?.let(::File)?.name ?: defaultFileName
}

fun Context.getFileName(uri: Uri) =
    this.getContentFileName(uri, "file${System.currentTimeMillis()}")

private fun Context.getContentFileName(uri: Uri, defaultFileName: String): String = runCatching {
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
        return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
    }
}.getOrElse {
    defaultFileName
}!!

fun Uri.getMimeType(context: Context): String? {
    return when (scheme) {
        ContentResolver.SCHEME_CONTENT -> context.contentResolver.getType(this)
        ContentResolver.SCHEME_FILE -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(toString()).lowercase(Locale.US)
        )
        else -> null
    }
}