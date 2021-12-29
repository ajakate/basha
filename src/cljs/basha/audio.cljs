(ns basha.audio
  (:require
   [re-frame.core :as rf]))

(def record (.querySelector js/document ".record-start"))

(def stopper (.querySelector js/document ".record-stop"))

(defn init-audio []
  (if (.. js/navigator -mediaDevices -getUserMedia)
    (do
      (.log js/console "getUserMedia supported.")
      (->
       (-> (.getUserMedia (.-mediaDevices js/navigator) #js {:audio true}))
       (.then
        (fn [stream]
          (let [mediaRecorder (new js/MediaRecorder stream)]
            (set!
             (.-onclick record)
             (fn [] (.start mediaRecorder) (.log js/console "begin")))
            (def chunks #js [])
            (set!
             (.-ondataavailable mediaRecorder)
             (fn [e] (.push chunks (.-data e))))
            (set!
             (.-onclick stopper)
             (fn [] (.stop mediaRecorder) (.log js/console "end")))
            (set!
             (.-onstop mediaRecorder)
             (fn [e]
               (.log js/console "recorder stopped")
               (def blob
                 (new js/Blob chunks #js {:type "audio/ogg; codecs=opus"}))
               (set! chunks #js [])
               (let [audioURL (.createObjectURL (.-URL js/window) blob)]
                 (rf/dispatch [:set-temp-recording audioURL])
                 (.log js/console "done blob")
                 (.log js/console audioURL)))))))
       (.catch
        (fn [err]
          (.log
           js/console
           (str "The following getUserMedia error occurred: " err))))))
    (.log js/console "getUserMedia not supported on your browser!")))

(defn start-audio []
  (.click record))

(defn stop-audio []
  (.click stopper))
