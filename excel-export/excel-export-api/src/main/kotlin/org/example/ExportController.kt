package org.example

import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.streaming.DeferredSXSSFWorkbook
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.bind.annotation.*
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.streams.asSequence

@RestController
@RequestMapping("/export")
class ExportController(
    val sqlClient: KSqlClient,
    val jdbcTemplate: JdbcTemplate,
    val redisTemplate: StringRedisTemplate,
    val exportEventChannel: ExportEventChannel
) {

    val minioClient: MinioClient = MinioClient.builder()
        .endpoint("http://localhost:9000")
        .credentials("uBuOzGYe7K9ebxM8uxkU", "5ePr46DXqaJlEE3pmjbxASHeAaJaNFjGjpgq5XyO")
        .build()

    data class AsyncExportRequest(
        val downloadFileName: String
    )

    @PostMapping("/start")
    fun start(@RequestBody request: AsyncExportRequest): Long {
        val total = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM person")
        if (total == 0L) {
            throw IllegalStateException("No data to export")
        }

        val insertResult = sqlClient.insert(ExportTask {
            downloadFileName = request.downloadFileName
        })
        val taskId = insertResult.modifiedEntity.id

        Thread
            .ofVirtual()
            .start {
                asyncRun(total, taskId)
            }

        return taskId
    }

    private fun asyncRun(total: Long, taskId: Long) {
        var current = 0L
        val chunkSize = 10000

        DeferredSXSSFWorkbook(1000).use { wb ->
            wb.createSheet("人员信息").setRowGenerator { sheet ->
                jdbcTemplate
                    .queryForStream("SELECT * FROM person", ColumnMapRowMapper())
                    .asSequence()
                    .chunked(chunkSize)
                    .forEachIndexed { chunkIndex, chunk ->
                        // write data to excel
                        for ((index, person) in chunk.withIndex()) {
                            val row = sheet.createRow(chunkIndex * chunkSize + index)
                            fillRow(row, person)
                        }

                        current += chunk.size
                        val progress = (current * 99 / total)
                        redisTemplate.convertAndSend("export:$taskId", progress.toString())
                    }
            }

            // upload file to S3
            val url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket("export")
                    .`object`("$taskId")
                    .expiry(5, TimeUnit.MINUTES)
                    .build()
            )
            val connection = URI.create(url).toURL().openConnection() as HttpURLConnection
            connection.doOutput = true
            connection.setRequestMethod("PUT")
            connection.outputStream.use { wb.write(it) }
            connection.responseCode

            sqlClient.update(ExportTask {
                id = taskId
                finishedAt = Instant.now()
                state = ExportState.COMPLETED
            })
        }

        redisTemplate.convertAndSend("export:$taskId", "100")
    }

    private fun fillRow(row: Row, person: Map<String, Any>) {
        val id = row.createCell(0)
        val name = row.createCell(1)
        val age = row.createCell(2)
        val email = row.createCell(3)
        val phone = row.createCell(4)
        val address = row.createCell(5)
        val city = row.createCell(6)
        val state = row.createCell(7)
        val zip = row.createCell(8)
        val country = row.createCell(9)
        val createdAt = row.createCell(10)
        val updatedAt = row.createCell(11)

        id.setCellValue((person["id"] as Long).toDouble())
        name.setCellValue(person["name"] as String)
        age.setCellValue((person["age"] as Int).toDouble())
        email.setCellValue(person["email"] as String)
        phone.setCellValue(person["phone"] as String)
        address.setCellValue(person["address"] as String)
        city.setCellValue(person["city"] as String)
        state.setCellValue(person["state"] as String)
        zip.setCellValue(person["zip"] as String)
        country.setCellValue(person["country"] as String)
        createdAt.setCellValue(person["created_at"] as Timestamp)
        updatedAt.setCellValue(person["updated_at"] as Timestamp)
    }

    @GetMapping("/status")
    fun status(@RequestParam taskId: Long) = exportEventChannel.subscribe(taskId)

    @GetMapping("/tasks")
    fun tasks() = sqlClient.executeQuery(ExportTask::class) {
        select(table)
    }

    @GetMapping("/url")
    fun url(@RequestParam taskId: Long): String {
        val task = sqlClient.findOneById(ExportTask::class, taskId)
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket("export")
                .`object`("$taskId")
                .expiry(5, TimeUnit.MINUTES)
                .extraQueryParams(
                    mapOf(
                        "response-content-disposition" to "attachment; filename*=UTF-8''${
                            URLEncoder.encode(
                                "${task.downloadFileName}.xlsx",
                                StandardCharsets.UTF_8
                            )
                        }"
                    )
                )
                .build()
        )
    }
}
