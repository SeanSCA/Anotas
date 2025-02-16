package com.example.jinotas.api.userApi

import com.google.gson.annotations.SerializedName

data class ApiUser(
    @SerializedName("userName") val userName: String,
    @SerializedName("password") val password: String,
    @SerializedName("token") val token: String
)