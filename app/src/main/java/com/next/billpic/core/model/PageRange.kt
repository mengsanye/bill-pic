package com.next.billpic.core.model

/**
 * 页码范围解析。
 *
 * 为什么需要它：一份 PDF 里常有十几页发票，用户只想要其中几页。
 * 上一版只能在满 30 页时提示「仅转前 30 页」，用户没有任何选择权——
 * 而报销场景恰恰经常是「这张 PDF 里只有第 3 页是我的」。
 *
 * 输入容错做得比较宽：中文逗号、顿号、全角数字与全角减号都接受，
 * 区间写反（如 `3-1`）自动纠正为 `1-3` 而不是报错——错在格式上的挫败感不值得。
 */
object PageRange {

    sealed class Result {
        /** pages 为 1-based 页码，升序去重 */
        data class Success(val pages: List<Int>, val truncated: Boolean) : Result()

        data class Failure(val message: String) : Result()
    }

    /**
     * @param input     用户输入，空白表示「全部」
     * @param pageCount 这份 PDF 的总页数
     * @param maxPages  单次转换页数上限
     */
    fun parse(input: String, pageCount: Int, maxPages: Int = AppConfig.MAX_PAGES): Result {
        val total = pageCount.coerceAtLeast(0)
        if (total == 0) return Result.Failure("这个 PDF 没有可转换的页面")

        val normalized = normalize(input)
        if (normalized.isBlank()) {
            return Result.Success((1..total).take(maxPages), truncated = total > maxPages)
        }

        val picked = sortedSetOf<Int>()
        normalized.split(',').forEach { rawToken ->
            val token = rawToken.trim()
            if (token.isEmpty()) return@forEach

            val bounds = token.split('-').filter { it.isNotBlank() }
            when (bounds.size) {
                1 -> {
                    val page = bounds[0].toIntOrNull()
                        ?: return Result.Failure("「$token」看不懂，页码请填数字，例如 1-3,5")
                    validate(page, total, token)?.let { return it }
                    picked += page
                }

                2 -> {
                    val a = bounds[0].toIntOrNull()
                        ?: return Result.Failure("「$token」看不懂，页码请填数字，例如 1-3,5")
                    val b = bounds[1].toIntOrNull()
                        ?: return Result.Failure("「$token」看不懂，页码请填数字，例如 1-3,5")
                    validate(a, total, token)?.let { return it }
                    validate(b, total, token)?.let { return it }
                    // 写反了自动纠正，不报错
                    val from = minOf(a, b)
                    val to = maxOf(a, b)
                    for (page in from..to) picked += page
                }

                else -> return Result.Failure("「$token」里有多个「-」，区间请写成 1-3 这样")
            }
        }

        if (picked.isEmpty()) return Result.Failure("没解析出任何页码，留空表示转换全部")

        val ordered = picked.toList()
        return if (ordered.size > maxPages) {
            Result.Success(ordered.take(maxPages), truncated = true)
        } else {
            Result.Success(ordered, truncated = false)
        }
    }

    /** 把常见的中文/全角写法归一化，减少用户的格式挫败感。 */
    private fun normalize(input: String): String {
        val builder = StringBuilder(input.length)
        input.forEach { ch ->
            val mapped = when (ch) {
                '，', '、', '；', ';', ' ', '\t', '\n' -> ','
                '－', '–', '—', '~', '～', '至' -> '-'
                else -> if (ch in '０'..'９') ('0' + (ch - '０')) else ch
            }
            builder.append(mapped)
        }
        return builder.toString().trim().trim(',').trim()
    }

    private fun validate(page: Int, total: Int, token: String): Result.Failure? = when {
        page < 1 -> Result.Failure("页码要从 1 开始，「$token」不对")
        page > total -> Result.Failure("这份发票一共 $total 页，填不了第 $page 页")
        else -> null
    }

    /** 把选中的页码压缩成展示文本：1-3,5,8-10 */
    fun describe(pages: List<Int>): String {
        if (pages.isEmpty()) return "—"
        val parts = mutableListOf<String>()
        var start = pages.first()
        var prev = start
        for (i in 1 until pages.size) {
            val current = pages[i]
            if (current == prev + 1) {
                prev = current
                continue
            }
            parts += span(start, prev)
            start = current
            prev = current
        }
        parts += span(start, prev)
        return parts.joinToString(",")
    }

    private fun span(from: Int, to: Int): String = if (from == to) "$from" else "$from-$to"
}
