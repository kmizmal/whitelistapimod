package com.zmal.whitelistapi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID


object PlayerStatsManager {
    private val gson = Gson()
    val statsMap = mutableMapOf<String, MutableMap<String, Any>>()
    private val statsDir = File("world/stats")
    private val files = statsDir.listFiles { file -> file.extension == "json" }

    // 异步初始化所有文件
    @OptIn(DelicateCoroutinesApi::class)
    fun initAll() {
        // 使用协程启动异步任务
        GlobalScope.launch {
            // 使用协程并行解析所有文件
            files?.let { files ->
                val deferredResults = files.map { file ->
                    async {
                        try {
                            val jsonString = file.readText()
                            // 使用 TypeToken 明确指定类型
                            val type = object : TypeToken<Map<String, Any>>() {}.type

                            // 解析 JSON 为 Map<String, Any>
                            val playerStats: Map<String, Any> = gson.fromJson(jsonString, type)

                            file.name.substringBeforeLast(".") to playerStats["stats"]
                        } catch (e: Exception) {
                            println("Error parsing file ${file.name}: ${e.message}")
                            e.printStackTrace()
                            file.name to emptyMap<String, Any>()
                        }
                    }
                }

                // 等待所有文件解析完成，并将结果存储到 statsMap
                deferredResults.awaitAll().forEach { (fileName, playerStats) ->
                    // 获取当前 Map 或者初始化一个新的空 Map
                    val currentStats = statsMap[fileName] ?: mutableMapOf()

                    // 更新 currentStats 中的键值对
                    if (playerStats is Map<*, *>) {
                        currentStats["uuid"]    = fileName
                        currentStats["custom"]  = playerStats["minecraft:custom"] ?: emptyMap<String, Any>()
                        currentStats["mined"]   = playerStats["minecraft:mined"] ?: emptyMap<String, Any>()
                        currentStats["crafted"] = playerStats["minecraft:crafted"] ?: emptyMap<String, Any>()
                        currentStats["used"]    = playerStats["minecraft:used"] ?: emptyMap<String, Any>()
                        currentStats["killed"]  = playerStats["minecraft:killed"] ?: emptyMap<String, Any>()
                    }
                    statsMap[fileName] = currentStats
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun reflash(uuid: UUID) {
        // 查找对应的文件
        val file = File(statsDir, "$uuid.json")

        if (file.exists()) {
            GlobalScope.launch {
                try {
                    val jsonString = file.readText()

                    val playerStats: Map<String, Any> = gson.fromJson(jsonString, object : TypeToken<Map<String, Any>>() {}.type)
                    val stats = (playerStats["stats"] as? Map<*, *>) ?: emptyMap<String, Any>()
                    // 更新 statsMap 中的该 uuid 的数据
                    val currentStats = statsMap[uuid.toString()] ?: mutableMapOf()

                    // 更新 currentStats 中的键值对
                     currentStats["uuid"]    =uuid
                     currentStats["custom"]  =stats["minecraft:custom"] ?: emptyMap<String, Any>()
                     currentStats["mined"]   =stats["minecraft:mined"] ?: emptyMap<String, Any>()
                     currentStats["crafted"] =stats["minecraft:crafted"] ?: emptyMap<String, Any>()
                     currentStats["used"]    =stats["minecraft:mined"] ?: emptyMap<String, Any>()
                     currentStats["killed"]  =stats["minecraft:killed"] ?: emptyMap<String, Any>()

                    // 将修改后的 Map 放回 statsMap
                    statsMap[uuid.toString()] = currentStats

                } catch (e: Exception) {
                    println("Error refreshing stats for UUID $uuid: ${e.message}")
                    e.printStackTrace()
                }
            }
        } else {
            println("File for UUID $uuid does not exist.")
        }
    }


    data class WhitelistEntry(
        val uuid: String, val name: String, val allowed: Boolean
    )

    object WhitelistCache {
        private val gson = Gson()
        private var cache: Map<String, UUID> = mutableMapOf()

        // 初始化缓存
        @OptIn(DelicateCoroutinesApi::class)
        fun loadCache() {
            GlobalScope.launch {
                val file = File("whitelist.json")
                if (!file.exists()) {
                    println("whitelist.json not found.")
                    return@launch
                }

                val type = object : TypeToken<List<WhitelistEntry>>() {}.type
                val whitelistEntries: List<WhitelistEntry> = gson.fromJson(file.readText(), type)

                // 使用玩家名称作为键，UUID 作为值，建立缓存
                cache = whitelistEntries.associate { it.name.lowercase() to UUID.fromString(it.uuid) }
                println("Whitelist cache loaded with ${cache.size} entries.")
            }
        }

        // 根据玩家名查询 UUID
        fun getUUID(playerName: String): UUID? {
            return cache[playerName.lowercase()]
        }

    }

}
