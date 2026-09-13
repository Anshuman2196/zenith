package com.zenith.launcher.data.model

/** Metadata about one installed third-party icon pack, discovered via PackageManager. */
data class IconPackInfo(
    val packageName: String,
    val label: String
)
