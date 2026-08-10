package com.shopmate.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shopmate.app.data.local.dao.ProductDao
import com.shopmate.app.data.local.dao.SaleDao
import com.shopmate.app.data.local.entity.Product
import com.shopmate.app.data.local.entity.Sale
import com.shopmate.app.data.local.entity.SaleItem

/**
 * The app's SQLite database (created by Room).
 * Get it anywhere with: AppDatabase.getInstance(context)
 */
@Database(
    entities = [Product::class, Sale::class, SaleItem::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopmate.db"
                )
                    // For learning projects: on a version bump, recreate the DB instead of migrating.
                    .fallbackToDestructiveMigration()
                    // Make SQLite enforce the sale -> sale_items foreign key.
                    .addCallback(
                    object : Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys=ON")
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}
