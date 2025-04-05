package com.calliope.server.utils

import com.calliope.server.model.domain.yandex.YandexKey
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.*

object JWTYandexGenerator {
    @JvmStatic
    @Throws(Exception::class)
    fun getYandexJWT(keyInfo: YandexKey): String {
        val privateKeyString: String = keyInfo.privateKey!!
        val serviceAccountId: String = keyInfo.serviceAccountId!!
        val keyId: String = keyInfo.id!!

        val privateKeyPem: PemObject
        PemReader(StringReader(privateKeyString)).use { reader ->
            privateKeyPem = reader.readPemObject()
        }
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyPem.content))

        val now = Instant.now()

        // Формирование JWT.
        val encodedToken = Jwts.builder()
            .setHeaderParam("kid", keyId)
            .setIssuer(serviceAccountId)
            .setAudience("https://iam.api.cloud.yandex.net/iam/v1/tokens")
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(3600)))
            .signWith(privateKey, SignatureAlgorithm.PS256)
            .compact()
        println(encodedToken)

        return encodedToken
    }
}