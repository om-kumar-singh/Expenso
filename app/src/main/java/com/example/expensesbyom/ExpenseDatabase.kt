package com.example.expensesbyom

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(
    entities = [AccountEntity::class, MonthlyNotesEntity::class, TransactionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun monthlyNotesDao(): MonthlyNotesDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "expenses.db"
        const val DEFAULT_ACCOUNT_NAME = "My Expenses"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `month_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `originalLine` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_month_transactions_year_month` ON `month_transactions` (`year`, `month`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "INSERT INTO `accounts` (`id`, `name`, `isDefault`) VALUES (1, '$DEFAULT_ACCOUNT_NAME', 1)"
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monthly_notes_new` (
                        `accountId` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `notesText` TEXT NOT NULL,
                        PRIMARY KEY(`accountId`, `year`, `month`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `monthly_notes_new` (`accountId`, `year`, `month`, `notesText`)
                    SELECT 1, `year`, `month`, `notesText` FROM `monthly_notes`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `monthly_notes`")
                db.execSQL("ALTER TABLE `monthly_notes_new` RENAME TO `monthly_notes`")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `month_transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        `month` INTEGER NOT NULL,
                        `day` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `amount` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `originalLine` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `month_transactions_new`
                    (`id`, `accountId`, `year`, `month`, `day`, `description`, `amount`, `type`, `originalLine`)
                    SELECT `id`, 1, `year`, `month`, `day`, `description`, `amount`, `type`, `originalLine`
                    FROM `month_transactions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `month_transactions`")
                db.execSQL("ALTER TABLE `month_transactions_new` RENAME TO `month_transactions`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_month_transactions_accountId_year_month` ON `month_transactions` (`accountId`, `year`, `month`)"
                )
            }
        }

        @Volatile
        private var instance: ExpenseDatabase? = null

        val ioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

        fun getInstance(context: Context): ExpenseDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseDatabase::class.java,
                    DB_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}
