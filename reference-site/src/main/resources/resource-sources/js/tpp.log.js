/**
 * Telenor logging
 * 
 * Wraps the browsers console.log.
 */
(function($) {
	"use strict";
	
	function Logger() {
	}

	Logger.prototype = {
		enabled : false,
		
		info : function() {
			if(typeof(window.console) != 'undefined' && this.enabled) {
				console.log(arguments[0]);
			}
		}
	};
	
	if(window.telenor === undefined) {
		window.telenor = {};
	}
	
	window.telenor.log = new Logger();
})();