(ns main.view-helper)

(defn add-class [elem class-name]
  (-> elem .-classList (.add class-name))
  elem)

(defn remove-class [elem class-name]
  (-> elem .-classList (.remove class-name))
  elem)
