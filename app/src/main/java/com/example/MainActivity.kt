package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.LoanRepository
import com.example.ui.LoanViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = LoanRepository(
      database.memberDao(),
      database.loanDao(),
      database.repaymentDao()
    )

    setContent {
      MyApplicationTheme {
        val viewModel: LoanViewModel by viewModels {
          LoanViewModel.Factory(repository)
        }
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          bottomBar = {
            if (currentRoute == "dashboard" || currentRoute == "monthly_collection") {
              NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar")
              ) {
                NavigationBarItem(
                  selected = currentRoute == "dashboard",
                  onClick = {
                    if (currentRoute != "dashboard") {
                      navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                      }
                    }
                  },
                  icon = { Icon(Icons.Rounded.Handshake, contentDescription = "Loans") },
                  label = { Text("Loans") },
                  modifier = Modifier.testTag("nav_loans_tab")
                )
                NavigationBarItem(
                  selected = currentRoute == "monthly_collection",
                  onClick = {
                    if (currentRoute != "monthly_collection") {
                      navController.navigate("monthly_collection") {
                        popUpTo("dashboard") { inclusive = false }
                        launchSingleTop = true
                      }
                    }
                  },
                  icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Monthly Collection") },
                  label = { Text("Collection") },
                  modifier = Modifier.testTag("nav_collection_tab")
                )
              }
            }
          }
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("login") {
              LoginScreen(
                onLoginSuccess = { name, email, photoUrl ->
                  viewModel.signInGoogle(name, email, photoUrl) {
                    navController.navigate("dashboard") {
                      popUpTo("login") { inclusive = true }
                    }
                  }
                }
              )
            }

            composable("dashboard") {
              val stats by viewModel.dashboardStats.collectAsState()
              val summaries by viewModel.memberSummaries.collectAsState()
              val user by viewModel.currentUser.collectAsState()
              val syncLog by viewModel.syncStatus.collectAsState()

              DashboardScreen(
                stats = stats,
                memberSummaries = summaries,
                onMemberClick = { id ->
                  navController.navigate("member_detail/$id")
                },
                onAddMemberClick = {
                  navController.navigate("add_member")
                },
                currentUser = user,
                syncStatus = syncLog,
                onSyncClick = { viewModel.syncWithSupabase() },
                onSignOutClick = {
                  viewModel.signOut()
                  navController.navigate("login") {
                    popUpTo("dashboard") { inclusive = true }
                  }
                }
              )
            }

            composable("monthly_collection") {
              val membersList by viewModel.members.collectAsState()

              MonthlyCollectionScreen(
                members = membersList,
                onToggleCollected = { member ->
                  viewModel.toggleMonthlyCollected(member)
                },
                onResetCollections = {
                  viewModel.resetMonthlyCollections()
                }
              )
            }

            composable("add_member") {
              AddMemberScreen(
                onBack = { navController.popBackStack() },
                onSave = { name, phone, email, notes ->
                  viewModel.addMember(name, phone, email, notes) {
                    navController.popBackStack()
                  }
                }
              )
            }

            composable(
              "edit_member/{memberId}",
              arguments = listOf(navArgument("memberId") { type = NavType.IntType })
            ) { backStackEntry ->
              val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
              val summaries by viewModel.memberSummaries.collectAsState()
              val summary = summaries.firstOrNull { it.member.id == memberId }

              AddMemberScreen(
                onBack = { navController.popBackStack() },
                onSave = { name, phone, email, notes ->
                  if (summary != null) {
                    viewModel.updateMember(
                      summary.member.copy(
                        name = name,
                        phone = phone,
                        email = email,
                        notes = notes
                      )
                    )
                  }
                  navController.popBackStack()
                },
                memberToEdit = summary?.member
              )
            }

            composable(
              "member_detail/{memberId}",
              arguments = listOf(navArgument("memberId") { type = NavType.IntType })
            ) { backStackEntry ->
              val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
              val summary by viewModel.getMemberSummary(memberId).collectAsState(initial = null)
              val loansList by viewModel.getLoansWithSummaries(memberId).collectAsState(initial = emptyList())

              MemberDetailScreen(
                memberSummary = summary,
                loansList = loansList,
                onBack = { navController.popBackStack() },
                onEditMember = { id ->
                  navController.navigate("edit_member/$id")
                },
                onDeleteMember = { member ->
                  viewModel.deleteMember(member)
                  navController.popBackStack()
                },
                onAddLoan = { id ->
                  navController.navigate("add_loan/$id")
                },
                onLoanClick = { id ->
                  navController.navigate("loan_detail/$id")
                }
              )
            }

            composable(
              "add_loan/{memberId}",
              arguments = listOf(navArgument("memberId") { type = NavType.IntType })
            ) { backStackEntry ->
              val memberId = backStackEntry.arguments?.getInt("memberId") ?: 0
              val summaries by viewModel.memberSummaries.collectAsState()
              val summary = summaries.firstOrNull { it.member.id == memberId }

              AddLoanScreen(
                member = summary?.member,
                onBack = { navController.popBackStack() },
                onSave = { principal, interestRate, interestType, purpose, notes, startDate ->
                  viewModel.addLoan(
                    memberId = memberId,
                    principalAmount = principal,
                    interestRate = interestRate,
                    interestType = interestType,
                    purpose = purpose,
                    notes = notes,
                    startDate = startDate
                  ) {
                    navController.popBackStack()
                  }
                }
              )
            }

            composable(
              "loan_detail/{loanId}",
              arguments = listOf(navArgument("loanId") { type = NavType.IntType })
            ) { backStackEntry ->
              val loanId = backStackEntry.arguments?.getInt("loanId") ?: 0
              val summary by viewModel.getLoanSummary(loanId).collectAsState(initial = null)
              val membersList by viewModel.members.collectAsState()
              val borrower = summary?.let { s -> membersList.firstOrNull { it.id == s.loan.memberId } }

              LoanDetailScreen(
                loanSummary = summary,
                borrower = borrower,
                onBack = { navController.popBackStack() },
                onAddRepayment = { amount, date, notes ->
                  viewModel.addRepayment(loanId, amount, date, notes)
                },
                onDeleteRepayment = { repayment ->
                  viewModel.deleteRepayment(repayment)
                },
                onToggleFinished = {
                  summary?.loan?.let { loan ->
                    viewModel.toggleLoanFinished(loan)
                  }
                },
                onDeleteLoan = {
                  summary?.loan?.let { loan ->
                    viewModel.deleteLoan(loan)
                    navController.popBackStack()
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}

