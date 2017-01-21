(ns ca.gt0.theasp.reagent-mdl.test
  (:require-macros [cljs.test :as m :refer (deftest testing are is)])
  (:require [ca.gt0.theasp.reagent-mdl :as mdl]
            [cljs.test :as t]
            [taoensso.timbre :as timbre
             :refer-macros (tracef debugf infof warnf errorf)]))

(timbre/set-level! :trace) ; Uncomment for more logging
(enable-console-print!)
