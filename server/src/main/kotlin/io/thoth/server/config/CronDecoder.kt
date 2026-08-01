package io.thoth.server.config

import com.cronutils.model.Cron
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import io.thoth.server.common.extensions.toCron
import kotlin.reflect.KType

class CronDecoder : NullHandlingDecoder<Cron> {
    override fun supports(type: KType): Boolean = type.classifier == Cron::class

    override fun safeDecode(
        node: Node,
        type: KType,
        context: DecoderContext,
    ): ConfigResult<Cron> =
        if (node is StringNode) {
            runCatching { node.value.toCron().valid() }.getOrElse { ConfigFailure.DecodeError(node, type).invalid() }
        } else {
            ConfigFailure.DecodeError(node, type).invalid()
        }
}
