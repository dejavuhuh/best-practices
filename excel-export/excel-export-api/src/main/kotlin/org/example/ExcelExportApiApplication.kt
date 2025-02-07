package org.example

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class ExcelExportApiApplication {

    @Bean
    fun runner(sqlClient: KSqlClient) = ApplicationRunner {
        val tasks = sqlClient.executeQuery(ExportTask::class) {
            select(table)
        }
        println()
    }
}

fun main(args: Array<String>) {
    runApplication<ExcelExportApiApplication>(*args)
}
