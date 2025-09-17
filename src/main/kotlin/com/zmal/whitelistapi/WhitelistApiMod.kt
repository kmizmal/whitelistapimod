package com.zmal.whitelistapi

import com.google.gson.Gson
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
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

    private var tickTimes = mutableListOf<Long>()  // 存储每个 tick 的时间
    private const val MAX_TICK_HISTORY = 100  // 保留最近 100 次 tick 的数据，用于计算TPS
    private var currentTps = 0.0  // 存储当前TPS

    override fun onInitialize() {
        config = loadConfig()

        PlayerStatsManager.initAll()
        PlayerStatsManager.WhitelistCache.loadCache()

//        logger.info(PlayerStatsManager.statsMap.toString())

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

        ServerTickEvents.START_SERVER_TICK.register { _ ->
            val currentTickTime = System.nanoTime()  // 获取当前tick时间的时间戳（纳秒）
            tickTimes.add(currentTickTime)

            // 保证 tickTimes 不会超过历史记录最大数
            if (tickTimes.size > MAX_TICK_HISTORY) {
                tickTimes.removeAt(0)
            }

            // 计算TPS
            currentTps = calculateTps()

        }

    }

    private fun calculateTps(): Double {
        if (tickTimes.size < 2) {
            return 0.0
        }

        val totalTime = tickTimes.last() - tickTimes.first()  // 总时间
        val ticks = tickTimes.size - 1  // 总的 tick 数量

        // 计算每秒的平均TPS，单位为每秒
        val seconds = totalTime / 1_000_000_000.0  // 转换为秒
        return ticks / seconds
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
        httpServer!!.createContext("/whitelist/remove", RemoveWhitelistHandler())
        httpServer!!.createContext("/server/tps", GetTpsHandler())
        httpServer!!.createContext("/server/playStats", GetStats())
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
            PlayerStatsManager.WhitelistCache.loadCache()
            logger.info("Player $player added to whitelist")
        }
    }

    class RemoveWhitelistHandler : HttpHandler {


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
            val query = exchange.requestURI.query  // ?player=Steve
            if (query == null || !query.startsWith("player=")) {
                sendResponse(exchange, 400, "Bad Request: missing player")
                return
            }

            val player = query.substring("player=".length)

            // 在 MC 主线程执行
            mcServer?.let { server ->
                server.execute {
                    server.commandManager.executeWithPrefix(
                        server.commandSource, "whitelist remove $player"
                    )
                }
            }

            sendResponse(exchange, 200, "Player $player removed from whitelist")
            PlayerStatsManager.WhitelistCache.loadCache()
            logger.info("Player $player removed from whitelist")
        }
    }

    class GetTpsHandler : HttpHandler {
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

            // 获取当前 TPS

            sendResponse(exchange, 200, currentTps.toString())

        }
    }

    class GetStats : HttpHandler {
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
            val query = exchange.requestURI.query  // ?player=Steve
            if (query == null || !query.startsWith("player=")) {
                sendResponse(exchange, 400, "Bad Request: missing player")
                return
            }

            val player = query.substring("player=".length)

            val uuid = PlayerStatsManager.WhitelistCache.getUUID(player)

            logger.info("uuid $uuid")
            // 获取玩家的 stats 信息
            val playerStats = PlayerStatsManager.statsMap[uuid.toString()]

            // 如果找不到玩家 stats，返回 404
            if (playerStats == null) {
                sendResponse(exchange, 404, "Player stats not found")
                uuid?.let { PlayerStatsManager.reflash(it) }
                return
            }

            // 返回玩家的 stats 信息，作为 JSON 格式
            sendResponse(exchange, 200, Gson().toJson(playerStats), "application/json")
            uuid?.let { PlayerStatsManager.reflash(it) }
        }
    }


    @Throws(IOException::class)
    private fun sendResponse(exchange: HttpExchange, status: Int, body: String, contentType: String = "text/plain") {
        // 设置响应头，指定 Content-Type
        exchange.sendResponseHeaders(status, body.toByteArray(StandardCharsets.UTF_8).size.toLong())

        // 设置响应头的 Content-Type，默认为 text/plain
        exchange.responseHeaders["Content-Type"] = listOf(contentType)

        // 写入响应体
        exchange.responseBody.use { os ->
            os.write(body.toByteArray(StandardCharsets.UTF_8))
        }
    }

}