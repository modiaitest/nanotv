package app.nanotv.tv.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CuratedSourceEntity::class,
        PlaylistEntity::class,
        VideoEntity::class,
        PlaylistVideoCrossRef::class,
        ChannelEntity::class,
        WatchStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NanoTvDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: NanoTvDatabase? = null

        fun getInstance(context: Context): NanoTvDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NanoTvDatabase::class.java,
                    "nanotv_family.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
