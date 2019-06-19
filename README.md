# xml-in

your friendly XML navigator

[![Clojars Project](http://clojars.org/tolitius/xml-in/latest-version.svg)](http://clojars.org/tolitius/xml-in)

- [What](#what)
- [Why](#why)
- [Property based navigation](#property-based-navigation)
  - [All matching vs. The first matching](#all-matching-vs-the-first-matching)
- [Functional navigation](#functional-navigation)
- [Creating sub documents](#creating-sub-documents)

## What

XML is this new hot markup language everyone is raving about. Attributes, namespaces, schemas, security, XSL.. what's there not to love.

`xml-in` is _not_ about parsing XML, but rather working with already parsed XML.

It takes heavily nested `{:tag .. :attrs .. :content [...]}` structures that Clojure XML parsers produce and helps to navigate these structures in a Clojure "`get-in` style" using internal and custom transducers.

* _[clojure/data.xml](https://github.com/clojure/data.xml) is an example of a good and lazy Clojure/ClojureScript XML parser_
* _[funcool/tubax](https://github.com/funcool/tubax) is another example of a ClojureScript XML parser_

## Why

XML navigation in Clojure is usually done with help of zippers. [clojure/data.zip](https://github.com/clojure/data.zip) is usially used, and a common navigation looks like this:

```clojure
(data.zip/xml1-> (clojure.zip/xml-zip parsed-xml)
                 :universe
                 :system
                 :delta-orionis
                 :δ-ori-aa1
                 :radius
                 data.zip/text)
```

There is a great article "[XML for fun and profit](http://blog.korny.info/2014/03/08/xml-for-fun-and-profit.html)" that shows how zippers
are used to navigate XML DOM trees.

But we can do better: faster, cleaner, composable and "no zippers".

How much faster? Let's see:

zippers:
```clojure
=> (time (dotimes [_ 250000] 
           (data.zip/xml1-> (clojure.zip/xml-zip parsed-xml)
                            :universe
                            :system
                            :delta-orionis
                            :δ-ori-aa1
                            :radius
                            data.zip/text)))
"Elapsed time: 13385.563442 msecs"
```

xml-in:
```clojure
=> (time (dotimes [_ 250000]
     (xml/find-first parsed-xml [:universe
                                 :system
                                 :delta-orionis
                                 :δ-ori-aa1
                                 :radius])))
"Elapsed time: 765.884111 msecs"
```

## Property based navigation

Here is an XML document all the examples in this documentation are based on:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<universe>
  <system>
    <solar>
      <planet age="4.543" inhabitable="true">Earth</planet>
      <planet age="4.503">Mars</planet>
    </solar>
    <delta-orionis>
      <constellation>Orion</constellation>
      <δ-ori-aa1>
        <mass>24</mass>
        <radius>16.5</radius>
        <luminosity>190000</luminosity>
        <surface-gravity>3.37</surface-gravity>
        <temperature>29500</temperature>
        <rotational-velocity>130</rotational-velocity>
      </δ-ori-aa1>
    </delta-orionis>
  </system>
</universe>
```

> _it lives in [dev-resources/universe.xml](dev-resources/universe.xml)_

Since `xml-in` works with a parsed XML (e.g. a DOM tree), let's parse it once and call it the "`universe`":

```clojure
=> (require '[clojure.data.xml :as dx])
=> (def universe (dx/parse-str (slurp "dev-resources/universe.xml")))
#'boot.user/universe
```

it gets parsed into a common nested `{:tag :attrs :content}` structure that looks like this:

```clojure
=> (pprint universe)
{:tag :universe,
 :attrs {},
 :content
 ("\n  "
  {:tag :system,
   :attrs {},
   :content
   ("\n    "
    {:tag :solar,
     :attrs {},
     :content
     ("\n      "
      {:tag :planet,
      ;; ...
      ;; ...
```

One way to access child nodes in this XML document is to use "a vector of nested properties".

For example, let's check out "those two" planets in a solar system.

Bringing `xml-in` in:

```clojure
=> (require '[xml-in.core :as xml])
```

and

```clojure
=> (xml/find-all universe [:universe :system :solar :planet])
("Earth" "Mars")
```

All the planets are returned. In case we need "a" planet we can match the first one and stop searching:

```clojure
=> (xml/find-first universe [:universe :system :solar :planet])
("Earth")
```

notice `find-all` vs. `find-first`

### All matching vs. The first matching

Even if there is only one element that matches a search criteria it is best _not_ to look for it using `find-all`
since there is a cost of _looking_ at **all** the child nodes that are on the same level as a matched element.

Let's look at the example. From the XML above, let's find a `radius` of `δ-ori-aa1` component of the `delta-orionis` star system:

```clojure
=> (xml/find-all universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])
("16.5")
=> (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])
("16.5")
```

Both `find-all` and `find-first` return the same exact value, but we know for a fact that the `δ-ori-aa1` component has only _one_ `radius`. Which means it is best found with `find-first` rather than `find-all`.

Let's see the performance difference:

```clojure
=> (time (dotimes [_ 250000]
           (xml/find-all universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])))
"Elapsed time: 1216.927309 msecs"
```

```clojure
=> (time (dotimes [_ 250000]
           (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])))
"Elapsed time: 792.958283 msecs"
```

Quite a difference. The secret is quite simple: `find-first` stops searching once it finds a matching element.
But it does improve performance, especially for a large number of XML documents.

> _NOTE: `find-first` returns a "`seq`", and not just a "single" value, so it can be composed as described in [Creating sub documents](#creating-sub-documents)_

## Functional navigation

Navigation using functions, or rather transducers, adds custom "predicate batteries" to the process.

A few internal batteries are included in `xml-in`:

```clojure
=> (require '[xml-in.core :as xml :refer [tag= some-tag= attr=]])
```

* `tag=` finds child nodes under _all_ matched tags

* `some-tag=` finds child nodes under _the first_ matching tag

* `attr=` finds child nodes under all tags with attribute's key and value

Let's find all inhabitable planets of the solar system to the best of our knowledge (i.e. based on the XML above):

```clojure
=> (xml/find-in universe [(tag= :universe)
                          (tag= :system)
                          (some-tag= :solar)
                          (attr= :inhabitable "true")])
("Earth")
```

a `find-in` function takes a parsed XML and a sequence of transducers and computes a sequence from the application of all the transducers composed

Since `find-in` does not need to create transducers like `find-all` and `find-first` it is a bit more performant:

```clojure
=> (time (dotimes [_ 250000] (xml/find-first universe [:universe :system :solar :planet])))
"Elapsed time: 507.325005 msecs"
```
vs.
```clojure
=> (time (dotimes [_ 250000] (xml/find-in universe [(some-tag= :universe)
                                                    (some-tag= :system)
                                                    (some-tag= :solar)
                                                    (some-tag= :planet)])))
"Elapsed time: 467.535705 msecs"
```

## Creating sub documents

Let's say we need to get several properties out of the `δ-ori-aa1` component. We can do it as:

```clojure
=> (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :mass])
("24")
=> (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])
("16.5")
=> (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :surface-gravity])
("3.37")
```

we can of course group `[:mass :radius :surface-gravity]` together and map over them to call `xml/find-first universe` with a prefix, but it would not change the fact that we would need to "get-into" "`:universe :system :delta-orionis :δ-ori-aa1`" on every property lookup.

We can do better: navigate to `:universe :system :delta-orionis :δ-ori-aa1` _once_ and treat is as a document instead:

```clojure
=> (def aa1 (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1]))
#'boot.user/aa1
```
```clojure
=> (xml/find-first aa1 [:mass])
("24")
=> (xml/find-first aa1 [:radius])
("16.5")
=> (xml/find-first aa1 [:surface-gravity])
("3.37")
```

to create a sub document no special syntax is needed, just search "upto" the new root element.

and in cases where it is applicable, using a sub document is a bit faster:

```clojure
=> (time (dotimes [_ 100000]
           [(xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :mass])
            (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :radius])
            (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1 :surface-gravity])]))

"Elapsed time: 973.376399 msecs"
```

vs.

```clojure
=> (time (dotimes [_ 100000]
           (let [aa1 (xml/find-first universe [:universe :system :delta-orionis :δ-ori-aa1])]
             [(xml/find-first aa1 [:mass])
              (xml/find-first aa1 [:radius])
              (xml/find-first aa1 [:surface-gravity])])))

"Elapsed time: 760.332762 msecs"
```

## License

Copyright © 2019 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
