(in-ns 'clojure.core)

(require 'clojure.string)

(def ^:dynamic dispatch-class)

(def objc? (clojure.lang.ObjC/objc))

(when-not objc?
  (require '[clojure.walk :as walk]))

(defmacro dispatch-main
  "Runs the body with dispatch_sync in the main queue

  dispatch_sync(dispatch_get_main_queue(), ^{
    ...
  });"
  [& body]
  `(clojure.lang.RT/dispatchInMainSync
    (fn [] ~@body)))

(defn sel
  "Creates an objc selector.

  (sel \"some:selector:\")"
  [s]
  (clojure.lang.Selector. s))

(defn objc-class
  "Lookup an objc class by name.

  (objc-class \"UIView\")"
  [s]
  (RT/objcClass (name s)))

(defmacro $
  "The objc interop macro. Use to lookup a class or to msgSend.
  
  [UIView class] -> ($ UIView)
  [MyObject objectWithString:myString] -> ($ MyObject :objectWithString myString)
  [@\"Hello\" description] -> ($ \"Hello\" :description)
  [UIView alloc] -> ($ ($ UIView) :alloc)
  "
  [& args]
  (let [is-class (= 1 (count args))
        t (first args)]
    (if is-class
      `(objc-class '~t)
      (let [args (vec (next args))
            has-params (even? (count args))
            args (partition 2 (if has-params args (conj args nil)))
            params (mapv second args)
            selector (str (subs (apply str (map first args)) 1) (if has-params ":" ""))]
        (if has-params
          `(clojure.lang.Selector/invokeSelector ~selector ~t ~params)
          `(clojure.lang.Selector/invokeSelector ~selector ~t))))))

(defmacro $$
  "Like $ but calls super.
  
  [super initWithFrame:frame] -> ($$ self :initWithFrame frame)
  "
  [& args]
  (let [t (first args)
        dispatch-class (if (bound? #'dispatch-class) dispatch-class nil)
        args (vec (next args))
        has-params (even? (count args))
        args (if has-params (partition 2 args) args)
        params (if has-params (mapv second args) [])
        selector (if has-params (str (subs (apply str (map first args)) 1) ":") (name (first args)))]
    `($ ($ NSCommon) :invokeSuperSel ~t :withDispatchClass ~dispatch-class :withSelector ~selector :withArgs ~params)))

(def objc-types
  {:void \v
   :float \f
   :longlong \q
   :long \l
   :char \c
   :short \s
   :int \i
   :double \d
   :ulonglong \Q
   :ulong \L
   :uchar \C
   :ushort \S
   :uint \I
   :bool \b
   :cgpoint \P
   :nsrange \N
   :uiedge \E
   :cgsize \Z
   :cgaffinetransform \A
   :catransform3d \T
   :uioffset \O
   :cgrect \R
   :id \p
   :pointer \Y
   :cgfloat (if ($ ($ NSCommon) :cgfloatIsDouble) \d \f)})

(defmethod print-method clojure.lang.RemoteRef
  [^clojure.lang.RemoteRef r, ^java.io.Writer w]
  (.write w (str "#remote-ref \"" (.getId r) "\"")))

(defmethod print-method clojure.lang.ObjCClass
  [^clojure.lang.ObjCClass r, ^java.io.Writer w]
  (.write w (str "#objc-class \"" (.getName r) "\"")))

(defn read-remote-ref [id]
  (.get (clojure.lang.RemoteRef. id)))

(defn read-sel [name]
  (clojure.lang.Selector. name))

(alter-var-root #'*data-readers*
                (fn [m] (merge m {'sel #'read-sel
                                  'remote-ref #'read-remote-ref
                                  'objc-class #'objc-class})))

(defn class->types
  "Lookup a type for a class simple name"
  [c] 
  (case c
    :long :longlong
    :integer :int
    :boolean :bool
    :float :float
    :double :double
    :short :short
    :void :void
    :character :char
    :remoteref :pointer
    (if ($ c :isKindOfClass ($ NSValue))
      (some #(if (= (val %) ($ ($ NSCommon) :signatureToType ($ c :objCType))) (key %)) objc-types)
      :id)))

(defn types-for-vals [vals]
  (map (comp objc-types class->types keyword clojure.string/lower-case #(.getSimpleName %) class) vals))

(defn ccall
  "The c interop. To use a c function you need to register it in using:
  
  #import \"NSCommon.h\"
  reg_c(CGRectMake); // From now on you can use CGRectMake in ccall
  
  The args are unboxed when necessary and the return is always a boxed value.

  fun: the c function name
  types: a vector with the return type followed by the parameters types. Types map: clojure.core/objc-types
  args: the arguments vector

  CGRectMake(1, 2, 3, 4) -> (ccall \"CGRectMake\" [\\R \\f \\f \\f \\f] [1 2 3 4])
  "
  [fun types args]
  ($ ($ NSCommon) :ccall (name fun) :types (vec types) :args (vec args)))

(defmacro defc
  "Defines a c function. Supports variadic functions.
  Takes the function name, the return type and a vector of the arguments types.
  
  (defc CGRectMake :cgrect [:cgfloat :cgfloat :cgfloat :cgfloat])
  (CGRectMake 12 23 44 55)

  ;; Variadic
  (defc NSLog :void [:id &])
  (NSLog \"%@ %@ %d\" \"Hello\" \"World\" 44)
  "
  [n r types]
  (let [nn (name n)
        variadic? (= '& (last types))
        types (if variadic? (drop-last types) types)
        types (vec (map objc-types (cons r types)))]
    (if variadic?
      `(defn ~n [& args#] (ccall ~nn (apply conj ~types (types-for-vals (drop (dec (count ~types)) args#))) args#))
      `(defn ~n [& args#] (ccall ~nn ~types args#)))))

(defmacro nsproxy
  "nsproxy mocks an object. It's intended to implement protocols/delegators.

  (nsproxy
    ([^id self :doSomething] 
      \"I don't do anything\")
    ([^:bool self :textFieldShouldReturn ^:id field]
      ($ field :resignFirstResponder) 
      true))
  "
  [& methods]
  (let [objc-meta-type (comp objc-types ffirst meta)
        has-class (not (list? (first methods)))
        clazz (when has-class (name (first methods)))
        methods (if has-class (next methods) methods)
        i (map (fn [[args & body]]
                 (let [self-sym (first args)
                       args (next args)
                       sel (take-nth 2 args)
                       fields (take-nth 2 (next args))
                       types (map objc-meta-type (cons self-sym fields))
                       sel (if (pos? (count fields))
                             (reduce str (map #(str (name %) ":") sel))
                             (reduce str (map name sel)))]
                   `[~sel [[~@types] (fn [~self-sym ~@fields] ~@body)]]))
               methods)
        i (into {} i)]
    `($ ($ ($ NSProxyImpl) :alloc) :initWithClass ~clazz :map ~i)))

(defc objc_setAssociatedObject :void [:id :pointer :id :int])

(defc objc_getAssociatedObject :id [:id :pointer])

(def objc-keys-map (atom {}))

(defn objc-tag [tag]
  (if-let [t (tag @objc-keys-map)]
    t
    (let [t ($ (str "__" (name tag) "__") :UTF8String)]
      (swap! objc-keys-map assoc tag t)
      t)))

(defn objc-set!
  "Retains and sets a value into an object."
  [self tag value]
  (objc_setAssociatedObject self (objc-tag tag) value 1)) ; retain assign

(defn objc-get
  "Gets a value for a tag set with objc-set!."
  [self tag]
  (objc_getAssociatedObject self (objc-tag tag)))

(defmacro defnstype
  "nstype creates a new objc class.
 Takes a name, a superclass and a list of methods.
 
 The signature on the methods is optional when the method exists
 on the superclass, otherwise is mandatory.
 
 For the list of all available types see: clojure.core/objc-types
 
 (nstype MyTextField UITextField
  ([self :initWithFrame frame] ; Here the signature is not needed
   (doto ($$ self :initWithFrame frame)
    ($ :setDelegate self)))
  ([^:bool self :textFieldShouldReturn ^:id field]
   ($ self :resignFirstResponder)
   true))
 "
 [na super & methods]
 (let [methods
       (binding [dispatch-class (name na)]
         (mapv walk/macroexpand-all methods))
       na (name na)
       super (name super)
       fnsi (map
             (fn [[args & body]]
               (let [objc-meta-type (comp objc-types ffirst meta)
                     self-sym (first args)
                     args (next args)
                     sel (take-nth 2 args)
                     fields-vec (take-nth 2 (next args))
                     fields (take (count fields-vec) (repeatedly gensym))
                     fields-let (interleave fields-vec fields)
                     types (map objc-meta-type
                                (cons self-sym fields-vec))
                     types (if (nil? (first types)) nil (vec types))
                     sel (if (pos? (count fields))
                           (reduce str (map #(str (name %) ":") sel))
                           (reduce str (map name sel)))
                     fnname (symbol (str na ":" sel))]
                 [`(defn ~fnname [~self-sym ~@fields]
                     (let [~@fields-let] ~@body))
                  `[~sel [~types ~fnname]]]))
             methods)
       fns (map first fnsi)
       i (into {} (map second fnsi))]
   `(do
      ~@fns
      ($ ($ NSTypeImpl)
         :makeClassWithName ~na
         :superclass ~super
         :map ~i))))

(defn remote-repl
  "Starts a remote repl"
  ([] (remote-repl 35813))
  ([port]
     (clojure.lang.RemoteRepl/listen port)))
