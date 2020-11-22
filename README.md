# yaml-simple

[![Build Status](https://travis-ci.org/pwall567/yaml-simple.svg?branch=main)](https://travis-ci.org/pwall567/yaml-simple)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v1.4.0&color=blue&logo=kotlin)](https://github.com/JetBrains/kotlin/releases/tag/v1.4.0)
[![Maven Central](https://img.shields.io/maven-central/v/net.pwall.yaml/yaml-simple?label=Maven%20Central)](https://search.maven.org/search?q=g:%22net.pwall.yaml%22%20AND%20a:%22yaml-simple%22)

A simple YAML processor.

## Quick Start

To parse a YAML file:
```kotlin
    val file = File("path.to.file")
    val yamlDocument = YAMLSimple.processFile(file)
```

The result is a `YAMLDocument`, and the `rootNode` property contains the root (or only) node of the tree of YAML nodes.
The tree may be navigated as if it were a JSON structure, using the [jsonutil](https://github.com/pwall567/jsonutil) or
[json-pointer](https://github.com/pwall567/json-pointer) libraries or others.

For example, to retrieve the `description` property of the `info` entry of a Swagger 2.0 YAML file:
```kotlin
    val file = File("path.to.swagger.file")
    val yamlDocument = YAMLSimple.processFile(file)
    val pointer = JSONPointer("/info/description")
    val description = pointer.find(yamlDocument.rootNode)
```

## Implemented Subset

This parser does not implement the full [YAML specification](https://yaml.org/spec/1.2/spec.html).
The currently implemented subset includes:

- Block Mappings
- Block Sequences
- Block Scalars (literal and folded)
- Flow Scalars (plain, single quoted and double quoted)
- Flow Sequences
- Flow Mappings
- Comments

Not yet implemented:

- Anchors and Aliases
- The "?" and ":" syntax for Block Mapping entries
- Directives
- Tags
- Multiple documents in a single file

Also, the parser may not yet meet the specification in all respects, even for the constructs that it does handle.

## Dependency Specification

The latest version of the library is 0.2.1, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.yaml</groupId>
      <artifactId>yaml-simple</artifactId>
      <version>0.2.1</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.yaml:yaml-simple:0.2.1'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.yaml:yaml-simple:0.2.1")
```

Peter Wall

2020-10-25
