(ns iwaswhere-web.index
  "This namespace takes care of rendering the static HTML into which the
   React / Reagent components are mounted on the client side at runtime."
  (:require [hiccup.core :refer [html]]
            [compojure.route :as r]
            [iwaswhere-web.upload-qr :as qr]
            [iwaswhere-web.files :as f]
            [iwaswhere-web.img-route :as ir]))

(defn stylesheet [href] [:link {:href href :rel "stylesheet"}])
(defn script [src] [:script {:src src}])

(defn index-page
  "Generates index page HTML with the specified page title."
  [_]
  (html
    [:html
     {:lang "en"}
     [:head
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "iWasWhere"]
      ; Download from https://github.com/christiannaths/Redacted-Font
      ; then uncomment in _entry.scss and recompile CSS for redacted fron
      #_(stylesheet "/redacted-font/fonts/web/stylesheet.css")

      (stylesheet "/webjars/normalize-css/4.1.1/normalize.css")
      (stylesheet "/webjars/github-com-mrkelly-lato/0.3.0/css/lato.css")
      (stylesheet "/webjars/fontawesome/4.6.3/css/font-awesome.css")
      (stylesheet "/webjars/leaflet/0.7.7/dist/leaflet.css")
      (stylesheet "/css/iwaswhere.css")]

     [:body
      [:div.flex-container
       [:div#header]
       [:div#search]
       [:div#journal]
       [:div#word-cloud]
       [:div#stats]]
      ;; Currently, from http://www.orangefreesounds.com/old-clock-ringing-short/
      ;; TODO: record own alarm clock
      [:audio#ringer {:autoPlay false :loop false}
       [:source {:src "/mp3/old-clock-ringing-short.mp3" :type "audio/mp4"}]]
      [:audio#ticking-clock {:autoPlay false :loop false}
       [:source {:src "/mp3/tick.ogg" :type "audio/ogg"}]]

      (script "/webjars/d3/3.5.17/d3.js")
      (script "/webjars/d3-cloud/1.2.1/build/d3.layout.cloud.js")
      (script "/js/wordcloud.js")
      (script "/js/build/iwaswhere.js")]]))

(defn routes-fn
  "Adds routes for serving media files. This routes function will receive the
   put-fn of the ws-cmp, which is not used here but can be useful in scenarios
   when requests are supposed to be handled by a another component."
  [_put-fn]
  [(r/files "/photos" {:root (str f/data-path "/images/")})
   (r/files "/audio" {:root (str f/data-path "/audio/")})
   (r/files "/videos" {:root (str f/data-path "/videos/")})
   qr/address-qr-route
   ir/img-resized-route])

(def sente-map
  "Configuration map for sente-cmp."
  {:index-page-fn index-page
   :routes-fn     routes-fn
   :relay-types   #{:cmd/keep-alive-res :entry/saved :state/new
                    :stats/pomo-day :stats/activity-day :stats/tasks-day
                    :stats/wordcounts :state/stats-tags}})
