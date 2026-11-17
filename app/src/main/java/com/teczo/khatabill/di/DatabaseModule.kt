package com.teczo.khatabill.di

import android.content.Context
import androidx.room.Room
import com.teczo.khatabill.data.dao.BusinessDao
import com.teczo.khatabill.data.dao.CategoryDao
import com.teczo.khatabill.data.dao.InvoiceDao
import com.teczo.khatabill.data.dao.ItemDao
import com.teczo.khatabill.data.dao.PartyDao
import com.teczo.khatabill.data.dao.StockDao
import com.teczo.khatabill.data.db.ALL_MIGRATIONS
import com.teczo.khatabill.data.db.KhataBillDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KhataBillDatabase =
        Room.databaseBuilder(context, KhataBillDatabase::class.java, KhataBillDatabase.DB_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

    @Provides fun provideBusinessDao(db: KhataBillDatabase): BusinessDao = db.businessDao()
    @Provides fun provideCategoryDao(db: KhataBillDatabase): CategoryDao = db.categoryDao()
    @Provides fun providePartyDao(db: KhataBillDatabase): PartyDao = db.partyDao()
    @Provides fun provideItemDao(db: KhataBillDatabase): ItemDao = db.itemDao()
    @Provides fun provideInvoiceDao(db: KhataBillDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideStockDao(db: KhataBillDatabase): StockDao = db.stockDao()
}
