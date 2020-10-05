/*
 * @(#) LoggingTest.java
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

import kotlin.test.Test
import kotlin.test.expect

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

import net.pwall.log.ConsoleLogger
import net.pwall.log.Level

class LoggingTest {

    @Test fun `should output message on completion`() {
        val baos = ByteArrayOutputStream()
        val printStream = PrintStream(baos)
        val saveLogger = YAMLSimple.log
        try {
            YAMLSimple.log = ConsoleLogger("LoggingTest", Level.DEBUG, printStream)
            val emptyFile = File("src/test/resources/empty.yaml")
            val result = YAMLSimple.process(emptyFile)
            expect(null) { result.rootNode }
            expect("LoggingTest:DEBUG: Parse complete; result is null\n") { String(baos.toByteArray()).drop(13) }
        }
        finally {
            YAMLSimple.log = saveLogger
        }
    }

}
