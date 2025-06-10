package com.example.jinotas.utils

import android.app.Activity
import android.app.ActivityManager
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.SubMenu
import android.view.View
import android.widget.ExpandableListView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.GravityCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.jinotas.DrawerExpandableListAdapter
import com.example.jinotas.R
import com.example.jinotas.adapter.AdapterNotes
import com.example.jinotas.db.Note
import com.google.android.material.navigation.NavigationView
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Collections
import kotlin.coroutines.CoroutineContext

object Utils {
    var noteListStyle: MutableLiveData<String> = MutableLiveData("Vertical")
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")    // Método para obtener el ID del dispositivo
    val FILE = stringPreferencesKey("notes_list_style")
    private val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    //Urls para CrudApi (retrofit)
    val URL_FILE = dotenv["URL_FILE"]

    fun getIdDevice(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun setupExpandableListView(
        expandableListView: ExpandableListView,
        context: Context,
        drawerLayout: DrawerLayout // Nuevo parámetro
    ) {
        // Lista combinada de títulos de grupo
        val titleList: List<String> = listOf(
            context.getString(R.string.change_language), context.getString(R.string.tipo_de_lista)
        )

        // Mapa combinado de hijos
        val childMap = mapOf(
            context.getString(R.string.change_language) to getLanguageStrings(context),
            context.getString(R.string.tipo_de_lista) to getNotesListStyles(context)
        )

        // Adaptador único usando la lista y el mapa combinados
        val adapter = DrawerExpandableListAdapter(context, titleList, childMap)
        expandableListView.setAdapter(adapter)

        // Configurar el listener para manejar clics en los hijos de los grupos
        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val group = titleList[groupPosition]
            val child = childMap[group]?.get(childPosition)


            if (group == context.getString(R.string.change_language)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    vibratePhone(context)
                    context.getSystemService(LocaleManager::class.java).applicationLocales =
                        LocaleList.forLanguageTags(child)
                } else {
                    vibratePhone(context)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(child)
                    )
                }
            } else if (group == context.getString(R.string.tipo_de_lista)) {
                if (child == context.getString(R.string.notes_list_styles_vertical)) {
                    noteListStyle.value = context.getString(R.string.notes_list_styles_vertical)
                    CoroutineScope(Dispatchers.Main).launch {
                        saveValues(child, context)
                    }
                } else if (child == context.getString(R.string.notes_list_styles_widget)) {
                    noteListStyle.value = context.getString(R.string.notes_list_styles_widget)
                    CoroutineScope(Dispatchers.Main).launch {
                        saveValues(child, context)
                    }
                }
            }

            // Cerrar el drawer después de la selección
            drawerLayout.closeDrawer(GravityCompat.START)

            // Colapsar el grupo seleccionado
            expandableListView.collapseGroup(groupPosition)

            true
        }
    }

    // Función para leer el valor almacenado en DataStore
    fun getValues(context: Context): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[FILE] ?: "Valor no encontrado"
        }
    }

    suspend fun saveValues(name: String, context: Context) {
        context.dataStore.edit { settings ->
            settings[FILE] = name
            Log.d("saveValue", "Valor guardado: $name")
        }
    }


    fun Activity.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100, VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(100)
        }
    }

    fun Fragment.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100, VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(100)
        }
    }

    fun Utils.vibratePhone(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    100, VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            vibrator.vibrate(100)
        }
    }

    fun getJsonFromAssets(context: Context): String? {
        val assetManager = context.assets
        val inputStream: InputStream = assetManager.open(URL_FILE)
        return inputStream.bufferedReader(Charset.forName("UTF-8")).use { it.readText() }
    }
}