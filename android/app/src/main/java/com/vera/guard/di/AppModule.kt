package com.vera.guard.di

import android.content.Context
import androidx.room.Room
import com.vera.guard.data.AppDatabase
import com.vera.guard.data.SecureStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** Room on SQLCipher: the database file is AES-encrypted; the passphrase lives in EncryptedSharedPreferences. */
    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context, store: SecureStore): AppDatabase {
        System.loadLibrary("sqlcipher")
        val factory = SupportOpenHelperFactory(store.dbPassphrase().toByteArray())
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "vera.db").openHelperFactory(factory).build()
    }
    @Provides fun events(db: AppDatabase) = db.events()
    @Provides fun nearby(db: AppDatabase) = db.nearby()
}
