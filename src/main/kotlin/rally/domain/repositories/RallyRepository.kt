package org.picolobruno.racing.rally.domain.repositories

import org.picolobruno.racing.rally.domain.objects.Rally

interface FindAllRallies {
    fun findAll(): Result<List<Rally>>
}

interface SaveRally {
    fun save(rally: Rally) : Result<Rally>
}
