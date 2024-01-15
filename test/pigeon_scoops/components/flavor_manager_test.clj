(ns pigeon-scoops.components.flavor_manager_test
  (:require [clojure.test :as t]
            [pigeon-scoops.components.flavor-manager :as fm]
            [pigeon-scoops.spec.flavors :as fs]
            [pigeon-scoops.units.volume :as vol]))

(def flavor-no-mixin {::fs/name         "foobar"
                      ::fs/instructions ["mix it all together"]
                      ::fs/amount       1
                      ::fs/amount-unit  ::vol/qt
                      ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"})

(def flavor-with-mixins
  (assoc flavor-no-mixin ::fs/mixins [{::fs/amount      0.25
                                       ::fs/amount-unit ::vol/c
                                       ::fs/recipe-id   #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
                                      {::fs/amount      1
                                       ::fs/amount-unit ::vol/c
                                       ::fs/recipe-id   #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"}]))

(t/deftest scale-flavor-test
  (t/testing "A flavor can be scaled up and down"
    (t/are [flavor amount amount-unit expected]
      (= (fm/scale-flavor flavor amount amount-unit) expected)
      flavor-no-mixin 3 ::vol/qt {::fs/name         "foobar"
                                  ::fs/instructions ["mix it all together"]
                                  ::fs/amount       3
                                  ::fs/amount-unit  ::vol/qt
                                  ::fs/mixins       ()
                                  ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
      flavor-no-mixin 0.5 ::vol/qt {::fs/name         "foobar"
                                    ::fs/instructions ["mix it all together"]
                                    ::fs/amount       0.5
                                    ::fs/amount-unit  ::vol/qt
                                    ::fs/mixins       ()
                                    ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
      flavor-no-mixin 4 ::vol/c {::fs/name         "foobar"
                                 ::fs/instructions ["mix it all together"]
                                 ::fs/amount       4
                                 ::fs/amount-unit  ::vol/c
                                 ::fs/mixins       ()
                                 ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
      flavor-no-mixin 2 ::vol/l {::fs/name         "foobar"
                                 ::fs/instructions ["mix it all together"]
                                 ::fs/amount       2
                                 ::fs/amount-unit  ::vol/l
                                 ::fs/mixins       ()
                                 ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
      flavor-with-mixins 3 ::vol/qt {::fs/name         "foobar"
                                     ::fs/instructions ["mix it all together"]
                                     ::fs/amount       3
                                     ::fs/amount-unit  ::vol/qt
                                     ::fs/mixins       [{::fs/amount      0.75
                                                         ::fs/amount-unit ::vol/c
                                                         ::fs/recipe-id   #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"}
                                                        {::fs/amount      3.0
                                                         ::fs/amount-unit ::vol/c
                                                         ::fs/recipe-id   #uuid "dc5edc8e-79cf-4601-aab6-778d4897106a"}]
                                     ::fs/recipe-id    #uuid "c7044068-329e-4323-b814-d65bf3da6ba3"})))
