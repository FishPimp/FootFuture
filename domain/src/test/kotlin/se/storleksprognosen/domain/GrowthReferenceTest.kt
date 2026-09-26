package se.storleksprognosen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import se.storleksprognosen.domain.growth.AgeMath
import se.storleksprognosen.domain.growth.FootLengthReference
import se.storleksprognosen.domain.growth.NormalDistribution
import se.storleksprognosen.domain.growth.Percentile
import se.storleksprognosen.domain.growth.WhoHeightReference
import se.storleksprognosen.domain.model.Gender
import java.time.LocalDate
import kotlin.math.sqrt

class GrowthReferenceTest {

    @Test
    fun `WHO boys 12 months matches published SD values`() {
        // WHO lhfa boys, 12 mån: -2 SD = 71,0 cm, median = 75,7 cm, +2 SD = 80,5 cm.
        assertEquals(75.7488, WhoHeightReference.lms(Gender.BOY, 12.0).m, 1e-9)
        assertEquals(80.5, WhoHeightReference.valueAt(Gender.BOY, 12.0, 2.0), 0.05)
        assertEquals(71.0, WhoHeightReference.valueAt(Gender.BOY, 12.0, -2.0), 0.05)
        assertEquals(-2.0, WhoHeightReference.zScore(Gender.BOY, 12.0, 71.0), 0.03)
    }

    @Test
    fun `WHO girls 36 and 48 months matches published SD values`() {
        assertEquals(87.4, WhoHeightReference.valueAt(Gender.GIRL, 36.0, -2.0), 0.05)
        assertEquals(98.9, WhoHeightReference.valueAt(Gender.GIRL, 36.0, 1.0), 0.05)
        assertEquals(111.3, WhoHeightReference.valueAt(Gender.GIRL, 48.0, 2.0), 0.05)
    }

    @Test
    fun `interpolates between whole months and clamps outside 0-72`() {
        val mid = WhoHeightReference.lms(Gender.BOY, 12.5).m
        assertEquals((75.7488 + 76.9186) / 2, mid, 1e-9)
        assertEquals(WhoHeightReference.lms(Gender.GIRL, 0.0), WhoHeightReference.lms(Gender.GIRL, -3.0))
        assertEquals(WhoHeightReference.lms(Gender.GIRL, 72.0), WhoHeightReference.lms(Gender.GIRL, 80.0))
    }

    @Test
    fun `z score and value are inverse`() {
        for (age in listOf(0.0, 7.3, 24.0, 50.5, 72.0)) {
            for (gender in Gender.entries) {
                val value = WhoHeightReference.valueAt(gender, age, 1.23)
                assertEquals(1.23, WhoHeightReference.zScore(gender, age, value), 1e-9)
                val foot = FootLengthReference.valueAt(gender, age, -0.7)
                assertEquals(-0.7, FootLengthReference.zScore(gender, age, foot), 1e-9)
            }
        }
    }

    @Test
    fun `percentile curves use the right z scores`() {
        assertEquals(97.0, NormalDistribution.percentile(Percentile.P97.z), 0.01)
        assertEquals(85.0, NormalDistribution.percentile(Percentile.P85.z), 0.01)
        assertEquals(50.0, NormalDistribution.percentile(Percentile.P50.z), 1e-6)
        assertEquals(15.0, NormalDistribution.percentile(Percentile.P15.z), 0.01)
        assertEquals(3.0, NormalDistribution.percentile(Percentile.P3.z), 0.01)
    }

    @Test
    fun `foot reference is plausible for young children`() {
        // Nyfödd ≈ 7,8 cm, 3 år ≈ 15 cm, 6 år ≈ 18 cm.
        assertEquals(78.0, FootLengthReference.lms(Gender.GIRL, 0.0).m, 2.0)
        assertEquals(150.0, FootLengthReference.lms(Gender.GIRL, 36.0).m, 3.0)
        assertEquals(183.0, FootLengthReference.lms(Gender.BOY, 72.0).m, 3.0)

        val heightCv = WhoHeightReference.lms(Gender.GIRL, 36.0).s
        val footCv = FootLengthReference.lms(Gender.GIRL, 36.0).s
        assertEquals(sqrt(heightCv * heightCv + FootLengthReference.RATIO_CV * FootLengthReference.RATIO_CV), footCv, 1e-12)
        assertTrue(footCv in 0.05..0.065)
    }

    @Test
    fun `percentile curves are increasing with age`() {
        for (gender in Gender.entries) {
            for (percentile in Percentile.entries) {
                val curve = WhoHeightReference.curve(gender, percentile.z)
                assertEquals(145, curve.size)
                curve.zipWithNext().forEach { (a, b) -> assertTrue(b.second > a.second) }
            }
        }
    }

    @Test
    fun `age math`() {
        val birth = LocalDate.of(2024, 6, 15)
        assertEquals(2 to 3, AgeMath.yearsAndMonths(birth, LocalDate.of(2026, 9, 26)))
        assertEquals(0 to 0, AgeMath.yearsAndMonths(birth, birth))
        assertEquals(12.0, AgeMath.ageInMonths(birth, birth.plusDays(365)) , 0.03)
        assertEquals(birth.plusDays(1096), AgeMath.dateAtAge(birth, 36.0))
    }
}
