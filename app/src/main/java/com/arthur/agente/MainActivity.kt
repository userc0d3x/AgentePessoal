package com.arthur.agente

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.core.app.NotificationManagerCompat
import com.arthur.agente.suporte.AnaliseTicket
import com.arthur.agente.suporte.BaseConhecimento
import com.arthur.agente.suporte.NivelRisco
import com.arthur.agente.suporte.OrquestradorSuporte
import com.arthur.agente.whatsapp.ArmazenamentoWhatsApp
import com.arthur.agente.whatsapp.MensagemWhatsApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var mensagensWhatsApp by remember { mutableStateOf(ArmazenamentoWhatsApp.carregar(contexto)) }
    var whatsAtivo by remember { mutableStateOf(false) }

    fun atualizarWhatsApp() {
        mensagensWhatsApp = ArmazenamentoWhatsApp.carregar(contexto)
        whatsAtivo = NotificationManagerCompat.getEnabledListenerPackages(contexto)
            .contains(contexto.packageName)
    }

    LaunchedEffect(Unit) { atualizarWhatsApp() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agente Pessoal") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("WhatsApp", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (whatsAtivo) "Leitura de notificações ativa. O agente pode receber novas mensagens do WhatsApp."
                    else "Ative o acesso às notificações para o agente conseguir ler mensagens recebidas.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        contexto.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text(if (whatsAtivo) "Configurar acesso" else "Ativar acesso") }
                    OutlinedButton(onClick = { atualizarWhatsApp() }) { Text("Atualizar") }
                }
            }

            if (mensagensWhatsApp.isNotEmpty()) {
                item {
                    Text("Mensagens recebidas", style = MaterialTheme.typography.titleLarge)
                }
                items(mensagensWhatsApp.take(10)) { mensagem ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(mensagem.remetente, style = MaterialTheme.typography.titleMedium)
                            Text(mensagem.texto, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(mensagem.horario)),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(onClick = { ticket = mensagem.texto }) {
                                Text("Analisar esta mensagem")
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider()
                Text("Análise e resposta", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "O agente analisa a mensagem, estima risco e confiança e prepara uma resposta para você revisar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                OutlinedTextField(
                    value = ticket,
                    onValueChange = { ticket = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    label = { Text("Mensagem") },
                    placeholder = { Text("Cole uma mensagem do WhatsApp...") }
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
                        label = { Text("Resposta sugerida — revise antes de enviar") }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Resposta do Agente Pessoal", resposta))
                                Toast.makeText(contexto, "Resposta copiada", Toast.LENGTH_SHORT).show()
                            },
                            enabled = resposta.isNotBlank()
                        ) { Text("Copiar") }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, resposta)
                                    setPackage("com.whatsapp")
                                }
                                try {
                                    contexto.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(contexto, "WhatsApp não encontrado", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = resposta.isNotBlank()
                        ) { Text("Abrir no WhatsApp") }
                    }
                    Text(
                        "O envio continua sob seu controle: o agente prepara a resposta e o WhatsApp permite que você escolha a conversa e envie.",
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    "Integração atual: WhatsApp por notificações + preparação de resposta. Não há envio automático nem acesso às conversas internas do WhatsApp.",
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
