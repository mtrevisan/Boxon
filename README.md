# Boxon _[boˈzoŋ]_

![Java-11+](https://img.shields.io/badge/java-11%2B-orange.svg) [![License: GPL v3](https://img.shields.io/badge/License-MIT-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<a href="https://codeclimate.com/github/mtrevisan/Boxon/maintainability"><img src="https://api.codeclimate.com/v1/badges/bff8577200d792e1e197/maintainability" /></a>

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)
==========

<br />

## Forewords
Like [Preon](https://github.com/preon/preon) (currently not maintained anymore), but the code is understandable, shorter, easier to extend, uses the more powerful (and maintained) [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html), and the documentation is __really__ free...

This is a declarative, bit-level, message parser. All you have to do is write a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) that represents your message and annotate it. That's all. [Boxon](https://en.wikipedia.org/wiki/Boson) will take care of the rest for you.

If you want to use the parser straight away, just go [there](#examples).

<br/>

### Notable features
Boxon...
 - is easily extensible through the use of [converters](#how-to).
 - contains a minimal set of [annotations](#annotation-base) capable of handling all the primitive data.
 - contains a set of [special annotations](#annotation-special) that handles the various messages peculiarities (conditional bindings, skip bits/bytes, checksum, 'constant' assignments)
 - is capable of handle concatenation of messages, using the correct protocolMessage under the hood.
 - can handle [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html) on certain fields, thus more powerful and simpler than [Limbo](http://limbo.sourceforge.net/apidocs/)<sup>[1](#footnote-1)</sup> (but less than [janino](https://github.com/janino-compiler/janino), that has other problems).
 - can do decode and encode data on the fly with a single annotated class (thus avoiding separate decoder and encoder going out-of-sync).
 - has protocolMessages that are not complex: they do not call each other uselessly complicating the structure (apart, necessarily, for `@BindArray`), no complicated chains of factories: it's just a parser that works.
 - supports [SLF4J](http://www.slf4j.org/).
 - hides the complexities of encoding and decoding, thus simplifying the changes to be made to the code due to frequent protocol changes.

### Differences from...
#### Preon
Boxon differs from Preon in...
 - does not have a generic `Bound` annotation, as it does not have the need to read the native byte order of a particular machine in which the code is running: this is because the bytes of the message have little chance to be generated from the very same machine that will parse its messages, what if a message consider 24 bits as an Integer? If the code should be portable and installed and run everywhere it should not rely on the native properties of any machine.
   Moreover, `@Bound boolean visible;` is 1 bit- or 1 byte-length?
 - does not have `BoundList`: since the message is a finite sequence of bytes, then any array is of finite length, and thus the standard java array (`[]`) is sufficient. If someone wants a `List` (s)he could use a Converter.
 - does not rely on the type of the annotated variable (because of the converters); in fact, the annotation, eventually, serves the purpose to pass a predefined type of data to a converter.<br/>
   For this reason too, there is no need for the `Init` annotation, thus the annotated file can contain the least amount of data necessary for its decoding (moreover, this annotation has NOT the inverse operation, so it seems to me... so it's pretty useless anyway).
 - (by personal experience) enums can have different representations, or change between a version and the following of a protocol, so having an annotation that tells the value of a particular element of this enum is at least risky. So, for this reason, the `BoundEnumOption` is not present in this library.
 - does read and write more than 64 bits at a time (`BitBuffer.readBits`)

<br/>
---

<a name="footnote-1"></a>
<sub><sup>1</sup> Currently Limbo is merged with Preon... thus rendering Preon not only a parser, but also an evaluator, over-complicating and cluttering the code.</sub>

<br/>

### Pre-compiled executables
Get them [here](https://github.com/mtrevisan/Boxon/releases/).


<br/>
<br/>

## Table of Contents
1. [Base annotations](#annotation-base)
    1. [BindObject](#annotation-bindobject)
    2. [BindArray](#annotation-bindarray)
    3. [BindArrayPrimitive](#annotation-bindarrayprimitive)
    4. [BindBits](#annotation-bindbits)
    5. [BindByte](#annotation-bindbyte)
    6. [BindShort](#annotation-bindshort)
    7. [BindInt](#annotation-bindint)
    8. [BindLong](#annotation-bindlong)
    9. [BindInteger](#annotation-bindinteger)
    10. [BindFloat](#annotation-bindfloat)
    11. [BindDouble](#annotation-binddouble)
    12. [BindDecimal](#annotation-binddecimal)
    13. [BindString](#annotation-bindstring)
    14. [BindStringTerminated](#annotation-bindstringterminated)
2. [Special annotations](#annotation-special)
    1. [MessageHeader](#annotation-messageheader)
    2. [Skip](#annotation-skip)
    3. [Checksum](#annotation-checksum)
    4. [Evaluate](#annotation-evaluate)
3. [How to extend the functionalities](#how-to)
4. [Digging into the code](#digging)
    1. [Converters](#how-to-converters)
    2. [Custom annotations](#how-to-annotations)
5. [Examples](#examples)
    1. [Multi-message parser](#example-multi)
    2. [Message composer](#example-composer)
6. [Changelog](#changelog)
    1. [version 1.0.0](#changelog-1.0.0)

<br/>

<a name="annotation-base"></a>
## Base annotations
Here the build-in base annotations are described.

You can use them as a starting point to build your own customized readers.

<a name="annotation-bindobject"></a>
### BindObject

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `type`: the Class of the Object of the single element of the array (defaults to `Object`).
 - `selectFrom`: the selection from which to choose the instance type.
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
    @BindByte
    public byte major;
    @BindByte
    public byte minor;
    public byte build;
}

@BindBits(size = "1", converter = BitToBooleanConverter.class)
private boolean versionPresent;
@BindObject(condition = "versionPresent", type = Version.class)
private Version version;
```


<a name="annotation-bindarray"></a>
### BindArray

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `type`: the Class of the Object of the single element of the array (defaults to `Object`).
 - `size`: the size of the array (can be a SpEL expression).
 - `selectFrom`: the selection from which to choose the instance type.
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
    @BindByte
    public byte major;
    @BindByte
    public byte minor;
    public byte build;
}

@BindArray(size = "2", type = Version.class)
private Version[] versions;
```

```java
@BindByte
private byte positionsCount;
@BindArray(size = "positionsCount", type = Position.class,
   selectFrom = @ObjectChoices(prefixSize = 8,
        alternatives = {
          @ObjectChoices.ObjectChoice(condition = "#prefix == 0", prefix = 0, type = PositionInvalid.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 1", prefix = 1, type = PositionAbsolute.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 2", prefix = 2, type = PositionRelative.class),
          @ObjectChoices.ObjectChoice(condition = "#prefix == 3", prefix = 3, type = PositionSameAsPrevious.class)
       }
    ),
   converter = PositionsConverter.class)
private Position[] positions;
```


<a name="annotation-bindarrayprimitive"></a>
### BindArrayPrimitive

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
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
@BindBits(size = "1", converter = BitToBooleanConverter.class)
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


<a name="annotation-bindbits"></a>
### BindBits

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `size`: the number of bits to read (can be a SpEL expression).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value (the value to match must be something like `[1, 12]`, where the numbers are the position of the set bits in ascending order).
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a `BitMap`.

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindBits(size = "2")
private BitMap bits;
```


<a name="annotation-bindbyte"></a>
### BindByte

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a byte (or Byte).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindByte
public Byte mask;
```


<a name="annotation-bindshort"></a>
### BindShort

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a short (or Short).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindShort
private short numberShort;
```


<a name="annotation-bindint"></a>
### BindInt

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads an int (or Integer).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindInt
private int numberInt;
```


<a name="annotation-bindlong"></a>
### BindLong

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a long (or Long).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindLong
private long numberLong;
```


<a name="annotation-bindinteger"></a>
### BindInteger

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `size`: the number of bits to read (can be a SpEL expression).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `unsigned`: whether the resulting number is to be treated as unsigned, defaults to `true`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
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

@BindInteger(size = "Long.SIZE+10")
private BigInteger number;
```


<a name="annotation-bindfloat"></a>
### BindFloat

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a float (or Float).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindFloat
private float number;
```


<a name="annotation-binddouble"></a>
### BindDouble

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a double (or Double).

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindDouble
private double number;
```


<a name="annotation-binddecimal"></a>
### BindDecimal

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `type`: the Class of variable to be read (SHOULD BE `Float.class`, or `Double.class`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the converter).
 - `converter`: the converter used to convert the read value into the value that is assigned to the annotated variable. 
 - `selectConverterFrom`: the selection from which to choose the converter to apply (the `converter` parameter can be used as a default converter whenever no converters are selected from this parameter).

#### description
Reads a float or decimal (or Float or Double), depending on `type`, as a BigDecimal.

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindDecimal(type = Double.class)
private BigDecimal number;
```


<a name="annotation-bindstring"></a>
### BindString

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).
 - `size`: the size of the string (can be a SpEL expression).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
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
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `charset`: the charset to be interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).
 - `terminator`: the byte that terminates the string (defaults to `\0`).
 - `consumeTerminator`: whether to consume the terminator (defaults to `true`).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
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

<a name="annotation-messageheader"></a>
### MessageHeader

#### parameters
 - `start`: an array of possible start sequences (as string) for this message (defaults to empty).
 - `end`: a possible end sequence (as string) for this message (default to empty).
 - `charset`: the charset to be interpreted the `start` and `end` strings into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).

#### description
Marks a POJO as an annotated message.

#### annotation type
This annotation is bounded to a class.

#### example
```java
@MessageHeader(start = "+", end = "-")
private class Message{
    ...
}
```


<a name="annotation-skip"></a>
### Skip

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `size`: the number of bits to be skipped (can be a SpEL expression).
 - `terminator`: the byte that terminates the skip (defaults to `\0`).
 - `consumeTerminator`: whether to consume the terminator (defaults to `true`).

#### description
Skips `size` bits, or until a terminator is found.

If this should be placed at the end of the message, then a placeholder variable (that WILL NOT be read, and thus can be of any type) should be added.

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@Skip(size = "3")
@Skip(size = "1")
@BindString(size = "4")
public String text1;

@Skip(terminator = "x", consumeTerminator = false)
@BindString(size = "10")
public String text2;


@Skip(size = "10")
public Void lastUnreadPlaceholder;
```


<a name="annotation-checksum"></a>
### Checksum

#### parameters
 - `type`: the Class of variable to be read.
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).
 - `skipStart`: how many bytes are to be skipped from the start of the message for the calculation of the checksum (defaults to 0).
 - `skipEnd`: how many bytes are to be skipped from the end of the message for the calculation of the checksum (default to 0).
 - `algorithm`: the algorithm to be applied to calculate the checksum.

#### description
Reads a checksum.

Compute the message checksum and compare it to the read variable once a message has been completely read.

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindChecksum(type = short.class, skipStart = 4, skipEnd = 4, algorithm = CRC16.class, startValue = CRC16.START_VALUE_0xFFFF)
private short checksum;
```


<a name="annotation-evaluate"></a>
### Evaluate

#### parameters
 - `condition`: The SpEL expression that determines if this field has to be read (NOTE that the root object is the outmost object, in order to evaluate a variable of a parent object the complete path should be used, as in `#root.object1.object2.variable`).
 - `value`: the value to be assigned, or calculated (can be a SpEL expression).

#### description
Assign a constant value to a field (not present in the message).

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


<br/>

<a name="how-to"></a>
## How to extend the functionalities
Boxon can handle array of primitives, bit, byte, short, int, long, float, double, and their object counterpart, as long as Object, BigInteger, BigDecimal, string (with a given size, or with a terminator), and the special "[checksum](#annotation-checksum)".

You can extend the basic functionalities through the application of converters as shown below in some examples. Here lies the power of Boxon.

<a name="how-to-converters"></a>
### Converters

#### DateTime converter (from Unix timestamp to ZonedDateTime)
```java
@BindLong(converter = DateTimeUnixConverter.class)
private ZonedDateTime eventTime;

public class DateTimeUnixConverter implements Converter<Long, ZonedDateTime>{
    @Override
    public ZonedDateTime decode(final Long unixTimestamp){
        return DateTimeUtils.createFrom(unixTimestamp);
    }

    @Override
    public Long encode(final ZonedDateTime value){
        return value.toEpochSecond();
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

#### IMEI converter (from 'nibble' array to String)
```java
@BindArrayPrimitive(size = "8", type = byte.class, converter = IMEIConverter.class, validator = IMEIValidator.class)
private String imei;

public class IMEIConverter implements Converter<byte[], String>{
    @Override
    public String decode(final byte[] value){
        final StringBuilder sb = new StringBuilder();
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
@BindByte(converter = RSSIConverter.class)
private short rssi;

/**
 * input:	output:
 * -----------------------
 * 0:		< -133 dBm
 * 1:		-111 dBm
 * 2-30:	-109 - -53 dBm
 * 31:		> -51 dBm
 * 99:		unknown
 */
public class RSSIConverter implements Converter<Byte, Short>{

    public static final int RSSI_UNKNOWN = 0;

    @Override
    public Short decode(final Byte value){
        if(value == 0)
            //< -133 dBm
            return (byte)-133;
        if(value == 99)
            return RSSI_UNKNOWN;
        //31 is > -51 dBm
        return (short)(value * 2 - 113);
    }

    @Override
    public Byte encode(final Short value){
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

<b>... and remember to add it to the `Codec`s!</b>

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
    public Object decode(ProtoclMessageParser protocolMessageParser, BitBuffer reader, VarLengthEncoded annotation, Object data){
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean continuing = true;
        while(continuing){
            final byte b = reader.getByte();
            baos.write(b & 0x7F);

            continuing = ((b & 0x80) != 0x00);
        }
        return baos.toByteArray();
    }

    public void encode(ProtoclMessageParser protocolMessageParser, BitWriter writer, VarLengthEncoded annotation, Object data, Object value){
        final int size = Array.getLength(value);
        for(int i = 0; i < size; i ++)
            writer.put((byte)((byte)Array.get(value, i) | (i < size - 1? (byte)0x80: 0x00)), ByteOrder.BIG_ENDIAN);
    }

    public Class<?> codecType(){
        return VariableLengthByteArray.class;
    }
}
```

```java
//add the custom codec to the list of available codecs
//(use one of the lines below)
parser.withDefaultCodecs(); //loads all codecs from the package where this call was made
parser.withCodecs(CodecCustomTest.class); //this class is where the custom codec resides
parser.withCodecs(Arrays.asList(new VariableLengthByteArray()));
parser.addCodec(new VariableLengthByteArray()); //adds a single codec
```

<br/>

<a name="digging"></a>
## Digging into the code
Almost for each base annotation there is a corresponding class defined into `Codec.java` that manages the encoding and decoding of the underlying data.

The other annotations are managed directly into `ProtoclMessageParser.java`, that is the main class that orchestrates the parsing of a single message with all of its annotations.
If an error occurs a `ParseException` is thrown.

Messages can be concatenated, and the `Parser.java` class manages them, returning a [DTO](https://en.wikipedia.org/wiki/Data_transfer_object), `ParseResponse.java`, with a list of all successfully read messages and a list of all errors from problematic messages.

<br/>

Each annotated class is processed by `ProtocolMessage.class`, that is later retrieved by `Parser.java` depending on the starting header.
For that reason each starting header defined into `MessageHeader` annotation SHOULD BE unique.

All the SpEL expressions are evaluated by `Evaluator.java`.

This class can also accept a context.

<br/>

All the annotated classes are conveniently loaded using the `Loader.java` as is done automatically in the `Parser.java`.

Note that all codecs MUST BE loaded before the protocolMessages that use them, as they are used to verifying the annotations. 

If you want to provide your own classes you can use the appropriate constructor of `Parser`.

<br/>

The `Parser` is also used to encode a message.

<br/>

`BitBuffer.java` has the task to read the bits, whereas `BitWriter.java` has the task to write the bits.


<br/>

<a name="examples"></a>
## Examples

<a name="example-multi"></a>
### Multi-message parser

All you have to care about, for a simple example on multi-message automatically-loaded protocolMessages, is the `Parser`.
```java
//optionally create a context ('null' otherwise)
Map<String, Object> context = ...
//read all the codecs and annotated classes from where the parser resides and all of its children packages
Parser parser = Parser.create()
   .withContext(context);
//... or pass the parent package (see all the constructors of Parser for more)
Parser parser = Parser.create()
   .withContext(context)
   .withContextFunction(VersionHelper.class.getDeclaredMethod("compareVersion", new Class[]{String.class, String.class}))
   .withDefaultCodecs()
   .withProtocolMessages("base.package.messages");

//parse the message
byte[] payload = ...
ParseResponse result = parser.parse(payload);

//process the errors
for(int index = 0; index < result.getErrorCount(); index ++)
   LOGGER.error("An error occurred while parsing:\r\n   {}", result.getMessageForError(index));

//process the successfully parsed messages
for(int index = 0; index < result.getParsedMessageCount(); index ++){
    Object parsedMessage = result.getParsedMessageAt(index);
    ...
}
```

or, if you want to pass your protocolMessages by hand:
```java
//optionally create a context ('null' otherwise)
Map<String, Object> context = ...
ProtocolMessage<Message> protocolMessage = ProtocolMessage.createFrom(Message.class);
Parser parser = Parser.create()
   .withProtocolMessages(protocolMessage);

//parse the message
byte[] payload = ...
ParseResponse result = parser.parse(payload);
```

<a name="example-composer"></a>
### Message composer

The inverse of parsing is composing, and it's simply done as follows.
```java
//compose the message
Message data = ...;
ComposeResponse composeResult = parser.compose(data);

//process the read messages
if(!composeResult.hasErrors()){
    byte[] message = result.getComposedMessage();
    ...
}
//process the errors
else{
    List<ComposeException> errors = result.getErrors();
    ...
}
```


<br/>

<a name="changelog"></a>
## Changelog

<a name="changelog-1.0.0"></a>
### version 1.0.0 - 20200629
- first release
