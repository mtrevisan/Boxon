# Boxon _[boˈzoŋ]_

![Java-21+](https://img.shields.io/badge/java-21%2B-orange.svg) [![License: GPL v3](https://img.shields.io/badge/License-MIT-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<a href="https://codeclimate.com/github/mtrevisan/Boxon/maintainability"><img src="https://api.codeclimate.com/v1/badges/bff8577200d792e1e197/maintainability" /></a>

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.mtrevisan/boxon/badge.svg)](https://mvnrepository.com/artifact/io.github.mtrevisan/boxon) [![javadoc](https://javadoc.io/badge2/io.github.mtrevisan/boxon/javadoc.svg)](https://javadoc.io/doc/io.github.mtrevisan/boxon)

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)
==========

<br />

<img alt="logo" src="https://i.postimg.cc/500W5mVV/horizontal-white.png" width="50%">

## Forewords

This is a declarative, bit-level, message parser. All you have to do is write a [DTO](https://en.wikipedia.org/wiki/Data_transfer_object) that represents your message and annotate it. That's all. [Boxon](https://en.wikipedia.org/wiki/Boson) will take care of the rest for you.

If you want to use the parser straight away, just go [here](#examples).

<br />

| This project adheres to the **[Zero Bugs Commitment](https://github.com/classgraph/classgraph/blob/master/Zero-Bugs-Commitment.md)**. |
|---------------------------------------------------------------------------------------------------------------------------------------|

<br />

(Like [Preon](https://github.com/preon/preon) — currently not maintained anymore —, but the code is understandable, shorter, easier to extend, uses the more powerful (and maintained) [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html), and the documentation is __really__ free...)

<br/>

### Notable features

Boxon...
 - Is easily extensible through the use of [converters](#how-to).
 - Contains a minimal set of [annotations](#annotation-base) capable of handling "all" the primitive data (aside `char`, but this could be easily handled with a converter).
 - Contains a set of [special annotations](#annotation-special) that handles the various messages peculiarities (defining message header properties, conditional choosing of converter, or object while reading an array, skip bits, checksum, 'constant' assignments)
 - Is capable of handle concatenation of messages, using the correct template under the hood.
 - The template is selected in a clever way, i.e. selecting the one with the longest `start` parameter that matches the message.
 - Can handle [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html) on certain fields, thus more powerful and simpler than [Limbo](http://limbo.sourceforge.net/apidocs/)<sup>[1](#footnote-1)</sup> (but less than [janino](https://github.com/janino-compiler/janino), that has other problems).
 - Can decode and encode data on the fly with a single annotated class (thus avoiding separate decoder and encoder going out-of-sync).
 - Supported data types are:
   - Integers: 1-, 4-, 8-, 16-, 32- and 64-bit signed and unsigned integers, little- or big-endian.
   - Floating point numbers: 32- and 64-bit floating point values.
   - Bit fields: bit fields with length from 1 to 2,147,483,647 bits.
   - Strings: fixed-length, variable-length and zero terminated strings with various encodings.
   - Arrays: fixed-length and variable-length arrays of built-in or user-defined element types.
   - Objects: custom-type DTOs.
   - Choices: supports integer keys.
 - User defined types (arbitrary combination of built-in types)
 - Has templates (annotated classes) that are not complex: they do not call each other uselessly complicating the structure (apart, necessarily, for `@BindArray`), no complicated chains of factories: it's just a parser that works.
 - Supports [SLF4J](http://www.slf4j.org/).
 - Hides the complexities of encoding and decoding, thus simplifying the changes to be made to the code due to frequent protocol changes.
 - Can automatically scan and loads all the binding annotations and/or templates from a package.

---

<a name="footnote-1"></a>
<sub><sup>1</sup> Currently Limbo is merged with Preon... thus rendering Preon not only a parser, but also an evaluator, over-complicating and cluttering the code.</sub>

<br/>

### Differences from...

#### Preon

Boxon differs from [Preon](https://github.com/preon/preon) in...
 - Does not have a generic `Bound` annotation: it uses converters instead.
 - Does not need the "native byte order" constant. This is because the bytes of the message have little chance to be generated from the very same machine that will parse them, what if a message consider 24 bits as an Integer? If the code should be portable and installed and run everywhere it should not rely on the native properties of any machine.
   Moreover, `@Bound boolean visible;` is 1 bit- or 1 byte-length?
 - Does not have `BoundList`: since the message is a finite sequence of bytes, then any array is of finite length, and thus the standard java array (`[]`) is sufficient. If someone wants a `List` a converter can be used.
 - Does not rely on the type of the annotated variable (because of the existence of the converters); in fact, the annotation, eventually, serves the purpose to pass a predefined type of data to a converter.<br/>
   For this reason too, there is no need for the `Init` annotation, thus the annotated file can contain the least amount of data necessary for its decoding (moreover, this annotation has NOT the inverse operation -- so it seems to me... so it's pretty useless anyway).
 - (By personal experience) enumerations can have different representations, or change between a version and the next of a protocol, even inside the same protocol (!), so having an annotation that tells the value of a particular element of this enum is at least risky. So, for this reason, the `BoundEnumOption` is not present in this library.
 - Does read and write more than 64 bits at a time (`BitBuffer.readBits`)

<br/>

### Pre-compiled executables

Get them [here](https://github.com/mtrevisan/Boxon/releases/).

### Maven dependency

In order to include Boxon in a Maven project add the following dependency to your pom.xml (<b>Java 21 required</b>).

Replace `x.y.z` below int the version tag with the latest [release number](https://github.com/mtrevisan/Boxon/releases).

```xml
<dependency>
    <groupId>io.github.mtrevisan</groupId>
    <artifactId>boxon</artifactId>
    <version>x.y.z</version>
</dependency>
```

### Pre-built JARs

You can get pre-built JARs (usable on JRE 21 or newer) from [Sonatype](https://oss.sonatype.org/#nexus-search;quick~io.github.mtrevisan).

<br/>
<br/>

## Table of Contents

1. [Basic annotations](#annotation-basic)
    1. [Summary](#annotation-summary)
    2. [BindObject](#annotation-bindobject)
    3. [BindArray](#annotation-bindarray)
    4. [BindArrayPrimitive](#annotation-bindarrayprimitive)
    5. [BindList](#annotation-bindlist)
    6. [BindBitSet](#annotation-bindbitset)
    7. [BindInteger](#annotation-bindinteger)
    8. [BindString](#annotation-bindstring)
    9. [BindStringTerminated](#annotation-bindstringterminated)
2. [Special annotations](#annotation-special)
    1. [TemplateHeader](#annotation-templateheader)
    2. [SkipBits](#annotation-skip-bits)
    3. [SkipUntilTerminator](#annotation-skip-until-terminator)
    4. [Checksum](#annotation-checksum)
    5. [Evaluate](#annotation-evaluate)
    6. [PostProcess](#annotation-post-process)
3. [Protocol description](#protocol-description)
4. [Configuration annotations](#annotation-configuration)
    1. [ConfigurationHeader](#annotation-configurationheader)
    2. [ConfigurationSkip](#annotation-configurationskip)
    3. [ConfigurationField](#annotation-configurationfield)
    4. [CompositeConfigurationField](#annotation-compositeconfigurationfield)
    5. [CompositeSubField](#annotation-compositesubfield)
    6. [AlternativeConfigurationField](#annotation-alternativeconfigurationfield)
    7. [AlternativeSubField](#annotation-alternativesubfield)
5. [Describer](#describer)
6. [Extractor](#extractor)
7. [How to write SpEL expressions](#how-to-spel)
8. [How to extend the functionalities](#how-to-extend)
9. [Digging into the code](#digging)
    1. [Converters](#how-to-converters)
    2. [Custom annotations](#how-to-annotations)
10. [Examples](#examples)
    1. [Multi-message parser](#example-multi)
    2. [Message composer](#example-composer)
11. [Contributing](#contributing)
12. [Changelog](#changelog)
    1. [version 4.0.0](#changelog-4.0.0)
    2. [version 3.6.0](#changelog-3.6.0)
    3. [version 3.5.1](#changelog-3.5.1)
    4. [version 3.5.0](#changelog-3.5.0)
    5. [version 3.4.0](#changelog-3.4.0)
    6. [version 3.3.0](#changelog-3.3.0)
    7. [version 3.2.0](#changelog-3.2.0)
    8. [version 3.1.3](#changelog-3.1.3)
    9. [version 3.1.2](#changelog-3.1.2)
    10. [version 3.1.1](#changelog-3.1.1)
    11. [version 3.1.0](#changelog-3.1.0)
    12. [version 3.0.2](#changelog-3.0.2)
    13. [version 3.0.1](#changelog-3.0.1)
    14. [version 3.0.0](#changelog-3.0.0)
    15. [version 2.1.2](#changelog-2.1.2)
    16. [version 2.1.1](#changelog-2.1.1)
    17. [version 2.1.0](#changelog-2.1.0)
    18. [version 2.0.0](#changelog-2.0.0)
    19. [version 1.1.0](#changelog-1.1.0)
    20. [version 1.0.0](#changelog-1.0.0)
    21. [version 0.0.2](#changelog-0.0.2)
    22. [version 0.0.1](#changelog-0.0.1)
    23. [version 0.0.0](#changelog-0.0.0)
13. [License](#license)

<br/>

<a name="annotation-basic"></a>
## Basic annotations

Here the build-in basic annotations are described.

You can use them as a starting point to build your own customized readers.

<a name="annotation-summary"></a>
### Summary

Here is a brief summary of the parameters (described in detail below) for each annotation.

Note that [Dependency Injection](https://en.wikipedia.org/wiki/Dependency_injection) can be used in codecs on variables with types `TemplateParserInterface` or `Evaluator` IF annotated with `@Injected`.

|                      | condition |  type   | charset | terminator /<br/>consumeTerminator |  size   | byteOrder | selectFrom /<br/>selectDefault | validator | converter /<br/>selectConverterFrom |                      |
|----------------------|:---------:|:-------:|:-------:|:----------------------------------:|:-------:|:---------:|:------------------------------:|:---------:|:-----------------------------------:|---------------------:|
| BindObject           |  &#9745;  | &#9745; |         |                                    |         |           |            &#9745;             |  &#9745;  |               &#9745;               |           BindObject |
| BindArray            |  &#9745;  | &#9745; |         |                                    | &#9745; |           |            &#9745;             |  &#9745;  |               &#9745;               |            BindArray |
| BindArrayPrimitive   |  &#9745;  | &#9745; |         |                                    | &#9745; |  &#9745;  |                                |  &#9745;  |               &#9745;               |   BindArrayPrimitive |
| BindList             |  &#9745;  | &#9745; |         |                                    |         |           |            &#9745;             |  &#9745;  |               &#9745;               |             BindList |
| BindBitSet           |  &#9745;  |         |         |                                    | &#9745; |  &#9745;  |                                |  &#9745;  |               &#9745;               |           BindBitSet |
| BindInteger          |  &#9745;  |         |         |                                    | &#9745; |  &#9745;  |                                |  &#9745;  |               &#9745;               |          BindInteger |
| BindString           |  &#9745;  |         | &#9745; |                                    | &#9745; |           |                                |  &#9745;  |               &#9745;               |           BindString |
| BindStringTerminated |  &#9745;  |         | &#9745; |              &#9745;               |         |           |                                |  &#9745;  |               &#9745;               | BindStringTerminated |

|                     | condition | start /<br/>end | charset |  value  | consumeTerminator | byteOrder | skipStart /<br/>skipEnd | algorithm |  value  | valueDecode /<br/>valueEncode |                     |
|---------------------|:---------:|:---------------:|:-------:|:-------:|:-----------------:|:---------:|:-----------------------:|:---------:|:-------:|:-----------------------------:|--------------------:|
| TemplateHeader      |           |     &#9745;     | &#9745; |         |                   |           |                         |           |         |                               |      TemplateHeader |
| SkipBits            |  &#9745;  |                 |         | &#9745; |                   |           |                         |           |         |                               |            SkipBits |
| SkipUntilTerminator |  &#9745;  |                 |         | &#9745; |      &#9745;      |           |                         |           |         |                               | SkipUntilTerminator |
| Checksum            |  &#9745;  |                 |         |         |                   |  &#9745;  |         &#9745;         |  &#9745;  |         |                               |            Checksum |
| Evaluate            |  &#9745;  |                 |         |         |                   |           |                         |           | &#9745; |                               |            Evaluate |
| PostProcess         |  &#9745;  |                 |         |         |                   |           |                         |           |         |            &#9745;            |        ProcessField |

|                               | shortDescription | longDescription | minProtocol /<br/>maxProtocol | start /<br/>end | charset | terminator | unitOfMeasure | minValue /<br/>maxValue | pattern | enumeration | defaultValue |  radix  | composition |                               |
|-------------------------------|:----------------:|:---------------:|:-----------------------------:|:---------------:|:-------:|:----------:|:-------------:|:-----------------------:|:-------:|:-----------:|:------------:|:-------:|:-----------:|------------------------------:|
| ConfigurationHeader           |     &#9745;      |     &#9745;     |            &#9745;            |     &#9745;     | &#9745; |            |               |                         |         |             |              |         |             |           ConfigurationHeader |
| ConfigurationSkip             |                  |                 |            &#9745;            |                 |         |  &#9745;   |               |                         |         |             |              |         |             |             ConfigurationSkip |
| ConfigurationField            |     &#9745;      |     &#9745;     |            &#9745;            |                 | &#9745; |            |    &#9745;    |         &#9745;         | &#9745; |   &#9745;   |   &#9745;    | &#9745; |             |            ConfigurationField |
| CompositeConfigurationField   |     &#9745;      |     &#9745;     |            &#9745;            |                 | &#9745; |  &#9745;   |               |                         | &#9745; |             |              |         |   &#9745;   |   CompositeConfigurationField |
| CompositeSubField             |     &#9745;      |     &#9745;     |                               |                 |         |            |    &#9745;    |                         | &#9745; |             |   &#9745;    |         |             |             CompositeSubField |
| AlternativeConfigurationField |     &#9745;      |     &#9745;     |            &#9745;            |                 |         |  &#9745;   |    &#9745;    |                         |         |   &#9745;   |              |         |             | AlternativeConfigurationField |
| AlternativeSubField           |                  |     &#9745;     |            &#9745;            |                 | &#9745; |            |    &#9745;    |         &#9745;         | &#9745; |             |   &#9745;    | &#9745; |             |           AlternativeSubField |


<a name="annotation-bindobject"></a>
### BindObject

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `type`: the Class of the Object of the single element of the array (defaults to `Object`).
 - `selectFrom`: the selection from which to choose the instance type.
 - `selectDefault`: the default selection if none can be chosen from `selectFrom` (defaults to `void.class`).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable.
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a single Object.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
class Version{
    @BindInteger(size = "8")
    public byte major;
    @BindInteger(size = "8")
    public byte minor;
    public byte build;
}

@BindBitSet(size = "1", converter = BitSetToBooleanConverter.class)
private boolean versionPresent;
@BindObject(condition = "versionPresent", type = Version.class)
private Version version;
```


<a name="annotation-bindarray"></a>
### BindArray

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `type`: the Class of the Object of the single element of the array (defaults to `Object`).
 - `size`: the size of the array (can be a SpEL expression).
 - `selectFrom`: the selection from which to choose the instance type.
 - `selectDefault`: the default selection if none can be chosen from `selectFrom` (defaults to `void.class`).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads an array of Objects.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
class Version{
    @BindInteger(size = "8")
    public byte major;
    @BindInteger(size = "8")
    public byte minor;
    public byte build;
}

@BindArray(size = "2", type = Version.class)
private Version[] versions;
```

```java
@BindInteger(size = "8")
private byte positionsCount;
@BindArray(size = "positionsCount", type = Position.class,
   selectFrom = @ObjectChoices(prefixSize = 8,
        alternatives = {
          @ObjectChoices.ObjectChoice(condition = "#prefix == 0", prefix = "0", type = PositionInvalid.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = "1", type = PositionAbsolute.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = "2", type = PositionRelative.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 3", prefix = "3", type = PositionSameAsPrevious.class)
       }
    ),
   converter = PositionsConverter.class)
private Position[] positions;
```


<a name="annotation-bindarrayprimitive"></a>
### BindArrayPrimitive

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `type`: the Class of primitive of the single element of the array.
 - `size`: the size of the array (can be a SpEL expression).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads an array of primitives.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindArrayPrimitive(size = "2", type = byte.class)
private byte[] array;
```

```java
@BindBitSet(size = "1", converter = BitSetToBooleanConverter.class)
private boolean angularDataPresent;
@BindArrayPrimitive(condition = "angularDataPresent", size = "dataLength", type = byte.class,
    selectConverterFrom = @ConverterChoices(
        alternatives = {
            @ConverterChoices.ConverterChoice(condition = "angularDataPresent", converter = CrashDataWithAngularDataConverter.class),
            @ConverterChoices.ConverterChoice(condition = "!angularDataPresent", converter = CrashDataWithoutAngularDataConverter.class)
        })
    )
private BigDecimal[][] crashData;
```


<a name="annotation-bindlist"></a>
### BindList

#### parameters

- `condition`: The SpEL expression that determines if this field has to be read.
- `type`: the Class of primitive of the single element of the array.
- `selectFrom`: the selection from which to choose the instance type.
- `selectDefault`: the default selection if none can be chosen from `selectFrom` (defaults to `void.class`).
- `validator`: the Class of a validator (applied BEFORE the converter).
- `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable.
- `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a list of Objects.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindList(type = TestType3.class, selectFrom = @ObjectChoicesList(terminator = ',',
    alternatives = {
        @ObjectChoices.ObjectChoice(condition = "#prefix == '1'", prefix = "1", type = TestType4.class),
        @ObjectChoices.ObjectChoice(condition = "#prefix == '2'", prefix = "2", type = TestType5.class)
    }))
private List<TestType3> value;
```


<a name="annotation-bindbitset"></a>
### BindBitSet

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `size`: the number of bits to read (can be a SpEL expression).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable.
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a java `BitSet`.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindBitSet(size = "2")
private BitSet bitmap;
```


<a name="annotation-bindinteger"></a>
### BindInteger

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `size`: the number of bits to read (can be a SpEL expression).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a long number (primitive or not) or a BigInteger given the amount of bits.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindInteger(size = "3")
private BigInteger number;

@BindInteger(size = "Long.SIZE + 10")
private BigInteger number;
```


<a name="annotation-bindstring"></a>
### BindString

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, e.g. `UTF-8` (the default), `ISO-8859-1`, etc.).
 - `size`: the size of the string (can be a SpEL expression).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a String.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindString(size = "4")
public String text;
```


<a name="annotation-bindstringterminated"></a>
### BindStringTerminated

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, e.g. `UTF-8` (the default), `ISO-8859-1`, etc.).
 - `terminator`: the byte that terminates the string (defaults to `\0`).
 - `consumeTerminator`: whether to consume the terminator (defaults to `true`).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description

Reads a String.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindStringTerminated(terminator = ',')
public String text;
```

<br/>

<a name="annotation-special"></a>
## Special annotations

Here are described the build-in special annotations.

<a name="annotation-templateheader"></a>
### TemplateHeader

#### parameters

 - `start`: an array of possible start sequences (as string) for this message (defaults to empty).
 - `end`: a possible end sequence (as string) for this message (default to empty).
 - `charset`: the charset to be interpreted the `start` and `end` strings into (SHOULD BE the charset name, e.g. `UTF-8` (the default), `ISO-8859-1`, etc.).

#### description

Marks a DTO as an annotated message.

#### annotation type

This annotation is bounded to a class.

#### example

```java
@TemplateHeader(start = "+", end = "-")
private class Message{
    ...
}
```


<a name="annotation-skip-bits"></a>
### SkipBits

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `value`: the number of bits to be skipped (can be a SpEL expression).

#### description

Skips `size` bits.

If this should be placed at the end of the message, then a placeholder variable (that WILL NOT be read, and thus can be of any type) should be added.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@SkipBits("3")
@SkipBits("1")
@BindString(size = "4")
public String text1;


@SkipBits("10")
public Void lastUnreadPlaceholder;
```


<a name="annotation-skip-until-terminator"></a>
### SkipUntilTerminator

#### parameters

- `condition`: The SpEL expression that determines if this field has to be read.
- `value`: the byte that terminates the skip.
- `consumeTerminator`: whether to consume the terminator (defaults to `true`).

#### description

Skips bits until a terminator is found.

If this should be placed at the end of the message, then a placeholder variable (that WILL NOT be read, and thus can be of any type) should be added.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@SkipUntilTerminator('x')
@BindString(size = "10")
public String text2;


@SkipUntilTerminator(value = '\0', consumeTerminator = false)
public Void lastUnreadPlaceholder;
```


<a name="annotation-checksum"></a>
### Checksum

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).
 - `skipStart`: how many bytes are to be skipped from the start of the message for the calculation of the checksum (defaults to 0).
 - `skipEnd`: how many bytes are to be skipped from the end of the message for the calculation of the checksum (default to 0).
 - `algorithm`: the algorithm to be applied to calculate the checksum.

#### description

Reads a checksum.

Compute the message checksum and compare it to the read variable once a message has been completely read.
The amount of bytes read depends on the output size of the checksum algorithm.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@Checksum(skipStart = 4, skipEnd = 4, algorithm = CRC16CCITT_FALSE.class)
private short checksum;
```


<a name="annotation-evaluate"></a>
### Evaluate

#### parameters

 - `condition`: The SpEL expression that determines if this field has to be read.
 - `value`: The value to be assigned, or calculated (can be a SpEL expression).

#### description

Assign a constant, calculated value to a field.

Note that the evaluations are done AFTER parsing the entire message.

This annotation with `runLast` to `true` can be used to force a parameter to have a certain value, after perhaps using it to calculate other parameters.
Its effect cannot be undone.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindString(size = "4")
private String messageHeader;

@Evaluate("T(java.time.ZonedDateTime).now()")
private ZonedDateTime receptionTime;

@Evaluate("messageHeader.startsWith('+B')")
private boolean buffered;

//from the variable `deviceTypes` passed in the context
@Evaluate("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
private String deviceTypeName;
```


<a name="annotation-post-process"></a>
### PostProcess

#### parameters

- `condition`: The SpEL expression that determines if this field has to be processed (both in the decode and encode phases).
- `valueDecode`: The value to be assigned, or calculated, at the decode phase (can be a SpEL expression).
- `valueEncode`: The value to be assigned, or calculated, at the encode phase (can be a SpEL expression).

#### description

Assign a constant, or calculated value, to a field after all the other annotations are processed.

Note that the evaluations are done AFTER parsing the entire message in the decode phase, or BEFORE in the encode phase.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@BindString(size = "4")
//this annotation restores the '+ACK' value after all the fields and evaluations are done (this is because the evaluation of `buffered`
//requires the read value of `messageHeader`, and '+BCK' means a buffered message)
@PostProcess(condition = "buffered", valueDecode = "'+ACK'", valueEncode = "'+BCK'")
private String messageHeader;
```


<br/>

<a name="protocol-description"></a>
## Protocol description

A description of the protocol can be obtained through the methods `Describer.describeTemplates` and `Describer.describeTemplate`.

These returns a JSON with a description of all the annotations of the loaded templates.

Example:

```java
DeviceTypes deviceTypes = DeviceTypes.create()
    .with((byte)0x46, "QUECLINK_GB200S");
Core core = CoreBuilder.builder()
    .withContextPair("deviceTypes", deviceTypes)
    .withContext(ParserTest.class.getDeclaredMethod("headerLength"))
    .withDefaultCodecs()
    .withTemplate(ACKMessageHex.class)
   .create();
Describer describer = Describer.create(core);

List<Map<String, Object>> descriptions = describer.describeTemplates();
```

gives as output the following

```json
{
   "header": {
      "start": ["+ACK"],
      "charset": "UTF-8"
   },
   "fields": [
      {
         "charset": "UTF-8",
         "size": "#headerLength()",
         "name": "messageHeader",
         "annotationType": "io.github.mtrevisan.boxon.annotations.bindings.BindString",
         "fieldType": "java.lang.String"
      },
      {
         "converter": "MessageTypeConverter",
         "name": "messageType",
         "annotationType": "io.github.mtrevisan.boxon.annotations.bindings.BindInteger",
         "size": "8",
         "fieldType": "java.lang.String"
      },
      {
         "condition": "mask.hasProtocolVersion()",
         "size": "2",
         "converter": "io.github.mtrevisan.boxon.core.codecs.queclink.QueclinkHelper$VersionConverter",
         "name": "protocolVersion",
         "annotationType": "io.github.mtrevisan.boxon.annotations.bindings.BindArrayPrimitive",
         "type": "byte",
         "fieldType": "java.lang.String",
         "byteOrder": "BIG_ENDIAN"
      }
   ],
   "context": {
      "headerLength": "private static int io.github.mtrevisan.boxon.core.ParserTest.headerLength()",
      "deviceTypes": ["QUECLINK_GB200S(0x46)"]
   }
}
```

<br/>

<a name="annotation-configuration"></a>
## Configuration annotations

Configurations are mainly used to compose a message.

Note that currently only composing in ASCII text format is supported.

<br />
Firstly, load the configuration as shown below:

```java
//add the custom codec to the list of available codecs
//(use one of the lines below)
core.withConfiguration(ConfigurationCustomTest.class); //loads the given configuration
core.withConfigurationsFrom(ConfigurationCustomTest.class); //loads all configuration from the package where the given class resides
```

Then, to retrieve all the possible protocol version boundaries, call

```java
Configurator configurator = Configurator.create(core);
List<String> protocolVersionBoundaries = configurator.getProtocolVersionBoundaries();
```

Then, to retrieve all the messages for a given protocol version, simply call

```java
Configurator configurator = Configurator.create(core);
List<Map<String, Object>> configurationMessages = configurator.getConfigurations("1.35");
```

Moreover, to compose a configuration message (remember to also load the codecs), call

```java
Configurator configurator = Configurator.create(core);
Map<String, Object> configurationData = new HashMap<>();
configurationData.put(Parser.CONFIGURATION_FIELD_TYPE, "AT+");
configurationData.put("Weekday", "TUESDAY|WEDNESDAY");
...

Response<String, byte[]> composedMessage = configurator.composeConfiguration("1.20", "AT+", configurationData);
```


<br/>

<a name="annotation-configurationheader"></a>
### ConfigurationHeader

#### parameters

 - `shortDescription`: a short description of the field, mandatory, used as an identifier (and thus must be unique for every configuration message).
 - `longDescription`: a more expressive description, optional.
 - `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
 - `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
 - `start`: starting text of the message, mandatory, used as an identifier (and thus must be unique for every configuration message).
 - `end`: ending text of the message, optional.
 - `charset`: charset of the message, optional.

#### description

Marks a DTO as an annotated configuration message.

#### annotation type

This annotation is bounded to a class.

#### example

```java
@ConfigurationHeader(shortDescription = "A configuration message", start = "+", end = "-")
private class ConfigurationMessage{
    ...
}
```


<br/>

<a name="annotation-configurationskip"></a>
### ConfigurationSkip

#### parameters

- `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `terminator`: the string that terminates the skip (defaults to empty string), optional.

#### description

Skips a field.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@ConfigurationSkip(minProtocol = "1.2", maxProtocol = "1.3")
@ConfigurationSkip
@ConfigurationField(shortDescription = "A field")
public String text;
```


<br/>

<a name="annotation-configurationfield"></a>
### ConfigurationField

#### parameters

- `shortDescription`: a short description of the field, mandatory, used as an identifier (and thus must be unique inside every configuration message).
- `longDescription`: a more expressive description, optional.
- `unitOfMeasure`: the unit of measure, optional (the format should follow [UCUM](https://ucum.org/ucum.html)/[ISO 80000](https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure) standard).
- `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `minValue`: minimum value this field can assume, optional (alternative to `pattern` and `enumeration`).
- `maxValue`: maximum value this field can assume, optional (alternative to `pattern` and `enumeration`).
- `pattern`: regex pattern this field must obey, optional (alternative to `minValue`/`maxValue` and `enumeration`).
- `enumeration`: enumeration for this field, optional (alternative to `pattern` and `minValue`/`maxValue`). If the field is not an array, then each value of this enum is mutually exclusive.
- `defaultValue`: default value, optional. If the variable is an array, then this field may represent an `or` between values (e.g. `ONE|TWO|THREE`), otherwise can be a single value (e.g. `TWO`). If not present, then the field is mandatory.
- `charset`: charset of the field (if string value), optional.
- `radix`: radix of the number field when written to the message, optional.
- `terminator`: the string that terminates the skip (defaults to empty string), optional.

#### description

Defines a field of the configuration message.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@ConfigurationField(shortDescription = "Report interval", terminator = ",", minProtocol = "1.19", maxProtocol = "1.20",
minValue = "90", maxValue = "86400", defaultValue = "3600", unitOfMeasure = "s")
public int motionlessReportInterval;
```


<br/>

<a name="annotation-compositeconfigurationfield"></a>
### CompositeConfigurationField

#### parameters

- `value`: a set of [CompositeSubField](#annotation-compositesubfield)
- `shortDescription`: a short description of the field, mandatory, used as an identifier (and thus must be unique inside every configuration message).
- `longDescription`: a more expressive description, optional.
- `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `pattern`: regex pattern this field must obey, optional.
- `composition`: the [FreeMarker](https://freemarker.apache.org/) pattern used to compose the field. The short description of each subfield is used as identifier.
- `charset`: charset of the field (if string value), optional.
- `terminator`: the string that terminates the skip (defaults to empty string), optional.

#### description

Defines a composite field of the configuration message.

#### annotation type

This annotation is bounded to a string variable.

#### example

```java
@CompositeConfigurationField(
   value = {
      @CompositeSubField(shortDescription = "URL", pattern = "https?://.{0,92}"),
      @CompositeSubField(shortDescription = "username", pattern = ".{1,32}"),
      @CompositeSubField(shortDescription = "password", pattern = ".{1,32}")
   },
   shortDescription = "Download URL",
   composition = "${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>",
   terminator = ",",
   pattern = ".{0,100}"
)
public String downloadURL;
```


<br/>

<a name="annotation-compositesubfield"></a>
### CompositeSubField

#### parameters

- `shortDescription`: a short description of the field, mandatory, used as an identifier (and thus must be unique inside every configuration message).
- `longDescription`: a more expressive description, optional.
- `unitOfMeasure`: the unit of measure, optional (the format should follow [UCUM](https://ucum.org/ucum.html)/[ISO 80000](https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure) standard).
- `pattern`: regex pattern this field must obey, optional.
- `defaultValue`: default value, optional. If the variable is an array, then this field may represent an `or` between values (e.g. `ONE|TWO|THREE`), otherwise can be a single value (e.g. `TWO`). If not present, then the field is mandatory.

#### description

Defines a subfield of a composite field of the configuration message.

#### annotation type

This annotation is bounded to a string variable.

#### example

```java
@CompositeConfigurationField(
   value = {
      @CompositeSubField(shortDescription = "URL", pattern = "https?://.{0,92}"),
      @CompositeSubField(shortDescription = "username", pattern = ".{1,32}"),
      @CompositeSubField(shortDescription = "password", pattern = ".{1,32}")
   },
   shortDescription = "Download URL",
   composition = "${URL}<#if username?has_content && password?has_content>@${username}@${password}</#if>",
   terminator = ",",
   pattern = ".{0,100}"
)
public String downloadURL;
```


<br/>

<a name="annotation-alternativeconfigurationfield"></a>
### AlternativeConfigurationField

#### parameters

- `value`: a set of [AlternativeSubField](#annotation-alternativesubfield)
- `shortDescription`: a short description of the field, mandatory, used as an identifier (and thus must be unique inside every configuration message).
- `longDescription`: a more expressive description, optional.
- `unitOfMeasure`: the unit of measure, optional (the format should follow [UCUM](https://ucum.org/ucum.html)/[ISO 80000](https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure) standard).
- `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `enumeration`: enumeration for this field, optional. If the field is not an array, then each value of this enum is mutually exclusive.
- `terminator`: the string that terminates the skip (defaults to empty string), optional.

#### description

Defines an alternative field of the configuration message.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@AlternativeConfigurationField(
   shortDescription = "Download protocol", terminator = ",", enumeration = DownloadProtocol.class,
   value = {
      @AlternativeSubField(maxProtocol = "1.35", defaultValue = "HTTP"),
      @AlternativeSubField(minProtocol = "1.36", defaultValue = "HTTPS")
   }
)
private DownloadProtocol downloadProtocol;
```


<br/>

<a name="annotation-alternativesubfield"></a>
### AlternativeSubField

#### parameters

- `longDescription`: a more expressive description, optional.
- `unitOfMeasure`: the unit of measure, optional (the format should follow [UCUM](https://ucum.org/ucum.html)/[ISO 80000](https://en.wikipedia.org/wiki/Unified_Code_for_Units_of_Measure) standard).
- `minProtocol`: minimum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `maxProtocol`: maximum protocol for which this configuration message is valid, optional (should follow [Semantic Versioning](https://semver.org/)).
- `minValue`: minimum value this field can assume, optional (alternative to `pattern` and `enumeration`).
- `maxValue`: maximum value this field can assume, optional (alternative to `pattern` and `enumeration`).
- `pattern`: regex pattern this field must obey, optional.
- `defaultValue`: default value, optional. If the variable is an array, then this field may represent an `or` between values (e.g. `ONE|TWO|THREE`), otherwise can be a single value (e.g. `TWO`). If not present, then the field is mandatory.
- `charset`: charset of the field (if string value), optional.
- `radix`: radix of the number field when written to the message, optional.

#### description

Defines a subfield of an alternative field of the configuration message.

#### annotation type

This annotation is bounded to a variable.

#### example

```java
@AlternativeConfigurationField(
   shortDescription = "Download timeout", terminator = ",", unitOfMeasure = "min",
   value = {
      @AlternativeSubField(maxProtocol = "1.18", minValue = "5", maxValue = "30", defaultValue = "10"),
      @AlternativeSubField(minProtocol = "1.19", minValue = "5", maxValue = "30", defaultValue = "20")
   }
)
private int downloadTimeout;
```


<br/>

<a name="describer"></a>
## Describer

Return a description of the loaded templates and configuration.
It basically provides a description of the annotations in JSON format.

```java
Describer describer = Describer.create(core);

List<Map<String, Object>> templateDescriptions = describer.describeTemplates();
List<Map<String, Object>> configurationDescriptions = describer.describeConfiguration();
```


<br/>

<a name="extractor"></a>
## Extractor

Extract values from a DTO using <a href="https://tools.ietf.org/html/rfc6901">RFC6901</a> syntax.

```java
Parser parser = Parser.create(core);
List<Response<byte[], Object>> result = parser.parse(payload);
ACKMessageASCII parsedMessage = (ACKMessageASCII)result.get(0).getMessage();
Extractor extractor = Extractor.create(parsedMessage);

String messageHeader = extractor.get("/messageHeader");
int protocolVersionMinor = extractor.get("/protocolVersion/minor");
```


<br/>

<a name="how-to-spel"></a>
## How to write SpEL expressions

Care should be taken in writing [SpEL expressions](https://docs.spring.io/spring-framework/reference/core/expressions.html) for the fields `condition`, and `size`.

The root object is the outermost object.

In order to evaluate a variable of a parent object the complete path should be used, from the root to the desired property, as in `object1.variable1`.

In order to evaluate a variable of a children object, that is the object currently scanned, the relative path should be used introduced by the special keyword `#self`, as in `#self.variable2`.<br />
Note that the `#` prefix is intended to refer to the context (whether it is an object or a method), and the current object is stored in the context under the name `self`.

See also [Spring Expression Language (SpEL) Primer](https://dhruba.wordpress.com/2009/12/30/spring-expression-language-spel-primer/).

### Example:

```java
class A{
    @BindInteger(size = "8")
    private byte value;

    @BindObject(type = OtherClass.class)
    private OtherClass other;

    @BindString(condition = "value == 2", size = "1")
    private String var3;
}

class OtherClass{
    @BindString(condition = "value == 1", size = "1")
    private String var1;
    @BindString(condition = "#self.var1.equals('2')", size = "1")
    private String var2;
}
```

<br/>

<a name="how-to-extend"></a>
## How to extend the functionalities

Boxon can handle array of primitives, bit, byte, short, int, long, float, double, and their object counterpart, as long as Object, BigInteger, string (with a given size, or with a terminator), and the special "[checksum](#annotation-checksum)".

You can extend the basic functionalities through the application of converters as shown below in some examples. Here lies the power of Boxon.

Boxon already provides some build-in converters: BitsToBoolean, ShortToChar, UnsignedByte, UnsignedInt, and UnsignedShort.

<a name="how-to-converters"></a>
### Converters

NOTE that `decode` and `encode` MUST BE the inverse of each other, that is they MUST BE invertible (injective), or partly invertible, that is, otherwise said, `decode(x) = y iff encode(y) = x` (eventually in a restricted domain).

#### DateTime converter (from Unix timestamp to ZonedDateTime)

```java
@BindInteger(size = "64", converter = UnixTimestampConverter.class)
private LocalDateTime eventTime;

public class UnixTimestampConverter implements Converter<Long, LocalDateTime>{
    @Override
    public LocalDateTime decode(final Long unixTimestamp){
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC);
    }

    @Override
    public Long encode(final LocalDateTime value){
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
```

#### DateTime converter (from YYYYMMDDHHMMSS as bytes to ZonedDateTime)

```java
@BindArrayPrimitive(size = "7", type = byte.class, converter = DateTimeYYYYMMDDHHMMSSConverter.class)
private ZonedDateTime eventTime;

public class DateTimeYYYYMMDDHHMMSSConverter implements Converter<byte[], ZonedDateTime>{
    @Override
    public ZonedDateTime decode(final byte[] value){
        final ByteBuffer bb = ByteBuffer.wrap(value);
        final int year = bb.getShort();
        final int month = bb.get();
        final int dayOfMonth = bb.get();
        final int hour = bb.get();
        final int minute = bb.get();
        final int second = bb.get();
        return DateTimeUtils.createFrom(year, month, dayOfMonth, hour, minute, second);
    }

    @Override
    public byte[] encode(final ZonedDateTime value){
        return ByteBuffer.allocate(7)
            .putShort((short)value.getYear())
            .put((byte)value.getMonthValue())
            .put((byte)value.getDayOfMonth())
            .put((byte)value.getHour())
            .put((byte)value.getMinute())
            .put((byte)value.getSecond())
            .array();
    }
}
```

#### IMEI converter (from 'nibble' array to String, that is, each nibble represents a character of the IMEI)

```java
@BindArrayPrimitive(size = "8", type = byte.class, converter = IMEIConverter.class, validator = IMEIValidator.class)
private String imei;

public class IMEIConverter implements Converter<byte[], String>{
    @Override
    public String decode(final byte[] value){
        final StringBuilder sb = new StringBuilder(15);
        for(int i = 0; i < 7; i ++)
            sb.append(String.format("%02d", value[i] & 255));
        sb.append(ByteHelper.applyMaskAndShift(value[7], Byte.SIZE, (byte)0x0F));
        return sb.toString();
    }

    @Override
    public byte[] encode(final String value){
        final byte[] imei = new byte[8];
        final String[] components = value.split("(?<=\\G\\d{2})", 8);
        for(int i = 0; i < 8; i ++)
            imei[i] = Integer.valueOf(components[i]).byteValue();
        return imei;
    }
}
```

#### RSSI converter (from encoded byte to short value)

```java
@BindInteger(size = "8", converter = RSSIConverter.class)
private short rssi;

/**
 * input:   output:
 * -----------------------
 * 0:      < -133 dBm
 * 1:      -111 dBm
 * 2-30:   -109 - -53 dBm
 * 31:      > -51 dBm
 * 99:      unknown
 */
public class RSSIConverter implements Converter<Byte, Short>{

    public static final int RSSI_UNKNOWN = 0;

    @Override
    public Short decode(final Byte value){
        if(value == null)
            return null;
        if(value == 0)
            //< -133 dBm
            return (byte)-133;
        if(value == 99)
            return RSSI_UNKNOWN;
        //31 is > -51 dBm
        return (short)(value * 2 - 133);
    }

    @Override
    public Byte encode(final Short value){
        if(value == null)
            return null;
        if(value == -133)
            return 0;
        if(value == RSSI_UNKNOWN)
            return 99;
        return (byte)((value + 133) / 2);
    }
}
```

<a name="how-to-annotations"></a>
### Custom annotations

You can also define your own annotation by define an annotation and implementing `CodecInterface` as in the following example.

Optionally, the method `String condition()` could be defined.

<b>... and remember to add it to the `Parser`!</b>

```java
//annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface VarLengthEncoded{}
```

```java
//codec
//the number of bytes to read is determined by the leading bit of each individual bytes
//(if the first bit of a byte is 1, then another byte is expected to follow)
class VariableLengthByteArray implements CodecInterface<VarLengthEncoded>{
    public Object decode(TemplateParser templateParser, BitBuffer reader, VarLengthEncoded annotation, Object data){
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean continuing = true;
        while(continuing){
            final byte b = reader.getByte();
            baos.write(b & 0x7F);

            continuing = ((b & 0x80) != 0x00);
        }
        return baos.toByteArray();
    }

    public void encode(TemplateParser templateParser, BitWriter writer, VarLengthEncoded annotation, Object data, Object value){
        final int size = Array.getLength(value);
        for(int i = 0; i < size; i ++)
            writer.put((byte)((byte)Array.get(value, i) | (i < size - 1? (byte)0x80: 0x00)), ByteOrder.BIG_ENDIAN);
    }
}
```

```java
//add the custom codec to the list of available codecs
//(use one of the lines below)
core.withDefaultCodecs(); //loads all codecs from the package where this call was made
core.withCodec(CodecCustomTest.class); //this class is where the custom codec resides
core.withCodec(new VariableLengthByteArray());
```

<br/>

<a name="digging"></a>
## Digging into the code

Almost for each base annotation there is a corresponding class defined into `Template.java` that manages the encoding and decoding of the underlying data.

The other annotations are managed directly into `TemplateParser.java`, that is the main class that orchestrates the parsing of a single message with all of its annotations.
If an error occurs an `AnnotationException` (an error occurs on an annotation definition), `CodecException` (an error occurs while finding the appropriate codec), `TemplateException` (an error occurs if a template class is badly annotated), or `DecodeException`/`EncodeException` (a container exception for the previous ones for decoding and encoding respectively) is thrown.

Messages can be concatenated, and the `Parser.java` class manages them, returning a [DTO](https://en.wikipedia.org/wiki/Data_transfer_object), `ParserResponse.java`, which contains a list of all successfully read messages and a list of all errors from problematic messages.

<br/>

Each annotated class is processed by `Template.class`, that is later retrieved by `Parser.java` depending on the starting header.
For that reason each starting header defined into `TemplateHeader` annotation MUST BE unique. This class can also accept a context.

All the SpEL expressions are evaluated by `Evaluator.java`.

<br/>

All the annotated classes are conveniently loaded using the `Loader.java` as is done automatically in the `Parser.java`.

If you want to provide your own classes you can use the appropriate `with...` method of `Parser`.

<br/>

The `Parser` is also used to encode a message.

<br/>

`BitBuffer.java` has the task to read the bits, whereas `BitWriter.java` has the task to write the bits.

<br/>

`BitSet.java` is the container for the bits (like `java.utils.BitSet`, but enhanced for speed).

<br/>

`ByteOrder.java` is the enum that is used to indicate the byte order.


<br/>

<a name="examples"></a>
## Examples

<a name="example-multi"></a>
### Multi-message parser

All you have to care about, for a simple example on multi-message automatically-loaded templates, is the `Parser`.

```java
//optionally create a context
Map<String, Object> context = ...
Core core = CoreBuilder.builder()
   .withContext(context)
   .withContext(VersionHelper.class, "compareVersion", String.class, String.class)
   .withContext(VersionHelper.class.getDeclaredMethod("compareVersion", new Class[]{String.class, String.class}))
   .withDefaultCodecs()
   .withTemplate(...)
   .create();
Parser parser = Parser.create(core);

//parse the message
byte[] payload = ...
List<Response<byte[], Object>> result = parser.parse(payload);

//process the successfully parsed messages and errors
for(int index = 0; index < result.size(); index ++){
   Response<byte[], Object> response = result.get(index);
   Object parsedMessage = response.getMessage();
   Exception error = response.getError();

   if(error != null){
      LOGGER.error("An error occurred while parsing:\r\n   {}", response.getSource());
   }
   else if(parsedMessage != null){
      ...
   }
}
```

<a name="example-composer"></a>
### Message composer

The inverse of parsing is composing, and it's simply done as follows.

```java
//compose the message (`Template` should be a recognized template)
Template data = ...;
Response<Template, byte[]> composeResult = composer.compose(data);

//process the composed messages
byte[] composedMessage = response.getMessage();
Exception error = response.getError();

if(error != null){
   LOGGER.error("An error occurred while composing:\r\n   {}", response.getSource());
}
else if(composedMessage != null){
   ...
}
```


<br/>

<a name="contributing"></a>
## Contributing

Please report issues to the [issue tracker](https://github.com/mtrevisan/boxon/issues) if you have any difficulties using this module, found a bug, or request a new feature.

Pull requests are welcomed.


<br/>

<a name="changelog"></a>
## Changelog

<a name="changelog-4.0.0"></a>
### version 4.0.0 - 20240419

- Incompatibility between read type, converter input, converter output, validator, and field type is now resolved during core construction, rather than at runtime.
- Fixed error while handling of `SkipBits` and `SkipUntilTerminator` to allow processing multiple instances.
- Removed useless `BindFloat` and `BindDouble` has they are rarely used and can be substituted by the appropriate converters (`IntegerToFloatConvert` and `LongToDoubleConverter`).
- Removed useless `BindByte`, `BindShort`, `BindInt`, and `BindLong` as they can be substituted by `BindInteger`.
- Renamed `Descriptor` into the more meaningful `Describer`.
- Added builder to `Version`.
- Many smells removed, major code refactor.

<a name="changelog-3.6.0"></a>
### version 3.6.0 - 20240413

- Specialized `Skip` annotation into `SkipBits` and `SkipUntilTerminator`.
- Added subtypes describer on `BindObject`, `BindArray`, `BindList`, `ObjectChoices.ObjectChoice`, and `ObjectChoicesList.ObjectChoiceList`.
- Some minor improvements.

<a name="changelog-3.5.1"></a>
### version 3.5.1 - 20240409

- Fix error while assessing size value.
- Corrected `NullObjectChoice` and `NullObjectChoiceList` type value.
- Fix error while putting numeric value in `NumberWriterManager`.
- Removed the default on `Skip` size: it was intended to be mandatory.
- Fixed bug on evaluating an expression on a deep nested object.
- (minor) Fix missing field name in parser context in `TemplateParser.encode`.

<a name="changelog-3.5.0"></a>
### version 3.5.0 - 20240406

- Added condition on `Checksum` annotation.
- Removed `type` and `startValue` from `Checksum`: the same information can be retrieved from the algorithm used, plus, the start value can be embedded in the implemented class.
- Fixed a problem while converting a numeric string to number.
- Make record classes work with annotations.
- Slightly reduced memory footprint and improved execution time.

<a name="changelog-3.4.0"></a>
### version 3.4.0 - 20240318

- Removed `runLast` from `Evaluator`, added a specialized annotation that can work in both decoding and encoding phases.
- Added method `describeParser` to `Descriptor` to also include the description of evaluation and post-processing fields.

<a name="changelog-3.3.0"></a>
### version 3.3.0 - 20240317

- Added description of configuration.
- Changed the key to reference a configuration (from `start` to `shortDescription`).
- Corrected generation of parameter `MutuallyExclusive` on configuration description.
- Corrected log text on enumeration error.
- Fixed a test that occasionally fails.

<a name="changelog-3.2.0"></a>
### version 3.2.0 - 20240312

- Added `runLast` to `Evaluator` to run an evaluation after all other evaluations have been made (used, for example, to restore a default value after being used by another evaluator).

<a name="changelog-3.1.3"></a>
### version 3.1.3 - 20240311

- Fixed duplicated descriptions.
- Fixed validation on max value while composing a message.
- Fixed number not written with the correct radix.
- Made `shortDescription` mandatory in the annotation, as it should have been.
- Added method to map a DTO into a `Map<String, Object>` in `ReflectionHelper`.
- Added method `Configurator.composeConfiguration` accepting a DTO.
- Corrected errors in the documentation.
- Migrated from java 11 to java 21 for performance.

<a name="changelog-3.1.2"></a>
### version 3.1.2 - 20240302

- Improvement on handling values inside big decimal converter.
- Improvement on error reporting.
- Renamed `Composer.composeMessage` into `compose`.
- Corrected error while showing the start array of message header in the description.
- Fix size validation of array and list (now it can be zero).

<a name="changelog-3.1.1"></a>
### version 3.1.1 - 20240229

- Fixed an error if annotating with `@Skip` as the last annotation of the DTO.

<a name="changelog-3.1.0"></a>
### version 3.1.0 - 20240228

- Added `@BindList`, the equivalent of `@BindArray` for messages with separators.

<a name="changelog-3.0.2"></a>
### version 3.0.2 - 20240223

- Fixed a bug on `@ConfigurationHeader` where the protocol range check was incorrectly done considering the minimum protocol as the maximum.

<a name="changelog-3.0.1"></a>
### version 3.0.1 - 20240220

- Updated library versions.

<a name="changelog-3.0.0"></a>
### version 3.0.0 - 20220313

- Added `CoreBuilder` to facilitate the creation of a `Core`: now it is no longer necessary to remember the order in which the methods should be called.
- Added missing javadoc. Enhanced existing javadoc.
- Added `@BindBitSet` binding for java `@BitSet`.
- Added `Extractor`, used to programmatically extract values from a DTO.
- Removed `Bits`.
- Enhanced binding validation.
- Fixed a concurrency bug on the validation of alternatives.
- Reordered some packages to better reflect usage.

<a name="changelog-2.1.2"></a>
### version 2.1.2 - 20210118

- Added missing javadoc.
- No more cycles between classes or packages.

<a name="changelog-2.1.1"></a>
### version 2.1.1 - 20210114

- Bug fix: `Evaluator` class is now exportable.
- Removed a package cycle.
- General cleaning of the code (removed duplicated code, useless templates, etc.).

<a name="changelog-2.1.0"></a>
### version 2.1.0 - 20211213

- Made library thread-safe.
- Added methods to retrieve a description of the protocol (in JSON format).
- Decomposed and simplified `Parser` class.

<a name="changelog-2.0.0"></a>
### version 2.0.0 - 20211127

- Completely revised the packages, solving a lot of structural problems and refactorings that have to be done.
- Added methods to retrieve the configurations, a.k.a. a JSON that tells the configuration parameters of certain annotated messages.

<a name="changelog-1.1.0"></a>
### version 1.1.0 - 20200901

- Better handling of NOP logger.
- Abandoned [Reflections](https://github.com/ronmamo/reflections) in favor of [ClassGraph](https://github.com/classgraph/classgraph).
- Added `BindArray.selectDefault` and `BindObject.selectDefault` to cope with default selector that has no prefix.
- Added some feasibility checks on annotation data.
- Added public constructor to `Parser` to allow for extensions.
- Changed the signature of `Checksummer.calculateChecksum` returning short instead of long.
- Changed method `Validator.validate` into `Validator.isValid`.
- Changed method `ParserResponse.getMessageForError` into `ParserResponse.getErrorMessageAt` to align it to other method name's conventions.
- Moved classes `ParserResponse` and `ComposerResponse` from `io.github.mtrevisan.boxon.external` to `io.github.mtrevisan.boxon.core` in order to hide add methods; the constructors are also hidden.
- Minor refactorings.
- Added `originator` variable (and its getter) to `ComposerResponse` to hold the given objects used to create the message.
- Added/modified javadocs to better explain some classes.
- Removed `ComposerResponse.getErrors`, `BindInteger.unsigned` and `BitReader.getInteger(int, ByteOrder, boolean)` as they are useless.
- Removed `BitWriter.putText(String, byte, boolean)` because of the [Boolean Trap](https://ariya.io/2011/08/hall-of-api-shame-boolean-trap).
- Removed useless `match()` parameter from bindings.
- Enhanced the exception message thrown if the type of `BitReader.get(Class, ByteOrder)` is not recognized.
- Renamed `BindChecksum` into `Checksum`.
- Relocated all binding annotations inside `annotations.bindings` (`Bind*` and `*Choices`).
- Corrected bug while reading skips in `TemplateParser.decode`.

<a name="changelog-1.0.0"></a>
### version 1.0.0 - 20200825

- Speed-up execution.
- Revision of the packages with removal of cycles.
- Better handling of class retrieval (codecs and templates).

<a name="changelog-0.0.2"></a>
### version 0.0.2 - 20200731

- Final revision.

<a name="changelog-0.0.1"></a>
### version 0.0.1 - 20200721

- First revision.
- Some more thoughts on how it should work.

<a name="changelog-0.0.0"></a>
### version 0.0.0 - 20200629

- First version.


<br/>

<a name="license"></a>
## License

This project is licensed under [MIT license](http://opensource.org/licenses/MIT).
For the full text of the license, see the [LICENSE](LICENSE) file.
