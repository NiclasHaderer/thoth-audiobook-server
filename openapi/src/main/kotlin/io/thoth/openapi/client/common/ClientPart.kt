package io.thoth.openapi.client.common

import java.nio.file.Path

data class ClientPart(val path: Path, val content: String) {
    constructor(path: String, content: String) : this(Path.of(path), content)
}
