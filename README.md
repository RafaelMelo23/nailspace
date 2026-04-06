# Agenda Nails SaaS

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=java)](https://jdk.java.net/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue?logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue?logo=docker)](https://www.docker.com/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.0-green?logo=swagger)](https://swagger.io/specification/)

SaaS de agendamento e retenção para salões de beleza e estúdios de unhas. A plataforma combina motor de agenda, automação de CRM e mensageria multicanal para reduzir no-show, aumentar recorrência e dar visão operacional em tempo real.

**Este repositório é um produto completo**: backend com multi-tenancy, integrações com WhatsApp via Evolution API, front-end completo, jobs de automação e observabilidade.

---

## Para Recrutadores (Resumo Técnico)

- **Multi-tenant real com isolamento**: filtro Hibernate habilitado via AOP em `TenantAspect`, resolvendo `tenantId` por JWT claim ou subdomínio.
- **Motor de agenda consistente**: cálculo de disponibilidade por janela, intervalos de buffer e bloqueios, com lock pessimista na reserva para evitar corrida.
- **Arquitetura orientada a eventos**: `@TransactionalEventListener` + `@Async` para confirmação e lembretes sem travar a experiência do usuário.
- **Mensageria inteligente**: WhatsApp integrado via Evolution API, webhooks processados por Strategy e feedback em tempo real via SSE.
- **Segurança sólida**: JWT, refresh token, RBAC por papéis.
- **Observabilidade pronta para produção**: Sentry, logs estruturados e métricas baseadas em Spring Actuator.
- **Testes robustos**: JUnit 5, Testcontainers e suíte pronta para validar regras de negócio e infraestrutura.

---

## Visão de Produto

O Scheduling Nails Pro foi pensado como uma plataforma que faz o salão operar no piloto automático:

- **Agenda inteligente**: disponibilidade por profissional, intervalos de serviço, buffers e bloqueios manuais.
- **Retenção automatizada**: previsão de retorno por manutenção e disparo de convites personalizados.
- **WhatsApp como canal principal**: confirmação, lembretes e follow-up com status rastreável.
- **Insights gerenciais**: receita, ticket médio, produtividade e comportamento de clientes.

---

## Funcionalidades (v2)

- **Agendamento do cliente**: disponibilidade por profissional, múltiplos serviços (add-ons), cancelamento e janela preferencial.
- **Gestão de profissionais**: agenda semanal, pausas, bloqueios rápidos e perfil com foto.
- **Gestão do salão**: serviços ativos/inativos, intervalos de manutenção e configurações gerais.
- **CRM e retenção**: previsão automática de retorno, status do cliente e follow-up programado.
- **Painéis administrativos**: receita mensal, histórico diário e perfil 360º do cliente.
- **Mensageria**: confirmação, lembretes programados, reenvio com status e limpeza de mensagens antigas.
- **Onboarding multi-tenant**: criação de tenant, salão e primeiro admin com slug dedicado.
- **Portal web**: páginas públicas e privadas servidas pelo próprio backend (Thymeleaf + JS).

---

## Arquitetura e Design

- **Camadas bem definidas**: `domain` (regras), `application` (casos de uso), `infrastructure` (adapters), `shared` (cross-cutting).
- **Multi-tenant por filtro**: habilitação automática do filtro de tenant em repositórios, com exceções explícitas via `@IgnoreTenantFilter`.
- **Engine de disponibilidade**: normalização de intervalos ocupados (appointments + blocks), cálculo de slots em passos de 30 minutos.
- **Mensageria resiliente**: pipeline com status de envio, retries e persistência de mensagens.
- **SSE em tempo real**: comunicação de eventos de conexão e QR Code para WhatsApp.

---

## Stack Tecnológica

- **Backend**: Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Validation.
- **Persistência**: Spring Data JPA, PostgreSQL 15, Flyway.
- **Autenticação**: JWT (Auth0), BCrypt.
- **Integrações**: Evolution API (WhatsApp), Resend (e-mail).
- **Observabilidade**: Sentry, Actuator, Logback com encoder JSON.
- **Frontend**: Thymeleaf + HTML/CSS/JS modularizado.
- **Infra**: Docker, Docker Compose.
- **Testes**: JUnit 5, Testcontainers.
- **Qualidade**: suíte de testes automatizados com suporte a banco real via Testcontainers.

---

## Executar Localmente

```bash
cp .env.example .env
./mvnw clean package -DskipTests
docker compose up -d --build
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Variáveis de Ambiente

| Variável | Descrição |
| --- | --- |
| `POSTGRES_DB_APP` | Nome do banco principal da aplicação |
| `POSTGRES_DB_EVO` | Nome do banco da Evolution API |
| `POSTGRES_USER` | Usuário do PostgreSQL |
| `POSTGRES_PASSWORD` | Senha do PostgreSQL |
| `JWT_SECRET` | Segredo de assinatura do JWT |
| `ALLOWED_ORIGINS` | CORS permitido |
| `RESEND_API_KEY` | Chave da Resend (e-mail) |
| `MAIL_HOST` | Host SMTP (fallback) |
| `MAIL_PORT` | Porta SMTP |
| `MAIL_USER` | Usuário SMTP |
| `EVO_API_KEY` | Chave da Evolution API |
| `DOMAIN_URL` | URL pública do domínio do produto |
| `SENTRY_DSN` | DSN do Sentry |

---

## Acesso e Papéis

- **SUPER_ADMIN**: onboarding de tenants e endpoints internos.
- **ADMIN**: gestão do salão, serviços, profissionais e insights.
- **PROFESSIONAL**: agenda e perfil profissional.
- **CLIENT**: agendamento e área do cliente.

---