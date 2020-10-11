# yaml-simple

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
    val description = pointer.eval(yamlDocument.rootNode)
```

## Implemented Subset

This parser does not implement the full [YAML specification](https://yaml.org/spec/1.2/spec.html).
The currently implemented subset includes:

- Block Mappings
- Block Sequences
- Block Scalars (literal and folded)
- Flow Scalars (plain, single quoted and double quoted)
- Flow Sequences
- Comments

Not yet implemented, but in the development pipeline:

- Flow Mappings

Further down the track:

- Anchors and Aliases
- The "?" and ":" syntax for Block Mapping entries
- Directives
- Tags

Also, the parser may not meet the specification in all respects, even for the constructs that it does handle.

## Dependency Specification

The latest version of the library is 0.1, and it may be obtained from the Maven Central repository.

### Maven
```xml
    <dependency>
      <groupId>net.pwall.yaml</groupId>
      <artifactId>yaml-simple</artifactId>
      <version>0.1</version>
    </dependency>
```
### Gradle
```groovy
    implementation 'net.pwall.yaml:yaml-simple:0.1'
```
### Gradle (kts)
```kotlin
    implementation("net.pwall.yaml:yaml-simple:0.1")
```

Peter Wall

2020-10-05
