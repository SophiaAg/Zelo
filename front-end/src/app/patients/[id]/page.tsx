import { Profissional } from "@/components/profissional";
export default async function Paciente({ params }: { params: Promise<{ id: string }> }) {
  return <Profissional tela="paciente" id={(await params).id} />;
}
