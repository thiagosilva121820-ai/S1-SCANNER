package com.s1scanner.mobile

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var output: TextView
    private val findings = mutableListOf<String>()
    private var selectedTree: Uri? = null
    private val keywords = listOf(
        "delta", "fluxus", "hydrogen", "arceus", "codex",
        "evon", "vegax", "trigon", "cryptic"
    )
    private val pickFolder = 4101
    private val saveReport = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 32, 36, 36)
            setBackgroundColor(0xFF101820.toInt())
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "S1 SCANNER"
            textSize = 29f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF40D6C5.toInt())
            gravity = Gravity.CENTER
        }
        root.addView(title)
        root.addView(label("AUDITORIA DE DISPOSITIVO", 14f, 0xFFFFFFFF.toInt()))
        root.addView(label("Use somente no aparelho do proprietário e com autorização.", 13f, 0xFFB9C7D0.toInt()))

        addButton(root, "INFORMAÇÕES DO DISPOSITIVO") { showDeviceInfo() }
        addButton(root, "ANALISAR APLICATIVOS VISÍVEIS") { scanApps() }
        addButton(root, "ESCOLHER PASTA PARA ANALISAR") { chooseFolder() }
        addButton(root, "EXPORTAR RELATÓRIO") { exportReport() }
        addButton(root, "LIMPAR RESULTADOS") {
            findings.clear()
            output.text = "Resultados limpos."
        }

        output = TextView(this).apply {
            text = "Aguardando análise."
            textSize = 13f
            setTextColor(0xFFE8EEF2.toInt())
            setPadding(0, 24, 0, 12)
        }
        root.addView(output)
        setContentView(scroll)
    }

    private fun label(text: String, size: Float, color: Int) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        setPadding(0, 14, 0, 8)
    }

    private fun addButton(parent: LinearLayout, text: String, action: () -> Unit) {
        val button = Button(this).apply {
            this.text = text
            isAllCaps = false
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = 8
        })
    }

    private fun showDeviceInfo() {
        val info = """
            DISPOSITIVO
            Fabricante: ${android.os.Build.MANUFACTURER}
            Modelo: ${android.os.Build.MODEL}
            Android: ${android.os.Build.VERSION.RELEASE}
            API: ${android.os.Build.VERSION.SDK_INT}
            App: S1 SCANNER 1.0
        """.trimIndent()
        findings.add(info)
        output.text = info
    }

    @Suppress("DEPRECATION")
    private fun scanApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val matches = apps.filter { app ->
            val haystack = (app.packageName + " " + pm.getApplicationLabel(app)).lowercase(Locale.ROOT)
            keywords.any { haystack.contains(it) }
        }.sortedBy { it.packageName }

        val report = buildString {
            appendLine("ANÁLISE DE APLICATIVOS")
            appendLine("Aplicativos visíveis ao sistema: ${apps.size}")
            appendLine("Possíveis indicadores encontrados: ${matches.size}")
            if (matches.isEmpty()) appendLine("Nenhum nome correspondente foi encontrado.")
            matches.forEach {
                appendLine("- ${pm.getApplicationLabel(it)} | ${it.packageName}")
            }
            appendLine()
            appendLine("ATENÇÃO: correspondência por nome não comprova uso ou atividade indevida.")
        }
        findings.add(report)
        output.text = report
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        startActivityForResult(intent, pickFolder)
    }

    @Deprecated("Using activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickFolder && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            selectedTree = uri
            val flags = data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            try { contentResolver.takePersistableUriPermission(uri, flags) } catch (_: Exception) {}
            scanTree(uri)
        } else if (requestCode == saveReport && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(makeReport()) }
                Toast.makeText(this, "Relatório exportado.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível salvar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scanTree(uri: Uri) {
        val root = DocumentFile.fromTreeUri(this, uri)
        if (root == null) {
            output.text = "Não foi possível abrir a pasta selecionada."
            return
        }
        val hits = mutableListOf<String>()
        var visited = 0
        fun walk(dir: DocumentFile, depth: Int) {
            if (depth > 12 || visited >= 8000) return
            val children = try { dir.listFiles() } catch (_: Exception) { emptyArray() }
            for (file in children) {
                if (visited++ >= 8000) break
                val name = file.name ?: continue
                if (keywords.any { name.lowercase(Locale.ROOT).contains(it) }) {
                    hits.add("${if (file.isDirectory) "[PASTA]" else "[ARQUIVO]"} $name")
                }
                if (file.isDirectory) walk(file, depth + 1)
            }
        }
        walk(root, 0)
        val report = buildString {
            appendLine("ANÁLISE DA PASTA AUTORIZADA")
            appendLine("Itens verificados: $visited")
            appendLine("Possíveis indicadores: ${hits.size}")
            if (hits.isEmpty()) appendLine("Nenhum nome correspondente foi encontrado.")
            hits.forEach { appendLine("- $it") }
            appendLine()
            appendLine("A análise considera nomes de arquivos e pastas, não seu conteúdo.")
            appendLine("O Android pode limitar o que fica disponível pela pasta escolhida.")
        }
        findings.add(report)
        output.text = report
    }

    private fun makeReport(): String {
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("S1 SCANNER - RELATÓRIO")
            appendLine("Data: $date")
            appendLine("Este relatório registra somente verificações iniciadas pelo usuário.")
            appendLine()
            findings.forEachIndexed { index, item ->
                appendLine("========== RESULTADO ${index + 1} ==========")
                appendLine(item)
                appendLine()
            }
            appendLine("LIMITES")
            appendLine("- Nomes correspondentes são indicadores, não prova.")
            appendLine("- Não acessa dados privados de outros aplicativos.")
            appendLine("- Não consulta histórico privado do navegador.")
            appendLine("- Não recupera arquivos permanentemente apagados.")
        }
    }

    private fun exportReport() {
        if (findings.isEmpty()) {
            Toast.makeText(this, "Faça uma análise antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "S1_SCANNER_relatorio.txt")
        }
        startActivityForResult(intent, saveReport)
    }
}
