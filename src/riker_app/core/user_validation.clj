(ns riker-app.core.user-validation
  (:require [pe-core-utils.core :as ucore]))

(def su-any-issues                      (bit-shift-left 1 0))
(def su-invalid-email                   (bit-shift-left 1 1))
(def su-username-and-email-not-provided (bit-shift-left 1 2))
(def su-password-not-provided           (bit-shift-left 1 3))
(def su-email-already-registered        (bit-shift-left 1 4))
(def su-username-already-registered     (bit-shift-left 1 5))
(def su-current-password-not-provided   (bit-shift-left 1 6))
(def su-current-password-incorrect      (bit-shift-left 1 7))

(def pwd-reset-any-issues              (bit-shift-left 1 0))
(def pwd-reset-unknown-email           (bit-shift-left 1 1))
(def pwd-reset-token-not-found         (bit-shift-left 1 2))
(def pwd-reset-token-flagged           (bit-shift-left 1 3))
(def pwd-reset-token-expired           (bit-shift-left 1 4))
(def pwd-reset-token-already-used      (bit-shift-left 1 5))
(def pwd-reset-token-not-prepared      (bit-shift-left 1 6))
(def pwd-reset-unverified-acct         (bit-shift-left 1 7))
(def pwd-reset-trial-and-grace-exp     (bit-shift-left 1 8))

(def ^:private email-regex
  #"[a-zA-Z0-9[!#$%&'()*+,/\-_\.\"]]+@[a-zA-Z0-9[!#$%&'()*+,/\-_\"]]+\.[a-zA-Z0-9[!#$%&'()*+,/\-_\"\.]]+")

(defn is-valid-email? [email]
  (and (not (nil? email))
       (not (nil? (re-find email-regex email)))))

(defn save-new-user-validation-mask
  [{email :user/email
    username :user/username
    password :user/password}]
  (-> 0
      (ucore/add-condition #(and (empty? email)
                                 (empty? username))
                           su-username-and-email-not-provided
                           su-any-issues)
      (ucore/add-condition #(and (not (empty? email))
                                 (not (is-valid-email? email)))
                           su-invalid-email
                           su-any-issues)
      #_(ucore/add-condition #(empty? password)
                           su-password-not-provided
                           su-any-issues)))

(defn save-user-validation-mask
  [{email :user/email
    username :user/username
    password :user/password
    :as user}]
  (-> 0
      (ucore/add-condition #(and (contains? user :user/email)
                                 (contains? user :user/username)
                                 (empty? email)
                                 (empty? username))
                           su-username-and-email-not-provided
                           su-any-issues)
      (ucore/add-condition #(and (contains? user :user/email)
                                 (not (empty? email))
                                 (not (is-valid-email? email)))
                           su-invalid-email
                           su-any-issues)
      (ucore/add-condition #(and (contains? user :user/password)
                                 (empty? password))
                           su-password-not-provided
                           su-any-issues)))
