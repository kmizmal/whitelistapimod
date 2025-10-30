package com.zmal.whitelistapi

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PlayerStatsManager {
    private val gson = Gson()
    private val statsDir = File("world/stats")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class PlayerStats(
        val uuid: String,
        val custom: Map<String, Any>,
        val mined: Map<String, Any>,
        val crafted: Map<String, Any>,
        val used: Map<String, Any>,
        val killed: Map<String, Any>
    )

    val statsMap: MutableMap<String, PlayerStats> = ConcurrentHashMap()

    // 异步初始化所有文件
    fun initAll() {
        scope.launch {
            val files = statsDir.listFiles { file -> file.extension == "json" } ?: return@launch
            val results = files.map { file ->
                async { parseStatsFile(file) }
            }.awaitAll()

            results.filterNotNull().forEach { stats ->
                statsMap[stats.uuid] = stats
            }
        }
    }

    fun reflash(uuid: UUID) {
        val file = File(statsDir, "$uuid.json")

        if (!file.exists()) {
            println("File for UUID $uuid does not exist.")
            return
        }

        scope.launch {
            parseStatsFile(file)?.let { stats ->
                statsMap[uuid.toString()] = stats
            }
        }
    }

    private fun parseStatsFile(file: File): PlayerStats? {
        return try {
            val jsonString = file.readText()
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val playerStats: Map<String, Any> = gson.fromJson(jsonString, type)
            val stats = playerStats["stats"] as? Map<*, *>
            val uuid = file.name.substringBeforeLast(".")

            PlayerStats(
                uuid = uuid,
                custom = extractSection(stats, "minecraft:custom"),
                mined = extractSection(stats, "minecraft:mined"),
                crafted = extractSection(stats, "minecraft:crafted"),
                used = extractSection(stats, "minecraft:used"),
                killed = extractSection(stats, "minecraft:killed")
            )
        } catch (e: Exception) {
            println("Error parsing file ${file.name}: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun extractSection(stats: Map<*, *>?, key: String): Map<String, Any> {
        val rawSection = stats?.get(key) as? Map<*, *> ?: return emptyMap()
        return rawSection.entries.mapNotNull { (k, v) ->
            val keyString = k?.toString() ?: return@mapNotNull null
            val value = v ?: return@mapNotNull null
            keyString to value
        }.toMap()
    }

    data class WhitelistEntry(
        val uuid: String, val name: String, val allowed: Boolean
    )

    object WhitelistCache {
        private val gson = Gson()
        private val cache: MutableMap<String, UUID> = ConcurrentHashMap()

        // 初始化缓存
        fun loadCache() {
            scope.launch {
                val file = File("whitelist.json")
                if (!file.exists()) {
                    println("whitelist.json not found.")
                    cache.clear()
                    return@launch
                }

                val type = object : TypeToken<List<WhitelistEntry>>() {}.type
                val whitelistEntries: List<WhitelistEntry> = gson.fromJson(file.readText(), type)

                cache.clear()
                cache.putAll(whitelistEntries.associate { it.name.lowercase() to UUID.fromString(it.uuid) })
                println("Whitelist cache loaded with ${cache.size} entries.")
            }
        }

        // 根据玩家名查询 UUID
        fun getUUID(playerName: String): UUID? {
            return cache[playerName.lowercase()]
        }
    }
}
