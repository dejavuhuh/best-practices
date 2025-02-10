package org.example

import org.babyfish.jimmer.sql.*
import java.time.Instant

@Entity
interface ExportTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val downloadFileName: String

    @Default("RUNNING")
    val state: ExportState

    val finishedAt: Instant?
}

enum class ExportState {
    RUNNING,
    COMPLETED,
}
