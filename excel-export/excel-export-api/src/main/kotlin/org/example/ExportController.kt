package org.example

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.bind.annotation.*
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
        val insertResult = sqlClient.insert(ExportTask {
            downloadFileName = request.downloadFileName
        })
        val taskId = insertResult.modifiedEntity.id

        Thread
            .ofVirtual()
            .start {
                val total = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM person")
                var current = 0L

                jdbcTemplate
                    .queryForStream("SELECT * FROM person", ColumnMapRowMapper())
                    .asSequence()
                    .chunked(10000)
                    .forEach {
                        Thread.sleep(10)
                        current += it.size
                        val progress = (current * 100 / total)
                        redisTemplate.convertAndSend("export:$taskId", progress.toString())
                    }
            }

        return taskId
    }

    @GetMapping("/status")
    fun status(@RequestParam taskId: Long) = exportEventChannel.subscribe(taskId)

    @GetMapping("/tasks")
    fun tasks(): List<ExportTask> {
        TODO()
    }
}
