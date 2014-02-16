(ns clojure.remoterepl
  (:import [java.net ServerSocket Socket]
           [clojure.lang LineNumberingPushbackReader]
           [java.io PrintWriter InputStreamReader OutputStreamWriter]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def work (atom []))

(def repl (atom nil))

(def responses (atom {}))

(defn safe-apply [f args]
  (if objc?
    (clojure.lang.RemoteRepl/safetry (fn [] (apply f args)))
    (try (apply f args) (catch Exception e (.printStackTrace e)))))

(defn process-msg [f]
  (if (= 2 (count f))
    (let [[id r] f]
      (swap! responses assoc id r))
    (let [[id f args] f
          r (safe-apply f args)]
      (if objc?
        ($ @repl :println (pr-str [id r]))
        (.println @repl (pr-str [id r]))))))

(defn do-work []
  (let [w @work]
    (reset! work [])
    (doall (map process-msg w))))

(def in-call-remote (atom 0))

(defn call-remote [sel args]
  (swap! in-call-remote inc)
  (let [args (vec args)
        id (keyword (uuid))
        msg (pr-str [id sel args])]
    (if objc?
      ($ @repl :println msg)
      (.println @repl msg))
    (loop []
      (if (some #{id} (keys @responses))
        (let [r (id @responses)]
          (swap! responses dissoc id)
          (swap! in-call-remote dec)
          r)
        (do
          (Thread/sleep 10)
          (do-work)
          (recur))))))

(defn listen-objc [host port]
  (let [s ($ ($ ($ NSSocketImpl) :alloc)
             :initWithHost host
             :withPort (str port))]
    (clojure.lang.RemoteRepl/setConnected true)
    (println "Remote repl connected to" host ":" port)
    (reset! repl s)
    (loop [f ($ s :read)]
      (swap! work conj (read-string f))
      (recur ($ s :read)))))

(defn listen-jvm [port]
  (let [server (ServerSocket. port)]
    (println "Remote repl listening on port" port)
    (loop []
      (let [s (.accept server)
            out (PrintWriter. (.getOutputStream s) true)
            in (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s)))]
        (future
          (clojure.lang.RemoteRepl/setConnected true)
          (println "Client has connected!")
          (try
            (reset! repl out)
            (loop [f (read in)]
              (swap! work conj f)
              (recur (read in)))
            (catch Exception e
              (.printStackTrace e)))))
      (recur))))

(defn listen
  ([port] (listen nil port))
  ([host port]
      (future
        (if objc?
          (listen-objc host port)
          (listen-jvm port)))
      (future
        (loop []
          (when (zero? @in-call-remote)
            (if objc?
              (dispatch-main (do-work))
              (future (do-work))))
          (Thread/sleep 10)
          (recur)))))
