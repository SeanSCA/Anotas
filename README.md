
# 📒 Notes Application in Kotlin

This is a native Android application built with **Kotlin** that provides a fast and simple way to manage your notes. The app is designed to keep your content always available, both **offline** (stored locally with **SQLite**) and **online** (synchronized with **Firebase/Firestore**), allowing you to access your notes across the connected ecosystem of applications.

## 🚀 Main Features

- ✅ **Native development with Kotlin** for optimized Android performance.  
- ✅ **MVVM**
- ✅ **Local database with SQLite** for offline access.  
- ✅ **Cloud database with Firebase/Firestore** for seamless synchronization.  
- ✅ Use of **Kotlin coroutines** and **asynchronous code** to ensure a smooth user experience.  
- ✅ **Multilanguage support and translations** for global accessibility.  
- ✅ **Custom UI styles** designed with Material Design principles.  
- ✅ **Connection handling** that syncs your notes automatically when the device is back online.  
- ✅ Full **CRUD operations**: create, read, update, and delete notes.  
- ✅ RecyclerView, Custom components, Widget.

## 🛠️ Technologies Used

- [Kotlin](https://kotlinlang.org/) → Main development language.  
- [SQLite](https://developer.android.com/training/data-storage/sqlite) → Local database storage.  
- [Firebase Firestore](https://firebase.google.com/docs/firestore) → Cloud database.  
- **Kotlin Coroutines** → For asynchronous and efficient execution.  

## 📱 Functionality

1. **Create notes**: jot down your ideas instantly.  
2. **Edit notes**: update your content whenever needed.  
3. **Delete notes**: keep your list clean and organized.  
4. **Smart synchronization**: notes are saved locally first and then synced to the cloud when a connection is available.  
5. **Cross-platform access**: retrieve your notes from other apps connected to the same cloud database.  

## ⚙️ Setup Instructions

1. Clone this repository.  
2. Open the project in **Android Studio**.  
3. Run a Gradle sync to download dependencies.  
4. Download the `google-services.json` file from your [Firebase project console](https://console.firebase.google.com/).  
5. Place the `google-services.json` file in the directory:  "**App/src**"
6. Build and run the application on your device or emulator.  

## 🌐 Compatibility

- Android (minimum version configurable in `build.gradle`).  
- Multilanguage support with included translations.  
- Interface designed following **Material Design** guidelines.  

---

✨ This project is a great foundation for learning how to combine **local and cloud storage**, implement **asynchronous flows with coroutines**, and follow Android development best practices with **Kotlin**.


