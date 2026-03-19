# Bitser IO
There are a couple of ways to (de)serialize objects using bitser. This guide will demonstrate the possibilities.
All the possibilities need an instance of `Bitser`, which you can create by simply using
```java
var bitser = new Bitser();
```
You can create a new instance of `Bitser` every time, but reusing `Bitser` instances can have performance benefits if
you (de)serialize many instances of the same class(es).

## bitser.toBytes() and bitser.fromBytes()
The easiest way to serialize an object of class `C`, is by using
```java
byte[] bytes = bitser.toBytes(instanceOfClassC);
```
The easiest way to deserialize it, is by using
```java
C deserializedInstance = bitser.fromBytes(C.class, bytes);
```
You can use these bytes however you see fit (e.g. store them in a file, or send them over the network).

You can add additional serialization flags to both methods by using their varargs parameter, for instance
`bitser.toBytes(instanceOfClassC, Bitser.BACKWARD_COMPATIBLE)` and
`bitser.fromBytes(C.class, bytes, Bitser.BACKWARD_COMPATIBLE)`.

## bitser.serialize() and bitser.deserialize()
Alternatively, you can use `bitser.serialize(instanceOfClassC, bitOutputStream)` and
`bitser.deserialize(C.class, bitInputStream)`. 

### BitInputStream and BitOutputStream
`BitInputStream`s and `BitOutputStream`s are wrappers around `InputStream`s and `OutputStream`s that can read/write 8
`boolean`s (bits) for each `byte`. You can create instances by using `new BitInputStream(inputStream)` and
`new BitOutputStream(outputStream)`. This allows you to use bitser whenever you get an `InputStream` or `OutputStream`
from any source.

### Basic usage
The following example code demonstrates how `serialize()` and `deserialize()` can be used to write and read a file.
```java
class Example {
	void fileExample(Bitser bitser, Path someFilePath) {
		var output = new BitOutputStream(Files.newOutputStream(someFilePath));
		bitser.serialize(instanceOfClassC, output);
		output.finish();

		var input = new BitInputStream(Files.newInputStream(someFilePath));
		var deserializedC = bitser.deserialize(C.class, input);
		input.close();
	}
}
```
At this point, it looks rather verbose, but it offers a few possibilities that `toBytes()` and `fromBytes()` don't.
For instance, you may deserialize objects that are too big for a `byte[]` (although deserializing such massive
objects will take a while...)

### Using bit streams more than once
Another advantage is that you can reuse the same bit stream, without wasting up to 7 bits after each object.
For instance, if serializing `instancwOfClassC` takes 20 bits, the following code would spend exactly 40 bits = 8
bytes:
```java
class Example {
	void doubleExample(Bitser bitser, Path someFilePath) {
		var output = new BitOutputStream(Files.newOutputStream(someFilePath));
		bitser.serialize(instanceOfClassC, output);
		bitser.serialize(anotherInstanceOfClassC, output);
		output.finish();
	}
}
```
If you would instead use `toBytes()` twice, it would take 2 times 3 bytes since the last 4 bits are wasted each time.

### Using subclasses of BitInputStream and BitOutputStream
You can also use subclasses of `BitInputStream` and `BitOutputStream`. For instance, you could pass a `BitCountStream`
to `bitser.serialize`, which will simply count the number of saved bits, without storing them anywhere.

### Additional serialization flags
Just like with `toBytes()` and `fromBytes()`, you can use the varargs parameter to specify additional flags/options for
the (de)serialization, for instance:

```java
class Example {
	void backwardCompatibleExample(Bitser bitser, Path someFilePath) {
		var output = new BitOutputStream(Files.newOutputStream(someFilePath));
		bitser.serialize(instanceOfClassC, output, Bitser.BACKWARD_COMPATIBLE);
		output.finish();

		var input = new BitInputStream(Files.newInputStream(someFilePath));
		var deserializedC = bitser.deserialize(C.class, input, Bitser.BACKWARD_COMPATIBLE);
		input.close();
	}
}
```
