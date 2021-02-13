package com.cubearrow.cubelang.utils

import java.io.File


class IOUtils {
    companion object {
        /**
         * Reads all of the text from a file.
         * @param filePath The path to the file to be read.
         */
        fun readAllText(filePath: String): String {
            return File(filePath).readText()
        }

        /**
         * Writes text to a file. This overwrites the previous contents of the file.
         *
         * @param filePath The path to the file.
         * @param lines The text to write to the file.
         */
        fun writeAllLines(filePath: String, lines: String) {
            File(filePath).writeText(lines)
        }
    }
}