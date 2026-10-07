package com.example.laboratorio.lab09.aviones.ui.data

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [FavoriteEntity::class, OrderLineEntity::class],
    version = 1
)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun dao(): StoreDao
    companion object {
        @Volatile
        private var instance: StoreDatabase? = null
        fun get(context: Context): StoreDatabase =

            // se utilizó Claude en esta línea de código, ya que al momento de calificarlo indicó que había un error de muchas sesiones abirtas, por lo que convenia sincronizarlas
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder<StoreDatabase>(
                    context.applicationContext,
                    "store.db"
                )
                    .setDriver(AndroidSQLiteDriver())
                    .build()
                    .also { instance = it }
            }
    }
}