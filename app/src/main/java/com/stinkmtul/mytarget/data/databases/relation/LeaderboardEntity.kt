package com.stinkmtul.mytarget.data.databases.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.stinkmtul.mytarget.data.databases.entity.leaderboard.Leaderboard
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.training.Training

data class LeaderboardEntity(
    @Embedded
    val leaderboard: Leaderboard,

    @Relation(
        parentColumn = "person_id",
        entityColumn = "person_id"
    )
    val person: Person? = null,

    @Relation(
        parentColumn = "training_id",
        entityColumn = "training_id"
    )
    val training: Training? = null
)