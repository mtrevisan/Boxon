# Boxon _[boˈzoŋ]_
Like [Preon](https://github.com/preon/preon), but the code is understandable...

This is a declarative, bit-level, message parser. All you have to do is write a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) that represents your message and annotate it. That's all. Boxon will taks care of the rest for you.


## Table of Contents
1. [Base annotations](#annotation-base)
    1. [BindArray](#annotation-bindarray)
    2. [BindArrayPrimitive](#annotation-bindarrayprimitive)
    3. [BindBit](#annotation-bindbit)
    4. [BindByte](#annotation-bindbyte)
    5. [BindShort](#annotation-bindshort)
    6. [BindInteger](#annotation-bindinteger)
    7. [BindLong](#annotation-bindlong)
    8. [BindFloat](#annotation-bindfloat)
    9. [BindDouble](#annotation-binddouble)
    10. [BindNumber](#annotation-bindnumber)
    11. [BindString](#annotation-bindstring)
    12. [BindStringTerminated](#annotation-bindstringterminated)
2. [Special annotations](#annotation-special)
    1. [MessageHeader](#annotation-messageheader)
    2. [BindIf](#annotation-bindif)
    3. [Skip](#annotation-skip)
    4. [Checksum](#annotation-checksum)
    4. [Assign](#annotation-assign)
3. [How to extend the functionalities](#how-to)
7. [Changelog](#changelog)
    6. [version 1.0.0](#changelog-1.0.0)

<br/>

<a name="annotation-base"></a>
## Base annotations
Here are described the build-in base annotations.

You can use them as a starting point to build your own customized readers.

<a name="annotation-bindarray"></a>
### BindArray

#### parameters
 - `type`: the Class of the Object of the single element of the array.
 - `size`: the size of the array (can be a SpEL expression).
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads an array of Objects.

#### annotation type
This annotation is bounded to a variable.

#### example
```
private class Version{
    @BindByte
    public byte major;
    @BindByte
    public byte minor;
    public byte build;
}

@BindArray(size = "2", type = Version.class)
private Version[] versions;
```


<a name="annotation-bindarrayprimitive"></a>
### BindArrayPrimitive

#### parameters
 - `type`: the Class of primitive of the single element of the array (SHOULD BE WRITTEN in array style, as `byte[].class`, or `int[].class`).
 - `size`: the size of the array (can be a SpEL expression).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads an array of primitives.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindArrayPrimitive(size = "2", type = byte[].class)
private byte[] array;
```


<a name="annotation-bindbit"></a>
### BindBit

#### parameters
 - `size`: the number of bits to read (can be a SpEL expression).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a `BitSet`.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindBit(size = "2")
private BitSet bits;
```


<a name="annotation-bindbyte"></a>
### BindByte

#### parameters
 - `unsigned`: whether the read value is treated as signed or unsigned (defaults to `false`).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a byte (or Byte).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindByte
public Byte mask;
```


<a name="annotation-bindshort"></a>
### BindShort

#### parameters
 - `unsigned`: whether the read value is treated as signed or unsigned (defaults to `false`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a short (or Short).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindShort
private short numberShort;
```


<a name="annotation-bindinteger"></a>
### BindInteger

#### parameters
 - `unsigned`: whether the read value is treated as signed or unsigned (defaults to `false`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads an int (or Integer).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindInteger
private int numberInt;
```


<a name="annotation-bindlong"></a>
### BindLong

#### parameters
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a long (or Long).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindLong
private long numberLong;
```


<a name="annotation-bindfloat"></a>
### BindFloat

#### parameters
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a float (or Float).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindFloat
private float numberFloat;
```


<a name="annotation-binddouble"></a>
### BindDouble

#### parameters
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a double (or Double).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindDouble
private double numberDouble;
```


<a name="annotation-bindnumber"></a>
### BindNumber

#### parameters
 - `type`: the Class of variable the be read (SHOULD BE `Float.class`, or `Double.class`).
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN`.
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a float or decimal (or Float or Double), depending on `type`, as a BigDecimal.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindNumber(type = Double.class)
private BigDecimal number;
```


<a name="annotation-bindstring"></a>
### BindString

#### parameters
 - `charset`: the charset to interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).
 - `size`: the size of the string (can be a SpEL expression).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a String.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindString(size = "4")
public String text;
```


<a name="annotation-bindstringterminated"></a>
### BindStringTerminated

#### parameters
 - `charset`: the charset to interpreted the string into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).
 - `terminator`: the byte that terminates the string (defaults to `\0`).
 - `consumeTerminator`: whether to consume the terminator (defaults to `true`).
 - `match`: a string/regex/SpEl expression that is used as an expected value.
 - `validator`: the Class of a validator (applied BEFORE the transformer).
 - `transformer`: the transformer used to convert the read value into the value that is assigned to the annotated variable. 

#### description
Reads a String.

#### annotation type
This annotation is bounded to a variable.

#### example
```
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
 - `charset`: the charset to interpreted the `start` and `end` strings into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).

#### description
Marks a POJO as an annotated message.

#### annotation type
This annotation is bounded to a class.

#### example
```
@MessageHeader(start = "+", end = "-")
private class Message{
    ...
}
```


<a name="annotation-bindif"></a>
### BindIf

#### parameters
 - `value`: the condition used to know if a variable has to be read (can be a SpEL expression).

#### description
Compute if a variable is to be read or skipped.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindByte(transformer = Mask.MaskTransformer.class)
public Mask mask;

@BindIf("mask.hasProtocolVersion()")
@BindArrayPrimitive(size = "2", type = byte[].class)
private byte[] protocolVersion;
```


<a name="annotation-skip"></a>
### Skip

#### parameters
 - `size`: the number of bits to be skipped (can be a SpEL expression).
 - `terminator`: the byte that terminates the skip (defaults to `\0`).
 - `consumeTerminator`: whether to consume the terminator (defaults to `true`).

#### description
Skips `size` bits, or until a terminator is found.

If this should be placed at the end of the message, then a placeholder variable (that WILL NOT be read, and thus can be of any type) should be added.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@Skip(size = "3")
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
 - `type`: the Class of variable the be read.
 - `byteOrder`: the byte order, `ByteOrder.BIG_ENDIAN` or `ByteOrder.LITTLE_ENDIAN` (used for primitives other than `byte`).
 - `skipStart`: how many byte are to be skipped from the start of the message for the calculation of the checsum (defaults to 0).
 - `skipEnd`: how many byte are to be skipped from the end of the message for the calculation of the checsum (default to 0).
 - `algorithm`: the algorithm to be applied to calculate the checksum.

#### description
Reads a checksum.

Compute the message checksum and compare it to the read variable once a message has been completely read.

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindChecksum(type = short.class, skipStart = 4, skipEnd = 4, algorithm = CRC16.class)
private short checksum;
```


<a name="annotation-assign"></a>
### Assign

#### parameters
 - `value`: the value to be assigned, or calculated (can be a SpEL expression).

#### description
Assign a constant value to a field (not present in the message).

#### annotation type
This annotation is bounded to a variable.

#### example
```
@BindString(size = "4")
private String messageHeader;

@Assign("T(java.time.ZonedDateTime).now()")
private ZonedDateTime receptionTime;

@Assign("messageHeader.startsWith('+B')")
private boolean buffered;

//from the variable `deviceTypes` passed in the context
@Assign("#deviceTypes.getDeviceTypeName(deviceTypeCode)")
private String deviceTypeName;
```

<br/>

<a name="how-to"></a>
## How to extend the functionalities
Boxon can handle on its own array of primitives, bit, byte, short, int, long, float, double, and their object counterpart, as long as BigDecimal, string (with a given size, or a terminator), and the special "[checksum](#annotation-checksum)".

You can extend the basic functionalities through the application of transformers.

<br/>

<a name="changelog"></a>
## Changelog

<a name="changelog-1.0.0"></a>
### version 1.0.0 - 20200617
- first release
