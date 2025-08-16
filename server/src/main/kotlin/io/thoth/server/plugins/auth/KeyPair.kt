package io.thoth.server.plugins.auth

import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import kotlin.io.path.reader
import kotlin.io.path.writer

private fun createKeyPair(jwtKeyFile: Path): KeyPair {
    val kpg = KeyPairGenerator.getInstance("RSA")
    kpg.initialize(2048, SecureRandom())
    val kp = kpg.generateKeyPair()

    JcaPEMWriter(jwtKeyFile.writer()).use { pemWriter ->
        pemWriter.writeObject(kp.private)
        pemWriter.writeObject(kp.public)
    }
    return kp
}

fun getOrCreateKeyPair(path: Path): KeyPair =
    try {
        path.reader().use { reader ->
            val parsed: PEMKeyPair = PEMParser(reader).readObject() as PEMKeyPair
            JcaPEMKeyConverter().getKeyPair(parsed)
        }
    } catch (e: Exception) {
        createKeyPair(path)
    }
