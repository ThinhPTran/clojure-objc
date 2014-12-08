# clojure-objc

WARNING! THIS IS AN ALPHA RELEASE

A clojure compiler that targets objc runtimes.

## Leiningen plugin
 
 https://github.com/galdolber/lein-objcbuild

## Goals

 * Write iOS and MacOS apps in clojure
 * Future proof: share 99.99% of the code base with clojure-jvm
 * Distribute clojure-objc libs using maven
 * Existing clojure libs should 'just work'
 * Dynamic objc interop
 * Dynamic c interop
 * Dynamic objc subclassing
 * REPL
 
## Memory management
 
 All generated code manage memory automagically, but if you alloc with interop you need to release!
 
## ObjC interop

    (defn say-hi [name]
        ($ ($ ($ ($ UIAlertView) :alloc)
             :initWithTitle (str "Hello " name)
             :message "Hi! from clojure"
             :delegate nil
             :cancelButtonTitle "Cancelar"
             :otherButtonTitles nil) :show))
 
## Discuss
 
 https://groups.google.com/d/forum/clojure-objc-discuss
 
## How to build dist
 
 lein exec build.clj

## License

Portions of this project derived from Clojure:
Copyright © 2006-2014 Rich Hickey

Original code and Clojure modifications:
Copyright © 2014 Gal Dolber

Both are distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
