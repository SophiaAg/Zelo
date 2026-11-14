# Zelo — Fluxos do sistema

Os estados e registros de negócio descritos abaixo são persistidos no MySQL. As opções de data e horário são fictícias e vêm de configuração no backend, diferenciadas por procedimento; não há agenda real nem gestão de vagas. Consulte [`SPEC.md`](SPEC.md) para requisitos e regras completas.

## 1. Encaminhamento para consulta

```mermaid
flowchart TD
    A["Profissional registra encaminhamento de consulta"] --> B["Backend persiste encaminhamento pendente no MySQL"]
    B --> C["Assistente contata paciente conforme autorização"]
    C --> D{"Paciente pede opções?"}
    D -- "Não" --> E["Lembrete periódico ou pedido de ajuda"] --> D
    D -- "Sim" --> F["Backend fornece opções fictícias configuradas para consulta"]
    F --> G["Assistente apresenta opções"]
    G --> H["Paciente escolhe opção"]
    H --> I{"Backend valida opção e encaminhamento?"}
    I -- "Não" --> F
    I -- "Sim" --> J["Backend persiste agendamento e atualiza situação no MySQL"]
    J --> K["Assistente confirma agendamento"]
    K --> L["Lembretes antes do atendimento"]
    L --> M{"Paciente compareceu?"}
    M -- "Sim" --> N["Profissional confirma; encaminhamento CONCLUIDO"]
    M -- "Não" --> O["Profissional registra ausência; NECESSITA_REAGENDAMENTO"]
    O --> D
```

## 2. Encaminhamento para exame com retorno obrigatório

```mermaid
flowchart TD
    A["Profissional registra exame com necessitaRetorno=true"] --> B["Backend persiste encaminhamento do exame e retorno vinculado AGUARDANDO_PRE_REQUISITO"]
    B --> C["Assistente contata paciente conforme autorização"]
    C --> D{"Paciente pede opções para exame?"}
    D -- "Não" --> E["Lembrete periódico ou assistência humana"] --> D
    D -- "Sim" --> F["Backend fornece opções fictícias configuradas para exame"]
    F --> G["Paciente escolhe; backend valida e persiste agendamento do exame no MySQL"]
    G --> H["Assistente confirma e envia lembretes antes do exame"]
    H --> I{"Compareceu ao exame?"}
    I -- "Não" --> J["Profissional registra ausência do exame"] --> D
    I -- "Sim" --> K["Profissional confirma exame realizado e libera retorno"]
    K --> L["Backend atualiza retorno para PENDENTE_AGENDAMENTO no MySQL"]
    L --> M{"Paciente pede opções para retorno?"}
    M -- "Não" --> N["Lembrete periódico ou assistência humana"] --> M
    M -- "Sim" --> O["Backend fornece opções fictícias de consulta de retorno"]
    O --> P["Paciente escolhe; backend valida e persiste agendamento do retorno"]
    P --> Q["Assistente confirma e envia lembretes antes do retorno"]
    Q --> R{"Compareceu ao retorno?"}
    R -- "Não" --> S["Profissional registra ausência; reagendar somente retorno"] --> M
    R -- "Sim" --> T["Profissional confirma retorno; ciclo CONCLUIDO"]
```

**Regra:** falta ao retorno não reinicia nem exige repetir o exame. Apenas o encaminhamento do retorno volta à etapa de agendamento.

## 3. Exame sem retorno

```mermaid
flowchart TD
    A["Profissional registra exame com necessitaRetorno=false"] --> B["Backend persiste encaminhamento pendente no MySQL"]
    B --> C["Assistente contata paciente"]
    C --> D{"Paciente pede opções?"}
    D -- "Não" --> E["Lembrete periódico ou assistência humana"] --> D
    D -- "Sim" --> F["Backend fornece opções fictícias de exame"]
    F --> G["Paciente escolhe; backend valida e persiste agendamento no MySQL"]
    G --> H["Assistente confirma e lembra antes do exame"]
    H --> I{"Compareceu?"}
    I -- "Não" --> J["Profissional registra ausência; reagendar somente exame"] --> D
    I -- "Sim" --> K["Profissional confirma realização"]
    K --> L["Encaminhamento CONCLUIDO; não criar retorno"]
```

## 4. Situações e transições

```mermaid
stateDiagram-v2
    [*] --> PENDENTE_AGENDAMENTO: encaminhamento liberado
    [*] --> AGUARDANDO_PRE_REQUISITO: retorno vinculado bloqueado
    AGUARDANDO_PRE_REQUISITO --> PENDENTE_AGENDAMENTO: exame confirmado e retorno liberado
    PENDENTE_AGENDAMENTO --> AGENDADO: backend valida e persiste escolha
    AGENDADO --> CONCLUIDO: profissional confirma atendimento
    AGENDADO --> NECESSITA_REAGENDAMENTO: profissional registra ausência
    NECESSITA_REAGENDAMENTO --> PENDENTE_AGENDAMENTO: mesma etapa pode ser agendada novamente
    PENDENTE_AGENDAMENTO --> NECESSITA_ASSISTENCIA: paciente pede ajuda ou não há opções
    NECESSITA_ASSISTENCIA --> PENDENTE_AGENDAMENTO: equipe resolve e retoma fluxo
    PENDENTE_AGENDAMENTO --> CANCELADO: cancelamento autorizado
```

Para encaminhamentos com retorno, as transições se aplicam separadamente ao exame e ao retorno vinculado. A conclusão do exame nunca conclui o retorno.

## 5. Encaminhamento para atendimento humano

```mermaid
flowchart TD
    A["Paciente pede ajuda ou backend não tem opção adequada"] --> B["Assistente não inventa horário"]
    B --> C["Backend persiste situação NECESSITA_ASSISTENCIA no MySQL"]
    C --> D["Pendência aparece para equipe"]
    D --> E{"Equipe resolveu?"}
    E -- "Não" --> D
    E -- "Sim" --> F["Backend retoma etapa correta como PENDENTE_AGENDAMENTO"]
    F --> G["Assistente oferece opções fictícias válidas do backend"]
```

Se o atendimento humano ocorrer durante o fluxo de retorno, retomar o retorno; se ocorrer durante o exame, retomar o exame.

## 6. Lembretes e exceções

- Encaminhamento liberado sem agendamento: lembrete periódico a cada 15 dias, mediante autorização e respeito às regras do canal.
- Agendamento: lembretes 7 dias e 1 dia antes quando as datas forem futuras.
- Parar lembretes quando a etapa for agendada, concluída, cancelada ou mudar para uma etapa em que o lembrete deixou de ser aplicável.
- A demonstração pode disparar lembrete manual identificado como simulação; não requer infraestrutura complexa de mensageria.
- Sem opção configurada: informar que não há opção disponível e registrar assistência humana.
- Resposta ambígua: pedir esclarecimento; não registrar agendamento por suposição.
- Falha de canal: preservar encaminhamento e permitir demonstração do mesmo fluxo no chat.

## 7. Persistência

O backend registra encaminhamentos, agendamentos e mudanças de situação no MySQL 8.0+. A seleção de uma opção fictícia é validada e persistida pelo backend; o assistente só confirma após a resposta bem-sucedida. O esquema é evoluído por migrations Flyway em `src/main/resources/db/migration/`, e Hibernate valida o esquema. Opções mockadas permanecem configuração de backend, sem tabelas de agenda ou cadastro de unidade.

## 8. Login profissional

O profissional entra em /login com e-mail e senha. Após validar as credenciais, a conta, o perfil e o vínculo institucional, o backend cria a sessão e o frontend abre /dashboard. Ao recarregar, /api/auth/me revalida o acesso. Quando a sessão expira ou o profissional sai, a rota volta para /login.
