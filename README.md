# Zelo.
**Zelo** é uma plataforma inteligente de continuidade do cuidado em saúde que conecta pacientes e instituições por meio de lembretes personalizados, acompanhamento de consultas, exames e retornos, utilizando IA e WhatsApp para reduzir faltas e facilitar o acesso à saúde.

## Executar localmente

Requisitos: Docker Desktop instalado e iniciado. O Compose inicia MySQL 8 e o backend Java 17; não é necessário instalar Java ou Maven na máquina.

1. Copie `.env.example` para `.env` e defina `MYSQL_ROOT_PASSWORD` com uma senha local. Não faça commit do `.env`.
2. Na pasta raiz do projeto, execute `docker compose up --build`.
3. A API ficará disponível em `http://localhost:8080`. O MySQL ficará disponível em `localhost:3307`.

O banco `zelo` é criado automaticamente pelo container MySQL. Na primeira inicialização, Flyway cria as tabelas a partir das migrations. Os dados persistem no volume Docker `zelo-mysql-data` entre reinicializações.
