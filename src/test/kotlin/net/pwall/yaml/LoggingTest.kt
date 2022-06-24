/*
 * @(#) LoggingTest.java
 *
 * yaml-simple  Simple implementation of YAML
 * Copyright (c) 2020, 2022 Peter Wall
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

import java.io.File

import net.pwall.log.Level
import net.pwall.log.LogList

class LoggingTest {

    @Test fun `should output message on completion`() {
        LogList().use { // relies on DEBUG level being enabled (see logback.xml)
            val emptyFile = File("src/test/resources/empty.yaml")
            val result = YAMLSimple.process(emptyFile)
            expect(null) { result.rootNode }
            val list = it.toList()
            expect(1) { list.size }
            with(list[0]) {
                expect("net.pwall.yaml.YAMLSimple") { name }
                expect(Level.DEBUG) { level }
                expect("Parse complete; result is null") { message }
            }
        }
    }

}
