(ns main.service-worker)

(def cache-name "v1")

(def resources-to-cache
  #js ["/manifest.webmanifest"
       "/"
       "/?utm_source=pwa"
       "/index.html"
       "/js/main.js"
       "/img/icon-96x96.png"
       "/img/icon-128x128.png"
       "/img/icon-144x144.png"
       "/img/icon-152x152.png"
       "/img/icon-192x192.png"
       "/img/icon-256x256.png"
       "/img/icon-384x384.png"
       "/img/icon-512x512.png"
       "/img/bg.png"])

(defn add-to-cache [cache]
  (.addAll cache resources-to-cache))

(defn put-in-cache
  [cache request response]
  (js/console.info "Updating cache for" (.-url request))
  (.put cache request response))

(defn fetch-and-cache [request]
  (js/console.info "Fetching" (.-url request))
  (-> (js/fetch request)
      (.then (fn [response]
               (js/console.info "New response for" (.-url request))
               (-> (.open js/caches cache-name)
                   (.then (fn [cache]
                            (put-in-cache cache request (.clone response))
                            response)))))))

(defn cache-first-and-revalidate [request cache-response]
  (if cache-response
    (do
      (js/console.info "Using cache response for" (.-url request))
      (fetch-and-cache request)
      (js/Promise.resolve cache-response))
    (fetch-and-cache request)))

(defn delete-old-caches []
  (-> (.keys js/caches)
      (.then (fn [cache-keys]
               (let [old-caches-keys (remove #{cache-name} cache-keys)]
                 (js/console.info "Removing old cache keys" (clj->js old-caches-keys))
                 (->> old-caches-keys
                      (map #(.delete js/caches %))
                      (js/Promise.all)))))))

(.addEventListener
 js/self "install"
 (fn [event]
   (.skipWaiting js/self)
   (.waitUntil event
               (-> (.open js/caches cache-name)
                   (.then add-to-cache)))))

(.addEventListener
 js/self "activate"
 (fn [event]
   (.waitUntil event
               (delete-old-caches))))

(.addEventListener
 js/self "fetch"
 (fn [event]
   (let [request (.-request event)]
     (when (-> request .-method (= "GET"))
       (.respondWith event
                     (-> (.match js/caches request)
                         (.then (partial cache-first-and-revalidate request))))))))
