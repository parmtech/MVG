package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ModelGuardTest {
    @Test fun standardFourKRequiresEightSeconds() {
        val tooShort = VideoOptions(
            model = "veo-3.1-generate-001", duration = 6,
            resolution = "4k", ratio = "16:9"
        )
        assertFalse(ModelGuard.validate(tooShort).isEmpty())
        assertNull(ModelGuard.estimateUsd(tooShort))
        val valid = tooShort.copy(duration = 8)
        assertTrue(ModelGuard.validate(valid).isEmpty())
        assertEquals(BigDecimal("4.80"), ModelGuard.estimateUsd(valid))
    }

    @Test fun fastAndLiteCannotSelectFourK() {
        for (model in listOf("veo-3.1-fast-generate-001", "veo-3.1-lite-generate-001")) {
            val invalid = VideoOptions(model = model, duration = 8, resolution = "4k")
            assertFalse(ModelGuard.validate(invalid).isEmpty())
        }
    }

    @Test fun portraitAndEconomyQuote() {
        val opt = VideoOptions(
            model = "veo-3.1-lite-generate-001", duration = 8,
            resolution = "720p", audio = false, ratio = "9:16"
        )
        assertTrue(ModelGuard.validate(opt).isEmpty())
        assertEquals(BigDecimal("0.24"), ModelGuard.estimateUsd(opt))
    }

    @Test fun invalidModelHasNoQuote() {
        val opt = VideoOptions(model = "not-a-real-model", duration = 8)
        assertFalse(ModelGuard.validate(opt).isEmpty())
        assertNull(ModelGuard.estimateUsd(opt))
    }
}
