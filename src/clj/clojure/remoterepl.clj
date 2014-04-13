(ns clojure.remoterepl
  (:import [java.net ServerSocket Socket]
           [clojure.lang LineNumberingPushbackReader]
           [java.io PrintWriter InputStreamReader OutputStreamWriter]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def pending (atom nil))

(def repl (atom nil))

(def responses (atom {}))

(defn process-msg [f]
  (println "PROCESS MSG" f)
  (let [[id f args] f]
    (.println @repl
              (pr-str [id (try (apply f args)
                               (catch Exception e
                                 (.printStackTrace e)))]))))

(def in-call-remote (atom 0))

(defn call-remote [sel args]
  (swap! in-call-remote inc)
  (let [args (vec args)
        id (keyword (uuid))]
    (.println @repl (pr-str [id sel args]))
    (loop []
      (if (some #{id} (keys @responses))
        (let [r (id @responses)]
          (swap! responses dissoc id)
          (swap! in-call-remote dec)
          (println "CALL REMOTE" sel r)
          r)
        (do
          (Thread/sleep 10)
          (when-let [w @pending]
            (reset! pending nil)
            (process-msg w))
          (recur))))))

(defn listen [port]
  (future
    (let [server (ServerSocket. port)]
      (println "Remote repl listening on port" port)
      (loop []
        (let [s (.accept server)
              out (PrintWriter. (.getOutputStream s) true)
              in (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s)))]
          (clojure.lang.RemoteRepl/setConnected true)
          (println "Client has connected!")
          (try
            (reset! repl out)
            (loop [f (read in)]
              (if (= 2 (count f))
                (let [[id r] f]
                  (swap! responses assoc id r))
                (if (zero? @in-call-remote)
                  (process-msg f)
                  (reset! pending f)))
              (recur (read in)))
            (catch Exception e
              (.printStackTrace e))))
        (recur)))))
