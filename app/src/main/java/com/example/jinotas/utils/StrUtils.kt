package com.example.jinotas.utils

class StrUtils {
    // exception-less conversion of string to int
    fun strToInt(value: String?): Int {
        return strToInt(value, 0)
    }

    fun strToInt(value: String?, defaultInt: Int): Int {
        if (value == null) return defaultInt
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            defaultInt
        }
    }
}