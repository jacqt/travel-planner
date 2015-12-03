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
  * Putting legend on the side
  * Asking users about map misalignment

## FEATURES

  * Adding tags
    * Get out of the city
    * Rainy day
  * Internal subreddit
  * Filter by london radius
  * Custom addresses

## FEEDBACK

Tina
  * Might have too many cards, maybe need to fold the events so that there's less content
  * Make the add to journey bigger
  * It should be a step process? Should have a more structured layout.
  * Because she didn't know that there would be a big map on the bottom, she was confused as to how she we get the final result
  * Should add some indication that there is a final map at the very bottom with the details of the journey

Ashley
  * Maybe add a go button
  * Give user the freedom to select the order of journey
  * Also add options to drive / walk / bike / etc.   
  * Timing is very important - add closing and opening time
  * How long it takes to get from one place to another place
  * Multi-day?

Michael
  * Too hard?
  * If I want to see multiple places, I want to be able to see them
  * Show me the places I'm interested in
  * Just the start point? No end point.
  * This is a landmark cruising scenario - it's not for in depth traveling
  * It's not that great if you want to really visit a place (e.g. Forbidden City)
  * Maybe make it an app that chooses the routes for them, so that it's as easy as possible to use
  * Make some lists that people can select automatically. It's too much work to sort through all the cards
  * We can sell one day passes for this too

