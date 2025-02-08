package org.example

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.beans.factory.DisposableBean
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlin.streams.asSequence

@RestController
@RequestMapping("/export")
class ExportController(
    val sqlClient: KSqlClient,
    val jdbcTemplate: JdbcTemplate
) : DisposableBean {

    val subscribers = mutableMapOf<Long, SseEmitter>()

    data class AsyncExportRequest(
        val downloadFileName: String
    )

    @PostMapping("/start")
    fun start(@RequestBody request: AsyncExportRequest): Long {
        val insertResult = sqlClient.insert(ExportTask {
            downloadFileName = request.downloadFileName
        })
        val taskId = insertResult.modifiedEntity.id
        val emitter = SseEmitter()
        subscribers[taskId] = emitter

        Thread
            .ofVirtual()
            .uncaughtExceptionHandler { _, e ->
                e.printStackTrace()
                emitter.completeWithError(e)
            }
            .start {
                val total = jdbcTemplate.queryForObject<Long>("SELECT count(*) FROM person")
                var current = 0L

                jdbcTemplate
                    .queryForStream("SELECT * FROM person", ColumnMapRowMapper())
                    .asSequence()
                    .chunked(2000)
                    .forEach {
                        Thread.sleep(20)
                        current += it.size
                        val progress = (current * 100 / total)
                        emitter.send(SseEmitter.event().data(progress))
                    }

                emitter.send(SseEmitter.event().data(100))
                emitter.complete()
            }

        return taskId
    }

    @GetMapping("/status")
    fun status(@RequestParam taskId: Long): SseEmitter {
        return subscribers[taskId] ?: throw IllegalArgumentException("Invalid task id")
    }

    override fun destroy() {
        subscribers.values.forEach(SseEmitter::complete)
    }
}
