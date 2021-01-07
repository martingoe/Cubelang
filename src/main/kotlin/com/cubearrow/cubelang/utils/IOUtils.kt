package com.cubearrow.cubelang.utils

import java.io.File


class IOUtils {
    companion object {
        fun readAllText(filePath: String): String {
            return File(filePath).readText()
        }

        fun writeAllLines(filePath: String, lines: String) {
            File(filePath).writeText(lines)
        }
    }
}