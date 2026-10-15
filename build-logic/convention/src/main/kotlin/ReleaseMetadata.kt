// SPDX-FileCopyrightText: 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

/** The same strict, unescaped format consumed by script/release/release.py. */
data class ReleaseMetadata(
    val versionName: String,
    val versionCode: Int,
    val previousVersionCode: Int,
    val repository: String,
    val branch: String,
    val applicationId: String,
    val certificateSha256: String,
    val abis: List<String>,
) {
    val tag: String
        get() = "v$versionName"

    val repositoryUrl: String
        get() = "https://github.com/$repository"

    companion object {
        // All 26 public APKs audited on 2026-10-15; later history may raise this floor.
        private const val FIRST_AUDITED_PUBLISHED_MAXIMUM = 20261121

        private val fields =
            setOf(
                "formatVersion",
                "versionName",
                "versionCode",
                "previousVersionCode",
                "repository",
                "branch",
                "applicationId",
                "certificateSha256",
                "abis",
            )

        fun parse(text: String): ReleaseMetadata {
            val values = mutableMapOf<String, String>()
            text.lineSequence().forEach { line ->
                if (line.isBlank() || line.trimStart().startsWith("#")) return@forEach
                require(Regex("[A-Za-z][A-Za-z0-9]*=[A-Za-z0-9.,/_-]+").matches(line)) {
                    "Use plain key=value metadata"
                }
                val (key, value) = line.split('=', limit = 2)
                require(values.put(key, value) == null) { "Duplicate release property: $key" }
            }
            require(values.keys == fields && values["formatVersion"] == "1") {
                "Unknown release metadata format"
            }
            fun value(key: String) = values.getValue(key)

            require(value("repository") == "X2M7/trime" && value("branch") == "develop") {
                "Only X2M7/trime:develop may publish"
            }
            require(Regex("[0-9]+(?:\\.[0-9]+){2}(?:-[A-Za-z0-9.-]+)?").matches(value("versionName"))) {
                "Invalid versionName"
            }
            require(Regex("[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+").matches(value("applicationId"))) {
                "Invalid package name"
            }
            require(Regex("[0-9a-f]{64}").matches(value("certificateSha256"))) {
                "Invalid signing certificate"
            }

            fun code(key: String): Int {
                val raw = value(key)
                require(Regex("[1-9][0-9]*").matches(raw)) { "Invalid versionCode" }
                return requireNotNull(raw.toIntOrNull()) { "Invalid versionCode" }
            }

            val versionCode = code("versionCode")
            val previousVersionCode = code("previousVersionCode")
            require(
                previousVersionCode >= FIRST_AUDITED_PUBLISHED_MAXIMUM &&
                    previousVersionCode < versionCode && versionCode <= 2100000000,
            ) {
                "versionCode must exceed the published maximum"
            }
            val abis = value("abis").split(',')
            require(abis == listOf("arm64-v8a", "x86_64")) {
                "This release channel ships ARM64 and x86_64 separately"
            }
            return ReleaseMetadata(
                value("versionName"),
                versionCode,
                previousVersionCode,
                value("repository"),
                value("branch"),
                value("applicationId"),
                value("certificateSha256"),
                abis,
            )
        }
    }
}

/** An old CI override may confirm the source identity, but must never replace it. */
fun releaseIdentityValue(
    expected: String,
    vararg overrides: Pair<String, String?>,
): String {
    overrides.forEach { (name, value) ->
        require(value == null || value == expected) { "$name must match release.properties ($expected)" }
    }
    return expected
}

/** Official builds use the release ABIs; CI may still exercise supported compatibility ABIs. */
fun resolveBuildAbis(
    abiOverride: String?,
    defaults: List<String>,
): List<String> =
    (abiOverride?.split(',') ?: defaults).also { abis ->
        require(abis.isNotEmpty() && abis.distinct().size == abis.size && abis.all { it in Versions.supportedAbis }) {
            "BUILD_ABI/buildABI must contain distinct supported ABI names"
        }
    }
