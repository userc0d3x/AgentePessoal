package com.arthur.agente.suporte

class OrquestradorSuporte(
    private val modelo: ModeloIA = ModeloSuporteLocal()
) {
    fun processar(texto: String, canal: CanalIntegracao = CanalIntegracao.APLICATIVO): AnaliseTicket {
        return modelo.analisar(Ticket(texto = texto, canal = canal))
    }
}
