# Bitser
## Compact binary serialization

Bitser is a custom binary serialization library for Java. It needs all serializable fields to be annotated with
(detailed) information that bitser will use to serialize them using the minimum number of bits.

### Motivation
To illustrate how compact it can be, consider the following example class (it's a very simplified class from one of my
game projects):
```java
class Monster {
	int maxHealth;
	int maxMana;
	boolean ignoresFireDamage;
	int strength;
	int agility;
	int hitChance;
}
```
Using very naive JSON, it could be serialized like this:
`{"maxHealth":100,"maxMana":40,"ignoresFireDamage":false,"strength":15,"agility":5,"hitChance":90}`
which would take almost 100 bytes. (With pretty-printed JSON, it would be even worse.)

Using a naive `DataOutputStream`, it could be serialized using 4 bytes per `int` and 1 byte per `boolean`,
so 21 bytes, which is progress, but still suboptimal. First of all, by using 1 byte for the boolean, the remaining 7
bits of that byte are wasted. Furthermore, each `int` takes 4 bytes, which would be optimal **if the fields were
uniformly distributed between `Integer.MIN_VALUE` and `Integer.MAX_VALUE`**. But... they usually are not...

In most of (my) cases, integers are very often much closer to 0 than to `Integer.MIN_VALUE` or `Integer.MAX_VALUE`.
Especially the values 0 and 1 are extremely common. This could be 'solved' by using `byte`s instead of `int`s,
but this would constraint the minimum/maximum possible values, which is often undesirable. Instead, variable-length
integers can be used to exploit the high frequency of smaller integers.

When this example monster is serialized by bitser, it occupies 57 bits (7 or 8 bytes), which is considerably less than
the naive approaches. By annotating the fields in more detail, this can be reduced further to 46 bits (6 bytes).
Next to compact serialization, bitser also provides [references](./docs/references.md) and optionally
[backward compatiblity](./docs/backward-compatibility.md), as well as some smaller features and tools.

### Basic usage
To use bitser, all serializable classes must be annotated with bitser annotations. The example class above
could be annotated (naively) like this:
```java
@BitStruct(backwardCompatible = false)
class Monster {

	@IntegerField(expectUniform = false)
	int maxHealth;

	@IntegerField(expectUniform = false)
	int maxMana;

	@BitField
	boolean ignoresFireDamage;

	@IntegerField(expectUniform = false)
	int strength;

	@IntegerField(expectUniform = false)
	int agility;

	@IntegerField(expectUniform = false)
	int hitChance;
}
```
Or, you can save bits by specifying some more detailed annotations:
```java
@BitStruct(backwardCompatible = false)
class Monster {

	@IntegerField(expectUniform = false, minValue = 1)
	int maxHealth;

	@IntegerField(expectUniform = false, minValue = 1)
	int maxMana;

	@BitField
	boolean ignoresFireDamage;

	@IntegerField(expectUniform = false, minValue = 0)
	int strength;

	@IntegerField(expectUniform = false, minValue = 0)
	int agility;

	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	int hitChance;
}
```
Instances of this example class could be serialized to a `byte[]` by using
```java
byte[] bytes = new Bitser().toBytes(monsterInstance);
```
and deserialized using
```java
Monster deserialized = new Bitser().fromBytes(Monster.class, bytes);
```
The [IO guide](./docs/io.md) gives more information about the other options for IO (e.g. for using `InputStream`s and
`OutputStream`s). The [basics guide](./docs/basics.md) elaborates on the basic usage and the most important annotations.

### Adding bitser as dependency
This project requires **Java 17** or later.

#### Gradle
```
...
repositories {
  ...
  maven { url 'https://jitpack.io' }
}
...
dependencies {
  ...
  implementation 'com.github.knokko:bitser:v0.3.3'
}
```

#### Maven
```
...
<repositories>
  ...
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
...
<dependency>
  <groupId>com.github.knokko</groupId>
  <artifactId>bitser</artifactId>
  <version>v0.3.3</version>
</dependency>
```
