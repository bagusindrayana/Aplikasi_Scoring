package com.stinkmtul.mytarget.data.databases

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.LeaderboardDao
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.person.PersonDao
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.shot.ShotDao
import com.stinkmtul.mytarget.data.databases.entity.training.Training
import com.stinkmtul.mytarget.data.databases.entity.training.TrainingDao

@Database(entities = [Person::class, Training::class, Shot::class, Leaderboard::class], version= 7)
abstract class MyArcheryRoomDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun trainingDao(): TrainingDao
    abstract fun shotDao(): ShotDao
    abstract fun leaderboardDao(): LeaderboardDao

    companion object {
        @Volatile
        private var INSTANCE: MyArcheryRoomDatabase? = null
        @JvmStatic
        fun getDatabase(context: Context): MyArcheryRoomDatabase {
            if (INSTANCE == null) {
                synchronized(MyArcheryRoomDatabase::class.java) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        MyArcheryRoomDatabase::class.java, "myarchery")
                        .build()
                }
            }
            return INSTANCE as MyArcheryRoomDatabase
        }
    }
}
