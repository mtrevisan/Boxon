# Boxon _[boˈzoŋ]_
Like [Preon](https://github.com/preon/preon), but the code is understandable, shorter, uses [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html), and the documentation is __really__ free...

This is a declarative, bit-level, message parser. All you have to do is write a [POJO](https://en.wikipedia.org/wiki/Plain_old_Java_object) that represents your message and annotate it. That's all. Boxon will taks care of the rest for you.

If you want to use the parser straight away, just go [there](#examples).

<br/>

#### Notable features
[Boxon](https://en.wikipedia.org/wiki/Boson)...
 - is easily extensible through the use of [tranformers](#how-to).
 - contains a minimal set of [annotations](#annotation-base) capable of handling all the primitive data.
 - contains a set of [special annotations](#annotation-special) that handles the various messages peculiarities (conditional bindings, skip bits/bytes, checksum, 'constant' assignments)
 - is capable of handle concatenation of messages, using the correct codec under the hood.
 - can handle [SpEL expressions](https://docs.spring.io/spring/docs/4.3.10.RELEASE/spring-framework-reference/html/expressions.html) on certain fields.
 - can do decode and encode data on the fly with a single annotated class (thus avoiding separate decoder and encoder going out-of-sync).


<br/>

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
4. [Digging into the code](#digging)
5. [Examples](#examples)
    1. [Multi-message parser](#example-multi)
6. [Changelog](#changelog)
    1. [version 1.0.0](#changelog-1.0.0)

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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
```java
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
 - `charset`: the charset to interpreted the `start` and `end` strings into (SHOULD BE the charset name, eg. `UTF-8` (the default), `ISO-8859-1`, etc).

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


<a name="annotation-bindif"></a>
### BindIf

#### parameters
 - `value`: the condition used to know if a variable has to be read (can be a SpEL expression).

#### description
Compute if a variable is to be read or skipped.

#### annotation type
This annotation is bounded to a variable.

#### example
```java
@BindByte(transformer = Mask.MaskTransformer.class)
public Mask mask;

@BindIf("mask.hasProtocolVersion()")
@BindArrayPrimitive(size = "2", type = byte[].class)
private byte[] protocolVersion;

@BindIf("protocolVersion[0] > 1")
@BindShort
private short conditionedValue;
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
```java
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
```java
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
```java
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

You can extend the basic functionalities through the application of transformers as shown below in some examples.

### DateTime transformer (from Unix timestamp to ZonedDateTime)
```java
@BindLong(transformer = DateTimeUnixTransformer.class)
private ZonedDateTime eventTime;

public class DateTimeUnixTransformer implements Transformer<Long, ZonedDateTime>{
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

### DateTime transformer (from YYYYMMDDHHMMSS as bytes to ZonedDateTime)
```java
@BindArrayPrimitive(size = "7", type = byte[].class, transformer = DateTimeYYYYMMDDHHMMSSTransformer.class)
private ZonedDateTime eventTime;

public class DateTimeYYYYMMDDHHMMSSTransformer implements Transformer<byte[], ZonedDateTime>{
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

### IMEI transformer (from 'nibble' array to String)
```java
@BindArrayPrimitive(size = "8", type = byte[].class, transformer = IMEITransformer.class, validator = IMEIValidator.class)
private String imei;

public class IMEITransformer implements Transformer<byte[], String>{
    @Override
    public String decode(final byte[] value){
        final StringBuffer sb = new StringBuffer();
        for(int i = 0; i < 7; i ++)
            sb.append(String.format("%02d", value[i] & 255));
        sb.append(ByteHelper.applyMaskAndShift(value[7], (byte)0x0F));
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

### RSSI transformer (from encoded byte to short value)
```java
@BindByte(transformer = RSSITransformer.class)
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
public class RSSITransformer implements Transformer<Byte, Short>{

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

<br/>

<a name="digging"></a>
## Digging into the code
Almost for each base annotation there is a corresponding class defined into `Coder.java` that manages the encoding and decoding of the underlying data.

The other annotations are managed directly into `MessageParser.java`, that is the main class that orchestrates the parsing of a single message with all of its annotations.
If an error occurs a `ParseException` is thrown.

Messages can be concatenated, and the `Parser.java` class manages them, returning a [DTO](https://en.wikipedia.org/wiki/Data_transfer_object), `ParseResponse.java`, with a list of all successfully read messages and a list of all errors from problematic messages.

<br/>

Each annotated class is processed by `Codec.class`, that is later retrieved by `Parser.java` depending on the starting header.
For that reason each starting header defined into `MessageHeader` annotation SHOULD BE unique.

All the SpEL expressions are evaluated by `Evaluator.java`.

This class can also accept a context.

<br/>

All the annotated classes are conveniently loaded using the `Loader.java` as is done automatically in the `Parser.java`.

If you want to provide your own classes you can use the appropriate constructor of `Parser`.

<br/>

The `MessageParser` is also used to encode a message (_NOTE: this feature will be moved in the future!_).

<br/>

`BitBuffer.java` has the task to read the bits, whereas `BitWriter.java` has the task to write the bits.


<br/>

<a name="examples"></a>
## Examples

<a name="example-multi"></a>
### Multi-message parser

All you have to care about, for a simple example on multi-message automatically-loaded codecs, is the `Parser`.
```java
//optionally create a context ('null' otherwise)
Map<String, Object> context = ...
//read all the annotated classes from where the parser resides and all of its children packages
Parser parser = new Parser(context);
//... or pass the parent package (see all the constructors of Parser for more)
//Parser parser = new Parser(context, "base.package.messages");

//parse the message
byte[] payload = ...
ParseResponse result = parser.parse(payload);

//process the read messages
if(!result.hasErrors()){
    List<Object> messages = result.getParsedMessages();
    ...
}
//process the errors
else{
    List<ParseException> errors = result.getErrors();
    ...
}
```

or, if you want to pass your codecs by hand:
```java
//optionally create a context ('null' otherwise)
Map<String, Object> context = ...
Codec<ACKMessage> codec = Codec.createFrom(Message.class);
List<Codec<?>> codecs = Collections.singletonList(codec);
Parser parser = new Parser(context, codecs);

//parse the message
byte[] payload = ...
ParseResponse result = parser.parse(payload);
```


<br/>

<a name="changelog"></a>
## Changelog

<a name="changelog-1.0.0"></a>
### version 1.0.0 - 20200617
- first release
