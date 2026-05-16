# NailSpace SaaS

[![Java 21](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)](https://jdk.java.net/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL 15](https://img.shields.io/badge/PostgreSQL-15-blue?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)](https://www.docker.com/)

Plataforma de agendamento multi-tenant para estúdios de unhas com integração de mensagens via WhatsApp e acompanhamento de retorno de clientes.

---

## 🎯 Destaques Técnicos

Este projeto demonstra a aplicação de padrões de engenharia de software em um cenário real de SaaS:
- **Arquitetura:** Uso de Domain-Driven Design (DDD), eventos e isolamento de dados (Multi-tenancy).
- **Confiabilidade:** Rotinas de manutenção, retentativa de mensagens e monitoramento via Sentry.
- **Performance:** Uso de cache e execução assíncrona para garantir baixa latência.
- **Qualidade:** Testes de integração com Testcontainers, documentação com Swagger e containerização com Docker para maior paridade entre prod/dev.

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

## 🚀 Funcionalidades

### Para Clientes
- **Sistema de Agendamento:** Reserva de horários baseada na disponibilidade e tempo total do serviço (incluindo serviços adicionais).
- **Lembretes via WhatsApp:** Notificações enviadas automaticamente em horários definidos antes do atendimento.
- **Perfil do Usuário:** Histórico de visitas, status de fidelidade e dados de cadastro.
- **Regras de Reserva:** Configuração de janelas de agendamento que podem variar conforme o perfil do cliente.

### Para Profissionais
- **Agenda em Tempo Real:** Visualização da agenda diária e semanal.
- **Controle de Atendimento:** Gestão de status do agendamento (Confirmado, Finalizado, Cancelado ou Falta).
- **Bloqueios de Horário:** Opção para bloquear horários/dias na agenda para pausas ou compromissos.
- **Horário de Trabalho:** Definição de jornadas e intervalos de almoço.

### Para Gestão do Salão
- **Cadastro de Unidades (Multi-tenant):** Fluxo para registro de novos salões no sistema.
- **Gestão de Serviços:** Configuração de preços, durações e prazos sugeridos para retorno (manutenção).
- **Relatórios:** Dashboards de receita, ticket e agendamentos.
- **Customização Básica:** Configuração do nome e identidade visual da interface de agendamento.

---

## 🏗️ Arquitetura

O projeto utiliza princípios de **Domain-Driven Design (DDD)** para organizar a lógica de negócio e garantir um código manutenível.

### Decisões de Projeto
- **Isolamento de Dados (Multi-tenancy):** Implementado via AOP e filtros do Hibernate. Cada requisição aplica o `tenantId` automaticamente em todas as consultas SQL, garantindo a separação de dados entre salões.
- **Processamento Baseado em Eventos:** Uso de `DomainEvents` para desacoplar tarefas. Ações como envio de mensagens e atualização de métricas são processadas de forma assíncrona após o commit da transação.
- **Cálculo de Disponibilidade:** Algoritmo para busca de horários livres baseado em horários ocupados e cache via Caffeine para garantir respostas rápidas.
- **Frontend Modular:** Uso de ES Modules (Vanilla JS) e fragmentos Thymeleaf (para telas de erro) para uma navegação rápida sem a necessidade de frameworks complexos.

---

## ⚙️ Rotinas Automáticas

O sistema executa diversas tarefas em segundo plano para manter a operação:

### Automações de Processo
- **API de Retorno de Clientes:** Rotina diária que analisa o histórico de serviços e sugere datas para novos agendamentos, enviando convites automáticos via WhatsApp.
- **Lembretes de Agendamento:** Monitoramento constante de horários próximos para envio de alertas aos clientes, ajudando a reduzir faltas.
- **Retentativa de Mensagens:** Mecanismo que identifica falhas no envio de notificações e tenta reenviá-las automaticamente.

### Manutenção do Sistema
- **Limpeza de Dados:** Rotinas para remover tokens expirados e logs de mensagens antigos (mais de 30 dias).
- **Reset de Demonstração:** O ambiente de demo é restaurado diariamente para garantir que novos usuários sempre encontrem o sistema em um estado limpo.

---

## 🛡️ Segurança
- **Controle de Acesso (RBAC):** Níveis de permissão distintos para `SUPER_ADMIN`, `ADMIN`, `PROFESSIONAL` e `CLIENT`.
- **Autenticação JWT:** Implementação stateless com suporte a refresh tokens e cookies seguros.
- **Proteção de Dados:** Isolamento de inquilinos (tenants) validado em cada transação de banco de dados.

---

## 🛠️ Tech Stack
| Camada | Tecnologias                                                   |
| --- |---------------------------------------------------------------|
| **Backend** | Java 21, Spring Boot 3.5 (Web, Data, Security), Caffeine Cache |
| **Frontend** | Vanilla JavaScript (ESM), CSS, Thymeleaf                      |
| **Banco de Dados** | PostgreSQL 15, Flyway                                         |
| **Integrações** | Evolution API (WhatsApp), Sentry (Observabilidade)            |
| **Infraestrutura** | Docker Compose, Spring Boot Actuator, Logback (JSON Encoding) |
| **Testes** | JUnit 5, Testcontainers, Mockito                              |

* A Resend API foi removida da versão de produção atual.
---

## 🚦 Execução Local

### 1. Pré-requisitos
- Docker e Docker Compose

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