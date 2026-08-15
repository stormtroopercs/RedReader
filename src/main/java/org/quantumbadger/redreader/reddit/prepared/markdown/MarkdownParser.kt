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
 */
package org.quantumbadger.redreader.reddit.prepared.markdown

object MarkdownParser {
    fun parse(raw: CharArray?): MarkdownParagraphGroup {
        val rawLines: Array<CharArrSubstring?> = CharArrSubstring.Companion.generateFromLines(raw)

        val lines = arrayOfNulls<MarkdownLine>(rawLines.size)

        for (i in rawLines.indices) {
            lines[i] = MarkdownLine.Companion.generate(rawLines[i])
        }

        val mergedLines = ArrayList<MarkdownLine>(rawLines.size)
        var currentLine: MarkdownLine?=null

        for (i in lines.indices) {
            if (currentLine != null) {
                when (lines[i]!!.type) {
                    MarkdownParagraphType.BULLET, MarkdownParagraphType.NUMBERED, MarkdownParagraphType.HEADER, MarkdownParagraphType.CODE, MarkdownParagraphType.HLINE, MarkdownParagraphType.QUOTE -> {
                        mergedLines.add(currentLine)
                        currentLine = lines[i]
                    }

                    MarkdownParagraphType.EMPTY -> {
                        mergedLines.add(currentLine)
                        currentLine = null
                    }

                    MarkdownParagraphType.TEXT -> when (lines[i - 1]!!.type) {
                        MarkdownParagraphType.QUOTE, MarkdownParagraphType.BULLET, MarkdownParagraphType.NUMBERED, MarkdownParagraphType.TEXT -> if (lines[i - 1]!!.spacesAtEnd >= 2) {
                            mergedLines.add(currentLine)
                            currentLine = lines[i]
                        } else {
                            currentLine = currentLine.rejoin(lines[i])
                        }

                        MarkdownParagraphType.CODE, MarkdownParagraphType.HEADER, MarkdownParagraphType.HLINE -> {
                            mergedLines.add(currentLine)
                            currentLine = lines[i]
                        }
                    }

                }
            } else if (lines[i]!!.type != MarkdownParagraphType.EMPTY) {
                currentLine = lines[i]
            }
        }

        if (currentLine != null) {
            mergedLines.add(currentLine)
        }

        val outputParagraphs =             ArrayList<MarkdownParagraph?>(mergedLines.size)

        for (line in mergedLines) {
            val lastParagraph = if (outputParagraphs.isEmpty())
                null
            else
                outputParagraphs.get(outputParagraphs.size - 1)

            val paragraph = line.tokenize(lastParagraph)

            if (!paragraph.isEmpty) {
                outputParagraphs.add(paragraph)
            }
        }

        return MarkdownParagraphGroup(outputParagraphs.toTypedArray<MarkdownParagraph?>())
    }

    enum class MarkdownParagraphType {
        TEXT, CODE, BULLET, NUMBERED, QUOTE, HEADER, HLINE, EMPTY
    }
}
