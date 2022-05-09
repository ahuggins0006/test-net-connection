(ns test-net-connection.core
  (:require
   [clojure.test :refer [is]]
   [cljfx.api :as fx]
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [test-net-connection.report :as r]
   )
  (:import [javafx.stage FileChooser]
             [javafx.event ActionEvent]
             [javafx.scene Node])
  (:gen-class))

(def *state
  (atom {}))

(defmulti handle ::event)

(defmethod handle ::open-file [{:keys [^ActionEvent fx/event]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Open File"))]
    (when-let [file (.showOpenDialog chooser window)]
      (swap! *state merge (r/generate-report file))
      )))

(defn label-view [state] (mapv (fn [k] {:fx/type :label
                                        :text (str k)}) (keys state)))

(defn value-view [state] (mapv (fn [k] {:fx/type :rectangle
                                        :width 12
                                        :height 12
                                        :fill (if k :green :red)
                                        }) (vals state)))

(defn root-view [{:keys [ping-state loop-back-state config config-name]}]
  {:fx/type :stage
   :showing true
   :title "Net Connection Tool"
   :width 800
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :h-box
                  :spacing 50
                  :padding 50
                  :children [{:fx/type :v-box
                              :spacing 10
                              :children (concat
                                         (label-view ping-state)
                                         (label-view loop-back-state))
                              }
                             {:fx/type :v-box
                              :spacing 15
                              :padding 5
                              :children (concat
                                         (value-view ping-state)
                                         (value-view loop-back-state))
                              }
                             {:fx/type :v-box
                              :spacing 10
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
                                                     "CONFIG-NAME: "(with-out-str (pprint/pprint [(str config-name)]))
                                                     )}
                                         {:fx/type :button
                                          :text "LOAD CONFIG FILE..."
                                          :on-action {::event ::open-file}
                                          }

                                         {:fx/type :button
                                          :text "RETRY"
                                          :on-action (fn [_]
                                                       (swap! *state merge (:report (r/generate-report config-name)))
                                                       )}
                                         {:fx/type :button
                                          :text "EXIT"
                                          :on-action (fn [_] (System/exit 0))
                                          }
                                         ]
                              }

                             ]}}})

(defn -main [& args]
  (let [renderer (fx/create-renderer
                  :middleware (fx/wrap-map-desc #(root-view %))
                  :opts {:fx.opt/map-event-handler
                         (-> handle
                             (fx/wrap-co-effects {:report (fx/make-deref-co-effect *state)})
                             (fx/wrap-effects {:report (fx/make-reset-effect *state)
                                               :dispatch fx/dispatch-effect}))})]
    (fx/mount-renderer *state renderer)))


