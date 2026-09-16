package com.zenith.launcher.data.model

/** Controls how strongly Zenith protects the study surface from system-level distractions. */
enum class AttentionProtectionMode(val storageValue: String, val title: String, val description: String) {
    NORMAL("normal", "Normal", "Zenith keeps the experience focused without changing system UI."),
    STRONG("strong", "Strong", "Hide the status and navigation areas during Focus Mode and reflection windows."),
    DEDICATED("dedicated", "Dedicated device", "Use Android Lock Task when Zenith is provisioned as a device owner; otherwise fall back to Strong.");

    companion object {
        fun fromStorageValue(value: String?): AttentionProtectionMode =
            entries.firstOrNull { it.storageValue == value } ?: STRONG
    }
}
