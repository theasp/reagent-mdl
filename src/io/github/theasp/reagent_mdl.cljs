(ns io.github.theasp.reagent-mdl
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [cljsjs.material :as material]
   [cljsjs.dialog-polyfill :as dialog]))

(defn join [& args]
  (str/join " " (remove nil? (flatten args))))

(defn with-content [v content]
  (into [] (concat v content)))

(defn extract-props [args & [prop-list]]
  (if (map? (first args))
    [(select-keys (first args) prop-list)
     (reduce #(dissoc %1 %2) (first args) prop-list)
     (rest args)]
    [{} {} args]))

(defn upgrade
  "Returns a reagent class that upgrades it's child element"
  [child]
  (r/create-class
   {:display-name "mdl-upgrade"
    :component-did-mount
    (fn [node]
      (.upgradeElements js/componentHandler (r/dom-node node)))

    :component-will-unmount
    (fn [node]
      (.downgradeElements js/componentHandler (r/dom-node node)))

    :reagent-render
    (fn [child] child)}))

(defn mdl-element [element base-class content & [upgrade? prop-to-class prop-list]]
  (let [prop-list
        (or prop-list (keys prop-to-class))

        [props other-props content]
        (extract-props content (conj prop-list :class))

        class
        (join (:class props)
              (map #(when-let [value (get props %)]
                      (prop-to-class % value))
                   (keys (dissoc props :class)))
              base-class)

        props
        (assoc other-props :class class)

        element
        (into [element props]
              content)]
    (if upgrade?
      [upgrade element]
      element)))

(def icon-props [:class])

(defn icon [& content]
  (mdl-element :i "material-icons" content))

(defn set-textfield-state [node {:keys [disabled? value]}]
  (let [dom-node (r/dom-node node)]
    (if disabled?
      (.MaterialTextfield.disable dom-node)
      (.MaterialTextfield.enable dom-node))

    (.MaterialTextfield.change dom-node value)))

(defn textfield-state
  "Returns a reagent class that corrects the state of a checkbox"
  [props child]
  (r/create-class
   {:display-name "textfield-state"
    :component-will-update
    (fn [node [_ props]] (set-textfield-state node props))

    :component-did-mount
    (fn [node] (set-textfield-state node props))

    :reagent-render
    (fn [child] child)}))

(def textfield-props
  [:disabled? :error :expandable? :expandable-icon :floating-label?
   :id? :invalid? :input-class :label :max-rows :on-change :pattern
   :required? :rows :style :value :cursor])

(defn textfield [& content]
  (let [[props other-props content]
        (extract-props content textfield-props)

        {:keys [disabled? error expandable? expandable-icon floating-label?
                id invalid? input-class label max-rows on-change pattern
                required? rows style value cursor]}
        props


        label           (or label content)

        id              (or id
                            (when (string? label)
                              (str/replace label #"[^a-zA-Z0-9]" "")))

        value           (if value
                          (if (fn? value)
                            (value)
                            value)
                          (when (some? cursor)
                            @cursor))

        on-change       (or on-change
                            (when (some? cursor)
                              #(reset! cursor (.-target.value %))))

        input-props     (merge {:class     (join "mdl-textfield__input"
                                                 input-class)
                                :id        id
                                :rows      rows
                                :max-rows  max-rows
                                :on-change on-change}
                               other-props)

        input           [(if rows :textarea :input) input-props]
        label           [:label.mdl-textfield__label {:for id} label]
        error           (when error [:span.mdl-textfield__error error])

        container-props {:style style
                         :class (join "mdl-textfield mdl-js-textfield"
                                      [(when expandable?
                                         "mdl-textfield--expandable")
                                       (when floating-label?
                                         "mdl-textfield--floating-label")])}]
    [textfield-state {:value     value
                      :disabled? disabled?}
     [upgrade
      (if expandable?
        [:div container-props
         [:label.mdl-button.mdl-js-button.mdl-button--icon
          icon
          expandable-icon]
         [:div.mdl-textfield__expandable-holder
          input
          label
          error]]
        [:div container-props
         input
         label
         error])]]))


(defn set-checkbox-state [node props]
  (if (:checked? props)
    (.MaterialCheckbox.check (r/dom-node node))
    (.MaterialCheckbox.uncheck (r/dom-node node)))

  (if (:disabled? props)
    (.MaterialCheckbox.disable (r/dom-node node))
    (.MaterialCheckbox.enable (r/dom-node node))))

(defn checkbox-state
  "Returns a reagent class that corrects the state of a checkbox"
  [props child]
  (r/create-class
   {:display-name "checkbox-state"
    :component-will-update
    (fn [node [_ props]] (set-checkbox-state node props))

    :component-did-mount
    (fn [node] (set-checkbox-state node props))

    :reagent-render
    (fn [child] child)}))

(def checkbox-props
  [:checked? :disabled? :label :on-change :ripple? :cursor])

(defn checkbox [& content]
  (let [[props other-props content]
        (extract-props content textfield-props)

        {:keys [checked? disabled? label on-change ripple? cursor]}
        props

        label-props {:class (join "mdl-checkbox mdl-js-checkbox"
                                  [(when ripple? "mdl-js-ripple-effect")])}

        on-change   (or on-change
                        (when (some? cursor)
                          #(reset! cursor
                                   (.-target.checked %))))

        input-props (merge {:class     "mdl-checkbox__input"
                            :type      :checkbox
                            :on-change on-change}
                           other-props)]

    [checkbox-state {:disabled? disabled?
                     :checked?  (if (some? checked?)
                                  checked?
                                  (when (some? cursor)
                                    (if @cursor true false)))}
     [upgrade
      [:label label-props
       [:input input-props]
       (when-let [label (or label content)]
         [:span.mdl-checkbox__label label])]]]))

(def button-prop-list
  [:type :colored? :primary? :accent? :ripple?])

(defn button-prop-to-class [k v]
  (condp = k
    :type     (condp = v
                :raised   "mdl-button--raised"
                :fab      "mdl-button--fab"
                :mini-fab "mdl-button--mini-fab"
                :icon     "mdl-button--icon"
                :flat     nil
                nil)
    :colored? "mdl-button--colored"
    :primary? "mdl-button--primary"
    :accent?  "mdl-button--accent"
    :ripple?  "mdl-js-ripple-effect"))

(defn button [& content]
  (mdl-element :button "mdl-button mdl-js-button" content true
               button-prop-to-class button-prop-list))

(def grid-prop-to-class
  {:no-spacing? "mdl-grid--no-spacing"})

(defn grid [& content]
  (mdl-element :div "mdl-grid" content false grid-prop-to-class))

(def cell-prop-list
  [:size :size-desktop :size-tablet :size-phone
   :offset :offset-desktop :offset-tablet :offset-phone
   :order :order-desktop :order-tablet :order-phone
   :hide? :hide-desktop? :hide-tablet? :hide-phone?
   :stretch? :top? :middle? :bottom? :class])

(defn cell-prop-to-class [k v]
  (condp = k
    :size           (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col"))
    :size-desktop   (when (and (integer? v) (<= 1 v 8))
                      (str "mdl-cell--" v "-col-desktop"))
    :size-tablet    (when (and (integer? v) (<= 1 v 6))
                      (str "mdl-cell--" v "-col-tablet"))
    :size-phone     (when (and (integer? v) (<= 1 v 4))
                      (str "mdl-cell--" v "-col-phone"))
    :offset         (when (and (integer? v) (<= 1 v 11))
                      (str "mdl-cell--" v "-col-offset"))
    :offset-desktop (when (and (integer? v) (<= 1 v 7))
                      (str "mdl-cell--" v "-col-offset-desktop"))
    :offset-tablet  (when (and (integer? v) (<= 1 v 5))
                      (str "mdl-cell--" v "-col-offset-tablet"))
    :offset-phone   (when (and (integer? v) (<= 1 v 3))
                      (str "mdl-cell--" v "-col-offset-phone"))
    :order          (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col-order"))
    :order-desktop  (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col-order-desktop"))
    :order-tablet   (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col-order-tablet"))
    :order-phone    (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col-order-phone"))
    :hide?          "mdl-cell--hide"
    :hide-desktop?  "mdl-cell--hide-desktop"
    :hide-tablet?   "mdl-cell--hide-tablet"
    :hide-phone?    "mdl-cell--hide-phone"
    :stretch?       "mdl-cell--stretch"
    :top?           "mdl-cell--top"
    :middle?        "mdl-cell--middle"
    :bottom?        "mdl-cell--bottom"))

(defn cell [& content]
  (mdl-element :div "mdl-cell" content false cell-prop-to-class cell-prop-list))


(def layout-props
  {:fixed-drawer?             "mdl-layout--fixed-drawer"
   :fixed-header?             "mdl-layout--fixed-header"
   :fixed-tabs?               "mdl-layout--fixed-tabs"
   :no-drawer-button?         "mdl-layout--no-drawer-button"
   :no-desktop-drawer-button? "mdl-layout--no-desktop-drawer-button"})
(defn layout [& content]
  (mdl-element :div "mdl-layout mdl-js-layout" content true layout-props))

(defn navigation [& content]
  (mdl-element :nav "mdl-navigation" content))

(defn navigation-link [& content]
  (mdl-element :a "mdl-navigation__link" content))

(defn layout-drawer [& content]
  (mdl-element :div "mdl-layout__drawer" content))

(defn layout-title [& content]
  (mdl-element :div "mdl-layout-title" content))

(defn dialog [& content]
  (r/create-class
   {:display-name "mdl-dialog"

    :component-did-mount
    #(let [dialog (r/dom-node %)]
       (when (nil? (.-showModal dialog))
         (.registerDialog js/dialogPolyfill dialog)))

    :reagent-render
    (fn [& content]
      (mdl-element :dialog "mdl-dialog" content))}))

(defn dialog-title [& content]
  (mdl-element :div "mdl-dialog__title" content))

(defn dialog-content [& content]
  (mdl-element :div "mdl-dialog__content" content))

(defn dialog-actions [& content]
  (mdl-element :div "mdl-dialog__actions" content))

(defn menu-item [& content]
  (mdl-element :li "mdl-menu__item" content))

(defn header-menu-items [menu-items]
  [upgrade
   (into     [:ul.mdl-menu.mdl-menu--bottom-right.mdl-js-menu.mdl-js-ripple-effect
              {:for "header-menu"}]
             menu-items)])

(defn header-menu [menu-items]
  [button {:type :icon
           :id   "header-menu"}
   [icon "more_vert"]])

(defn header [title {:keys [buttons menu]}]
  [:header.mdl-layout__header
   [:div.mdl-layout__header-row
    [:span.mdl-layout-title title]
    [:div.mdl-layout-spacer]
    (into [:nav.mdl-navigation]
          (concat
           buttons
           (when menu
             [[header-menu menu]
              [header-menu-items menu]])))]])

(defn layout-content [& content]
  (mdl-element :main "mdl-layout__content" content))

(defn footer []
  [:div])

(def page-props
  [:title :subtitle :dialogs :drawer :content :header :layout :footer])

(defn page [& content]
  (let [[props other-props content]
        (extract-props content page-props)

        {:keys [title subtitle dialogs]}
        props

        subtitle (or subtitle title)]
    (if (= title subtitle)
      (set! (.-title js/document) title)
      (set! (.-title js/document) (str title " - " subtitle)))

    [:div
     (when dialogs
       (into [:div] dialogs))
     [layout (assoc (:layout props)
                    :fixed-drawer? (some? (:drawer props)))
      (when (some? (:drawer props))
        (:drawer props))
      [header subtitle (:header props)]
      (into [layout-content] content)
      [footer (:footer footer)]]]))


(defn error-page [& content]
  (let [[_ props content] (extract-props content nil)]
    [page props
     [grid
      (into [cell {:size 12}]
            content)]]))
