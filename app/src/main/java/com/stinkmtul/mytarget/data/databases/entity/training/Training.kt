package com.stinkmtul.mytarget.data.databases.entity.training

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Training(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "training_id")
    var training_id: Int = 0,

    @ColumnInfo(name = "date")
    var date: String? = null,

    @ColumnInfo(name = "description") 
    var description: String? = null,

    @ColumnInfo(name = "session_count")
    var session_count: Int? = null,

    @ColumnInfo(name = "shot_count")
    var shot_count: Int? = null,

    @ColumnInfo(name = "token")
    var token: String? = null
) : Parcelable
