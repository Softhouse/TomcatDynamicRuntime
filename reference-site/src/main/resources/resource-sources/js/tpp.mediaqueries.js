/*
 * mediaqueries.js
 * 
 * Modified version of Syze v1.1.1 MIT/GPL2 @rezitech
 * http://rezitech.github.com/syze/
 * 
 * Changes to the original script:
 * - Do not listen to resize event on IE7,8
 * - Dispatch Telenor.Events instead of calling callback function
 * - Do not run onResize at start
 * - Do not dispatch any events if nothing changed
 * 
 * @dependencies: (Telenor.Events), ([Telenor.Log])
 */
(function (window, html, document) {

    var
    _sizes = [],
    _names = {},
    _from = 'browser',
    _debounceRate = 50,
    _oldSize,
    _currentView, 
    _responsive = isResponsive(),
    _viewportmeta = document.querySelector && document.querySelector('meta[name="viewport"]');
    
    
    
    
    /* 
     * Private
     * 
     * Attach event wrapper for browser compatibility
     * 
     * @param type String event type
     * @param fn Function callback function
     */ 
    function addWinEvent(type, fn) {
        if (window.addEventListener) addEventListener(type, fn, false); 
        else attachEvent('on' + type, fn);
    }

    /* 
     * Private
     * 
     * Calculate delay before recalculating sizes
     * 
     * @param fn Function callback function
     */ 
    function debounce(fn) {
        var timeout;
        return function () {
            var obj = this, args = arguments;
            function delayed () {
                fn.apply(obj, args);
                timeout = null;
            }
            if (timeout) clearTimeout(timeout);
            timeout = setTimeout(delayed, _debounceRate); 
        };
    };
    
    /* 
     * Private
     * 
     * Check if client supports responsive design (media queries)
     * 
     */ 
    function isResponsive() {
        if (/MSIE (\d+\.\d+);/.test(navigator.userAgent)){ //test for MSIE x.x;
            var ieversion = new Number(RegExp.$1); // capture x.x portion and store as a number
            if (ieversion<9) {
                return false;
            }
        }
        return true;
    };
    
    /* 
     * Private
     * 
     * Hack to set meta viewport depending on screen width (reason: iPad scale problem)
     * 
     */ 
    function onOrientationchange() {
        onResize();
        
        if(_viewportmeta) {
            if(_currentView == 'is-mobile') {
                _viewportmeta.content = "width=320, maximum-scale=1, minimum-scale=1";  
                
                // Hack to center page on Android devices
                if(!navigator.userAgent.match(/iPhone|iPad|iPod/i)){
                    var top = document.pageYOffset ? document.pageYOffset : (document.body ? document.body.scrollTop : 0);
                    window.scrollTo(0,top == 0 ? top+100 : 0);
                    setTimeout(function() {
                        window.scrollTo(0, top);
                    }, 1000);
                }
            
            } else {
                _viewportmeta.content = "width=960, maximum-scale=1.6, minimum-scale=.5";
            }

        };
    }
    /* 
     * Private
     * 
     * Check device dimensions and update css class names and finally dispatch event to listeners
     * 
     */ 
    function onResize() {
        var
        currentSize = 
            /^device$/i.test(String(_from)) ? !window.orientation || orientation == 180 ? screen.width : screen.height
            : /^browser$/i.test(String(_from)) ? html.clientWidth
            : (_from instanceof String) ? Function('return ' + _from)()
            : parseInt(_from, 10) || 0,
            htmlClassNames = html.className.replace(/^\s+|(^|\s)(gt\-|is\-|lt\-)[^\s]+/g, '').replace(/\s{2,}/,' ').split(' ');
        classNames = [], i = -1, arr = _sizes, len = arr.length;

        // sort sizes ascending numerically
        arr.sort(function (a, b) { return(a - b); });
        
        // hack to cover landscape view in handhelds (not touch pads) with landscape view larger than our mobile view (480px)
        if((window.orientation === 0 && screen.width < 768) || (window.orientation && screen.height < 768)) currentSize = arr[0];
        
        // get index of currentsize in sizes array
        while (++i < len) if (currentSize < arr[i]) break;
        currentSize = arr[Math.max(Math.min(--i, len - 1), 0)];
        
        // build new media query classnames 
        i = -1;
        while (++i < len) {
            classNames.push((currentSize > arr[i] ? 'gt-' : currentSize < arr[i] ? 'lt-' : 'is-') + (_names[arr[i]] || arr[i]));
            if(/is/.test(classNames[classNames.length-1])) _currentView = classNames[classNames.length-1];
        }
        
        // if the client does not support responsive design
        if(!_responsive) {
            classNames = ["is-desktop"];
        }
        // add media query class names and original class names to html element
        html.className = htmlClassNames.concat(classNames).join(' ');
        
        // do not continue if current size (client width) is unchanged
        if(_oldSize == currentSize) return;
        _oldSize = currentSize;
        
        // broadcast event to subscribers (depending on Telenor.Events is loaded)
        if(typeof window.telenor != 'undefined' && typeof window.telenor.events != 'undefined') {
            window.telenor.events.publish('update-device-width', this, { width: currentSize, className: _currentView});
            if(typeof (window.telenor) != 'undefined' && typeof(window.telenor.log) != 'undefined') {
                window.telenor.log.info('telenor.events dispatched update-device-width event ');
            }     
        };

    }
    
    /* 
     * Set up public API
     */ 
    window.Mediaqueries = {
        setup: function (val) { 
            if (val instanceof Object) { 
                _names = val;  
                _sizes = [];
                for(n in _names) {
                    _sizes.push(parseInt(n));
                }
                onOrientationchange();
            }
        },
        sizes: function (val) { _sizes = [].concat.apply([], arguments); return this; },
        names: function (val) { if (val instanceof Object) { _names = val; onResize(); } return this; },
        from: function (val) { _from = val; onResize(); return this; },
        debounceRate: function (val) { _debounceRate = parseInt(val, 10) || 0; onResize(); return this; },
        view: function() {return _currentView;},
        responsive: function() {return _responsive;}
    };
    
    /* 
     * Attach event handlers
     */ 
    addWinEvent('resize', debounce(onResize));
    addWinEvent('orientationchange', onOrientationchange);
    
}(this, this.document.documentElement, this.document));

/*
 * 
 * Initialize Mediaquery Object
 * 
 * Note that this is the only place where we define the available 
 * device widths. Remember that device names here corresponds to 
 * CSS class names and any changes must be done with care.
 * window.Mediaqueries.setup({ 480:'mobile', 481:'desktop'});
 * 
 */
window.Mediaqueries.setup({ 480:'mobile', 481:'desktop'});