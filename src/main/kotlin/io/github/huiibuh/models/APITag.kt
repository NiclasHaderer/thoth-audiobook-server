package io.github.huiibuh.models

import com.papsign.ktor.openapigen.APITag


enum class ApiTags(override val description: String = "hello world") : APITag {
    Audiobook("All things concerning audiobooks")
}
