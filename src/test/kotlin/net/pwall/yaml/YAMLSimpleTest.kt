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
import kotlin.test.fail

import java.io.File
import java.math.BigDecimal

import io.kstuff.test.shouldBe

import net.pwall.json.JSONFormat
import net.pwall.log.getLogger

class YAMLSimpleTest {

    @Test fun `should return null document for empty file`() {
        val emptyFile = File("src/test/resources/empty.yaml")
        val result = YAMLSimple.process(emptyFile)
        result.rootNode shouldBe null
    }

    @Test fun `should return null document for empty file as InputStream`() {
        val inputStream = File("src/test/resources/empty.yaml").inputStream()
        val result = YAMLSimple.process(inputStream)
        result.rootNode shouldBe null
    }

    @Test fun `should return null document for empty file as Reader`() {
        val reader = File("src/test/resources/empty.yaml").reader()
        val result = YAMLSimple.process(reader)
        result.rootNode shouldBe null
    }

    @Test fun `should process file starting with separator`() {
        val file = File("src/test/resources/separator1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abc"
    }

    @Test fun `should process file starting with separator and ending with terminator`() {
        val file = File("src/test/resources/separator2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abc"
        result.majorVersion shouldBe 1
        result.minorVersion shouldBe 2
    }

    @Test fun `should process file starting with separator with comment`() {
        val file = File("src/test/resources/separator3.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "Hello"
        result.majorVersion shouldBe 1
        result.minorVersion shouldBe 2
    }

    @Test fun `should process file starting with YAML directive`() {
        val file = File("src/test/resources/directive1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abc"
        result.majorVersion shouldBe 1
        result.minorVersion shouldBe 2
    }

    @Test fun `should process file starting with YAML 1 1 directive`() {
        val file = File("src/test/resources/directive2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abc"
        result.majorVersion shouldBe 1
        result.minorVersion shouldBe 1
    }

    @Test fun `should process file starting with YAML directive with comment`() {
        val file = File("src/test/resources/directive4.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abcdef"
        result.majorVersion shouldBe 1
        result.minorVersion shouldBe 2
    }

    @Test fun `should fail on YAML directive not 1 x`() {
        val file = File("src/test/resources/directive3.yaml")
        val exception = assertFailsWith<YAMLException> { YAMLSimple.process(file) }
        exception.message shouldBe "%YAML version must be 1.x at 1:10"
    }

    @Test fun `should process plain scalar`() {
        val file = File("src/test/resources/plainscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "http://pwall.net/schema.json#/aaa"
    }

    @Test fun `should process double quoted scalar`() {
        val file = File("src/test/resources/doublequotedscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "a b \n \r \" A A \u2014 A \uD83D\uDE02"
    }

    @Test fun `should process multi-line scalar`() {
        val file = File("src/test/resources/multilinescalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLString).value shouldBe "abc def ghi"
    }

    @Test fun `should process integer scalar`() {
        val file = File("src/test/resources/integerscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLInt).value shouldBe 123
    }

    @Test fun `should process decimal scalar`() {
        val file = File("src/test/resources/decimalscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as YAMLDecimal).value shouldBe BigDecimal("12345.67")
    }

    @Test fun `should process simple key-value`() {
        val file = File("src/test/resources/keyvalue.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.value shouldBe "value"
    }

    @Test fun `should process simple key-integer`() {
        val file = File("src/test/resources/keyinteger.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLInt)?.value shouldBe 123
    }

    @Test fun `should process simple block property`() {
        val file = File("src/test/resources/keyblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        ((result.rootNode as? YAMLMapping)?.get("key") as? YAMLString)?.value shouldBe "data"
    }

    @Test fun `should process nested block property`() {
        val file = File("src/test/resources/nestedblock.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            it.size shouldBe 1
            (it["key"] as? YAMLMapping)?.let { inner ->
                inner.size shouldBe 1
                (inner["nested"] as? YAMLString)?.value shouldBe "inner"
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process multiple properties`() {
        val file = File("src/test/resources/multipleproperties.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            it.size shouldBe 3
            (it["prop1"] as? YAMLString)?.value shouldBe "abc"
            (it["prop2"] as? YAMLString)?.value shouldBe " X "
            it["prop3"] shouldBe null
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array with single item`() {
        val file = File("src/test/resources/array1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        ((result.rootNode as? YAMLSequence)?.get(0) as? YAMLString)?.value shouldBe "abc"
    }

    @Test fun `should process array with two items`() {
        val file = File("src/test/resources/array2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            it.size shouldBe 2
            (it[0] as? YAMLString)?.value shouldBe "abc"
            (it[1] as? YAMLString)?.value shouldBe "def"
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process literal block scalar`() {
        val file = File("src/test/resources/literalblockscalar.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        ((result.rootNode as? YAMLMapping)?.get("abc") as? YAMLString)?.value shouldBe "hello\nworld\n"
    }

    @Test fun `should process flow sequence`() {
        val file = File("src/test/resources/flowsequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            it.size shouldBe 2
            (it[0] as? YAMLString)?.value shouldBe "abc"
            (it[1] as? YAMLString)?.value shouldBe "def"
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process more complex flow sequence`() {
        val file = File("src/test/resources/flowsequence2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let {
            it.size shouldBe 3
            (it[0] as? YAMLString)?.value shouldBe "abc def"
            (it[1] as? YAMLString)?.value shouldBe "ghi"
            (it[2] as? YAMLString)?.value shouldBe "jkl"
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process flow sequence of mappings`() {
        val file = File("src/test/resources/flowsequence3.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let { sequence ->
            sequence.size shouldBe 3
            (sequence[0] as? YAMLMapping)?.let {
                it.size shouldBe 1
                (it["abc"] as? YAMLInt)?.value shouldBe 123
            }
            (sequence[1] as? YAMLMapping)?.let {
                it.size shouldBe 1
                (it["abc"] as? YAMLInt)?.value shouldBe 456
            }
            (sequence[2] as? YAMLMapping)?.let {
                it.size shouldBe 1
                (it["def"] as? YAMLInt)?.value shouldBe 789
            }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process nested flow sequences`() {
        val file = File("src/test/resources/flowsequence4.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLSequence)?.let { sequence ->
            sequence.size shouldBe 3
            (sequence[0] as? YAMLSequence)?.let {
                it.size shouldBe 1
                (it[0] as? YAMLString)?.value shouldBe "abc"
            }
            (sequence[1] as? YAMLSequence)?.let {
                it.size shouldBe 2
                (it[0] as? YAMLString)?.value shouldBe "def"
                (it[1] as? YAMLInt)?.value shouldBe 888
            }
            (sequence[2] as? YAMLSequence)?.let {
                it.size shouldBe 0
            }
        } ?: fail("Outer block not a sequence")
    }

    @Test fun `should process flow mapping`() {
        val file = File("src/test/resources/flowmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let {
            it.size shouldBe 2
            (it["abcde"] as? YAMLInt)?.value shouldBe 1234
            (it["hello"] as? YAMLString)?.value shouldBe "World!"
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array as property of mapping`() {
        val file = File("src/test/resources/arrayProperty1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                inner.size shouldBe 1
                (inner["beta"] as? YAMLSequence)?.let { array ->
                    array.size shouldBe 2
                    (array[0] as? YAMLInt)?.value shouldBe 123
                    (array[1] as? YAMLInt)?.value shouldBe 456
                } ?: fail("Array not a sequence")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process array as property of mapping with comment line`() {
        val file = File("src/test/resources/arrayProperty2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                inner.size shouldBe 1
                (inner["beta"] as? YAMLSequence)?.let { array ->
                    array.size shouldBe 2
                    (array[0] as? YAMLInt)?.value shouldBe 123
                    (array[1] as? YAMLInt)?.value shouldBe 789
                } ?: fail("Array not a sequence")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process nested properties`() {
        val file = File("src/test/resources/propertyProperty1.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                inner.size shouldBe 1
                (inner["beta"] as? YAMLMapping)?.let { third ->
                    third.size shouldBe 2
                    (third["gamma"] as? YAMLInt)?.value shouldBe 123
                    (third["delta"] as? YAMLInt)?.value shouldBe 456
                } ?: fail("Third level block not a mapping")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process nested properties with comment line`() {
        val file = File("src/test/resources/propertyProperty2.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["alpha"] as? YAMLMapping)?.let { inner ->
                inner.size shouldBe 1
                (inner["beta"] as? YAMLMapping)?.let { third ->
                    third.size shouldBe 2
                    (third["gamma"] as? YAMLInt)?.value shouldBe 123
                    (third["epsilon"] as? YAMLInt)?.value shouldBe 789
                } ?: fail("Third level block not a mapping")
            } ?: fail("Inner block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process explicit block mapping`() {
        val file = File("src/test/resources/explicitblockmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["outer"] as? YAMLMapping)?.let { nested ->
                nested.size shouldBe 1
                (nested["key1"] as? YAMLMapping)?.let { inner->
                    inner.size shouldBe 1
                    (inner["inner1"] as? YAMLString)?.value shouldBe "value1"
                } ?: fail("Innermost block not a mapping")
            } ?: fail("Nested block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process block sequence with empty first line`() {
        val file = File("src/test/resources/blocksequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { outer ->
            outer.size shouldBe 1
            (outer["outer"] as? YAMLSequence)?.let { array ->
                array.size shouldBe 2
                (array[0] as? YAMLMapping)?.let { inner ->
                    inner.size shouldBe 1
                    (inner["inner"] as? YAMLString)?.value shouldBe "value1"
                } ?: fail("Innermost block not a mapping")
                (array[1] as? YAMLMapping)?.let { inner ->
                    inner.size shouldBe 1
                    (inner["inner"] as? YAMLString)?.value shouldBe "value2"
                } ?: fail("Innermost block not a mapping")
            } ?: fail("Content not a sequence")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should accept escaped newline in double-quoted scalar`() {
        val file = File("src/test/resources/multilinedoublequoted.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            obj.size shouldBe 2
            (obj["first"] as? YAMLString)?.value shouldBe "alphabet"
            (obj["second"] as? YAMLString)?.value shouldBe "alpha bet"
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process combination of explicit and conventional block mapping`() {
        val file = File("src/test/resources/combinedblockmapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            obj.size shouldBe 2
            (obj["first"] as? YAMLMapping)?.let { first ->
                first.size shouldBe 2
                (first["key1"] as? YAMLString)?.value shouldBe "value1"
                (first["key2"] as? YAMLString)?.value shouldBe "value2"
            } ?: fail("First block not a mapping")
            (obj["second"] as? YAMLMapping)?.let { second ->
                second.size shouldBe 2
                (second["key1"] as? YAMLString)?.value shouldBe "value1"
                (second["key2"] as? YAMLString)?.value shouldBe "value2"
            } ?: fail("Second block not a mapping")
        } ?: fail("Outer block not a mapping")
    }

    @Test fun `should process mixed flow sequence content`() {
        val file = File("src/test/resources/mixedFlowSequence.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            obj.size shouldBe 1
            (obj["enum"] as? YAMLSequence)?.let { seq ->
                seq.size shouldBe 3
                (seq[0] as? YAMLString)?.value shouldBe "ABC"
                (seq[1] as? YAMLString)?.value shouldBe "123"
                (seq[2] as? YAMLString)?.value shouldBe "XYZ"
            }
        }
    }

    @Test fun `should process mixed flow sequence with flow mapping`() {
        val file = File("src/test/resources/mixedFlowSequenceWithMapping.yaml")
        val result = YAMLSimple.process(file)
        log.debug { result.rootNode?.toJSON() }
        (result.rootNode as? YAMLMapping)?.let { obj ->
            obj.size shouldBe 1
            (obj["example"] as? YAMLSequence)?.let { seq ->
                seq.size shouldBe 1
                (seq[0] as? YAMLMapping)?.let { mapping ->
                    mapping.size shouldBe 1
                    (mapping["prop1"] as? YAMLString)?.value shouldBe "1.5"
                }
            }
        }
    }

    @Test fun `should process example JSON schema`() {
        val file = File("src/test/resources/example.schema.yaml")
        val result = YAMLSimple.process(file)
        log.debug { JSONFormat.create().format(result.rootNode) }
        (result.rootNode as? YAMLMapping)?.let {
            it.size shouldBe 6
            (it["\$schema"] as? YAMLString)?.value shouldBe "http://json-schema.org/draft/2019-09/schema"
            (it["\$id"] as? YAMLString)?.value shouldBe "http://pwall.net/test"
            (it["title"] as? YAMLString)?.value shouldBe "Product"
            (it["type"] as? YAMLString)?.value shouldBe "object"
            (it["required"] as? YAMLSequence)?.let { required ->
                required.size shouldBe 3
                (required[0] as? YAMLString)?.value shouldBe "id"
                (required[1] as? YAMLString)?.value shouldBe "name"
                (required[2] as? YAMLString)?.value shouldBe "price"
            } ?: fail("required not a sequence")
            (it["properties"] as? YAMLMapping)?.let { properties ->
                properties.size shouldBe 5
                (properties["id"] as? YAMLMapping)?.let { id ->
                    id.size shouldBe 2
                    (id["type"] as? YAMLString)?.value shouldBe "number"
                    (id["description"] as? YAMLString)?.value shouldBe "Product identifier"
                } ?: fail("id not a mapping")
                (properties["name"] as? YAMLMapping)?.let { name ->
                    name.size shouldBe 2
                    (name["type"] as? YAMLString)?.value shouldBe "string"
                    (name["description"] as? YAMLString)?.value shouldBe "Name of the product"
                } ?: fail("name not a mapping")
                (properties["tags"] as? YAMLMapping)?.let { tags ->
                    tags.size shouldBe 2
                    (tags["type"] as? YAMLString)?.value shouldBe "array"
                    (tags["items"] as? YAMLMapping)?.let { items ->
                        items.size shouldBe 1
                        (items["type"] as? YAMLString)?.value shouldBe "string"
                    } ?: fail("items not a mapping")
                } ?: fail("tags not a mapping")
                (properties["stock"] as? YAMLMapping)?.let { stock ->
                    stock.size shouldBe 2
                    (stock["type"] as? YAMLString)?.value shouldBe "object"
                    (stock["properties"] as? YAMLMapping)?.let { properties2 ->
                        properties2.size shouldBe 2
                        (properties2["warehouse"] as? YAMLMapping)?.let { warehouse ->
                            warehouse.size shouldBe 1
                            (warehouse["type"] as? YAMLString)?.value shouldBe "number"
                        } ?: fail("warehouse not a mapping")
                        (properties2["retail"] as? YAMLMapping)?.let { retail ->
                            retail.size shouldBe 1
                            (retail["type"] as? YAMLString)?.value shouldBe "number"
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
