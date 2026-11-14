import { demonstracao, usuarioDemo } from "./demonstracao";

export type Usuario = { id: number; name: string; email: string; role: string };

export async function consultarSessao(signal?: AbortSignal): Promise<Usuario | null> {
  if (demonstracao) return sessionStorage.getItem("zelo-demo-saiu") ? null : usuarioDemo;
  const resposta = await fetch("/api/auth/me", { cache: "no-store", signal });
  if (resposta.status === 401 || resposta.status === 403) return null;
  if (!resposta.ok) throw new Error("Não foi possível consultar a sessão.");
  return resposta.json();
}

export async function enviarAuth(acao: "login" | "logout", dados?: { email: string; password: string }) {
  if (demonstracao) {
    if (acao === "login") sessionStorage.removeItem("zelo-demo-saiu");
    else sessionStorage.setItem("zelo-demo-saiu", "1");
    return new Response(null, { status: 204 });
  }
  const token = await fetch("/api/auth/csrf", { cache: "no-store" });
  if (!token.ok) throw new Error("Não foi possível iniciar a solicitação.");
  const { headerName, token: valor } = await token.json();
  return fetch(`/api/auth/${acao}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", [headerName]: valor },
    body: dados ? JSON.stringify(dados) : undefined,
  });
}
