(defproject ca.gt0.theasp/reagent-mdl "0.1.0-SNAPSHOT"
  :description "Reagent components for Material Design Lite"
  :url "http://github.com/theasp/reagent-mdl"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.494"]
                 [cljsjs/material "1.3.0-0"]
                 [cljsjs/dialog-polyfill "0.4.3-0"]
                 [com.taoensso/timbre "4.8.0"]
                 [reagent "0.6.0"]
                 [doo "0.1.7"]]

  :plugins [[lein-codox "0.10.2"]
            [lein-pprint "1.1.2"]
            [lein-cljsbuild "1.1.5"]
            [lein-doo "0.1.7"]
            [lein-npm "0.6.1"]]

  :hooks [leiningen.cljsbuild]

  :codox {:language :clojurescript}

  :doo {:build "reagent-mdl-test-browser"}

  :cljsbuild {:builds
              [{:id           "reagent-mdl"
                :source-paths ["src/cljs"]
                :jar          true
                :compiler
                {:output-dir    "target/js"
                 :output-to     "target/js/reagent_mdl.js"
                 :optimizations :whitespace
                 :pretty-print  false}}

               {:id           "reagent-mdl-test-browser"
                :source-paths ["src/cljs" "test/cljs"]
                :compiler
                {:output-dir    "target/js-test/out"
                 :output-to     "target/js-test/reagent_mdl.js"
                 :source-map    "target/js-test/reagent_mdl.js.map"
                 :main          ca.gt0.theasp.reagent-mdl.test-runner
                 :optimizations :whitespace
                 :pretty-print  false}}]}

  :prep-tasks ["compile" ["cljsbuild" "once"]]

  :profiles  {:simple
              {:shared
               {:cljsbuild
                {:builds
                 [{:id       "reagent-mdl"
                   :compiler {:optimizations :simple
                              :pretty-print  false}}]}}}
              :advanced
              {:shared
               {:cljsbuild
                {:builds
                 [{:id       "reagent-mdl"
                   :compiler {:optimizations :advanced
                              :pretty-print  false}}]}}}}

  :deploy-repositories [["clojars" {:username :env/clojars_username
                                    :password :env/clojars_password}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :aliases  {"test" ["doo" "phantom" "reagent-mdl-test-browser" "once"]})
