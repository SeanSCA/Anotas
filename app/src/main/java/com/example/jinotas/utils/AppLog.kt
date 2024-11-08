package com.example.jinotas.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val LOG_MAX = 100

    private val mQueue: LinkedHashMap<Int?, String?> = object : LinkedHashMap<Int?, String?>() {
        protected override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int?, String?>?): Boolean {
            return this.size > LOG_MAX
        }
    }

    fun add(type: Type, message: String) {
        val log: String

        if (type == Type.ACCOUNT || type == Type.DEVICE) {
            log = message + "\n"
        } else {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            log = "$timestamp - $type: $message\n"
        }

        mQueue[mQueue.size] = log
    }

    fun get(): String {
        val queue = StringBuilder()

        for ((_, value) in mQueue) {
            queue.append(value)
        }

        return queue.toString()
    }

    enum class Type {
        ACCOUNT,
        ACTION,
        AUTH,
        DEVICE,
        LAYOUT,
        NETWORK,
        SCREEN,
        SYNC,
        IMPORT,
        EDITOR
    }
}
