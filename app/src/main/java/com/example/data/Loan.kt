package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.roundToInt

@Entity(
    tableName = "loans",
    foreignKeys = [
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val memberId: Int,
    val principalAmount: Double,
    val interestRate: Double, // e.g. 5% or fixed amount $100
    val interestType: String, // "FIXED", "MONTHLY_SIMPLE", "YEARLY_SIMPLE"
    val purpose: String = "",
    val notes: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val isFinished: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun calculateInterest(asOfDate: Long = System.currentTimeMillis()): Double {
        return when (interestType) {
            "FIXED" -> interestRate
            "MONTHLY_SIMPLE" -> {
                val durationMs = maxOf(0L, asOfDate - startDate)
                val months = durationMs.toDouble() / (30.0 * 24.0 * 60.0 * 60.0 * 1000.0)
                val interest = principalAmount * (interestRate / 100.0) * months
                (interest * 100.0).roundToInt() / 100.0
            }
            "YEARLY_SIMPLE" -> {
                val durationMs = maxOf(0L, asOfDate - startDate)
                val years = durationMs.toDouble() / (365.0 * 24.0 * 60.0 * 60.0 * 1000.0)
                val interest = principalAmount * (interestRate / 100.0) * years
                (interest * 100.0).roundToInt() / 100.0
            }
            else -> 0.0
        }
    }

    fun getTotalOwed(asOfDate: Long = System.currentTimeMillis()): Double {
        val calculated = principalAmount + calculateInterest(asOfDate)
        return (calculated * 100.0).roundToInt() / 100.0
    }
}
