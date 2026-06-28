package com.crsmthw.unotracker.data.config.dto

/** All-nullable Gson DTOs for `manuals.json` (Unsafe-deserialization safety; ProGuard-kept). */
class ManualsRootDto {
    var manuals: List<ManualDto>? = null
}

class ManualDto {
    var variantId: String? = null
    var subtitle: String? = null
    var sections: List<SectionDto>? = null
}

class SectionDto {
    var key: String? = null
    var title: String? = null
    var body: String? = null
}
