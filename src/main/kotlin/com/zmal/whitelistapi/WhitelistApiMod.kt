package com.zmal.whitelistapi

import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path


object WhitelistApiMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("whitelistapimod")
    var mcServer: MinecraftServer? = null
    var config: Config? = null
    private var httpServer: HttpServer? = null
    private var started = false

    override fun onInitialize() {
        config = loadConfig()

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            mcServer = server
            if (!started) {
                try {
                    startHttpServer()
                    started = true
                } catch (e: IOException) {
                    logger.error("Failed to start HTTP server", e)
                }
            }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            httpServer?.stop(0)
            logger.info("[WhitelistAPI] HTTP server stopped")
            started = false
        }
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                literal("wla").then(
                    literal("reload").executes { _ ->
                        httpServer?.stop(0)
                        Thread.sleep(500)
                        logger.info("[WhitelistAPI] HTTP server stopped for reload")
                        config = loadConfig()
                        try {
                            startHttpServer()
                            started = true
                        } catch (e: IOException) {
                            logger.error("Failed to start HTTP server after reload", e)
                        }
                        mcServer?.execute {
                            mcServer!!.playerManager.broadcast(Text.literal("配置已重载"), false)
                        }
                        1
                    })
            )

        }
    }

    class Config internal constructor(var port: Int, var token: String?)

    private fun loadConfig(): Config? {
        val path: Path = Path.of("config/whitelist_api.json")
        val gson = Gson()
        try {
            if (!Files.exists(path)) {
                val defaultConfig = Config(6626, "change_me")
                Files.createDirectories(path.parent)
                Files.writeString(path, gson.toJson(defaultConfig), StandardCharsets.UTF_8)
                return defaultConfig
            }
            val json: String? = Files.readString(path)
            return gson.fromJson(json, Config::class.java)
        } catch (e: IOException) {
            e.printStackTrace()
            return Config(6626, "change_me")
        }
    }

    @Throws(IOException::class)
    private fun startHttpServer() {
        var port = config!!.port
        while (true) {
            try {
                httpServer = HttpServer.create(InetSocketAddress(port), 0)
                break
            } catch (_: BindException) {
                port++
            }
        }

        httpServer!!.createContext("/whitelist/add", AddWhitelistHandler())
        httpServer!!.executor = null
        httpServer!!.start()
        logger.info("[WhitelistAPI] HTTP server started at port $port")


    }

    class AddWhitelistHandler : HttpHandler {
        @Throws(IOException::class)
        override fun handle(exchange: HttpExchange) {
            if (!"GET".equals(exchange.requestMethod, ignoreCase = true)) {
                sendResponse(exchange, 405, "Method Not Allowed")
                return
            }

            // 鉴权
            val auth = exchange.requestHeaders.getFirst("Authorization")
            if (auth == null || auth != "Bearer " + config?.token) {
                sendResponse(exchange, 401, "Unauthorized")
                return
            }

            // 获取参数
            val query = exchange.requestURI.getQuery() // ?player=Steve
            if (query == null || !query.startsWith("player=")) {
                sendResponse(exchange, 400, "Bad Request: missing player")
                return
            }

            val player = query.substring("player=".length)

            // 在 MC 主线程执行
            mcServer?.let { server ->
                server.execute {
                    server.commandManager.executeWithPrefix(
                        server.commandSource, "whitelist add $player"
                    )
                }
            }


            sendResponse(exchange, 200, "Player $player added to whitelist")
            logger.info("Player $player added to whitelist")
        }
    }

    @Throws(IOException::class)
    private fun sendResponse(exchange: HttpExchange, status: Int, body: String) {
        exchange.sendResponseHeaders(status, body.toByteArray(StandardCharsets.UTF_8).size.toLong())
        exchange.responseBody.use { os ->
            os.write(body.toByteArray(StandardCharsets.UTF_8))
        }
    }
}