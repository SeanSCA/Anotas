package com.example.jinotas.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.jinotas.R
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.RepositoryNotes
import com.example.jinotas.db.Token
import com.example.jinotas.db.UserToken
import com.example.jinotas.utils.Utils
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = getApplication<Application>().applicationContext
    private val db: AppDatabase = AppDatabase.getDatabase(application)
    private val repositoryNotes = RepositoryNotes(db.noteDAO(), db.tokenDAO())
    private val sharedPreferences: SharedPreferences =
        appContext.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)

    private val _notesCounter = MutableLiveData<String>()
    val notesCounter: LiveData<String> get() = _notesCounter

    private val _noteSavedMessage = MutableLiveData<String>()
    val noteSavedMessage: LiveData<String> get() = _noteSavedMessage

    private val _noteByCode = MutableLiveData<Note?>()
    val noteByCode: LiveData<Note?> get() = _noteByCode


//    private val _notesListStyle = MutableStateFlow<String>("Valor no encontrado")
//    val notesListStyle: StateFlow<String> get() = _notesListStyle

    private val _notesListStyle = MutableLiveData<String>()
    val notesListStyle: LiveData<String> get() = _notesListStyle

    private val _mutableNotesList = MutableLiveData<List<Note>>()
    val notesList: LiveData<List<Note>> get() = _mutableNotesList


    /**
     * Here updates the notes counter
     * @param navigationView the navigation view
     */
    fun notesCounter(navigationView: NavigationView) {
        viewModelScope.launch {
            val headerView = navigationView.getHeaderView(0)
            val navViewTotalNotes = headerView.findViewById<TextView>(R.id.notesCounter)

            _notesCounter.value = db.noteDAO().getNotesCount()
                .toString() + " " + appContext.getString(R.string.notes_counter)

            navViewTotalNotes.text = notesCounter.value
        }
    }

    /**
     * Sync all notes that have not been inserted, updated or deleted from the API as needed
     * @param userName the username of the logged user
     */
    fun syncPendingNotes(/*userName: String*/) {
//        viewModelScope.launch {
//            if (isConnectionStableAndFast(appContext)) {
//
//                val cloudNotes = (crudApi.getNotesList() as? ArrayList<Note>) ?: arrayListOf()
//                val pendingNotes = repositoryNotes.getNoteSynced()
//                val localNotes = repositoryNotes.getNotesList()
//
//                // Filtrar notas en la nube que pertenecen al usuario actual
//                val userCloudNotes =
//                    cloudNotes/*.filter { it.userFrom == userName }*/      //Se ha comentado para no tener que usar usuarios
//
//                // Obtener los códigos de notas locales
//                val localCodes = localNotes.map { it.code }.toSet()
//
//                // Identificar qué notas en la nube deben eliminarse (las que no existen localmente)
//                val notesToDelete = userCloudNotes.filter { it.code !in localCodes }
//
//                for (note in pendingNotes) {
//                    val cloudNote = cloudNotes.find { it.code == note.code }
//
//                    if (cloudNote != null) {
//                        if (note.updatedTime!! > cloudNote.updatedTime!!) {
//                            // 🔹 Solo actualizar en la nube si la versión local es más reciente
//                            updateNoteInCloud(note, appContext)
//                        } else {
//                            // 🔹 Si la nube tiene una versión más nueva, traerla a local
//                            repositoryNotes.updateNote(cloudNote)
//                        }
//                    } else {
//                        // 🔹 Si la nota no está en la nube, subirla
//                        saveNoteToCloud(note, appContext)
//                    }
//
//                    // ✅ Marcar como sincronizada
//                    note.isSynced = true
//                    note.syncStatus = SyncStatus.SYNCED
//                    repositoryNotes.updateNote(note)
//                    updateLastSyncTime()
//                }
//
//                for (note in cloudNotes) {
//                    if (!localNotes.any { it.code == note.code }) {
//                        if (note.updatedTime!! > getLastSyncTime()) {
//                            // 🔹 Si la nota es más reciente que la última sincronización, recuperarla en local
//                            repositoryNotes.insertNote(note)
//                            Log.i("Sync", "Nota restaurada desde la nube: ${note.title}")
//                        } else if (!cloudNotes.contains(note)) {
//                            repositoryNotes.insertNote(note)
//                        } else {
//                            // 🔹 Si la nota en local está marcada como eliminada, eliminarla de la nube
//                            if (notesToDelete.any { it.code == note.code }) {
//                                deleteNoteInCloud(note, appContext)
//                                Log.i("Sync", "Nota eliminada en la nube: ${note.title}")
//                            }
//                        }
//                    }
//                }
//
//                // 🔹 Solo eliminar si en local tiene estado DELETED
//                for (note in localNotes.filter { it.syncStatus == SyncStatus.DELETED }) {
//                    deleteNoteInCloud(note, appContext)
//                    repositoryNotes.deleteNote(note) // 🔹 Eliminar también de local después de confirmar eliminación en la nube
//                    Log.i("Sync", "Nota eliminada en la nube: ${note.title}")
//                }
//
//                // 🔹 Si la nota está en la nube pero no en local, podríamos recuperarla
//                for (note in cloudNotes) {
//                    if (!localNotes.any { it.code == note.code }) {
//                        repositoryNotes.insertNote(note)  // 🔹 Recuperamos la nota en local
//                        Log.i("Sync", "Nota recuperada desde la nube: ${note.title}")
//                    }
//                }
//
//            }
//        }
//        println("Llega hasta aquí")
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong("last_sync_time", 0L)
    }

    fun updateLastSyncTime() {
        sharedPreferences.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
    }


    fun saveNoteConcurrently(note: Note) {
        val db = Firebase.firestore

        // Add a new document with a generated ID
        db.collection("notas").add(note).addOnSuccessListener {
            Log.d("Nota Insertada", note.toString())
        }.addOnFailureListener { e ->
            Log.d("Nota No Insertada", note.toString())
        }
    }


    fun overwriteNoteConcurrently(note: Note) {
        val db = Firebase.firestore

        db.collection("notas").whereEqualTo("code", note.code).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    for (document in result) {
                        val docId = document.id

                        // Aquí modificamos campos individuales
                        db.collection("notas").document(docId).update(
                            mapOf(
                                "title" to note.title, "textContent" to note.textContent
                            )
                        ).addOnSuccessListener {
                            Log.d("Firestore", "Nota actualizada correctamente")
                        }.addOnFailureListener { e ->
                            Log.w("Firestore", "Error al actualizar", e)
                        }
                    }
                } else {
                    Log.d("Firestore", "No se encontró ninguna nota con code = $note.code")
                }
            }.addOnFailureListener { e ->
                Log.w("Firestore", "Error al buscar nota", e)
            }
    }


    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes() {
        val db = Firebase.firestore

        db.collection("notas").addSnapshotListener { snapshots, exception ->
            if (exception != null) {
                Log.w("Firestore", "Error al escuchar cambios", exception)
                return@addSnapshotListener
            }

            if (snapshots != null && !snapshots.isEmpty) {
                val notes = snapshots.map { it.toObject(Note::class.java) }
                _mutableNotesList.value = notes
                Log.d("LoadNotes", notes.toString())
                Log.d("Firestore", "Notas actualizadas: $notes")
            } else {
                _mutableNotesList.value = emptyList()
                Log.d("Firestore", "No hay notas")
            }
        }
    }


    /**
     * Sort all notes in the list according to the chosen option
     * @param option the option to sort the notes
     */
    fun orderByNotes(option: String): ArrayList<Note> {
        return try {
            when (option) {
                "date" -> return repositoryNotes.getNoteOrderByDate()
                "title" -> return repositoryNotes.getNoteOrderByTitle()
                else -> return repositoryNotes.getNotesList()
            }
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    /**
     * Filter all notes in the list according to the filter
     * @param filter the text to filter
     */
    fun filterNotes(filter: String): ArrayList<Note> {
        return try {
            repositoryNotes.getNoteByTitle(filter)
        } catch (e: Exception) {
            arrayListOf()
        }
    }

    /**
     * Gets a LiveData array list of Note that can be observed
     */
    fun getAllNotesLive(): LiveData<ArrayList<Note>>? {
        return try {
            repositoryNotes.getAllNotesLive()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Gets a note by a given code
     * @param codeSearchUpdate the note code to search
     */
//    fun getNoteByCode(codeSearchUpdate: Int) {
//        try {
//            viewModelScope.launch {
//                _noteByCode.postValue(repositoryNotes.getNoteByCode(codeSearchUpdate))
//            }
//        } catch (e: Exception) {
//            Log.e("getNoteByCode", e.message.toString())
//        }
//    }

    /**
     * Gets a note by a given code
     * @param codeSearchUpdate the note code to search
     */
    fun getNoteByCode(codeSearchUpdate: Int) {
        val db = Firebase.firestore

        db.collection("notas").whereEqualTo("code", codeSearchUpdate).get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val note = result.documents[0].toObject(Note::class.java)
                    _noteByCode.value = note!!
                    Log.d("Firestore", "Nota encontrada: $note")
                } else {
                    _noteByCode.value = null!!
                    Log.d("Firestore", "No se encontró ninguna nota con code = $codeSearchUpdate")
                }
            }.addOnFailureListener { exception ->
                Log.e("Firestore", "Error al buscar la nota", exception)
            }
    }

    /**
     * Inserts a token
     * @param token the token to be inserted
     */
    fun insertToken(token: Token) {
        try {
            repositoryNotes.insertToken(token)
        } catch (e: Exception) {
            Log.e("insertToken", e.message.toString())
        }
    }

    /**
     * Gets the token stored at the database
     */
    fun getToken(): String {
        return try {
            repositoryNotes.getToken()
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Posts the UserToken
     * @param userToken the UserToken
     */
    fun postTokenByUser(userToken: UserToken) {
        viewModelScope.launch {
//            crudApi.postTokenByUser(userToken)
        }
    }

    /**
     * Saves the userName in the shared preferences and posts the UserToken at the api
     * @param userName The username logged
     * @param sharedPreferences Shared Preferences
     */
    fun saveUserToken(userName: String, sharedPreferences: SharedPreferences) {
        viewModelScope.launch {
            // Guardar el nombre de usuario en SharedPreferences
            val editor = sharedPreferences.edit()
            editor.putBoolean("isFirstTime", false)  // Marcar que ya no es la primera vez
            editor.putString("userFrom", userName)  // Guardar el nombre de usuario
            editor.apply()

            val userToken: UserToken
            val token = getToken()
            userToken = UserToken(token = token, userName = userName!!, password = "")

            postTokenByUser(userToken)
        }
    }

    /**
     * Saves the note list type when the user changes it
     * @param name the type
     * @param context application context
     */
    fun saveNoteListStyle(name: String, context: Context) {
        viewModelScope.launch {
            Utils.saveValues(name, context)
        }
    }

    /**
     * Loads the type of the list from datastore
     */
    fun loadNotesStyle() {
        viewModelScope.launch {
            Utils.getValues(appContext).collect { value ->
                _notesListStyle.postValue(value)
            }
        }
    }
}