# yaml-simple

[![Build Status](https://github.com/pwall567/yaml-simple/actions/workflows/build.yml/badge.svg)](https://github.com/pwall567/yaml-simple/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/static/v1?label=Kotlin&message=v2.0.21&color=7f52ff&logo=kotlin&logoColor=7f52ff)](https://github.com/JetBrains/kotlin/releases/tag/v2.0.21)
[![Maven Central](https://img.shields.io/maven-central/v/net.pwall.yaml/yaml-simple?label=Maven%20Central)](https://search.maven.org/search?q=g:%22net.pwall.yaml%22%20AND%20a:%22yaml-simple%22)

A simple YAML processor.

## Important

This project is no longer being maintained.
A more complete YAML implementation is available at [`kjson-yaml`](https://github.com/pwall567/kjson-yaml).

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
- %YAML directive

Not yet implemented:

- Anchors and Aliases
- Directives other than %YAML
- Tags
- Multiple documents in a single file
- Named floating-point pseudo-values (`.inf`, `.nan`)

Also, the parser may not yet meet the specification in all respects, even for the constructs that it does handle.

## Dependency Specification

The latest version of the library is 1.19, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.yaml</groupId>
      <artifactId>yaml-simple</artifactId>
      <version>1.19</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.yaml:yaml-simple:1.19'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.yaml:yaml-simple:1.19")
```

Peter Wall

2025-01-26
