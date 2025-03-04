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

    private val _notesCounter = MutableLiveData<String>()
    val notesCounter: LiveData<String> get() = _notesCounter

    private val _noteSavedMessage = MutableLiveData<String>()
    val noteSavedMessage: LiveData<String> get() = _noteSavedMessage

    private val _noteByCode = MutableLiveData<Note>()
    val noteByCode: LiveData<Note> get() = _noteByCode

    /**
     * Here updates the notes counter
     * @param navigationView the navigation view
     */
    fun notesCounter(navigationView: NavigationView) {
        LifecycleService().lifecycleScope.launch {
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
    fun syncPendingNotes(userName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (isConnectionStableAndFast(appContext)) {
                val cloudNotes = (CrudApi().getNotesList() as? ArrayList<Note>) ?: arrayListOf()
                val pendingNotes = db.noteDAO().getNotesList().filter { !it.isSynced }
                val localNotes = db.noteDAO().getNotesList() // Lista completa de notas locales

                // Filtrar notas en la nube que pertenecen al usuario actual
                val userCloudNotes = cloudNotes.filter { it.userFrom == userName }

                // Obtener los códigos de notas locales
                val localCodes = localNotes.map { it.code }.toSet()

                // Identificar qué notas en la nube deben eliminarse (las que no existen localmente)
                val notesToDelete = userCloudNotes.filter { it.code !in localCodes }

                for (note in pendingNotes) {
                    try {
                        if (cloudNotes.any { it.code == note.code }) {
                            updateNoteInCloud(note, appContext)
                        } else {
                            saveNoteToCloud(note, appContext)
                        }
                        //Actualiza la nota localmente para saber que está sincronizado
                        note.isSynced = true
                        db.noteDAO().updateNote(note = note)
                        Log.i("Sync", "Nota sincronizada: ${note.title}")
                    } catch (e: Exception) {
                        Log.e("SyncError", "Error al sincronizar la nota: ${note.title}")
                    }
                }

                // Eliminar las notas que están en la nube pero no en las notas locales
                for (note in notesToDelete) {
                    deleteNoteInCloud(note, appContext)
                    Log.e("nota eliminar", note.toString())
                    Log.i("Delete", "Nota eliminada en la nube: ${note.title}")
                }
            }
        }
    }

    fun saveNoteConcurrently(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                coroutineScope {
                    val localSave = async {
                        note.isSynced = isConnectionStableAndFast(appContext)
                        saveNoteToLocalDatabase(note, appContext)
                    }
                    localSave.await()

                    if (note.isSynced) {
                        val cloudSave = async { saveNoteToCloud(note, appContext) }
                        cloudSave.await()
                    } else {
                        Log.e("Sync", "Nota guardada localmente, pendiente de sincronización")
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            if (note.isSynced) "Nota sincronizada con la nube" else "Nota guardada localmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ErrorGuardar", e.message.toString())
                    Toast.makeText(
                        appContext, "Error al guardar la nota", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun overwriteNoteConcurrently(note: Note) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                coroutineScope {
                    val localUpdate = async {
                        note.isSynced = isConnectionStableAndFast(appContext)
                        updateNoteInLocalDatabase(note, appContext)
                        //Esto es para actualizar el contenido del widget
                        withContext(Dispatchers.Main) {
                            val sharedPreferences =
                                appContext.getSharedPreferences("WidgetPrefs", MODE_PRIVATE)
                            val appWidgetManager = AppWidgetManager.getInstance(appContext)

                            val widgetIds = appWidgetManager.getAppWidgetIds(
                                ComponentName(appContext, WidgetProvider::class.java)
                            )

                            for (appWidgetId in widgetIds) {
                                val savedNoteCode = sharedPreferences.getString(
                                    "widget_note_${appWidgetId}_code", ""
                                )

                                // Verifica si la nota actualizada es la que está asociada al widget
                                if (savedNoteCode == note.code.toString()) {
                                    WidgetProvider.updateWidget(
                                        appContext,
                                        appWidgetManager,
                                        appWidgetId,
                                        note.title,
                                        note.textContent
                                    )
                                }
                            }
                        }
                        //Hasta aquí
                    }
                    localUpdate.await()

                    if (note.isSynced) {
                        val cloudUpdate = async { updateNoteInCloud(note, appContext) }
                        cloudUpdate.await()
                    } else {
                        Log.e("Sync", "Nota actualizada localmente, pendiente de sincronización")
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            appContext,
                            if (note.isSynced) "Nota sincronizada con la nube" else "Nota actualizada localmente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("ErrorActualizar", e.message.toString())
                    Toast.makeText(
                        appContext, "Error al actualizar la nota", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Load all the notes into the recyclerview
     */
    fun loadNotes(): ArrayList<Note>? {
        return try {
            val notes = db.noteDAO().getNotesList() as ArrayList<Note>
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
            crudApi.postTokenByUser(userToken)
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
}