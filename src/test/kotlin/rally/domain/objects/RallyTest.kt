package rally.domain.objects

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.picolobruno.racing.rally.domain.objects.Rally
import java.time.LocalDate

class RallyTest {

    @Nested
    inner class NewRallyCreation {

        @Test
        fun `should create rally when start date is before end date`() {
            val name = "Desert Storm"
            val description = "Endurance race"
            val startDate = LocalDate.of(2025, 1, 10)
            val endDate = LocalDate.of(2025, 1, 15)

            val result = Rally.new(name, description, startDate, endDate)

            assertTrue(result.isSuccess)
            val rally = result.getOrNull()!!
            assertEquals(name, rally.name)
            assertEquals(description, rally.description)
            assertEquals(startDate, rally.startDate)
            assertEquals(endDate, rally.endDate)
            assertNotNull(rally.id)
        }

        @Test
        fun `should fail when start date is after end date`() {
            val result = Rally.new(
                name = "Test Rally",
                description = null,
                startDate = LocalDate.of(2025, 1, 20),
                endDate = LocalDate.of(2025, 1, 10)
            )

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Start date must be before end date", exception?.message)
        }

        @Test
        fun `should fail when start date equals end date`() {
            val date = LocalDate.of(2025, 1, 10)

            val result = Rally.new(
                name = "Same Day Rally",
                description = "Instant race",
                startDate = date,
                endDate = date
            )

            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is IllegalArgumentException)
            assertEquals("Start date must be before end date", exception?.message)
        }
    }
}
