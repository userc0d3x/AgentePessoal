package com.arthur.agente.suporte

interface ModeloIA {
    fun analisar(ticket: Ticket): AnaliseTicket
}

class ModeloSuporteLocal(
    private val base: List<ArtigoConhecimento> = BaseConhecimento.artigos
) : ModeloIA {

    override fun analisar(ticket: Ticket): AnaliseTicket {
        val texto = ticket.texto.trim()
        val normalizado = texto.lowercase()

        if (texto.isBlank()) {
            return AnaliseTicket(
                categoria = "Sem conteúdo",
                risco = NivelRisco.MEDIO,
                confianca = 0,
                justificativa = "O ticket está vazio. Não há informação suficiente para analisar.",
                respostaSugerida = "Preciso de mais informações para entender o problema e ajudar você."
            )
        }

        val correspondencias = base.map { artigo ->
            artigo to artigo.palavrasChave.count { palavra -> normalizado.contains(palavra) }
        }.filter { it.second > 0 }.sortedByDescending { it.second }

        val melhor = correspondencias.firstOrNull()
        val quantidadeSinaisDeRisco = listOf(
            "ameaça", "ameaça de", "fraude", "golpe", "invad", "senha", "pagamento", "cobrança",
            "dados pessoais", "urgente", "banimento", "banir", "legal", "jurídico"
        ).count { normalizado.contains(it) }

        val risco = when {
            quantidadeSinaisDeRisco >= 2 -> NivelRisco.ALTO
            quantidadeSinaisDeRisco == 1 -> NivelRisco.MEDIO
            else -> NivelRisco.BAIXO
        }

        if (melhor == null) {
            return AnaliseTicket(
                categoria = "Não classificado",
                risco = if (risco == NivelRisco.BAIXO) NivelRisco.MEDIO else risco,
                confianca = 25,
                justificativa = "Nenhum artigo da base de conhecimento correspondeu claramente ao ticket.",
                respostaSugerida = "Obrigado por entrar em contato. Vou analisar melhor o caso para orientar você corretamente."
            )
        }

        val pontuacao = melhor.second
        val confianca = (55 + pontuacao * 12 - (quantidadeSinaisDeRisco.coerceAtMost(2) * 5)).coerceIn(35, 95)
        val categoria = melhor.first.titulo
        val resposta = when (categoria) {
            "Acesso à conta" -> "Entendi. Vamos verificar o acesso à conta. Por favor, confirme o que acontece ao tentar entrar e, se aparecer, envie a mensagem de erro exibida."
            "Pagamento ou cobrança" -> "Entendi. Vamos verificar a questão de pagamento ou cobrança. Por segurança, não envie senhas, códigos de autenticação ou dados completos do cartão."
            "Problema técnico" -> "Entendi o problema técnico. Por favor, informe o que aconteceu, quando começou e, se houver, a mensagem de erro exibida."
            else -> "Obrigado pelas informações. Vou verificar o caso com base na orientação disponível e retornar com os próximos passos."
        }

        return AnaliseTicket(
            categoria = categoria,
            risco = risco,
            confianca = confianca,
            justificativa = "Correspondência encontrada na base de conhecimento: ${melhor.first.titulo}.",
            respostaSugerida = resposta
        )
    }
}

object BaseConhecimento {
    val artigos = listOf(
        ArtigoConhecimento(
            titulo = "Acesso à conta",
            conteudo = "Orientar o usuário a descrever o erro de acesso sem compartilhar senha ou códigos de autenticação.",
            palavrasChave = listOf("login", "entrar", "acesso", "conta", "senha", "não consigo entrar", "erro ao entrar")
        ),
        ArtigoConhecimento(
            titulo = "Pagamento ou cobrança",
            conteudo = "Solicitar informações sobre a cobrança sem pedir dados completos de cartão, senha ou códigos de autenticação.",
            palavrasChave = listOf("pagamento", "cobrança", "cobrado", "cartão", "pix", "valor", "fatura")
        ),
        ArtigoConhecimento(
            titulo = "Problema técnico",
            conteudo = "Coletar descrição do erro, momento em que começou e mensagem apresentada antes de encaminhar o caso.",
            palavrasChave = listOf("erro", "bug", "travou", "não funciona", "problema", "falha", "técnico", "app")
        )
    )
}
