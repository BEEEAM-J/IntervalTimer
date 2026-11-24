package com.beeeam.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * 러닝 기록 DB.
 *
 * 테이블을 바꾸면 `version` 을 올리고 마이그레이션을 추가한다. 버전별 구조는 `data/local/schemas/` 에 남는다.
 */
@Database(
    entities = [RunningSessionEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RunningDatabase : RoomDatabase() {
    abstract fun runningSessionDao(): RunningSessionDao
}

/** [RunningDatabase] 와 DAO 를 그래프에 제공한다. */
@BindingContainer
@ContributesTo(AppScope::class)
object RunningDatabaseProviders {

    /** DB 연결은 무거우므로 앱 전체에서 하나만 만든다. [context] 는 그래프의 Application Context 다. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideRunningDatabase(context: Context): RunningDatabase =
        Room.databaseBuilder(context, RunningDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideRunningSessionDao(database: RunningDatabase): RunningSessionDao = database.runningSessionDao()

    private const val DATABASE_NAME = "running.db"
}
