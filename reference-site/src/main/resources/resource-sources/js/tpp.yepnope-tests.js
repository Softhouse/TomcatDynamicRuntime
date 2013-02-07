/*
 * Yepnop tests
 * 
 * @dependencies: Modernizr (with yepnope)
 * 
 */
(function(window, Modernizr) {
    if(typeof Modernizr == "undefined") return false;
    yepnope({
        test : Modernizr["array-indexof"],
        yep : '/assets/js/browser/array-indexof.jquery.js'
    });
    yepnope({
        test: Modernizr["last-child"],
        nope: '/assets/js/browser/nth-child.jquery.js'
    });
    yepnope({
        test : Modernizr["box-size-bug"],
        yep : '/assets/js/browser/box-sizing.jquery.js'
    });
    yepnope({
        test : Modernizr["percent-width-bug"],
        yep : '/assets/js/browser/percent-width.jquery.js'
    });
    yepnope({
        test : Modernizr["vertical-align-bug"],
        yep : '/assets/js/browser/vertical-align.jquery.js'
    });
    yepnope({
        test: Modernizr["display-table"],
        nope: '/assets/js/browser/display-table.jquery.js'
    });
                    
}(this, this.Modernizr));