const chave = "zelo-demonstracao-atendimentos";
export const demonstracao = process.env.NEXT_PUBLIC_ZELO_DEMO === "true";
export const usuarioDemo = { id: 1, name: "Profissional de demonstração", email: "profissional@example.test", role: "PROFESSIONAL" };

const pacientes = [
  { id: 1, nome: "Marina de Demonstração", telefone: "DEMO-001" },
  { id: 2, nome: "Rafael de Demonstração", telefone: "DEMO-002" },
  { id: 3, nome: "Lúcia de Demonstração", telefone: "DEMO-003" },
];

function agora() {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Sao_Paulo",
    dateStyle: "short", timeStyle: "medium" }).format(new Date()).replace(" ", "T");
}

function iniciais() {
  const hoje = agora().slice(0, 10);
  return pacientes.map((p, i) => ({
    id: p.id, pacienteId: p.id, paciente: p.nome, procedimento: ["Consulta de acompanhamento", "Hemograma", "Consulta de retorno"][i],
    tipo: ["CONSULTA", "EXAME", "RETORNO"][i], dataHora: hoje + "T00:00:00",
    unidade: "Unidade demonstrativa", instituicao: "Zelo Demonstração", responsavel: usuarioDemo.name,
    situacao: i === 2 ? "AUSENTE" : "AGENDADO",
    situacaoEncaminhamento: i === 2 ? "NECESSITA_REAGENDAMENTO" : "AGENDADO",
    registradoPor: null as string | null, registradoEm: null as string | null, observacoes: null as string | null,
  }));
}

export function apiDemo(caminho: string, corpo?: object): unknown {
  const salvo = localStorage.getItem(chave);
  const itens: ReturnType<typeof iniciais> = salvo ? JSON.parse(salvo) : iniciais();
  const url = new URL(caminho, "http://demo.local");
  const [, recurso, id, acao] = url.pathname.split("/");
  const hoje = agora().slice(0, 10);
  if (caminho === "/dashboard/summary") {
    const agendados = itens.filter((a) => a.situacao === "AGENDADO");
    return { hoje: agendados.filter((a) => a.dataHora.startsWith(hoje)).length,
      pendentes: agendados.filter((a) => a.dataHora.slice(0, 10) <= hoje).length,
      faltas: itens.filter((a) => a.situacao === "AUSENTE").length };
  }
  if (recurso === "patients") {
    const dados = pacientes.map((p) => ({ ...p, ativo: itens.some((a) =>
      a.pacienteId === p.id && a.situacao !== "CONCLUIDO" && a.situacao !== "CANCELADO") }));
    if (!id) {
      const busca = (url.searchParams.get("search") ?? "").toLowerCase();
      return dados.filter((p) => p.nome.toLowerCase().includes(busca) || String(p.id) === busca);
    }
    const paciente = dados.find((p) => String(p.id) === id);
    if (!paciente) throw new Error("Paciente indisponível.");
    return acao === "appointments" ? itens.filter((a) => a.pacienteId === paciente.id) : paciente;
  }
  if (recurso === "appointments") {
    if (!id) {
      const dia = url.searchParams.get("date");
      return dia ? itens.filter((a) => a.dataHora.startsWith(dia)) :
        itens.filter((a) => a.situacao === "AGENDADO" && a.dataHora.slice(0, 10) >= hoje);
    }
    const item = itens.find((a) => String(a.id) === id);
    if (!item) throw new Error("Atendimento indisponível.");
    if (!corpo) return item;
    if (item.situacao !== "AGENDADO" || item.dataHora > agora()) {
      throw new Error("Este atendimento já foi atualizado ou ainda não está disponível.");
    }
    const compareceu = acao === "complete";
    item.situacao = compareceu ? "CONCLUIDO" : "AUSENTE";
    item.situacaoEncaminhamento = compareceu ? "CONCLUIDO" : "NECESSITA_REAGENDAMENTO";
    item.registradoPor = usuarioDemo.name;
    item.registradoEm = agora();
    item.observacoes = compareceu ? (corpo as { observacoes?: string }).observacoes ?? null : null;
    localStorage.setItem(chave, JSON.stringify(itens));
    return;
  }
  throw new Error("Operação indisponível na demonstração.");
}
