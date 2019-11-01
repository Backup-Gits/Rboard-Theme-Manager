package de.dertyp7214.rboardthememanager.helper

import android.annotation.SuppressLint
import com.dertyp7214.logs.helpers.Logger
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import com.topjohnwu.superuser.io.SuFileOutputStream
import de.dertyp7214.rboardthememanager.Config.GBOARD_PACKAGE_NAME
import de.dertyp7214.rboardthememanager.Config.MAGISK_THEME_LOC
import java.io.File
import java.io.InputStream

object ThemeHelper {

    fun installTheme(inputStream: InputStream, name: String): Boolean {
        inputStream.copyTo(SuFileOutputStream(File(MAGISK_THEME_LOC, name)))
        return true
    }

    @SuppressLint("SdCardPath")
    fun applyTheme(name: String): Boolean {
        val inputPackageName = GBOARD_PACKAGE_NAME
        val fileName =
            "/data/data/$inputPackageName/shared_prefs/${inputPackageName}_preferences.xml"
        Logger.log(Logger.Companion.Type.INFO, "APPLY", "$name $inputPackageName $fileName")
        val content = SuFileInputStream(SuFile(fileName)).use {
            it.bufferedReader().readText()
        }.let {
            if (it.contains("<string name=\"additional_keyboard_theme\">"))
                it.replace(
                    "<string name=\"additional_keyboard_theme\">.*</string>".toRegex(),
                    "<string name=\"additional_keyboard_theme\">system:$name</string>"
                )
            else {
                it.replace(
                    "<map>",
                    "<map><string name=\"additional_keyboard_theme\">system:$name</string>"
                )
            }
        }
        SuFileOutputStream(File(fileName)).writer().use { outputStreamWriter ->
            outputStreamWriter.write(content)
        }
        return "am force-stop $inputPackageName".runAsCommand()
    }

    @SuppressLint("SdCardPath")
    fun getActiveTheme(): String {
        val inputPackageName = "com.google.android.inputmethod.latin"
        val fileLol =
            SuFile("/data/data/$inputPackageName/shared_prefs/${inputPackageName}_preferences.xml")
        return SuFileInputStream(fileLol).use { it.bufferedReader().readText() }
            .split("<string name=\"additional_keyboard_theme\">")
            .let { if (it.size > 1) it[1].split("</string>")[0] else "" }.replace("system:", "")
            .replace(".zip", "")
    }
}

fun String.runAsCommand(): Boolean {
    return Shell.getShell().newJob().add(this).exec().isSuccess.apply {
        Logger.log(Logger.Companion.Type.INFO, "RUN COMMAND", "$this ${this@runAsCommand}")
    }
}