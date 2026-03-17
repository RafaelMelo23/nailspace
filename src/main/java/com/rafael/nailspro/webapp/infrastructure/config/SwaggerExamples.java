package com.rafael.nailspro.webapp.infrastructure.config;

public final class SwaggerExamples {

    private SwaggerExamples() {
    }

    public static final String LOGIN_REQUEST = """
            {
              "email": "cliente@exemplo.com",
              "password": "mudar123"
            }
            """;

    public static final String REGISTER_REQUEST = """
            {
              "fullName": "Maria Cliente",
              "email": "maria.nova@exemplo.com",
              "rawPassword": "mudar123",
              "phoneNumber": "5500000000000"
            }
            """;

    public static final String CHANGE_EMAIL_REQUEST = """
            {
              "newEmail": "cliente.novo@exemplo.com",
              "password": "mudar123"
            }
            """;

    public static final String CHANGE_PHONE_REQUEST = """
            {
              "newPhone": "5500000000001",
              "password": "mudar123"
            }
            """;

    public static final String RESET_PASSWORD_REQUEST = """
            {
              "userEmail": "cliente@exemplo.com",
              "newPassword": "NovaSenha123",
              "resetToken": "reset-token-exemplo"
            }
            """;

    public static final String APPOINTMENT_CREATE_REQUEST = """
            {
              "professionalExternalId": "11111111-1111-1111-1111-111111111111",
              "mainServiceId": 1001,
              "addOnsIds": [1003],
              "zonedAppointmentDateTime": "2026-04-01T10:00:00-03:00",
              "observation": "Primeiro atendimento"
            }
            """;

    public static final String WORK_SCHEDULE_LIST_REQUEST = """
            [
              {
                "dayOfWeek": "MONDAY",
                "startTime": "09:00",
                "endTime": "18:00",
                "lunchBreakStartTime": "12:00",
                "lunchBreakEndTime": "13:00",
                "isActive": true
              },
              {
                "dayOfWeek": "TUESDAY",
                "startTime": "09:00",
                "endTime": "18:00",
                "lunchBreakStartTime": "12:00",
                "lunchBreakEndTime": "13:00",
                "isActive": true
              }
            ]
            """;

    public static final String SCHEDULE_BLOCK_REQUEST = """
            {
              "dateAndStartTime": "2026-04-10T09:00:00-03:00",
              "dateAndEndTime": "2026-04-10T12:00:00-03:00",
              "isWholeDayBlocked": false,
              "reason": "Almoco"
            }
            """;

    public static final String CREATE_PROFESSIONAL_REQUEST = """
            {
              "fullName": "Ana Profissional",
              "email": "profissional.novo@exemplo.com",
              "servicesOfferedByProfessional": [1001, 1002]
            }
            """;

    public static final String SALON_SERVICE_REQUEST = """
            {
              "name": "Manicure Simples",
              "value": 50,
              "durationInSeconds": 3600,
              "description": "Servico basico de manicure",
              "maintenanceIntervalDays": 30,
              "requiresLoyalty": false,
              "isAddOn": false,
              "professionals": [2002]
            }
            """;

    public static final String SALON_PROFILE_REQUEST = """
            {
              "tradeName": "Studio Bella Unhas",
              "slogan": "Cuidado e beleza para suas unhas",
              "primaryColor": "#FB7185",
              "logoBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA",
              "comercialPhone": "+5500000000000",
              "fullAddress": "Rua das Flores, 123",
              "socialMediaLink": "https://instagram.com/salaodebeleza_demo",
              "status": "OPEN",
              "warningMessage": "Atendimento com horario reduzido",
              "appointmentBufferMinutes": 0,
              "zoneId": "America/Sao_Paulo",
              "isLoyalClientelePrioritized": false,
              "loyalClientBookingWindowDays": 45,
              "standardBookingWindow": 30
            }
            """;

    public static final String ONBOARDING_REQUEST = """
            {
              "fullName": "Ana Profissional",
              "email": "profissional@exemplo.com",
              "domainSlug": "studio-bella-unhas"
            }
            """;

    public static final String PROFILE_PICTURE_BASE64 = "\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\"";
}
