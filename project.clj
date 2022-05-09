(defproject test-net-connection "0.1.0-SNAPSHOT"
  :description "Network test tool to ping that devices are avaialable then runs specified loop-back tests"
  :java-source-paths ["java-src"]
  :license {:name "Unlicense"
            :url "https://unlicense.org/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [aleph/aleph "0.4.7-alpha10"]
                 [gloss/gloss "0.2.6"]
                 [manifold/manifold "0.2.3"]
                 [cljfx/cljfx "1.7.19"]
                 [org.openjfx/javafx-base "18"]
                 [org.openjfx/javafx-base "18" :classifier "linux"]
                 [org.openjfx/javafx-base "18" :classifier "mac"]
                 [org.openjfx/javafx-base "18" :classifier "win"]
                 [org.openjfx/javafx-controls "18"]
                 [org.openjfx/javafx-controls "18" :classifier "linux"]
                 [org.openjfx/javafx-controls "18" :classifier "mac"]
                 [org.openjfx/javafx-controls "18" :classifier "win"]
                 [org.openjfx/javafx-graphics "18"]
                 [org.openjfx/javafx-graphics "18" :classifier "linux"]
                 [org.openjfx/javafx-graphics "18" :classifier "mac"]
                 [org.openjfx/javafx-graphics "18" :classifier "win"]
                 [org.openjfx/javafx-media "18"]
                 [org.openjfx/javafx-media "18" :classifier "linux"]
                 [org.openjfx/javafx-media "18" :classifier "mac"]
                 [org.openjfx/javafx-media "18" :classifier "win"]
                 [org.openjfx/javafx-web "18"]
                 [org.openjfx/javafx-web "18" :classifier "linux"]
                 [org.openjfx/javafx-web "18" :classifier "mac"]
                 [org.openjfx/javafx-web "18" :classifier "win"]
                 [org.openjfx/javafx-fxml "18"]
                 [org.openjfx/javafx-fxml "18" :classifier "linux"]
                 [org.openjfx/javafx-fxml "18" :classifier "mac"]
                 [org.openjfx/javafx-fxml "18" :classifier "win"]
                 [org.openjfx/javafx-swing "18"]
                 [org.openjfx/javafx-swing "18" :classifier "linux"]
                 [org.openjfx/javafx-swing "18" :classifier "mac"]
                 [org.openjfx/javafx-swing "18" :classifier "win"]]
  :repl-options {:init-ns test-net-connection.core}
  :main test-net-connection.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :injections [(javafx.application.Platform/exit)]
                       :jvm-opts ["-Dcljfx.skip-javafx-initialization=true"]
                       }})
