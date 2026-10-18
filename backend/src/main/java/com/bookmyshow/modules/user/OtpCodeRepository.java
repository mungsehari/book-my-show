package com.bookmyshow.modules.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findFirstByPhoneOrderByCreatedAtDesc(String phone);
}
