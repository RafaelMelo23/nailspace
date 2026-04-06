package com.rafael.agendanails.webapp.infrastructure.dto.salon.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalonProfilePublicDTO(
        String tradeName,
        String slogan,
        String primaryColor,
        String comercialPhone,
        String fullAddress,
        String socialMediaLink,
        String warningMessage
) {
}
