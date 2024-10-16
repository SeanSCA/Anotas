package com.example.jinotas.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TokenDAO {

    @Query("SELECT token FROM token")
    fun getToken(): String

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNote(token: Token)

    @Update
    fun updateToken(token: Token)
}