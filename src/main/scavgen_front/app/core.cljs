(ns scavgen-front.app.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.dom :as rdom]
            [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defonce health (r/atom false))

(defn build-url [path]
  (str "http://scavgen.com/api" path))

(defn healthy? []
  @health)

(defn check-health []
  (go (let [response (<! (http/get (build-url "/health")
                                   {:with-credentials? false}))]
        (reset! health (:success response)))))

(defn simple-scav []
  (let [scav-state (r/atom {})]
    (fn []
      (let [scav-button [:button
                         {:on-click (fn []
                                      (go (let [response (<! (http/get (build-url "/single") {:with-credentials? false}))]
                                            (reset! health (:success response))
                                            (when (= (:status response) 200)
                                              (reset! scav-state (:scavenger (:body response)))))))} "Generate scav!"]]
      (if (empty? @scav-state)
        [:div
         [:p "Try generate new scavenger!"]
         scav-button]
        [:div
         (let [scavenger @scav-state
               scav-info (str (:fullName scavenger) " " (:rarity scavenger))]
           [:p scav-info])
         scav-button])))))

(defn scav-team []
  (let [team-state (r/atom [])]
    (fn []
      (let [scav-button [:button
                         {:on-click (fn []
                                      (go (let [response (<! (http/get (build-url "/team") {:with-credentials? false}))]
                                            (reset! health (:success response))
                                            (when (= (:status response) 200)
                                              (reset! team-state (:scavengers (:body response)))))))} "Generate team!"]]
        (if (empty? @team-state)
          [:div
           [:p "Try generate new team!"]
           scav-button]
          (let [scavengers @team-state
                scavs-info (reduce (fn [acc s] (conj acc [:p (str (:fullName s) " " (:rarity s))])) [:div] scavengers)]
            [:div
             scavs-info
             scav-button]))))))

(defn app []
  [:div
   (if (healthy?)
     [:div
      [simple-scav]
      [scav-team]]
     [:div [:p "Service temporary unavailable ¯\\_(ツ)_/¯"]
      [:button {:on-click check-health} "Ping service!"]])])

(defn render []
  (rdom/render [app] (.getElementById js/document "root")))

(defn ^:export main []
  (check-health)
  (render))

(defn ^:dev/after-load reload! []
  (render))
