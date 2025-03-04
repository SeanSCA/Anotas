package com.example.jinotas.db

import androidx.lifecycle.LiveData

class RepositoryNotes(private val noteDAO: NoteDAO, private val tokenDAO: TokenDAO) {

    fun getNoteOrderByDate(): ArrayList<Note> {
        return noteDAO.getNoteOrderByDate() as ArrayList<Note>
    }

    fun getNoteOrderByTitle(): ArrayList<Note> {
        return noteDAO.getNoteOrderByTitle() as ArrayList<Note>
    }

    fun getNotesList(): ArrayList<Note> {
        return noteDAO.getNotesList() as ArrayList<Note>
    }

    fun getNoteByTitle(filter: String): ArrayList<Note> {
        return noteDAO.getNoteByTitle(filter) as ArrayList<Note>
    }

    fun getAllNotesLive(): LiveData<ArrayList<Note>>{
        return noteDAO.getAllNotesLive() as LiveData<ArrayList<Note>>
    }

    fun insertToken(token: Token){
        tokenDAO.insertToken(token)
    }

    fun getToken(): String{
        return tokenDAO.getToken()
    }

    fun getNoteByCode(codeSearchUpdate: Int): Note{
        return noteDAO.getNoteByCode(codeSearchUpdate)
    }
}