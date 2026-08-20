package com.arthur.agente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Mensagem(
    val texto: String,
    val usuario: Boolean
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AgentePessoalApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentePessoalApp() {

    var mensagem by remember { mutableStateOf("") }

    val mensagens = remember {
        mutableStateListOf(
            Mensagem(
                "Olá, Arthur 👋\nSeu agente está pronto para começar.",
                false
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Agente Pessoal")
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mensagens) { msg ->

                    Text(
                        text = if (msg.usuario) {
                            "Você: ${msg.texto}"
                        } else {
                            "Agente: ${msg.texto}"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = mensagem,
                    onValueChange = { mensagem = it },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text("Fale com seu agente")
                    }
                )

                Button(
                    onClick = {

                        if (mensagem.isNotBlank()) {

                            val pergunta = mensagem

                            mensagens.add(
                                Mensagem(pergunta, true)
                            )

                            mensagem = ""

                            mensagens.add(
                                Mensagem(
                                    "Recebi sua mensagem: \"$pergunta\"",
                                    false
                                )
                            )
                        }
                    }
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}
