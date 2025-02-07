//package org.example
//
//import org.babyfish.jimmer.sql.*
//import java.time.Instant
//
//@Entity
//interface ExportTask {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    val id: Long
//
//    val s3ObjectKey: String?
//
//    val downloadFileName: String
//
//    @Default("PENDING")
//    val state: ExportState
//
//    val startedAt: Instant?
//
//    val finishedAt: Instant?
//}
//
//enum class ExportState {
//    PENDING,
//    RUNNING,
//    COMPLETED,
//    FAILED
//}
