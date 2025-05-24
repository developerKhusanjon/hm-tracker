package com.hmtacker.config

case class DatabaseConfig(
                           driver: String = "org.postgresql.Driver",
                           url: String = "jdbc:postgresql://localhost:5432/academic_tasks",
                           username: String = "postgres",
                           password: String = "password",
                           maxPoolSize: Int = 10
                         )