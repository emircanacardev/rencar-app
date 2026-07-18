package com.flowbytestudio.rencar.data.license

/** `LicenseStatusResponse.status` alanının backend sözleşmesi. */
enum class LicenseStatus {
    NOT_SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    // Henüz yüklenmedi ya da yükleme başarısız oldu; onaylı kullanıcıya yanlışlıkla
    // "ehliyetini doğrula" gösterilmemesi için varsayılan bu olmalı.
    UNKNOWN,
    ;

    companion object {
        fun from(raw: String?): LicenseStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

val LicenseStatusResponse.licenseStatusEnum: LicenseStatus
    get() = LicenseStatus.from(status)
