package domain

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TrackTest {

 @Test
 fun `Track with non-empty coordinates is valid`() {
  // Arrange
  val coords = listOf(Coordinate(10.0, 10.0))

  // Act
  val track = Track(coords)

  // Assert
  assertEquals(1, track.coordinates.size)
 }

 @Test
 fun `Track with empty coordinates throws an exception`() {
  // Arrange
  val coords = emptyList<Coordinate>()

  // Act & Assert
  val exception = assertThrows<IllegalArgumentException> {
   Track(coords)
  }
  assertTrue(exception.message?.contains("A track should shouldn't be empty!") == true)
 }
}
