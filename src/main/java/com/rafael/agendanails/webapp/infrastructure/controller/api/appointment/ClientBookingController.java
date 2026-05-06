package com.rafael.agendanails.webapp.infrastructure.controller.api.appointment;

import com.rafael.agendanails.webapp.application.appointment.booking.BookingAppointmentUseCase;
import com.rafael.agendanails.webapp.application.appointment.booking.CancelAppointmentUseCase;
import com.rafael.agendanails.webapp.application.appointment.booking.FindProfessionalAvailabilityUseCase;
import com.rafael.agendanails.webapp.domain.model.UserPrincipal;
import com.rafael.agendanails.webapp.infrastructure.config.SwaggerExamples;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.AppointmentCreateDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.appointment.ProfessionalAvailabilityDTO;
import com.rafael.agendanails.webapp.infrastructure.dto.professional.FindProfessionalAvailabilityDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/booking")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Booking", description = "Client appointment booking")
public class ClientBookingController {

    private final BookingAppointmentUseCase bookingAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final FindProfessionalAvailabilityUseCase findProfessionalAvailabilityUseCase;

    @Operation(summary = "Book appointment", description = "Books a new appointment for the authenticated client.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment created"),
            @ApiResponse(responseCode = "400", description = "Validation error or schedule conflict"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AppointmentCreateDTO.class),
                    examples = @ExampleObject(name = "AppointmentCreateRequest", value = SwaggerExamples.APPOINTMENT_CREATE_REQUEST))
    )
    @PostMapping
    public ResponseEntity<Void> bookAppointment(@Valid @RequestBody AppointmentCreateDTO appointmentDTO,
                                                @AuthenticationPrincipal UserPrincipal userPrincipal) {

        bookingAppointmentUseCase.bookAppointment(appointmentDTO, userPrincipal);
        return ResponseEntity.status(201).build();
    }

    @Operation(summary = "Cancel appointment", description = "Cancels an appointment by id.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appointment cancelled"),
            @ApiResponse(responseCode = "400", description = "Invalid appointment id"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PatchMapping("/{appointmentId}")
    public ResponseEntity<Void> cancelAppointment(
                                                  @Parameter(example = "3001")
                                                  @PathVariable Long appointmentId,
                                                  @AuthenticationPrincipal UserPrincipal userPrincipal) {

        cancelAppointmentUseCase.cancelAppointment(appointmentId, userPrincipal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get professional availability",
            description = "Finds available time slots for a professional and the requested services.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Availability returned",
                    content = @Content(schema = @Schema(implementation = ProfessionalAvailabilityDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{professionalExternalId}/availability")
    public ResponseEntity<ProfessionalAvailabilityDTO> findAvailableProfessionalTimes(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @PathVariable
            @NotBlank(message = "O identificador externo do profissional é obrigatório.")
            @Parameter(example = "11111111-1111-1111-1111-111111111111")
            String professionalExternalId,

            @RequestParam
            @Min(value = 1, message = "A duração do serviço deve ser maior que zero segundos.")
            @Parameter(example = "3600")
            int serviceDurationInSeconds,

            @RequestParam
            @NotEmpty(message = "Pelo menos um serviço deve ser informado.")
            @Parameter(example = "1001,1003")
            List<@NotNull(message = "O ID do serviço não pode ser nulo.") Long> servicesIds
    ) {

        FindProfessionalAvailabilityDTO dto = FindProfessionalAvailabilityDTO.builder()
                .professionalExternalId(professionalExternalId)
                .serviceDurationInSeconds(serviceDurationInSeconds)
                .servicesIds(servicesIds)
                .build();

        return ResponseEntity.ok(findProfessionalAvailabilityUseCase.findAvailableTimes(dto, userPrincipal));
    }
}
