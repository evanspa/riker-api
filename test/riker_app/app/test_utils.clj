(ns riker-app.app.test-utils
  "A set of helper functions to aid in the unit testing of REST services."
  (:require [ring.mock.request :as mock]
            [clojure.tools.logging :as log]))

(defn last-url-part
  "Returns the last path component of url-str. E.g., given
  http://www.example.com/abc/123, returns '123'"
  [url-str]
  (log/debug "in last-url-part fn, url-str " url-str)
  (.substring url-str (inc (.lastIndexOf url-str "/"))))

(defn header
  "If val is not empty, a new request is returned containing a new header with
  name hdr and val for the value.  Otherwise req is returned."
  [req hdr val]
  (if (not (empty? val))
    (mock/header req hdr val)
    req))

(defn authorization-req-hdr-val
  "Returns the string value to use for an 'Authorization' request header based
  on the given parameters."
  [auth-scheme auth-scheme-param-name auth-token]
  (str auth-scheme " " auth-scheme-param-name "=\"" auth-token "\""))

(defn mt
  "Returns a media type string based on the given parameters."
  [mt-type
   mt-subtype
   version
   format-ind]
  (let [accept (format "%s/%s" mt-type mt-subtype)
        version (if version (format "-v%s" version) "")
        format-ind (if format-ind (format "+%s" format-ind) "")]
    (format "%s%s%s" accept version format-ind)))

(defn req-w-std-hdrs
  "Returns a mock request using the given parameters:

  mt-type - Type part of 'Accept' request header media type.
  mt-subtype - Subtype part of 'Accept' request header media type.
  version - Version part of 'Accept' request header media type.
  charset - Character set part of 'Accept' request header media type.
  format-ind - Format indicator part of 'Accept' request header media type.
  lang - 'Accept-Language' request header value.
  method - The HTTP method.
  uri - The request URI."
  [mt-type
   mt-subtype
   version
   charset
   format-ind
   lang
   method
   uri]
  (-> (mock/request method uri)
      (header "Accept" (mt mt-type mt-subtype version format-ind))
      (header "Accept-Charset" charset)
      (header "Accept-Language" lang)))
