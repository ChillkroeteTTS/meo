(ns iwaswhere-electron.update.core
  (:require [iwaswhere-electron.update.log :as log]
            [iwaswhere-electron.update.ipc :as ipc]
            [iwaswhere-electron.update.ui :as ui]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :updater/switchboard))


(defn start []
  (log/info "Starting SYSTEM")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :updater/ipc-cmp #{:update/check})
                       (ui/cmp-map :updater/ui-cmp)}]

     [:cmd/route {:from :updater/ipc-cmp
                  :to   #{:updater/ui-cmp}}]

     [:cmd/route {:from :updater/ui-cmp
                  :to   #{:updater/ipc-cmp}}]]))

(defn load-handler [ev]
  (log/info "UPDATER CORE loaded")
  (let []
    (start)))


(.addEventListener js/window "load" load-handler)
