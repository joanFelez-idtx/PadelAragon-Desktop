package com.padelaragon.desktop.domain.usecase

import com.padelaragon.desktop.data.model.MatchResult

class FindDefaultJornadaUseCase {
    operator fun invoke(
        sortedJornadas: List<Int>,
        allResults: Map<Int, List<MatchResult>>
    ): Int? {
        val lastWithResults = sortedJornadas.lastOrNull { jornada ->
            allResults[jornada]?.any { it.localScore != "--" && it.visitorScore != "--" } == true
        }
        return lastWithResults ?: sortedJornadas.firstOrNull()
    }
}
