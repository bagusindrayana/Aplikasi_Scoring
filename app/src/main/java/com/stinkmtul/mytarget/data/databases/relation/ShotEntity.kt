package com.stinkmtul.mytarget.data.databases.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.stinkmtul.mytarget.data.databases.entity.person.Person
import com.stinkmtul.mytarget.data.databases.entity.shot.Shot
import com.stinkmtul.mytarget.data.databases.entity.training.Training

data class ShotEntity (
    @Embedded
    val shot: Shot,

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