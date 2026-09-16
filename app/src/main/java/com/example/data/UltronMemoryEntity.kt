package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ultron_memory")
data class UltronMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String, // "SHORT_TERM", "SCENE", "PROJECT", "LEARNING", "PREFERENCE"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
