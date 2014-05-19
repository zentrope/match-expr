# match-expr

A Clojure library for filtering arbitrary maps of data using a boolean
expression language. The expressions can be stored as strings in a
database (say), then parsed and evaluated for any arbitrary map.

If your app accepts incoming data you have no control over other than
it consists of key/value pairs, you can treat these expressions as
data for filtering and grouping that data without having to re-compile
or deploy the original app.

## Example

Suppose you have some maps containing similar information (by domain)
but don't necessarily use the same keys for the same information.

For example, server information pulled from disparate sources and
systems:

    {:name   "foo23.bar.com" :ip   "192.168.1.2"}
    {:name   "foo11.bar.com" :inet "192.168.1.3"}
    {:dns    "foo99.bar.com" :ipv4 #{"10.10.1.1", "192.168.1.1"}}
    {:server "foo1.host.com" :ip   "176.88.1.5"}

Furthermore, some of the attributes have multiple values (as seen on
the third line above).

If you want to find all the `bar.com` servers on the `192.168.1`
network, you can create an expression like this:

    (and (or (match :name   ".*bar.com$")
             (match :dns    ".*bar.com$")
             (match :server ".*bar.com$"))
         (or (cidr  :ip     "192.168.1/24")
             (cidr  :inet   "192.168.1/24")
             (cidr  :ipv4   "192.168.1/24")))


In English: Find all the servers in which either `:name` or `:dns` or
`:server` end in "bar.com", and in which either `:ip` or `:inet` or `:ipv4` are
in the "192.168.1/24" CIDR range.

You might use this in a Clojure app like:

    (require '[match-expr.core :as expr])

    (def rule (expr/parse '(and (or (match :name   ".*bar.com$")
                                    (match :dns    ".*bar.com$")
                                    (match :server ".*bar.com$"))
                                (or (cidr  :ip     "192.168.1/24")
                                    (cidr  :inet   "192.168.1/24")
                                    (cidr  :ipv4   "192.168.1/24")))))

    (defn special-server?
      [server]
      (expr/eval server expr))

    (let [all-servers (retrieve-server-data)]
      (filter special-server? data))

If all goes well, the result should be:

    {:name   "foo23.bar.com" :ip   "192.168.1.2"}
    {:name   "foo11.bar.com" :inet "192.168.1.3"}
    {:dns    "foo99.bar.com" :ipv4 #{"10.10.1.1", "192.168.1.1"}}


## Context

The reason I wrote this was to help integrate different products and
systems in a "professional services" context. You can attempt to
dictate your own API, or that of all other participants, but you won't
have much luck if you want to get things done in a timely,
not-too-political manner.

On the other hand, if you can write an engine that allows us to
"update" the rules without re-deploying the application or revising an
API, so much the better.

This is my attempt at doing that.

## TODO

 - Add additional operators, as necessary.

 - The numeric comparison operators should work with multiple
   values for a single key.

 - The parser/evaluator should return a function rather than walk the
   expression tree for each comparison.

## License

Copyright &copy; 2014 Keith Irwin

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
