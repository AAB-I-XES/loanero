package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val isMonthlyCollected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
