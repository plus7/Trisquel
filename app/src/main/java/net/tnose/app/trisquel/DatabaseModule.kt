package net.tnose.app.trisquel

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrisquelRoomDatabase {
        return TrisquelRoomDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDao(database: TrisquelRoomDatabase): TrisquelDao2 {
        return database.trisquelDao()
    }
}
