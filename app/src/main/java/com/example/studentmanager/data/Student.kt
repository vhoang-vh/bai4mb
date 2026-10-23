package com.example.studentmanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val studentNumber: String,
    val email: String,
    val avatar: String = ""
)
