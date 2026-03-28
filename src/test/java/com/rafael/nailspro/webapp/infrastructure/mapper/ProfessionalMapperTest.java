package com.rafael.nailspro.webapp.infrastructure.mapper;

import com.rafael.nailspro.webapp.domain.model.Appointment;
import com.rafael.nailspro.webapp.domain.model.Professional;
import com.rafael.nailspro.webapp.domain.model.SalonProfile;
import com.rafael.nailspro.webapp.infrastructure.dto.appointment.ProfessionalAppointmentScheduleDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.ProfessionalResponseDTO;
import com.rafael.nailspro.webapp.infrastructure.dto.professional.ProfessionalSimplifiedDTO;
import com.rafael.nailspro.webapp.support.factory.TestAppointmentFactory;
import com.rafael.nailspro.webapp.support.factory.TestClientFactory;
import com.rafael.nailspro.webapp.support.factory.TestProfessionalFactory;
import com.rafael.nailspro.webapp.support.factory.TestSalonServiceFactory;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionalMapperTest {

    private final ProfessionalMapper professionalMapper = new ProfessionalMapper();

    @Test
    void mapProfessionalsToSimplifiedDTO_ShouldMapCorrectly() {
        Professional professional = TestProfessionalFactory.standard();
        Set<ProfessionalSimplifiedDTO> result = professionalMapper.mapProfessionalsToSimplifiedDTO(Set.of(professional));

        assertThat(result).hasSize(1);
        ProfessionalSimplifiedDTO dto = result.iterator().next();
        assertThat(dto.externalId()).isEqualTo(professional.getExternalId());
        assertThat(dto.name()).isEqualTo(professional.getFullName());
        assertThat(dto.professionalPicture()).isEqualTo(professional.getProfessionalPicture());
    }

    @Test
    void toScheduleDTO_ShouldMapComplexFields() {
        ZoneId zoneId = ZoneId.of("America/Sao_Paulo");
        Professional professional = TestProfessionalFactory.standard();
        professional.setSalonProfile(SalonProfile.builder().zoneId(zoneId).build());

        Appointment appointment = TestAppointmentFactory.standard(
                TestClientFactory.standard(),
                professional,
                TestSalonServiceFactory.standard()
        );

        ProfessionalAppointmentScheduleDTO dto = ProfessionalMapper.toScheduleDTO(appointment);

        assertThat(dto.appointmentId()).isEqualTo(appointment.getId());
        assertThat(dto.clientId()).isEqualTo(appointment.getClient().getId());
        assertThat(dto.clientName()).isEqualTo(appointment.getClient().getFullName());
        assertThat(dto.status()).isEqualTo(appointment.getAppointmentStatus());
        assertThat(dto.totalValue()).isEqualByComparingTo(appointment.calculateTotalValue());
        
        assertThat(dto.startDate().getZone()).isEqualTo(zoneId);
        assertThat(dto.endDate().getZone()).isEqualTo(zoneId);
    }

    @Test
    void toDTO_ShouldMapBasicFields() {
        Professional professional = TestProfessionalFactory.standard();
        ProfessionalResponseDTO dto = ProfessionalMapper.toDTO(professional);

        assertThat(dto.id()).isEqualTo(professional.getId());
        assertThat(dto.externalId()).isEqualTo(professional.getExternalId());
        assertThat(dto.name()).isEqualTo(professional.getFullName());
        assertThat(dto.email()).isEqualTo(professional.getEmail());
        assertThat(dto.isActive()).isEqualTo(professional.getIsActive());
    }

    @Test
    void toDTOList_ShouldMapListCorrectly() {
        List<Professional> professionals = List.of(TestProfessionalFactory.standard(), TestProfessionalFactory.standard());
        List<ProfessionalResponseDTO> result = ProfessionalMapper.toDTOList(professionals);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo(professionals.get(0).getFullName());
        assertThat(result.get(1).name()).isEqualTo(professionals.get(1).getFullName());
    }
}
