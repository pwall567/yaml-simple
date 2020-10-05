/*
 * @(#) YAMLSimple.java
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

package net.pwall.yaml

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.Reader

import net.pwall.json.JSONException
import net.pwall.log.Logger
import net.pwall.log.LoggerFactory
import net.pwall.util.ListMap
import net.pwall.util.ParseText
import net.pwall.yaml.parser.NumberedParseText

/**
 * A simple YAML processor.
 *
 * @author  Peter Wall
 */
object YAMLSimple {

    var log: Logger = LoggerFactory.getDefaultLogger(YAMLSimple::class.qualifiedName)

    enum class State { INITIAL, MAIN, CLOSED }

    fun process(file: File): YAMLDocument {
        file.inputStream().use {
            return process(it)
        }
    }

    fun process(inputStream: InputStream): YAMLDocument {
        return process(inputStream.reader())
    }

    fun process(reader: Reader): YAMLDocument {
        var state = State.INITIAL
        val brdr = if (reader is BufferedReader) reader else reader.buffered()
        val outerBlock = InitialBlock(0)
        var lineNumber = 0
        while (true) {
            ++lineNumber
            val line = brdr.readLine() ?: break
            when (state) {
                State.INITIAL -> {
                    when {
                        line.startsWith("%YAML") -> warn("%YAML directive ignored - $line")
                        line.startsWith('%') -> warn("Unrecognised directive ignored - $line")
                        line.startsWith("---") -> state = State.MAIN
                        line.startsWith("...") -> state = State.CLOSED
                        else -> {
                            val npt = NumberedParseText(lineNumber, line)
                            if (!npt.atEnd()) {
                                state = State.MAIN
                                outerBlock.processLine(npt)
                            }
                        }
                    }
                }
                State.MAIN -> {
                    when {
                        line.startsWith("...") -> state = State.CLOSED
                        else -> {
                            val npt = NumberedParseText(lineNumber, line)
                            if (npt.isExhausted)
                                outerBlock.processBlankLine(npt)
                            else
                                outerBlock.processLine(npt)
                        }
                    }
                }
                State.CLOSED -> {
                    if (line.trim().isNotEmpty())
                        fatal("Non-blank lines after close", NumberedParseText(lineNumber, ""))
                }
            }
        }
        val rootNode = outerBlock.conclude(NumberedParseText(lineNumber, ""))
        log.debug {
            val type = when (rootNode) {
                null -> "null"
                else -> rootNode::class.simpleName
            }
            "Parse complete; result is $type"
        }
        return YAMLDocument(rootNode)
    }

    private fun warn(message: String) {
        log.warn { "YAML Warning: $message" }
    }

    private fun fatal(message: String, npt: NumberedParseText): Nothing {
        val exception = YAMLException(message, npt)
        log.error { exception.message }
        throw exception
    }

    fun NumberedParseText.processPlainScalar(initial: String = ""): PlainScalar {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                matchColon() -> {
                    revert()
                    skipBackSpaces()
                    break
                }
                matchHash() -> {
                    revert()
                    skipBackSpaces()
                    break
                }
                else -> sb.append(char)
            }
        }
        return PlainScalar(sb.toString().trim())
    }

    fun NumberedParseText.processSingleQuotedScalar(initial: String = ""): SingleQuotedScalar {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                match('\'') -> {
                    if (match('\''))
                        sb.append('\'')
                    else
                        return SingleQuotedScalar(sb.toString(), true)
                }
                else -> sb.append(char)
            }
        }
        return SingleQuotedScalar(sb.toString(), false)
    }

    fun NumberedParseText.processDoubleQuotedScalar(initial: String = ""): DoubleQuotedScalar {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                match('"') -> return DoubleQuotedScalar(sb.toString(), true)
                match('\\') -> processBackslash(sb)
                else -> sb.append(char)
            }
        }
        return DoubleQuotedScalar(sb.toString(), false)
    }

    private fun NumberedParseText.processBackslash(sb: StringBuilder) {
        when {
            match('0') -> sb.append('\u0000')
            match('a') -> sb.append('\u0007')
            match('b') -> sb.append('\b')
            match('t') -> sb.append('\t')
            match('\t') -> sb.append('\t')
            match('n') -> sb.append('\n')
            match('v') -> sb.append('\u000B')
            match('f') -> sb.append('\u000C')
            match('e') -> sb.append('\u001B')
            match(' ') -> sb.append(' ')
            match('"') -> sb.append('"')
            match('/') -> sb.append('/')
            match('\\') -> sb.append('\\')
            match('N') -> sb.append('\u0085')
            match('_') -> sb.append('\u00A0')
            match('L') -> sb.append('\u2028')
            match('P') -> sb.append('\u2029')
            match('x') -> {
                if (!matchHexFixed(2))
                    fatal("Illegal hex value in double quoted scalar", this)
                sb.append(resultHexInt.toChar())
            }
            match('u') -> {
                if (!matchHexFixed(4))
                    fatal("Illegal unicode value in double quoted scalar", this)
                sb.append(resultHexInt.toChar())
            }
            else -> fatal("Illegal escape sequence in double quoted scalar", this)
        }
    }

    abstract class Block(val indent: Int) {
        abstract fun processLine(npt: NumberedParseText)
        open fun processBlankLine(npt: NumberedParseText) {}
        abstract fun conclude(npt: NumberedParseText): YAMLNode?
    }

    object ErrorBlock : Block(0) {

        override fun processLine(npt: NumberedParseText) = fatal("Should not happen", npt)

        override fun conclude(npt: NumberedParseText): YAMLNode? = fatal("Should not happen", npt)

    }

    class InitialBlock(indent: Int) : Block(indent) {

        enum class State { INITIAL, CHILD, CLOSED }

        private var state: State = State.INITIAL
        private var node: YAMLNode? = null
        private var child: Block = ErrorBlock

        override fun processLine(npt: NumberedParseText) {
            when (state) {
                State.INITIAL -> processFirstLine(npt)
                State.CHILD -> child.processLine(npt)
                State.CLOSED -> fatal("Unexpected state in YAML processing", npt)
            }
        }

        private fun processFirstLine(npt: NumberedParseText) {
            val initialIndex = npt.index
            val scalar = when {
                npt.atEnd() -> return
                npt.matchDash() -> {
                    child = if (npt.isExhausted || npt.matchHash())
                        SequenceBlock(initialIndex)
                    else
                        SequenceBlock(initialIndex, npt)
                    state = State.CHILD
                    return
                }
                npt.match('"') -> npt.processDoubleQuotedScalar()
                npt.match('\'') -> npt.processSingleQuotedScalar()
                else -> npt.processPlainScalar()
            }
            npt.skipSpaces()
            if (npt.matchColon()) {
                if (npt.atEnd()) {
                    child = MappingBlock(initialIndex, scalar.text)
                    state = State.CHILD
                }
                else {
                    child = MappingBlock(initialIndex, scalar.text, npt)
                    state = State.CHILD
                }
            }
            else { // single line scalar?
                if (scalar.terminated) {
                    node = scalar.getYAMLNode()
                    state = State.CLOSED
                }
                else {
                    child = ChildBlock(initialIndex, scalar)
                    state = State.CHILD
                }
            }
        }

        override fun processBlankLine(npt: NumberedParseText) {
            when (state) {
                State.INITIAL -> {}
                State.CHILD -> child.processBlankLine(npt)
                State.CLOSED -> {}
            }
        }

        override fun conclude(npt: NumberedParseText): YAMLNode? {
            when (state) {
                State.INITIAL -> {}
                State.CHILD -> node = child.conclude(npt)
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return node
        }

    }

    class MappingBlock private constructor(indent: Int) : Block(indent) {

        enum class State { KEY, CHILD, CLOSED }

        private var state: State = State.CHILD
        private var child: Block = ErrorBlock
        private val properties = ListMap<String, YAMLNode?>()
        private var key: String = ""

        constructor(indent: Int, key: String) : this(indent) {
            this.key = key
            child = InitialBlock(indent + 1)
        }

        constructor(indent: Int, key: String, npt: NumberedParseText) : this(indent) {
            this.key = key
            child = ChildBlock(indent + 1)
            child.processLine(npt)
        }

        override fun processLine(npt: NumberedParseText) {
            var effectiveIndent = npt.index
            if (npt.matchDash()) {
                effectiveIndent = npt.index
                npt.revert()
            }
            when (state) {
                State.KEY -> processKey(npt)
                State.CHILD -> {
                    if (effectiveIndent >= child.indent)
                        child.processLine(npt)
                    else {
                        properties[key] = child.conclude(npt)
                        state = State.KEY
                        processKey(npt)
                    }
                }
                State.CLOSED -> fatal("Unexpected state in YAML processing", npt)
            }
        }

        override fun processBlankLine(npt: NumberedParseText) {
            when (state) {
                State.KEY -> {}
                State.CHILD -> child.processBlankLine(npt)
                State.CLOSED -> {}
            }
        }

        private fun processKey(npt: NumberedParseText) {
            val scalar = when {
                npt.isExhausted -> return
                npt.match('#') -> return
                npt.match('"') -> npt.processDoubleQuotedScalar()
                npt.match('\'') -> npt.processSingleQuotedScalar()
                else -> npt.processPlainScalar()
            }
            npt.skipSpaces()
            if (npt.matchColon()) {
                key = scalar.text
                if (properties.containsKey(key))
                    fatal("Duplicate key in mapping - $key", npt)
                npt.skipSpaces()
                if (npt.isExhausted || npt.matchHash()) {
                    child = InitialBlock(indent + 1)
                    state = State.CHILD
                }
                else {
                    child = ChildBlock(indent + 1)
                    child.processLine(npt)
                    state = State.CHILD
                }
            }
            else
                fatal("Illegal key in mapping", npt)
        }

        override fun conclude(npt: NumberedParseText): YAMLNode? {
            when (state) {
                State.KEY -> {}
                State.CHILD -> properties[key] = child.conclude(npt)
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return YAMLMapping(properties)
        }

    }

    class SequenceBlock(indent: Int) : Block(indent) {

        enum class State { DASH, CHILD, CLOSED }

        private var state: State = State.DASH
        private val items = mutableListOf<YAMLNode?>()
        private var child: Block = ErrorBlock

        constructor(indent: Int, npt: NumberedParseText) : this(indent) {
            child = InitialBlock(indent + 2)
            child.processLine(npt)
            state = State.CHILD
        }

        override fun processLine(npt: NumberedParseText) {
            when (state) {
                State.DASH -> processDash(npt)
                State.CHILD -> {
                    if (npt.index >= child.indent)
                        child.processLine(npt)
                    else {
                        val childValue = child.conclude(npt)
                        items.add(childValue)
                        state = State.DASH
                        processDash(npt)
                    }
                }
                State.CLOSED -> fatal("Unexpected state in YAML processing", npt)
            }
        }

        override fun processBlankLine(npt: NumberedParseText) {
            when (state) {
                State.DASH -> {}
                State.CHILD -> child.processBlankLine(npt)
                State.CLOSED -> {}
            }
        }

        private fun processDash(npt: NumberedParseText) {
            if (npt.matchDash()) {
                if (npt.isExhausted || npt.matchHash()) {
                    child = InitialBlock(indent + 1)
                }
                else {
                    child = InitialBlock(npt.index)
                    child.processLine(npt)
                }
                state = State.CHILD
            }
        }

        override fun conclude(npt: NumberedParseText): YAMLNode? {
            when (state) {
                State.DASH -> {}
                State.CHILD -> {
                    val childValue = child.conclude(npt)
                    items.add(childValue)
                }
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return YAMLSequence(items)
        }

    }

    class ChildBlock(indent: Int) : Block(indent) {

        enum class State { INITIAL, CONTINUATION, CLOSED }

        private var state: State = State.INITIAL
        private var child: Child = PlainScalar("")
        private var node: YAMLNode? = null

        constructor(indent: Int, scalar: Child) : this(indent) {
            this.child = scalar
            state = State.CONTINUATION
        }

        override fun processLine(npt: NumberedParseText) {
            child = when (state) {
                State.INITIAL -> {
                    when {
                        npt.isExhausted -> return
                        npt.match('#') -> return
                        npt.match('"') -> npt.processDoubleQuotedScalar()
                        npt.match('\'') -> npt.processSingleQuotedScalar()
                        npt.match('|') -> {
                            val chomping = npt.determineChomping()
                            npt.skipSpaces()
                            if (!(npt.matchHash() || npt.isExhausted))
                                fatal("Illegal literal block header", npt)
                            LiteralBlockScalar(indent, chomping)
                        }
                        npt.match('>') -> {
                            val chomping = npt.determineChomping()
                            npt.skipSpaces()
                            if (!(npt.matchHash() || npt.isExhausted))
                                fatal("Illegal folded block header", npt)
                            FoldedBlockScalar(indent, chomping)
                        }
                        // this could be where we check for flow sequence or mapping
                        else -> npt.processPlainScalar()
                    }
                }
                State.CONTINUATION -> child.continuation(npt)
                State.CLOSED -> fatal("Unexpected state in YAML processing", npt)
            }
            npt.skipSpaces()
            if (npt.isExhausted || npt.matchHash()) {
                if (child.terminated) {
                    node = child.getYAMLNode()
                    state = State.CLOSED
                }
                else
                    state = State.CONTINUATION
            }
            else
                fatal("Illegal data following scalar", npt)
        }

        override fun processBlankLine(npt: NumberedParseText) {
            when (state) {
                State.INITIAL -> {}
                State.CONTINUATION -> child.continuation(npt)
                State.CLOSED -> {}
            }
        }

        override fun conclude(npt: NumberedParseText): YAMLNode? {
            when (state) {
                State.INITIAL -> {}
                State.CONTINUATION -> {
                    if (child.complete)
                        node = child.getYAMLNode()
                    else
                        fatal("Incomplete scalar", npt)
                }
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return node
        }

        private fun NumberedParseText.determineChomping(): BlockScalar.Chomping = when {
            match('-') -> BlockScalar.Chomping.STRIP
            match('+') -> BlockScalar.Chomping.KEEP
            else -> BlockScalar.Chomping.CLIP
        }

    }

    abstract class Child(val terminated: Boolean) { // rename Nested ??? or Flow ???

        open val complete: Boolean
            get() = terminated

        abstract fun getYAMLNode(): YAMLNode?

        abstract fun continuation(npt: NumberedParseText): Child

    }

    abstract class FlowScalar(val text: String, terminated: Boolean) : Child(terminated) {

        override fun getYAMLNode(): YAMLNode? = YAMLString(text)

    }

    class PlainScalar(text: String) : FlowScalar(text, false) {

        override val complete: Boolean
            get() = true

        override fun continuation(npt: NumberedParseText): Child {
            return npt.processPlainScalar("$text ")
        }

        override fun getYAMLNode(): YAMLNode? {
            if (text == "null" || text == "Null" || text == "NULL" || text == "~")
                return null
            if (text == "true" || text == "True" || text == "TRUE")
                return YAMLBoolean.TRUE
            if (text == "false" || text == "False" || text == "FALSE")
                return YAMLBoolean.FALSE
            if (text.startsWith("0o") && text.length > 2 && text.drop(2).all { it in '0'..'7' })
                return intOrLong(text.drop(2).toLong(8))
            if (text.startsWith("0x") && text.length > 2 &&
                    text.drop(2).all { it in '0'..'9' || it in 'A'..'F' || it in 'a'..'f' })
                return intOrLong(text.drop(2).toLong(16))
            if (text.matchesInteger())
                return intOrLong(text.toLong())
            if (text.matchesDecimal())
                return YAMLDecimal(text)
            return YAMLString(text)
        }

        private fun intOrLong(value: Long): YAMLScalar {
            return if (value in Int.MIN_VALUE..Int.MAX_VALUE)
                YAMLInt(value.toInt())
            else
                YAMLLong(value)
        }

        private fun String.matchesInteger(): Boolean {
            return ParseText(this).let {
                it.match('+') || it.match('-')
                it.matchDec() && it.isExhausted
            }
        }

        private fun String.matchesDecimal(): Boolean {
            val pt = ParseText(this)
            pt.match('+') || pt.match('-')
            if (pt.match('.')) {
                if (!pt.matchDec())
                    return false
            }
            else {
                if (!pt.matchDec())
                    return false
                if (pt.match('.'))
                    pt.matchDec()
            }
            if (pt.match('e') || pt.match('E')) {
                pt.match('+') || pt.match('-')
                if (!pt.matchDec())
                    return false
            }
            return pt.isExhausted
        }

    }

    class DoubleQuotedScalar(text: String, terminated: Boolean) : FlowScalar(text, terminated) {

        override fun continuation(npt: NumberedParseText): DoubleQuotedScalar {
            val space = if (text.endsWith(' ')) "" else " "
            return npt.processDoubleQuotedScalar("$text$space")
        }

    }

    class SingleQuotedScalar(text: String, terminated: Boolean) : FlowScalar(text, terminated) {

        override fun continuation(npt: NumberedParseText): SingleQuotedScalar {
            val space = if (text.endsWith(' ')) "" else " "
            return npt.processSingleQuotedScalar("$text$space")
        }

    }

    abstract class BlockScalar(private var indent: Int, private val chomping: Chomping) : Child(false) {

        enum class Chomping { STRIP, KEEP, CLIP }

        enum class State { INITIAL, CONTINUATION }

        private var state: State = State.INITIAL
        val text = StringBuilder()

        override val complete: Boolean
            get() = true

        abstract fun appendText(string: String)

        override fun continuation(npt: NumberedParseText): BlockScalar {
            when (state) {
                State.INITIAL -> {
                    if (npt.index > indent)
                        indent = npt.index
                    if (!npt.isExhausted) {
                        state = State.CONTINUATION
                        npt.skipToEnd()
                        appendText(npt.resultString)
                    }
                }
                State.CONTINUATION -> {
                    if (!npt.isExhausted && npt.index < indent)
                        fatal("Bad indentation in block scalar", npt)
                    if (npt.index > indent)
                        npt.index = indent
                    npt.skipToEnd()
                    appendText(npt.resultString)
                }
            }
            return this
        }

        override fun getYAMLNode(): YAMLString {
            val sb = StringBuilder(text)
            when (chomping) {
                Chomping.STRIP -> sb.strip()
                Chomping.CLIP -> {
                    sb.strip()
                    sb.append("\n")
                }
                Chomping.KEEP -> {}
            }
            return YAMLString(sb.toString())
        }

        private fun StringBuilder.strip() {
            while (endsWith('\n'))
                setLength(length - 1)
        }

    }

    class LiteralBlockScalar(indent: Int, chomping: Chomping) : BlockScalar(indent, chomping) {

        override fun appendText(string: String) {
            text.append(string)
            text.append('\n')
        }

    }

    class FoldedBlockScalar(indent: Int, chomping: Chomping) : BlockScalar(indent, chomping) {

        override fun appendText(string: String) {
            if (text.endsWith('\n')) {
                text.setLength(text.length - 1)
                text.append(' ')
            }
            text.append(string)
            text.append('\n')
        }

    }

    class YAMLException(message: String, npt: NumberedParseText, e: Exception? = null) :
            JSONException("$message at ${npt.lineNumber}:${npt.index}", e)

}
