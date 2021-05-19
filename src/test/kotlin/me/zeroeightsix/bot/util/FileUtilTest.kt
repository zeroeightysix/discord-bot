package me.zeroeightsix.bot.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class FileUtilTest {
    @Test
    fun `test file extension trim util`() {
        assertEquals("foo", "foo.bar".withoutFileExtension)
        assertEquals("foobar", "foobar".withoutFileExtension)
    }
}