import { expect, test } from "@playwright/test";

test.beforeEach(async ({ page }) => {
  await page.route("**/api/auth/csrf", (route) =>
    route.fulfill({ json: { headerName: "X-CSRF-TOKEN", token: "teste" } }),
  );
});

test("valida campos obrigatórios e formato antes de enviar", async ({ page }) => {
  let envios = 0;
  await page.route("**/api/auth/login", (route) => {
    envios++;
    return route.fulfill({ status: 401 });
  });
  await page.route("**/api/auth/me", (route) => route.fulfill({ status: 401 }));
  await page.goto("/login");
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page.locator("#email:invalid")).toBeVisible();
  await page.getByLabel("E-mail").fill("invalido");
  await page.getByLabel("Senha", { exact: true }).fill("senha de teste");
  await page.getByLabel("Senha", { exact: true }).press("Enter");
  await expect(page.locator("#email:invalid")).toBeVisible();
  expect(envios).toBe(0);
});

test("mostra carregamento, visibilidade da senha e erro de autenticação", async ({ page }) => {
  await page.route("**/api/auth/me", (route) => route.fulfill({ status: 401 }));
  let liberar!: () => void;
  const solicitacao = new Promise<void>((resolve) => { liberar = resolve; });
  await page.route("**/api/auth/login", async (route) => {
    await solicitacao;
    await route.fulfill({ status: 401 });
  });
  await page.goto("/login");
  await page.getByLabel("E-mail").fill("profissional@example.test");
  await page.getByLabel("Senha", { exact: true }).fill("senha de teste");
  await page.getByRole("button", { name: "Mostrar senha" }).click();
  await expect(page.getByLabel("Senha", { exact: true })).toHaveAttribute("type", "text");
  await page.getByLabel("Senha", { exact: true }).press("Enter");
  try {
    await expect(page.getByRole("button", { name: "Entrando…" })).toBeDisabled();
  } finally {
    liberar();
  }
  await expect(page.getByRole("main").getByRole("alert")).toHaveText("Não foi possível entrar com os dados informados.");
  await expect(page).toHaveURL("/login");
});

test("protege dashboard, redireciona, restaura sessão após recarga e encerra acesso", async ({ page }) => {
  let autenticado = false;
  const usuario = { id: 1, name: "Profissional de Teste", email: "profissional@example.test", role: "PROFESSIONAL" };
  await page.route("**/api/auth/me", (route) =>
    autenticado ? route.fulfill({ json: usuario }) : route.fulfill({ status: 401 }),
  );
  await page.route("**/api/auth/login", async (route) => {
    expect(route.request().headers()["x-csrf-token"]).toBe("teste");
    autenticado = true;
    await route.fulfill({ json: usuario });
  });
  await page.route("**/api/auth/logout", async (route) => {
    autenticado = false;
    await route.fulfill({ status: 204 });
  });
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/login");
  await page.getByLabel("E-mail").fill(usuario.email);
  await page.getByLabel("Senha", { exact: true }).fill("senha de teste");
  await page.getByRole("button", { name: "Entrar", exact: true }).click();
  await expect(page).toHaveURL("/dashboard");
  await expect(page.getByText("Você entrou como Profissional de Teste.")).toBeVisible();
  await page.reload();
  await expect(page.getByRole("heading", { name: "Módulo profissional" })).toBeVisible();
  await page.goto("/login");
  await expect(page).toHaveURL("/dashboard");
  await page.getByRole("button", { name: "Sair" }).click();
  await expect(page).toHaveURL("/login");
  await page.goto("/dashboard");
  await expect(page).toHaveURL("/login");
});
