# Zelo.

**Zelo** é uma plataforma inteligente de continuidade do cuidado em saúde que conecta pacientes e instituições por meio de lembretes personalizados, acompanhamento de consultas, exames e retornos, utilizando IA e WhatsApp para reduzir faltas e facilitar o acesso à saúde.

## Login profissional local

Use JDK 17, Maven, MySQL 8.0+ e Node.js 20.9 ou mais recente. Crie um banco vazio de desenvolvimento e um usuário MySQL com permissão para executar as migrations Flyway. Aplique as migrations iniciando o backend.

No PowerShell, a partir da raiz do projeto:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/zelo?serverTimezone=UTC'
$env:DB_USERNAME = 'zelo'
$env:DB_PASSWORD = Read-Host 'Senha do MySQL' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:ZELO_DEMO_EMAIL = 'profissional@example.test'
$env:ZELO_DEMO_PASSWORD = Read-Host 'Senha sintética do profissional (mínimo de 12 caracteres)' -MaskInput
$env:SESSION_COOKIE_SECURE = 'false'
mvn -f backend/pom.xml spring-boot:run
```

O perfil `demo` cria uma conta sintética e seu vínculo ativo quando o e-mail ainda não existe. Em reinicializações, ele preserva a senha e o acesso já armazenados. Use somente o valor `false` para o cookie Secure no desenvolvimento via HTTP. Em HTTPS mantenha o padrão seguro e deixe o perfil `demo` desativado.

Em outro terminal:

```powershell
cd front-end
npm ci
npm run dev
```

Acesse [http://localhost:3000/login](http://localhost:3000/login) e entre com a conta demonstrativa. O perfil `demo` inclui dois pacientes sintéticos com atendimentos disponíveis para registro. Abra um atendimento no dashboard, confirme o comparecimento e acompanhe a atualização dos indicadores. Em **Pacientes**, busque outro paciente por nome, abra o histórico e registre a falta. Reabra os detalhes para conferir autoria e situação. Uma reinicialização mantém os registros. Se os casos foram usados em uma demonstração anterior, consulte-os pelo histórico ou inicie a demonstração em outro banco vazio.

As APIs de acompanhamento usam a mesma sessão. Os registros devolvidos são restritos a vínculos institucionais ativos. Encaminhamentos anteriores à V3 exigem atribuição explícita de instituição para aparecer no módulo.

O Next.js encaminha as APIs para `ZELO_API_URL` (padrão `http://localhost:8080`). O navegador usa a origem do frontend para o aplicativo e a API, com cookies de sessão enviados nessa mesma origem.

## API de autenticação

| Método e caminho | Contrato |
| --- | --- |
| `GET /api/auth/csrf` | Retorna `{ headerName, token }`. Envie o token no cabeçalho indicado nas requisições POST. |
| `POST /api/auth/login` | Recebe `{ email, password }` e devolve `{ id, name, email, role }` com 200. Retorna 400 para dados inválidos, 401 para credenciais inválidas, 403 para acesso negado e 429 para limite de tentativas. |
| `GET /api/auth/me` | Devolve o mesmo DTO do login. Retorna 401 sem sessão e 403 sem autorização. |
| `POST /api/auth/logout` | Encerra a sessão e devolve 204. Obtenha um token CSRF novo após o login. |

A sessão expira após 30 minutos de inatividade. O backend verifica conta, perfil e vínculo institucional em cada chamada protegida. O limite de tentativas funciona por minuto e por instância do backend. Atrás do proxy, o limite por endereço pode ser compartilhado pelos usuários atendidos pelo mesmo proxy.

## Testes

```powershell
mvn -f backend/pom.xml test
cd front-end
npm ci
npx playwright install chromium
npx playwright test
npm run lint
npm run build
```

Os testes MockMvc usam o Spring Security com um repositório simulado. Os testes Playwright executam a interface no navegador e simulam as respostas da API. O roteiro de login local conecta o frontend ao Spring Boot e ao MySQL.
