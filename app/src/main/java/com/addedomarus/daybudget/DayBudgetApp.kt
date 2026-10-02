package com.addedomarus.daybudget

import android.app.Application
import android.content.Context
import com.addedomarus.daybudget.core.DayBudgetDatabase
import com.addedomarus.daybudget.core.EventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@HiltAndroidApp
class DayBudgetApp : Application()

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context) = DayBudgetDatabase.open(context)

    @Provides
    fun events(db: DayBudgetDatabase): EventDao = db.events()
}
