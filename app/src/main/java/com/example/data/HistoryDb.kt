package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "calculation_history")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val toolName: String,
    val inputs: String,
    val outputs: String,
    val dateString: String,
    val timeString: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryItem)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)
}

@Database(entities = [HistoryItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

class HistoryRepository(private val dao: HistoryDao) {
    val allHistory: Flow<List<HistoryItem>> = dao.getAllHistory()

    suspend fun insert(item: HistoryItem) {
        dao.insertHistory(item)
    }

    suspend fun clear() {
        dao.clearAllHistory()
    }

    suspend fun delete(id: Int) {
        dao.deleteHistoryItem(id)
    }
}
