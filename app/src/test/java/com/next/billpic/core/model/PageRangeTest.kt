package com.next.billpic.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 页码范围解析的单元测试。
 *
 * 这块逻辑的容错规则比较多（中文标点、全角数字、区间写反、越界、上限截断），
 * 而且是纯函数，最值得用测试把行为钉住——不然改一行 normalize 就可能悄悄退化。
 */
class PageRangeTest {

    private fun pages(input: String, total: Int, max: Int = 30): List<Int> {
        val result = PageRange.parse(input, total, max)
        assertTrue("期望解析成功，实际：$result", result is PageRange.Result.Success)
        return (result as PageRange.Result.Success).pages
    }

    private fun failure(input: String, total: Int): String {
        val result = PageRange.parse(input, total)
        assertTrue("期望解析失败，实际：$result", result is PageRange.Result.Failure)
        return (result as PageRange.Result.Failure).message
    }

    @Test
    fun `空白表示全部页`() {
        assertEquals(listOf(1, 2, 3), pages("", 3))
        assertEquals(listOf(1, 2, 3), pages("   ", 3))
    }

    @Test
    fun `单个页码与逗号列表`() {
        assertEquals(listOf(2), pages("2", 5))
        assertEquals(listOf(1, 3, 5), pages("1,3,5", 5))
    }

    @Test
    fun `区间展开且结果去重升序`() {
        assertEquals(listOf(1, 2, 3), pages("1-3", 5))
        assertEquals(listOf(1, 2, 3, 5), pages("3-1,5", 5))
        assertEquals(listOf(2, 3, 4), pages("2-4,3", 5))
    }

    @Test
    fun `接受中文标点与全角数字`() {
        assertEquals(listOf(1, 2, 3), pages("1，2、3", 5))
        assertEquals(listOf(1, 2, 3), pages("１－３", 5))
        assertEquals(listOf(1, 2, 3, 5), pages("1至3；5", 5))
    }

    @Test
    fun `区间写反自动纠正而不是报错`() {
        assertEquals(listOf(2, 3, 4), pages("4-2", 5))
    }

    @Test
    fun `页码越界会报错`() {
        assertTrue(failure("0", 5).contains("从 1 开始"))
        assertTrue(failure("9", 5).contains("一共 5 页"))
    }

    @Test
    fun `无法识别的写法会报错`() {
        assertTrue(failure("abc", 5).contains("看不懂"))
        assertTrue(failure("1-2-3", 5).contains("多个"))
    }

    @Test
    fun `超过单次上限时截断并标记`() {
        val result = PageRange.parse("", 50, maxPages = 30) as PageRange.Result.Success
        assertEquals(30, result.pages.size)
        assertTrue(result.truncated)

        val small = PageRange.parse("", 10, maxPages = 30) as PageRange.Result.Success
        assertEquals(10, small.pages.size)
        assertTrue(!small.truncated)
    }

    @Test
    fun `零页 PDF 无法解析`() {
        assertTrue(failure("", 0).contains("没有可转换的页面"))
    }

    @Test
    fun `describe 把连续页码压缩成区间`() {
        assertEquals("1-3", PageRange.describe(listOf(1, 2, 3)))
        assertEquals("1-3,5", PageRange.describe(listOf(1, 2, 3, 5)))
        assertEquals("1-3,5,8-10", PageRange.describe(listOf(1, 2, 3, 5, 8, 9, 10)))
        assertEquals("—", PageRange.describe(emptyList()))
    }
}
