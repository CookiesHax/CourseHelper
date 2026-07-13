package com.cookieshax.coursehelper.core.utils

import android.annotation.SuppressLint
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.interfaces.RSAPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.UUID

// 密钥常量
object Constant {
    // schild 的盐
    const val SCHILD_SALT = $$"ipL$TkeiEmfy1gTXb2XHrdLN0a@7c^vu"

    // 网页登录
    const val WEB_LOGIN_KEY = "u2oh6Vu^HWe4_AES"

    // 发送验证码
    const val SEND_CAPTCHA_KEY = "jsDyctOCnay7uotq"

    // APP登录
    const val APP_LOGIN_KEY = "z4ok6lu^oWp4_AES"

    // 设备指纹
    const val DEVICE_CODE_KEY = "QrCbNY@MuK1X8HGw"

    // getDeviceInfo 公钥 / decryptDeviceInfo 私钥
    const val DEVICE_INFO_KEY =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC79d8Ot0hCbxxSISC6x8SCwTBspFSzlLKHJUYqoFNu1TSRaw4hEYkOnvEaL1VyoxV6HXcDrzwYvaFZaZaPQPFnfCHZy5dQwxcmifgSHqS+oKXw40Ys4cVIqnU5d90S7EWSRdBglX489jlqVaNcQSkDx2TYmC+DbAq9FV/BU09ISQIDAQAB"

    // inf_enc
    const val INF_ENC_TOKEN = "4faa8662c59590c6f43ae9fe5b002b42"
    const val INF_ENC_KEY = "Z(AfY@XS"

    // 验证码
    const val CAPTCHA_ID = "Qt9FIw9o4pwRjOyqM6yizZBh682qN2TU"

    // 获取用户档案照片
    const val FACE_ID = "uWwjeEKsri"
}

object EncryptionUtils {
    fun aesCbcEncrypt(text: String, key: String): String {
        try {
            val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
            val ivBytes = key.toByteArray(StandardCharsets.UTF_8)

            val secretKey = SecretKeySpec(keyBytes, "AES")
            val ivSpec = IvParameterSpec(ivBytes)

            // 获取 Cipher 实例
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

            // 初始化为加密模式
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            // 加密
            val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))

            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun aesEcbEncrypt(text: String, key: String): String {
        try {
            val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // 获取 Cipher 实例
            @SuppressLint("GetInstance")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

            // 初始化为加密模式
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            // 加密
            val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))

            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun parsePublicKey(publicKeyBase64: String): RSAPublicKey {
        val keyDer = Base64.decode(publicKeyBase64, Base64.NO_WRAP)

        val keySpec = X509EncodedKeySpec(keyDer)
        val keyFactory = KeyFactory.getInstance("RSA")

        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }

    fun rsaEncrypt(text: String, publicKeyBase64: String): String {
        val publicKey = parsePublicKey(publicKeyBase64)
        // 计算密钥字节长度并获取最大分块大小
        val keyLength = (publicKey.modulus.bitLength() + 7) / 8 // 1024 位 => 128 字节
        val maxChunkSize = keyLength - 11 // PKCS#1 填充最多 117 字节

        val plainBytes = text.toByteArray(Charsets.UTF_8)

        // 初始化 Cipher
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val outputStream = ByteArrayOutputStream()
        var i = 0

        // 进行分段加密
        while (i < plainBytes.size) {
            val end = if (i + maxChunkSize < plainBytes.size) i + maxChunkSize else plainBytes.size
            val chunk = plainBytes.copyOfRange(i, end)

            val encryptedChunk = cipher.doFinal(chunk)
            outputStream.write(encryptedChunk)

            i += maxChunkSize
        }

        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun rsaDecrypt(ciphertextBase64: String, publicKeyBase64: String): String {
        if (ciphertextBase64.isEmpty()) return ""
        try {
            val keyBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
            val keySpec = X509EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val publicKey = keyFactory.generatePublic(keySpec) as RSAPublicKey

            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, publicKey)

            val keyLength = (publicKey.modulus.bitLength() + 7) / 8

            val cipherBytes = Base64.decode(ciphertextBase64, Base64.DEFAULT)
            val bos = ByteArrayOutputStream()
            var offset = 0

            while (offset < cipherBytes.size) {
                val len =
                    if (offset + keyLength < cipherBytes.size) keyLength else cipherBytes.size - offset
                val decryptedChunk = cipher.doFinal(cipherBytes, offset, len)
                bos.write(decryptedChunk)
                offset += len
            }

            return bos.toString("UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun md5Hash(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun getPlainUuid(): String = getUuid().replace("-", "")
    fun getUuid(): String = UUID.randomUUID().toString()
    fun getDeviceCode(): String = aesEcbEncrypt(getUuid(), Constant.DEVICE_CODE_KEY)

    fun getEncParams(params: Map<String, String>): Map<String, String> {
        val encParams = mutableMapOf<String, String>()
        encParams["_c_0_"] = getPlainUuid()
        encParams["token"] = Constant.INF_ENC_TOKEN
        encParams["_time"] = System.currentTimeMillis().toString()
        encParams.putAll(params)

        val queryString = encParams.entries.joinToString("&") { "${it.key}=${it.value}" }

        encParams["inf_enc"] = md5Hash("$queryString&DESKey=${Constant.INF_ENC_KEY}")
        return encParams
    }

    fun getFileCRC32(file: File): String {
        val totalSize = file.length()
        val md5Digest = MessageDigest.getInstance("MD5")

        RandomAccessFile(file, "r").use { raf ->
            if (totalSize > 1048576) { // 1MB = 1024 * 1024
                val halfMeg = 524288 // 512KB

                // 读取前 512KB
                val firstBuffer = ByteArray(halfMeg)
                raf.readFully(firstBuffer)
                md5Digest.update(firstBuffer)

                // 跳到距离末尾 512KB 处读取
                val lastBuffer = ByteArray(halfMeg)
                raf.seek(totalSize - halfMeg)
                raf.readFully(lastBuffer)
                md5Digest.update(lastBuffer)
            } else {
                // 读取全部
                val buffer = ByteArray(totalSize.toInt())
                raf.readFully(buffer)
                md5Digest.update(buffer)
            }
        }

        // 拼接文件大小的十六进制字符串 (toRadixString(16))
        val sizeHex = totalSize.toString(16)
        md5Digest.update(sizeHex.toByteArray(Charsets.UTF_8))

        // 计算最终 MD5 并转为十六进制 String
        return md5Digest.digest().joinToString("") { "%02x".format(it) }
    }
}
