/*
 * @(#) Line.java
 *
 * yaml-simple  Simple implementation of YAML
 * Copyright (c) 2020 Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.yaml.parser

import net.pwall.util.ParseText

/**
 * An extension to [ParseText] for parsing YAML lines.  The class includes a line number (principally for error
 * reporting) and on initialisation, positions the index after any leading spaces.
 *
 * @author  Peter Wall
 */
class Line(val lineNumber: Int, line: String) : ParseText(line) {

    init {
        skipPast { it == 0x20 }
    }

    /**
     *  Match a character followed by end of line or whitespace.  If the match is successful, the index will be
     *  positioned at the end of the whitespace.
     */
    private fun matchWithWhitespace(ch: Char): Boolean {
        if (!match(ch))
            return false
        if (isExhausted)
            return true
        val charIndex = start
        if (matchSpaces()) {
            start = charIndex
            return true
        }
        index = charIndex
        return false
    }

    /** Match a colon - must be followed by end of line or whitespace */
    fun matchColon(): Boolean = matchWithWhitespace(':')

    /** Match a dash - must be followed by end of line or whitespace */
    fun matchDash(): Boolean = matchWithWhitespace('-')

    /** Match a hash - must be preceded by start of line or whitespace */
    fun matchHash(): Boolean {
        if (!match('#'))
            return false
        if (start == 0 || isSpace(charAt(start - 1)))
            return true
        index = start
        return false
    }

    fun atEnd(): Boolean = isExhausted || matchHash()

    fun skipBackSpaces() {
        while (index > 0 && isSpace(charAt(index - 1)))
            index--
    }

}
