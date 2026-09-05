package io.thoth.server.common.exposed

import org.jetbrains.exposed.v1.core.Op

fun Op<Boolean>.unless(showInvisible: Boolean): Op<Boolean> = if (showInvisible) Op.TRUE else this
