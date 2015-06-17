# clojure-objc

A Clojure compiler that targets objc runtimes.

 * Write native apps in Clojure
 * Strong iOS support
 * Future proof: shares 99.99% of the code base with clojure for the jvm
 * Distribute clojure-objc libs using maven
 * Most existing Clojure libs should just work
 * ObjC interop
 * C interop
 * ObjC subclassing
 * REPL!

![alt usage guide](https://github.com/galdolber/clojure-objc-sample/raw/master/ios.gif)

## Leiningen plugin
 
 https://github.com/galdolber/lein-objcbuild
 
## Dependency

[![Clojars Project](http://clojars.org/galdolber/clojure-objc/latest-version.svg)](http://clojars.org/galdolber/clojure-objc)
 
## Memory management
 
 All generated code manage memory automagically, but if you alloc with interop you need to release!
 
## ObjC interop
    
    ;; calling objc methods
    (defn say-hi [name]
      (-> ($ UIAlertView)
          ($ :alloc)
          ($ :initWithTitle (str "Hello " name)
             :message "Hi! from clojure"
             :delegate nil
             :cancelButtonTitle "Cancelar"
             :otherButtonTitles nil)
          ($ :autorelease)
          ($ :show)))
 
    ;; extend objc class
    (defnstype UIKitController UIViewController
      ([^:id self :initWith ^:id [view s]]
         (doto ($$ self :init)
           ($ :setView ($ view :retain))
           (objc-set! :scope s)
           (#(post-notification ($ % :view) :init)))))
           
    ;; c interop
    (defc NSLog :void [:id &]) ; & for variadic
    (NSLog "%@ %@ %d" "Hello" "World" 13)

    ;; proxy objc class
    (nsproxy
      ([^:bool self :textFieldShouldReturn ^:id field]
        ($ field :resignFirstResponder) 
        true))
      
## Presentations

http://www.slideshare.net/GalDolber/clojureobjc-47500127
 
## Discuss
 
 https://groups.google.com/d/forum/clojure-objc-discuss
 
## How to build dist
 
 lein exec build.clj

## License

Portions of this project derived from Clojure:
Copyright © 2006-2015 Rich Hickey

Original code and Clojure modifications:
Copyright © 2014-2015 Gal Dolber

Both are distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
