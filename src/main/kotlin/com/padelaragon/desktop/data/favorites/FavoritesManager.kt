package com.padelaragon.desktop.data.favorites

import com.padelaragon.desktop.data.model.League
import com.padelaragon.desktop.data.repository.datasource.FavoritesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.prefs.Preferences

/**
 * Desktop replacement for the Android SharedPreferences-based FavoritesManager.
 * Persists favorite group ids using java.util.prefs (stored per-OS-user, no Context needed).
 * Favorites are scoped per [League] (composite key "leagueId:groupId"), capped at [MAX_FAVORITES]
 * per league, so up to 3 favorites can be kept in each of Absoluta/Veteranos/Menores independently.
 */
class FavoritesManager(private val league: League) : FavoritesDataSource {
    private val prefs: Preferences = Preferences.userRoot().node("com/padelaragon/desktop/favorites")

    private val _favorites = MutableStateFlow(readFavoritesFromPrefs())
    override val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    override fun toggleFavorite(groupId: Int): Boolean {
        val currentFavorites = _favorites.value

        if (currentFavorites.contains(groupId)) {
            val updated = currentFavorites.toMutableSet().apply { remove(groupId) }
            persistFavorites(updated)
            _favorites.value = updated
            return false
        }

        if (currentFavorites.size >= MAX_FAVORITES) {
            return false
        }

        val updated = currentFavorites.toMutableSet().apply { add(groupId) }
        persistFavorites(updated)
        _favorites.value = updated
        return true
    }

    override fun isFavorite(groupId: Int): Boolean {
        return _favorites.value.contains(groupId)
    }

    private fun prefKey(): String = "favorite_group_ids_${league.id}"

    private fun readFavoritesFromPrefs(): Set<Int> {
        val stored = prefs.get(prefKey(), "")
        if (stored.isBlank()) return emptySet()
        return stored.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    private fun persistFavorites(favorites: Set<Int>) {
        prefs.put(prefKey(), favorites.joinToString(","))
    }

    companion object {
        private const val MAX_FAVORITES = 3
    }
}

