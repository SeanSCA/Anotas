package com.example.jinotas.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import com.example.jinotas.R

class MarkdownEditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val webView: WebView

    init {
        // Inflar el layout personalizado
        LayoutInflater.from(context).inflate(R.layout.custom_markdown_editor, this, true)
        webView = findViewById(R.id.webViewMarkdown) // Asegúrate que este ID esté en tu layout XML
        // Configurar el WebView
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(this, "Android") // Agregar esta línea
        webView.loadUrl("file:///android_asset/markdown_editor.html")

        // Iniciar el WebView como editable
        initializeEditableWebView()
    }

    private fun initializeEditableWebView() {
        // Hacer que el contenido sea editable
        val js = """
            document.body.contentEditable = true;
            document.body.style.fontFamily = 'Arial, sans-serif';
            document.body.style.fontSize = '16px';
            document.body.style.color = 'black';
            document.body.style.padding = '10px';
            document.body.setAttribute('placeholder', 'Escribe tu Markdown aquí...');
        """.trimIndent()

        // Cargar el script para hacerlo editable
        webView.evaluateJavascript(js, null)

        // Escuchar los cambios en el contenido
        webView.evaluateJavascript(
            """
            document.body.addEventListener('input', function() {
                const currentContent = document.body.innerText;
                Android.updateMarkdown(currentContent); // Actualiza el Markdown en cada cambio
            });
            """.trimIndent(), null
        )
    }

    // Método para cargar contenido Markdown en el WebView
    fun loadMarkdown(markdown: String) {
        val htmlContent = convertMarkdownToHtml(markdown)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    // Convierte el texto Markdown a HTML
    private fun convertMarkdownToHtml(markdown: String): String {
        val lines = markdown.split("\n")
        val htmlBuilder = StringBuilder()

        for (line in lines) {
            when {
                line.startsWith("- [ ]") -> { // Checkbox sin marcar
                    htmlBuilder.append(
                        "<label><input type='checkbox' onchange='updateCheckbox(this)'/> ${
                            line.substring(6)
                        }</label><br>"
                    )
                }

                line.startsWith("- [x]") -> { // Checkbox marcado
                    htmlBuilder.append(
                        "<label><input type='checkbox' onchange='updateCheckbox(this)' checked/> ${
                            line.substring(6)
                        }</label><br>"
                    )
                }

                else -> {
                    htmlBuilder.append("<p>${line}</p>")
                }
            }
        }

        return """
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; font-size: 16px; color: black; }
                </style>
                <script>
                    function updateCheckbox(checkbox) {
                        var lines = document.body.innerText.split('\\n');
                        lines.forEach((line, index) => {
                            if (checkbox.nextSibling.nodeValue.trim() === line.trim()) {
                                lines[index] = checkbox.checked ? '- [x] ' + line.trim() : '- [ ] ' + line.trim();
                            }
                        });
                        Android.updateMarkdown(lines.join('\\n'));
                    }
                </script>
            </head>
            <body>$htmlBuilder</body>
            </html>
        """.trimIndent()
    }

    // Método para actualizar el Markdown y cargar en el WebView
    @JavascriptInterface
    fun updateMarkdown(markdown: String) {
        loadMarkdown(markdown)
    }

    // Método para obtener el texto en Markdown, si es necesario
    fun getMarkdownText(callback: (String) -> Unit) {
        webView.evaluateJavascript("document.body.innerText") { markdown ->
            callback(markdown.trim('"'))
        }
    }
}
