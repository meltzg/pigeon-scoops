(ns pigeon-scoops.fetchers-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [pigeon-scoops.fetchers :as fetchers]))

(deftest encode-body-test
  (testing "encodes application/json with JSON.stringify, converting keyword keys via str"
    (let [result (fetchers/encode-body "application/json" {:foo "bar"})]
      (is (string? result))
      (is (= "bar" (aget (js/JSON.parse result) ":foo")))))

  (testing "encodes application/transit+json as a transit string"
    (let [result (fetchers/encode-body "application/transit+json" {:foo "bar"})]
      (is (string? result))
      (is (str/includes? result "foo"))))

  (testing "throws on unsupported content type"
    (is (thrown? ExceptionInfo
                 (fetchers/encode-body "text/plain" {:foo "bar"})))))

(deftest decode-body-test
  (testing "decodes application/json back to a Clojure map"
    (is (= {:foo "bar"}
           (fetchers/decode-body "application/json" (js/JSON.stringify #js {:foo "bar"})))))

  (testing "decodes application/transit+json back to a Clojure value"
    (let [encoded (fetchers/encode-body "application/transit+json" {:hello "world"})]
      (is (= {:hello "world"}
             (fetchers/decode-body "application/transit+json" encoded)))))

  (testing "throws on unsupported content type"
    (is (thrown? ExceptionInfo
                 (fetchers/decode-body "text/plain" "some body"))))

  (testing "encode/decode roundtrip preserves nested data"
    (let [data {:items [{:id 1 :name "foo"} {:id 2 :name "bar"}]}]
      (is (= data (fetchers/decode-body "application/transit+json"
                                        (fetchers/encode-body "application/transit+json" data)))))))
