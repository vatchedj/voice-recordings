(ns voice-recordings.prod
  (:require [voice-recordings.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
