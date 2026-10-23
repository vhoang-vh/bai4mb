package com.example.studentmanager.data

class StudentRepository(private val studentDao: StudentDao) {
    val students = studentDao.observeAll()

    fun observeStudent(id: Long) = studentDao.observeById(id)

    suspend fun add(student: Student) = studentDao.insert(student)

    suspend fun update(student: Student) = studentDao.update(student)

    suspend fun delete(student: Student) = studentDao.delete(student)
}
