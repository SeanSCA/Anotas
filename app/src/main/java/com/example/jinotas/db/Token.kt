package com.example.jinotas.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "token")
class Token(
    @PrimaryKey val token: String
)