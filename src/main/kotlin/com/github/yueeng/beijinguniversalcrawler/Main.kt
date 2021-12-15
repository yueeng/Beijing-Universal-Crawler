package com.github.yueeng.beijinguniversalcrawler

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path
import kotlin.system.exitProcess

class Save : LinkedHashMap<Date, List<ListElement>>()

data class Result(
    @SerializedName("data") val `data`: Data,
    @SerializedName("errorcode") val errorcode: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("ret") val ret: Int
)

data class Data(
    @SerializedName("list") val list: List<ListElement>,
    @SerializedName("pagination") val pagination: Pagination
)

data class ListElement(
    @SerializedName("area") val area: String,
    @SerializedName("collect_count") val collectCount: Int,
    @SerializedName("cover_image") val coverImage: String,
    @SerializedName("custom_label") val customLabel: String,
    @SerializedName("favourited") val favourited: Boolean,
    @SerializedName("gems_copywriting") val gemsCopywriting: String,
    @SerializedName("gems_status") val gemsStatus: String,
    @SerializedName("id") val id: String,
    @SerializedName("indoor") val indoor: Boolean,
    @SerializedName("is_closed") val isClosed: Int,
    @SerializedName("is_support_express") val isSupportExpress: Int,
    @SerializedName("label") val label: String,
    @SerializedName("list_sort") val listSort: Int,
    @SerializedName("list_sort_info") val listSortInfo: ListSortInfo,
    @SerializedName("location_id") val locationId: String,
    @SerializedName("map_image") val mapImage: String,
    @SerializedName("map_label") val mapLabel: String,
    @SerializedName("material_id") val materialId: String,
    @SerializedName("material_type") val materialType: String,
    @SerializedName("next_time_str") val nextTimeStr: String,
    @SerializedName("play_time") val playTime: String,
    @SerializedName("poi") val poi: Poi,
    @SerializedName("position") val position: Position,
    @SerializedName("queuing_time") val queuingTime: String,
    @SerializedName("show_time") val showTime: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("thrilling") val thrilling: Int,
    @SerializedName("thrilling_degree") val thrillingDegree: Int,
    @SerializedName("title") val title: String,
    @SerializedName("waiting_time") val waitingTime: Int
)

data class Pagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("page_size") val pageSize: Int,
    @SerializedName("total") val total: Int,
    @SerializedName("total_page") val totalPage: Int
)

data class ListSortInfo(
    @SerializedName("collect_time") val collectTime: Int,
    @SerializedName("gems_status") val gemsStatus: Int,
    @SerializedName("id") val id: String,
    @SerializedName("is_collect") val isCollect: Int,
    @SerializedName("list_sort") val listSort: Int
)

data class Poi(
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("poi_id") val poiId: String
)

data class Position(
    @SerializedName("address") val address: String,
    @SerializedName("distance") val distance: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("poi_id") val poiId: String
)

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val opts = Options().addOption("?", "help", false, "Help")
//            .addOption("u", "url", true, "Universal Beijing api url")
            .addOption("r", "retry", true, "Retry times when error")
        val cmd = DefaultParser().parse(opts, args)
        if (cmd.hasOption("?")) {
            HelpFormatter().printHelp("unlock", opts)
            return
        }
        val retry = (if (cmd.hasOption("r")) cmd.getOptionValue("r").toIntOrNull() else null) ?: 3
        val gson = GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd HH:mm:ssZ")
            .enableComplexMapKeySerialization().create()
        val okhttp = OkHttpClient.Builder().build()
        val url = "https://gw.app.universalbeijingresort.com/attraction/list"
        for (i in 1..retry) {
            val response = okhttp.newCall(Request.Builder().url(url).build()).execute()
            if (response.code != 200) continue
            val json = response.body?.string() ?: continue
            val result = runCatching { gson.fromJson(json, Result::class.java) }.getOrNull() ?: continue
            if (result.ret != 0) continue
            val now = LocalDateTime.now(ZoneId.of("+8"))
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val path = Path(".", "data", "${now.year}", "${now.monthValue}", "${fmt.format(now)}.json").toFile()
            val saved = runCatching { gson.fromJson(path.readText(), Save::class.java) }.getOrNull() ?: Save()
            saved[Date()] = result.data.list
            if (path.parentFile.isFile) path.parentFile.delete()
            if (!path.parentFile.exists()) path.parentFile.mkdirs()
            runCatching { path.writeText(gson.toJson(saved)) }.onSuccess { exitProcess(0) }.onFailure { println(it.message) }
            break
        }
        exitProcess(1)
    }
}