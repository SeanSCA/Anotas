package com.example.jinotas.viewmodels

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.jinotas.R
import com.example.jinotas.api.CrudApi
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.RepositoryNotes
import com.example.jinotas.db.Token
import com.example.jinotas.db.UserToken
import com.example.jinotas.utils.SyncStatus
import com.example.jinotas.utils.Utils
import com.example.jinotas.utils.UtilsDBAPI.deleteNoteInCloud
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToCloud
import com.example.jinotas.utils.UtilsDBAPI.saveNoteToLocalDatabase
import com.example.jinotas.utils.UtilsDBAPI.updateNoteInCloud
import com.example.jinotas.utils.UtilsDBAPI.updateNoteInLocalDatabase
import com.example.jinotas.utils.UtilsInternet.isConnectionStableAndFast
import com.example.jinotas.widget.WidgetProvider
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext: Context = getApplication<Application>().applicationContext
    private val db: AppDatabase = AppDatabase.getDatabase(application)
    private val repositoryNotes = RepositoryNotes(db.noteDAO(), db.tokenDAO())
    private val crudApi: CrudApi = CrudApi()
    private val sharedPreferences: SharedPreferences =
        appContext.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)

    private val _notesCounter = MutableLiveData<String>()
    val notesCounter: LiveData<String> get() = _notesCounter

    private val _noteSavedMessage = MutableLiveData<String>()
    val noteSavedMessage: LiveData<String> get() = _noteSavedMessage

    private val _noteByCode = MutableLiveData<Note>()
    val noteByCode: LiveData<Note> get() = _noteByCode

//    private val _notesListStyle = MutableStateFlow<String>("Valor no encontrado")
//    val notesListStyle: StateFlow<String> get() = _notesListStyle

    private val _notesListStyle = MutableLiveData<String>()
    val notesListStyle: LiveData<String> get() = _notesListStyle


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
        viewModelScope.launch {
            if (isConnectionStableAndFast(appContext)) {

                val cloudNotes = (crudApi.getNotesList() as? ArrayList<Note>) ?: arrayListOf()
                val pendingNotes = repositoryNotes.getNoteSynced()
                val localNotes = repositoryNotes.getNotesList()

                // Filtrar notas en la nube que pertenecen al usuario actual
                val userCloudNotes =
                    cloudNotes/*.filter { it.userFrom == userName }*/      //Se ha comentado para no tener que usar usuarios

                // Obtener los códigos de notas locales
                val localCodes = localNotes.map { it.code }.toSet()

                // Identificar qué notas en la nube deben eliminarse (las que no existen localmente)
                val notesToDelete = userCloudNotes.filter { it.code !in localCodes }

                for (note in pendingNotes) {
                    val cloudNote = cloudNotes.find { it.code == note.code }

                    if (cloudNote != null) {
                        if (note.updatedTime!! > cloudNote.updatedTime!!) {
                            // 🔹 Solo actualizar en la nube si la versión local es más reciente
                            updateNoteInCloud(note, appContext)
                        } else {
                            // 🔹 Si la nube tiene una versión más nueva, traerla a local
                            repositoryNotes.updateNote(cloudNote)
                        }
                    } else {
                        // 🔹 Si la nota no está en la nube, subirla
                        saveNoteToCloud(note, appContext)
                    }

                    // ✅ Marcar como sincronizada
                    note.isSynced = true
                    note.syncStatus = SyncStatus.SYNCED
                    repositoryNotes.updateNote(note)
                    updateLastSyncTime()
                }

                for (note in cloudNotes) {
                    if (!localNotes.any { it.code == note.code }) {
                        if (note.updatedTime!! > getLastSyncTime()) {
                            // 🔹 Si la nota es más reciente que la última sincronización, recuperarla en local
                            repositoryNotes.insertNote(note)
                            Log.i("Sync", "Nota restaurada desde la nube: ${note.title}")
                        } else if (!cloudNotes.contains(note)) {
                            repositoryNotes.insertNote(note)
                        } else {
                            // 🔹 Si la nota en local está marcada como eliminada, eliminarla de la nube
                            if (notesToDelete.any { it.code == note.code }) {
                                deleteNoteInCloud(note, appContext)
                                Log.i("Sync", "Nota eliminada en la nube: ${note.title}")
                            }
                        }
                    }
                }

                // 🔹 Solo eliminar si en local tiene estado DELETED
                for (note in localNotes.filter { it.syncStatus == SyncStatus.DELETED }) {
                    deleteNoteInCloud(note, appContext)
                    repositoryNotes.deleteNote(note) // 🔹 Eliminar también de local después de confirmar eliminación en la nube
                    Log.i("Sync", "Nota eliminada en la nube: ${note.title}")
                }

                // 🔹 Si la nota está en la nube pero no en local, podríamos recuperarla
                for (note in cloudNotes) {
                    if (!localNotes.any { it.code == note.code }) {
                        repositoryNotes.insertNote(note)  // 🔹 Recuperamos la nota en local
                        Log.i("Sync", "Nota recuperada desde la nube: ${note.title}")
                    }
                }

            }
        }
        println("Llega hasta aquí")
    }

    fun getLastSyncTime(): Long {
        return sharedPreferences.getLong("last_sync_time", 0L)
    }

    fun updateLastSyncTime() {
        sharedPreferences.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
    }


    fun saveNoteConcurrently(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                note.updatedTime = System.currentTimeMillis() // ✅ Actualizar timestamp
                note.syncStatus = SyncStatus.CREATED // ✅ Marcar como nueva
                note.isSynced = isConnectionStableAndFast(appContext)

                repositoryNotes.insertNote(note) // Guardar localmente

                if (note.isSynced) {
                    saveNoteToCloud(note, appContext) // Guardar en la nube
                    note.syncStatus = SyncStatus.SYNCED // ✅ Marcar como sincronizada
                    repositoryNotes.updateNote(note) // Actualizar estado local
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        if (note.isSynced) "Nota sincronizada" else "Guardada localmente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("saveNoteConcurrently", "Error al guardar: ${e.message}")
            }
        }
    }


    fun overwriteNoteConcurrently(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                note.updatedTime = System.currentTimeMillis() // ✅ Actualizar timestamp
                note.syncStatus = SyncStatus.UPDATED // ✅ Marcar como modificada
                note.isSynced = isConnectionStableAndFast(appContext)

                repositoryNotes.updateNote(note) // Guardar cambios en local

                if (note.isSynced) {
                    updateNoteInCloud(note, appContext) // Actualizar en la nube
                    note.syncStatus = SyncStatus.SYNCED // ✅ Marcar como sincronizada
                    repositoryNotes.updateNote(note) // Guardar estado actualizado
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        if (note.isSynced) "Nota sincronizada" else "Actualizada localmente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("overwriteNoteConcurrently", "Error al actualizar: ${e.message}")
            }
        }
    }


    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes(): ArrayList<Note>? {
        return try {
            val notes = db.noteDAO().getNotesList()
                .filter { it.syncStatus != SyncStatus.DELETED } as ArrayList<Note>
            notes
        } catch (e: Exception) {
            null
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
    fun getNoteByCode(codeSearchUpdate: Int) {
        try {
            viewModelScope.launch {
                _noteByCode.postValue(repositoryNotes.getNoteByCode(codeSearchUpdate))
            }
        } catch (e: Exception) {
            Log.e("getNoteByCode", e.message.toString())
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
            Utils.saveValues("Vertical", context)
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