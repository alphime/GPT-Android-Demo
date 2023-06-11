package com.alphi.airobot.utils

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class EncryptionUtil {
    companion object {
        private val ENCRYPTION_ALGORITHM = "AES"
        private val SECRET_KEY_ALGORITHM = "AES"
        private val HASH_ALGORITHM = "SHA-256"

        fun encrypt(data: String, key: String): String {
            val secretKeySpec = generateSecretKeySpec(key)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            return Base64.getEncoder().encodeToString(encryptedBytes)
        }

        fun decrypt(data: String?, key: String): String {
            val secretKeySpec = generateSecretKeySpec(key)
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decodedBytes: ByteArray = Base64.getDecoder().decode(data)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, StandardCharsets.UTF_8)
        }

        private fun generateSecretKeySpec(key: String): SecretKeySpec {
            val sha = MessageDigest.getInstance(HASH_ALGORITHM)
            val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
            val hashedBytes = sha.digest(keyBytes)
            println(hashedBytes.size)
            return SecretKeySpec(hashedBytes, SECRET_KEY_ALGORITHM)
        }
    }
}