package com.arthur.agente.suporte

enum class CanalIntegracao {
    APLICATIVO,
    DISCORD_FUTURO
}

data class Ticket(
    val texto: String,
    val canal: CanalIntegracao = CanalIntegracao.APLICATIVO
)

data class ArtigoConhecimento(
    val titulo: String,
    val conteudo: String,
    val palavrasChave: List<String>
)

enum class NivelRisco {
    BAIXO,
    MEDIO,
    ALTO
}

data class AnaliseTicket(
    val categoria: String,
    val risco: NivelRisco,
    val confianca: Int,
    val justificativa: String,
    val respostaSugerida: String
)
