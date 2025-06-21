package rally.infrastructure.repositories

import io.mockk.every
import io.mockk.mockk
import org.instancio.Instancio
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.picolobruno.racing.rally.domain.objects.Rally
import org.picolobruno.racing.rally.infrastructure.entities.RallyEntity
import org.picolobruno.racing.rally.infrastructure.entities.toDomain
import org.picolobruno.racing.rally.infrastructure.entities.toEntity
import org.picolobruno.racing.rally.infrastructure.repositories.RallyRepositoryV1
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

class RallyRepositoryV1Test {

    private val jpa = mockk<JpaRepository<RallyEntity, UUID>>()
    private val repository = RallyRepositoryV1(jpa)

    @Nested
    inner class SaveRallyTest {

        @Test
        fun `should return Rally when save succeeds`() {
            val rally = Instancio.create(Rally::class.java)
            val entity = rally.toEntity()
            val savedEntity = entity.copy()

            every { jpa.save(entity) } returns savedEntity

            val result = repository.save(rally)

            assertTrue(result.isSuccess)
            assertEquals(savedEntity.toDomain(), result.getOrNull())
        }

        @Test
        fun `should return failure when save throws`() {
            val rally = Instancio.create(Rally::class.java)
            val exception = RuntimeException("DB write failed")

            every { jpa.save(any()) } throws exception

            val result = repository.save(rally)

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
    }

    @Nested
    inner class FindAllRalliesTest {

        @Test
        fun `should return list of rallies when findAll succeeds`() {
            val entities = Instancio.ofList(RallyEntity::class.java).size(3).create()
            every { jpa.findAll() } returns entities

            val result = repository.findAll()

            assertTrue(result.isSuccess)
            assertEquals(entities.map { it.toDomain() }, result.getOrNull())
        }

        @Test
        fun `should return failure when findAll throws`() {
            val exception = RuntimeException("DB read failed")
            every { jpa.findAll() } throws exception

            val result = repository.findAll()

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }
    }
}
