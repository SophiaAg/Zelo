# Zelo — Especificação do MVP

## 1. Visão do produto

Zelo é uma plataforma de continuidade do cuidado que conecta profissionais de saúde e pacientes.

Após uma consulta, o profissional registra os exames ou consultas necessários. Um assistente conversacional acompanha o paciente, facilita o agendamento e envia lembretes até a conclusão das pendências.

## 2. Problema

Pacientes recebem encaminhamentos, mas podem esquecer de agendar exames, faltar aos atendimentos ou não retornar ao profissional após realizá-los.

A equipe de saúde nem sempre possui visibilidade centralizada sobre essas pendências.

## 3. Objetivo do MVP

Demonstrar uma jornada completa:

**Encaminhamento → agendamento → lembretes → comparecimento → retorno → conclusão.**

A solução utiliza uma agenda própria com disponibilidades cadastradas no sistema. Não consulta vagas reais de instituições externas.

## 4. Usuários

### Profissional de saúde

- Registra encaminhamentos.
- Consulta pacientes e suas pendências.
- Gerencia unidades, procedimentos e disponibilidade da agenda.
- Confirma comparecimentos ou ausências.
- Libera o retorno após o exame, quando aplicável.
- Acompanha solicitações de ajuda humana.

### Paciente

- Recebe notificações pelo WhatsApp (ou chat demonstrativo).
- Escolhe horários disponíveis.
- Interage por texto; áudio é opcional no MVP.
- Recebe lembretes de agendamento e comparecimento.
- Agenda retornos indicados pelo profissional.

## 5. Requisitos funcionais

- **RF-01:** Cadastrar e consultar pacientes fictícios.
- **RF-02:** Cadastrar unidades, procedimentos e horários disponíveis na agenda.
- **RF-03:** Permitir ao profissional criar encaminhamentos para exames e consultas.
- **RF-04:** Permitir criar retorno vinculado a um encaminhamento de exame.
- **RF-05:** Notificar o paciente quando existir um novo encaminhamento, mediante autorização de contato e regras do canal.
- **RF-06:** Permitir consultar, escolher e reservar horários disponíveis pelo assistente.
- **RF-07:** Interpretar mensagens em linguagem natural usando IA.
- **RF-08:** Registrar e atualizar estados de encaminhamentos e agendamentos.
- **RF-09:** Programar lembretes e permitir disparo de demonstração sem aguardar as datas reais.
- **RF-10:** Permitir ao profissional confirmar comparecimento ou ausência.
- **RF-11:** Disponibilizar agendamento do retorno após confirmação do exame e liberação pelo profissional.
- **RF-12:** Atualizar o painel conforme os estados se alterarem.
- **RF-13:** Encaminhar à equipe os casos que precisem de intervenção humana.

## 6. Regras de negócio

- **RN-01:** Todo encaminhamento pertence a um paciente e a um procedimento.
- **RN-02:** Somente horários cadastrados e disponíveis podem ser reservados.
- **RN-03:** Um horário de capacidade unitária não pode ter mais de uma reserva ativa. Revalidar a disponibilidade em transação no backend.
- **RN-04:** Encaminhamentos ainda não agendados podem gerar lembretes a cada 15 dias, respeitando consentimento e regras do canal.
- **RN-05:** Agendamentos podem gerar lembretes 7 dias e 1 dia antes, apenas se essas datas estiverem no futuro.
- **RN-06:** Lembretes deixam de ser aplicáveis após conclusão, cancelamento ou mudança de etapa.
- **RN-07:** Ausência ao exame permite reagendar **somente o exame**.
- **RN-08:** Ausência ao retorno permite reagendar **somente a consulta de retorno**.
- **RN-09:** Retorno vinculado a exame começa bloqueado para agendamento e só é liberado após confirmação de realização do exame e autorização do profissional.
- **RN-10:** Somente profissional de saúde confirma oficialmente comparecimento ou ausência.
- **RN-11:** A IA não pode inventar horários, confirmar reservas sem resposta do backend ou oferecer recomendações clínicas.
- **RN-12:** Se não houver vagas ou o paciente pedir ajuda, registrar necessidade de assistência humana.
- **RN-13:** Mensagens proativas exigem autorização do paciente e respeito às regras do provedor de WhatsApp.
- **RN-14:** Um exame concluído não conclui automaticamente seu retorno associado.
- **RN-15:** A confirmação de reserva é ação transacional da aplicação, não do modelo de IA.

## 7. Estados de encaminhamento

- `WAITING_PREREQUISITE`: aguarda etapa anterior, especialmente exame antes do retorno.
- `PENDING_SCHEDULING`: encaminhamento liberado e ainda sem reserva.
- `SCHEDULED`: existe agendamento confirmado.
- `COMPLETED`: atendimento realizado e confirmado pelo profissional.
- `NEEDS_RESCHEDULE`: houve falta ou perda de reserva; deve voltar ao agendamento.
- `NEEDS_ASSISTANCE`: exige intervenção da equipe.
- `CANCELLED`: cancelado.

O retorno começa em `WAITING_PREREQUISITE`. A realização do exame não o conclui automaticamente.

## 8. Entidades principais

- **Patient:** `id`, `name`, `phone`, `contactOptIn`.
- **Unit:** `id`, `name`.
- **Procedure:** `id`, `name`, `type` (`EXAM` ou `CONSULTATION`).
- **Slot:** `id`, `unitId`, `procedureId`, `startAt`, `status`.
- **Referral:** `id`, `patientId`, `procedureId`, `status`, `parentReferralId`, `createdAt`.
- **Appointment:** `id`, `referralId`, `slotId`, `status`.
- **Notification:** `id`, `referralId`, `type`, `scheduledAt`, `sentAt`.

Nomes e atributos adicionais podem ser definidos durante a implementação, desde que preservem as regras e os contratos acordados entre frontend e backend.

## 9. Telas do MVP

1. Dashboard de acompanhamento.
2. Lista de pacientes e encaminhamentos.
3. Formulário de novo encaminhamento.
4. Agenda e disponibilidade de horários.
5. Chat demonstrativo de contingência, caso o WhatsApp não esteja operacional.

## 10. Critérios de aceite

- **CA-01:** O profissional cria um encaminhamento e vê a pendência no dashboard.
- **CA-02:** O paciente recebe a mensagem ou a visualiza no chat demonstrativo.
- **CA-03:** O paciente escolhe um horário e a aplicação registra a reserva.
- **CA-04:** Uma vaga de capacidade unitária reservada deixa de ser ofertada.
- **CA-05:** O sistema demonstra lembretes associados ao agendamento.
- **CA-06:** O profissional confirma a realização do exame.
- **CA-07:** O profissional libera o retorno, sem exigir outro exame.
- **CA-08:** O paciente agenda o retorno.
- **CA-09:** O profissional confirma o retorno e conclui o ciclo.
- **CA-10:** Um pedido de ajuda aparece como pendência para a equipe.
- **CA-11:** Ao reiniciar a aplicação, agendamentos e estados persistidos continuam disponíveis no MySQL.

## 11. Fora do escopo

- Prontuário eletrônico completo.
- Diagnóstico, prescrição ou interpretação clínica de exames.
- Integrações reais com SUS/SISREG ou agendas externas.
- Convênios, faturamento e pagamentos.
- Autenticação e autorização completas (MVP com dados sintéticos, não adequado a produção).
- Agendamento em instituições sem agenda integrada.
- Processamento de resultados laboratoriais.
- RAG, agentes autônomos complexos e arquitetura de microsserviços.

## 12. Restrições técnicas e de demonstração

- Usar exclusivamente dados **fictícios/sintéticos**.
- Backend em **Java 21 + Spring Boot**.
- Frontend em **React + TypeScript**.
- Banco obrigatório: **MySQL 8.0+** (não usar H2 no MVP).
- Persistência de entidades e mudanças de estado com **Spring Data JPA / Hibernate**.
- Credenciais apenas em variáveis de ambiente; nunca no repositório.
- Garantir integridade referencial e transações para reserva de horários.
- Oferecer uma demonstração funcional sem dependência obrigatória do WhatsApp real.
- Priorizar uma jornada completa ponta a ponta.
- Identificar como simuladas todas as integrações, notificações e disponibilidades que não forem reais.
