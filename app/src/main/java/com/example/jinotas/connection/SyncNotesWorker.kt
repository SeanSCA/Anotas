package com.example.jinotas.connection

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.utils.UtilsDBAPI.getPendingNotes
import com.example.jinotas.utils.UtilsDBAPI.localPendingNotes
import com.example.jinotas.utils.UtilsDBAPI.removePendingNote
import com.example.jinotas.utils.UtilsDBAPI.saveNoteApiWhenRecoverInternet
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToCloud
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToLocalDatabase

class SyncNotesWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.e("worker", "ejecuta worker")
            val unsyncedNotes = getPendingNotes(applicationContext) // Fetch notes from local DB
            for (note in unsyncedNotes) {
                saveNoteApiWhenRecoverInternet(
                    note,
                    localPendingNotes,
                    applicationContext
                ) // Retry syncing
                removePendingNote(note) // Mark as synced
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncNotesWorker", "Error syncing notes: ${e.message}")
            Result.retry()
        }
    }

//    fun addNoteToPendingNotes(note: Note){
//        localPendingNotes.add(note)
//    }
//
//    fun getPendingNotes(): MutableList<Note> {
//        // Implement your logic to retrieve the pending notes from local storage
//        var db = AppDatabase.getDatabase(applicationContext)
//        localPendingNotes = db.noteDAO().getNotesList() as MutableList<Note>
//        return localPendingNotes // Or fetch from database/shared preferences
//    }
//
//    fun removePendingNote(note: Note) {
//        // Implement your logic to remove the note from local storage
//        localPendingNotes.remove(note) // Or delete from the database
//    }
}