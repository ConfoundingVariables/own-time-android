package com.addedomarus.daybudget

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.addedomarus.daybudget.core.DayBudgetDatabase
import com.addedomarus.daybudget.core.EventStore
import com.addedomarus.daybudget.core.exportSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class Exporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: EventStore,
    private val db: DayBudgetDatabase,
) {
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        File(dir, "day-budget-${Instant.now().epochSecond}.db").apply { exportSnapshot(db, store.ledgerNow(), this, Instant.now(), ZoneId.systemDefault()) }
    }

    fun share(file: File): Intent = Intent(Intent.ACTION_SEND)
        .setType("application/x-sqlite3")
        .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, "$packageName.fileprovider", file))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    private val packageName get() = context.packageName
}
