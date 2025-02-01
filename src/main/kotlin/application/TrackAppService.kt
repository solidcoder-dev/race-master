package org.picolobruno.racing.application

import org.picolobruno.racing.domain.repository.RaceRepository
import org.picolobruno.racing.domain.services.ScoringService
import org.springframework.web.multipart.MultipartFile

class TrackAppService(
    private val scoringService: ScoringService,
    private val raceRepository: RaceRepository
) {

    fun saveTrack(raceId: String, trackInKmlFile: MultipartFile) {
        val race = raceRepository.findRaceById(raceId)
        // val track = klmParser.parseTrack(trackInKmlFile)
        // val score = scoringService.calculateScore(race, track)
        // track.score = score
        // tracRepository.save(track)
        TODO("not yet implemented")
    }

}
