(defproject ca.gt0.theasp/reagent-mdl "0.1.0-SNAPSHOT"
  :description "Reagent components for Material Design Lite"
  :url "http://github.com/theasp/reagent-mdl"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.34"]
                 [cljsjs/material "1.1.3-0"]
                 [cljsjs/dialog-polyfill "0.4.3-0"]
                 [com.taoensso/timbre "4.3.1"]
                 [reagent "0.5.1"]]

  :plugins [[lein-cljsbuild "1.1.1"]])
