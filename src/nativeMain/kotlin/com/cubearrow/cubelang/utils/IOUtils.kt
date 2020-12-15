package com.cubearrow.cubelang.utils

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.*

class IOUtils {
    companion object {
        fun readAllText(filePath: String): String {
            val returnBuffer = StringBuilder()
            val file = fopen(filePath, "r") ?: throw IllegalArgumentException("Cannot open input file $filePath")

            try {
                memScoped {
                    val readBufferLength = 64 * 1024
                    val buffer = allocArray<ByteVar>(readBufferLength)
                    var line = fgets(buffer, readBufferLength, file)?.toKString()
                    while (line != null) {
                        returnBuffer.append(line)
                        line = fgets(buffer, readBufferLength, file)?.toKString()
                    }
                }
            } finally {
                fclose(file)
            }

            return returnBuffer.toString()
        }

        fun writeAllLines(filePath: String, lines: String, lineEnding: String = "\n") {
            val file = fopen(filePath, "w") ?: throw IllegalArgumentException("Cannot open output file $filePath")
            try {
                memScoped {
                    lines.split(lineEnding).forEach {
                        if (fputs(it + lineEnding, file) == EOF) {
                            throw Error("File write error")
                        }
                    }
                }
            } finally {
                fclose(file)
            }
        }
    }
}