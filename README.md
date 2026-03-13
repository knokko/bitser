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