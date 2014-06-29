(ns clojure.remoterepl
  (:import [java.net ServerSocket Socket]
           [clojure.lang LineNumberingPushbackReader]
           [java.io PrintWriter InputStreamReader OutputStreamWriter]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(def repl-main-thread (atom nil))
(def socket (atom nil))
(def socket1 (atom nil))
(def socket2 (atom nil))

(defn process-msg [out f]
  (let [[run-in-main id f args] f]
    (->> [id (if run-in-main
               (binding [force-main-thread true]
                 (apply f args))
               (apply f args))]
         pr-str 
         (.println out))))

(defn call-remote [sel args]
  (let [args (vec args)
        id (keyword (uuid))]
    (.println (:out @socket)
              (pr-str [(or (= (Thread/currentThread) @repl-main-thread)
                           (and (bound? #'force-main-thread) force-main-thread))
                       id sel args]))
    (loop [msg (read (:in @socket))]
      (if (instance? String msg)
        (throw (Exception. msg))
        (if (= 2 (count msg))
          (let [[rid r] msg]
            (if (= rid id)
              r
              (do
                (.println (:out @socket) (pr-str [:retry rid]))
                                        ; retries until the sender gets the response
                (recur (read (:in @socket))))))
          (do
            (process-msg (:out @socket) msg)
            (recur (read (:in @socket)))))))))

(defn listen []
  (reset! repl-main-thread (Thread/currentThread))
  (future
    (let [server (ServerSocket. 35813)]
      (println "Remote repl started")
      (let [s (.accept server)
            out (PrintWriter. (.getOutputStream s) true)
            in (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s)))]
        (clojure.lang.RemoteRepl/setConnected true)
        (println "Client has connected!")
        (reset! socket1 {:out out :in in})
        (reset! socket {:out out :in in}))))

  (future
    (let [server (ServerSocket. 35814)
          s (.accept server)
          out (PrintWriter. (.getOutputStream s) true)
          in (LineNumberingPushbackReader. (InputStreamReader. (.getInputStream s)))]
      (reset! socket2 {:out out :in in})
      (loop [f (read in)]
        (let [s @socket]
          (reset! socket @socket2)
          (process-msg out f)
          (reset! socket s))
        (recur (read in))))))
