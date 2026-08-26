package com.padelaragon.desktop.data.favorites

import com.padelaragon.desktop.data.model.League
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesManagerTest {

    @After
    fun cleanup() {
        // Reset state for both leagues so this test is repeatable and doesn't leak into other tests.
        League.entries.forEach { league ->
            val manager = FavoritesManager(league)
            manager.favorites.value.toList().forEach { manager.toggleFavorite(it) }
        }
    }

    @Test
    fun `favorites are isolated per league`() {
        val absoluta = FavoritesManager(League.ABSOLUTA)
        val veteranos = FavoritesManager(League.VETERANOS)

        absoluta.toggleFavorite(100)
        veteranos.toggleFavorite(200)

        assertTrue(absoluta.isFavorite(100))
        assertFalse(absoluta.isFavorite(200))
        assertTrue(veteranos.isFavorite(200))
        assertFalse(veteranos.isFavorite(100))
    }

    @Test
    fun `each league is capped at 3 favorites independently`() {
        val absoluta = FavoritesManager(League.ABSOLUTA)
        val menores = FavoritesManager(League.MENORES)

        listOf(1, 2, 3).forEach { absoluta.toggleFavorite(it) }
        val rejected = absoluta.toggleFavorite(4)

        assertFalse(rejected)
        assertEquals(3, absoluta.favorites.value.size)

        // A different league's cap is independent of Absoluta's.
        listOf(11, 12, 13).forEach { menores.toggleFavorite(it) }
        assertEquals(3, menores.favorites.value.size)
    }
}
