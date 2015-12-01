# travel-site


## Development

Open a terminal and type `lein repl` to start a Clojure REPL
(interactive prompt).

In the REPL, type

```clojure
(run)
(browser-repl)
```

The call to `(run)` does two things, it starts the webserver at port
10555, and also the Figwheel server which takes care of live reloading
ClojureScript code and CSS. Give them some time to start.

Running `(browser-repl)` starts the Weasel REPL server, and drops you
into a ClojureScript REPL. Evaluating expressions here will only work
once you've loaded the page, so the browser can connect to Weasel.

When you see the line `Successfully compiled "resources/public/app.js"
in 21.36 seconds.`, you're ready to go. Browse to
`http://localhost:10555` and enjoy.

## TODO

  * Add animations and polish
  * Add info window to the markers (legend)
  * Change url encoding so that it looks something like ?start_address=Hyde Park&end_address=Imperial COllege&waypoint-ids[]=1
  * ~~Batch requests to Google Directions API~~

## Chestnut

Created with [Chestnut](http://plexus.github.io/chestnut/) 0.8.1 (3acc5e81).
