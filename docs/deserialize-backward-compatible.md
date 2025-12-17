# Deserialize with backward compatibility
TODO Fix this because it is outdated

This page summarizes how we can deserialize an object of interest in a
backward-compatible manner. For instance, we could have saved an object
of interest to disk a week ago, added new fields to its class, and
deserialize it anyway!

## Deserialize class hierarchy
Start by deserializing an instance of `LegacyClasses`, which holds
`LegacyStruct`s and `LegacyClass`es.
- Each `LegacyClass` holds the deserialized `BitField`s of a single Java
class that was used to serialize the object of interest.
- Each `LegacyStruct` represents a single `BitStruct`. It has 1 `LegacyClass`
for each Java class in the inheritance chain between `Object` and the
Java class that was annotated with the `BitStruct`.

## Deserialize the object of interest to a `LegacyStructInstance`
Using the `LegacyClasses`, we can deserialize the content from the
`BitInputStream`. Since the `LegacyClasses` doesn't know
the exact classes that it should use to deserialize the data,
and some classes may be altered since the object of interest was serialized,
it doesn't have enough information to deserialize the object of interest
completely.

To work around this limitation, it will instead deserialize each `BitStruct`
to a `LegacyStructInstance`, which uses `Object[]`s to hold most of the
deserialized data. This will eventually spit out a root `LegacyStructInstance`,
which will have children if and only if the serialized object of interest
had children.

Furthermore, references can't be resolved yet at this point, so all
stable references will be substituted with their corresponding `UUID`,
and all unstable references will be substituted with their corresponding
`Integer`.

Finally, since not all type information about arrays, collections,
and maps is known, we will temporarily:
- Use a `LegacyCollection` for any collection field.
- Use a `LegacyMap` for any map field.

This logic is implemented in the `LegacyClasses.read(...)` method.

## Fix the struct & collection types
Once the `LegacyStructInstance` has been deserialized, we will compare it with
the current class of the object of interest, and fix the types of the structs
and collections.
- All `LegacyStructInstance`s (used for struct substitution) will be replaced
by an instance of the corresponding class, but its fields won't be initialized
yet.
- All `LegacyCollection`s that should become an array, will be 
replaced by an array with the *right size*, but uninitialized elements.
- All `LegacyCollection`s that should become a collection, will be
replaced by an *empty* collection of the right type.
- All `LegacyMap`s will be replaced by an *empty* map of the right type.

During this step, we will also update the affected reference targets.
This logic is implemented in the `BitStructWrapper.fixLegacyTypes(...)`
method.

## Resolve the references
Once all struct & collection types are fixed, we can resolve all
references of the deserialized object, using the `ReferenceIdLoader.resolve()`
method. Note that all structs will have uninitialized fields, and that all
collections will be empty, which doesn't matter.

## Initialize fields of the structs & elements of the collections
As mentioned in the previous steps, the structs, collections, arrays, and
maps will be mostly uninitialized. Since all values and references
should be available now, it's time to populate everything. This logic is
implemented in the `setLegacyValues(...)` methods.

## Calling post-init
Once everything has been initialized, we should call the `postInit(...)`
methods of all objects that implement `BitPostInit`, which will happen
during the `postResolve()` method of `ReferenceIdLoader`.
