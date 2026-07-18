package com.flowbytestudio.rencar.data.auth

/**
 * Telefon/parola/OTP alan kısıtları (bkz. api-openapi_v2.json: LoginDto, VerifyOtpDto,
 * RegisterDto). Login ve kayıt ekranları aynı kuralları uyguladığı için tek yerden okunur.
 */
object AuthConstants {
    // Backend'e "+90" öneki ile gönderilen yerel telefon numarasının hane sayısı.
    const val PHONE_DIGIT_COUNT = 10

    // SMS ile gönderilen tek kullanımlık doğrulama kodunun hane sayısı.
    const val OTP_DIGIT_COUNT = 6

    const val MIN_PASSWORD_LENGTH = 6

    // OTP tekrar gönderme sayacı (bkz. /auth/login açıklaması: kod 5 dk geçerli;
    // burada kullanıcıyı spam'den korumak için ekran tarafında ayrı bir bekleme uygulanır).
    const val OTP_RESEND_COOLDOWN_SECONDS = 60
}
