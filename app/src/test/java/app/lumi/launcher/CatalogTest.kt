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
    fun savedOrderIsUsedAndNewTilesAreKept() {
        val d = Catalog.startTiles
        val saved = listOf(d[1].id, d[0].id, "gone-tile").joinToString(",")
        val out = applyOrder(d, saved)
        assertEquals(d[1].id, out[0].id)
        assertEquals(d[0].id, out[1].id)
        // Nothing lost, nothing doubled, unknown ids ignored.
        assertEquals(d.map { it.id }.toSet(), out.map { it.id }.toSet())
        assertEquals(d.size, out.size)
    }

    @Test
    fun onlySameSizeTilesSwap() {
        val d = Catalog.startTiles
        val wide = d.first { it.size == TileSize.WIDE }.id
        val medium = d.first { it.size == TileSize.MEDIUM }.id
        assertEquals(d, swapTiles(d, wide, medium))
        val other = d.last { it.size == TileSize.MEDIUM }.id
        val swapped = swapTiles(d, medium, other)
        assertEquals(other, swapped[d.indexOfFirst { it.id == medium }].id)
    }

    @Test
    fun anyArrangementStaysFreeOfHoles() {
        // Swaps keep the sequence of tile sizes, so shuffling mediums never makes a gap.
        val d = Catalog.startTiles
        val mediums = d.filter { it.size == TileSize.MEDIUM }.map { it.id }
        var t = d
        for (k in 0 until 40) t = swapTiles(t, mediums[k % mediums.size], mediums[(k * 7 + 3) % mediums.size])
        val sizes = t.map { it.size.units }
        assertTrue(packRows(sizes, 4).all { r -> r.sumOf { sizes[it] } == 4 })
        assertTrue(packRows(sizes, 6).all { r -> r.sumOf { sizes[it] } == 6 })
    }

    @Test
    fun helpNeedsAHoldAndSitsNearTheFront() {
        val i = Catalog.startTiles.indexOfFirst { it.id == "help" }
        assertTrue(i in 0..5)
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
