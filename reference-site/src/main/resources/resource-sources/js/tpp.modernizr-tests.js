/*
 * Modernizr tests
 * 
 * @dependencies: Modernizr
 * 
 */

(function(window, Modernizr) {
    if(typeof Modernizr == "undefined") return false;
    
    var userAgent = navigator.userAgent.toLowerCase();
    
    Modernizr.addTest('last-child', function () {
        return Modernizr.testStyles("#modernizr :last-child{width:200px;display:block}",function(elem){
            return elem.lastChild.offsetWidth === 200;
        }, 1);
    });
    
    Modernizr.addTest('vertical-align-bug', /msie/.test(userAgent) && /7.0/.test(userAgent));
    
    Modernizr.addTest('box-size-bug', /firefox/.test(userAgent) || /msie/.test(userAgent) || /opera/.test(userAgent));
    
    Modernizr.addTest('percent-width-bug', /opera/.test(userAgent));
    
    Modernizr.addTest('array-indexof', !Array.prototype.indexOf);
    
    Modernizr.addTest('ie7', /msie/.test(userAgent) && /7.0/.test(userAgent));
    
    Modernizr.addTest('ie8', document.documentMode == 8);
    
    Modernizr.addTest('ie9', document.documentMode == 9);
    
    Modernizr.addTest('oldie', document.documentMode < 9 || (/msie/.test(userAgent) && /7.0/.test(userAgent)));
    
    Modernizr.addTest('touch-screen', ('ontouchstart' in window));
    
}(this, this.Modernizr));