(ns leiningen.circle
  (:require [leiningen
             [core.eval :as eval]
             [release :as release]
             [deploy :as deploy]]))

(defn env [s]
  (System/getenv s))

(defn circle [project & args]
  (let [branch (env "CIRCLE_BRANCH")]
    (condp re-find branch
      #"master"
      (deploy/deploy project "clojars")

      #"(?i)release"
      (do
        (eval/sh "git" "reset" "--hard" "origin/master")
        (release/release project)
        (eval/sh "git" "push" "origin" "--delete" branch)))))
