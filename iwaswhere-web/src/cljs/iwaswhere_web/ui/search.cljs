(ns iwaswhere-web.ui.search
  (:require [iwaswhere-web.helpers :as h]
            [iwaswhere-web.utils.misc :as u]
            [iwaswhere-web.utils.parse :as p]
            [matthiasn.systems-toolbox-ui.reagent :as r]
            [clojure.string :as s]
            [clojure.set :as set]))

(defn search-view
  "Renders search component."
  [{:keys [observed local put-fn]}]
  (let [local-snapshot @local
        store-snapshot @observed
        show-pvt? (:show-pvt (:cfg store-snapshot))
        hashtags (:hashtags (:cfg store-snapshot))
        pvt-hashtags (:pvt-hashtags (:cfg store-snapshot))
        hashtags (if show-pvt? (set/union hashtags pvt-hashtags) hashtags)
        mentions (:mentions (:cfg store-snapshot))
        on-input-fn #(let [search (p/parse-search (.. % -target -innerText))]
                      (put-fn [:search/update search])
                      (put-fn [:stats/count-words search]))

        ; find incomplete tag or mention before cursor, show suggestions
        before-cursor (h/string-before-cursor (:search-text (:current-query @observed)))
        [curr-tag f-tags] (p/autocomplete-tags before-cursor "#" hashtags)
        [curr-mention f-mentions] (p/autocomplete-tags before-cursor "@" mentions)

        tag-replace-fn
        (fn [curr-tag tag]
          (let [curr-tag-regex (js/RegExp (str curr-tag "(?!" p/tag-char-cls ")") "i")
                search-text (:search-text (:current-query @observed))
                new-search (p/parse-search (s/replace search-text curr-tag-regex tag))]
            (swap! local assoc-in [:current-query] new-search)
            (put-fn [:search/update new-search])))
        get-tags #(% (:current-query @local))

        on-keydown-fn
        (fn [ev]
          (let [key-code (.. ev -keyCode)]
            (when (= key-code 9)          ; TAB key pressed
              (when (and curr-tag (seq f-tags))
                (tag-replace-fn curr-tag (first f-tags)))
              (when (and curr-mention (seq f-mentions))
                (tag-replace-fn curr-mention (first f-mentions)))
              (.setTimeout js/window (fn [] (h/focus-on-end (.-target ev))) 50)
              (.preventDefault ev))))]
    [:div.search
     [:div.hashtags
      (for [tag (get-tags :tags)]
        ^{:key (str "search-" tag)} [:span.hashtag tag])
      (for [tag (get-tags :not-tags)]
        ^{:key (str "search-n" tag)} [:span.hashtag.not-tag tag])
      (for [tag (get-tags :mentions)]
        ^{:key (str "search-" tag)} [:span.mention tag])]
     [:div.search-field {:content-editable true
                         :on-input         on-input-fn
                         :on-key-down      on-keydown-fn}
      (:search-text (:current-query local-snapshot))]
     [u/suggestions "search" f-tags curr-tag tag-replace-fn "hashtag"]
     [u/suggestions "search" f-mentions curr-mention tag-replace-fn "mention"]]))

(defn init-fn
  "Initializes listener for location hash changes, which alters local component state with
  the latest query on change, plus sends query to backend."
  [{:keys [local observed put-fn]}]
  (let [hash-change-fn #(let [new-search (h/query-from-search-hash put-fn)]
                         (when (not= new-search (:current-query @observed))
                           (swap! local assoc-in [:current-query] new-search)
                           (put-fn [:search/update new-search])))]
    (aset js/window "onhashchange" hash-change-fn)
    (hash-change-fn)))

(defn cmp-map
  [cmp-id]
  (r/cmp-map {:cmp-id  cmp-id
              :init-fn init-fn
              :view-fn search-view
              :dom-id  "search"}))
