package com.rafael.agendanails.webapp.infrastructure.dto.admin.salon.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rafael.agendanails.webapp.domain.enums.salon.OperationalStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.ZoneId;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SalonProfileDTO(
        @NotBlank(message = "Nome comercial é obrigatório")
        @Size(max = 100, message = "Nome comercial deve ter no máximo 100 caracteres")
        String tradeName,
        
        @Size(max = 200, message = "Slogan deve ter no máximo 200 caracteres")
        String slogan,
        
        @NotBlank(message = "Cor primária é obrigatória")
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", 
                 message = "Cor primária deve ser um código hexadecimal válido (ex: #FF5733)")
        String primaryColor,
        
        @Pattern(regexp = "^data:image\\/(jpeg|jpg|png|gif);base64,[A-Za-z0-9+/=]+$",
                 message = "Logo deve ser uma imagem válida em formato Base64")
        String logoBase64,
        
        @NotBlank(message = "Telefone comercial é obrigatório")
        @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$|^\\([0-9]{2}\\)\\s[0-9]{4,5}-[0-9]{4}$",
                 message = "Telefone comercial deve ter um formato válido")
        String comercialPhone,
        
        @NotBlank(message = "Endereço completo é obrigatório")
        @Size(max = 300, message = "Endereço completo deve ter no máximo 300 caracteres")
        String fullAddress,
        
        @Pattern(regexp = "^(https?://)?(www\\.)?(facebook|instagram|twitter|linkedin|youtube|tiktok)\\.com/.+",
                 message = "Link de rede social deve ser uma URL válida")
        String socialMediaLink,
        
        @NotNull(message = "Status operacional é obrigatório")
        OperationalStatus status,
        
        @Size(max = 500, message = "Mensagem de aviso deve ter no máximo 500 caracteres")
        String warningMessage,
        
        @NotNull(message = "Tempo de buffer é obrigatório")
        @PositiveOrZero(message = "Tempo de buffer entre agendamentos deve ser zero ou um número positivo")
        @Max(value = 120, message = "Tempo de buffer não pode exceder 120 minutos")
        Integer appointmentBufferMinutes,
        
        @NotNull(message = "Fuso horário é obrigatório")
        ZoneId zoneId,
        
        @NotNull(message = "Priorização de clientes fiéis deve ser informada")
        Boolean isLoyalClientelePrioritized,
        
        @Positive(message = "Janela de agendamento para clientes fiéis deve ser um número positivo de dias")
        @Max(value = 365, message = "Janela de agendamento para clientes fiéis não pode exceder 365 dias")
        Integer loyalClientBookingWindowDays,
        
        @NotNull(message = "Janela de agendamento padrão é obrigatória")
        @Positive(message = "Janela de agendamento padrão deve ser um número positivo de dias")
        @Max(value = 180, message = "Janela de agendamento padrão não pode exceder 180 dias")
        Integer standardBookingWindow) {
}