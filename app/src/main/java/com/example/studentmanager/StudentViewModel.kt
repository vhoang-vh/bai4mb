package com.example.studentmanager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studentmanager.data.Student
import com.example.studentmanager.data.StudentDatabase
import com.example.studentmanager.data.StudentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StudentRepository(
        StudentDatabase.getInstance(application).studentDao()
    )

    val students = repository.students.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    fun observeStudent(id: Long) = repository.observeStudent(id)

    fun addStudent(student: Student, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.add(student)
            onComplete()
        }
    }

    fun updateStudent(student: Student, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.update(student)
            onComplete()
        }
    }

    fun deleteStudent(student: Student, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.delete(student)
            onComplete()
        }
    }
}
