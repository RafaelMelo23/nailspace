package com.rafael.agendanails.webapp.infrastructure.dto.salon.profile;

import lombok.Builder;

@Builder
public record SalonProfilePublicDTO(
        String tradeName,
        String slogan,
        String primaryColor,
        String comercialPhone,
        String fullAddress,
        String socialMediaLink
) {
}
