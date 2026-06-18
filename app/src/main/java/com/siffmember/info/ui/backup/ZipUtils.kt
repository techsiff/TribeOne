package com.siffmember.info.ui.backup

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipUtils {
    fun zipFiles(files: List<Pair<String, File>>, outZip: File) {
        ZipOutputStream(FileOutputStream(outZip)).use { zos ->
            val buffer = ByteArray(4096)
            for ((entryName, file) in files) {
                FileInputStream(file).use { fis ->
                    BufferedInputStream(fis).use { bis ->
                        val entry = ZipEntry(entryName)
                        entry.size = file.length()
                        zos.putNextEntry(entry)
                        var count: Int
                        while (bis.read(buffer).also { count = it } != -1) {
                            zos.write(buffer, 0, count)
                        }
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}
