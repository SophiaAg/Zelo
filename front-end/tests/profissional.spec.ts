import { expect, test } from "@playwright/test";

for (const compareceu of [true, false]) {
  test(compareceu ? "confirma comparecimento" : "registra falta", async ({ page }) => {
    let situacao = "AGENDADO";
    const paciente = { id: 3, nome: "Marina de Demonstração", telefone: "DEMO-001", ativo: true };
    const atendimento = () => ({
      id: 5, pacienteId: 3, paciente: paciente.nome, procedimento: "Hemograma", tipo: "EXAME",
      dataHora: "2020-02-01T10:00:00", unidade: "Unidade demonstrativa", instituicao: "Zelo",
      responsavel: "Profissional de Teste", situacao,
      situacaoEncaminhamento: situacao === "AUSENTE" ? "NECESSITA_REAGENDAMENTO" : situacao,
    });
    await page.route("**/api/auth/me", (route) => route.fulfill({ json: { name: "Profissional de Teste" } }));
    await page.route("**/api/auth/csrf", (route) => route.fulfill({ json: { headerName: "X-CSRF-TOKEN", token: "teste" } }));
    await page.route("**/api/dashboard/summary", (route) => route.fulfill({
      json: { hoje: 0, pendentes: situacao === "AGENDADO" ? 1 : 0, faltas: situacao === "AUSENTE" ? 1 : 0 },
    }));
    await page.route("**/api/appointments", (route) => route.fulfill({ json: [] }));
    await page.route("**/api/patients?search=*", (route) => {
      const busca = new URL(route.request().url()).searchParams.get("search") ?? "";
      return route.fulfill({ json: paciente.nome.toLowerCase().includes(busca) ? [paciente] : [] });
    });
    await page.route("**/api/patients/3", (route) => route.fulfill({ json: paciente }));
    await page.route("**/api/patients/3/appointments", (route) => route.fulfill({ json: [atendimento()] }));
    await page.route("**/api/appointments/5", (route) => route.fulfill({ json: atendimento() }));
    await page.route(`**/api/appointments/5/${compareceu ? "complete" : "no-show"}`, (route) => {
      expect(route.request().method()).toBe("POST");
      expect(route.request().headers()["x-csrf-token"]).toBe("teste");
      situacao = compareceu ? "CONCLUIDO" : "AUSENTE";
      return route.fulfill({ status: 204 });
    });

    await page.goto("/dashboard");
    await expect(page.getByText("Aguardando confirmação até hoje").locator("..")).toContainText("1");
    await page.getByRole("link", { name: "Pacientes", exact: true }).click();
    await page.getByLabel("Nome ou identificador").fill("mari");
    await page.getByRole("button", { name: "Buscar" }).click();
    await page.getByRole("link", { name: "Ver detalhes" }).click();
    await page.getByRole("button", { name: "Abrir atendimento" }).click();
    const modal = page.getByRole("dialog");
    await modal.getByRole("radio", { name: compareceu ? "Sim" : "Não", exact: true }).check();
    await modal.getByRole("button", { name: compareceu ? "Confirmar atendimento" : "Registrar falta" }).click();
    await modal.getByRole("button", { name: "Confirmar registro" }).click();
    await expect(modal.getByText(compareceu ? "Atendimento confirmado." : "Falta registrada.", { exact: true })).toBeVisible();
    await expect(modal.getByText(compareceu ? "Realizado" : "Falta registrada", { exact: true })).toBeVisible();
    await expect(modal.getByRole("radio")).toHaveCount(0);
    await modal.getByRole("button", { name: "Fechar" }).click();
    await page.getByRole("link", { name: "Dashboard", exact: true }).click();
    await expect(page.getByText("Aguardando confirmação até hoje").locator("..")).toContainText("0");
    await expect(page.getByText("Faltas registradas").locator("..")).toContainText(compareceu ? "0" : "1");
  });
}
