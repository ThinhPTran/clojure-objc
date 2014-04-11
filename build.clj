(use '[clojure.java.shell :only [sh with-sh-dir]])
(use '[clojure.java.io :only [delete-file file]])
(require '[clojure.string :as st])
(import '[java.io File])

(def iphone-os-sdk "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS7.1.sdk")
(def iphone-simulator-sdk "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator7.1.sdk")
(def frameworks "-framework UIKit -framework Foundation")
(def opts "-g -miphoneos-version-min=6.1 -fmessage-length=0 -fmacro-backtrace-limit=0 -std=gnu99 -fpascal-strings -O3 -DDEBUG=1 -Wno-unsequenced")

(let [a (agent nil)]
  (defn println+ [& other]
    (send a (fn [_] (apply println other)))))

(defn walk [^File dir]
  (let [children (.listFiles dir)
        subdirs (filter #(.isDirectory %) children)
        files (filter #(.isFile %) children)]
    (concat files (mapcat walk subdirs))))

(defn find-files [folder extension]
  (filter #(.endsWith (.getName %) (str "." extension)) (walk (file folder))))

(defn sh+ [& args]
  (let [silent (= :silent (first args))
        _ (when-not silent
            (println+ "Running: " (reduce str (interpose " " args))))
        args (map #(clojure.string/split % #" ") 
                  (if silent (next args) args))
        r (apply sh (flatten args))]
    (when-not (zero? (:exit r))
      (println+ "FAILED" (:err r)))))

(defn makeoname [f]
  (str (st/replace f #"/" ".") ".o"))

(defn clang [id params sdk target f]
  (println+ id (.getName f))
  (sh+ :silent "clang" "-x" "objective-c" params opts "-isysroot" sdk
       (str "-I" target "/../src/ffi")
       (str "-I" target "/objc")
       "-I/Users/admin/github/j2objc/dist/include"
       "-c" (.getCanonicalPath f) "-o" (makeoname (.getPath f))))

(defn build [id params sdk]
  (let [target (File. "target")
        tcn (.getCanonicalPath target)]
    (with-sh-dir target
      (println+ "Compiling" id)
      (sh+ "rm" "-Rf" id)
      (sh+ "mkdir" id)
      (with-sh-dir (File. (str "target/" id))
        (doall (pmap (partial clang id params sdk tcn) 
                     (find-files "target/objc" "m")))
        (spit (str tcn "/" id "/files.LinkFileList") (reduce str (interpose "\n" (find-files (str tcn "/" id) "o"))))
        (sh+ "libtool" "-static" "-syslibroot" sdk "-filelist" 
             "files.LinkFileList" frameworks "-o" "libclojure-objc.a")))))

(sh+ "mvn" "clean" "compile" "test-compile")
(sh+ "rm" "-Rf" "target/objc")
(sh+ "mkdir" "target/objc")
(sh+ "cp" "-R" "src/objc/." "target/objc")
(sh+ "cp" "-R" "src/ffi/." "target/objc")
(sh+ "zip" "-r" "target/objc.jar" "target/gen" "src/jvm" "test/java")
(sh+ "j2objc" "-d" "target/objc" "-classpath" 
     "target/classes:target/test-classes" 
     "target/objc.jar")

(let [i (File. "target/include")]
  (when-not (.exists i)
    (.mkdirs i)))

(with-sh-dir (File. "target/objc")
  (sh "rsync" "-avm" "--delete" "--include" "*.h" "-f" 
       "hide,! */" "." "../include"))

(build "iphoneos" "-arch armv7 -arch armv7s -arch arm64" iphone-os-sdk)
(build "iphonesimulator" "-arch i386 -arch x86_64" iphone-simulator-sdk)

(let [a (File. "target/libclojure-objc.a")]
  (when (.exists a)
    (.delete a))
  (sh+ "lipo" "-create" "-output" "target/libclojure-objc.a" "target/iphoneos/libclojure-objc.a" "target/iphonesimulator/libclojure-objc.a"))


