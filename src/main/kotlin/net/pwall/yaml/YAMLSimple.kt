/*
 * @(#) YAMLSimple.java
 *
 * yaml-simple  Simple implementation of YAML
 * Copyright (c) 2020, 2021 Peter Wall
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

import java.io.File
import java.io.InputStream
import java.io.Reader

import net.pwall.log.Logger
import net.pwall.log.LoggerFactory
import net.pwall.util.ListMap
import net.pwall.util.ParseText
import net.pwall.util.Strings
import net.pwall.yaml.parser.Line

/**
 * A simple YAML processor.
 *
 * @author  Peter Wall
 */
object YAMLSimple {

    var log: Logger = LoggerFactory.getDefaultLogger(YAMLSimple::class.qualifiedName)

    enum class State { INITIAL, DIRECTIVE, MAIN, ENDED }

    /**
     * Process a [File] to a YAML document.
     *
     * @param   file    a file containing a YAML document
     * @return          the parsed YAML document
     */
    fun process(file: File): YAMLDocument {
        file.inputStream().use {
            return process(it)
        }
    }

    /**
     * Process a [InputStream] to a YAML document.
     *
     * @param   inputStream an [InputStream] containing a YAML document
     * @return              the parsed YAML document
     */
    fun process(inputStream: InputStream): YAMLDocument {
        return process(inputStream.reader())
    }

    /**
     * Process a [Reader] to a YAML document.
     *
     * @param   reader  a [Reader] containing a YAML document
     * @return          the parsed YAML document
     */
    fun process(reader: Reader): YAMLDocument {
        var state = State.INITIAL
        var version: Pair<Int, Int>? = null
        val outerBlock = InitialBlock(0)
        var lineNumber = 0
        reader.forEachLine { text ->
            val line = Line(++lineNumber, text)
            when (state) {
                State.INITIAL -> {
                    when {
                        text.startsWith("%YAML") -> {
                            version = processYAMLDirective(line)
                            state = State.DIRECTIVE
                        }
                        text.startsWith('%') -> {
                            warn("Unrecognised directive ignored - $text")
                            state = State.DIRECTIVE
                        }
                        text.startsWith("---") -> state = State.MAIN
                        text.startsWith("...") -> state = State.ENDED
                        !line.atEnd() -> {
                            state = State.MAIN
                            outerBlock.processLine(line)
                        }
                    }
                }
                State.DIRECTIVE -> {
                    when {
                        text.startsWith("%YAML") -> fatal("Duplicate or misplaced %YAML directive", line)
                        text.startsWith('%') -> warn("Unrecognised directive ignored - $text")
                        text.startsWith("---") -> state = State.MAIN
                        text.startsWith("...") -> state = State.ENDED
                        !line.atEnd() -> fatal("Illegal data following directive(s)", line)
                    }
                }
                State.MAIN -> {
                    when {
                        text.startsWith("...") -> state = State.ENDED
                        text.startsWith("---") -> fatal("Multiple documents not allowed", line)
                        line.isExhausted || line.matchHash() -> outerBlock.processBlankLine(line)
                        else -> outerBlock.processLine(line)
                    }
                }
                State.ENDED -> {
                    if (text.trim().isNotEmpty())
                        fatal("Non-blank lines after end of document", line)
                }
            }
        }
        val rootNode = outerBlock.conclude(Line(lineNumber, ""))
        log.debug {
            val type = when (rootNode) {
                null -> "null"
                else -> rootNode::class.simpleName
            }
            "Parse complete; result is $type"
        }
        return version?.let { YAMLDocument(rootNode, it.first, it.second) } ?: YAMLDocument(rootNode)
    }

    private fun processYAMLDirective(line: Line): Pair<Int, Int> {
        line.skip(5)
        if (!line.matchSpaces() || !line.matchDec())
            fatal("Illegal %YAML directive", line)
        val majorVersion = line.resultInt
        if (!line.match('.') || !line.matchDec())
            fatal("Illegal version number on %YAML directive", line)
        val minorVersion = line.resultInt
        if (majorVersion != 1)
            fatal("%YAML version must be 1.x", line)
        line.skipSpaces()
        if (!line.atEnd())
            fatal("Illegal data on %YAML directive", line)
        if (minorVersion !in 1..2)
            warn("Unexpected YAML version - $majorVersion.$minorVersion")
        return majorVersion to minorVersion
    }

    private fun warn(message: String) {
        log.warn { "YAML Warning: $message" }
    }

    private fun fatal(message: String, line: Line): Nothing {
        val exception = YAMLException(message, line)
        log.error { exception.message }
        throw exception
    }

    fun Line.processPlainScalar(initial: String = ""): PlainScalar {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                matchColon() -> {
                    revert()
                    skipBackSpaces() // ???
                    break
                }
                matchHash() -> {
                    skipBackSpaces() // ???
                    break
                }
                else -> sb.append(char)
            }
        }
        return PlainScalar(sb.toString().trim())
    }

    fun Line.processFlowNode(initial: String = ""): FlowNode {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                matchAnyOf("[]{},") -> {
                    revert()
                    return FlowNode(sb.toString().trim()).also { it.terminated = true }
                }
                matchColon() -> {
                    revert()
                    skipBackSpaces() // ???
                    break
                }
                matchHash() -> {
                    skipBackSpaces() // ???
                    break
                }
                else -> sb.append(char)
            }
        }
        return FlowNode(sb.toString().trim())
    }

    fun Line.processSingleQuotedScalar(initial: String = ""): SingleQuotedScalar {
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

    fun Line.processDoubleQuotedScalar(initial: String = ""): DoubleQuotedScalar {
        val sb = StringBuilder(initial)
        while (!isExhausted) {
            when {
                match('"') -> return DoubleQuotedScalar(sb.toString(), true)
                match('\\') -> {
                    if (isExhausted)
                        return DoubleQuotedScalar(sb.toString(), false, escapedNewline =  true)
                    processBackslash(sb)
                }
                else -> sb.append(char)
            }
        }
        return DoubleQuotedScalar(sb.toString(), false)
    }

    private fun Line.processBackslash(sb: StringBuilder) {
        when {
            match('0') -> sb.append('\u0000')
            match('a') -> sb.append('\u0007')
            match('b') -> sb.append('\b')
            match('t') -> sb.append('\t')
            match('\t') -> sb.append('\t')
            match('n') -> sb.append('\n')
            match('v') -> sb.append('\u000B')
            match('f') -> sb.append('\u000C')
            match('r') -> sb.append('\r')
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
            match('U') -> {
                if (!matchHexFixed(8))
                    fatal("Illegal unicode value in double quoted scalar", this)
                try {
                    Strings.appendUTF16(sb, resultHexInt)
                }
                catch (e: IllegalArgumentException) {
                    fatal("Illegal 32-bit unicode value in double quoted scalar", this)
                }
            }
            else -> fatal("Illegal escape sequence in double quoted scalar", this)
        }
    }

    abstract class Block(val indent: Int) {
        abstract fun processLine(line: Line)
        open fun processBlankLine(line: Line) {}
        abstract fun conclude(line: Line): YAMLNode?
    }

    object ErrorBlock : Block(0) {

        override fun processLine(line: Line) = fatal("Should not happen", line)

        override fun conclude(line: Line) = fatal("Should not happen", line)

    }

    class InitialBlock(indent: Int) : Block(indent) {

        enum class State { INITIAL, CHILD, CLOSED }

        private var state: State = State.INITIAL
        private var node: YAMLNode? = null
        private var child: Block = ErrorBlock

        override fun processLine(line: Line) {
            when (state) {
                State.INITIAL -> processFirstLine(line)
                State.CHILD -> child.processLine(line)
                State.CLOSED -> fatal("Unexpected state in YAML processing", line)
            }
        }

        private fun processFirstLine(line: Line) {
            val initialIndex = line.index
            val scalar = when {
                line.atEnd() -> return
                line.matchDash() -> {
                    child = SequenceBlock(initialIndex)
                    if (!line.atEnd())
                        child.processLine(line)
                    state = State.CHILD
                    return
                }
                line.match('"') -> line.processDoubleQuotedScalar()
                line.match('\'') -> line.processSingleQuotedScalar()
                line.match('[') -> FlowSequence(false).also { it.continuation(line) }
                line.match('{') -> FlowMapping(false).also { it.continuation(line) }
                line.match('?') -> {
                    line.skipSpaces()
                    child = ExplicitMappingBlock(initialIndex)
                    if (!line.atEnd())
                        child.processLine(line)
                    state = State.CHILD
                    return
                }
                line.match(':') -> fatal("Mapping value without key", line)
                else -> line.processPlainScalar()
            }
            line.skipSpaces()
            if (line.matchColon()) {
                if (line.atEnd()) {
                    child = MappingBlock(initialIndex, scalar.text.toString())
                    state = State.CHILD
                }
                else {
                    child = MappingBlock(initialIndex, scalar.text.toString(), line)
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

        override fun processBlankLine(line: Line) {
            when (state) {
                State.INITIAL -> {}
                State.CHILD -> child.processBlankLine(line)
                State.CLOSED -> {}
            }
        }

        override fun conclude(line: Line): YAMLNode? {
            when (state) {
                State.INITIAL -> {}
                State.CHILD -> node = child.conclude(line)
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

        constructor(indent: Int, key: String, line: Line) : this(indent) {
            this.key = key
            child = ChildBlock(indent + 1)
            child.processLine(line)
        }

        override fun processLine(line: Line) {
            var effectiveIndent = line.index
            if (line.matchDash()) {
                effectiveIndent = line.index
                line.revert()
            }
            when (state) {
                State.KEY -> processKey(line)
                State.CHILD -> {
                    if (effectiveIndent >= child.indent)
                        child.processLine(line)
                    else {
                        properties[key] = child.conclude(line)
                        state = State.KEY
                        processKey(line)
                    }
                }
                State.CLOSED -> fatal("Unexpected state in YAML processing", line)
            }
        }

        override fun processBlankLine(line: Line) {
            when (state) {
                State.KEY -> {}
                State.CHILD -> child.processBlankLine(line)
                State.CLOSED -> {}
            }
        }

        private fun processKey(line: Line) {
            val scalar = when {
                line.isExhausted -> return
                line.match('#') -> return
                line.match('"') -> line.processDoubleQuotedScalar()
                line.match('\'') -> line.processSingleQuotedScalar()
                else -> line.processPlainScalar()
            }
            line.skipSpaces()
            if (line.matchColon()) {
                key = scalar.text
                if (properties.containsKey(key))
                    fatal("Duplicate key in mapping - $key", line)
                line.skipSpaces()
                if (line.isExhausted || line.matchHash()) {
                    child = InitialBlock(indent + 1)
                    state = State.CHILD
                }
                else {
                    child = ChildBlock(indent + 1)
                    child.processLine(line)
                    state = State.CHILD
                }
            }
            else
                fatal("Illegal key in mapping", line)
        }

        override fun conclude(line: Line): YAMLMapping {
            when (state) {
                State.KEY -> {}
                State.CHILD -> properties[key] = child.conclude(line)
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return YAMLMapping(properties)
        }

    }

    class ExplicitMappingBlock(indent: Int) : Block(indent) {

        enum class State { QM, QM_CHILD, COLON, COLON_CHILD, CLOSED }

        private var state: State = State.QM_CHILD
        private var child: Block = InitialBlock(indent + 1)
        private val properties = ListMap<String, YAMLNode?>()
        private var key: String = ""

        override fun processLine(line: Line) {
            when (state) {
                State.QM -> processDetail(line, '?', State.QM_CHILD)
                State.QM_CHILD -> {
                    if (line.index >= child.indent)
                        child.processLine(line)
                    else {
                        key = child.conclude(line)?.let { if (it is YAMLString) it.value else it.toString() } ?: "null"
                        state = State.COLON
                        processDetail(line, ':', State.COLON_CHILD)
                    }
                }
                State.COLON -> processDetail(line, ':', State.COLON_CHILD)
                State.COLON_CHILD -> {
                    if (line.index >= child.indent)
                        child.processLine(line)
                    else {
                        properties[key] = child.conclude(line)
                        state = State.QM
                        processDetail(line, '?', State.QM_CHILD)
                    }
                }
                State.CLOSED -> fatal("Unexpected state in YAML processing", line)
            }
        }

        override fun processBlankLine(line: Line) {
            when (state) {
                State.QM -> {}
                State.QM_CHILD -> child.processBlankLine(line)
                State.COLON -> {}
                State.COLON_CHILD -> child.processBlankLine(line)
                State.CLOSED -> {}
            }
        }

        private fun processDetail(line: Line, indicator: Char, targetState: State) {
            if (line.match(indicator)) {
                line.skipSpaces()
                if (line.atEnd())
                    child = InitialBlock(indent + 1)
                else {
                    child = InitialBlock(line.index)
                    child.processLine(line)
                }
                state = targetState
            }
            else
                fatal("Unexpected content in block mapping", line)
        }

        override fun conclude(line: Line): YAMLMapping {
            when (state) {
                State.QM -> {}
                State.QM_CHILD,
                State.COLON -> fatal("Block mapping value missing", line)
                State.COLON_CHILD -> properties[key] = child.conclude(line)
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return YAMLMapping(properties)
        }

    }

    class SequenceBlock(indent: Int) : Block(indent) {

        enum class State { DASH, CHILD, CLOSED }

        private var state: State = State.CHILD
        private val items = mutableListOf<YAMLNode?>()
        private var child: Block = InitialBlock(indent + 2)

        override fun processLine(line: Line) {
            when (state) {
                State.DASH -> processDash(line)
                State.CHILD -> {
                    if (line.index >= child.indent)
                        child.processLine(line)
                    else {
                        val childValue = child.conclude(line)
                        items.add(childValue)
                        state = State.DASH
                        processDash(line)
                    }
                }
                State.CLOSED -> fatal("Unexpected state in YAML processing", line)
            }
        }

        override fun processBlankLine(line: Line) {
            when (state) {
                State.DASH -> {}
                State.CHILD -> child.processBlankLine(line)
                State.CLOSED -> {}
            }
        }

        private fun processDash(line: Line) {
            if (line.matchDash()) {
                if (line.atEnd())
                    child = InitialBlock(indent + 2)
                else {
                    child = InitialBlock(line.index)
                    child.processLine(line)
                }
                state = State.CHILD
            }
            else
                fatal("Unexpected content in block sequence", line)
        }

        override fun conclude(line: Line): YAMLSequence {
            when (state) {
                State.DASH -> {}
                State.CHILD -> items.add(child.conclude(line))
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

        override fun processLine(line: Line) {
            child = when (state) {
                State.INITIAL -> {
                    when {
                        line.isExhausted -> return
                        line.match('#') -> return
                        line.match('"') -> line.processDoubleQuotedScalar()
                        line.match('\'') -> line.processSingleQuotedScalar()
                        line.match('|') -> {
                            val chomping = line.determineChomping()
                            line.skipSpaces()
                            if (!(line.matchHash() || line.isExhausted))
                                fatal("Illegal literal block header", line)
                            LiteralBlockScalar(indent, chomping)
                        }
                        line.match('>') -> {
                            val chomping = line.determineChomping()
                            line.skipSpaces()
                            if (!(line.matchHash() || line.isExhausted))
                                fatal("Illegal folded block header", line)
                            FoldedBlockScalar(indent, chomping)
                        }
                        line.match('[') -> FlowSequence(false).also { it.continuation(line) }
                        line.match('{') -> FlowMapping(false).also { it.continuation(line) }
                        line.match('?') -> fatal("Can't handle standalone mapping keys", line)
                        line.match(':') -> fatal("Can't handle standalone mapping values", line)
                        else -> line.processPlainScalar()
                    }
                }
                State.CONTINUATION -> child.continuation(line)
                State.CLOSED -> fatal("Unexpected state in YAML processing", line)
            }
            line.skipSpaces()
            if (line.isExhausted || line.matchHash()) {
                if (child.terminated) {
                    node = child.getYAMLNode()
                    state = State.CLOSED
                }
                else
                    state = State.CONTINUATION
            }
            else
                fatal("Illegal data following scalar", line)
        }

        override fun processBlankLine(line: Line) {
            when (state) {
                State.INITIAL -> {}
                State.CONTINUATION -> child.continuation(line)
                State.CLOSED -> {}
            }
        }

        override fun conclude(line: Line): YAMLNode? {
            when (state) {
                State.INITIAL -> {}
                State.CONTINUATION -> {
                    if (child.complete)
                        node = child.getYAMLNode()
                    else
                        fatal("Incomplete scalar", line)
                }
                State.CLOSED -> {}
            }
            state = State.CLOSED
            return node
        }

        private fun Line.determineChomping(): BlockScalar.Chomping = when {
            match('-') -> BlockScalar.Chomping.STRIP
            match('+') -> BlockScalar.Chomping.KEEP
            else -> BlockScalar.Chomping.CLIP
        }

    }

    abstract class Child(var terminated: Boolean) { // rename Nested ??? or Flow ???

        open val complete: Boolean
            get() = terminated

        abstract val text: CharSequence

        abstract fun getYAMLNode(): YAMLNode?

        abstract fun continuation(line: Line): Child

    }

    class FlowSequence(terminated: Boolean) : Child(terminated) {

        enum class State { ITEM, CONTINUATION, COMMA, CLOSED }

        override val text: CharSequence
            get() = getYAMLNode().toString()

        private var state: State = State.ITEM
        private var child: Child = FlowNode("")
        private var key: String? = null
        private val items = mutableListOf<YAMLNode?>()

        private fun processLine(line: Line) {
            while (!line.atEnd()) {
                if (state != State.COMMA) {
                    when (state) {
                        State.ITEM -> {
                            child = when {
                                line.match('"') -> line.processDoubleQuotedScalar()
                                line.match('\'') -> line.processSingleQuotedScalar()
                                line.match('[') -> FlowSequence(false).also { it.continuation(line) }
                                line.match('{') -> FlowMapping(false).also { it.continuation(line) }
                                else -> line.processFlowNode()
                            }
                        }
                        State.CONTINUATION -> child = child.continuation(line)
                        else -> {}
                    }
                    line.skipSpaces()
                    if (line.atEnd()) {
                        state = when {
                            child.terminated -> State.COMMA
                            else -> State.CONTINUATION
                        }
                        break
                    }
                }
                when {
                    line.match(']') -> {
                        if (key == null)
                            child.getYAMLNode()?.let { items.add(it) }
                        else
                            items.add(YAMLMapping(mapOf(key to child.getYAMLNode())))
                        terminated = true
                        state = State.CLOSED
                        break
                    }
                    line.matchColon() -> {
                        key = child.getYAMLNode().toString()
                        state = State.ITEM
                    }
                    line.match(',') -> {
                        if (key == null)
                            items.add(child.getYAMLNode())
                        else
                            items.add(YAMLMapping(mapOf(key to child.getYAMLNode())))
                        key = null
                        state = State.ITEM
                    }
                    else -> fatal("Unexpected character in flow sequence", line)
                }
            }
        }

        override fun continuation(line: Line): Child {
            if (state != State.CLOSED)
                processLine(line)
            return this
        }

        override fun getYAMLNode() = YAMLSequence(items)

    }

    class FlowMapping(terminated: Boolean) : Child(terminated) {

        enum class State { ITEM, CONTINUATION, COMMA, CLOSED }

        private var state: State = State.ITEM
        private var child: Child = FlowNode("")
        private var key: String? = null
        private val properties = LinkedHashMap<String, YAMLNode?>()

        override val text: CharSequence
            get() = getYAMLNode().toString()

        override fun continuation(line: Line): Child {
            if (state != State.CLOSED)
                processLine(line)
            return this
        }

        override fun getYAMLNode() = YAMLMapping(properties)

        private fun processLine(line: Line) {
            while (!line.atEnd()) {
                if (state != State.COMMA) {
                    when (state) {
                        State.ITEM -> {
                            child = when {
                                line.match('"') -> line.processDoubleQuotedScalar()
                                line.match('\'') -> line.processSingleQuotedScalar()
                                line.match('[') -> FlowSequence(false).also { it.continuation(line) }
                                line.match('{') -> FlowMapping(false).also { it.continuation(line) }
                                else -> line.processFlowNode()
                            }
                        }
                        State.CONTINUATION -> child = child.continuation(line)
                        else -> {}
                    }
                    line.skipSpaces()
                    if (line.atEnd()) {
                        state = when {
                            child.terminated -> State.COMMA
                            else -> State.CONTINUATION
                        }
                        break
                    }
                }
                when {
                    line.match('}') -> {
                        if (key == null) {
                            if (child.getYAMLNode() != null)
                                fatal("Unexpected end of flow mapping", line)
                        }
                        else
                            properties[key.toString()] = child.getYAMLNode()
                        terminated = true
                        state = State.CLOSED
                        break
                    }
                    line.matchColon() || child is DoubleQuotedScalar && line.match(':') -> {
                        key = child.getYAMLNode().toString()
                        state = State.ITEM
                    }
                    line.match(',') -> {
                        if (key == null)
                            fatal("Key missing in flow mapping", line)
                        properties[key.toString()] = child.getYAMLNode()
                        key = null
                        state = State.ITEM
                    }
                    else -> fatal("Unexpected character in flow mapping", line)
                }
            }
        }

    }

    abstract class FlowScalar(override val text: String, terminated: Boolean) : Child(terminated) {

        override fun getYAMLNode(): YAMLNode? = YAMLString(text)

    }

    open class PlainScalar(text: String) : FlowScalar(text, false) {

        override val complete: Boolean
            get() = true

        override fun continuation(line: Line): Child {
            return line.processPlainScalar("$text ")
        }

        override fun getYAMLNode(): YAMLNode? {
            if (text.isEmpty() || text == "null" || text == "Null" || text == "NULL" || text == "~")
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

    class FlowNode(text: String) : PlainScalar(text) {

        override fun continuation(line: Line): Child {
            return line.processFlowNode("$text ")
        }

    }

    class DoubleQuotedScalar(text: String, terminated: Boolean, private val escapedNewline: Boolean = false) :
            FlowScalar(text, terminated) {

        override fun continuation(line: Line): DoubleQuotedScalar {
            val space = if (escapedNewline || text.endsWith(' ')) "" else " "
            return line.processDoubleQuotedScalar("$text$space")
        }

    }

    class SingleQuotedScalar(text: String, terminated: Boolean) : FlowScalar(text, terminated) {

        override fun continuation(line: Line): SingleQuotedScalar {
            val space = if (text.endsWith(' ')) "" else " "
            return line.processSingleQuotedScalar("$text$space")
        }

    }

    abstract class BlockScalar(private var indent: Int, private val chomping: Chomping) : Child(false) {

        enum class Chomping { STRIP, KEEP, CLIP }

        enum class State { INITIAL, CONTINUATION }

        private var state: State = State.INITIAL
        override val text = StringBuilder()

        override val complete: Boolean
            get() = true

        abstract fun appendText(string: String)

        override fun continuation(line: Line): BlockScalar {
            when (state) {
                State.INITIAL -> {
                    if (line.index > indent)
                        indent = line.index
                    if (!line.isExhausted) {
                        state = State.CONTINUATION
                        line.skipToEnd()
                        appendText(line.resultString)
                    }
                }
                State.CONTINUATION -> {
                    if (line.isExhausted) {
                        if (line.index > indent) {
                            line.start = indent
                            appendText(line.resultString)
                        }
                        else
                            appendText("")
                    }
                    else {
                        if (line.index < indent) {
                            if (!line.matchHash())
                                fatal("Bad indentation in block scalar", line)
                        }
                        else {
                            if (line.index > indent)
                                line.index = indent
                            line.skipToEnd()
                            appendText(line.resultString)
                        }
                    }
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
                while (text.endsWith('\n'))
                    text.setLength(text.length - 1)
                text.append(' ')
            }
            text.append(string)
            text.append('\n')
        }

    }

}
