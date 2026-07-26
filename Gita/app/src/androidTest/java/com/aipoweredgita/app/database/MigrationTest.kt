package com.aipoweredgita.app.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GitaDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        // Create earliest database schema (version 1) and apply all migrations up to current version 42
        helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Open latest database version and validate schema
        helper.runMigrationsAndValidate(TEST_DB, 42, true, *ALL_MIGRATIONS)
    }

    companion object {
        private val ALL_MIGRATIONS = GitaDatabase.ALL_MIGRATIONS
    }
}
