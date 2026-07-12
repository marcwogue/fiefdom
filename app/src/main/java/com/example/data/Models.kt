package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // "COMMUNITY" or "PERSONAL"
    val completed: Boolean = false,
    val pointsReward: Int, // 10 for community, 5 for personal
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "members")
data class CommunityMember(
    @PrimaryKey val id: String,
    val name: String,
    val title: String, // "Empereur", "Roi", "Prince", "Duc", "Marquis", "Comte", "Vicomte", "Baron", "Chevalier", "Seigneur", "Artisan", "Ouvrier", "Esclave"
    val points: Int,
    val gridX: Int,
    val gridY: Int,
    val statusMessage: String,
    val isPlayer: Boolean = false,
    val avatarEmoji: String = "👤",
    val dailyQuest: String? = null,
    val dailyQuestReward: Int = 0,
    val dailyQuestStatus: String? = null, // "En cours", "Accompli", "Échoué"
    val passwordHash: String? = null // For user authentication
)

@Entity(tableName = "tax_logs")
data class TaxLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val payerName: String,
    val payerTitle: String,
    val initialAmount: Int,
    val details: String // Description of who got what share
)

@Entity(tableName = "game_state")
data class GameState(
    @PrimaryKey val key: String,
    val value: String
)
