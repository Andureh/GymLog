package com.gymlog.app

import com.gymlog.app.data.GymState
import com.gymlog.app.data.GymStore
import com.gymlog.app.data.Gym
import com.gymlog.app.data.BarSpec
import com.gymlog.app.data.LoadSetup
import com.gymlog.app.data.PlateSpec
import com.gymlog.app.domain.smallestJump
import com.gymlog.app.domain.solvePlates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlatesTest {
    private val kg = listOf(25.0 to 4, 20.0 to 4, 15.0 to 2, 10.0 to 4, 5.0 to 4, 2.5 to 4, 1.25 to 2).map { PlateSpec(it.first, it.second) }

    @Test fun exactWithBar() {
        val r = solvePlates(100.0, 20.0, 2, kg)
        val e = r.exact!!
        assertEquals(100.0, e.total, 1e-9)
        assertEquals(40.0, e.perSide.sum(), 1e-9)
        assertEquals(2, e.perSide.size) // o menor número de discos
    }

    @Test fun machineWithoutBar() {
        val e = solvePlates(162.5, 0.0, 2, kg).exact!!
        assertEquals(81.25, e.perSide.sum(), 1e-9)
    }

    @Test fun closestWhenNotExact() {
        val r = solvePlates(61.0, 20.0, 2, kg)
        assertNull(r.exact)
        assertEquals(60.0, r.below!!.total, 1e-9)
        assertEquals(62.5, r.above!!.total, 1e-9)
    }

    @Test fun respectsAvailablePairs() {
        // Só 1 par de 20 e 1 par de 5: 70 kg com barra de 20 → 25 por lado = 20 + 5
        val few = listOf(PlateSpec(20.0, 1), PlateSpec(5.0, 1))
        assertEquals(listOf(20.0, 5.0), solvePlates(70.0, 20.0, 2, few).exact!!.perSide)
        // 120 kg não cabe: o máximo é 70
        val r = solvePlates(120.0, 20.0, 2, few)
        assertNull(r.exact)
        assertEquals(70.0, r.below!!.total, 1e-9)
        assertNull(r.above)
    }

    @Test fun oneSideUsesBothPlatesOfAPair() {
        val e = solvePlates(50.0, 0.0, 1, listOf(PlateSpec(20.0, 1), PlateSpec(10.0, 1))).exact!!
        assertEquals(listOf(20.0, 20.0, 10.0), e.perSide)
    }

    @Test fun lighterThanBar() {
        val r = solvePlates(15.0, 20.0, 2, kg)
        assertNull(r.exact)
        assertNull(r.below)
        assertNotNull(r.above)
    }

    @Test fun pounds() {
        val lb = listOf(45.0 to 6, 25.0 to 4, 10.0 to 4, 5.0 to 4, 2.5 to 2).map { PlateSpec(it.first, it.second) }
        val e = solvePlates(185.0, 45.0, 2, lb).exact!!
        assertEquals(listOf(45.0, 25.0), e.perSide)
    }

    @Test fun smallestJumpFromPlates() {
        assertEquals(2.5, smallestJump(kg, 2)!!, 1e-9)
        assertEquals(1.25, smallestJump(kg, 1)!!, 1e-9)
    }

    @Test fun gymJsonRoundTrip() {
        val g = Gym("g1", "Ginásio", false, kg, listOf(BarSpec("olympic", "Barra", 20.0)))
        val s = GymState(listOf(g), "g1", mapOf(5L to LoadSetup("olympic", 1)))
        assertEquals(s, GymStore.fromJson(GymStore.toJson(s)))
    }
}
