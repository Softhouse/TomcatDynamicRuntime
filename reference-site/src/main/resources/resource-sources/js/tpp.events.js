/**
 * An asynchronous event channel for publishing event outside jQuery and not
 * relying on DOM. Simple publish and subscribe.
 * 
 * The events are async and fire-and-forget, no return value is used and no
 * guarentee that subscribers will in fact get the event.
 * 
 * This script should be included prior to any plugins as it binds function
 * to the telenor.event prototype.
 */
(function() {
	"use strict";
	
	function EventChannel() {
		this.init();
	}
	
	EventChannel.prototype = {
		constructor : EventChannel,
		
		init : function() {
			// events are keys and subscribers are an array of functions
			this.events = {};
		},
		
		/**
		 * Called by the publisher to publish data to all registered
		 * subscribers for the given event.
		 * 
		 * Subscribers will be served the event asynchronously.
		 * 
		 * @param event The name of the event
		 * @param sender The object that publishes (sends) the event
		 * @param data The data to publish
		 * @param sync True to make notifications synchronous (default is false) 
		 */
		publish : function(event, sender, data, sync) {
			if(typeof(b) == 'undefined') {
				sync = false;
			}
			
			if(!this.events.hasOwnProperty(event)) {
				telenor.log.info("No subscriber registered for event: " + event);
				return;
			}
			
			telenor.log.info('Publishing event: ' + event);
			
			var self = this,
				eData = data,
				eSender = sender,
				broadcast = function() {
				var subscribers = self.events[event],
					len = subscribers.length;
				for(var i = 0; i < len; i++) {
					subscribers[i].handler(event, eSender, eData);
				}
			};
			
			sync ? broadcast() : setTimeout(broadcast, 0);
		},
		
		/**
		 * Called by the subscriber to register for being called in the case
		 * of event being published.
		 * 
		 * @param event The name of the event to subscribe to.
		 * @param handler The function to handle the event (signature function(event, data))
		 * @return The unique subscription id for this event (may not be unique between event types)
		 */
		subscribe : function(event, handler) {
			telenor.log.info('Subscribing to event: ' + event);
			
			if(!this.events.hasOwnProperty(event)) {
				this.events[event] = [];
			}
			var id = ("0000" + (Math.random()*Math.pow(36,4) << 0).toString(36)).substr(-4);
			this.events[event].push({id: id, handler : handler});
			return id;
		},
		
		/**
		 * Remove the registered subscriber from the event (synchronously)
		 * 
		 * @param event The event to unsubscribe from 
		 * @param id The unique subscription id assigned at subscription
		 */
		unsubscribe : function(event, id) {
			if(this.events.hasOwnProperty(event)) {
				return;
			}
			var subscribers = this.events[event],
				len = subscribers.length;
			for(var i = 0; i < len; i++) {
				if(subscribers[i].id === id) {
					subscribers[i].splice(i, 1);
				}
			}
		}
	};
	
	if(window.telenor === undefined) {
		window.telenor = {};
	}
	
	window.telenor.events = new EventChannel();
})();
