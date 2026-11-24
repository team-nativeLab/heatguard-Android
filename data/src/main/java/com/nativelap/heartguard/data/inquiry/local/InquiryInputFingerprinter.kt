package com.nativelap.heartguard.data.inquiry.local

interface InquiryInputFingerprinter {
    fun fingerprint(
        ownerId: String,
        title: String,
        content: String,
        createKeyIfMissing: Boolean,
    ): String
}
