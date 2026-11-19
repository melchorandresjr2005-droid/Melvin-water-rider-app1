package com.example.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExpenseEntity
import com.example.data.ValeLogEntity
import com.example.ui.components.AddExpenseDialog
import com.example.ui.components.AddValeDialog
import com.example.ui.components.BigCard
import com.example.ui.theme.WaterRiderTheme
import com.example.viewmodel.MainViewModel

@Composable
fun ExpensesScreen(viewModel: MainViewModel) {
    val themeColors = WaterRiderTheme.colors
    val expenses by viewModel.expenses.collectAsState()
    val vales by viewModel.vales.collectAsState()

    var showAddExpense by remember { mutableStateOf(false) }
    var showAddVale by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Vale

    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    val totalVales = remember(vales) { vales.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Expenses & Vale", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { if (activeTab == 0) showAddExpense = true else showAddVale = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeTab == 0) themeColors.expense else themeColors.primaryAccent,
                    contentColor = if (activeTab == 0) themeColors.onExpense else themeColors.onPrimaryAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (activeTab == 0) "Add Expense" else "Add Vale", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                label = { Text("Expenses (₱%.0f)".format(totalExpenses), fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f).height(48.dp)
            )
            FilterChip(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                label = { Text("Vale / Pauna (₱%.0f)".format(totalVales), fontWeight = FontWeight.Bold) },
                modifier = Modifier.weight(1f).height(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeTab == 0) {
                if (expenses.isEmpty()) {
                    item(key = "empty_expenses", contentType = "empty_state") {
                        BigCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = themeColors.expense.copy(alpha = 0.65f), modifier = Modifier.size(44.dp))
                                Text("No route expenses logged", fontWeight = FontWeight.Bold)
                                Text("Log Gasolina, Vulcanizing, or Other route costs here.", fontSize = 13.sp, color = themeColors.textSecondary)
                            }
                        }
                    }
                } else {
                    items(
                        items = expenses,
                        key = { it.id },
                        contentType = { "expense" }
                    ) { expense ->
                        ExpenseCard(expense = expense, onDelete = { viewModel.deleteExpense(expense) })
                    }
                }
            } else {
                if (vales.isEmpty()) {
                    item(key = "empty_vales", contentType = "empty_state") {
                        BigCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = themeColors.primaryAccent.copy(alpha = 0.65f), modifier = Modifier.size(44.dp))
                                Text("No cash advances (Vale) logged", fontWeight = FontWeight.Bold)
                                Text("Log mid-week Vale / Pauna payouts from the boss.", fontSize = 13.sp, color = themeColors.textSecondary)
                            }
                        }
                    }
                } else {
                    items(
                        items = vales,
                        key = { it.id },
                        contentType = { "vale" }
                    ) { vale ->
                        ValeCard(vale = vale, onDelete = { viewModel.deleteVale(vale) })
                    }
                }
            }
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            onDismiss = { showAddExpense = false },
            onSave = { category, amount, notes, dateString ->
                viewModel.addExpense(category, amount, notes, dateString)
                showAddExpense = false
            }
        )
    }

    if (showAddVale) {
        AddValeDialog(
            onDismiss = { showAddVale = false },
            onSave = { amount, notes, dateString ->
                viewModel.addVale(amount, notes, dateString)
                showAddVale = false
            }
        )
    }
}

@Composable
fun ExpenseCard(expense: ExpenseEntity, onDelete: () -> Unit) {
    val themeColors = WaterRiderTheme.colors
    BigCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = themeColors.expenseContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            expense.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = themeColors.expense
                        )
                    }
                    if (expense.isRemitted) {
                        Surface(color = themeColors.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                            Text("Remitted", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }
                }
                if (expense.notes.isNotBlank()) {
                    Text(expense.notes, fontSize = 13.sp, color = themeColors.textSecondary)
                }
                Text(expense.dateString, fontSize = 11.sp, color = themeColors.textSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₱%.2f".format(expense.amount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = themeColors.expense)
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = themeColors.utang)
                }
            }
        }
    }
}

@Composable
fun ValeCard(vale: ValeLogEntity, onDelete: () -> Unit) {
    val themeColors = WaterRiderTheme.colors
    BigCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cash Advance (Vale)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeColors.textPrimary)
                    if (vale.isArchived) {
                        Surface(color = themeColors.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                            Text("Archived", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp)
                        }
                    }
                }
                if (vale.notes.isNotBlank()) {
                    Text(vale.notes, fontSize = 13.sp, color = themeColors.textSecondary)
                }
                Text(vale.dateString, fontSize = 11.sp, color = themeColors.textSecondary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₱%.2f".format(vale.amount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = themeColors.textPrimary)
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = themeColors.utang)
                }
            }
        }
    }
}
