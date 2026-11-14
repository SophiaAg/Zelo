"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { consultarSessao, enviarAuth } from "@/lib/auth";

type Paciente = { id: number; nome: string; telefone: string; ativo: boolean };
type Atendimento = {
  id: number; pacienteId: number; paciente: string; procedimento: string; tipo: string;
  dataHora: string; unidade: string; instituicao: string; responsavel: string | null;
  situacao: string; situacaoEncaminhamento: string; registradoPor: string | null;
  registradoEm: string | null; observacoes: string | null;
};
type Resumo = { hoje: number; pendentes: number; faltas: number };
const status: Record<string, string> = {
  AGENDADO: "Agendado", CONCLUIDO: "Realizado", AUSENTE: "Falta registrada", CANCELADO: "Cancelado",
};
const tipos: Record<string, string> = { CONSULTA: "Consulta", EXAME: "Exame", RETORNO: "Retorno" };
const cartao = "rounded-xl border bg-card p-4";
const botao = "rounded-md border px-4 py-2 hover:bg-muted disabled:opacity-60";

function horario(data: string) {
  const [dia, hora] = data.split("T");
  return dia.split("-").reverse().join("/") + " às " + hora.slice(0, 5);
}

async function api<T>(caminho: string, signal?: AbortSignal, corpo?: object): Promise<T> {
  const headers: Record<string, string> = {};
  if (corpo) {
    const csrf = await fetch("/api/auth/csrf", { cache: "no-store", signal });
    if (!csrf.ok) throw new Error("Não foi possível iniciar a operação.");
    const { headerName, token } = await csrf.json();
    headers[headerName] = token;
    headers["Content-Type"] = "application/json";
  }
  const resposta = await fetch("/api" + caminho, {
    cache: "no-store", signal, headers, method: corpo ? "POST" : "GET",
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  if (resposta.status === 401) window.location.replace("/login");
  if (resposta.status === 403) throw new Error("Acesso negado. Verifique sua sessão.");
  if (resposta.status === 404) throw new Error("Registro indisponível.");
  if (resposta.status === 409) throw new Error("O atendimento já foi atualizado ou ainda não pode ser registrado.");
  if (!resposta.ok) throw new Error("Não foi possível concluir a solicitação. Tente novamente.");
  return resposta.status === 204 ? undefined as T : resposta.json();
}

function useDados<T>(caminho: string, versao = 0) {
  const [resultado, setResultado] = useState<{ caminho: string; versao: number; dados?: T; erro?: string }>();
  useEffect(() => {
    const controle = new AbortController();
    api<T>(caminho, controle.signal)
      .then((dados) => { if (!controle.signal.aborted) setResultado({ caminho, versao, dados }); })
      .catch((erro: Error) => { if (!controle.signal.aborted) setResultado({ caminho, versao, erro: erro.message }); });
    return () => controle.abort();
  }, [caminho, versao]);
  return resultado?.caminho === caminho && resultado.versao === versao ? resultado : undefined;
}

function Dados<T>({ resultado, children }: {
  resultado?: { dados?: T; erro?: string }; children: (dados: T) => ReactNode;
}) {
  if (!resultado) return <p role="status">Carregando…</p>;
  if (resultado.erro) return <p role="alert">{resultado.erro}</p>;
  return resultado.dados === undefined ? null : children(resultado.dados);
}

export function Profissional({ tela, id = "" }: { tela: "dashboard" | "pacientes" | "paciente"; id?: string }) {
  const [nome, setNome] = useState("");
  const [erro, setErro] = useState("");
  const [saindo, setSaindo] = useState(false);

  useEffect(() => {
    const controle = new AbortController();
    async function verificar() {
      try {
        const sessao = await consultarSessao(controle.signal);
        if (controle.signal.aborted) return;
        if (!sessao) window.location.replace("/login");
        setNome(sessao?.name ?? "");
        setErro("");
      } catch {
        if (!controle.signal.aborted) setErro("Não foi possível verificar a sessão.");
      }
    }
    void verificar();
    window.addEventListener("focus", verificar);
    return () => { controle.abort(); window.removeEventListener("focus", verificar); };
  }, []);

  async function sair() {
    setSaindo(true);
    try {
      const resposta = await enviarAuth("logout");
      if (resposta.status !== 204 && resposta.status !== 401) throw new Error();
      window.location.replace("/login");
    } catch {
      setErro("Não foi possível sair. Tente novamente.");
      setSaindo(false);
    }
  }

  return <div className="min-h-screen md:grid md:grid-cols-[14rem_1fr]">
    {nome && <aside className="flex flex-wrap items-center gap-4 border-b bg-card p-5 md:flex-col md:items-stretch md:border-r">
      <strong className="text-3xl">Zelo.</strong>
      <nav className="flex flex-wrap gap-2 md:flex-col" aria-label="Módulo profissional">
        <Link className={botao} href="/dashboard" aria-current={tela === "dashboard" ? "page" : undefined}>Dashboard</Link>
        <Link className={botao} href="/patients" aria-current={tela !== "dashboard" ? "page" : undefined}>Pacientes</Link>
      </nav>
      <button className={botao} onClick={sair} disabled={saindo}>{saindo ? "Saindo…" : "Sair"}</button>
    </aside>}
    <main className="mx-auto w-full max-w-6xl space-y-5 p-5 md:p-10">
      {erro && <p role="alert">{erro}</p>}
      {!nome && !erro && <p role="status">Verificando sessão…</p>}
      {nome && (tela === "dashboard" ? <Painel nome={nome} /> :
        tela === "pacientes" ? <Pacientes /> : <Historico id={id} />)}
    </main>
  </div>;
}

function Painel({ nome }: { nome: string }) {
  const [versao, setVersao] = useState(0);
  const resumo = useDados<Resumo>("/dashboard/summary", versao);
  return <>
    <h1 className="text-3xl font-bold">Dashboard</h1>
    <p>Olá, {nome}.</p>
    <Dados resultado={resumo}>{(dados) => <dl className="grid gap-3 sm:grid-cols-3">
      <div className={cartao}><dt>Agendados para hoje</dt><dd className="text-3xl font-bold">{dados.hoje}</dd></div>
      <div className={cartao}><dt>Aguardando confirmação até hoje</dt><dd className="text-3xl font-bold">{dados.pendentes}</dd></div>
      <div className={cartao}><dt>Faltas registradas</dt><dd className="text-3xl font-bold">{dados.faltas}</dd></div>
    </dl>}</Dados>
    <h2 className="text-xl font-semibold">Próximos atendimentos</h2>
    <Lista caminho="/appointments" versao={versao} mostrarPaciente atualizar={() => setVersao((v) => v + 1)} />
  </>;
}

function Pacientes() {
  const [busca, setBusca] = useState("");
  const [filtro, setFiltro] = useState("");
  const resultado = useDados<Paciente[]>(`/patients?search=${encodeURIComponent(filtro)}`);
  return <>
    <h1 className="text-3xl font-bold">Pacientes</h1>
    <form className="flex flex-wrap items-end gap-3" onSubmit={(evento) => { evento.preventDefault(); setFiltro(busca.trim()); }}>
      <label>Nome ou identificador<input type="search" className="block rounded-md border p-2"
        maxLength={160} value={busca} onChange={(evento) => setBusca(evento.target.value)} /></label>
      <button className={botao}>Buscar</button>
    </form>
    <Dados resultado={resultado}>{(itens) => itens.length === 0 ? <p>Nenhum paciente encontrado.</p> :
      <ul className="space-y-3">{itens.map((p) => <li className={cartao + " flex flex-wrap items-center justify-between gap-3"} key={p.id}>
        <div><strong>{p.nome}</strong><p>#{p.id} · {p.telefone}</p></div>
        <span>{p.ativo ? "Acompanhamento ativo" : "Sem acompanhamento ativo"}</span>
        <Link className={botao} href={`/patients/${p.id}`}>Ver detalhes</Link>
      </li>)}</ul>}</Dados>
  </>;
}

function Historico({ id }: { id: string }) {
  const [versao, setVersao] = useState(0);
  const caminho = `/patients/${encodeURIComponent(id)}`;
  const paciente = useDados<Paciente>(caminho, versao);
  return <>
    <Link className="underline" href="/patients">Voltar para pacientes</Link>
    <Dados resultado={paciente}>{(p) => <>
      <h1 className="text-3xl font-bold">{p.nome}</h1>
      <p>#{p.id} · {p.telefone} · {p.ativo ? "Acompanhamento ativo" : "Sem acompanhamento ativo"}</p>
    </>}</Dados>
    <h2 className="text-xl font-semibold">Consultas, exames e retornos</h2>
    <Lista caminho={caminho + "/appointments"} versao={versao} atualizar={() => setVersao((v) => v + 1)} />
  </>;
}

function Lista({ caminho, versao, mostrarPaciente, atualizar }: {
  caminho: string; versao: number; mostrarPaciente?: boolean; atualizar: () => void;
}) {
  const agenda = useDados<Atendimento[]>(caminho, versao);
  const [id, selecionar] = useState<number>();
  const [feedback, setFeedback] = useState("");
  return <>
    <p role="status">{feedback}</p>
    <Dados resultado={agenda}>{(itens) => itens.length === 0 ? <p>Nenhum atendimento encontrado.</p> :
      <ul className="space-y-3">{itens.map((a) => <li className={cartao + " flex flex-wrap items-center justify-between gap-3"} key={a.id}>
        <div>
          <strong>{a.procedimento}</strong>
          {mostrarPaciente && <p><Link className="underline" href={`/patients/${a.pacienteId}`}>{a.paciente}</Link></p>}
          <p>{tipos[a.tipo]} · {horario(a.dataHora)} · {a.instituicao}</p>
        </div>
        <span className="rounded-full bg-muted px-3 py-1 text-sm">{status[a.situacao]}</span>
        <button className={botao} onClick={() => selecionar(a.id)}>Abrir atendimento</button>
      </li>)}</ul>}</Dados>
    {id !== undefined && <Modal id={id} versao={versao} fechar={() => selecionar(undefined)}
      aoSalvar={(mensagem) => { setFeedback(mensagem); atualizar(); }} />}
  </>;
}

function Modal({ id, versao, fechar, aoSalvar }: {
  id: number; versao: number; fechar: () => void; aoSalvar: (mensagem: string) => void;
}) {
  const dialogo = useRef<HTMLDialogElement>(null);
  const resultado = useDados<Atendimento>(`/appointments/${id}`, versao);
  const [decisao, escolher] = useState<"complete" | "no-show">();
  const [observacoes, editar] = useState("");
  const [confirma, confirmar] = useState(false);
  const [ocupado, ocupar] = useState(false);
  const [erro, setErro] = useState("");
  const [mensagem, setMensagem] = useState("");
  useEffect(() => { dialogo.current?.showModal(); }, []);

  async function salvar() {
    if (!decisao || ocupado) return;
    ocupar(true);
    setErro("");
    try {
      await api<void>(`/appointments/${id}/${decisao}`, undefined,
        decisao === "complete" ? { observacoes: observacoes.trim() || null } : {});
      const aviso = decisao === "complete" ? "Atendimento confirmado." : "Falta registrada.";
      setMensagem(aviso);
      aoSalvar(aviso);
    } catch (falha) {
      setErro((falha as Error).message);
    } finally {
      ocupar(false);
      confirmar(false);
    }
  }

  const agora = new Intl.DateTimeFormat("sv-SE", {
    timeZone: "America/Sao_Paulo", dateStyle: "short", timeStyle: "medium",
  }).format(new Date()).replace(" ", "T");

  return <dialog className="m-auto max-h-[90vh] w-[min(35rem,92vw)] overflow-auto rounded-xl border bg-card p-6 text-foreground backdrop:bg-black/40"
    ref={dialogo} aria-labelledby="titulo-atendimento"
    onCancel={(evento) => { evento.preventDefault(); if (!ocupado) fechar(); }}>
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
      <h2 className="text-xl font-semibold" id="titulo-atendimento">Detalhes do atendimento</h2>
      <button className={botao} disabled={ocupado} onClick={fechar}>Fechar</button>
    </div>
    {erro && <p role="alert">{erro}</p>}
    <p role="status">{mensagem}</p>
    <Dados resultado={resultado}>{(a) => <>
      <p className="font-semibold">{a.procedimento} · {tipos[a.tipo]}</p>
      <dl className="my-4 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2">
        <dt>Paciente</dt><dd>{a.paciente}</dd>
        <dt>Data e horário</dt><dd>{horario(a.dataHora)}</dd>
        <dt>Local</dt><dd>{a.instituicao} · {a.unidade}</dd>
        <dt>Responsável</dt><dd>{a.responsavel ?? "Não informado"}</dd>
        <dt>Situação</dt><dd>{status[a.situacao]}</dd>
        {a.registradoEm && <><dt>Registrado por</dt><dd>{a.registradoPor} · {horario(a.registradoEm)}</dd></>}
        {a.observacoes && <><dt>Observações</dt><dd className="whitespace-pre-wrap break-words">{a.observacoes}</dd></>}
      </dl>
      {!mensagem && a.situacao === "AGENDADO" && a.situacaoEncaminhamento === "AGENDADO" && a.dataHora <= agora &&
        <fieldset className="space-y-3" disabled={ocupado}>
          <legend className="font-semibold">Paciente compareceu?</legend>
          <label className="mr-4"><input type="radio" name="compareceu" disabled={confirma}
            checked={decisao === "complete"} onChange={() => escolher("complete")} /> Sim</label>
          <label><input type="radio" name="compareceu" disabled={confirma}
            checked={decisao === "no-show"} onChange={() => escolher("no-show")} /> Não</label>
          {decisao === "complete" && <label className="block">Observações (opcional)
            <textarea className="block w-full rounded-md border p-2" maxLength={2000}
              disabled={confirma} value={observacoes} onChange={(evento) => editar(evento.target.value)} />
          </label>}
          {decisao && !confirma && <div><button className={botao} onClick={() => confirmar(true)}>
            {decisao === "complete" ? "Confirmar atendimento" : "Registrar falta"}
          </button></div>}
          {confirma && <div className="space-y-2">
            <p>{decisao === "complete" ? "Finalizar este atendimento?" : "Confirmar a falta neste atendimento?"}</p>
            <button className={botao} onClick={salvar}>{ocupado ? "Salvando…" : "Confirmar registro"}</button>{" "}
            <button className={botao} onClick={() => confirmar(false)}>Voltar</button>
          </div>}
        </fieldset>}
    </>}</Dados>
  </dialog>;
}

