# xml-in

your friendly XML navigator

[![Clojars Project](http://clojars.org/tolitius/xml-in/latest-version.svg)](http://clojars.org/tolitius/xml-in)

## What

XML is this new hot format everyone is raving about. Attributes, namespaces, schemas, security, XSL.. what's there not to love.

`xml-in` is not about parsing XML, but rather working with already parsed XML.
It takes heavily nested `{:tag .. :attrs .. :content [...]}` structures that Clojure XML parsers produce and allows to navigate
this structures in a Clojure's "`get-in` style" using internal or custom transducers.

[clojure/data.xml](https://github.com/clojure/data.xml) is an example of a good and lazy Clojure/ClojureScript XML parser
[funcool/tubax](https://github.com/funcool/tubax) is another example of ClojureScript XML parser

## Why

The most common XML navigation is done via zippers. [clojure/data.zip](https://github.com/clojure/data.zip) is usially used
and a common navigation looks like this:

```clojure
(data.zip/xml1-> (clojure.zip/xml-zip parsed-xml)
                 :universe
                 :system
                 :delta-orionis
                 :δ-ori-aa1
                 :radius
                 data.zip/text)
```

There is a great article [XML for fun and profit](http://blog.korny.info/2014/03/08/xml-for-fun-and-profit.html) that shows how zippers
are used to navigate XML DOM trees.

But we can do better: faster, cleaner, composable and no zippers.

How much faster? Let's see:

zippers
```clojure
boot.user=> (time (dotimes [_ 250000] (data.zip/xml1-> (clojure.zip/xml-zip parsed-xml) :universe :system :delta-orionis :δ-ori-aa1 :radius data.zip/text)))
"Elapsed time: 13385.563442 msecs"
```

xml-iz
```clojure
boot.user=> (time (dotimes [_ 250000] (xml/find-first parsed-xml [:universe :system :delta-orionis :δ-ori-aa1 :radius])))
"Elapsed time: 885.660409 msecs"
```

## License

Copyright © 2017 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
