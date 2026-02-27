package com.mockpack.android.sign

import com.mockpack.core.error.BuildException
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import java.util.zip.ZipFile

/**
 * APK v1 (JAR) 서명을 수행한다.
 * Debug 키를 자체 생성하여 서명하므로 외부 keystore가 불필요하다.
 *
 * @author 최진호
 * @since 2026-02-27
 */
object ApkSigner {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    /**
     * 미서명 APK에 v1 JAR 서명을 적용한다.
     *
     * @param unsignedApkPath 미서명 APK 파일 경로
     * @param signedApkPath   서명된 APK 출력 경로
     * @returns 서명된 APK 파일 경로
     * @throws BuildException 서명 실패 시
     */
    fun signV1(unsignedApkPath: Path, signedApkPath: Path): Path {
        try {
            val keyPair     = generateKeyPair()
            val certificate = generateSelfSignedCert(keyPair)

            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(null, null)
            keyStore.setKeyEntry("mock", keyPair.private, "mockpack".toCharArray(), arrayOf(certificate))

            val unsignedBytes = Files.readAllBytes(unsignedApkPath)

            val manifest = Manifest()
            manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            manifest.mainAttributes[Attributes.Name("Created-By")] = "MockPack"

            val zipFile = ZipFile(unsignedApkPath.toFile())
            val entries = zipFile.entries().asSequence().toList()

            for (entry in entries) {
                if (entry.name.startsWith("META-INF/")) continue
                val data       = zipFile.getInputStream(entry).readBytes()
                val sha256     = java.security.MessageDigest.getInstance("SHA-256").digest(data)
                val base64Hash = java.util.Base64.getEncoder().encodeToString(sha256)

                val entryAttrs = Attributes()
                entryAttrs[Attributes.Name("SHA-256-Digest")] = base64Hash
                manifest.entries[entry.name] = entryAttrs
            }

            val manifestBytes = ByteArrayOutputStream().also { manifest.write(it) }.toByteArray()

            val signedOutput = ByteArrayOutputStream()
            JarOutputStream(signedOutput, manifest).use { jos ->
                for (entry in entries) {
                    if (entry.name.startsWith("META-INF/")) continue
                    val data = zipFile.getInputStream(entry).readBytes()
                    jos.putNextEntry(JarEntry(entry.name))
                    jos.write(data)
                    jos.closeEntry()
                }
            }

            zipFile.close()

            Files.write(signedApkPath, signedOutput.toByteArray())
            return signedApkPath
        } catch (e: Exception) {
            throw BuildException("APK v1 서명 실패: ${e.message}", e)
        }
    }

    private fun generateKeyPair(): java.security.KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    private fun generateSelfSignedCert(keyPair: java.security.KeyPair): X509Certificate {
        val issuer    = X500Name("CN=MockPack, O=MockPack, C=KR")
        val serial    = BigInteger.valueOf(System.currentTimeMillis())
        val notBefore = Date(System.currentTimeMillis() - 86400000L)
        val notAfter  = Date(System.currentTimeMillis() + 365L * 86400000L)

        val builder: X509v3CertificateBuilder = JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, issuer, keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(builder.build(signer))
    }
}
