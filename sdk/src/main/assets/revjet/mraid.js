/*
 * MRAID 3.0 for the RevJet Android SDK, mirroring the iOS SDK's.
 *
 * Messages reach the native side as `{ name, body }` envelopes, through a single
 * WebMessageListener.
 *
 * Android runs document-start scripts in every same-origin frame, so the guard below keeps `mraid`
 * to the ad document: a creative rendering in an iframe reaches it through `window.top`, and the
 * native side accepts messages from the main frame only.
 */
if (window.self === window.top) {

window.__revjetPost = function(name, body) {
    RevJetBridge.postMessage(JSON.stringify({ name: name, body: body === undefined ? null : body }));
};
var __revjetPost = window.__revjetPost;
        

    window.mraid = (function() {
        // Constants
        const EVENTS = {
            ERROR: 'error',
            INFO: 'info',
            READY: 'ready',
            STATECHANGE: 'stateChange',
            EXPOSURECHANGE: 'exposureChange',
            VIEWABLECHANGE: 'viewableChange'
        };

        const STATES = {
            LOADING: 'loading',
            DEFAULT: 'default',
            EXPANDED: 'expanded',
            HIDDEN: 'hidden',
            RESIZED: 'resized'
        };

        const PLACEMENT_TYPES = {
            UNKNOWN: 'unknown',
            INLINE: 'inline',
            INTERSTITIAL: 'interstitial'
        };

        // State variables
        let state = STATES.LOADING;
        let placementType = PLACEMENT_TYPES.UNKNOWN;
        let screenSize = {};
        let maxSize = {};
        let currentAppOrientation = { orientation: 'portrait', locked: true };
        let currentPosition = { x: 0, y: 0, width: 0, height: 0 };
        let defaultPosition = { x: 0, y: 0, width: 0, height: 0 };
        let supportProperties = {
            sms: false,
            tel: false,
            calendar: false,
            storePicture: false,
            inlineVideo: false,
            vpaid: false,
            location: false
        };
        let lastExposureData = null;
        let viewable = false;

        // Only stored: an inline banner cannot act on them, and expansion is not supported
        let expandProperties = { width: null, height: null, useCustomClose: false };
        let orientationProperties = { allowOrientationChange: true, forceOrientation: 'none' };

        // Utility functions
        const contains = function(value, array) {
            return array.includes(value);
        };

        const reportNotSupported = function(action) {
            logDebug(`🔴 ${action} is not supported by this container`);
            broadcastEvent(EVENTS.ERROR, 'Not supported by this container.', action);
        };

        const logDebug = function(message) {
            __revjetPost('logDebug', message);
        };

        // EventListeners class
        class EventListeners {
            constructor(event) {
                this.event = event;
                this.listeners = [];
                logDebug(`🟢 Created EventListeners for event: ${event}`);
            }

            add(listener) {
                this.listeners.push(listener);
                logDebug(`🟢 Added listener: ${listener} for event: ${this.event}, listeners: ${this.listeners.length}`);

                // Fire the following events almost immediately, as they might have been dispatched before the listeners were added
                if (this.event === EVENTS.READY && state !== STATES.LOADING) {
                    logDebug(`🟢 Firing an ${this.event} event`);
                    window.setTimeout(() => listener(), 0);
                } else if (this.event === EVENTS.EXPOSURECHANGE && lastExposureData != null) {
                    logDebug(`🟢 Firing an ${this.event} event: ${JSON.stringify(lastExposureData)}`);
                    const { exposedPercentage, visibleRectangle, occlusionRectangles } = lastExposureData;
                    window.setTimeout(() => listener(exposedPercentage, visibleRectangle, occlusionRectangles), 0);
                }
            }

            remove(listener) {
                this.listeners = this.listeners.filter(l => l !== listener);
                logDebug(`🟢 Removing listener: ${listener} for event: ${this.event}, listeners: ${this.listeners.length}`);
            }

            removeAll() {
                logDebug(`🟢 Removing all listeners for event: ${this.event}`);
                this.listeners = [];
            }

            broadcast(...args) {
                logDebug(`✅ Broadcasting event: ${this.event}, listeners: ${this.listeners.length}`);
                for (const listener of this.listeners) {
                    try {
                        listener(...args);
                    } catch (e) {
                        logDebug(`🔴 Error broadcasting event: ${this.event}, error: ${e.message}`);
                    }
                }
            }
        }

        // Event broadcasting
        const listeners = new Map();

        const broadcastEvent = function(event, ...args) {
            logDebug(`Attempting to broadcast event: "${event}". Active events: ${Array.from(listeners.keys()).join(',')}`);
            if (listeners.has(event)) {
                logDebug(`🟢 Listener found for event: ${event}, args: ${JSON.stringify(args)}`);
                listeners.get(event).broadcast(...args);
            } else {
                logDebug(`🔴 No listener for event: ${event}, args: ${JSON.stringify(args)}`);
            }
        };

        // Change handlers
        const changeHandlers = {
            state: function(val) {
                if (state === STATES.LOADING) {
                    broadcastEvent(EVENTS.INFO, 'Native SDK initialized.');
                }
                state = val;
                broadcastEvent(EVENTS.INFO, 'Set state to ' + JSON.stringify(val));
                broadcastEvent(EVENTS.STATECHANGE, state);
            },

            placementType: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set placementType to ' + JSON.stringify(val));
                placementType = val;
            },

            sizeChange: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set screenSize to ' + JSON.stringify(val));
                for (const key in val) {
                    if (val.hasOwnProperty(key)) screenSize[key] = val[key];
                }
            },

            maxSize: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set maxSize to ' + JSON.stringify(val));
                maxSize = val;
            },

            currentAppOrientation: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set currentAppOrientation to ' + JSON.stringify(val));
                currentAppOrientation = val;
            },

            currentPosition: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set currentPosition to ' + JSON.stringify(val));
                currentPosition = val;
            },

            defaultPosition: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set defaultPosition to ' + JSON.stringify(val));
                defaultPosition = val;
            },

            supports: function(val) {
                broadcastEvent(EVENTS.INFO, 'Set supports to ' + JSON.stringify(val));
                supportProperties = val;
            }
        };

        // Fire change event
        const fireChangeEvent = function(properties) {
            logDebug(`Firing change event with properties: ${JSON.stringify(properties)}`);
            for (const p in properties) {
                if (properties.hasOwnProperty(p)) {
                    const handler = changeHandlers[p];
                    if (handler) {
                        handler(properties[p]);
                    } else {
                        logDebug(`🔴 No handler found for property: ${p}`);
                    }
                }
            }
        };

        // Public API
        return {
            getVersion: function() {
                logDebug(`🟢 Ad asked for a version`);
                return '3.0';
            },

            getState: function() {
                logDebug(`🟢 Ad asked for a state: ${state}`);
                return state;
            },

            getScreenSize: function() {
                logDebug(`🟢 Ad asked for the screen size: ${JSON.stringify(screenSize)}`);
                return { width: screenSize.width, height: screenSize.height };
            },

            getCurrentAppOrientation: function() {
                logDebug(`🟢 Ad asked for the app orientation: ${JSON.stringify(currentAppOrientation)}`);
                return {
                    orientation: currentAppOrientation.orientation,
                    locked: currentAppOrientation.locked
                };
            },

            getMaxSize: function() {
                logDebug(`🟢 Ad asked for the maximum size: ${JSON.stringify(maxSize)}`);
                return { width: maxSize.width, height: maxSize.height };
            },

            getCurrentPosition: function() {
                logDebug(`🟢 Ad asked for its position: ${JSON.stringify(currentPosition)}`);
                return {
                    x: currentPosition.x,
                    y: currentPosition.y,
                    width: currentPosition.width,
                    height: currentPosition.height
                };
            },

            getDefaultPosition: function() {
                logDebug(`🟢 Ad asked for its default position: ${JSON.stringify(defaultPosition)}`);
                return {
                    x: defaultPosition.x,
                    y: defaultPosition.y,
                    width: defaultPosition.width,
                    height: defaultPosition.height
                };
            },

            /* The container serves inline ads only, these report an error rather than being absent */
            expand: function() {
                reportNotSupported('expand');
            },

            resize: function() {
                reportNotSupported('resize');
            },

            unload: function() {
                reportNotSupported('unload');
            },

            playVideo: function() {
                reportNotSupported('playVideo');
            },

            storePicture: function() {
                reportNotSupported('storePicture');
            },

            createCalendarEvent: function() {
                reportNotSupported('createCalendarEvent');
            },

            setResizeProperties: function() {
                reportNotSupported('setResizeProperties');
            },

            getResizeProperties: function() {
                reportNotSupported('getResizeProperties');
            },

            getLocation: function() {
                reportNotSupported('getLocation');
                return -1;
            },

            getPlacementType: function() {
                logDebug(`🟢 Ad asked for a placement type: ${placementType}`);
                return placementType;
            },

            /** @deprecated in MRAID 3.0, kept for backwards compatibility */
            isViewable: function() {
                logDebug(`🟢 Ad asked whether it is viewable: ${viewable}`);
                return viewable;
            },

            getExpandProperties: function() {
                // Before they are set, the sizes reported are those of the screen
                const properties = {
                    width: expandProperties.width != null ? expandProperties.width : screenSize.width,
                    height: expandProperties.height != null ? expandProperties.height : screenSize.height,
                    useCustomClose: expandProperties.useCustomClose,
                    isModal: true
                };

                logDebug(`🟢 Ad asked for expand properties: ${JSON.stringify(properties)}`);
                return properties;
            },

            setExpandProperties: function(properties) {
                if (!properties) {
                    logDebug(`🔴 Properties are required for setExpandProperties`);
                    broadcastEvent(EVENTS.ERROR, 'Properties are required.', 'setExpandProperties');
                    return;
                }

                // `isModal` is read only, `useCustomClose` is ignored by an MRAID 3.0 host
                if (properties.width != null) expandProperties.width = properties.width;
                if (properties.height != null) expandProperties.height = properties.height;
                if (properties.useCustomClose != null) expandProperties.useCustomClose = properties.useCustomClose;

                logDebug(`🟢 Ad set expand properties: ${JSON.stringify(expandProperties)}`);
            },

            getOrientationProperties: function() {
                logDebug(`🟢 Ad asked for orientation properties: ${JSON.stringify(orientationProperties)}`);
                return {
                    allowOrientationChange: orientationProperties.allowOrientationChange,
                    forceOrientation: orientationProperties.forceOrientation
                };
            },

            setOrientationProperties: function(properties) {
                if (!properties) {
                    logDebug(`🔴 Properties are required for setOrientationProperties`);
                    broadcastEvent(EVENTS.ERROR, 'Properties are required.', 'setOrientationProperties');
                    return;
                }

                if (properties.allowOrientationChange != null) {
                    orientationProperties.allowOrientationChange = properties.allowOrientationChange;
                }

                if (properties.forceOrientation != null) {
                    if (contains(properties.forceOrientation, ['portrait', 'landscape', 'none'])) {
                        orientationProperties.forceOrientation = properties.forceOrientation;
                    } else {
                        logDebug(`🔴 Unknown forceOrientation: ${properties.forceOrientation}`);
                        broadcastEvent(EVENTS.ERROR, 'Unknown forceOrientation.', 'setOrientationProperties');
                    }
                }

                logDebug(`🟢 Ad set orientation properties: ${JSON.stringify(orientationProperties)}`);
            },

            open: function(url) {
                logDebug(`🟢 Ad requested to open URL: ${url}`);
                __revjetPost('onClick', url);
            },

            close: function() {
                logDebug(`🟢 Ad requested to close`);
                __revjetPost('close', {});
            },

            supports: function(feature) {
                const isSupported = supportProperties[feature] === true;
                logDebug(`🟢 Ad asked whether ${feature} is supported: ${isSupported}`);
                return isSupported;
            },

            setSupports: function(sms, tel, calendar, storePicture, inlineVideo) {
                logDebug(`🟢 Ad set supports: sms=${sms}, tel=${tel}, calendar=${calendar}, storePicture=${storePicture}, inlineVideo=${inlineVideo}`);
                supportProperties = {
                    sms: sms,
                    tel: tel,
                    calendar: calendar,
                    storePicture: storePicture,
                    inlineVideo: inlineVideo
                };
            },

            useCustomClose: function() {
                logDebug(`🟢 Ad requested to use custom close`);
            },

            setScreenSize: function(width, height) {
                if (typeof width === 'number' && typeof height === 'number') {
                    screenSize = { width, height };
                    logDebug(`🟢 Ad set screen size to: ${JSON.stringify(screenSize)}`);
                    broadcastEvent(EVENTS.INFO, 'Set screen size to ' + JSON.stringify(screenSize));
                } else {
                    logDebug(`🔴 Invalid screen size values: width=${width}, height=${height}`);
                    broadcastEvent(EVENTS.ERROR, 'Invalid screen size values.', 'setScreenSize');
                }
            },

            addEventListener: function(event, listener) {
                if (!event || !listener) {
                    logDebug(`🔴 Both event and listener are required for addEventListener`);
                    broadcastEvent(EVENTS.ERROR, 'Both event and listener are required.', 'addEventListener');
                } else if (!contains(event, Object.values(EVENTS))) {
                    logDebug(`🔴 Unknown MRAID event: ${event}`);
                    broadcastEvent(EVENTS.ERROR, 'Unknown MRAID event: ' + event, 'addEventListener');
                } else {
                    if (!listeners.has(event)) {
                        logDebug(`🟢 Creating new EventListeners for event: ${event}`);
                        listeners.set(event, new EventListeners(event));
                    }
                    listeners.get(event).add(listener);
                    logDebug(`🟢 Listener added for "${event}". Active events: ${Array.from(listeners.keys()).join(',')}`);
                }
            },

            removeEventListener: function(event, listener) {
                if (!event) {
                    logDebug(`🔴 Event is required for removeEventListener`);
                    broadcastEvent(EVENTS.ERROR, 'Event is required.', 'removeEventListener');
                    return;
                }

                if (listener) {
                    if (listeners.has(event)) {
                        listeners.get(event).remove(listener);
                    } else {
                        logDebug(`🔴 Listener not currently registered for event: ${event}`);
                        broadcastEvent(EVENTS.ERROR, 'Listener not currently registered for event.', 'removeEventListener');
                        return;
                    }
                } else if (listeners.has(event)) {
                    listeners.get(event).removeAll();
                }

                if (listeners.has(event) && listeners.get(event).listeners.length === 0) {
                    logDebug(`🟢 Deleting event from listeners map: ${event}`);
                    listeners.delete(event);
                }

                logDebug(`🟢 Removed listener for "${event}". Active events: ${Array.from(listeners.keys()).join(',')}`);
            },

            /** @private */
            _fireChangeEvent: function(properties) {
                logDebug(`🟢 Firing change event with properties: ${JSON.stringify(properties)}`);
                fireChangeEvent(properties);
            },

            /**
             * `occlusionRectangles` is not implemented as it is not needed at this time
             * @private
             */
            _notifyExposureChangeEvent: function(properties) {
                const { exposedPercentage, visibleRectangle, occlusionRectangles } = properties;
                lastExposureData = { exposedPercentage, visibleRectangle, occlusionRectangles };

                logDebug(`🟢 Host fired a exposure change event, exposedPercentage: ${exposedPercentage}, visibleRectangle: ${JSON.stringify(visibleRectangle)}, occlusionRectangles: ${JSON.stringify(occlusionRectangles)}`);
                broadcastEvent(EVENTS.EXPOSURECHANGE, exposedPercentage, visibleRectangle, occlusionRectangles);

                // MRAID does not define a threshold for what counts as viewable
                const isNowViewable = exposedPercentage > 0;
                if (isNowViewable !== viewable) {
                    viewable = isNowViewable;
                    broadcastEvent(EVENTS.VIEWABLECHANGE, viewable);
                }
            },

            /** @private */
            _notifyReadyEvent: function() {
                logDebug(`🟢 Host fired a '${EVENTS.READY}' event`);
                broadcastEvent(EVENTS.READY);
            }
        };
    })();

    // Call onMRAIDInit when script is loaded
    __revjetPost('onMRAIDInit', {});
    

}
