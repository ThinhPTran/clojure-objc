;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(in-ns 'clojure.core)

(import '(java.lang.reflect Modifier Constructor)
        '(clojure.asm ClassWriter ClassVisitor Opcodes Type)
        '(clojure.asm.commons Method GeneratorAdapter)
        '(clojure.lang IPersistentMap Compiler Compiler$HostExpr))

;(defn method-sig [^java.lang.reflect.Method meth]
;  [(. meth (getName)) (seq (. meth (getParameterTypes)))])

(defn- filter-methods [^Class c invalid-method?]
  (loop [mm {}
         considered #{}
         c c]
    (if c
      (let [[mm considered]
            (loop [mm mm
                   considered considered
                   meths (seq (concat
                                (seq (. c (getDeclaredMethods)))
                                (seq (. c (getMethods)))))]
              (if meths
                (let [^java.lang.reflect.Method meth (first meths)
                      mods (. meth (getModifiers))
                      mk (method-sig meth)]
                  (if (or (considered mk)
                          (invalid-method? meth))
                    (recur mm (conj considered mk) (next meths))
                    (recur (assoc mm mk meth) (conj considered mk) (next meths))))
                [mm considered]))]
        (recur mm considered (. c (getSuperclass))))
      mm)))

(defn- non-private-methods [^Class c]
  (let [not-overridable? (fn [^java.lang.reflect.Method meth]
                           (let [mods (. meth (getModifiers))]
                             (or (not (or (Modifier/isPublic mods) (Modifier/isProtected mods)))
                                 (. Modifier (isStatic mods))
                                 (. Modifier (isFinal mods))
                                 (= "finalize" (.getName meth)))))]
    (filter-methods c not-overridable?)))

(defn- protected-final-methods [^Class c]
  (let [not-exposable? (fn [^java.lang.reflect.Method meth]
                         (let [mods (. meth (getModifiers))]
                           (not (and (Modifier/isProtected mods)
                                     (Modifier/isFinal mods)
                                     (not (Modifier/isStatic mods))))))]
    (filter-methods c not-exposable?)))

(defn- ctor-sigs [^Class super]
  (for [^Constructor ctor (. super (getDeclaredConstructors))
        :when (not (. Modifier (isPrivate (. ctor (getModifiers)))))]
    (apply vector (. ctor (getParameterTypes)))))

(defn- escape-class-name [^Class c]
  (.. (.getSimpleName c) 
      (replace "[]" "<>")))

(defn- overload-name [mname pclasses]
  (if (seq pclasses)
    (apply str mname (interleave (repeat \-) 
                                 (map escape-class-name pclasses)))
    (str mname "-void")))

(defn- ^java.lang.reflect.Field find-field [^Class c f]
  (let [start-class c]
    (loop [c c]
      (if (= c Object)
        (throw (new Exception (str "field, " f ", not defined in class, " start-class ", or its ancestors")))
        (let [dflds (.getDeclaredFields c)
              rfld (first (filter #(= f (.getName ^java.lang.reflect.Field %)) dflds))]
          (or rfld (recur (.getSuperclass c))))))))

;(distinct (map first(keys (mapcat non-private-methods [Object IPersistentMap]))))

(def ^{:private true} prim->class
     {'int Integer/TYPE
      'ints (Class/forName "[I")
      'long Long/TYPE
      'longs (Class/forName "[J")
      'float Float/TYPE
      'floats (Class/forName "[F")
      'double Double/TYPE
      'doubles (Class/forName "[D")
      'void Void/TYPE
      'short Short/TYPE
      'shorts (Class/forName "[S")
      'boolean Boolean/TYPE
      'booleans (Class/forName "[Z")
      'byte Byte/TYPE
      'bytes (Class/forName "[B")
      'char Character/TYPE
      'chars (Class/forName "[C")})

(defn- ^Class the-class [x] 
  (cond 
   (class? x) x
   (contains? prim->class x) (prim->class x)
   :else (let [strx (str x)]
           (clojure.lang.RT/classForName 
            (if (some #{\. \[} strx)
              strx
              (str "java.lang." strx))))))

;; someday this can be made codepoint aware
(defn- valid-java-method-name
  [^String s]
  (= s (clojure.lang.Compiler/munge s)))

(defn- validate-generate-class-options
  [{:keys [methods]}]
  (let [[mname] (remove valid-java-method-name (map (comp str first) methods))]
    (when mname (throw (IllegalArgumentException. (str "Not a valid method name: " mname))))))

(defn- generate-class [options-map]
  (validate-generate-class-options options-map)
  (let [default-options {:prefix "-" :load-impl-ns true :impl-ns (ns-name *ns*)}
        {:keys [name extends implements constructors methods main factory state init exposes 
                exposes-methods prefix load-impl-ns impl-ns post-init]} 
          (merge default-options options-map)
        name-meta (meta name)
        name (str name)
        package-name (subs name 0 (.lastIndexOf name "."))
        class-name (subs name (inc (.lastIndexOf name ".")))
        super (if extends (the-class extends) Object)
        interfaces (map the-class implements)
        supers (cons super interfaces)
        ctor-sig-map (or constructors (zipmap (ctor-sigs super) (ctor-sigs super)))
        cv (new ClassWriter (. ClassWriter COMPUTE_MAXS))
        cname (. name (replace "." "/"))
        pkg-name name
        impl-pkg-name (str impl-ns)
        impl-cname (.. impl-pkg-name (replace "." "/") (replace \- \_))
        ctype (. Type (getObjectType cname))
        iname (fn [^Class c] (.. Type (getType c) (getInternalName)))
        totype (fn [^Class c] (. Type (getType c)))
        to-types (fn [cs] (if (pos? (count cs))
                            (into-array (map totype cs))
                            (make-array Type 0)))
        obj-type ^Type (totype Object)
        arg-types (fn [n] (if (pos? n)
                            (into-array (replicate n obj-type))
                            (make-array Type 0)))
        super-type ^Type (totype super)
        init-name (str init)
        post-init-name (str post-init)
        factory-name (str factory)
        state-name (str state)
        main-name "main"
        var-name (fn [s] (clojure.lang.Compiler/munge (str s "__var")))
        class-type  (totype Class)
        rt-type  (totype clojure.lang.RT)
        var-type ^Type (totype clojure.lang.Var)
        ifn-type (totype clojure.lang.IFn)
        iseq-type (totype clojure.lang.ISeq)
        ex-type  (totype java.lang.UnsupportedOperationException)
        all-sigs (distinct (concat (map #(let[[m p] (key %)] {m [p]}) (mapcat non-private-methods supers))
                                   (map (fn [[m p]] {(str m) [p]}) methods)))
        sigs-by-name (apply merge-with concat {} all-sigs)
        overloads (into1 {} (filter (fn [[m s]] (next s)) sigs-by-name))
        var-fields (concat (when init [init-name]) 
                           (when post-init [post-init-name])
                           (when main [main-name])
                           ;(when exposes-methods (map str (vals exposes-methods)))
                           (distinct (concat (keys sigs-by-name)
                                             (mapcat (fn [[m s]] (map #(overload-name m (map the-class %)) s)) overloads)
                                             (mapcat (comp (partial map str) vals val) exposes))))
        emit-get-var (fn [^GeneratorAdapter gen v]
                       (let [false-label (. gen newLabel)
                             end-label (. gen newLabel)]
                         (. gen getStatic ctype (var-name v) var-type)
                         (. gen dup)
                         (. gen invokeVirtual var-type (. Method (getMethod "boolean isBound()")))
                         (. gen ifZCmp (. GeneratorAdapter EQ) false-label)
                         (. gen invokeVirtual var-type (. Method (getMethod "Object get()")))
                         (. gen goTo end-label)
                         (. gen mark false-label)
                         (. gen pop)
                         (. gen visitInsn (. Opcodes ACONST_NULL))
                         (. gen mark end-label)
                         (Compiler/emitSource "value = null;")
                         (Compiler/emitSource (str "if (" (var-name v) ".isBound()) {"))
                         (Compiler/tab)
                         (Compiler/emitSource (str "value = " (var-name v) ".get();"))
                         (Compiler/untab)
                         (Compiler/emitSource "}")))
        emit-unsupported (fn [^GeneratorAdapter gen ^Method m]
                           (let [msg (str (. m (getName)) " ("
                                          impl-pkg-name "/" prefix (.getName m)
                                          " not defined?)")]
                             (Compiler/emitSource (str "throw new " (Compiler/printClass ex-type) "(\"" msg "\");"))
                             (. gen (throwException ex-type msg))))
        emit-forwarding-method
        (fn [name pclasses rclass exclasses as-static else-gen]
          (let [mname (str name)
                pmetas (map meta pclasses)
                pclasses (map the-class pclasses)
                rclass (the-class rclass)
                ptypes (to-types pclasses)
                rtype ^Type (totype rclass)
                m (new Method mname rtype ptypes)
                is-overload (seq (overloads mname))
                gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (if as-static (. Opcodes ACC_STATIC) 0))
                         m nil nil cv)
                found-label (. gen (newLabel))
                else-label (. gen (newLabel))
                end-label (. gen (newLabel))
                rvoid (= (. rtype (getSort)) (. Type VOID))]
            (add-annotations gen (meta name))
            (dotimes [i (count pmetas)]
              (add-annotations gen (nth pmetas i) i))
            (Compiler/emitSource (str "public " (if as-static "static " "") (Compiler/printClass rclass) " " mname
                                      "(" (reduce1 str (interpose ", " (map #(str (Compiler/printClass
                                                                                   (nth pclasses %)) " p" %) (range (count pclasses)))))
                                      ")" (if (empty? exclasses) "" (str " throws "
                                                                         (reduce1 str (interpose ", "
                                                                                                 (map #(Compiler/printClass %) exclasses))))) " {"))
            (Compiler/tab)
            (. gen (visitCode))
            (Compiler/emitSource "Object value = null;")
            (if (> (count pclasses) 18)
              (else-gen gen m)
              (do
                (when is-overload
                                        ; TODO ?
                  (emit-get-var gen (overload-name mname pclasses))
                  (. gen (dup))
                  (. gen (ifNonNull found-label))
                  (. gen (pop)))
                (emit-get-var gen mname)
                (. gen (dup))
                (Compiler/emitSource (str "if (value != null) {"))
                (Compiler/tab)
                (. gen (ifNull else-label))
                (when is-overload
                  (. gen (mark found-label)))
                ;if found
                (.checkCast gen ifn-type)
                (when-not as-static
                  (. gen (loadThis)))
                ;box args
                (Compiler/emitSource (str (if rvoid "" "return ")
                                          (Compiler/unboxVal
                                           rclass (str "((IFn)value).invoke("
                                                       (reduce1
                                                        str (interpose
                                                             ", "
                                                             (map #(do
                                                                     (. gen (loadArg %))
                                                                     (. clojure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses %) (str "p" %))))
                                                                  (range (count ptypes))))) ")")) ";"))

                ;call fn
                (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                      (to-types (replicate (+ (count ptypes)
                                                                              (if as-static 0 1))
                                                                           Object)))))
                ;(into-array (cons obj-type
                ;                 (replicate (count ptypes) obj-type))))))
                ;unbox return
                (. gen (unbox rtype))
                (when rvoid
                  (. gen (pop)))
                (. gen (goTo end-label))

                ;else call supplied alternative generator
                (. gen (mark else-label))
                (. gen (pop))
                (Compiler/untab)
                (Compiler/emitSource "} else {")
                (Compiler/tab)

                (else-gen gen m)

                (Compiler/untab)
                (Compiler/emitSource "}")

                (. gen (mark end-label))))
            (Compiler/untab)
            (Compiler/emitSource "}")
            (. gen (returnValue))
            (. gen (endMethod))))
        ]
    (binding [*source-writer* (.getSc cv)]
      (Compiler/emitSource (str "package " package-name ";"))
      (Compiler/emitSource)
      (Compiler/emitSource (str "import clojure.lang.*;"))
      (Compiler/emitSource)
                                        ;start class definition
      (. cv (visit (. Opcodes V1_5) (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_SUPER))
                   cname nil (iname super)
                   (when-let [ifc (seq interfaces)]
                     (into-array (map iname ifc)))))

                                        ; class annotations
      (add-annotations cv name-meta)
      (Compiler/emitSource (str "public class " class-name " extends " (Compiler/printClass super)
                                (if (empty? interfaces) "" (str " implements " (apply str (interpose ", " (map #(Compiler/printClass %) interfaces))))) " {"))
      (Compiler/tab)

                                        ;static fields for vars
      (doseq [v var-fields]
        (. cv (visitField (+ (. Opcodes ACC_PRIVATE) (. Opcodes ACC_FINAL) (. Opcodes ACC_STATIC))
                          (var-name v)
                          (. var-type getDescriptor)
                          nil nil))
        (Compiler/emitSource (str "private final static Var " (var-name v) ";")))

                                        ;instance field for state
      (when state
        (. cv (visitField (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_FINAL))
                          state-name
                          (. obj-type getDescriptor)
                          nil nil))
        (Compiler/emitSource (str "public final Object " state-name ";")))

                                        ;static init to set up var fields and load init
      (let [gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                     (. Method getMethod "void <clinit> ()")
                     nil nil cv)]
        (Compiler/emitSource "static {")
        (Compiler/tab)
        (. gen (visitCode))
        (doseq [v var-fields]
          (. gen push impl-pkg-name)
          (. gen push (str prefix v))
          (. gen (invokeStatic var-type (. Method (getMethod "clojure.lang.Var internPrivate(String,String)"))))
          (. gen putStatic ctype (var-name v) var-type)
          (Compiler/emitSource (str (var-name v) " = Var.internPrivate(\"" impl-pkg-name "\", \"" (str prefix v) "\");")))

        (when load-impl-ns
          (. gen push "clojure.core")
          (. gen push "load")
          (. gen (invokeStatic rt-type (. Method (getMethod "clojure.lang.Var var(String,String)"))))
          (. gen push (str "/" impl-cname))
          (. gen (invokeInterface ifn-type (new Method "invoke" obj-type (to-types [Object]))))
                                        ;        (. gen push (str (.replace impl-pkg-name \- \_) "__init"))
                                        ;        (. gen (invokeStatic class-type (. Method (getMethod "Class forName(String)"))))
          (. gen pop)
          (Compiler/emitSource (str "RT.var(\"clojure.core\", " "\"load\").invoke(\"" (str "/" impl-cname) "\");")))

        (Compiler/untab)
        (Compiler/emitSource "}")
        (. gen (returnValue))
        (. gen (endMethod)))

                                        ;ctors
      (doseq [[pclasses super-pclasses] ctor-sig-map]
        (let [constructor-annotations (meta pclasses)
              pclasses (map the-class pclasses)
              super-pclasses (map the-class super-pclasses)
              ptypes (to-types pclasses)
              super-ptypes (to-types super-pclasses)
              m (new Method "<init>" (. Type VOID_TYPE) ptypes)
              super-m (new Method "<init>" (. Type VOID_TYPE) super-ptypes)
              gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) m nil nil cv)
              _ (add-annotations gen constructor-annotations)
              no-init-label (. gen newLabel)
              end-label (. gen newLabel)
              no-post-init-label (. gen newLabel)
              end-post-init-label (. gen newLabel)
              nth-method (. Method (getMethod "Object nth(Object,int)"))
              local (. gen newLocal obj-type)]
          (. gen (visitCode))
          (Compiler/emitSource (str "public " class-name "("
                                    (reduce1 str (interpose ", " (map #(str (Compiler/printClass
                                                                             (nth pclasses %)) " p" %) (range (count pclasses))))) ") {"))
          ; TODO init and post-init
          (comment (Compiler/tab)
            (Compiler/emitSource "Object value = null;"))
          (if init
            (do
              (Compiler/emitSource "{")
              (Compiler/tab)
              (emit-get-var gen init-name)
              (. gen dup)
              (. gen ifNull no-init-label)
              (Compiler/emitSource "if (value != null) {")
              (Compiler/tab)
              (.checkCast gen ifn-type)
                                        ;box init args
              (Compiler/emitSource
               (str "final Object found = RT.nth(((IFn)value).invoke("
                    (reduce1 str
                             (interpose ", "
                                        (map #(do
                                                (. gen (loadArg %))
                                                (. clojure.lang.Compiler$HostExpr
                                                   (emitBoxReturn nil gen (nth pclasses %) (str "p" %))))
                                             (range (count pclasses))))) "), 0);"))
                                        ;call init fn
              (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                    (arg-types (count ptypes)))))
                                        ;expecting [[super-ctor-args] state] returned
              (. gen dup)
              (. gen push (int 0))
              (. gen (invokeStatic rt-type nth-method))
              (. gen storeLocal local)

              (. gen (loadThis))
              (. gen dupX1)
              (Compiler/emitSource
               (str "super("
                (reduce1 str
                         (interpose ", "
                                    (map #(do
                                            (. gen loadLocal local)
                                            (. gen push (int %))
                                            (. gen (invokeStatic rt-type nth-method))
                                            (. clojure.lang.Compiler$HostExpr (emitUnboxArg nil gen (nth super-pclasses %)
                                                                                            (str "RT.nth(" "found" ", " % ")"))))
                                         (range (count super-pclasses))))) ");"))
              (. gen (invokeConstructor super-type super-m))

              (if state
                (do
                  (Compiler/emitSource (str state-name " = RT.nth(" "found" ", 1);"))
                  (. gen push (int 1))
                  (. gen (invokeStatic rt-type nth-method))
                  (. gen (putField ctype state-name obj-type)))
                (. gen pop))

              (. gen goTo end-label)
                                        ;no init found
              (Compiler/untab)
              (Compiler/emitSource "} else {")
              (Compiler/tab)
              (. gen mark no-init-label)
              (let [msg (str impl-pkg-name "/" prefix init-name " not defined")]
                (. gen (throwException ex-type msg))
                (Compiler/emitSource (str "throw new " (Compiler/printClass ex-type) "(\"" msg "\");")))
              (. gen mark end-label)
              (Compiler/untab)
              (Compiler/emitSource "}")
              (Compiler/untab)
              (Compiler/emitSource "}"))
            (if (= pclasses super-pclasses)
              (do
                (. gen (loadThis))
                (. gen (loadArgs))
                (. gen (invokeConstructor super-type super-m))
                (Compiler/emitSource (str "super(" (reduce1 str (interpose ", " (map #(str "p" %) (range (count pclasses))))) ");")))
              (throw (new Exception ":init not specified, but ctor and super ctor args differ"))))

          (when post-init
            (Compiler/emitSource "{")
            (Compiler/tab)
            (emit-get-var gen post-init-name)
            (. gen dup)
            (Compiler/emitSource "if (value != null) {")
            (Compiler/tab)
            (. gen ifNull no-post-init-label)
            (.checkCast gen ifn-type)
            (. gen (loadThis))
                                        ;box init args
            (Compiler/emitSource
             (str "((IFn)value).invoke("
                  (reduce1 str
                           (interpose ", "
                                      (map #(do
                                              (. gen (loadArg %))
                                              (. clojure.lang.Compiler$HostExpr (emitBoxReturn nil gen (nth pclasses %) (str "p" %))))
                                           (range (count pclasses))))) ")"))
                                        ;call init fn
            (. gen (invokeInterface ifn-type (new Method "invoke" obj-type
                                                  (arg-types (inc (count ptypes))))))
            (. gen pop)
            (. gen goTo end-post-init-label)
                                        ;no init found
            (. gen mark no-post-init-label)
            (Compiler/untab)
            (Compiler/emitSource "} else {")
            (let [msg (str impl-pkg-name "/" prefix post-init-name " not defined")]
              (Compiler/emitSource (str "throw new " (Compiler/printClass ex-type) "(\"" msg "\");"))
              (. gen (throwException ex-type msg)))
            (Compiler/untab)
            (Compiler/emitSource "}")
            (Compiler/untab)
            (Compiler/emitSource "}")
            (. gen mark end-post-init-label))

          (. gen (returnValue))
          (. gen (endMethod))
          (Compiler/untab)
          (Compiler/emitSource "}")
                                        ;factory
          (when factory
            (let [fm (new Method factory-name ctype ptypes)
                  gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                           fm nil nil cv)]
              (Compiler/emitSource (str "public static " (Compiler/printClass ctype) " " factory-name "("
                                        (reduce1 str (interpose ", " (map #(str (Compiler/printClass
                                                                                 (nth pclasses %)) " p" %) (range (count pclasses)))))
                                        ") {"))
              (Compiler/tab)
              (. gen (visitCode))
              (. gen newInstance ctype)
              (. gen dup)
              (. gen (loadArgs))
              (. gen (invokeConstructor ctype m))
              (Compiler/emitSource (str "return new " (Compiler/printClass ctype) "("
                                        (reduce1 str (interpose ", " (map #(str "p" %) (range (count pclasses))))) ");"))
              (Compiler/untab)
              (Compiler/emitSource "}")
              (. gen (returnValue))
              (. gen (endMethod))))))

                                        ;add methods matching supers', if no fn -> call super
      (let [mm (non-private-methods super)]
        (doseq [^java.lang.reflect.Method meth (vals mm)]
          (emit-forwarding-method (.getName meth) (.getParameterTypes meth) (.getReturnType meth) (.getExceptionTypes meth) false
                                  (fn [^GeneratorAdapter gen ^Method m]
                                    (. gen (loadThis))
                                        ;push args
                                    (. gen (loadArgs))
                                        ;call super
                                    (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                                            (. super-type (getInternalName))
                                                            (. m (getName))
                                                            (. m (getDescriptor))))
                                    (let [pclasses (map the-class (.getParameterTypes meth))
                                          rclass (the-class (.getReturnType meth))
                                          rtype ^Type (totype rclass)
                                          rvoid (= (. rtype (getSort)) (. Type VOID))]
                                      (Compiler/emitSource (str (if rvoid "" "return ") "super." (.getName m)
                                                                "(" (reduce1 str (interpose ", " (map #(str "p" %) (range (count pclasses))))) ");"))))))
                                        ;add methods matching interfaces', if no fn -> throw
        (reduce1 (fn [mm ^java.lang.reflect.Method meth]
                   (if (contains? mm (method-sig meth))
                     mm
                     (do
                       (emit-forwarding-method (.getName meth) (.getParameterTypes meth) (.getReturnType meth) (.getExceptionTypes meth) false
                                               emit-unsupported)
                       (assoc mm (method-sig meth) meth))))
                 mm (mapcat #(.getMethods ^Class %) interfaces))
                                        ;extra methods
        (doseq [[mname pclasses rclass :as msig] methods]
          (emit-forwarding-method mname pclasses rclass nil (:static (meta msig))
                                  emit-unsupported))
                                        ;expose specified overridden superclass methods
        (doseq [[local-mname ^java.lang.reflect.Method m] (reduce1 (fn [ms [[name _ _] m]]
                                                                     (if (contains? exposes-methods (symbol name))
                                                                       (conj ms [((symbol name) exposes-methods) m])
                                                                       ms)) [] (concat (seq mm)
                                                                                       (seq (protected-final-methods super))))]
          (let [pclasses (.getParameterTypes m)
                ptypes (to-types pclasses)
                rtype (totype (.getReturnType m))
                rvoid (= (. rtype (getSort)) (. Type VOID))
                exposer-m (new Method (str local-mname) rtype ptypes)
                target-m (new Method (.getName m) rtype ptypes)
                gen (new GeneratorAdapter (. Opcodes ACC_PUBLIC) exposer-m nil nil cv)]
            (Compiler/emitSource (str "public " (Compiler/printClass rtype) " " (.getName m) "("
                                      (reduce1 str (interpose ", " (map #(str (Compiler/printClass
                                                                               (nth pclasses %)) " p" %) (range (count pclasses))))) ") {"))
            (Compiler/tab)
            (. gen (loadThis))
            (. gen (loadArgs))
            (. gen (visitMethodInsn (. Opcodes INVOKESPECIAL)
                                    (. super-type (getInternalName))
                                    (. target-m (getName))
                                    (. target-m (getDescriptor))))
            (Compiler/emitSource (str (if rvoid "" "return ") "super." (.getName target-m) "("
                                      (reduce1 str (interpose ", " (map #(str "p" %) (range (count pclasses))))) ");"))
            (. gen (returnValue))
            (Compiler/untab)
            (Compiler/emitSource "}")
            (. gen (endMethod)))))
                                        ;main
      (when main
        (let [m (. Method getMethod "void main (String[])")
              gen (new GeneratorAdapter (+ (. Opcodes ACC_PUBLIC) (. Opcodes ACC_STATIC))
                       m nil nil cv)
              no-main-label (. gen newLabel)
              end-label (. gen newLabel)]
          (. gen (visitCode))
          (Compiler/emitSource (str "public static void main(String[] args) {"))
          (Compiler/tab)
          (Compiler/emitSource "Object value = null;")
          (emit-get-var gen main-name)

          (Compiler/emitSource (str "if (value != null) {"))
          (Compiler/tab)
          (Compiler/emitSource (str "((IFn)value).applyTo(RT.seq(args));"))
          (Compiler/emitSource "return;")
          (Compiler/untab)
          (Compiler/emitSource "}")
          (. gen dup)
          (. gen ifNull no-main-label)
          (.checkCast gen ifn-type)
          (. gen loadArgs)
          (. gen (invokeStatic rt-type (. Method (getMethod "clojure.lang.ISeq seq(Object)"))))
          (. gen (invokeInterface ifn-type (new Method "applyTo" obj-type
                                                (into-array [iseq-type]))))
          (. gen pop)
          (. gen goTo end-label)
                                        ;no main found
          (. gen mark no-main-label)
          (let [msg (str impl-pkg-name "/" prefix main-name " not defined")]
            (Compiler/emitSource (str "throw new " (Compiler/printClass ex-type) "(\"" msg "\");"))
            (. gen (throwException ex-type msg)))
          (. gen mark end-label)
          (. gen (returnValue))
          (Compiler/untab)
          (Compiler/emitSource "}")
          (. gen (endMethod))))
                                        ;field exposers
      (doseq [[f {getter :get setter :set}] exposes]
        (let [fld (find-field super (str f))
              ftype (totype (.getType fld))
              static? (Modifier/isStatic (.getModifiers fld))
              acc (+ Opcodes/ACC_PUBLIC (if static? Opcodes/ACC_STATIC 0))]
          (when getter
            (let [m (new Method (str getter) ftype (to-types []))
                  gen (new GeneratorAdapter acc m nil nil cv)]
              (Compiler/emitSource (str "public " (if static? "static " "") (Compiler/printClass ftype) " " (str getter) "() {"))
              (Compiler/tab)
              (. gen (visitCode))
              (Compiler/emitSource (str "return " f ";"))
              (if static?
                (. gen getStatic ctype (str f) ftype)
                (do
                  (. gen loadThis)
                  (. gen getField ctype (str f) ftype)))
              (Compiler/untab)
              (Compiler/emitSource "}")
              (. gen (returnValue))
              (. gen (endMethod))))
          (when setter
            (let [m (new Method (str setter) Type/VOID_TYPE (into-array [ftype]))
                  gen (new GeneratorAdapter acc m nil nil cv)]
              (Compiler/emitSource (str "public " (if static? "static " "") "void " (str setter) "(" (Compiler/printClass ftype) " value) {"))
              (Compiler/tab)
              (. gen (visitCode))
              (if static?
                (do
                  (Compiler/emitSource (str (Compiler/printClass ctype) "." f " = value;"))
                  (. gen loadArgs)
                  (. gen putStatic ctype (str f) ftype))
                (do
                  (Compiler/emitSource (str "this." f " = value;"))
                  (. gen loadThis)
                  (. gen loadArgs)
                  (. gen putField ctype (str f) ftype)))
              (Compiler/untab)
              (Compiler/emitSource "}")
              (. gen (returnValue))
              (. gen (endMethod))))))
                                        ;finish class def
      (. cv (visitEnd))
      (Compiler/untab)
      (Compiler/emitSource "}")
      (Compiler/writeSourceFile cname (str *source-writer*))
      [cname (. cv (toByteArray))])))

(defmacro gen-class
  "When compiling, generates compiled bytecode for a class with the
  given package-qualified :name (which, as all names in these
  parameters, can be a string or symbol), and writes the .class file
  to the *compile-path* directory.  When not compiling, does
  nothing. The gen-class construct contains no implementation, as the
  implementation will be dynamically sought by the generated class in
  functions in an implementing Clojure namespace. Given a generated
  class org.mydomain.MyClass with a method named mymethod, gen-class
  will generate an implementation that looks for a function named by
  (str prefix mymethod) (default prefix: \"-\") in a
  Clojure namespace specified by :impl-ns
  (defaults to the current namespace). All inherited methods,
  generated methods, and init and main functions (see :methods, :init,
  and :main below) will be found similarly prefixed. By default, the
  static initializer for the generated class will attempt to load the
  Clojure support code for the class as a resource from the classpath,
  e.g. in the example case, ``org/mydomain/MyClass__init.class``. This
  behavior can be controlled by :load-impl-ns

  Note that methods with a maximum of 18 parameters are supported.

  In all subsequent sections taking types, the primitive types can be
  referred to by their Java names (int, float etc), and classes in the
  java.lang package can be used without a package qualifier. All other
  classes must be fully qualified.

  Options should be a set of key/value pairs, all except for :name are optional:

  :name aname

  The package-qualified name of the class to be generated

  :extends aclass

  Specifies the superclass, the non-private methods of which will be
  overridden by the class. If not provided, defaults to Object.

  :implements [interface ...]

  One or more interfaces, the methods of which will be implemented by the class.

  :init name

  If supplied, names a function that will be called with the arguments
  to the constructor. Must return [ [superclass-constructor-args] state]
  If not supplied, the constructor args are passed directly to
  the superclass constructor and the state will be nil

  :constructors {[param-types] [super-param-types], ...}

  By default, constructors are created for the generated class which
  match the signature(s) of the constructors for the superclass. This
  parameter may be used to explicitly specify constructors, each entry
  providing a mapping from a constructor signature to a superclass
  constructor signature. When you supply this, you must supply an :init
  specifier.

  :post-init name

  If supplied, names a function that will be called with the object as
  the first argument, followed by the arguments to the constructor.
  It will be called every time an object of this class is created,
  immediately after all the inherited constructors have completed.
  Its return value is ignored.

  :methods [ [name [param-types] return-type], ...]

  The generated class automatically defines all of the non-private
  methods of its superclasses/interfaces. This parameter can be used
  to specify the signatures of additional methods of the generated
  class. Static methods can be specified with ^{:static true} in the
  signature's metadata. Do not repeat superclass/interface signatures
  here.

  :main boolean

  If supplied and true, a static public main function will be generated. It will
  pass each string of the String[] argument as a separate argument to
  a function called (str prefix main).

  :factory name

  If supplied, a (set of) public static factory function(s) will be
  created with the given name, and the same signature(s) as the
  constructor(s).

  :state name

  If supplied, a public final instance field with the given name will be
  created. You must supply an :init function in order to provide a
  value for the state. Note that, though final, the state can be a ref
  or agent, supporting the creation of Java objects with transactional
  or asynchronous mutation semantics.

  :exposes {protected-field-name {:get name :set name}, ...}

  Since the implementations of the methods of the generated class
  occur in Clojure functions, they have no access to the inherited
  protected fields of the superclass. This parameter can be used to
  generate public getter/setter methods exposing the protected field(s)
  for use in the implementation.

  :exposes-methods {super-method-name exposed-name, ...}

  It is sometimes necessary to call the superclass' implementation of an
  overridden method.  Those methods may be exposed and referred in
  the new method implementation by a local name.

  :prefix string

  Default: \"-\" Methods called e.g. Foo will be looked up in vars called
  prefixFoo in the implementing ns.

  :impl-ns name

  Default: the name of the current ns. Implementations of methods will be
  looked up in this namespace.

  :load-impl-ns boolean

  Default: true. Causes the static initializer for the generated class
  to reference the load code for the implementing namespace. Should be
  true when implementing-ns is the default, false if you intend to
  load the code via some other method."
  {:added "1.0"}

  [& options]
  (when *compile-files*
    (let [options-map (into1 {} (map vec (partition 2 options)))
          [cname bytecode] (generate-class options-map)]
      (Compiler/writeClassFile cname bytecode))))

;;;;;;;;;;;;;;;;;;;; gen-interface ;;;;;;;;;;;;;;;;;;;;;;
;; based on original contribution by Chris Houser

(defn- ^Type asm-type
  "Returns an asm Type object for c, which may be a primitive class
  (such as Integer/TYPE), any other class (such as Double), or a
  fully-qualified class name given as a string or symbol
  (such as 'java.lang.String)"
  [c]
  (if (or (instance? Class c) (prim->class c))
    (Type/getType (the-class c))
    (let [strx (str c)]
      (Type/getObjectType
       (.replace (if (some #{\. \[} strx)
                   strx
                   (str "java.lang." strx))
                 "." "/")))))

(defn- generate-interface
  [{:keys [name extends methods]}]
  (when (some #(-> % first clojure.core/name (.contains "-")) methods)
    (throw
     (IllegalArgumentException. "Interface methods must not contain '-'")))
  (let [sname (str name)
        iname (.replace sname "." "/")
        cv (ClassWriter. ClassWriter/COMPUTE_MAXS)
        lastdot (.lastIndexOf sname ".")
        classname (subs sname (inc lastdot))
        packagename (subs sname 0 lastdot)]
    (binding [*source-writer* (.getSc cv)]
      (Compiler/emitSource (str "package " packagename ";"))
      (Compiler/emitSource)
      (Compiler/emitSource (str "public interface " classname " {"))
      (Compiler/tab)
      (. cv visit Opcodes/V1_5 (+ Opcodes/ACC_PUBLIC
                                  Opcodes/ACC_ABSTRACT
                                  Opcodes/ACC_INTERFACE)
         iname nil "java/lang/Object"
         (when (seq extends)
           (into-array (map #(.getInternalName (asm-type %)) extends))))
      (add-annotations cv (meta name))
      (doseq [[mname pclasses rclass pmetas] methods]
        (Compiler/emitSource (str
                              (Compiler$HostExpr/tagToCanonical rclass)
                              " " (str mname) "("
                              (let [names (map #(str (let [c (nth pclasses %)]
                                                       (Compiler$HostExpr/tagToCanonical c)) " p" %) (range (count pclasses)))]
                                    (apply str (interpose ", " names)))
                                  ");"))
        (let [mv (. cv visitMethod (+ Opcodes/ACC_PUBLIC Opcodes/ACC_ABSTRACT)
                    (str mname)
                    (Type/getMethodDescriptor (asm-type rclass)
                                              (if pclasses
                                                (into-array Type (map asm-type pclasses))
                                                (make-array Type 0)))
                    nil nil)]
          (add-annotations mv (meta mname))
          (dotimes [i (count pmetas)]
            (add-annotations mv (nth pmetas i) i))
          (. mv visitEnd)))
      (. cv visitEnd)
      (Compiler/untab)
      (Compiler/emitSource "}")
      (when *compile-files*
        (Compiler/writeSourceFile iname (str *source-writer*)))
      [iname (. cv toByteArray)])))

(defmacro gen-interface
  "When compiling, generates compiled bytecode for an interface with
  the given package-qualified :name (which, as all names in these
  parameters, can be a string or symbol), and writes the .class file
  to the *compile-path* directory.  When not compiling, does nothing.

  In all subsequent sections taking types, the primitive types can be
  referred to by their Java names (int, float etc), and classes in the
  java.lang package can be used without a package qualifier. All other
  classes must be fully qualified.

  Options should be a set of key/value pairs, all except for :name are
  optional:

  :name aname

  The package-qualified name of the class to be generated

  :extends [interface ...]

  One or more interfaces, which will be extended by this interface.

  :methods [ [name [param-types] return-type], ...]

  This parameter is used to specify the signatures of the methods of
  the generated interface.  Do not repeat superinterface signatures
  here."
  {:added "1.0"}

  [& options]
    (let [options-map (apply hash-map options)
          [cname bytecode] (generate-interface options-map)]
      (when *compile-files*
        (clojure.lang.Compiler/writeClassFile cname bytecode))
      (.defineClass ^DynamicClassLoader (deref clojure.lang.Compiler/LOADER)
                    (str (:name options-map)) bytecode options)))

(comment

  (defn gen-and-load-class
    "Generates and immediately loads the bytecode for the specified
    class. Note that a class generated this way can be loaded only once
    - the JVM supports only one class with a given name per
    classloader. Subsequent to generation you can import it into any
    desired namespaces just like any other class. See gen-class for a
    description of the options."
    {:added "1.0"}

    [& options]
    (let [options-map (apply hash-map options)
          [cname bytecode] (generate-class options-map)]
      (.. (clojure.lang.RT/getRootClassLoader) (defineClass cname bytecode options))))

  )
