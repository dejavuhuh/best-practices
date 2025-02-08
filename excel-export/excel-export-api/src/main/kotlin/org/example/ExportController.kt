package org.example

import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.bind.annotation.*
import java.time.Instant
import kotlin.streams.asSequence

@RestController
@RequestMapping("/export")
class ExportController(
    val sqlClient: KSqlClient,
    val jdbcTemplate: JdbcTemplate,
    val redisTemplate: StringRedisTemplate,
    val exportEventChannel: ExportEventChannel
) {

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
                var current = 0L

                val wb = SXSSFWorkbook(100)

                jdbcTemplate
                    .queryForStream("SELECT * FROM person", ColumnMapRowMapper())
                    .asSequence()
                    .chunked(10000)
                    .forEach {
                        Thread.sleep(10)
                        current += it.size
                        val progress = (current * 99 / total)
                        redisTemplate.convertAndSend("export:$taskId", progress.toString())
                    }

                // upload file to S3
                sqlClient.update(ExportTask {
                    id = taskId
                    s3ObjectKey = "xxx"
                    finishedAt = Instant.now()
                    state = ExportState.COMPLETED
                })
                redisTemplate.convertAndSend("export:$taskId", "100")
            }

        return taskId
    }

    @GetMapping("/status")
    fun status(@RequestParam taskId: Long) = exportEventChannel.subscribe(taskId)

    @GetMapping("/tasks")
    fun tasks() = sqlClient.executeQuery(ExportTask::class) {
        select(table)
    }
}
