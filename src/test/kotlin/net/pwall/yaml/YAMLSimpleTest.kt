/*
 * @(#) YAMLSimpleTest.java
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

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.expect
import kotlin.test.fail

import java.io.File
import java.math.BigDecimal

import net.pwall.json.JSONFormat
import net.pwall.log.getLogger

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

    @Test fun `should process file starting with separator`() {
        val file = File("src/test/resources/separator1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { (result.rootNode as YAMLString).value }
    }

    @Test fun `should process file starting with separator and ending with terminator`() {
        val file = File("src/test/resources/separator2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { (result.rootNode as YAMLString).value }
        expect(1) { result.majorVersion }
        expect(2) { result.minorVersion }
    }

    @Test fun `should process file starting with separator with comment`() {
        val file = File("src/test/resources/separator3.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("Hello") { (result.rootNode as YAMLString).value }
        expect(1) { result.majorVersion }
        expect(2) { result.minorVersion }
    }

    @Test fun `should process file starting with YAML directive`() {
        val file = File("src/test/resources/directive1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { (result.rootNode as YAMLString).value }
        expect(1) { result.majorVersion }
        expect(2) { result.minorVersion }
    }

    @Test fun `should process file starting with YAML 1 1 directive`() {
        val file = File("src/test/resources/directive2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { (result.rootNode as YAMLString).value }
        expect(1) { result.majorVersion }
        expect(1) { result.minorVersion }
    }

    @Test fun `should process file starting with YAML directive with comment`() {
        val file = File("src/test/resources/directive4.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abcdef") { (result.rootNode as YAMLString).value }
        expect(1) { result.majorVersion }
        expect(2) { result.minorVersion }
    }

    @Test fun `should fail on YAML directive not 1 x`() {
        val file = File("src/test/resources/directive3.yaml")
        val exception = assertFailsWith<YAMLException> { YAMLSimple.process(file) }
        expect("%YAML version must be 1.x at 1:10") { exception.message }
    }

    @Test fun `should process plain scalar`() {
        val file = File("src/test/resources/plainscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("http://pwall.net/schema.json#/aaa") { (result.rootNode as YAMLString).value }
    }

    @Test fun `should process double quoted scalar`() {
        val file = File("src/test/resources/doublequotedscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("a b \n \r \" A A \u2014 A \uD83D\uDE02") { (result.rootNode as YAMLString).value }
    }

    @Test fun `should process multi-line scalar`() {
        val file = File("src/test/resources/multilinescalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc def ghi") { (result.rootNode as YAMLString).value }
    }

    @Test fun `should process integer scalar`() {
        val file = File("src/test/resources/integerscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(123) { (result.rootNode as YAMLInt).value }
    }

    @Test fun `should process decimal scalar`() {
        val file = File("src/test/resources/decimalscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(BigDecimal("12345.67")) { (result.rootNode as YAMLDecimal).value }
    }

    @Test fun `should process simple key-value`() {
        val file = File("src/test/resources/keyvalue.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("value") { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.value }
    }

    @Test fun `should process simple key-integer`() {
        val file = File("src/test/resources/keyinteger.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect(123) { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLInt)?.value }
    }

    @Test fun `should process simple block property`() {
        val file = File("src/test/resources/keyblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("data") { ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.value }
    }

    @Test fun `should process nested block property`() {
        val file = File("src/test/resources/nestedblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            expect(1) { it.size }
            (it["key"] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect("inner") { (it["nested"] as? YAMLString)?.value }
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process multiple properties`() {
        val file = File("src/test/resources/multipleproperties.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            expect(3) { it.size }
            expect("abc") { (it["prop1"] as? YAMLString)?.value }
            expect(" X ") { (it["prop2"] as? YAMLString)?.value }
            expect(null) { it["prop3"] }
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array with single item`() {
        val file = File("src/test/resources/array1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("abc") { ((result.rootNode as? YAMLSequence)?.get(0) as? YAMLString)?.value }
    }

    @Test fun `should process array with two items`() {
        val file = File("src/test/resources/array2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(2) { it.size }
            expect("abc") { (it[0] as? YAMLString)?.value }
            expect("def") { (it[1] as? YAMLString)?.value }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process literal block scalar`() {
        val file = File("src/test/resources/literalblockscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        expect("hello\nworld\n") { ((result.rootNode as? YAMLMapping)?.get("abc") as? YAMLString)?.value }
    }

    @Test fun `should process flow sequence`() {
        val file = File("src/test/resources/flowsequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(2) { it.size }
            expect("abc") { (it[0] as? YAMLString)?.value }
            expect("def") { (it[1] as? YAMLString)?.value }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process more complex flow sequence`() {
        val file = File("src/test/resources/flowsequence2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            expect(3) { it.size }
            expect("abc def") { (it[0] as? YAMLString)?.value }
            expect("ghi") { (it[1] as? YAMLString)?.value }
            expect("jkl") { (it[2] as? YAMLString)?.value }
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
                expect(123) { (it["abc"] as? YAMLInt)?.value }
            }
            (sequence[1] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect(456) { (it["abc"] as? YAMLInt)?.value }
            }
            (sequence[2] as? YAMLMapping)?.let {
                expect(1) { it.size }
                expect(789) { (it["def"] as? YAMLInt)?.value }
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
                expect("abc") { (it[0] as? YAMLString)?.value }
            }
            (sequence[1] as? YAMLSequence)?.let {
                expect(2) { it.size }
                expect("def") { (it[0] as? YAMLString)?.value }
                expect(888) { (it[1] as? YAMLInt)?.value }
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
            expect(1234) { (it["abcde"] as? YAMLInt)?.value }
            expect("World!") { (it["hello"] as? YAMLString)?.value }
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array as property of mapping`() {
        val file = File("src/test/resources/arrayProperty1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                expect(1) { inner.size }
                (inner["beta"] as? YAMLSequence)?.let { array ->
                    expect(2) { array.size }
                    expect(123) { (array[0] as? YAMLInt)?.value }
                    expect(456) { (array[1] as? YAMLInt)?.value }
                } ?: fail("Array not a sequence")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array as property of mapping with comment line`() {
        val file = File("src/test/resources/arrayProperty2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                expect(1) { inner.size }
                (inner["beta"] as? YAMLSequence)?.let { array ->
                    expect(2) { array.size }
                    expect(123) { (array[0] as? YAMLInt)?.value }
                    expect(789) { (array[1] as? YAMLInt)?.value }
                } ?: fail("Array not a sequence")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process nested properties`() {
        val file = File("src/test/resources/propertyProperty1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                expect(1) { inner.size }
                (inner["beta"] as? YAMLMapping)?.let { third ->
                    expect(2) { third.size }
                    expect(123) { (third["gamma"] as? YAMLInt)?.value }
                    expect(456) { (third["delta"] as? YAMLInt)?.value }
                } ?: fail("Third level block not a mapping")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process nested properties with comment line`() {
        val file = File("src/test/resources/propertyProperty2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                expect(1) { inner.size }
                (inner["beta"] as? YAMLMapping)?.let { third ->
                    expect(2) { third.size }
                    expect(123) { (third["gamma"] as? YAMLInt)?.value }
                    expect(789) { (third["epsilon"] as? YAMLInt)?.value }
                } ?: fail("Third level block not a mapping")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process explicit block mapping`() {
        val file = File("src/test/resources/explicitblockmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["outer"] as? YAMLMapping)?.let { nested ->
                expect(1) { nested.size }
                (nested["key1"] as? YAMLMapping)?.let { inner->
                    expect(1) { inner.size }
                    expect("value1") { (inner["inner1"] as? YAMLString)?.value }
                } ?: fail("Innermost block not a mapping")
            } ?: fail("Nested block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process block sequence with empty first line`() {
        val file = File("src/test/resources/blocksequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            expect(1) { outer.size }
            (outer["outer"] as? YAMLSequence)?.let { array ->
                expect(2) { array.size }
                (array[0] as? YAMLMapping)?.let { inner ->
                    expect(1) { inner.size }
                    expect("value1") { (inner["inner"] as? YAMLString)?.value }
                } ?: fail("Innermost block not a mapping")
                (array[1] as? YAMLMapping)?.let { inner ->
                    expect(1) { inner.size }
                    expect("value2") { (inner["inner"] as? YAMLString)?.value }
                } ?: fail("Innermost block not a mapping")
            } ?: fail("Content not a sequence")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should accept escaped newline in double-quoted scalar`() {
        val file = File("src/test/resources/multilinedoublequoted.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            expect(2) { obj.size }
            expect("alphabet") { (obj["first"] as? YAMLString)?.value }
            expect("alpha bet") { (obj["second"] as? YAMLString)?.value }
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process combination of explicit and conventional block mapping`() {
        val file = File("src/test/resources/combinedblockmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            expect(2) { obj.size }
            (obj["first"] as? YAMLMapping)?.let { first ->
                expect(2) { first.size }
                expect("value1") { (first["key1"] as? YAMLString)?.value }
                expect("value2") { (first["key2"] as? YAMLString)?.value }
            } ?: fail("First block not a mapping")
            (obj["second"] as? YAMLMapping)?.let { second ->
                expect(2) { second.size }
                expect("value1") { (second["key1"] as? YAMLString)?.value }
                expect("value2") { (second["key2"] as? YAMLString)?.value }
            } ?: fail("Second block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process mixed flow sequence content`() {
        val file = File("src/test/resources/mixedFlowSequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            expect(1) { obj.size }
            (obj["enum"] as? YAMLSequence)?.let { seq ->
                expect(3) { seq.size }
                expect("ABC") { (seq[0] as? YAMLString)?.value }
                expect("123") { (seq[1] as? YAMLString)?.value }
                expect("XYZ") { (seq[2] as? YAMLString)?.value }
            }
        }
    }

    @Test fun `should process mixed flow sequence with flow mapping`() {
        val file = File("src/test/resources/mixedFlowSequenceWithMapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            expect(1) { obj.size }
            (obj["example"] as? YAMLSequence)?.let { seq ->
                expect(1) { seq.size }
                (seq[0] as? YAMLMapping)?.let { mapping ->
                    expect(1) { mapping.size }
                    expect("1.5") { (mapping["prop1"] as? YAMLString)?.value }
                }
            }
        }
    }

    @Test fun `should process example JSON schema`() {
        val file = File("src/test/resources/example.schema.yaml")
        val result = YAMLSimple.process(file)
        log.debug { JSONFormat.create().format(result.rootNode) }
        (result.rootNode as? YAMLMapping)?.let {
            expect(6) { it.size }
            expect("http://json-schema.org/draft/2019-09/schema") { (it["\$schema"] as? YAMLString)?.value }
            expect("http://pwall.net/test") { (it["\$id"] as? YAMLString)?.value }
            expect("Product") { (it["title"] as? YAMLString)?.value }
            expect("object") { (it["type"] as? YAMLString)?.value }
            (it["required"] as? YAMLSequence)?.let { required ->
                expect(3) { required.size }
                expect("id") { (required[0] as? YAMLString)?.value }
                expect("name") { (required[1] as? YAMLString)?.value }
                expect("price") { (required[2] as? YAMLString)?.value }
            } ?: fail("required not a sequence")
            (it["properties"] as? YAMLMapping)?.let { properties ->
                expect(5) { properties.size }
                (properties["id"] as? YAMLMapping)?.let { id ->
                    expect(2) { id.size }
                    expect("number") { (id["type"] as? YAMLString)?.value }
                    expect("Product identifier") { (id["description"] as? YAMLString)?.value }
                } ?: fail("id not a mapping")
                (properties["name"] as? YAMLMapping)?.let { name ->
                    expect(2) { name.size }
                    expect("string") { (name["type"] as? YAMLString)?.value }
                    expect("Name of the product") { (name["description"] as? YAMLString)?.value }
                } ?: fail("name not a mapping")
                (properties["tags"] as? YAMLMapping)?.let { tags ->
                    expect(2) { tags.size }
                    expect("array") { (tags["type"] as? YAMLString)?.value }
                    (tags["items"] as? YAMLMapping)?.let { items ->
                        expect(1) { items.size }
                        expect("string") { (items["type"] as? YAMLString)?.value }
                    } ?: fail("items not a mapping")
                } ?: fail("tags not a mapping")
                (properties["stock"] as? YAMLMapping)?.let { stock ->
                    expect(2) { stock.size }
                    expect("object") { (stock["type"] as? YAMLString)?.value }
                    (stock["properties"] as? YAMLMapping)?.let { properties2 ->
                        expect(2) { properties2.size }
                        (properties2["warehouse"] as? YAMLMapping)?.let { warehouse ->
                            expect(1) { warehouse.size }
                            expect("number") { (warehouse["type"] as? YAMLString)?.value }
                        } ?: fail("warehouse not a mapping")
                        (properties2["retail"] as? YAMLMapping)?.let { retail ->
                            expect(1) { retail.size }
                            expect("number") { (retail["type"] as? YAMLString)?.value }
                        } ?: fail("retail not a mapping")
                    } ?: fail("properties not a mapping")
                } ?: fail("tags not a mapping")
            } ?: fail("properties not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    companion object {

        val log = getLogger()

    }

}
