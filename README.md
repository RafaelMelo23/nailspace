# NailSpace SaaS

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://jdk.java.net/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)

Plataforma SaaS multi-tenant para gestão de agendamentos 
e automação de retenção, desenvolvida especificamente 
para estúdios de unhas.
O sistema integra um motor de disponibilidade 
de alta performance com automação via WhatsApp 
para otimizar a operação e a fidelização de clientes.
O sistema foi desenvolvido como um MVP, a escolha de abordagens e tecnologias foi feita
com esse propósito em mente, dimensionando as escolhas para um produto mínimo, mas também
levando em mente futuras incrementações e mudanças de paradigma (como tornar a aplicação distribuída).

---

## 📸 Visualização do Sistema

### Interface do Cliente
<p>
  <img src="pictures/book-first-page.png" width="30%" alt="Página Inicial de Agendamento">
  <img src="pictures/client-book-appointment-date.png" width="30%" alt="Seleção de Data">
  <img src="pictures/client-profile.png" width="30%" alt="Perfil do Cliente">
</p>

### Painel Administrativo
<p>
  <img src="pictures/admin-appointments.png" width="30%" alt="Gestão de Agendamentos">
  <img src="pictures/admin-professionals.png" width="30%" alt="Gestão de Profissionais">
  <img src="pictures/admin-salon.png" width="30%" alt="Configurações do Salão">
</p>

### Visão do Profissional
<p>
  <img src="pictures/professional-daily-app.png" width="30%" alt="Agenda Diária">
  <img src="pictures/professional-work-schedule.png" width="30%" alt="Horário de Trabalho">
  <img src="pictures/schedule-block.png" width="30%" alt="Bloqueio de Agenda">
</p>

### Automação WhatsApp
<p>
  <img src="pictures/example-confirmation.png" width="30%" alt="Confirmação">
  <img src="pictures/example-today-reminder.png" width="30%" alt="Lembrete">
  <img src="pictures/example-retention-mssg.png" width="30%" alt="Retenção">
</p>

---

## 🛠️ Diferenciais do Sistema

*   **Interface SPA Progressiva:** Arquitetura baseada em módulos Vanilla JS e fragmentos Thymeleaf, proporcionando transições de página rápidas sem a complexidade de frameworks pesados.
*   **Sincronização WhatsApp em Tempo Real:** Conexão nativa via Evolution API com monitoramento de status e pareamento via QR Code atualizado via **Server-Sent Events (SSE)**.
*   **Retenção Preditiva:** Algoritmo que calcula janelas de manutenção com base no histórico de serviços e automatiza o contato para novos agendamentos.

---

## 💼 Proposta de Valor

### Gestão Estratégica
- **Redução de Faltas:** Confirmações e lembretes automatizados via WhatsApp para garantir a ocupação da agenda.
- **Ciclo de Recorrência:** Previsão automática de retorno, notificando clientes no momento ideal para manutenção.
- **Identidade Visual (White-Label):** Personalização de cores e marca por salão, aplicada instantaneamente na interface do cliente.
- **Painéis Operacionais:** Dashboards com métricas de receita estimada, ticket médio e produtividade por profissional.

### Fluxo de Trabalho do Profissional
- **Agenda Dinâmica:** Visualização clara de compromissos diários com atualização de status (pendente, confirmado, finalizado).
- **Gestão de Jornada:** Configuração individualizada de horários de trabalho, pausas e bloqueios manuais.

---

## 🏗️ Arquitetura e Engenharia

### Padrões de Design e Implementação
- **Multi-Tenancy Nativa:** Isolamento rigoroso de dados em nível de repositório utilizando filtros Hibernate e Spring AOP (`TenantAspect`). A resolução de contexto suporta claims de JWT e roteamento por subdomínios.
- **Motor de Disponibilidade:** Cálculo de slots em janelas de 30 minutos com suporte a múltiplos serviços (add-ons) e trava pessimista para evitar conflitos de reserva.
- **Comunicação Orientada a Eventos:** Uso de `@TransactionalEventListener` e processamento assíncrono para pipelines de mensageria, garantindo que a experiência do usuário não seja afetada pelo tempo de resposta de APIs externas.
- **Caffeine Cache:** Implementação de cache de segundo nível para a API, reduzindo a latência e o consumo de recursos ao servir configurações do salão e horários disponíveis através de caching.
- **Strategy Pattern para Webhooks:** Processamento modular de eventos da Evolution API, permitindo fácil extensão para novos tipos de mensagens e notificações.

### Stack
| Camada | Tecnologias |
| --- | --- |
| **Backend** | Java 21, Spring Boot 3.5, Caffeine Cache, Spring Security (JWT) |
| **Frontend** | Vanilla JavaScript (ESM), CSS, Thymeleaf Fragments |
| **Banco de Dados** | PostgreSQL 15, Flyway |
| **Integrações** | Evolution API (WhatsApp), Resend (E-mail), Sentry (Observabilidade) |
| **Infraestrutura** | Docker Compose, Spring Boot Actuator, Logback (JSON Encoding) |
| **Testes** | JUnit 5, Testcontainers (PostgreSQL Real), Mockito |

* A Resend API foi removida da versão de produção atual.
---

## 📊 Arquitetura de Fluxo

| Origem             | Destino           | Meio / Protocolo | Finalidade |
|:-------------------|:------------------| :--- | :--- |
| Navegador          | Backend           | REST / SSE | Operações de interface e notificações live |
| Backend            | PostgreSQL        | Hibernate Filter | Persistência com isolamento multi-tenant |
| Backend            | Evolution API     | Webhooks / HTTP | Sincronização e disparos via WhatsApp |
| Backend            | Sentry / Actuator | Logs Estruturados | Observabilidade e métricas de saúde |
| Agendador          | Motor de Retenção | Spring Scheduling | Cálculo preditivo e gatilhos de mensagens |

---

## 🚦 Execução Local

### 1. Pré-requisitos
- Docker e Docker Compose
- Java 21+ (para desenvolvimento local)

### 2. Configuração de Ambiente
Crie o arquivo `.env` com base no exemplo:
```bash
cp .env.example .env
```

### 3. Deploy via Docker
```bash
./mvnw clean package -DskipTests

docker compose up -d --build
```

- **Acesso à Aplicação:** `http://localhost:8080`
- **Documentação da API:** `http://localhost:8080/swagger-ui/index.html`

---

## 🛡️ Segurança
- **Controle de Acesso (RBAC):** Níveis de permissão distintos para `SUPER_ADMIN`, `ADMIN`, `PROFESSIONAL` e `CLIENT`.
- **Autenticação JWT:** Implementação stateless com suporte a refresh tokens e cookies seguros.
- **Proteção de Dados:** Isolamento de inquilinos (tenants) validado em cada transação de banco de dados.
---