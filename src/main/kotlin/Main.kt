import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Transaction(
    val id: Int,
    val amount: Double,
    val category: String,
    val description: String,
    val date: String,
    val type: TransactionType
)

enum class TransactionType { INCOME, EXPENSE }

class FinanceManager {
    private val transactions = mutableListOf<Transaction>()
    private var nextId = 1
    private val dataFile = "finances.json"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadData()

    }

    fun addTransaction(amount: Double, category: String, description: String, type: TransactionType) {
        require(amount > 0) { "Amount must be positive" }
        require(category.isNotBlank()) { "Category cannot be empty" }
        require(description.isNotBlank()) { "Description cannot be empty" }
        val transactionId = nextId++

        val transaction = Transaction(
            id = transactionId,  // ✅ USE THE INCREMENTED ID
            amount = amount,
            category = category,
            description = description,
            date = LocalDate.now().format(dateFormatter),
            type = type
        )
        transactions.add(transaction)
        saveData()
        println("✅ ${type.name.lowercase().replaceFirstChar { it.uppercase() }} added successfully!")
    }

    fun getBalance(): Double {
        return transactions.sumOf {
            if (it.type == TransactionType.INCOME) it.amount else -it.amount
        }
    }
    fun getMonthlyReport(month: Int, year: Int): Map<String, Any> {
        val monthlyTransactions = transactions.filter {
            try {
                val transactionDate = LocalDate.parse(it.date)
                transactionDate.monthValue == month && transactionDate.year == year
            } catch (e: Exception) {
                false
            }
        }

        val income = monthlyTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = monthlyTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val net = income - expenses

        val expenseByCategory = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()

        return mapOf(
            "income" to income,
            "expenses" to expenses,
            "net" to net,
            "transactionCount" to monthlyTransactions.size,
            "topExpenseCategories" to expenseByCategory
        )
    }






    fun getCategorySpending(): Map<String, Double> {
        return transactions
            .filter { it.type == TransactionType.EXPENSE }  // ✅ CONSISTENT
            .groupBy { it.category }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

    }

    fun searchTransactions(query: String): List<Transaction> {
        return transactions.filter {
            it.description.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
        }
    }

    fun deleteTransaction(id: Int): Boolean {
        val removed = transactions.removeAll { it.id == id }
        if (removed) saveData()
        return removed
    }

        fun getRecentTransactions(limit: Int = 10): List<Transaction> {
            return transactions.sortedByDescending { it.date }.take(limit)
        }

    private fun saveData() {
        try {
            val jsonArray = JSONArray()
            transactions.forEach { transaction ->
                val jsonObject = JSONObject().apply {
                    put("id", transaction.id)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("description", transaction.description)
                    put("date", transaction.date)
                    put("type", transaction.type.name)


                }
                jsonArray.put(jsonObject)
            }
            File(dataFile).writeText(jsonArray.toString(2))
        }catch (e: Exception){
            println("❌ Error saving data: ${e.message}")



        }

    }
    private fun loadData(){
        try {
            if (File(dataFile).exists()) {
                val json = File(dataFile).readText()
                val jsonArray = JSONArray(json)

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val transaction = Transaction(
                        id = jsonObject.getInt("id"),
                        amount = jsonObject.getDouble("amount"),
                        category = jsonObject.getString("category"),
                        description = jsonObject.getString("description"),
                        date = jsonObject.getString("date"),
                        type = TransactionType.valueOf(jsonObject.getString("type"))

                    )
                    transactions.add(transaction)
                }
                nextId = (transactions.maxOfOrNull { it.id } ?: 0) + 1
            }
        }catch (e: Exception)    {
            println("❌ Error loading data: ${e.message}")

        }
    }
}
