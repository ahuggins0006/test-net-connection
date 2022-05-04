(ns hugginsat.tcp-tool
  (:require
   [gloss.io :as io]
   [gloss.core :as gloss]
   [aleph.tcp :as tcp]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]
   [tilakone.core :as tk :refer [_]]
   [clojure.string :as str]))

;; ping something
(defn ping-something [something] (shell/sh "ping" "-c" "1" "-W" "3" something))



(def addresses (:addresses (edn/read-string (slurp "resources/config.edn"))))
(def p (map (comp first ping-something) addresses))

addresses
p

(defn check-addresses [addresses]
  (let [p (mapv (comp #(if (= (second %) 0) true false) first ping-something) addresses)
        k (mapv keyword addresses)]
    (zipmap k p)
  ))

(check-addresses addresses)


(def ping-state {:ping-state (check-addresses addresses)})

ping-state

(-> ping-state :ping-state :10.10.3.200)


;; using aleph

(def vector-bytes-frame
  "Frames are just clojure data structures, this frame is a vector of bytes."
  [:byte :byte :byte :byte :byte :byte :byte :byte :byte ])
;;(def protocol (gloss/compile-frame vector-bytes-frame))
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


(defn start-server
  [handler port]
  (tcp/start-server
    (fn [s info]
      (handler (wrap-duplex-stream protocol s) info))
    {:port port}))

(defn fast-echo-handler
  [f]
  (fn [s info]
    (s/connect
      (s/map f s)
      s)))

;;servers
(def s
  (start-server
    (fast-echo-handler str)
    4762))
(def s2
  (start-server
   (fast-echo-handler str)
   10002))

;;clients
(def c @(client "10.10.1.40" 4760))

(def c2 @(client "10.10.1.40" 4762))

(defn try-client [ip port] (try @(client ip port) (catch Exception e (str "caught exception: " (.getMessage e)))))

(def c-try (try-client "localhost" 4762))
(s/put! c-try "hello")
(type c-try)
(def loop-conf {:loop-back-config {:ip "10.10.1.40" :send-port 4760 :receive-port 4762 :data "hello clojure!"}})

(def loop-local-conf {:loop-back-config {:ip "localhost" :send-port 4760 :receive-port 4762 :data "hello clojure!\n"}})
(def loop-local-same {:loop-back-config {:ip "localhost" :send-port 4762 :receive-port 4762 :data "hello clojure!\n"}})


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

(loop-back-test (:loop-back-config loop-local-conf))
(loop-back-test (:loop-back-config loop-local-same))

;; test all configs and merge results
(def loopbacks (:loop-back-configs (edn/read-string (slurp "resources/config.edn"))))
loopbacks
(map #({(key %) (loop-back-test (val %))}) loopbacks)
(zipmap (keys loopbacks) (map loop-back-test (vals loopbacks)))

(def lbs {:loop-back-state  (loop-back-test "10.10.1.40" 4760 4762 "1")})
lbs

(merge ping-state {:loop-back-state(zipmap (keys loopbacks) (map loop-back-test (vals loopbacks)))})


(def c3 @(client "localhost" 4762))
(def c4 @(client "localhost" 4762))

(s/put! c "1")
;;(s/put! c  (map unchecked-byte [0xfe 0x6b 0x28 0x40 0x82 0x00 0x00 0x00 0x01]))
;;(s/put! c "hello world")
;;(s/put! c3 (map unchecked-byte [0xfe 0x6b 0x28 0x40 0x82 0x00 0x00 0x00 0x01]))
(s/put! c3 "how are you?")
;;(s/put! c2 "1a")

;;(s/take! c)
@(s/take! c3)
;;deref stream to get value
(def answer @(s/take! c3))

(= "how are you?\n" answer )
(s/put! c4 "hello from c4")
(s/take! c4)

(.close s)
(.close s2)
