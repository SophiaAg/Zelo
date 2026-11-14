# AGENTS.md — Zelo

## 1. Contexto e prioridade

Zelo é um MVP de hackathon para **continuidade do cuidado**. O produto acompanha o paciente após um encaminhamento, apoiando agendamento, lembretes, comparecimento e retorno. Priorize uma jornada ponta a ponta demonstrável; não transforme o MVP em um sistema hospitalar de agendamento.

## 2. Fonte de verdade

Antes de implementar funcionalidades, leia [`docs/SPEC.md`](docs/SPEC.md) e [`docs/FLOW.md`](docs/FLOW.md). Resolva contradições entre esses documentos antes de implementar e atualize ambos quando regras de negócio mudarem.

## 3. Stack e persistência

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA/Hibernate e JUnit.
- Banco oficial: **MySQL 8.0+**, com MySQL Connector/J. Não usar H2 como banco do MVP.
- Evolução do esquema: **Flyway** em `src/main/resources/db/migration/`.
- Usar português brasileiro para entidades, atributos, estados, regras e módulos de domínio, preservando nomes técnicos exigidos por Java/Spring.
- Credenciais e conexão vêm de `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`; nunca incluir segredos no código ou em commits.

Toda mudança no modelo persistido exige uma nova migration versionada, por exemplo `V2__descricao_da_alteracao.sql`. Não editar migrations já aplicadas em ambientes compartilhados, não depender de mudanças manuais no banco e nunca usar Hibernate `ddl-auto=update`, `create` ou `create-drop` para gerir o esquema. Flyway é a única ferramenta que cria ou altera tabelas; Hibernate deve validar (`ddl-auto=validate`).

## 4. Escopo do agendamento

As opções de data e horário são fictícias, pré-configuradas no backend e distintas por procedimento. O backend valida a opção e persiste o agendamento no MySQL. Não desenvolver gestão completa de agendas, CRUD de unidades, disponibilidade real, capacidade avançada, interfaces administrativas de vagas ou integração hospitalar/SISREG/SUS. A unidade é um nome fictício associado à opção. Não criar abstrações ou tabelas de vagas sem necessidade comprovada.

## 5. Regras de negócio e IA

- Centralizar regras nos serviços; manter controllers enxutos, validar entradas, usar DTOs, erros consistentes e transações ao registrar reservas.
- Persistir encaminhamentos, agendamentos e mudanças de estado; impedir reserva duplicada.
- A IA limita-se a interpretar mensagens e preferências, pedir esclarecimentos, apresentar opções fornecidas pelo backend e identificar pedidos de ajuda humana. Ela não inventa horários, toma decisões clínicas nem confirma reservas.
- Encaminhamentos de exame podem criar pendência de retorno (`necessitaRetorno`). O retorno aguarda exame realizado e liberação profissional. Falta ao retorno permite reagendar somente o retorno, sem repetir o exame.
- Lembretes periódicos de encaminhamentos não agendados e lembretes antes do atendimento devem parar quando deixarem de ser aplicáveis. Deve ser possível simulá-los/dispará-los manualmente na demonstração, sem infraestrutura complexa de mensageria.
- Usar somente dados sintéticos. Mensagens proativas exigem autorização e respeito às regras do canal.

## 6. Arquitetura

Manter monólito modular, com módulos de domínio em português conforme a necessidade, como `paciente`, `procedimento`, `encaminhamento`, `agendamento`, `notificacao` e `assistente`. Evitar camadas, módulos e microsserviços sem uso concreto. Antes de adicionar dependências, inspecionar a estrutura e as versões existentes.

## 7. Verificação e fluxo de trabalho

Antes de alterar código, leia este arquivo e os dois documentos de produto, inspecione estrutura/dependências/configurações e defina o critério de aceite e arquivos afetados. Implemente a menor solução suficiente. Execute compilação e testes disponíveis; não afirme resultados que não foram verificados. Se MySQL de teste não estiver disponível, explicite que a integração não foi validada. Não apagar dados nem executar operações destrutivas sem autorização.

Cobertura mínima do produto, quando houver implementação: criação de encaminhamento; reserva e prevenção de duplicidade; persistência MySQL após reinicialização; confirmação e ausência; exame com/sem retorno; ausência ao retorno sem repetir exame; conclusão do retorno; pedido de ajuda humana.
