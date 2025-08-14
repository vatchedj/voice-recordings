(ns voice-recordings.util)

(defn valid-phone-number
  "Returns a valid phone number, or nil if invalid."
  [phone-number]
  (let [phone-regex #"^\d{3}-\d{3}-\d{4}$"]
    (cond
      (empty? phone-number) nil
      (not (re-matches phone-regex phone-number)) nil
      :else phone-number)))
