"use client";

import { Eye, EyeOff, HeartPulse, LoaderCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useRef, useState } from "react";
import { consultarSessao, enviarAuth } from "@/lib/auth";

export default function Login() {
  const router = useRouter();
  const [verificando, setVerificando] = useState(true);
  const [visivel, setVisivel] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const emEnvio = useRef(false);

  useEffect(() => {
    const controle = new AbortController();
    consultarSessao(controle.signal)
      .then((usuario) => {
        if (controle.signal.aborted) return;
        if (usuario) router.replace("/dashboard");
        else setVerificando(false);
      })
      .catch(() => {
        if (controle.signal.aborted) return;
        setErro("Não foi possível verificar sua sessão.");
        setVerificando(false);
      });
    return () => controle.abort();
  }, [router]);

  async function entrar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (emEnvio.current) return;
    const dados = new FormData(evento.currentTarget);
    emEnvio.current = true;
    setEnviando(true);
    setErro("");
    try {
      const resposta = await enviarAuth("login", {
        email: String(dados.get("email")).trim(),
        password: String(dados.get("password")),
      });
      if (resposta.ok) {
        router.replace("/dashboard");
        return;
      }
      if (resposta.status === 400) setErro("Verifique o e-mail e a senha.");
      else if (resposta.status === 429) setErro("Muitas tentativas. Aguarde um minuto e tente novamente.");
      else if (resposta.status === 401 || resposta.status === 403) {
        setErro("Não foi possível entrar com os dados informados.");
      } else throw new Error();
    } catch {
      setErro("Não foi possível conectar ao Zelo. Tente novamente.");
    }
    emEnvio.current = false;
    setEnviando(false);
  }

  const campo = "w-full rounded-md border bg-background px-3.5 py-3 text-sm outline-none focus-visible:ring-2 focus-visible:ring-ring";

  return (
    <main className="grid min-h-dvh place-items-center bg-muted px-4 py-8">
      <section className="w-full max-w-md rounded-xl border bg-card p-6 shadow-sm sm:p-9" aria-labelledby="titulo">
        <div className="mb-8 flex items-center gap-2.5 text-xl font-bold tracking-tight" aria-label="Zelo">
          <span className="grid size-9 place-items-center rounded-md bg-primary text-primary-foreground">
            <HeartPulse aria-hidden="true" size={21} />
          </span>
          Zelo
        </div>
        <h1 id="titulo" className="text-3xl font-bold tracking-tight">Boas-vindas ao Zelo</h1>
        <p className="mt-2 text-sm text-neutral-600">Entre para acessar o módulo profissional.</p>

        {verificando ? <p role="status" className="mt-7 text-sm">Verificando sessão…</p> : (
          <form onSubmit={entrar} className="mt-7 space-y-5" aria-busy={enviando}>
            <div className="space-y-2">
              <label className="block text-sm font-medium" htmlFor="email">E-mail</label>
              <input className={campo} id="email" name="email" type="email" autoComplete="username"
                maxLength={254} required />
            </div>
            <div className="space-y-2">
              <label className="block text-sm font-medium" htmlFor="password">Senha</label>
              <div className="relative">
                <input className={`${campo} pr-12`} id="password" name="password"
                  type={visivel ? "text" : "password"} autoComplete="current-password"
                  maxLength={72} required />
                <button type="button" className="absolute inset-y-1 right-1 grid w-10 place-items-center rounded-md focus-visible:ring-2 focus-visible:ring-ring"
                  aria-label={visivel ? "Ocultar senha" : "Mostrar senha"} onClick={() => setVisivel(!visivel)}>
                  {visivel ? <EyeOff size={18} aria-hidden="true" /> : <Eye size={18} aria-hidden="true" />}
                </button>
              </div>
            </div>
            {erro && <p role="alert" className="rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">{erro}</p>}
            <button type="submit" disabled={enviando}
              className="flex min-h-11 w-full items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-70">
              {enviando && <LoaderCircle className="motion-safe:animate-spin" size={16} aria-hidden="true" />}
              {enviando ? "Entrando…" : "Entrar"}
            </button>
          </form>
        )}
      </section>
    </main>
  );
}
