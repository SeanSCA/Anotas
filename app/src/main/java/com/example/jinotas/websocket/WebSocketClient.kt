package com.example.jinotas.websocket

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.wss
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WebSocketClient(private val url: String, private val scope: CoroutineScope) {
    private val client = HttpClient(CIO) {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    fun connect(listener: WebSocketListener) {
        scope.launch {
            try {
                client.wss(url) {
                    session = this
                    listener.onConnected()
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                listener.onMessage(frame.readText())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            listener.onDisconnected()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    listener.onDisconnected()
                }
            }
        }
    }

    fun sendMessage(message: String) {
        scope.launch {
            session?.send(Frame.Text(message))
        }
    }

    fun disconnect() {
        client.close()
    }
}
