package com.stinkmtul.mytarget.data.databases.entity.leaderboard

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Leaderboard(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "leaderboard_id")
    var leaderboard_id: Int = 0,

    @ColumnInfo(name = "person_id")
    var person_id: Int? = null,

    @ColumnInfo(name = "score")
    var score: Int? = null,

    @ColumnInfo(name = "the_champion")
    var the_champion: Int? = null,

    @ColumnInfo(name = "training_id")
    var training_id: Int? = null
) : Parcelable