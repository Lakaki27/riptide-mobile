package com.evertide.riptide.di

import android.content.Context
import androidx.room.Room
import com.evertide.riptide.data.local.AppDatabase
import com.evertide.riptide.data.local.CachedImageDao
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
    fun db(@ApplicationContext c: Context): AppDatabase =
            Room.databaseBuilder(c, AppDatabase::class.java, "riptide.db").build()

    @Provides fun cachedImageDao(db: AppDatabase): CachedImageDao = db.cachedImageDao()
}
