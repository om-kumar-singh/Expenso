package com.example.expensesbyom

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MonthlyNotesDaoTest {

    private lateinit var database: ExpenseDatabase
    private lateinit var dao: MonthlyNotesDao
    private var accountId: Long = 1

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ExpenseDatabase::class.java
        ).allowMainThreadQueries().build()
        accountId = database.repository().ensureDefaultAccount().id
        dao = database.monthlyNotesDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndLoadSeptemberNotes() {
        val notes = """
            11- milk-46
            dosa-120
            12- charity-51
        """.trimIndent()
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 9, notes))
        assertEquals(notes, dao.getNotes(accountId, 2026, 9)?.notesText)
    }

    @Test
    fun septemberAndOctoberStaySeparate() {
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 9, "11- milk-46"))
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 10, "1- rent-8000"))
        assertEquals("11- milk-46", dao.getNotes(accountId, 2026, 9)?.notesText)
        assertEquals("1- rent-8000", dao.getNotes(accountId, 2026, 10)?.notesText)
    }

    @Test
    fun updatingSeptemberReplacesPreviousNotes() {
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 9, "11- milk-46"))
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 9, "11- milk-50\ndosa-120"))
        assertEquals("11- milk-50\ndosa-120", dao.getNotes(accountId, 2026, 9)?.notesText)
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM monthly_notes").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }

    @Test
    fun deleteRemovesMonth() {
        dao.upsertNotes(MonthlyNotesEntity(accountId, 2026, 9, "11- milk-46"))
        dao.deleteNotes(accountId, 2026, 9)
        assertNull(dao.getNotes(accountId, 2026, 9))
    }
}
