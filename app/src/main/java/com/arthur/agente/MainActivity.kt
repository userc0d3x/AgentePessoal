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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.arthur.agente.atualizacao.AtualizadorApp
import com.arthur.agente.suporte.AnaliseTicket
import com.arthur.agente.suporte.BaseConhecimento
import com.arthur.agente.suporte.NivelRisco
import com.arthur.agente.suporte.OrquestradorSuporte
import com.arthur.agente.whatsapp.ArmazenamentoWhatsApp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val escopo = rememberCoroutineScope()
    val listaState = rememberLazyListState()
    var ticket by remember { mutableStateOf("") }
    var resposta by remember { mutableStateOf("") }
    var analise by remember { mutableStateOf<AnaliseTicket?>(null) }
    var mostrarBase by remember { mutableStateOf(false) }
    var mensagensWhatsApp by remember { mutableStateOf(ArmazenamentoWhatsApp.carregar(contexto)) }
    var whatsAtivo by remember { mutableStateOf(false) }
    var atualizacao by remember { mutableStateOf<AtualizadorApp.Atualizacao?>(null) }
    var verificandoAtualizacao by remember { mutableStateOf(true) }
    var baixandoAtualizacao by remember { mutableStateOf(false) }

    fun atualizarWhatsApp() {
        mensagensWhatsApp = ArmazenamentoWhatsApp.carregar(contexto)
        whatsAtivo = NotificationManagerCompat.getEnabledListenerPackages(contexto)
            .contains(contexto.packageName)
    }

    fun selecionarMensagemParaAnalise(texto: String) {
        ticket = texto
        resposta = ""
        analise = null
        escopo.launch {
            // O índice 0 é o cabeçalho do WhatsApp; a área de análise fica depois das mensagens.
            listaState.animateScrollToItem(0)
            delay(50)
            val indiceAnalise = mensagensWhatsApp.take(10).size + 1
            listaState.animateScrollToItem(indiceAnalise)
        }
    }

    LaunchedEffect(Unit) {
        atualizarWhatsApp()
        while (true) {
            delay(700)
            atualizarWhatsApp()
        }
    }

    LaunchedEffect(Unit) {
        verificandoAtualizacao = true
        atualizacao = withContext(Dispatchers.IO) { AtualizadorApp.verificar() }
        verificandoAtualizacao = false
    }

    if (atualizacao != null && !baixandoAtualizacao) {
        AlertDialog(
            onDismissRequest = { atualizacao = null },
            title = { Text("Nova versão disponível") },
            text = {
                Text("A versão ${atualizacao!!.versao} do Agente Pessoal está disponível. Deseja atualizar agora?")
            },
            confirmButton = {
                Button(onClick = {
                    val nova = atualizacao
                    if (nova != null) {
                        atualizacao = null
                        baixandoAtualizacao = true
                        escopo.launch(Dispatchers.Main) {
                            val sucesso = AtualizadorApp.baixarEInstalar(contexto, nova)
                            baixandoAtualizacao = false
                            if (!sucesso) {
                                Toast.makeText(
                                    contexto,
                                    "Não foi possível baixar a atualização",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }) { Text("Atualizar") }
            },
            dismissButton = {
                TextButton(onClick = { atualizacao = null }) { Text("Agora não") }
            }
        )
    }

    if (baixandoAtualizacao) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Baixando atualização") },
            text = { Text("Aguarde enquanto o novo APK é baixado...") },
            confirmButton = {}
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Agente Pessoal") }) }
    ) { padding ->
        LazyColumn(
            state = listaState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("WhatsApp", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (whatsAtivo) "Leitura de notificações ativa. Novas mensagens são atualizadas automaticamente."
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
                            OutlinedButton(onClick = {
                                selecionarMensagemParaAnalise(mensagem.texto)
                            }) {
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
                Text("Atualização", style = MaterialTheme.typography.titleLarge)
                Text(
                    if (verificandoAtualizacao) "Verificando se existe uma nova versão..."
                    else "O app verifica atualizações automaticamente ao abrir.",
                    style = MaterialTheme.typography.bodySmall
                )
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
                    "Integração atual: WhatsApp por notificações + atualização automática da tela + verificação de novas versões. O envio de mensagens continua manual.",
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
