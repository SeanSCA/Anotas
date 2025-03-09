package com.example.jinotas.db

data class UserToken(
    val token: String,
    val userName: String,
    val password: String
) {
    override fun toString(): String {
        return "UserToken(token='$token', userName='$userName', password='$password')"
    }
}


