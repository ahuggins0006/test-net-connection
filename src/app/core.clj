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
   [cljfx.api :as fx]
   [clojure.pprint :as pprint]
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
        c2 (try-client ip receive-port)]
    (try
      (s/put! c data)
      {:ip ip, :send-port send-port, :receive-port receive-port, :result (str/includes? @(s/take! c2) data)}
      (catch Exception e (str "caught exception: " (.getMessage e))))))

(defn generate-report [file]
  (let [config-data  (edn/read-string (slurp file))
        addresses    (:addresses config-data)
        loop-backs   (:loop-back-configs config-data)
        ping-state   {:ping-state (check-addresses addresses)}]
    (if (some false? (vals (:ping-state ping-state)))
      {:report (merge ping-state {:config config-data} {:config-name file})}
      {:report (merge ping-state
                      {:loop-back-state
                       (zipmap (keys loop-backs)
                               (map (comp #(if (str/includes? % "exception") false %) loop-back-test)
                                    (vals loop-backs)))}
                      {:config config-data}
                      {:config-name file})})))




;; GUI


(defmulti event-handler :event/type)

(defn label-view [state] (mapv (fn [k] {:fx/type :label
                                        :font 16
                                        :text (str k)}) (keys state)))

(defn value-view [state] (mapv (fn [k] {:fx/type :rectangle
                                        :width 10
                                        :height 10
                                        :fill (if k :green :red)
                                        }) (vals state)))

(def *state
  (atom {}))

(defn root-view [{{:keys [ping-state loop-back-state config config-name]} :state}]
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :spacing 30
                  :padding 50
                  :children [{:fx/type :v-box
                              :spacing 15
                              :children (concat
                                         (label-view ping-state)
                                         (label-view loop-back-state))
                              }
                             {:fx/type :v-box
                              :spacing 25
                              :padding 5
                              :children (concat
                                         (value-view ping-state)
                                         (value-view loop-back-state))
                              }
                             {:fx/type :v-box
                              :spacing 30
                              :children [{:fx/type :text-area
                                          :editable false
                                          :text (str "REPORT\n\n"
                                                     "================================================================================\n\n"
                                                     "PINGS: "(with-out-str (pprint/pprint [ping-state]))
                                                     "\n"
                                                     "LOOP-BACKS: "(with-out-str (pprint/pprint [loop-back-state]))
                                                     "\n"
                                                     "CONFIG: "(with-out-str (pprint/pprint [config]))
                                                     "\n"
                                                     "CONFIG-NAME: "(with-out-str (pprint/pprint [config-name]))
                                                     )}
                                         {:fx/type :button
                                          :text "RETRY"
                                          :on-action (fn [_]
                                                       (reset! *state (:report (generate-report config-name)))
                                                       )}
                                         {:fx/type :button
                                          :text "EXIT"
                                          :on-action (fn [_] (System/exit 0))
                                          }

                                         ]
                              }

                             ]}}})


(defn -main [& args]
  {:pre [(is (not (empty? args)) "Please provide configuration file name.")]}
  ;;update state
  (let [renderer (fx/create-renderer
                  :middleware (fx/wrap-map-desc (fn [state]
                                                  {:fx/type root-view
                                                   :state state}))
                  :opts {:fx.opt/map-event-handler event-handler})]

    (swap! *state merge (:report (generate-report (first args))))

    (fx/mount-renderer *state renderer)))

