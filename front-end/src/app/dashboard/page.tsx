"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { consultarSessao, enviarAuth, type Usuario } from "@/lib/auth";

export default function Dashboard() {
  const router = useRouter();
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [erro, setErro] = useState("");
  const [saindo, setSaindo] = useState(false);

  useEffect(() => {
    const controle = new AbortController();
    async function verificar() {
      try {
        const sessao = await consultarSessao(controle.signal);
        if (controle.signal.aborted) return;
        setUsuario(sessao);
        setErro("");
        if (!sessao) router.replace("/login");
      } catch {
        if (controle.signal.aborted) return;
        setUsuario(null);
        setErro("Não foi possível verificar a sessão. Recarregue a página.");
      }
    }
    void verificar();
    window.addEventListener("focus", verificar);
    return () => {
      controle.abort();
      window.removeEventListener("focus", verificar);
    };
  }, [router]);

  async function sair() {
    setSaindo(true);
    setErro("");
    try {
      const resposta = await enviarAuth("logout");
      if (resposta.status !== 204 && resposta.status !== 401) throw new Error();
      setUsuario(null);
      router.replace("/login");
    } catch {
      setErro("Não foi possível sair. Tente novamente.");
      setSaindo(false);
    }
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      {erro && <p role="alert" className="mb-4 text-red-800">{erro}</p>}
      {!usuario && !erro && <p role="status">Verificando sessão…</p>}
      {usuario && (
        <section className="rounded-xl border bg-card p-6">
          <h1 className="text-2xl font-bold">Módulo profissional</h1>
          <p className="mt-2">Você entrou como {usuario.name}.</p>
          <button type="button" onClick={sair} disabled={saindo}
            className="mt-6 rounded-md border px-4 py-2 focus-visible:ring-2 focus-visible:ring-ring disabled:opacity-70">
            {saindo ? "Saindo…" : "Sair"}
          </button>
        </section>
      )}
    </main>
  );
}
