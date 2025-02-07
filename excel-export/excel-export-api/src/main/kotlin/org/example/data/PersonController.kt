package org.example.data

import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@RestController
class PersonController(val sqlClient: KSqlClient) {

    @PostMapping("/mock-huge-data")
    fun mockHugeData(@RequestBody options: Options) {
        val names = listOf("Alice", "Bob", "Charlie", "David", "Eve", "Frank", "Grace", "Helen", "Ivy", "Jack")
        val ages = (1..100).toList()
        val emails = listOf("example@gmail.com", "example@qq.com", "example@outlook.com", "example@163.com")
        val phones = listOf("1234567890", "2345678901", "3456789012", "4567890123", "5678901234", "6789012345", "7890123456", "8901234567", "9012345678", "0123456789")
        val addresses = listOf("123 Main St", "456 Elm St", "789 Oak St", "012 Pine St", "345 Cedar St", "678 Walnut St", "901 Maple St", "234 Birch St", "567 Spruce St", "890 Ash St")
        val cities = listOf("New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose")
        val states = listOf("NY", "CA", "IL", "TX", "AZ", "PA", "TX", "CA", "TX", "CA")
        val zips = listOf("10001", "90001", "60601", "77001", "85001", "19101", "78201", "92101", "75201", "95101")
        val countries = listOf("US", "JP", "CN", "IN", "BR", "RU", "ID", "PK", "NG", "BD")
        val createdAt = listOf("2021-01-01T00:00:00Z", "2021-02-01T00:00:00Z", "2021-03-01T00:00:00Z", "2021-04-01T00:00:00Z", "2021-05-01T00:00:00Z", "2021-06-01T00:00:00Z", "2021-07-01T00:00:00Z", "2021-08-01T00:00:00Z", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z")
        val updatedAt = listOf("2021-01-01T00:00:00Z", "2021-02-01T00:00:00Z", "2021-03-01T00:00:00Z", "2021-04-01T00:00:00Z", "2021-05-01T00:00:00Z", "2021-06-01T00:00:00Z", "2021-07-01T00:00:00Z", "2021-08-01T00:00:00Z", "2021-09-01T00:00:00Z", "2021-10-01T00:00:00Z")

        val completed = AtomicInteger()
        val chunkSize = 2000
        (0 until options.count)
            .asSequence()
            .chunked(chunkSize)
            .map {
                it.map { index ->
                    Person {
                        this.name = names[index % names.size]
                        this.age = ages[index % ages.size]
                        this.email = emails[index % emails.size]
                        this.phone = phones[index % phones.size]
                        this.address = addresses[index % addresses.size]
                        this.city = cities[index % cities.size]
                        this.state = states[index % states.size]
                        this.zip = zips[index % zips.size]
                        this.country = countries[index % countries.size]
                        this.createdAt = Instant.parse(createdAt[index % createdAt.size])
                        this.updatedAt = Instant.parse(updatedAt[index % updatedAt.size])
                    }
                }
            }
            .forEach {
                Thread.ofVirtual().start {
                    sqlClient.insertEntities(it)
                    completed.addAndGet(it.size)
                    println("Progress: ${completed.get()} / ${options.count}")
                }
            }
    }

    data class Options(val count: Int)
}
