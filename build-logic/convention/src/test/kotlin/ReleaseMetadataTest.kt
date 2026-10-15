// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReleaseMetadataTest {
    private val valid =
        """
        # Audited fork release identity
        formatVersion=1
        versionName=3.3.13-t9.17
        versionCode=20261122
        previousVersionCode=20261121
        repository=X2M7/trime
        branch=develop
        applicationId=com.osfans.trime.debug
        certificateSha256=c6fb875b1959f56fc6b3992d603b86e2dcd793677d32aba83687b34ef6937dd2
        abis=arm64-v8a,x86_64
        """.trimIndent()

    private fun reject(changed: String) {
        assertFailsWith<IllegalArgumentException>(changed) { ReleaseMetadata.parse(changed) }
    }

    @Test
    fun `checked in release metadata parses with the production parser`() {
        val metadataFile =
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .map { File(it, "release.properties") }
                .first { it.isFile }
        val metadata = ReleaseMetadata.parse(metadataFile.readText())
        assertEquals("X2M7/trime", metadata.repository)
        assertEquals("com.osfans.trime.debug", metadata.applicationId)
        assertTrue(metadata.versionCode > metadata.previousVersionCode)
        assertTrue(metadata.previousVersionCode >= 20261121)
    }

    @Test
    fun `release identity is explicit and tag has only one prefix`() {
        val metadata = ReleaseMetadata.parse(valid)
        assertEquals("3.3.13-t9.17", metadata.versionName)
        assertEquals("v3.3.13-t9.17", metadata.tag)
        assertEquals(20261122, metadata.versionCode)
        assertEquals(20261121, metadata.previousVersionCode)
        assertEquals("com.osfans.trime.debug", metadata.applicationId)
        assertEquals("https://github.com/X2M7/trime", metadata.repositoryUrl)
        assertEquals("develop", metadata.branch)
        assertEquals(listOf("arm64-v8a", "x86_64"), metadata.abis)
    }

    @Test
    fun `windows newlines and comment indentation do not alter identity`() {
        assertEquals(ReleaseMetadata.parse(valid), ReleaseMetadata.parse("  # comment\r\n" + valid.replace("\n", "\r\n") + "\r\n"))
    }

    @Test
    fun `unknown missing and duplicate fields fail closed`() {
        reject("$valid\nextra=1")
        reject(valid.replace("formatVersion=1\n", ""))
        reject("$valid\nversionCode=20261122")
        reject(valid.replace("formatVersion=1", "formatVersion=2"))
    }

    @Test
    fun `escaped empty spaced and inline comment values are rejected`() {
        listOf("", " 3.3.13-t9.17", "3.3.13-t9.17 ", "3.3.13-t9.17 # release", "3.3.13-t9.\\u0031\\u0037").forEach {
            reject(valid.replace("versionName=3.3.13-t9.17", "versionName=$it"))
        }
    }

    @Test
    fun `versionCode must advance over published history within Android limit`() {
        listOf("0", "020261122", "20261121", "20261120", "-1", "2100000001", "2147483648", "999999999999999999999999").forEach {
            reject(valid.replace("versionCode=20261122", "versionCode=$it"))
        }
        reject(valid.replace("previousVersionCode=20261121", "previousVersionCode=20261120"))
        reject(valid.replace("previousVersionCode=20261121", "previousVersionCode=20261122"))
        assertEquals(2100000000, ReleaseMetadata.parse(valid.replace("versionCode=20261122", "versionCode=2100000000")).versionCode)
    }

    @Test
    fun `future audited history may advance independently of floor`() {
        val next = valid.replace("versionCode=20261122", "versionCode=20261124").replace("previousVersionCode=20261121", "previousVersionCode=20261123")
        assertEquals(20261123, ReleaseMetadata.parse(next).previousVersionCode)
    }

    @Test
    fun `repository branch and APK ABI channel cannot be silently replaced`() {
        reject(valid.replace("X2M7/trime", "osfans/trime"))
        reject(valid.replace("branch=develop", "branch=master"))
        listOf("arm64-v8a", "x86_64,arm64-v8a", "arm64-v8a,arm64-v8a", "arm64-v8a,x86_64,x86").forEach {
            reject(valid.replace("abis=arm64-v8a,x86_64", "abis=$it"))
        }
    }

    @Test
    fun `version package and certificate syntax is validated before BuildConfig`() {
        listOf("v3.3.13-t9.17", "3.3", "trime", "3.3.13/evil").forEach {
            reject(valid.replace("versionName=3.3.13-t9.17", "versionName=$it"))
        }
        reject(valid.replace("applicationId=com.osfans.trime.debug", "applicationId=com..trime"))
        reject(valid.replace("certificateSha256=c6fb875b", "certificateSha256=C6FB875B"))
        reject(valid.replace("certificateSha256=c6fb875b", "certificateSha256="))
    }

    @Test
    fun `matching legacy overrides are accepted but cannot hide a conflicting property`() {
        val version = ReleaseMetadata.parse(valid).versionName
        assertEquals(version, releaseIdentityValue(version, "BUILD_VERSION_NAME" to null, "buildVersionName" to null))
        assertEquals(version, releaseIdentityValue(version, "BUILD_VERSION_NAME" to version, "buildVersionName" to version))
        assertFailsWith<IllegalArgumentException> {
            releaseIdentityValue(version, "BUILD_VERSION_NAME" to version, "buildVersionName" to "$version-dirty")
        }
        assertFailsWith<IllegalArgumentException> { releaseIdentityValue(version, "BUILD_VERSION_NAME" to "") }
        assertFailsWith<IllegalArgumentException> {
            releaseIdentityValue("https://github.com/X2M7/trime", "buildGitRepo" to "https://github.com/osfans/trime")
        }
    }

    @Test
    fun `default ABIs match metadata while compatibility CI can explicitly build all four`() {
        val defaults = ReleaseMetadata.parse(valid).abis
        assertEquals(listOf("arm64-v8a", "x86_64"), resolveBuildAbis(null, defaults))
        assertEquals(
            listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"),
            resolveBuildAbis("armeabi-v7a,arm64-v8a,x86,x86_64", defaults),
        )
        listOf("", "arm64-v8a,arm64-v8a", "arm64-v8a, x86_64", "unknown", "x86,").forEach {
            assertFailsWith<IllegalArgumentException> { resolveBuildAbis(it, defaults) }
        }
    }
}
