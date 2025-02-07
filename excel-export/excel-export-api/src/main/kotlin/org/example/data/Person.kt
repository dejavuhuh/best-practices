package org.example.data

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.GenerationType
import org.babyfish.jimmer.sql.Id
import java.time.Instant

@Entity
interface Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long

    val name: String

    val age: Int

    val email: String

    val phone: String

    val address: String

    val city: String

    val state: String

    val zip: String

    val country: String

    val createdAt: Instant

    val updatedAt: Instant?
}
