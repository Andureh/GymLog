package com.gymlog.app

import com.gymlog.app.data.Exercise
import com.gymlog.app.i18n.Catalog
import com.gymlog.app.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class I18nTest {
    @Test fun catalogHasEveryLanguage() {
        Catalog.exercises.forEach { assertEquals(it.key, Catalog.LANGS, it.names.size) }
        Catalog.muscles.forEach { (k, v) -> assertEquals(k, Catalog.LANGS, v.size) }
        Catalog.equipment.forEach { (k, v) -> assertEquals(k, Catalog.LANGS, v.size) }
        assertTrue(Catalog.exercises.all { e -> e.names.all { it.isNotBlank() } })
        assertTrue(Catalog.exercises.all { it.muscle in Catalog.muscles && it.equipment in Catalog.equipment })
    }

    @Test fun languagesAreConsistent() {
        assertEquals(Catalog.LANGS, I18n.all.size)
        assertEquals(I18n.all.indices.toList(), I18n.all.map { it.col })
        assertEquals(I18n.all.size, I18n.all.map { it.code }.toSet().size)
    }

    @Test fun legacyNamesAreUnique() {
        assertEquals(Catalog.exercises.size, Catalog.legacyExerciseKeys.size)
    }

    @Test fun translatesBuiltInButKeepsCustomNames() {
        val bench = Exercise(name = "x", muscle = "chest", equipment = "barbell", nameKey = "bench_bb")
        val custom = Exercise(name = "O meu exercício", muscle = "chest", equipment = "barbell", isCustom = true)
        val (pt, en, es) = I18n.all
        assertEquals("Supino plano (barra)", pt.exName(bench))
        assertEquals("Bench Press (Barbell)", en.exName(bench))
        assertEquals("Press de banca (barra)", es.exName(bench))
        assertEquals("O meu exercício", en.exName(custom))
        assertEquals("Chest", en.muscle("chest"))
        assertEquals("Pecho", es.muscle("chest"))
    }
}
