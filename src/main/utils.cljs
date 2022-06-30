(ns main.utils)

(defn add-class [elem class-name]
  (some-> elem .-classList (.add class-name))
  elem)

(defn remove-class [elem class-name]
  (some-> elem .-classList (.remove class-name))
  elem)
