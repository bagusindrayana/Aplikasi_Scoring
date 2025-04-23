package com.stinkmtul.mytarget.data.databases.entity.shot

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Shot(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "shot_id")
    var shot_id: Int = 0,

    @ColumnInfo(name = "session")
    var session: Int? = null,

    @ColumnInfo(name = "shot_number")
    var shot_number: Int? = null,

    @ColumnInfo(name = "score")
    var score: Int? = null,

    @ColumnInfo(name = "person_id")
    var person_id: Int? = null,

    @ColumnInfo(name = "training_id")
    var training_id: Int? = null,

    @ColumnInfo(name = "scoretype")
    var scoretype: String? = null,
) : Parcelable