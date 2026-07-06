package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Loan
import com.example.data.LoanRepository
import com.example.data.Member
import com.example.data.Repayment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class MemberSummary(
    val member: Member,
    val totalLoansCount: Int,
    val pendingLoansCount: Int,
    val totalPrincipalAmount: Double,
    val totalOwedAmount: Double,
    val totalReturnedAmount: Double,
    val totalPendingAmount: Double
)

data class LoanSummary(
    val loan: Loan,
    val totalOwed: Double,
    val totalReturned: Double,
    val totalPending: Double,
    val repayments: List<Repayment>
)

data class DashboardStats(
    val totalMembersCount: Int,
    val totalPrincipalLoaned: Double,
    val totalInterestAccrued: Double,
    val totalOwed: Double,
    val totalReturned: Double,
    val totalPending: Double,
    val activeLoansCount: Int,
    val finishedLoansCount: Int
)

class LoanViewModel(private val repository: LoanRepository) : ViewModel() {

    // Global lists from Database
    val members: StateFlow<List<Member>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loans: StateFlow<List<Loan>> = repository.allLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val repayments: StateFlow<List<Repayment>> = repository.allRepayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined Member Summaries
    val memberSummaries: StateFlow<List<MemberSummary>> = combine(
        members, loans, repayments
    ) { memberList, loanList, repaymentList ->
        val asOfDate = System.currentTimeMillis()
        memberList.map { member ->
            val memberLoans = loanList.filter { it.memberId == member.id }
            var totalPrincipal = 0.0
            var totalOwed = 0.0
            var totalReturned = 0.0
            var pendingCount = 0

            memberLoans.forEach { loan ->
                val loanRepayments = repaymentList.filter { it.loanId == loan.id }
                val returned = loanRepayments.sumOf { it.amount }
                val owed = loan.getTotalOwed(asOfDate)
                val isFinished = loan.isFinished || (owed - returned <= 0.01)

                totalPrincipal += loan.principalAmount
                totalOwed += owed
                totalReturned += returned
                if (!isFinished) {
                    pendingCount++
                }
            }

            val pending = maxOf(0.0, totalOwed - totalReturned)

            MemberSummary(
                member = member,
                totalLoansCount = memberLoans.size,
                pendingLoansCount = pendingCount,
                totalPrincipalAmount = roundTwoDecimals(totalPrincipal),
                totalOwedAmount = roundTwoDecimals(totalOwed),
                totalReturnedAmount = roundTwoDecimals(totalReturned),
                totalPendingAmount = roundTwoDecimals(pending)
            )
        }.sortedBy { it.member.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard overall statistics
    val dashboardStats: StateFlow<DashboardStats> = memberSummaries.map { summaries ->
        var totalPrincipal = 0.0
        var totalOwed = 0.0
        var totalReturned = 0.0
        var totalPending = 0.0
        var activeLoans = 0
        var finishedLoans = 0

        summaries.forEach { summary ->
            totalPrincipal += summary.totalPrincipalAmount
            totalOwed += summary.totalOwedAmount
            totalReturned += summary.totalReturnedAmount
            totalPending += summary.totalPendingAmount
            activeLoans += summary.pendingLoansCount
            finishedLoans += (summary.totalLoansCount - summary.pendingLoansCount)
        }

        val totalInterest = maxOf(0.0, totalOwed - totalPrincipal)

        DashboardStats(
            totalMembersCount = summaries.size,
            totalPrincipalLoaned = roundTwoDecimals(totalPrincipal),
            totalInterestAccrued = roundTwoDecimals(totalInterest),
            totalOwed = roundTwoDecimals(totalOwed),
            totalReturned = roundTwoDecimals(totalReturned),
            totalPending = roundTwoDecimals(totalPending),
            activeLoansCount = activeLoans,
            finishedLoansCount = finishedLoans
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0))

    // Helper functions for Member Details
    fun getMemberSummary(memberId: Int): Flow<MemberSummary?> {
        return memberSummaries.map { summaries ->
            summaries.firstOrNull { it.member.id == memberId }
        }
    }

    fun getLoansWithSummaries(memberId: Int): Flow<List<LoanSummary>> {
        return combine(loans, repayments) { loanList, repaymentList ->
            val asOfDate = System.currentTimeMillis()
            loanList.filter { it.memberId == memberId }.map { loan ->
                val loanRepayments = repaymentList.filter { it.loanId == loan.id }.sortedByDescending { it.paymentDate }
                val returned = loanRepayments.sumOf { it.amount }
                val owed = loan.getTotalOwed(asOfDate)
                val pending = maxOf(0.0, owed - returned)

                LoanSummary(
                    loan = loan,
                    totalOwed = roundTwoDecimals(owed),
                    totalReturned = roundTwoDecimals(returned),
                    totalPending = roundTwoDecimals(pending),
                    repayments = loanRepayments
                )
            }.sortedByDescending { it.loan.startDate }
        }
    }

    fun getLoanSummary(loanId: Int): Flow<LoanSummary?> {
        return combine(loans, repayments) { loanList, repaymentList ->
            val asOfDate = System.currentTimeMillis()
            val loan = loanList.firstOrNull { it.id == loanId }
            if (loan != null) {
                val loanRepayments = repaymentList.filter { it.loanId == loan.id }.sortedByDescending { it.paymentDate }
                val returned = loanRepayments.sumOf { it.amount }
                val owed = loan.getTotalOwed(asOfDate)
                val pending = maxOf(0.0, owed - returned)

                LoanSummary(
                    loan = loan,
                    totalOwed = roundTwoDecimals(owed),
                    totalReturned = roundTwoDecimals(returned),
                    totalPending = roundTwoDecimals(pending),
                    repayments = loanRepayments
                )
            } else {
                null
            }
        }
    }

    // Operations / Actions
    fun addMember(name: String, phone: String, email: String, notes: String, onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val member = Member(name = name, phone = phone, email = email, notes = notes)
            val newId = repository.insertMember(member)
            onComplete(newId.toInt())
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member)
        }
    }

    fun toggleMonthlyCollected(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member.copy(isMonthlyCollected = !member.isMonthlyCollected))
        }
    }

    fun resetMonthlyCollections() {
        viewModelScope.launch {
            members.value.forEach { member ->
                if (member.isMonthlyCollected) {
                    repository.updateMember(member.copy(isMonthlyCollected = false))
                }
            }
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun addLoan(
        memberId: Int,
        principalAmount: Double,
        interestRate: Double,
        interestType: String,
        purpose: String,
        notes: String,
        startDate: Long,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val loan = Loan(
                memberId = memberId,
                principalAmount = principalAmount,
                interestRate = interestRate,
                interestType = interestType,
                purpose = purpose,
                notes = notes,
                startDate = startDate
            )
            val newId = repository.insertLoan(loan)
            onComplete(newId.toInt())
        }
    }

    fun updateLoan(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
        }
    }

    fun toggleLoanFinished(loan: Loan) {
        viewModelScope.launch {
            repository.updateLoan(loan.copy(isFinished = !loan.isFinished))
        }
    }

    fun addRepayment(loanId: Int, amount: Double, paymentDate: Long, notes: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val repayment = Repayment(
                loanId = loanId,
                amount = amount,
                paymentDate = paymentDate,
                notes = notes
            )
            repository.insertRepayment(repayment)
            onComplete()
        }
    }

    fun deleteRepayment(repayment: Repayment) {
        viewModelScope.launch {
            repository.deleteRepayment(repayment)
        }
    }

    private fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    class Factory(private val repository: LoanRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return LoanViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
