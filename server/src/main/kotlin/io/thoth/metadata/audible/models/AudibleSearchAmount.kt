package io.thoth.metadata.audible.models

import io.thoth.metadata.responses.MetadataSearchCount

enum class AudibleSearchAmount(
    val size: Int,
) {
    Twenty(20),
    Thirty(30),
    Forty(40),
    Fifty(50),
    ;

    companion object {
        fun from(searchCount: MetadataSearchCount): AudibleSearchAmount =
            when (searchCount) {
                MetadataSearchCount.Small -> Twenty
                MetadataSearchCount.Medium -> Thirty
                MetadataSearchCount.Large -> Forty
                MetadataSearchCount.ExtraLarge -> Fifty
            }
    }
}
