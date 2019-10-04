# Storesthal
A simple solution for JSON-HAL object retrieval and caching.

## Table of contents
<!-- toc -->- __[What is it?](#what-is-it)__
- __[Features](#features)__
- __[Example](#example)__
  - __[Object structure](#object-structure)__
  - __[Parent object](#parent-object)__
  - __[First child object](#first-child-object)__
  - __[Second child object](#second-child-object)__
  - __[First (and only) subchild object](#first-and-only-subchild-object)__
  - __[Here comes Storesthal](#here-comes-storesthal)__
  - __[How does that work?](#how-does-that-work)__
- __[Usage](#usage)__
  - __[Basic invocation](#basic-invocation)__
  - __[Relations](#relations)__
  - __[Caching](#caching)__
- __[What's that name about... :thinking:?](#whats-that-name-about-thinking)__
- __[TODOs and future of the project](#todos-and-future-of-the-project)__
  - __[Possible future plans](#possible-future-plans)__
- __[Third-Party software](#third-party-software)__
- __[Disclaimer](#disclaimer)__
- __[License](#license)__
<!-- /toc -->

## What is it?
It is a helper software (library) to make retrieving "JSON+HAL" objects (possibly from REST web services) easy. Additionally, it will _optionally_ cache the objects retrieved, so there won't be subsequent HTTP calls for fetching one and the some object.

Another speciality of storestahl is, that is will automatically follow [HAL](http://stateless.co/hal_specification.html "HAL specification")-relations and retrieve not only singular objects, but complete object structures, if they are linked. The caching described above will also take place for these linked objects.

## Features

- Lightweight architecture, only one entry point
- Fully automatic cache handling - one URL, one object
- Extensive test set
- Customizable relation and caching settings
- Comprehensive documentation


## Example

Here is an example of what Storesthal can do for you, regarding an exemplary object structure.

### Object structure
Consider the following object structure:

```
         Testparent
          /      \
  Testchild 1    Testchild 2
       |
Test-Subchild 1
```

This structure matches with the following JSON+HAL data retrieved from a web service, e. g. at `https://mygreatwebservice.com/api/`:

### Parent object
(Accessible at `https://mygreatwebservice.com/api/parents/3`)

```json 
{
  "_links": {
    "self": {"href": "https://mygreatwebservice.com/api/parents/3"},
    "children": [{
        "href": "https://mygreatwebservice.com/api/children/5"
    },{
        "href": "https://mygreatwebservice.com/api/children/14"    
    }]
  },
  "number": 3,
  "comment": "Test",
  "name": "Testparent"
}
```

### First child object

(Accessible at `https://mygreatwebservice.com/api/children/5`)

```json 
{
  "_links": {
    "self": {"href": "https://mygreatwebservice.com/api/children/5"},
    "parent": {"href": "https://mygreatwebservice.com/api/parents/3" },
    "children": [{
        "href": "https://mygreatwebservice.com/api/subchildren/99"
    }]
  },
  "number": 5,
  "comment": "Test",
  "name": "Testchild 1"
}
```


### Second child object

(Accessible at `https://mygreatwebservice.com/api/children/14`)

```json 
{
  "_links": {
    "self": {"href": "https://mygreatwebservice.com/api/children/14"},
    "parent": {"href": "https://mygreatwebservice.com/api/parents/3" },
    "children": []
  },
  "number": 5,
  "comment": "Test",
  "name": "Testchild 2"
}
```

### First (and only) subchild object

(Accessible at `https://mygreatwebservice.com/api/subchildren/99`)

```json 
{
  "_links": {
    "self": {"href": "https://mygreatwebservice.com/api/subchildren/99"},
    "parent": {"href": "https://mygreatwebservice.com/api/children/5" },
    "children": []
  },
  "number": 9,
  "comment": "Test",
  "name": "Test-Subchild 1"
}
```

(All of the objects, of course, having their individual attributes as well.)

### Here comes Storesthal
If you use Storesthal to retrieve just the parent URL (`https://mygreatwebservice.com/api/parents/3`), this is what will happen:

- The parent object is loaded from the parent URL and populated with its attributes
- Child object relations are _automatically_ followed, retrieved and "attached" to the parent object
- Back-References (child &rarr; parent) are also handled correctly. This means, there will be only _one_ instance of the particular parent object, linked to every child of it.
- Only one HTTP call will be made for the parent object (not one for every reference to it), it will from then be used from cache.
- The structure as depicted above will be returned by Storesthal (the parent object with its children and subschildren linked)

### How does that work?
OK, it's not pure magic :wink:. The Java classes for the objects must be existing, so there must be a class for the parent object and (at least) one for the child object. As the sub-child (in out example) has a structure equal to the one of the child, a specific sub-child object is not necessary at all.

The classes _should_ have matching `@HALRelation` annotations to let Storesthal know, which relation refers to which attribute. If these annotations are missing (working in "annotationless mode"), Storesthal will try its best to find these things out on its own.

Also, the classes _should_ have senseful `@Cacheable` annotations, so Storesthal will put the objects retrieved "into the right bucket". (Otherwise, a "general cache" for all objects is used or you can work without any caching).

You will find some examples below.

## Usage

When designing Storesthal, special emphasis was placed on ease of usage and convention over configuration. Especially when using "annotationless mode" (see below) and the default configuration, it will "just work" for a lot of common use cases. Nevertheless, it's possible (and advised) to customize and fine-tune Storesthal's settings.

Explaining the usage of Storesthal, we will mainly stick to the example object structure given [above](#example).

### Basic invocation
In order to retrieve an object via Storesthal, you basically just need one call:

```java
ParentObject Testparent = Storesthal.getObject("https://mygreatwebservice.com/api/parents/3", ParentObject.class);
```

Storesthal will then retrieve and examine the object found at the given URL and traverse the object structure as it discovers it and add matching sub-objects to every level of object relations.
Especially, Storesthal is able to handle back-references and references to (yet) unknown or "incomplete" objects correctly!
Additionally, Storesthal will use an object cache by default, making subsequent calls to the same URL performant.

### Relations
As stated above, Storesthal will automatically find and "attach" related objects to the one retrieved. For this to work, every object class needs to have
_either_ a matching setter method (e. g., if the relation is called `customer` in JSON, there must be a `setCustomer`) _or_ an arbitrary method (or attribute)
annotated with `@HALRelation(name=...)`. So, if your setter is called `setCustomer`, but the JSON relation is named `cstmr`, you would need to use
`@HALRelation(name="cstmr")` and could annotate any method with it, as long as it takes only one argument of the correct type.

The same scheme applies to any type of relation: 1 to 1, 1 to many, backreferences, ...

For convenience, it is also possible to annotate a class field with `@HALRelation`. If doing so, Storesthal will again expect a setter method with the correspondent name to be
present in your class. So, if the annotated field is named `studends`, there must be a method called `setStudents`, accepting any java `Collection` type as first and only parameter.
If that collection type is an abstract one, Storesthal will try to use an appropriate implementation, otherwise it will try and instantiate a new collection of the given type.
For details, see method `handleCollection` in `Storesthal.java`.

Please note, that - at least for the moment - Storesthal is not able to handle arrays instead of `Collection`s.

### Caching

One speciality about Storesthal is, that it brings along a simple, yet powerful, caching facility that comes out of the box.
This does especially make sense, as caching is often very helpful (/ performance increasing / resource saving) when it comes to JSON+HAL object retrieval.
Imagine you have 10.000 items each of which is linked to one of 20 categories. When loading the 10.000 items, it's not necessary to query one and the same category more than one single time and then
"link" it to every item associated. This will not only speed up your application and save network and hardware resources, it will also lead to a more convenient and comprehensible in-memory object structure.

By default, nevertheless caching is not applied to any object, as Storesthal can not know beforehand, if there are fast-changing objects in your object structure, that might change very
quickly and therefore shouldn't be cached. The only caching that takes place by default (and cannot be disabled) is the "intermediate caching" described below.

To add "rudimentary" caching to an object class, you would simply annotate it with `@Cacheable`. Instances of classes with this annotation will then be cached automatically.
Anyway, it is suggested to use the `cacheName` and `cacheSize` attributes of the annotation as well: If you omit the `cacheName`, the object will be stored in the "common" cache,
where any object of any kind will be stored, if no other cache name is given. Using a cache name, you have a better control about cache size and clearing of the cache. So, in the simple
example above, it should be best to annotate your item class with `@Cacheable(cacheName="ItemCache")` and your category class with `@Cacheable(cacheName="CategoryCache")`.
The `cacheSize` attribute allows you to specify, how many object instances the cache will hold. We're using a LRU (last recently used) cache here, so if the cache is full, the object
last recently _accessed_ will be evicted from it. This is not necessarily the object last recently added to the cache! If you use a cache name, you will also be able to clear the
whole Cache at once using the `Storesthal.clearCache` function and supplying that cache name. All the other caches won't be touched.

Caching happens based on the URL of the object retrieved. This means, if caching is enabled, only the first request for http://my.web.service/api/cagetory/42 will really cause
an HTTP call to that URL. Subsequent requests for the same URL will just retrieve the cached object from the memory as long it is not evicted from the cache or the cache is cleared.

#### The intermediate cache
There is one special cache, that can't be disabled: It's the _intermediate cache_. When traversing an object structure, it might occur that Storesthal finds one and the same related
URI (and therefore object) multiple times. In these cases, all references but the first one are fetched from the intermediate cache - everything else would result in an inconsistent
object structure as two references to the same URI would result in two different objects.

Please note, that the intermediate cache is only used within one single `getObject` call. It will be cleared before the next one starts. So, it will probably be in use for a few seconds
(or probably less) only.

## What's that name about... :thinking:?

I'm not a very creative person when it comes to such things... :wink: It's just a word composed of "Store", "REST" and "HAL". :simple_smile:

## TODOs and future of the project

For the moment, Storesthal is quite sufficient for my personal needs, so major changes or improvements are not planned with high priority. Anyway, I'll try and keep the project alive and well-maintained. Please feel free to use GitHubs possibilities to submit issues, feature requests or pull requests! :simple_smile:

If the project will have some kind of a bigger impact (I'm aware it will never be a real big thing ^^), it is more likely for me to continue working on it, as otherwise this would just be "for my private amusement".

### Possible future plans

 - I love Spring Boot and would like to integrate Storesthal better with it, perhaps even as kind of a plug-in. But I have not had the time to investigate on how to do this up to now.
 - See the "Issues" tab :simple_smile:

## Third-Party software

Storesthal makes heavy use of third-party software and libraries during the build process as well as during runtime. A detailed list of the third-party dependencies can be found
in [`build.gradle`](./build.gradle).

Different license terms may apply to this software packages and must be considered before usage. There is no relation between the author(s) of Storesthal and the people or companies
supplying third-party software. These packages are - gratefully! - used within Storesthal, but not maintained, merchandised, licensed or anything else by Storesthals author(s).

## Disclaimer
This program is free software. It comes without any warranty, not even for merchantability or fitness for a particular purpose.

Another disclaimer may apply to [third-party software included in Storesthal](#third-party_software), please see the respective license models.

Please see [the License section](#license) for more details.

## License
Storesthal is licensed und the terms of the GNU Lesser General Public License (LPGL). Please see [LICENSE.md](./LICENSE.md) for details.

Please consider the information in the ["Third-Party software"](#third-party-software) and ["Disclaimer"](#disclaimer) sections also.

