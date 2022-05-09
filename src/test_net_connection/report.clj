(ns test-net-connection.report
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]
   [test-net-connection.ping :as ping]
   [test-net-connection.loop-back :as lb]
   ))


(defn generate-report [file]
  (let [config-data  (edn/read-string (slurp file))
        addresses    (:addresses config-data)
        loop-backs   (:loop-back-configs config-data)
        ping-state   {:ping-state (ping/check-addresses addresses)}]
    (if (some false? (vals (:ping-state ping-state)))
      {:report (merge ping-state {:config config-data} {:config-name file})}
      {:report (merge ping-state
                      {:loop-back-state
                       (zipmap (keys loop-backs)
                               (map (comp #(if (str/includes? % "exception") false %) lb/loop-back-test)
                                    (vals loop-backs)))}
                      {:config config-data}
                      {:config-name file})})))
