(ns iwaswhere-electron.renderer.core
  (:require [iwaswhere-electron.renderer.log]
            [iwaswhere-web.client-store :as store]
            [iwaswhere-web.ui.re-frame :as rf]
            [iwaswhere-web.router :as router]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [matthiasn.systems-toolbox-sente.client :as sente]
            [iwaswhere-electron.renderer.exec :as exec]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]))

(defonce switchboard (sb/component :renderer/switchboard))

(defn console-msg-handler [ev]
  (info "GUEST:" (.-message ev)))

(def sente-cfg {:relay-types #{:entry/update :entry/find :entry/trash
                               :import/geo :import/photos :import/phone
                               :import/spotify :import/flight :export/pdf
                               :stats/pomo-day-get :import/screenshot
                               :stats/get :stats/get2 :import/movie :blink/busy
                               :state/stats-tags-get :import/weight :import/listen
                               :state/search :cfg/refresh :firehose/cmp-recv
                               :firehose/cmp-put}
                :sente-opts  {:host     "localhost:8765"
                              :protocol "http:"}})

(def OBSERVER true)

(defn make-observable
  [components]
  (if OBSERVER
    (let [mapper #(assoc-in % [:opts :msgs-on-firehose] true)]
      (set (mapv mapper components)))
    components))

(defn start []
  (info "Starting SYSTEM")
  (let [components #{(ipc/cmp-map :renderer/ipc-cmp #{:app/open-external})
                     (sente/cmp-map :renderer/ws-cmp sente-cfg)
                     (store/cmp-map :renderer/store-cmp)
                     (router/cmp-map :renderer/router-cmp)
                     (rf/cmp-map :renderer/ui-cmp)
                     (sched/cmp-map :renderer/scheduler-cmp)
                     (exec/cmp-map :renderer/exec-cmp #{})}
        components (make-observable components)]
    (sb/send-mult-cmd
      switchboard
      [[:cmd/init-comp components]
       [:cmd/route {:from :renderer/ipc-cmp
                    :to   #{:renderer/exec-cmp
                            :renderer/store-cmp
                            :renderer/ws-cmp}}]

       [:cmd/route {:from :renderer/router-cmp :to :renderer/store-cmp}]
       [:cmd/route {:from :renderer/store-cmp :to :renderer/router-cmp}]

       [:cmd/route {:from #{:renderer/ui-cmp
                            :renderer/store-cmp}
                    :to   #{:renderer/ws-cmp
                            :renderer/scheduler-cmp
                            :renderer/ipc-cmp}}]

       [:cmd/route {:from #{:renderer/ui-cmp
                            :renderer/ws-cmp}
                    :to   #{:renderer/store-cmp
                            :renderer/ipc-cmp}}]

       [:cmd/observe-state {:from :renderer/store-cmp
                            :to   :renderer/ui-cmp}]

       [:cmd/route {:from :renderer/scheduler-cmp
                    :to   #{:renderer/store-cmp
                            :renderer/ws-cmp}}]

       (when OBSERVER
         [:cmd/attach-to-firehose :renderer/ws-cmp])

       [:cmd/send {:to  :renderer/exec-cmp
                   :msg [:exec/js {:js "iwaswhere_web.ui.menu.hide()"}]}]])))

(defn load-handler [ev]
  (info "RENDERER loaded")
  (let [webview (.querySelector js/document "webview")]
    (start)))

(.addEventListener js/window "load" load-handler)
