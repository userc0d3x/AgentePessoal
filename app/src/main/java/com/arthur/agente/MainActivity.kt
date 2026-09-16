package com.arthur.agente

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.arthur.agente.suporte.AnaliseTicket
import com.arthur.agente.suporte.BaseConhecimento
import com.arthur.agente.suporte.NivelRisco
import com.arthur.agente.suporte.OrquestradorSuporte

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AgentePessoalApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentePessoalApp() {
    val contexto = LocalContext.current
    val orquestrador = remember { OrquestradorSuporte() }
    var ticket by remember { mutableStateOf("") }
    var resposta by remember { mutableStateOf("") }
    var analise by remember { mutableStateOf<AnaliseTicket?>(null) }
    var mostrarBase by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agente Pessoal") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Análise de tickets", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Cole um ticket abaixo. O agente classifica o caso, estima risco e confiança e prepara uma resposta para você revisar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                OutlinedTextField(
                    value = ticket,
                    onValueChange = { ticket = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    label = { Text("Ticket") },
                    placeholder = { Text("Cole aqui a mensagem do usuário...") }
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val resultado = orquestrador.processar(ticket)
                            analise = resultado
                            resposta = resultado.respostaSugerida
                        },
                        enabled = ticket.isNotBlank()
                    ) { Text("Analisar") }

                    OutlinedButton(onClick = {
                        ticket = ""
                        resposta = ""
                        analise = null
                    }) { Text("Limpar") }
                }
            }

            analise?.let { resultado ->
                item {
                    HorizontalDivider()
                    Text("Resultado", style = MaterialTheme.typography.titleLarge)
                }
                item { InfoLinha("Categoria", resultado.categoria) }
                item { InfoLinha("Risco", resultado.risco.nomeExibicao()) }
                item { InfoLinha("Confiança", "${resultado.confianca}%") }
                item { Text(resultado.justificativa, style = MaterialTheme.typography.bodyMedium) }

                item {
                    OutlinedTextField(
                        value = resposta,
                        onValueChange = { resposta = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
                        label = { Text("Resposta sugerida — edite antes de enviar") }
                    )
                }

                item {
                    Button(
                        onClick = {
                            val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Resposta do Agente Pessoal", resposta))
                            Toast.makeText(contexto, "Resposta copiada", Toast.LENGTH_SHORT).show()
                        },
                        enabled = resposta.isNotBlank()
                    ) { Text("Copiar resposta") }
                }
            }

            item {
                HorizontalDivider()
                TextButton(onClick = { mostrarBase = !mostrarBase }) {
                    Text(if (mostrarBase) "Ocultar base de conhecimento" else "Ver base de conhecimento")
                }
            }

            if (mostrarBase) {
                items(BaseConhecimento.artigos) { artigo ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(artigo.titulo, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(artigo.conteudo, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Text(
                    "Integrações: o modelo foi separado da interface e o canal DISCORD_FUTURO já existe apenas como contrato. Nenhuma automação do Discord foi implementada nesta etapa.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun InfoLinha(rotulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(rotulo, style = MaterialTheme.typography.labelLarge)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun NivelRisco.nomeExibicao(): String = when (this) {
    NivelRisco.BAIXO -> "Baixo"
    NivelRisco.MEDIO -> "Médio"
    NivelRisco.ALTO -> "Alto"
}
