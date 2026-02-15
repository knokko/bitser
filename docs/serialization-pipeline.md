# (De)serialization pipeline
The (de)serialization pipeline is used to serialize and deserialize `BitStruct`s. It is a *pipeline* where the order of
the stages is important.

## Stage 0: (de)serialize the relevant classes
**Stage 0 is only used in backward-compatible (de)serialization.**

During this stage, all relevant *classes* with their fields and annotations are (de)serialized. This is needed during
deserialization, because bitser needs to know the structure in which the actual content is encoded.

Note that this is per-class data, so it takes a relatively large amount of storage when you have only 1 instance of
each class, but it becomes negligible when you have thousands of instances per class (on average).

### Example
Let's consider this simple class as example.
```java
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.BitField;
import com.github.knokko.bitser.field.IntegerField;

@BitStruct(backwardCompatible = true)
class ExampleClass {

	@BitField(id = 0)
	@IntegerField(expectUniform = true, minValue = 0, maxValue = 100)
	int exampleField;
}
```
During this stage, bitser will store, among others, the following data:
- this class has 1 bit field and 0 functions
- the field is an integer field
- `expectUniform = true`, `minValue = 0`, and `maxValue = 100`

Bitser will *not* store the name of the class or field. In general, bitser will save the absolute minimum amount of
information needed. In this example, the `expectUniform`, `minValue`, and `maxValue` are required, since they dictate
(among others) how many bits are used to encode the `exampleField` value of each `ExampleClass` instance.

All this data is (de)serialized using the non-backward-compatible pipeline. Ideally, this would also be saved using
the backward-compatible pipeline, but that would cause a lot of problems...

## Stage 1: the primary (de)serialization
During this stage, the bulk of the data is (de)serialized. All structs, arrays, and collections are (de)serialized,
except the references (which are treated in a later stage).

### The iterative stack-based algorithm
Bitser will maintain a stack of structs and a stack of arrays/collections that need to be (de)serialized. The struct
stack initially only contains the root struct to be (de)serialized, and the arrays/collections stack is initially
empty.

This stage can take many iterations. During each iteration, the struct and the array at the top of the stacks are
removed and (de)serialized. Their children (if any) are added to the top of the stacks. This stage will go through
the root struct and all of its descendants, but without using any real recursion, to avoid potential
`StackOverflowError`s.

### Backward-compatible deserialization
During simple/non-backward-compatible deserialization, bitser will construct all class instances during this stage,
and immediately populate all their non-reference fields. During backward-compatible deserialization, this is not an
option, since the old class that was serialized may or may not be equivalent to the current class that is being
deserialized. Instead, bitser will create a `LegacyStructInstance` for each deserialized struct, which will be
converted to an instance of the right class during a later stage.

### References
Whenever a reference **target** is encountered in this stage, it is registered/remembered, since we will need it in a
later stage.

Whenever a **reference** is encountered, a job to *resolve* the reference (find its corresponding target) is
scheduled for a later stage. This is needed to ensure that all reference **targets** are registered before any
reference is resolved.

## Stage 2: scanning the 'with' objects
During this stage, all the 'with' objects passed to the `withAndOptions` parameter of `Bitser.serialize` and
`Bitser.deserialize`, will be scanned for **reference targets**.

Just like in stage 1, this scan will use a stack-based algorithm to traverse all the 'with' objects, which avoids
potential `StackOverflowError`s. All encountered reference targets will be registered, just like the reference targets
encountered during stage 1. Everything else will be ignored.

## Stage 3: mapping the stable reference targets
All the reference targets should have been collected during the previous stages, and they will be needed in the next
stage. This stage will speed up the next stage by putting all the *stable* reference targets in a `Map<id, target>`.
This allows stable reference targets to be looked up by their UUID, which avoids the need to frequently iterate over
all stable targets during the next stage.

Note that the insertion of the stable targets into the maps can *not* happen during stage 1, since we need to wait
until all IDs are deserialized.

## Stage 4: resolving the references
During this stage, all the **references** that were encountered during stage 1, will be *resolved*. Let's look at the
following example class:

```java
import com.github.knokko.bitser.BitStruct;
import com.github.knokko.bitser.field.IntegerField;
import com.github.knokko.bitser.field.ReferenceField;
import com.github.knokko.bitser.field.ReferenceFieldTarget;
import com.github.knokko.bitser.field.StableReferenceFieldId;

import java.util.UUID;

@BitStruct(backwardCompatible = false)
class ExampleClass {

	@BitStruct(backwardCompatible = false)
	static class ChildClass {

		@StableReferenceFieldId
		UUID id; // Will be initialized during stage 1
	}

	@IntegerField(expectUniform = false)
	int simpleField; // Will be initialized during stage 1

	@ReferenceField(stable = false, label = "unstable example")
	String unstableReference; // Will be initialized during this stage

	@ReferenceFieldTarget(label = "unstable example")
	String unstableReferenceTarget; // Will be initialized during stage 1

	@ReferenceField(stable = true, label = "stable example")
	ChildClass stableReference; // Will be initialized during this stage

	@ReferenceFieldTarget(label = "stable example")
	ChildClass stableTargetTarget; // Will be initialized during stage 1
}
```
The 'simple' fields, as well as the reference **targets** will be deserialized and initialized during stage **1**.
(These fields are `ChildClass.id`, `simpleField`, `unstableReferenceTarget`, and `stableReferenceTarget`.)

The **reference** fields on the other hand, will remain uninitialized until they are *resolved* in this stage.

### Unstable references
For each label `l`, all the targets with that label are put in an array or list `t` of size `n`. Every unstable
reference `u` with label `l` must be one of these `n` targets (have the same identity), and is serialized by storing
the *index* `i` such that `t[i] == u`, using `ceil(log2(t.size))` bits.

For instance, if there are 10 **targets** with label "x", 12345 **targets** with label "y", and 50 **references**
with label "x", each of the 50 references is serialized using `ceil(log2(10)) = 4` bits, so it takes 200 bits to
serialize all unstable references with label "x".

(If there were only 1 target with label "x", then it would take 0 bits to store all references with label "x",
since `log2(1) = 0`. This makes sense, since all references would have to point to the one and only target.)

### Stable references
Stable references are always serialized by storing the stable UUID of their target, using the full 128 bits.
During deserialization, the stable UUID of each reference will be deserialized, and the corresponding target will be
found by using the `Map<stableID, target>` generated in stage 3.

## Stage 5: converting `LegacyStructInstance`s and `LegacyArray`s
**Stage 5 is only used in backward-compatible (de)serialization.**

During backward-compatible deserialization, bitser doesn't know exactly which class it is deserializing (or whether
this class still exists), so it cannot create instances of the right class during stage 1. Instead, it creates
`LegacyStructInstance` and `LegacyArray`s, which will be *converted* during this stage.

During this stage, bitser *does* know to which class each `LegacyStructInstance` should be converted, and tries to do
so, as best as it can. Bitser will create an instance of this class using its default constructor. Then,
for each field with ID `x` of the class, bitser will try to set its value to
`legacyStructInstances.hierarcy.get(...).values[x]`. While doing this, bitser will also convert all *legacy* values to
their 'modern' values (e.g. convert `LegacyIntValue`s to `Long`s).

Similarly, all the legacy values in all the `LegacyArray`s will be converted to their modern counterparts.

During this stage, all the legacy values, **except** `LegacyReference`s are converted to their modern counterparts.
These legacy **references** will be converted in the next stage instead, because all legacy **targets** need to be
converted before the legacy **references** are converted. During this stage, an `IdentityHashMap` will be created and
filled to map all legacy reference targets to their modern reference target.

## Stage 6: converting `LegacyReference`s
**Stage 6 is only used in backward-compatible (de)serialization.**

During this stage, all the `LegacyReference`s will be replaced by their modern counterpart. This is skipped in the
previous stage, to ensure that all reference **targets** are modernized first.

Modernizing all these references is easy: we simply need to use the `IdentityHashMap` created during the previous stage.

## Stage 7: propagating references from bit methods to bit fields
This is a small stage that is only needed for fields with `readsMethodResult = true` whose corresponding bit method is
annotated with `@ReferenceField`. This stage simply copies the references that were deserialized for these
'reference methods', and puts them in the corresponding field.

All other fields with `readsMethodResult = true` are already handled in stage 5, but references are postponed to
this stage because stage 6 needs to happen first.

## Stage 8: populating the collections and maps
During all the previous stages, all deserialized collections are stored in *array*s rather than actual `Collection`s.
During this stage, these arrays will be converted to `Collection`s or `Map`s, if needed. For some collections/maps
(e.g. `HashSet`s and `HashMap`s), it is important that this stage happens *after* the primary deserialization, since
they would otherwise 'break' if the `hashCode()` of their keys change during deserialization.

Also, to minimize the number of `hashCode()` changes, this stage will populate the 'deepest' collections/maps first,
to correctly handle e.g. the case where the key of a `HashMap` is another collection.

Unfortunately, even this deepest-first approach does not always succeed (e.g. when the key of a `HashMap` is a
*reference* to a less-deep collection). To handle such cases, this stage needs multiple iterations. After each
iteration, this stage checks whether each key of each `Map` can still be found, and whether each element in each `Set`
can still be found. If not, this stage will perform another iteration. The maximum number of iterations should be bound
by the depth of the deepest collection or map.

## Stage 9: invoking the `BitPostInit`s
During this step, bitser will iterate over all bit structs that implement `BitPostInit`, and invoke their `postInit()`
method. The `postInit()` methods of the 'deepest' structs will be invoked first.

This stage happens *after* stage 8 to ensure that all collections and maps are intact during the `postInit()`
invocations. (At least, they are intact when the invocations *start*.)

## Stage 10: re-populating the collections and maps
It is possible that one or more `Map`s or `Set`s are 'damaged' during the previous stage, e.g. if a struct that
implements `BitPostInit` is used as key in a `HashMap`, and its `hashCode()` is changed during its `postInit()`
invocation.

To handle this problem, this stage does the same as stage 8: it checks whether each key or element can still be found
in each `Map` or `Set`, and re-populates them if not. This may also take multiple iterations, if needed.
