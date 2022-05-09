(ns test-net-connection.loop-back
  (:require
   [aleph.tcp :as tcp]
   [gloss.io :as io]
   [gloss.core :as gloss]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.string :as str]
   )
  )

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
        c2 (try-client ip receive-port)]
    (try
      (s/put! c data)
      {:ip ip, :send-port send-port, :receive-port receive-port, :result (str/includes? @(s/take! c2) data)}
      (catch Exception e (str "caught exception: " (.getMessage e))))))

