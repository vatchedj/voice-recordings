(ns voice-recordings.util
  (:require
    [clojure.string :as string]))

(def valid-phone-regexes
  [#"^\d{10}$"
   #"^\d{3}-\d{3}-\d{4}$"
   #"^\(\d{3}\)\s?\d{3}-\d{4}$"])

(defn- remove-non-digits
  "Takes a numeric phone number string and
  removes all the non-numeric characters."
  [number-str]
  (string/replace number-str #"[^\d]" ""))

(defn is-valid-phone-number?
  [phone-number]
  (->> valid-phone-regexes
       (map #(re-matches % phone-number))
       (not-every? nil?)))

(defn valid-phone-number
  "Returns a valid, digits-only phone number, or nil if invalid."
  [phone-number]
  (when (is-valid-phone-number? phone-number)
    (remove-non-digits phone-number)))
