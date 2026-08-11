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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.test.markdown;

import org.junit.Test;
import org.quantumbadger.redreader.reddit.prepared.markdown.CharArrSubstring;
import org.quantumbadger.redreader.reddit.prepared.markdown.IntArrayLengthPair;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownTokenizer;

import static org.junit.Assert.assertEquals;

public class MarkdownTokenizerTest {

	private static CharArrSubstring toCAS(final String s) {
		return CharArrSubstring.generate(s.toCharArray());
	}

	private static IntArrayLengthPair toIALP(final int[] data) {
		final IntArrayLengthPair result = new IntArrayLengthPair(data.length);
		result.append(data);
		return result;
	}

	private static IntArrayLengthPair naiveTokenize(final String markdown) {

		final IntArrayLengthPair in = new IntArrayLengthPair(markdown.length());
		final IntArrayLengthPair out = new IntArrayLengthPair(markdown.length());

		in.append(markdown.toCharArray());

		MarkdownTokenizer.naiveTokenize(in, out);

		return out;
	}

	private static void assertIAEquals(final int[] expected, final IntArrayLengthPair actual) {

		assertEquals(expected.length, actual.pos);

		for(int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual.data[i]);
		}
	}

	@Test
	public void testTokenizeItalic1() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a *b*"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_ASTERISK, 'b', MarkdownTokenizer.TOKEN_ASTERISK
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeItalic2() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a* *b*"));

		final int[] expected = new int[] {
				'a', '*', ' ', MarkdownTokenizer.TOKEN_ASTERISK, 'b', MarkdownTokenizer.TOKEN_ASTERISK
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeBold1() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a **b**"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b', MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeBold2() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a** **b**"));

		final int[] expected = new int[] {
				'a', '*', '*', ' ', MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b', MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink1() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [b](c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink2() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [b]c) d"));

		final int[] expected = new int[] {
				'a', ' ', '[', 'b', ']', 'c', ')', ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink3() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [b]  (c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink4() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [b] (c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink5() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [[b]](c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '[', 'b', ']', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink6() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [[[b]]] (c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '[', '[', 'b', ']', ']', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink7() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [[b](c) d"));

		final int[] expected = new int[] {
				'a', ' ', '[', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'b', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeLink8() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a [[[ *b*  **b**]]] (c) d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '[', '[', ' ', MarkdownTokenizer.TOKEN_ASTERISK,
				'b', MarkdownTokenizer.TOKEN_ASTERISK, ' ', MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, 'b',
				MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE, ']', ']', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'c', MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeUnderscore1() {
		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a_b_c_d"));

		final int[] expected = new int[] {
				'a', '_', 'b', '_', 'c', '_', 'd'
		};
		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeUnderscore2() {
		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("_abcd_"));

		final int[] expected = new int[] {
				MarkdownTokenizer.TOKEN_UNDERSCORE, 'a', 'b', 'c', 'd', MarkdownTokenizer.TOKEN_UNDERSCORE
		};
		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeUnderscore3() {
		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("_a_b cd_"));

		final int[] expected = new int[] {
				MarkdownTokenizer.TOKEN_UNDERSCORE, 'a', '_', 'b', ' ', 'c', 'd', MarkdownTokenizer.TOKEN_UNDERSCORE
		};
		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeUnderscore4() {
		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("ab _abcd_ ab"));

		final int[] expected = new int[] {
				'a', 'b', ' ', MarkdownTokenizer.TOKEN_UNDERSCORE, 'a', 'b', 'c', 'd',
				MarkdownTokenizer.TOKEN_UNDERSCORE, ' ', 'a', 'b'
		};
		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeSuperscript1() {
		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("^^^All ^^^of ^^^this ^^^should ^^^be ^^^superscripted"));

		final int[] expected = new int[] {
				MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
				'A', 'l', 'l', ' ',MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
				MarkdownTokenizer.TOKEN_CARET, 'o', 'f', ' ', MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
				MarkdownTokenizer.TOKEN_CARET, 't', 'h', 'i', 's', ' ', MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET,
				MarkdownTokenizer.TOKEN_CARET, 's', 'h', 'o', 'u', 'l', 'd', ' ', MarkdownTokenizer.TOKEN_CARET,
				MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, 'b', 'e', ' ', MarkdownTokenizer.TOKEN_CARET,
				MarkdownTokenizer.TOKEN_CARET, MarkdownTokenizer.TOKEN_CARET, 's', 'u', 'p', 'e', 'r', 's', 'c', 'r', 'i',
				'p', 't', 'e', 'd'
		};
		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeRedditLink1() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a /r/abc d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '/', 'r', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, '/', 'r', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeRedditLink2() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a /u/abc d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '/', 'u', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, '/', 'u', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeRedditLink3() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a r/abc d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'r', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, 'r', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testTokenizeRedditLink4() {

		final IntArrayLengthPair out = MarkdownTokenizer.tokenize(toCAS("a u/abc d"));

		final int[] expected = new int[] {
				'a', ' ', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, 'u', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_PAREN_OPEN, 'u', '/', 'a', 'b', 'c',
				MarkdownTokenizer.TOKEN_PAREN_CLOSE, ' ', 'd'
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testNaiveTokenizeLink1() {

		final IntArrayLengthPair out = naiveTokenize("[[a]](b)");

		final int[] expected = new int[] {
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN,
				'a', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE, MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'b', MarkdownTokenizer.TOKEN_PAREN_CLOSE
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testCleanLink1() {

		final IntArrayLengthPair in = naiveTokenize("[[a]](b)");
		final IntArrayLengthPair out = new IntArrayLengthPair(128);

		MarkdownTokenizer.clean(in, out);

		final int[] expected = new int[] {
				MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN, '[', 'a', ']', MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE,
				MarkdownTokenizer.TOKEN_PAREN_OPEN, 'b', MarkdownTokenizer.TOKEN_PAREN_CLOSE
		};

		assertIAEquals(expected, out);
	}

	@Test
	public void testFindCloseWellBracketed1() {

		assertEquals(MarkdownTokenizer.findCloseWellBracketed(
				new int[] {'(', ')'},
				'(',
				')',
				0,
				2
		), 1);
	}

	@Test
	public void testFindCloseWellBracketed2() {

		assertEquals(MarkdownTokenizer.findCloseWellBracketed(
				new int[] {'(', '(', ')', ')'},
				'(',
				')',
				0,
				4
		), 3);
	}

	@Test
	public void testFindCloseWellBracketed3() {

		assertEquals(MarkdownTokenizer.findCloseWellBracketed(
				new int[] {'(', '(', ')'},
				'(',
				')',
				0,
				3
		), -1);
	}
}
