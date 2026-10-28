package app.lumi.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogTest {

    @Test
    fun packsTilesInOrder() {
        // wide, wide, medium, medium, medium in a 4-wide column
        assertEquals(listOf(listOf(0), listOf(1), listOf(2, 3), listOf(4)), packRows(listOf(4, 4, 2, 2, 2), 4))
    }

    @Test
    fun portraitRowsFitThreeMediums() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4)), packRows(listOf(2, 2, 2, 4, 2), 6))
    }

    @Test
    fun tooWideTileStillGetsItsOwnRow() {
        assertEquals(listOf(listOf(0), listOf(1)), packRows(listOf(4, 2), 3))
    }

    @Test
    fun squareTileTucksBesideWideOne() {
        // clock (wide), keep listening (wide), photos, storage in portrait
        assertEquals(listOf(listOf(0, 2), listOf(1, 3)), packRows(listOf(4, 4, 2, 2), 6))
    }

    @Test
    fun everyGroupFillsItsColumnsWithNoHoles() {
        for (g in Catalog.groups) {
            val sizes = g.tiles.map { it.size.units }
            val land = packRows(sizes, 4)
            assertEquals("tiles in landscape", 0, land.size % 3)
            assertTrue("landscape rows full", land.all { r -> r.sumOf { sizes[it] } == 4 })
            val port = packRows(sizes, 6)
            assertTrue("portrait rows full", port.all { r -> r.sumOf { sizes[it] } == 6 })
        }
    }

    @Test
    fun helpIsTheVeryLastTile() {
        assertEquals("help", Catalog.groups.last().tiles.last().id)
    }

    @Test
    fun everyTileHasAUniqueId() {
        val ids = Catalog.groups.flatMap { g -> g.tiles.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun foldersAreAllOnTheStartScreen() {
        val onScreen = Catalog.groups.flatMap { it.tiles }.toSet()
        assertTrue(Catalog.folders.all { it in onScreen })
    }
}
