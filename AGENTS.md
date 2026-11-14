# AGENTS.md — Zelo

## 1. Contexto

Zelo é um MVP de hackathon voltado à **continuidade do cuidado**. Profissionais de saúde criam encaminhamentos para exames e consultas; um assistente conversacional ajuda pacientes a agendar, envia lembretes e acompanha pendências até a conclusão.

## 2. Fonte de verdade

Antes de implementar qualquer funcionalidade, leia:

- [`docs/SPEC.md`](docs/SPEC.md): requisitos, escopo e regras de negócio.
- [`docs/FLOW.md`](docs/FLOW.md): jornadas, estados e exceções.

Se houver contradição entre documentos, reporte e resolva antes de implementar. Não altere regras de negócio sem atualizar a documentação correspondente.

## 3. Prioridades

1. Entidades e persistência MySQL.
2. Agenda e disponibilidades.
3. Encaminhamentos.
4. Agendamento e reserva de vagas.
5. Confirmação de comparecimento ou ausência.
6. Fluxo de exame e retorno.
7. Assistente conversacional em texto.
8. Lembretes e exceções.
9. Integração WhatsApp.
10. Entrada/saída de áudio, se houver tempo.

Entregue primeiro uma **jornada ponta a ponta funcional**. Não comprometa o fluxo principal para adicionar funcionalidades secundárias.

## 4. Stack

### Backend

- Java 21.
- Spring Boot.
- Spring Web.
- Spring Data JPA / Hibernate.
- **MySQL 8.0+** como banco principal (não usar H2 no MVP).
- MySQL Connector/J.
- JUnit para testes.

### Frontend

- React, TypeScript e Vite.
- Componentes reutilizáveis e formulários simples.

### IA

- Gemini API, se disponível.
- Interpretar intenções e preferências de data/horário.
- Usar saídas estruturadas e validação pelo backend.

### WhatsApp

- Integração desacoplada do domínio.
- Provedor configurável; não fixar regras de negócio em SDK específico.
- Chat demonstrativo como contingência.

Antes de adicionar dependências, confira as bibliotecas e a estrutura já existentes.

## 5. Arquitetura

Adotar **monólito modular**, com módulos por domínio, conforme a necessidade:

- `patient`
- `unit`
- `procedure`
- `schedule`
- `referral`
- `appointment`
- `notification`
- `assistant`

Cada módulo pode conter `controller`, `service`, `repository` e DTOs. Evite criar camadas ou abstrações sem uso concreto. Não introduza microsserviços.

## 6. Regras de implementação

- Respeitar a especificação e os fluxos.
- Centralizar regras de negócio nos serviços.
- Manter controllers enxutos e validar entradas no backend.
- Utilizar DTOs nos contratos HTTP.
- Tratar erros com respostas consistentes.
- Garantir integridade referencial no MySQL.
- Utilizar transações nas operações de reserva de horários.
- Impedir reservas duplicadas com controle transacional e restrições adequadas no banco.
- Persistir alterações de estado; não depender de memória volátil.
- Nunca confirmar uma reserva apenas pela resposta da IA.
- Manter credenciais em variáveis de ambiente, nunca no código ou em commits.

## 7. IA e segurança

A IA interpreta **linguagem natural**, mas **não toma decisões clínicas**.

Pode:

- Identificar intenção de agendamento.
- Interpretar preferências de data e horário.
- Solicitar esclarecimentos.
- Apresentar horários fornecidos pelo backend.
- Identificar pedidos de ajuda humana.

Não pode:

- Inventar vagas.
- Prescrever tratamentos.
- Interpretar resultados de exames clinicamente.
- Alterar encaminhamentos médicos.
- Concluir atendimentos automaticamente.

A aplicação valida toda ação sugerida pela IA. Utilizar apenas dados sintéticos. Não inserir dados reais de saúde nem registrar segredos ou dados sensíveis em logs. Mensagens proativas só podem ser enviadas mediante autorização e em conformidade com o canal.

## 8. Testes mínimos

Cobrir, no mínimo:

1. Criação de encaminhamento.
2. Reserva de horário disponível.
3. Prevenção de reserva duplicada.
4. Persistência no MySQL após reinicialização.
5. Confirmação de comparecimento.
6. Ausência e reagendamento do mesmo procedimento.
7. Exame realizado e retorno liberado.
8. Ausência ao retorno sem exigir repetição do exame.
9. Conclusão do retorno.
10. Solicitação de assistência humana.

Executar os testes disponíveis antes de considerar uma tarefa concluída.

## 9. Fluxo de trabalho do agente

Antes de alterar código:

1. Ler `AGENTS.md`, `docs/SPEC.md` e `docs/FLOW.md`.
2. Inspecionar estrutura e dependências existentes.
3. Identificar a tarefa e seu critério de aceite.
4. Definir arquivos e contratos da API afetados.
5. Implementar a menor solução suficiente.
6. Executar testes e verificações disponíveis.
7. Relatar alterações, testes executados e pendências.

Não reestruturar módulos sem necessidade. Não adicionar escopo extra. Nunca afirmar que um teste passou sem executá-lo.

## 10. Critério de pronto

Uma tarefa só está concluída quando:

- Atende aos requisitos documentados.
- Respeita as regras de negócio e estados.
- Integra-se aos módulos necessários.
- Tem tratamento de erro adequado.
- Foi testada ou sua limitação foi explicitada.
- Não compromete a demonstração principal.
