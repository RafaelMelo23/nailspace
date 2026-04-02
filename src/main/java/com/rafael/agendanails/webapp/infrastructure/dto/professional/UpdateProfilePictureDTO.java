package com.rafael.agendanails.webapp.infrastructure.dto.professional;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfilePictureDTO(
    @NotBlank(message = "A imagem é obrigatória")
    String pictureBase64
) {}
