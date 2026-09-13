package com.zenith.launcher.data.model

import kotlinx.serialization.Serializable

/**
 * A quick-access shortcut to a study PDF. [uriString] stores a Storage-Access-Framework
 * Uri as a string, since DataStore/JSON can't persist Uri objects directly.
 */
@Serializable
data class PdfLink(
    val id: String,
    val title: String,
    val uriString: String
)
