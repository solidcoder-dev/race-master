package domain

import kotlin.test.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class CoordinateTest {

 @Test
 fun `Coordinate within valid ranges is created successfully`() {
  // Arrange
  val latitude = 45.0
  val longitude = 90.0

  // Act
  val coordinate = Coordinate(latitude, longitude)

  // Assert
  assertEquals(45.0, coordinate.latitude, 0.0001)
  assertEquals(90.0, coordinate.longitude, 0.0001)
 }

 @Test
 fun `Coordinate with latitude above 90 throws exception`() {
  val exception = assertThrows<IllegalArgumentException> {
   Coordinate(91.0, 0.0)
  }
  assertTrue(exception.message?.contains("Latitude must be between -90.0 and 90.0") == true)
 }

 @Test
 fun `Coordinate with latitude below -90 throws exception`() {
  val exception = assertThrows<IllegalArgumentException> {
   Coordinate(-91.0, 0.0)
  }
  assertTrue(exception.message?.contains("Latitude must be between -90.0 and 90.0") == true)
 }

 @Test
 fun `Coordinate with longitude above 180 throws exception`() {
  val exception = assertThrows<IllegalArgumentException> {
   Coordinate(0.0, 181.0)
  }
  assertTrue(exception.message?.contains("Longitude must be between -180.0 and 180.0") == true)
 }

 @Test
 fun `Coordinate with longitude below -180 throws exception`() {
  val exception = assertThrows<IllegalArgumentException> {
   Coordinate(0.0, -181.0)
  }
  assertTrue(exception.message?.contains("Longitude must be between -180.0 and 180.0") == true)
 }
}
