package com.beeeam.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * 인터벌 운동 기록 DB. 운동 종류마다 필요한 값과 버전 관리가 달라서 러닝 기록([RunningDatabase])과 따로 둔다.
 *
 * 테이블을 바꾸면 `version` 을 올리고 마이그레이션을 추가한다. 버전별 구조는 `data/local/schemas/` 에 남는다.
 */
@Database(
    entities = [IntervalSessionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class IntervalDatabase : RoomDatabase() {
    abstract fun intervalSessionDao(): IntervalSessionDao
}

/**
 * 앱이 강제 종료돼 진행 중으로 남은 기록을, 마지막으로 저장한 시각에 종료한 것으로 마무리한다.
 *
 * DB 를 여는 것은 앱 프로세스마다 한 번이고, 새 프로세스가 시작된 시점에는 진행 중인 운동이 있을 수 없다.
 * 그래서 이때 남아 있는 진행 중 기록은 모두 이전 실행에서 끝나지 못한 것이다.
 * 모든 조회·저장은 DB 를 연 뒤에 실행되므로, 새 기록을 만들거나 목록을 읽기 전에 항상 먼저 정리된다.
 */
private object CloseUnfinishedSessionsCallback : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE interval_session SET ended_at = updated_at WHERE ended_at IS NULL")
    }
}

/** [IntervalDatabase] 와 DAO 를 그래프에 제공한다. */
@BindingContainer
@ContributesTo(AppScope::class)
object IntervalDatabaseProviders {

    /** DB 연결은 무거우므로 앱 전체에서 하나만 만든다. [context] 는 그래프의 Application Context 다. */
    @Provides
    @SingleIn(AppScope::class)
    fun provideIntervalDatabase(context: Context): IntervalDatabase =
        Room.databaseBuilder(context, IntervalDatabase::class.java, DATABASE_NAME)
            .addCallback(CloseUnfinishedSessionsCallback)
            .build()

    @Provides
    fun provideIntervalSessionDao(database: IntervalDatabase): IntervalSessionDao = database.intervalSessionDao()

    private const val DATABASE_NAME = "interval.db"
}
