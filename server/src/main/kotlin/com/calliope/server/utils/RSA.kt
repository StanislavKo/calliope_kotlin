package com.calliope.server.utils

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.springframework.core.io.ClassPathResource
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.*
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RSA {
    @Throws(IOException::class)
    private fun getKey(filename: String): String {
        val classPathResource = ClassPathResource(filename)
        val fileContent = IOUtils.toString(classPathResource.inputStream)
        return fileContent
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKey(filename: String): RSAPrivateKey {
        val privateKeyPEM = getKey(filename)
        return getPrivateKeyFromString(privateKeyPEM)
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPrivateKeyFromString(key: String): RSAPrivateKey {
        var privateKeyPEM = key
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "")
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "")
        val encoded = Base64.decodeBase64(privateKeyPEM)
        val kf = KeyFactory.getInstance("RSA")
        val keySpec = PKCS8EncodedKeySpec(encoded)
        val privKey = kf.generatePrivate(keySpec) as RSAPrivateKey
        return privKey
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPublicKey(filename: String): RSAPublicKey {
        val publicKeyPEM = getKey(filename)
        return getPublicKeyFromString(publicKeyPEM)
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun getPublicKeyFromString(key: String): RSAPublicKey {
        var publicKeyPEM = key
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "")
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "")
        val encoded = Base64.decodeBase64(publicKeyPEM)
        val kf = KeyFactory.getInstance("RSA")
        val pubKey = kf.generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKey
        return pubKey
    }

    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        SignatureException::class,
        UnsupportedEncodingException::class
    )
    fun sign(privateKey: PrivateKey?, message: String): String {
        val sign = Signature.getInstance("SHA1withRSA")
        sign.initSign(privateKey)
        sign.update(message.toByteArray(charset("UTF-8")))
        return String(Base64.encodeBase64(sign.sign()), charset("UTF-8"))
    }


    @Throws(
        SignatureException::class,
        NoSuchAlgorithmException::class,
        UnsupportedEncodingException::class,
        InvalidKeyException::class
    )
    fun verify(publicKey: PublicKey?, message: String, signature: String): Boolean {
        val sign = Signature.getInstance("SHA1withRSA")
        sign.initVerify(publicKey)
        sign.update(message.toByteArray(charset("UTF-8")))
        return sign.verify(Base64.decodeBase64(signature.toByteArray(charset("UTF-8"))))
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun encrypt(rawText: String, publicKey: PublicKey?): String {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeBase64String(cipher.doFinal(rawText.toByteArray(charset("UTF-8"))))
    }

    @Throws(IOException::class, GeneralSecurityException::class)
    fun decrypt(cipherText: String?, privateKey: PrivateKey?): String {
        val cipher = Cipher.getInstance("RSA")
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return String(cipher.doFinal(Base64.decodeBase64(cipherText)), charset("UTF-8"))
    }
}