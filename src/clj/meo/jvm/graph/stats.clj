(ns meo.jvm.graph.stats
  "Get stats from graph."
  (:require [ubergraph.core :as uber]
            [meo.jvm.graph.query :as gq]
            [meo.jvm.graph.stats.awards :as aw]
            [meo.jvm.graph.stats.questionnaires :as q]
            [meo.jvm.graph.stats.custom-fields :as cf]
            [meo.jvm.graph.stats.git :as g]
            [meo.common.utils.misc :as u]
            [taoensso.timbre :refer [info error warn]]
            [clojure.set :as set]
            [clj-pid.core :as pid]
            [matthiasn.systems-toolbox.component :as st]))

(defn media-mapper
  "Create mapper function for media stats"
  [current-state]
  (fn [d]
    (let [g (:graph current-state)
          date-string (:date_string d)
          day-nodes (gq/get-nodes-for-day g {:date_string date-string})
          day-nodes-attrs (map #(uber/attrs g %) day-nodes)
          day-stats {:date_string date-string
                     :photo-cnt   (count (filter :img_file day-nodes-attrs))
                     :audio-cnt   (count (filter :audio-file day-nodes-attrs))
                     :video-cnt   (count (filter :video-file day-nodes-attrs))}]
      [date-string day-stats])))

(defn res-count [state query]
  (let [res (gq/extract-sorted2 state (merge {:n Integer/MAX_VALUE} query))]
    (count (set res))))

(defn completed-count [current-state]
  (let [q1 {:tags #{"#task" "#done"} :n Integer/MAX_VALUE}
        q2 {:tags #{"#task"} :opts #{":done"} :n Integer/MAX_VALUE}
        res1 (set (gq/extract-sorted2 current-state q1))
        res2 (set (gq/extract-sorted2 current-state q2))]
    (count (set/union res1 res2))))

(def started-tasks
  {:tags     #{"#task"}
   :not-tags #{"#done" "#backlog" "#closed"}
   :opts     #{":started"}})

(def waiting-habits
  {:tags #{"#habit"}
   :opts #{":waiting"}})

(defn map-w-names [items ks]
  (into {} (map (fn [[ts st]]
                  [ts (select-keys st (set/union ks #{:timestamp}))])
                items)))

(defn make-stats-tags
  "Generate stats and tags from current-state."
  [state]
  {:hashtags      (gq/find-all-hashtags state)
   :pvt-hashtags  (gq/find-all-pvt-hashtags state)
   :pvt-displayed (:pvt-displayed (:cfg state))
   :mentions      (gq/find-all-mentions state)
   :stories       (map-w-names
                    (gq/find-all-stories state) #{:story_name :linked-saga})
   :sagas         (map-w-names (gq/find-all-sagas state) #{:saga-name})
   :cfg           (merge (:cfg state) {:pid (pid/current)})})

(defn count-words
  "Count total number of words."
  [current-state]
  (let [g (:graph current-state)
        counts (map #(u/count-words (uber/attrs g %))
                    (:sorted-entries current-state))]
    (apply + counts)))

(defn hours-logged
  "Count total hours logged."
  [current-state]
  (let [g (:graph current-state)
        entries (map #(uber/attrs g %) (:sorted-entries current-state))
        seconds-logged (map (fn [entry]
                              (let [completed (or (get entry :completed_time) 0)
                                    manual (gq/summed-durations entry)
                                    summed (+ completed manual)]
                                summed))
                            entries)
        total-seconds (apply + seconds-logged)
        total-hours (/ total-seconds 60 60)]
    total-hours))
