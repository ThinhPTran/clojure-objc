(ns clojure.remoterepl
  (:import [java.net ServerSocket Socket]
           [clojure.lang LineNumberingPushbackReader]
           [java.io PrintWriter InputStreamReader OutputStreamWriter]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def server1 (atom nil))
(def server2 (atom nil))
(def repl-main-thread (atom nil))
(def socket (atom nil))
(def socket1 (atom nil))
(def socket2 (atom nil))

(defn socket-println [s d]
  (let [c (str (count (.getBytes d "UTF-8")))]
    (.println s 
      (str 
        (apply str (for [n (range (- 10 (count c)))] " ")) 
        c))
    (.println s d)))

(defn process-msg [out f]
  (let [[run-in-main id f args] f]
    (->> [id (binding [force-main-thread true]
               (apply f args))]
         pr-str 
         (socket-println out))))

(defn call-remote [sel args]
  (let [args (vec args)
        id (keyword (uuid))]
    (socket-println (:out @socket)
              (pr-str [(or (= (Thread/currentThread) @repl-main-thread) force-main-thread)
                       id sel args]))
    (loop [msg (read (:in @socket))]
      (if (instance? String msg)
        (throw (Exception. msg))
        (if (= 2 (count msg))
          (let [[rid r] msg]
            (if (= rid id)
              r
              (do
                (socket-println (:out @socket) (pr-str [:retry id]))
                                        ; retries until the sender gets the response
                (recur (read (:in @socket))))))
          (do
            (process-msg (:out @socket) msg)
            (recur (read (:in @socket)))))))))

(defn start-remote-repl []
  (clojure.lang.RemoteRef/reset)
  (let [s (.accept @server1)
        s2 (.accept @server2)
        out (PrintWriter. (.getOutputStream s) true)
        in (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s)))
        out2 (PrintWriter. (.getOutputStream s2) true)
        in2 (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s2)))]
    (clojure.lang.RemoteRepl/setConnected true)
    (reset! socket {:out out :in in})
    (reset! socket1 {:out out :in in})      
    (reset! socket2 {:out out2 :in in2})
    (future
      (try
        (loop [f (read in2)]
          (let [s @socket]
            (reset! socket @socket2)
            (process-msg out2 f)
            (reset! socket s))
          (recur (read in2)))
        (catch Exception e
          (println "REPL DISCONNECTED")
          (.printStackTrace e)
          (.close s)
          (.close s2)
          (start-remote-repl))))))

(defn connected? []
  clojure.lang.RemoteRepl/connected)

(defn listen []
  (reset! repl-main-thread (Thread/currentThread))
  (reset! server1 (ServerSocket. 35813))
  (reset! server2 (ServerSocket. 35814))
  (start-remote-repl))
