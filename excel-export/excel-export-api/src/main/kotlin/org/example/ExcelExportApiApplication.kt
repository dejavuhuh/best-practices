package org.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExcelExportApiApplication

fun main(args: Array<String>) {
    runApplication<ExcelExportApiApplication>(*args)
}
