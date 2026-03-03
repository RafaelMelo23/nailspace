-- 1. Create a Base Service
INSERT INTO service
(id, name, description, nail_count, duration_in_seconds, value, active, is_deleted, is_add_on, requires_loyalty, tenant_id)
VALUES
    (1001, 'Manicure', 'Basic Manicure', 10, 3600, 50, true, false, false, false, 'tenant-test');

-- 2. Create the Client
INSERT INTO users
(id, full_name, email, password, status, user_role, tenant_id)
VALUES
    (2001, 'Jane Client', 'jane.client@example.com', '$2a$12$Dw7G3DGxnNd4PpcYWUDPhe.6mAoCKIaG7xaRJOMs2.HoiCGmcIzTG', 'ACTIVE', 'CLIENT', 'tenant-test');

INSERT INTO clients
(user_id, missed_appointments, cancelled_appointments, phone_number)
VALUES
    (2001, 0, 0, '5561999081420');

-- 3. Create the Professional
INSERT INTO users
(id, full_name, email, password, status, user_role, tenant_id)
VALUES
    (2002, 'John Pro', 'john.pro@example.com', '$2a$12$Dw7G3DGxnNd4PpcYWUDPhe.6mAoCKIaG7xaRJOMs2.HoiCGmcIzTG', 'ACTIVE', 'ADMIN', 'tenant-test');

INSERT INTO professional
(user_id, external_id, is_active, is_first_login)
VALUES
    (2002, 'd4f901a1-9457-410a-83b5-31cf50d0322c', true, false);

-- 4. Create the Admin (Note: Based on OnboardingService, the Enum is typically ADMIN, not SUPER_ADMIN)
INSERT INTO users
(id, full_name, email, password, status, user_role, tenant_id)
VALUES
    (2003, 'System Administrator', 'admin@nailspro.com', '$2a$12$Dw7G3DGxnNd4PpcYWUDPhe.6mAoCKIaG7xaRJOMs2.HoiCGmcIzTG', 'ACTIVE', 'SUPER_ADMIN', 'tenant-test');

-- 5. Create 2 Appointments
-- Included 'id' column as the primary key mapped by the Java model
INSERT INTO appointment
(id, client_id, professional_id, main_service_id, total_value, observations, appointment_status, start_date, end_date, salon_trade_name, salon_zone_id, tenant_id)
VALUES
    (3001, 2001, 2002, 1001, 50.00, 'First appointment', 'CONFIRMED', '2026-04-01 10:00:00-03', '2026-04-01 11:00:00-03', 'Beauty Salon', 'America/Sao_Paulo', 'tenant-test'),
    (3002, 2001, 2002, 1001, 50.00, 'Monthly touch up', 'CONFIRMED', '2026-04-15 14:00:00-03', '2026-04-15 15:00:00-03', 'Beauty Salon', 'America/Sao_Paulo', 'tenant-test');

-- 6. Create 2 Appointment Notifications
-- Altered to 'appointment_id' to point to the Appointment's Primary Key mapped by JPA
-- Kept your accurate addition of 'attempts' and 'tenant_id'
INSERT INTO appointment_notification
(id, appointment_id, appointment_notification_type, appointment_notification_status, destination_number, attempts, tenant_id)
VALUES
    (4001, 3001, 'CONFIRMATION', 'PENDING', '5561999081420', 0, 'tenant-test'),
    (4002, 3002, 'REMINDER', 'PENDING', '5561999081420', 0, 'tenant-test');