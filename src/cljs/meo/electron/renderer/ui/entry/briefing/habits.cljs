(ns meo.electron.renderer.ui.entry.briefing.habits
  (:require [reagent.ratom :refer-macros [reaction]]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :refer-macros [info]]
            [meo.electron.renderer.ui.entry.utils :as eu]
            [meo.common.utils.parse :as up]
            [moment]))

(defn habit-sorter
  "Sorts habits."
  [x y]
  (let [c (compare (or (get-in x [:habit :priority]) :X)
                   (or (get-in y [:habit :priority]) :X))]
    (if (not= c 0) c (compare (get-in y [:habit :points])
                              (get-in x [:habit :points])))))

(defn habit-line [_entry _tab-group _put-fn]
  (let [query-cfg (subscribe [:query-cfg])
        query-id-left (reaction (get-in @query-cfg [:tab-groups :left :active]))
        search-text (reaction (get-in @query-cfg [:queries @query-id-left :search-text]))]
    (fn habit-line-render [entry tab-group put-fn]
      (let [ts (:timestamp entry)
            text (eu/first-line entry)]
        [:tr {:key      ts
              :on-click (up/add-search ts tab-group put-fn)
              :class    (when (= (str ts) search-text) "selected")}
         [:td.award-points
          (when-let [points (-> entry :habit :points)]
            points)]
         #_
         [:td.award-points
          (when-let [penalty (-> entry :habit :penalty)]
            penalty)]
         [:td.habit text]]))))


(defn waiting-habits
  "Renders table with open entries, such as started tasks and open habits."
  [local _put-fn]
  (let [backend-cfg (subscribe [:backend-cfg])
        gql-res (subscribe [:gql-res])
        briefing (reaction (-> @gql-res :briefing :data :briefing))
        habits (reaction (-> @gql-res :waiting-habits :data :waiting_habits))
        expand-fn #(swap! local update-in [:expanded-habits] not)
        saga-filter (fn [entry]
                      (if (seq (:selected-set @local))
                        (let [saga (get-in entry [:story :saga :timestamp])]
                          (contains? (:selected-set @local) saga))
                        true))
        habits (reaction (->> @habits
                              (filter saga-filter)
                              (sort habit-sorter)))]
    (fn waiting-habits-list-render [local put-fn]
      (let [habits @habits
            tab-group :briefing]
        (when (and (seq habits)
                   (contains? (:capabilities @backend-cfg) :habits))
          [:div.waiting-habits
           [:table.habits
            [:tbody
             [:tr {:on-click expand-fn}
              [:th [:span.fa.fa-diamond.award-points]]
              ;[:th [:span.fa.fa-diamond.penalty]]
              [:th "Stuff I said I'd do."]]
             (for [entry habits]
               ^{:key (:timestamp entry)}
               [habit-line entry tab-group put-fn])]]])))))
