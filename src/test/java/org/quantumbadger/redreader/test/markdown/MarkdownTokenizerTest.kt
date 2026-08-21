/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.test.markdown

import org.junit.Assert.assertEquals
import org.junit.Test
import org.quantumbadger.redreader.reddit.prepared.markdown.CharArrSubstring
import org.quantumbadger.redreader.reddit.prepared.markdown.IntArrayLengthPair
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownTokenizer

class MarkdownTokenizerTest {

    private companion object {

        private fun toCAS(s: String): CharArrSubstring {
            return CharArrSubstring.generate(s.toCharArray())
        }

        private fun toIALP(data: IntArray): IntArrayLengthPair {
            val result = IntArrayLengthPair(data.size)
            result.append(data)
            return result
        }

        private fun naiveTokenize(markdown: String): IntArrayLengthPair {

            val input = IntArrayLengthPair(markdown.length)
            val output = IntArrayLengthPair(markdown.length)

            input.append(markdown.toCharArray())

            MarkdownTokenizer.naiveTokenize(input, output)

            return output
        }

        private fun assertIAEquals(expected: IntArray, actual: IntArrayLengthPair) {

            assertEquals(expected.size, actual.pos)

            for (i in expected.indices) {
                assertEquals(expected[i], actual.data[i])
            }
        }
    }

    @Test
    fun testTokenizeItalic1() {

        val out = MarkdownTokenizer.tokenize(toCAS("a *b*"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK, 'b'.code, MarkdownTokenizer.TOKEN_ASTERISK
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeItalic2() {

        val out = MarkdownTokenizer.tokenize(toCAS("a* *b*"))

        val expected = intArrayOf(
            'a'.code, '*'.code, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK, 'b'.code, MarkdownTokenizer.TOKEN_ASTERISK
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeBold1() {

        val out = MarkdownTokenizer.tokenize(toCAS("a **b**"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b'.code, MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeBold2() {

        val out = MarkdownTokenizer.tokenize(toCAS("a** **b**"))

        val expected = intArrayOf(
            'a'.code, '*'.code, '*'.code, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b'.code, MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink1() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [b](c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink2() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [b]c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, '['.code, 'b'.code, ']'.code, 'c'.code, ')'.code, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink3() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [b]  (c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink4() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [b] (c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink5() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [[b]](c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '['.code, 'b'.code, ']'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink6() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [[[b]]] (c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '['.code, '['.code, 'b'.code, ']'.code, ']'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink7() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [[b](c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, '['.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeLink8() {

        val out = MarkdownTokenizer.tokenize(toCAS("a [[[ *b*  **b**]]] (c) d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '['.code, '['.code, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK,
            'b'.code, MarkdownTokenizer.TOKEN_ASTERISK, ' '.code, MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b'.code,
            MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, ']'.code, ']'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeUnderscore1() {
        val out = MarkdownTokenizer.tokenize(toCAS("a_b_c_d"))

        val expected = intArrayOf(
            'a'.code, '_'.code, 'b'.code, '_'.code, 'c'.code, '_'.code, 'd'.code
        )
        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeUnderscore2() {
        val out = MarkdownTokenizer.tokenize(toCAS("_abcd_"))

        val expected = intArrayOf(
            MarkdownTokenizer.TOKEN_UNDERSCORE, 'a'.code, 'b'.code, 'c'.code, 'd'.code, MarkdownTokenizer.TOKEN_UNDERSCORE
        )
        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeUnderscore3() {
        val out = MarkdownTokenizer.tokenize(toCAS("_a_b cd_"))

        val expected = intArrayOf(
            MarkdownTokenizer.TOKEN_UNDERSCORE, 'a'.code, '_'.code, 'b'.code, ' '.code, 'c'.code, 'd'.code, MarkdownTokenizer.TOKEN_UNDERSCORE
        )
        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeUnderscore4() {
        val out = MarkdownTokenizer.tokenize(toCAS("ab _abcd_ ab"))

        val expected = intArrayOf(
            'a'.code, 'b'.code, ' '.code, MarkdownTokenizer.TOKEN_UNDERSCORE, 'a'.code, 'b'.code, 'c'.code, 'd'.code,
            MarkdownTokenizer.TOKEN_UNDERSCORE, ' '.code, 'a'.code, 'b'.code
        )
        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeSuperscript1() {
        val out = MarkdownTokenizer.tokenize(toCAS("^^^All ^^^of ^^^this ^^^should ^^^be ^^^superscripted"))

        val expected = intArrayOf(
            MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
            'A'.code, 'l'.code, 'l'.code, ' '.code, MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
            MarkdownTokenizer.TOKEN_CARET, 'o'.code, 'f'.code, ' '.code, MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
            MarkdownTokenizer.TOKEN_CARET, 't'.code, 'h'.code, 'i'.code, 's'.code, ' '.code, MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
            MarkdownTokenizer.TOKEN_CARET, 's'.code, 'h'.code, 'o'.code, 'u'.code, 'l'.code, 'd'.code, ' '.code, MarkdownTokenizer.TOKEN_CARET,
            MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, 'b'.code, 'e'.code, ' '.code, MarkdownTokenizer.TOKEN_CARET,
            MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, 's'.code, 'u'.code, 'p'.code, 'e'.code, 'r'.code, 's'.code, 'c'.code, 'r'.code, 'i'.code,
            'p'.code, 't'.code, 'e'.code, 'd'.code
        )
        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeRedditLink1() {

        val out = MarkdownTokenizer.tokenize(toCAS("a /r/abc d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '/'.code, 'r'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, '/'.code, 'r'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeRedditLink2() {

        val out = MarkdownTokenizer.tokenize(toCAS("a /u/abc d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '/'.code, 'u'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, '/'.code, 'u'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeRedditLink3() {

        val out = MarkdownTokenizer.tokenize(toCAS("a r/abc d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'r'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, 'r'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testTokenizeRedditLink4() {

        val out = MarkdownTokenizer.tokenize(toCAS("a u/abc d"))

        val expected = intArrayOf(
            'a'.code, ' '.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'u'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, 'u'.code, '/'.code, 'a'.code, 'b'.code, 'c'.code,
            MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' '.code, 'd'.code
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testNaiveTokenizeLink1() {

        val out = naiveTokenize("[[a]](b)")

        val expected = intArrayOf(
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN,
            'a'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testCleanLink1() {

        val input = naiveTokenize("[[a]](b)")
        val out = IntArrayLengthPair(128)

        MarkdownTokenizer.clean(input, out)

        val expected = intArrayOf(
            MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '['.code, 'a'.code, ']'.code, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
            MarkdownTokenizer.TOKEN_PAREN_OPEN, 'b'.code, MarkdownTokenizer.TOKEN_PAREN_CLOSE
        )

        assertIAEquals(expected, out)
    }

    @Test
    fun testFindCloseWellBracketed1() {

        assertEquals(MarkdownTokenizer.findCloseWellBracketed(
            intArrayOf('('.code, ')'.code),
            '('.code,
            ')'.code,
            0,
            2
        ), 1)
    }

    @Test
    fun testFindCloseWellBracketed2() {

        assertEquals(MarkdownTokenizer.findCloseWellBracketed(
            intArrayOf('('.code, '('.code, ')'.code, ')'.code),
            '('.code,
            ')'.code,
            0,
            4
        ), 3)
    }

    @Test
    fun testFindCloseWellBracketed3() {

        assertEquals(MarkdownTokenizer.findCloseWellBracketed(
            intArrayOf('('.code, '('.code, ')'.code),
            '('.code,
            ')'.code,
            0,
            3
        ), -1)
    }
}
