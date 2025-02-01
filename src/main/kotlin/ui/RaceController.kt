package org.picolobruno.racing.ui

import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController("races")
class RaceController {
    @PutMapping
    fun addRace(
        raceDto: Any,
        raceInKmlFile: MultipartFile
    ) {
       TODO("Not yet implemented")
    }

    @PutMapping("{raceId}/tracks")
    fun addTrack(
        @PathVariable raceId: String,
        trackInKmlFile: MultipartFile
    ) {
        TODO("Not yet implemented")
    }
}