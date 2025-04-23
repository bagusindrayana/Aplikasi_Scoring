package com.stinkmtul.mytarget.data.databases.entity.person

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Person(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "person_id")
    var person_id: Int = 0,

    @ColumnInfo(name = "name")
    var name: String? = null
) : Parcelable
