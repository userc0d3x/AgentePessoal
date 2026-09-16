package com.arthur.agente.whatsapp

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

/** Uma mensagem recebida do WhatsApp através das notificações do Android. */
data class MensagemWhatsApp(
    val remetente: String,
    val texto: String,
    val horario: Long
)

/** Armazenamento local simples das últimas mensagens capturadas. */
object ArmazenamentoWhatsApp {
    private const val PREFS = "whatsapp_agente"
    private const val CHAVE = "mensagens"
    private const val LIMITE = 50

    fun salvar(contexto: Context, mensagem: MensagemWhatsApp) {
        val prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val antigas = carregar(contexto).toMutableList()
        antigas.removeAll { it.remetente == mensagem.remetente && it.texto == mensagem.texto }
        antigas.add(0, mensagem)
        val json = JSONArray()
        antigas.take(LIMITE).forEach {
            json.put(JSONObject().apply {
                put("remetente", it.remetente)
                put("texto", it.texto)
                put("horario", it.horario)
            })
        }
        prefs.edit().putString(CHAVE, json.toString()).apply()
    }

    fun carregar(contexto: Context): List<MensagemWhatsApp> {
        val bruto = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CHAVE, null) ?: return emptyList()
        return try {
            val json = JSONArray(bruto)
            buildList {
                for (i in 0 until json.length()) {
                    val item = json.getJSONObject(i)
                    add(
                        MensagemWhatsApp(
                            remetente = item.optString("remetente"),
                            texto = item.optString("texto"),
                            horario = item.optLong("horario")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun limpar(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(CHAVE).apply()
    }
}

/**
 * Recebe somente notificações do WhatsApp e guarda o texto localmente.
 * O agente não envia mensagens automaticamente: o usuário revisa e decide enviar.
 */
class WhatsAppNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.whatsapp" && sbn.packageName != "com.whatsapp.w4b") return

        val extras = sbn.notification.extras
        val remetente = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val texto = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        )?.toString()?.trim().orEmpty()

        if (remetente.isBlank() || texto.isBlank()) return
        if (texto.equals("Nova mensagem", ignoreCase = true)) return

        ArmazenamentoWhatsApp.salvar(
            applicationContext,
            MensagemWhatsApp(remetente, texto, System.currentTimeMillis())
        )
    }
}
