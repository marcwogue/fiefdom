package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FiefdomDao {

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    // --- Community Members ---
    @Query("SELECT * FROM members ORDER BY points DESC")
    fun getAllMembers(): Flow<List<CommunityMember>>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: String): CommunityMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: CommunityMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CommunityMember>)

    // --- Tax Logs ---
    @Query("SELECT * FROM tax_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllTaxLogs(): Flow<List<TaxLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxLog(log: TaxLog)

    // --- Game State ---
    @Query("SELECT * FROM game_state WHERE `key` = :key")
    suspend fun getGameState(key: String): GameState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGameState(state: GameState)
}
