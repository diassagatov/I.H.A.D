package com.example.dreamjournal

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {
    @Query("SELECT * FROM dreams ORDER BY createdAt DESC")
    fun getAllDreams(): Flow<List<DreamEntity>>

    @Query("SELECT * FROM dreams WHERE id = :id")
    fun getDreamById(id: String): Flow<DreamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDream(dream: DreamEntity)

    @Update
    suspend fun updateDream(dream: DreamEntity)

    @Delete
    suspend fun deleteDream(dream: DreamEntity)
}

@Database(entities = [DreamEntity::class], version = 1, exportSchema = false)
abstract class DreamDatabase : RoomDatabase() {
    abstract fun dreamDao(): DreamDao

    companion object {
        @Volatile
        private var INSTANCE: DreamDatabase? = null

        fun getDatabase(context: Context): DreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DreamDatabase::class.java,
                    "dream_journal_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class DreamRepository(private val dao: DreamDao) {
    val allDreams: Flow<List<DreamEntity>> = dao.getAllDreams()

    fun getDreamById(id: String): Flow<DreamEntity?> = dao.getDreamById(id)

    suspend fun insertDream(dream: DreamEntity) = dao.insertDream(dream)

    suspend fun updateDream(dream: DreamEntity) = dao.updateDream(dream)

    suspend fun deleteDream(dream: DreamEntity) = dao.deleteDream(dream)
}
