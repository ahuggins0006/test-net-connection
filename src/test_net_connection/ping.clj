(ns test-net-connection.ping
  (:require
   [clojure.java.shell :as shell]
   [clojure.string :as str]
   ))

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
