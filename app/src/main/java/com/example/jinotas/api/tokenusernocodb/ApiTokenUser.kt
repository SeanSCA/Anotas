package com.example.jinotas.api.tokenusernocodb

import com.google.gson.annotations.SerializedName

data class ApiTokenUser(
    @SerializedName("userName") val userName: String,
    @SerializedName("token") val token: String
)
