(ns ca.gt0.theasp.reagent-mdl
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [cljsjs.material :as material]
   [cljsjs.dialog-polyfill :as dialog]
   [taoensso.timbre :as timbre
    :refer-macros (tracef debugf infof warnf errorf)]))

(defn join [& args]
  (str/join " " (remove nil? (flatten args))))

(defn with-content [v content]
  (into [] (concat v content)))

(defn extract-props [args & [prop-list]]
  (if (map? (first args))
    [(select-keys (first args) prop-list)
     (reduce #(dissoc %1 %2) (first args) prop-list)
     (rest args)]
    [nil nil args]))

(defn upgrade-node [node]
  (.upgradeElements js/componentHandler (r/dom-node node)))

(defn downgrade-node [node]
  (.downgradeElements js/componentHandler (r/dom-node node)))

(defn upgrade
  "Returns a reagent class that upgrades it's child element"
  [child]
  (-> {:display-name           "mdl-upgrade"
       :component-did-mount    upgrade-node
       :component-will-unmount downgrade-node
       :reagent-render         identity}
      (r/create-class)))

(defn mdl-element [element base-class content & [upgrade? prop-to-class prop-list]]
  (let [prop-list
        (or prop-list (keys prop-to-class))

        [props other-props content]
        (extract-props content (conj prop-list :class))

        class
        (join (:class props)
              (map #(when-let [value (get props %)]
                      (if (map? prop-to-class)
                        (when value
                          (get prop-to-class %))
                        (prop-to-class % value)))
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

(defn textfield-set-invalid [dom-node error pattern]
  (when (and error
             (not pattern)
             (not (str/index-of (.-className dom-node) "is-invalid")))
    (set! (.-className dom-node) (str (.-className dom-node) " is-invalid"))))

(defn textfield-state
  "Returns a reagent class that corrects the state of a checkbox"
  [props child]
  (r/create-class
   {:display-name "textfield-state"
    :component-will-update
    (fn [node [_ {:keys [disabled? value error pattern] :as props}]]
      (let [dom-node (r/dom-node node)
            dirty?   (and (some? value) (not= "" value))]
        (if disabled?
          (.MaterialTextfield.disable dom-node)
          (.MaterialTextfield.enable dom-node))
        (.MaterialTextfield.change dom-node value)
        (.MaterialTextfield.checkValidity dom-node)
        (textfield-set-invalid dom-node error pattern)
        (when-not (str/index-of (.-className dom-node) "is-upgraded")
          (debugf "Upgrading textfield again...")
          (.upgradeElements js/componentHandler (r/dom-node node)))))

    :component-did-update
    (fn [node [_ {:keys [value error pattern disabled?] :as props}]])

    :component-did-mount
    (fn [node]
      (let [dom-node (r/dom-node node)]
        (.upgradeElements js/componentHandler dom-node)
        (.MaterialTextfield.change dom-node (:value props))
        (textfield-set-invalid dom-node (:error props) (:pattern props))))

    :component-will-unmount
    (fn [node]
      (.downgradeElements js/componentHandler (r/dom-node node)))

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
        input-props     (merge {:class     (join "mdl-textfield__input" input-class)
                                :id        id
                                :on-change on-change}
                               (when rows {:rows rows})
                               (when max-rows {:max-rows max-rows})
                               other-props)
        input           [(if rows :textarea :input) input-props]
        label           [:label.mdl-textfield__label {:for id} label]
        error           (when error [:span.mdl-textfield__error error])
        container-props {:style style
                         :class (join "mdl-textfield mdl-js-textfield"
                                      [(when expandable?
                                         "mdl-textfield--expandable")
                                       (when floating-label?
                                         "mdl-textfield--floating-label")
                                       (when (and error (not pattern))
                                         "is-invalid")])}]
    [textfield-state {:value     value
                      :disabled? disabled?
                      :error     error
                      :pattern   pattern}
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
        error])]))


(defn set-checkbox-state [node props]
  (if (:checked? props)
    (.MaterialCheckbox.check node)
    (.MaterialCheckbox.uncheck node))

  (if (:disabled? props)
    (.MaterialCheckbox.disable node)
    (.MaterialCheckbox.enable node)))

(defn checkbox-state
  "Returns a reagent class that corrects the state of a checkbox"
  [props child]
  (r/create-class
   {:display-name "checkbox-state"
    :component-will-update
    (fn [node [_ props]] (set-checkbox-state (r/dom-node node) props))

    :component-did-mount
    (fn [node] (set-checkbox-state (r/dom-node node) props))

    :reagent-render
    (fn [child] child)}))

(def checkbox-props
  [:checked? :disabled? :label :on-change :ripple? :cursor])

(defn checkbox [& content]
  (let [[props other-props content]
        (extract-props content checkbox-props)

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

(defn set-radio-state [node props]
  (if (:checked? props)
    (.MaterialRadio.check node)
    (.MaterialRadio.uncheck node))

  (if (:disabled? props)
    (.MaterialRadio.disable node)
    (.MaterialRadio.enable node)))


(defn radio-state
  "Returns a reagent class that corrects the state of a radio"
  [props child]
  (r/create-class
   {:display-name "radio-state"
    :component-will-update
    (fn [node [_ props]] (set-radio-state (r/dom-node node) props))

    :component-did-mount
    (fn [node] (set-radio-state (r/dom-node node) props))

    :reagent-render
    (fn [child] child)}))

(def radio-props
  [:value :checked? :disabled? :label :on-change :ripple? :cursor])

(defn radio [& content]
  (let [[props other-props content]
        (extract-props content radio-props)

        {:keys [value checked? disabled? label on-change ripple? cursor]}
        props

        label-props {:class (join "mdl-radio mdl-js-radio"
                                  [(when ripple? "mdl-js-ripple-effect")])}

        checked? (if (some? checked?)
                   checked?
                   (when (some? cursor)
                     (= @cursor value)))

        on-change (or on-change
                      (when (some? cursor)
                        #(when (.-target.checked %)
                           (reset! cursor value))))

        input-props (merge {:class     ""
                            :type      :radio
                            :value     value
                            :on-change on-change}
                           other-props)]

    [radio-state {:disabled? disabled?
                  :checked?  checked?}
     [upgrade
      [:label label-props
       [:input.mdl-radio__button input-props]
       (when-let [label (or label content)]
         [:span.mdl-radio__label label])]]]))

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
    :colored? (when v "mdl-button--colored")
    :primary? (when v "mdl-button--primary")
    :accent?  (when v "mdl-button--accent")
    :ripple?  (when v "mdl-js-ripple-effect")))

(defn button [& content]
  (mdl-element :button "mdl-button mdl-js-button" content true
               button-prop-to-class button-prop-list))

(defn link-button [& content]
  (mdl-element :a "mdl-button mdl-js-button" content true
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
    :size-desktop   (when (and (integer? v) (<= 1 v 12))
                      (str "mdl-cell--" v "-col-desktop"))
    :size-tablet    (when (and (integer? v) (<= 1 v 8))
                      (str "mdl-cell--" v "-col-tablet"))
    :size-phone     (when (and (integer? v) (<= 1 v 4))
                      (str "mdl-cell--" v "-col-phone"))
    :offset         (when (and (integer? v) (<= 1 v 11))
                      (str "mdl-cell--" v "-col-offset"))
    :offset-desktop (when (and (integer? v) (<= 1 v 11))
                      (str "mdl-cell--" v "-col-offset-desktop"))
    :offset-tablet  (when (and (integer? v) (<= 1 v 7))
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
    :hide?          (when v "mdl-cell--hide")
    :hide-desktop?  (when v "mdl-cell--hide-desktop")
    :hide-tablet?   (when v "mdl-cell--hide-tablet")
    :hide-phone?    (when v "mdl-cell--hide-phone")
    :stretch?       (when v "mdl-cell--stretch")
    :top?           (when v "mdl-cell--top")
    :middle?        (when v "mdl-cell--middle")
    :bottom?        (when v "mdl-cell--bottom")))

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

(defn card-prop-to-class [k v]
  (condp = k
    :shadow (when (and (integer? v) (<= 2 v 16))
              (str "mdl-shadow--" v "dp"))))

(def card-prop-list [:shadow])

(defn card [& content]
  (mdl-element :div "mdl-card" content false card-prop-to-class card-prop-list))


(def card-inner-props {:border? "mdl-card--border"})

(defn card-title [& content]
  (mdl-element :div "mdl-card__title" content false card-inner-props))

(defn card-title-text [& content]
  (mdl-element :span "mdl-card__title-text" content))

(defn card-subtitle-text [& content]
  (mdl-element :span "mdl-card__subtitle-text" content))

(defn card-media [& content]
  (mdl-element :div "mdl-card__media" content false card-inner-props))

(defn card-supporting-text [& content]
  (mdl-element :div "mdl-card__supporting-text" content false card-inner-props))

(defn card-actions [& content]
  (mdl-element :div "mdl-card__actions" content false card-inner-props))

(defn ul-list [& content]
  (mdl-element :ul "mdl-list" content))

(defn list-item-prop-to-class [k v]
  (condp = k
    :lines (condp = v
             1 ""
             2 "mdl-list__item--two-line"
             3 "mdl-list__item--three-line"
             "")))

(defn list-item [& content]
  (mdl-element :li "mdl-list__item" content false list-item-prop-to-class [:lines]))

(defn list-item-primary-content [& content]
  (mdl-element :span "mdl-list__item-primary-content" content))

(defn list-item-secondary-content [& content]
  (mdl-element :span "mdl-list__item-secondary-content" content))

(defn list-item-secondary-action [& content]
  (mdl-element :span "mdl-list__item-secondary-action" content))

(defn list-item-secondary-info [& content]
  (mdl-element :span "mdl-list__item-secondary-info" content))

(defn list-item-text-body [& content]
  (mdl-element :span "mdl-list__item-text-body" content))

(defn list-item-sub-title [& content]
  (mdl-element :span "mdl-list__item-sub-title" content))

(defn list-item-icon [& content]
  (mdl-element :i "material-icons mdl-list__item-icon" content))

(defn list-item-avatar [& content]
  (mdl-element :i "material-icons mdl-list__item-avatar" content))

(defn header-menu-items [menu-items]
  [upgrade
   (into [:ul
          {:class "mdl-menu mdl-menu--bottom-right mdl-js-menu mdl-js-ripple-effect"
           :for   "header-menu"}]
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

    [:div other-props
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
