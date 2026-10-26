// Implements: SCR-HOME / SCR-CATALOG / SCR-EDITOR / SCR-SAVED / SCR-EXPORT / FUN-SAVE-LOCAL
package com.openkitmaker.android.di

import android.content.Context
import androidx.room.Room
import com.openkitmaker.android.data.local.KitDao
import com.openkitmaker.android.data.local.KitDatabase
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.data.repository.KitRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindKitRepository(impl: KitRepositoryImpl): KitRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideKitDatabase(@ApplicationContext context: Context): KitDatabase =
        Room.databaseBuilder(
            context,
            KitDatabase::class.java,
            "openkitmaker_native.db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideKitDao(db: KitDatabase): KitDao = db.kitDao()
}
