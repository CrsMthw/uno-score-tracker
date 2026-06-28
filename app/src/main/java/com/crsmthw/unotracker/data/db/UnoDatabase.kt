package com.crsmthw.unotracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerEntity::class,
        GameEntity::class,
        GameEntityRow::class,
        GameEntityMember::class,
        RoundEntity::class,
        RoundEntryEntity::class,
        GameResultEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class UnoDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun gameEntityDao(): GameEntityDao
    abstract fun memberDao(): MemberDao
    abstract fun roundDao(): RoundDao
    abstract fun roundEntryDao(): RoundEntryDao
    abstract fun gameResultDao(): GameResultDao

    companion object {
        @Volatile private var INSTANCE: UnoDatabase? = null

        fun get(context: Context): UnoDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                UnoDatabase::class.java,
                "uno_tracker.db",
            ).build().also { INSTANCE = it }
        }
    }
}
