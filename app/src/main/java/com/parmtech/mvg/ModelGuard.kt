package com.parmtech.mvg

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Conservative versioned model guard and public-list-price USD quote.
 * Snapshot 2026-11-05. Billing, custom contracts, tax and regional restrictions vary.
 * Sources: https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing
 * https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate
 */
object ModelGuard {
    const val RATE_DATE = "2026-11-05"
    const val RATE_SOURCE = "https://cloud.google.com/gemini-enterprise-agent-platform/generative-ai/pricing"
    const val CAPABILITY_SOURCE =
        "https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/veo/3-1-generate"

    fun resolutions(model: String): List<String> = when (model) {
        "veo-3.1-generate-001" -> listOf("720p", "1080p", "4k")
        "veo-3.1-fast-generate-001", "veo-3.1-lite-generate-001" ->
            listOf("720p", "1080p")
        else -> emptyList()
    }

    fun durations(resolution: String): List<Int> =
        if (resolution == "720p") listOf(4, 6, 8) else listOf(8)

    fun validate(options: VideoOptions): List<String> {
        val errors = mutableListOf<String>()
        if (options.model !in VertexClient.VIDEO_MODELS) errors += "Video model not recognized"
        if (options.duration !in listOf(4, 6, 8)) errors += "Duration must be 4, 6 or 8 seconds"
        if (options.ratio !in listOf("16:9", "9:16")) errors += "Ratio must be 16:9 or 9:16"
        if (options.resolution !in resolutions(options.model)) errors +=
            "Resolution is unsupported by this model"
        if (options.resolution != "720p" && options.duration != 8) errors +=
            "1080p/4K output requires 8 seconds"
        if (options.firstFramePath.isNotBlank() && options.duration !in listOf(4, 6, 8)) errors +=
            "First-frame input requires supported duration"
        return errors
    }

    /** Official public price table (USD per generated video second, single output). */
    fun perSecondUsd(model: String, resolution: String, audio: Boolean): BigDecimal? {
        val amount = when(model) {
            "veo-3.1-generate-001" -> when (resolution) {
                "720p", "1080p" -> if (audio) "0.40" else "0.20"
                "4k" -> if (audio) "0.60" else "0.40"
                else -> null
            }
            "veo-3.1-fast-generate-001" -> when (resolution) {
                "720p" -> if (audio) "0.10" else "0.08"
                "1080p" -> if (audio) "0.12" else "0.10"
                else -> null
            }
            "veo-3.1-lite-generate-001" -> when (resolution) {
                "720p" -> if (audio) "0.05" else "0.03"
                "1080p" -> if (audio) "0.08" else "0.05"
                else -> null
            }
            else -> null
        }
        return amount?.let(::BigDecimal)
    }

    fun estimateUsd(options: VideoOptions): BigDecimal? {
        if (validate(options).isNotEmpty()) return null
        return perSecondUsd(options.model, options.resolution, options.audio)
            ?.multiply(BigDecimal.valueOf(options.duration.toLong()))
            ?.setScale(2, RoundingMode.HALF_UP)
    }

    fun quoteText(options: VideoOptions): String =
        estimateUsd(options)?.let { "$" + it.toPlainString() + " USD estimated" }
            ?: "Estimate unavailable"
}
