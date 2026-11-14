import Portal from "./portal";

export function generateStaticParams() {
  return ["login", "cadastro", "dashboard"].map((pagina) => ({ pagina }));
}

export default async function Page({ params }: { params: Promise<{ pagina: string }> }) {
  const { pagina } = await params;
  return <Portal pagina={pagina} />;
}
