package domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ToleranceTest {

 @Test
 fun `Tolerance with non-negative distance is valid`() {
  // Arrange
  val distance = 0.0

  // Act
  val tolerance = Tolerance(distance)

  // Assert
  assertEquals(0.0, tolerance.distanceInKm, 0.0001)
 }

 @Test
 fun `Tolerance with positive distance is valid`() {
  val tolerance = Tolerance(5.0)
  assertEquals(5.0, tolerance.distanceInKm, 0.0001)
 }

 @Test
 fun `Tolerance with negative distance throws an exception`() {
  // Arrange
  val negativeDistance = -1.0

  // Act & Assert
  val exception = assertThrows<IllegalArgumentException> {
   Tolerance(negativeDistance)
  }
  assertTrue(exception.message?.contains("Tolerance cannot be negative") == true)
 }
}
