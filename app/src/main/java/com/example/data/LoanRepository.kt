package com.example.data

import kotlinx.coroutines.flow.Flow

class LoanRepository(
    private val memberDao: MemberDao,
    private val loanDao: LoanDao,
    private val repaymentDao: RepaymentDao
) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()
    val allLoans: Flow<List<Loan>> = loanDao.getAllLoans()
    val allRepayments: Flow<List<Repayment>> = repaymentDao.getAllRepayments()

    fun getLoansForMember(memberId: Int): Flow<List<Loan>> {
        return loanDao.getLoansForMember(memberId)
    }

    fun getRepaymentsForLoan(loanId: Int): Flow<List<Repayment>> {
        return repaymentDao.getRepaymentsForLoan(loanId)
    }

    fun getMemberById(id: Int): Flow<Member?> {
        return memberDao.getMemberById(id)
    }

    fun getLoanById(id: Int): Flow<Loan?> {
        return loanDao.getLoanById(id)
    }

    suspend fun insertMember(member: Member): Long {
        return memberDao.insertMember(member)
    }

    suspend fun updateMember(member: Member) {
        memberDao.updateMember(member)
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
    }

    suspend fun insertLoan(loan: Loan): Long {
        return loanDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loan)
    }

    suspend fun insertRepayment(repayment: Repayment): Long {
        return repaymentDao.insertRepayment(repayment)
    }

    suspend fun deleteRepayment(repayment: Repayment) {
        repaymentDao.deleteRepayment(repayment)
    }
}
