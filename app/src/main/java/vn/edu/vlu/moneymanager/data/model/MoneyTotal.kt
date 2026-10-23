package vn.edu.vlu.moneymanager.data.model

/** Sum of amounts in mixed currencies, converted to one currency. */
data class MoneyTotal(
    val amount: Double,
    /** true when some rows were skipped because no exchange rate is known for their currency */
    val hasMissingRates: Boolean
)

/** Income counts as positive and expense as negative. */
val Transaction.signedAmount: Double get() = if (type == TransactionType.INCOME) amount else -amount

/**
 * Converts every transaction to [currency] and adds them up.
 * With [type] set only that type is counted (as a positive number); otherwise the result is the net amount.
 */
fun List<Transaction>.totalIn(currency: String, rates: ExchangeRates, type: TransactionType? = null): MoneyTotal {
    var sum = 0.0
    var missing = false
    for (transaction in this) {
        if (type != null && transaction.type != type) continue
        val value = if (type == null) transaction.signedAmount else transaction.amount
        val converted = rates.convert(value, transaction.currency, currency)
        if (converted == null) missing = true else sum += converted
    }
    return MoneyTotal(sum, missing)
}
