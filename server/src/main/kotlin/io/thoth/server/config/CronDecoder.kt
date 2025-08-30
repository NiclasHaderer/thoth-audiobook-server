package io.thoth.server.config

import com.cronutils.model.Cron
import com.sksamuel.hoplite.ConfigFailure
import com.sksamuel.hoplite.ConfigResult
import com.sksamuel.hoplite.DecoderContext
import com.sksamuel.hoplite.Node
import com.sksamuel.hoplite.StringNode
import com.sksamuel.hoplite.ThrowableFailure
import com.sksamuel.hoplite.decoder.Decoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import io.thoth.server.common.extensions.toCron
import kotlin.reflect.KType

class CronDecoder : Decoder<Cron> {
    override fun decode(
        node: Node,
        type: KType,
        context: DecoderContext,
    ): ConfigResult<Cron> =
        when (node) {
            is StringNode -> {
                try {
                    node.value.toCron().valid()
                } catch (e: Exception) {
                    ThrowableFailure(e).invalid()
                }
            }

            else -> ConfigFailure.DecodeError(node, type).invalid()
        }

    override fun supports(type: KType): Boolean = type.classifier == Cron::class
}
