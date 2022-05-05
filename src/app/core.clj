(ns app.core
  (:require
   [gloss.io :as io]
   [gloss.core :as gloss]
   [aleph.tcp :as tcp]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]
   [clojure.test :refer [is]]
   [clojure.string :as str])
  (:gen-class))

;;ping to make sure there is something to connect to
(defn ping-something [something]
  (case (.toLowerCase (System/getProperty "os.name"))
    "linux"                     (#(if (= (second %) 0) true false) (first (shell/sh "ping" "-c" "1" "-W" "3" something)))
    ("windows 10" "windows 11") (str/includes? (:out (shell/sh "cmd" "/C" "powershell.exe" "Test-Connection" something "-Quiet" "-Count" "1")) "True")))

;; ping all addresses
(defn check-addresses [addresses]
  (let [p (pmap ping-something addresses)
        k (mapv keyword addresses)]
    (zipmap k p)
  ))

;; using aleph to create clients for loop-back testing

(def protocol (gloss/compile-frame (gloss/string :ascii)))

(defn wrap-duplex-stream
  [protocol s]
  (let [out (s/stream)]
    (s/connect
     (s/map #(io/encode protocol %) out)
     s)
    (s/splice
     out
     (io/decode-stream s protocol))))

(defn client
  [host port]
  (d/chain (tcp/client {:host host, :port port})
    #(wrap-duplex-stream protocol %)))


(defn try-client [ip port] (try @(client ip port) (catch Exception e (str "caught exception: " (.getMessage e)))))

(defn loop-back-test [config]
  (let [{:keys [ip send-port receive-port data]} config
        c  (try-client ip send-port)
        c2 (try-client ip receive-port)
        ]
     (try
       (s/put! c data)
       {:ip ip, :send-port send-port, :receive-port receive-port, :result (str/includes? @(s/take! c2) data)}
                (catch Exception e (str "caught exception: " (.getMessage e))))
  )
)


(defn -main [& args]
  {:pre [(is (not (empty? args)) "Please provide configuration file name.")]}
  (let [addresses    (:addresses (edn/read-string (slurp (first args))))
        loop-backs   (:loop-back-configs (edn/read-string (slurp (first args))))
        ping-state   {:ping-state (check-addresses addresses)}]

    (if (some false? (vals (:ping-state ping-state)))
      (println ping-state)
      (println {:report (merge ping-state {:loop-back-state (zipmap (keys loop-backs) (map loop-back-test (vals loop-backs)))})})))
  (System/exit 0))

