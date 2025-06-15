package org.picolobruno.racing.rally.domain.repositories

import org.picolobruno.racing.rally.domain.objects.Rally

interface RallyRepository {
    fun save(rally: Rally) : Rally
    fun findAll(): List<Rally>
}
