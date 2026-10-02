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
        private var instance: StoreDatabase? = null
        fun get(context: Context): StoreDatabase =
            instance ?: Room.databaseBuilder<StoreDatabase>(
                context.applicationContext,
                "store.db"
            )
                .setDriver(AndroidSQLiteDriver())
                .build()
                .also { instance = it }
    }
}