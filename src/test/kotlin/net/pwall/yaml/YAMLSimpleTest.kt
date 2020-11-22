/*
 * @(#) YAMLSimpleTest.java
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
import kotlin.test.fail

import java.io.File
import java.math.BigDecimal
import net.pwall.json.JSONFormat

import net.pwall.log.Logger
import net.pwall.log.LoggerFactory

class YAMLSimpleTest {

    @Test fun `should return null document for empty file`() {
        val emptyFile = File("src/test/resources/empty.yaml")
        val result = YAMLSimple.process(emptyFile)
        expect(null) { result.rootNode }
    }

    @Test fun `should return null document for empty file as InputStream`() {
        val inputStream = File("src/test/resources/empty.yaml").inputStream()
        val result = YAMLSimple.process(inputStream)
        expect(null) { result.rootNode }
    }

    @Test fun `should return null document for empty file as Reader`() {
        val reader = File("src/test/resources/empty.yaml").reader()
        val result = YAMLSimple.process(reader)
        expect(null) { result.rootNode }
    }

    @Test fun `should process plain scalar`() {
        val file = File("src/test/resources/plainscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("http://pwall.net/schema.json#/aaa") { (result.rootNode as YAMLString).get() }
    }

    @Test fun `should process double quoted scalar`() {
        val file = File("src/test/resources/doublequotedscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("a b \n \r \" A A \u2014 A \uD83D\uDE02") { (result.rootNode as YAMLString).get() }
    }

    @Test fun `should process multi-line scalar`() {
        val file = File("src/test/resources/multilinescalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc def ghi") { (result.rootNode as YAMLString).get() }
    }

    @Test fun `should process integer scalar`() {
        val file = File("src/test/resources/integerscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(123) { (result.rootNode as YAMLInt).get() }
    }

    @Test fun `should process decimal scalar`() {
        val file = File("src/test/resources/decimalscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(BigDecimal("12345.67")) { (result.rootNode as YAMLDecimal).get() }
    }

    @Test fun `should process simple key-value`() {
        val file = File("src/test/resources/keyvalue.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("value") { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.get() }
    }

    @Test fun `should process simple key-integer`() {
        val file = File("src/test/resources/keyinteger.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(123) { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLInt)?.get() }
    }

    @Test fun `should process simple block property`() {
        val file = File("src/test/resources/keyblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("data") { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.get() }
    }

    @Test fun `should process nested block property`() {
        val file = File("src/test/resources/nestedblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            expect(1) { it.size }
            (it["key"] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect("inner") { (it["nested"] as? YAMLString)?.get() }
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process multiple properties`() {
        val file = File("src/test/resources/multipleproperties.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            expect(3) { it.size }
            expect("abc") { (it["prop1"] as? YAMLString)?.get() }
            expect(" X ") { (it["prop2"] as? YAMLString)?.get() }
            expect(null) { it["prop3"] }
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array with single item`() {
        val file = File("src/test/resources/array1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { ((result.rootNode as? YAMLSequence)?.get(0) as? YAMLString)?.get() }
    }

    @Test fun `should process array with two items`() {
        val file = File("src/test/resources/array2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(2) { it.size }
            expect("abc") { (it[0] as? YAMLString)?.get() }
            expect("def") { (it[1] as? YAMLString)?.get() }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process literal block scalar`() {
        val file = File("src/test/resources/literalblockscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("hello\nworld\n") { ((result.rootNode as? YAMLMapping)?.get("abc") as? YAMLString)?.get() }
    }

    @Test fun `should process flow sequence`() {
        val file = File("src/test/resources/flowsequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(2) { it.size }
            expect("abc") { (it[0] as? YAMLString)?.get() }
            expect("def") { (it[1] as? YAMLString)?.get() }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process more complex flow sequence`() {
        val file = File("src/test/resources/flowsequence2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(3) { it.size }
            expect("abc def") { (it[0] as? YAMLString)?.get() }
            expect("ghi") { (it[1] as? YAMLString)?.get() }
            expect("jkl") { (it[2] as? YAMLString)?.get() }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process flow sequence of mappings`() {
        val file = File("src/test/resources/flowsequence3.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let { sequence ->
            expect(3) { sequence.size }
            (sequence[0] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect(123) { (it["abc"] as? YAMLInt)?.get() }
            }
            (sequence[1] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect(456) { (it["abc"] as? YAMLInt)?.get() }
            }
            (sequence[2] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect(789) { (it["def"] as? YAMLInt)?.get() }
            }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process nested flow sequences`() {
        val file = File("src/test/resources/flowsequence4.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let { sequence ->
            expect(3) { sequence.size }
            (sequence[0] as? YAMLSequence)?.let {
                expect(1) { it.size }
                expect("abc") { (it[0] as? YAMLString)?.get() }
            }
            (sequence[1] as? YAMLSequence)?.let {
                expect(2) { it.size }
                expect("def") { (it[0] as? YAMLString)?.get() }
                expect(888) { (it[1] as? YAMLInt)?.get() }
            }
            (sequence[2] as? YAMLSequence)?.let {
                expect(0) { it.size }
            }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process flow mapping`() {
        val file = File("src/test/resources/flowmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            expect(2) { it.size }
            expect(1234) { (it["abcde"] as? YAMLInt)?.get() }
            expect("World!") { (it["hello"] as? YAMLString)?.get() }
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process example JSON schema`() {
        val file = File("src/test/resources/example.schema.yaml")
        val result = YAMLSimple.process(file)
        log.debug { JSONFormat.create().format(result.rootNode) }
        (result.rootNode as? YAMLMapping)?.let {
            expect(6) { it.size }
            expect("http://json-schema.org/draft/2019-09/schema") { (it["\$schema"] as? YAMLString)?.get() }
            expect("http://pwall.net/test") { (it["\$id"] as? YAMLString)?.get() }
            expect("Product") { (it["title"] as? YAMLString)?.get() }
            expect("object") { (it["type"] as? YAMLString)?.get() }
            (it["required"] as? YAMLSequence)?.let { required ->
                expect(3) { required.size }
                expect("id") { (required[0] as? YAMLString)?.get() }
                expect("name") { (required[1] as? YAMLString)?.get() }
                expect("price") { (required[2] as? YAMLString)?.get() }
            } ?: fail("required not a sequence")
            (it["properties"] as? YAMLMapping)?.let { properties ->
                expect(5) { properties.size }
                (properties["id"] as? YAMLMapping)?.let { id ->
                    expect(2) { id.size }
                    expect("number") { (id["type"] as? YAMLString)?.get() }
                    expect("Product identifier") { (id["description"] as? YAMLString)?.get() }
                } ?: fail("id not a mapping")
                (properties["name"] as? YAMLMapping)?.let { name ->
                    expect(2) { name.size }
                    expect("string") { (name["type"] as? YAMLString)?.get() }
                    expect("Name of the product") { (name["description"] as? YAMLString)?.get() }
                } ?: fail("name not a mapping")
                (properties["tags"] as? YAMLMapping)?.let { tags ->
                    expect(2) { tags.size }
                    expect("array") { (tags["type"] as? YAMLString)?.get() }
                    (tags["items"] as? YAMLMapping)?.let { items ->
                        expect(1) { items.size }
                        expect("string") { (items["type"] as? YAMLString)?.get() }
                    } ?: fail("items not a mapping")
                } ?: fail("tags not a mapping")
                (properties["stock"] as? YAMLMapping)?.let { stock ->
                    expect(2) { stock.size }
                    expect("object") { (stock["type"] as? YAMLString)?.get() }
                    (stock["properties"] as? YAMLMapping)?.let { properties2 ->
                        expect(2) { properties2.size }
                        (properties2["warehouse"] as? YAMLMapping)?.let { warehouse ->
                            expect(1) { warehouse.size }
                            expect("number") { (warehouse["type"] as? YAMLString)?.get() }
                        } ?: fail("warehouse not a mapping")
                        (properties2["retail"] as? YAMLMapping)?.let { retail ->
                            expect(1) { retail.size }
                            expect("number") { (retail["type"] as? YAMLString)?.get() }
                        } ?: fail("retail not a mapping")
                    } ?: fail("properties not a mapping")
                } ?: fail("tags not a mapping")
            } ?: fail("properties not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    companion object {

        val log: Logger = LoggerFactory.getDefaultLogger(YAMLSimpleTest::class.qualifiedName)

    }

}
