# Zelo — Especificação do MVP

## 1. Visão e problema

Zelo demonstra como um assistente pode reduzir a perda de continuidade do cuidado após uma consulta. Pacientes podem esquecer de agendar exames, faltar a atendimentos ou deixar de retornar ao profissional.

O objetivo é acompanhar a pendência do encaminhamento até a conclusão, com apoio de agendamento simplificado, lembretes e assistência humana. Zelo **não é um sistema hospitalar de agendamento**.

## 2. Usuários

### Profissional de saúde

- Registra encaminhamentos para exames ou consultas e indica se o exame exige retorno.
- Consulta pendências e confirma comparecimento ou ausência.
- Após a realização do exame, autoriza a liberação do retorno quando aplicável.
- Visualiza casos que precisam de assistência humana.

### Paciente

- Recebe contato autorizado por WhatsApp ou chat demonstrativo.
- Solicita opções, escolhe um horário fictício e recebe confirmação do backend.
- Recebe lembretes e pode pedir ajuda humana.

## 3. Escopo e jornada principal

1. Profissional registra um encaminhamento.
2. Assistente contata o paciente conforme autorização e regras do canal.
3. Paciente pede opções de horário.
4. Backend fornece opções fictícias previamente configuradas por tipo de procedimento (por exemplo, hemograma, ultrassonografia e consulta de retorno).
5. Assistente apresenta somente essas opções.
6. Paciente escolhe uma opção; backend valida e registra o agendamento no MySQL.
7. Sistema atualiza a situação do encaminhamento.
8. Assistente confirma a reserva e acompanha a próxima etapa.

Os horários são dados de demonstração definidos no backend; não representam disponibilidade real. O agendamento e as mudanças de estado são persistidos. A unidade pode ser um nome fictício apresentado junto ao horário. Não é necessário cadastrar ou administrar unidades.

## 4. Requisitos funcionais

- **RF-01:** Manter pacientes fictícios com nome, telefone e autorização de contato.
- **RF-02:** Permitir ao profissional registrar encaminhamento de exame ou consulta.
- **RF-03:** Oferecer opções fictícias configuradas no backend por procedimento; a IA não pode inventar opções.
- **RF-04:** Validar a escolha e persistir o agendamento e as mudanças de situação no MySQL.
- **RF-05:** Interpretar mensagens em linguagem natural para identificar intenção/preferências, esclarecer ambiguidades e encaminhar pedidos de ajuda; validar toda ação no backend.
- **RF-06:** Enviar ou simular lembretes periódicos para encaminhamentos não agendados e lembretes antes do atendimento, com interrupção quando não forem mais aplicáveis.
- **RF-07:** Permitir simular/disparar manualmente lembretes na demonstração, sem mensageria complexa.
- **RF-08:** Permitir ao profissional confirmar comparecimento ou ausência.
- **RF-09:** Quando `necessitaRetorno=true`, criar pendência de retorno vinculada e bloqueada até exame realizado e liberação profissional; depois oferecer agendamento e acompanhamento.
- **RF-10:** Quando `necessitaRetorno=false`, encerrar o encaminhamento após confirmação profissional da realização do exame.
- **RF-11:** Registrar solicitação de assistência humana e exibi-la como pendência.
- **RF-12:** Disponibilizar chat demonstrativo se WhatsApp não estiver operacional.

## 5. Regras de negócio

- **RN-01:** Todo encaminhamento pertence a um paciente e a um procedimento.
- **RN-02:** Horários são fictícios, definidos no backend e diferentes conforme o procedimento; somente opções oferecidas e ainda válidas podem ser escolhidas.
- **RN-03:** A confirmação da reserva é feita pelo backend após validação e persistência transacional, nunca pela IA.
- **RN-04:** Um mesmo encaminhamento não pode ter duas reservas ativas. Persistência e restrições devem evitar duplicidade.
- **RN-05:** Encaminhamentos liberados sem agendamento podem receber lembretes a cada 15 dias, respeitando consentimento e regras do canal.
- **RN-06:** Agendamentos podem receber lembretes 7 dias e 1 dia antes, se essas datas estiverem no futuro.
- **RN-07:** Interromper lembretes após conclusão, cancelamento, mudança de etapa ou quando atendimento humano tornar o lembrete inaplicável.
- **RN-08:** Ausência ao exame permite reagendar somente o exame.
- **RN-09:** Ausência ao retorno permite reagendar somente o retorno; não reiniciar o exame já realizado.
- **RN-10:** O retorno começa aguardando pré-requisito e só é liberado após realização do exame confirmada pelo profissional e liberação profissional.
- **RN-11:** Somente profissional confirma comparecimento/ausência e realização do exame. Mensagens do paciente ou IA não concluem etapa clínica.
- **RN-12:** Exame concluído não conclui automaticamente o retorno associado.
- **RN-13:** Se não houver opções ou o paciente pedir ajuda, registrar necessidade de assistência humana.
- **RN-14:** Mensagens proativas exigem autorização do paciente e respeito às regras do provedor.
- **RN-15:** Utilizar somente dados fictícios; não registrar segredos ou dados sensíveis em logs.

## 6. Situações

Valores em português brasileiro:

- `AGUARDANDO_PRE_REQUISITO`: etapa bloqueada, como retorno antes da realização do exame e liberação profissional.
- `PENDENTE_AGENDAMENTO`: etapa liberada sem agendamento.
- `AGENDADO`: existe agendamento confirmado pelo backend.
- `CONCLUIDO`: atendimento realizado e confirmado pelo profissional.
- `NECESSITA_REAGENDAMENTO`: houve ausência; reagendar o mesmo procedimento desta etapa.
- `NECESSITA_ASSISTENCIA`: requer intervenção humana.
- `CANCELADO`: encaminhamento cancelado.

## 7. Modelo conceitual

- **Paciente:** `id`, `nome`, `telefone`, `autorizouContato`.
- **Procedimento:** `id`, `nome`, `tipo` (`EXAME` ou `CONSULTA`). Exemplos fictícios: hemograma, ultrassonografia e consulta de retorno.
- **Encaminhamento:** `id`, `pacienteId`, `procedimentoId`, `necessitaRetorno`, `encaminhamentoOrigemId` (nulo para origem), `situacao`, `dataCriacao`.
- **Agendamento:** `id`, `encaminhamentoId`, `dataHora`, `nomeUnidade` (fictício), `situacao`.

Opções de horário podem residir em configuração simples do backend; não criar tabelas de agenda, vagas ou unidade para administrá-las. A modelagem relacional deve manter integridade referencial e permitir histórico suficiente para os fluxos. Adicionar atributos/entidades somente se necessários. Persistir o esquema no MySQL com migrations Flyway versionadas.

## 8. Persistência e configuração

- Banco oficial: MySQL 8.0+; não usar H2 como banco do MVP.
- Conexão por `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- Flyway é a única ferramenta de criação/alteração do esquema; migrations em `src/main/resources/db/migration/` (`V1__...sql`, `V2__...sql`). Não editar migration já aplicada em ambiente compartilhado.
- Hibernate usa `spring.jpa.hibernate.ddl-auto=validate`; não usar `update`, `create` ou `create-drop`.
- Agendamentos e estados devem sobreviver à reinicialização da aplicação.
- Dados de demonstração, se necessários, devem ter carregamento simples e reproduzível, separado das migrations obrigatórias e usando apenas dados sintéticos.

## 9. Interfaces previstas

1. Painel de acompanhamento de pendências e assistência humana.
2. Lista/formulário simples de pacientes e encaminhamentos.
3. Chat demonstrativo para o fluxo do paciente.

Não criar interface administrativa para gerenciar unidades, agendas ou vagas.

## 10. Critérios de aceite

- **CA-01:** Profissional registra encaminhamento e vê pendência.
- **CA-02:** Contato é enviado/simulado somente conforme autorização.
- **CA-03:** Backend oferece opções fictícias por procedimento e registra a opção escolhida no MySQL.
- **CA-04:** Opção reservada não pode ser reservada novamente para o mesmo encaminhamento; seleção inválida é recusada.
- **CA-05:** Lembretes podem ser demonstrados e cessam quando deixam de ser aplicáveis.
- **CA-06:** Profissional confirma realização/ausência do exame.
- **CA-07:** Com retorno solicitado, retorno vinculado aguarda realização e liberação profissional.
- **CA-08:** Paciente agenda retorno liberado.
- **CA-09:** Ausência ao retorno permite remarcar apenas o retorno; exame não é reiniciado.
- **CA-10:** Sem retorno solicitado, exame confirmado encerra o encaminhamento.
- **CA-11:** Confirmação de retorno conclui o ciclo.
- **CA-12:** Pedido de ajuda aparece como pendência humana.
- **CA-13:** Reiniciar aplicação mantém agendamentos e situações no MySQL.

## 11. Fora do escopo

- Gestão completa de agendas ou disponibilidade real.
- CRUD de unidades, capacidade avançada e administração de vagas.
- Integrações com sistemas hospitalares, SUS ou SISREG.
- Integração WhatsApp obrigatória ou infraestrutura complexa de mensageria.
- Prontuário completo, diagnóstico, prescrição ou interpretação clínica de exames.
- Convênios, faturamento, pagamentos, autenticação/autorização completas.
- Integrações reais com laboratórios ou processamento de resultados.
- Microsserviços, agentes autônomos complexos e áudio obrigatório.

## 12. Restrições técnicas

- Backend Java 17 + Spring Boot; frontend React + TypeScript.
- Spring Data JPA/Hibernate e JUnit; persistência em MySQL 8.0+.
- Credenciais somente por variáveis de ambiente.
- Usar dados sintéticos e identificar como simuladas disponibilidades e integrações não reais.
- Priorizar fluxo completo de continuidade do cuidado.

## 13. Acesso profissional

O login exige uma conta ativa com perfil PROFESSIONAL e vínculo ativo com uma instituição ativa. O backend revalida essas condições nas APIs protegidas. A sessão é encerrada no logout ou após 30 minutos de inatividade.
