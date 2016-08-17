(defproject matthiasn/iwaswhere-web "0.1.10"
  :description "Sample application built with systems-toolbox library"
  :url "https://github.com/matthiasn/systems-toolbox"
  :license {:name "GNU GENERAL PUBLIC LICENSE"
            :url  "http://www.gnu.org/licenses/gpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.216"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [com.taoensso/encore "2.75.0"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [hiccup "1.0.5"]
                 [clj-pid "0.1.2"]
                 [clj-time "0.12.0"]
                 [me.raynes/fs "1.4.6"]
                 [markdown-clj "0.9.89"]
                 [cheshire "5.6.3"]
                 [cljsjs/moment "2.10.6-4"]
                 [cljsjs/leaflet "0.7.7-4"]
                 [com.drewnoakes/metadata-extractor "2.9.1"]
                 [ubergraph "0.2.3"]
                 [camel-snake-kebab "0.4.0"]
                 [matthiasn/systems-toolbox "0.6.1-alpha4"]
                 [matthiasn/systems-toolbox-ui "0.6.1-alpha6"]
                 [matthiasn/systems-toolbox-sente "0.6.1-alpha5"]
                 [matthiasn/inspect-probe "0.6.1-alpha1"]
                 [org.clojure/data.priority-map "0.0.7"]
                 [clucy "0.4.0"]
                 [seesaw "1.4.5"]
                 [clj.qrgen "0.4.0"]
                 [image-resizer "0.1.9"]
                 [org.webjars.bower/d3 "3.5.17"]
                 [org.webjars.bower/d3-cloud "1.2.1"]
                 [org.webjars.bower/fontawesome "4.6.3"]
                 [org.webjars.bower/normalize-css "4.1.1"]
                 [org.webjars.bower/leaflet "0.7.7"]
                 [org.webjars.npm/github-com-mrkelly-lato "0.3.0"]
                 [alandipert/storage-atom "2.0.1"]]

  :source-paths ["src/cljc" "src/clj/"]

  :clean-targets ^{:protect false} ["resources/public/js/build/" "target/" "packages/"]
  :auto-clean false

  :main iwaswhere-web.core
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]

  :plugins [[lein-cljsbuild "1.1.3"
             :exclusions [org.apache.commons/commons-compress]]
            [lein-figwheel "0.5.4-7" :exclusions [org.clojure/clojure]]
            [lein-sassy "1.0.7"
             :exclusions [org.clojure/clojure org.codehaus.plexus/plexus-utils]]
            [com.jakemccrary/lein-test-refresh "0.16.0"]
            [test2junit "1.2.2"]
            [lein-doo "0.1.7"]
            [lein-codox "0.9.6" :exclusions [org.clojure/clojure]]]

  :sass {:src "src/scss/"
         :dst "resources/public/css/"}

  ;:global-vars {*assert* false}

  :figwheel {:server-port 3450
             :css-dirs    ["resources/public/css"]}

  :test-refresh {:notify-on-success false
                 :changes-only      false
                 :watch-dirs        ["src" "test"]}

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "release"]
                     ["sass" "once"] "uberjar"]}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljc" "src/cljs" "env/dev/cljs"]
     :figwheel     true
     :compiler     {:main          "iwaswhere-web.dev"
                    :asset-path    "js/build"
                    :optimizations :none
                    :output-dir    "resources/public/js/build/"
                    :output-to     "resources/public/js/build/iwaswhere.js"
                    :source-map    true}}

    {:id           "release"
     :source-paths ["src/cljc" "src/cljs"]
     :figwheel     true
     :compiler     {:main          "iwaswhere-web.core"
                    :asset-path    "js/build"
                    ;:elide-asserts true
                    :externs       ["externs/misc.js"
                                    "externs/leaflet.ext.js"]
                    :output-to     "resources/public/js/build/iwaswhere.js"
                    :optimizations :advanced}}
    {:id           "cljs-test"
     :source-paths ["src" "test"]
     :compiler     {:output-to     "out/testable.js"
                    :main          iwaswhere-web.runner
                    :optimizations :advanced}}]})
