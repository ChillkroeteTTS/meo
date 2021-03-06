(ns meo.electron.renderer.ui.entry.briefing.tasks
  (:require [matthiasn.systems-toolbox.component :as st]
            [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :refer-macros [info debug]]
            [meo.electron.renderer.ui.entry.utils :as eu]
            [meo.common.utils.parse :as up]
            [moment]
            [clojure.set :as set]))

(defn task-sorter [x y]
  (let [c1 (compare (get-in x [:task :done]) (get-in y [:task :done]))
        c2 (compare (or (get-in x [:task :priority]) :X)
                    (or (get-in y [:task :priority]) :X))
        c3 (compare (:timestamp x) (:timestamp y))]
    (if (not= c1 0) c1 (if (not= c2 0) c2 c3))))

(defn m-to-hhmm
  [minutes]
  (let [dur (.duration moment minutes "minutes")
        ms (.asMilliseconds dur)
        utc (.utc moment ms)
        fmt (.format utc "HH:mm")]
    fmt))

(defn s-to-hhmmss [seconds]
  (let [dur (.duration moment seconds "seconds")
        ms (.asMilliseconds dur)
        utc (.utc moment ms)
        fmt (.format utc "HH:mm:ss")]
    fmt))

(defn s-to-hhmm [seconds]
  (let [dur (.duration moment seconds "seconds")
        ms (.asMilliseconds dur)
        utc (.utc moment ms)
        fmt (.format utc "HH:mm")]
    fmt))

(defn task-row [entry _put-fn _cfg]
  (let [ts (:timestamp entry)
        new-entries (subscribe [:new-entries])
        busy-status (subscribe [:busy-status])]
    (fn [entry put-fn {:keys [tab-group search-text unlink show-logged?]}]
      (let [text (eu/first-line entry)
            active (= ts (:active @busy-status))
            active-selected (and (= (str ts) search-text) active)
            busy (> 1000 (- (st/now) (:last @busy-status)))
            cls (cond
                  (and active-selected busy) "active-timer-selected-busy"
                  (and active busy) "active-timer-busy"
                  active-selected "active-timer-selected"
                  active "active-timer"
                  (= (str ts) search-text) "selected")
            estimate (get-in entry [:task :estimate_m] 0)
            logged-time (eu/logged-total new-entries entry)
            done (get-in entry [:task :done])
            closed (get-in entry [:task :closed])]
        [:tr.task {:on-click (up/add-search ts tab-group put-fn)
                   :class    cls}
         [:td
          (if (or done closed)
            [:span.checked
             (when done [:i.fas.fa-check])
             (when closed [:i.fas.fa-times])]
            (when-let [prio (some-> entry :task :priority (name))]
              [:span.prio {:class prio} prio]))]
         [:td.award-points
          (when-let [points (-> entry :task :points)]
            points)]
         [:td.estimate
          (let [seconds (* 60 estimate)]
            [:span {:class cls}
             (s-to-hhmm (.abs js/Math seconds))])]
         (when show-logged?
           [:td.estimate
            (let [actual (if (and active busy)
                           logged-time
                           (:completed_s (:task entry)))
                  seconds (* 60 estimate)
                  remaining (- seconds actual)
                  cls (when (neg? remaining) "neg")]
              [:span {:class cls}
               (s-to-hhmmss actual)])])
         [:td.text text]
         (when unlink
           [:td [:i.fa.far.fa-unlink {:on-click unlink}]])]))))

(defn task-row2 [entry _put-fn _cfg]
  (let [ts (:timestamp entry)]
    (fn [entry put-fn {:keys [tab-group search-text unlink show-logged?]}]
      (let [text (str (eu/first-line entry))
            cls (when (= (str ts) search-text) "selected")
            estimate (get-in entry [:task :estimate_m] 0)]
        [:tr.task {:on-click (up/add-search ts tab-group put-fn)
                   :class    cls}
         [:td (when-let [prio (some-> entry :task :priority (name))]
                [:span.prio {:class prio} prio])]
         [:td.award-points
          (when-let [points (-> entry :task :points)]
            points)]
         [:td.estimate
          (let [seconds (* 60 estimate)]
            [:span {:class cls}
             (s-to-hhmm (.abs js/Math seconds))])]
         [:td.text
          (subs text 0 50)]
         (when unlink
           [:td [:i.fa.far.fa-unlink {:on-click unlink}]])]))))

(defn started-tasks
  "Renders table with open entries, such as started tasks and open habits."
  [local local-cfg put-fn]
  (let [gql-res (subscribe [:gql-res])
        started-tasks (reaction (-> @gql-res :started-tasks :data :started_tasks))
        query-cfg (subscribe [:query-cfg])
        query-id-left (reaction (get-in @query-cfg [:tab-groups :left :active]))
        search-text (reaction (get-in @query-cfg [:queries @query-id-left :search-text]))
        on-hold-filter (fn [entry]
                         (let [on-hold (:on_hold (:task entry))]
                           (if (:on-hold @local)
                             on-hold
                             (not on-hold))))
        saga-filter (fn [entry]
                      (if (seq (:selected-set @local))
                        (let [saga (get-in entry [:story :saga :timestamp])]
                          (contains? (:selected-set @local) saga))
                        true))
        open-filter (fn [entry] (not (-> entry :task :done)))
        filter-btn (fn [fk]
                     [:span.filter {:class    (when (:on-hold @local) "current")
                                    :on-click #(swap! local update-in [:on_hold] not)}
                      (name fk)])
        entries-list (reaction (->> @started-tasks
                                    (filter on-hold-filter)
                                    (filter saga-filter)
                                    (filter open-filter)
                                    (sort task-sorter)))]
    (fn started-tasks-list-render [local local-cfg put-fn]
      (let [entries-list @entries-list
            tab-group (:tab-group local-cfg)
            search-text @search-text]
        (when (seq entries-list)
          [:div.started-tasks
           [:table.tasks
            [:tbody
             [:tr
              [:th.xs [:i.far.fa-exclamation-triangle]]
              [:th [:i.fa.far.fa-gem]]
              [:th [:i.fal.fa-bell]]
              [:th [:i.far.fa-stopwatch]]
              [:th
               [:div
                "Started Tasks"
                ;[filter-btn :on_hold]
                ]]]
             (for [entry entries-list]
               ^{:key (:timestamp entry)}
               [task-row entry put-fn {:tab-group    tab-group
                                       :search-text  search-text
                                       :show-logged? true}])]]])))))

(defn open-tasks
  "Renders table with open entries, such as started tasks and open habits."
  [local local-cfg put-fn]
  (let [gql-res (subscribe [:gql-res])
        open-tasks (reaction (-> @gql-res :open-tasks :data :open_tasks))
        started-tasks (reaction (->> @gql-res
                                     :started-tasks
                                     :data
                                     :started_tasks
                                     (map :timestamp)
                                     set))
        query-cfg (subscribe [:query-cfg])
        query-id-left (reaction (get-in @query-cfg [:tab-groups :left :active]))
        search-text (reaction (get-in @query-cfg [:queries @query-id-left :search-text]))
        on-hold-filter (fn [entry]
                         (let [on-hold (:on_hold (:task entry))]
                           (if (:on-hold @local)
                             on-hold
                             (not on-hold))))
        saga-filter (fn [entry]
                      (if (seq (:selected-set @local))
                        (let [saga (get-in entry [:story :saga :timestamp])]
                          (contains? (:selected-set @local) saga))
                        true))
        open-filter (fn [entry] (not (-> entry :task :done)))
        entries-list (reaction (->> @open-tasks
                                    (filter on-hold-filter)
                                    (filter saga-filter)
                                    (filter open-filter)))]
    (fn open-tasks-render [local local-cfg put-fn]
      (let [tab-group (:tab-group local-cfg)
            entries-list (filter #(not (contains? @started-tasks (:timestamp %))) @entries-list)]
        (when (seq entries-list)
          [:div.open-tasks
           [:table.tasks
            [:tbody
             [:tr
              [:th.xs [:i.far.fa-exclamation-triangle]]
              [:th [:i.fa.far.fa-gem]]
              [:th [:i.fal.fa-bell]]
              [:th "Open Tasks"]]
             (doall
               (for [entry entries-list]
                 ^{:key (:timestamp entry)}
                 [task-row2 entry put-fn {:tab-group    tab-group
                                          :search-text  @search-text
                                          :show-logged? true}]))]]])))))

(defn open-linked-tasks
  "Show open tasks that are also linked with the briefing entry."
  [ts local put-fn]
  (let [gql-res (subscribe [:gql-res])
        started-tasks (reaction (-> @gql-res :started-tasks :data :started_tasks))
        briefing (reaction (-> @gql-res :briefing :data :briefing))
        query-cfg (subscribe [:query-cfg])
        query-id-left (reaction (get-in @query-cfg [:tab-groups :left :active]))
        search-text (reaction (get-in @query-cfg [:queries @query-id-left :search-text]))
        linked-filters {:all    identity
                        :open   #(and (not (-> % :task :done))
                                      (not (-> % :task :closed)))
                        :done   #(-> % :task :done)
                        :closed #(-> % :task :closed)}
        filter-btn (fn [fk text]
                     [:span.filter {:class    (when (= fk (:filter @local)) "current")
                                    :on-click #(swap! local assoc-in [:filter] fk)}
                      (name fk) (when (= fk (:filter @local)) text)])]
    (fn open-linked-tasks-render [ts local local-cfg put-fn]
      (let [{:keys [tab-group]} local-cfg
            linked-entries (:linked @briefing)
            current-filter (get linked-filters (:filter @local))
            saga-filter (fn [entry]
                          (if (seq (:selected-set @local))
                            (let [saga (get-in entry [:story :saga :timestamp])]
                              (contains? (:selected-set @local) saga))
                            true))
            started-tasks (set (map :timestamp @started-tasks))
            task-filter #(contains? (set/union (:perm_tags %) (:tags %)) "#task")
            linked-tasks (->> linked-entries
                              (filter task-filter)
                              (filter current-filter)
                              (filter saga-filter)
                              (filter #(not (contains? started-tasks (:timestamp %))))
                              (sort task-sorter))
            unlink (fn [entry ts]
                     (let [rm-link #(disj (set %) ts)
                           upd (update-in entry [:linked-entries] rm-link)]
                       (put-fn [:entry/update upd])))
            search-text @search-text]
        (when (seq linked-entries)
          [:div.linked-tasks
           [filter-btn :all]
           [filter-btn :open]
           [filter-btn :done]
           [filter-btn :closed]
           [:table.tasks
            [:tbody
             [:tr
              [:th.xs [:i.far.fa-exclamation-triangle]]
              [:th [:i.fa.far.fa-gem]]
              [:th [:i.fa.far.fa-stopwatch]]
              [:th [:strong "Linked Tasks"]]
              [:th.xs [:i.fa.far.fa-link]]]
             (for [entry linked-tasks]
               ^{:key (:timestamp entry)}
               [task-row entry put-fn {:tab-group   tab-group
                                       :search-text search-text
                                       :unlink      unlink}])]]])))))
