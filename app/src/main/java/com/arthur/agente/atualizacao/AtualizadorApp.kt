package com.arthur.agente.atualizacao

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Verifica a última release pública e instala o APK depois da confirmação do usuário. */
object AtualizadorApp {
    data class Atualizacao(
        val versao: String,
        val urlApk: String
    )

    private const val API_RELEASE = "https://api.github.com/repos/userc0d3x/AgentePessoal/releases/latest"
    private const val VERSAO_ATUAL = "0.2"

    suspend fun verificar(): Atualizacao? = withContext(Dispatchers.IO) {
        try {
            val conexao = URL(API_RELEASE).openConnection() as HttpURLConnection
            conexao.requestMethod = "GET"
            conexao.setRequestProperty("Accept", "application/vnd.github+json")
            conexao.setRequestProperty("User-Agent", "AgentePessoal")
            conexao.connectTimeout = 5000
            conexao.readTimeout = 5000

            if (conexao.responseCode !in 200..299) return@withContext null
            val corpo = conexao.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(corpo)
            val versao = json.optString("tag_name").removePrefix("v")
            val assets = json.optJSONArray("assets") ?: return@withContext null

            var urlApk: String? = null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name") == "app-debug.apk") {
                    urlApk = asset.optString("browser_download_url")
                    break
                }
            }

            if (urlApk.isNullOrBlank() || !versaoMaior(versao, VERSAO_ATUAL)) null
            else Atualizacao(versao, urlApk)
        } catch (_: Exception) {
            null
        }
    }

    private fun versaoMaior(nova: String, atual: String): Boolean {
        fun partes(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
        val n = partes(nova)
        val a = partes(atual)
        val tamanho = maxOf(n.size, a.size)
        for (i in 0 until tamanho) {
            val nv = n.getOrElse(i) { 0 }
            val av = a.getOrElse(i) { 0 }
            if (nv != av) return nv > av
        }
        return false
    }

    suspend fun baixarEInstalar(contexto: Context, atualizacao: Atualizacao): Boolean = withContext(Dispatchers.IO) {
        try {
            val destino = File(contexto.cacheDir, "agente-pessoal-update.apk")
            val conexao = URL(atualizacao.urlApk).openConnection() as HttpURLConnection
            conexao.instanceFollowRedirects = true
            conexao.connectTimeout = 10000
            conexao.readTimeout = 30000
            conexao.inputStream.use { entrada ->
                destino.outputStream().use { saida -> entrada.copyTo(saida) }
            }

            val uri: Uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                destino
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            contexto.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
