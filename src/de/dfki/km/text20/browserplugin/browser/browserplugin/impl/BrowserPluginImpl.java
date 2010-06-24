/*
* BrowserPluginImpl.java
* 
* Copyright (c) 2010, Ralf Biedert, DFKI. All rights reserved.
* 
* Redistribution and use in source and binary forms, with or without modification, are
* permitted provided that the following conditions are met:
* 
* Redistributions of source code must retain the above copyright notice, this list of
* conditions and the following disclaimer. Redistributions in binary form must reproduce the
* above copyright notice, this list of conditions and the following disclaimer in the
* documentation and/or other materials provided with the distribution.
* 
* Neither the name of the author nor the names of its contributors may be used to endorse or
* promote products derived from this software without specific prior written permission.
* 
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
* OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
* MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
* COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
* EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
* SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
* TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
* EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* 
*/
package de.dfki.km.text20.browserplugin.browser.browserplugin.impl;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xeoh.plugins.base.PluginManager;
import net.xeoh.plugins.base.util.JSPFProperties;
import net.xeoh.plugins.informationbroker.InformationBroker;
import net.xeoh.plugins.informationbroker.standarditems.strings.StringItem;
import net.xeoh.plugins.meta.updatecheck.UpdateCheck;
import net.xeoh.plugins.remote.RemoteAPI;
import net.xeoh.plugins.remotediscovery.RemoteDiscovery;
import netscape.javascript.JSObject;
import de.dfki.km.text20.browserplugin.browser.browserplugin.BrowserAPI;
import de.dfki.km.text20.browserplugin.browser.browserplugin.JSExecutor;
import de.dfki.km.text20.browserplugin.services.devicemanager.TrackingDeviceManager;
import de.dfki.km.text20.browserplugin.services.extensionmanager.ExtensionManager;
import de.dfki.km.text20.browserplugin.services.extensionmanager.SetupParameter;
import de.dfki.km.text20.browserplugin.services.mastergazehandler.MasterGazeHandler;
import de.dfki.km.text20.browserplugin.services.mastergazehandler.MasterGazeHandlerManager;
import de.dfki.km.text20.browserplugin.services.pagemanager.PageManager;
import de.dfki.km.text20.browserplugin.services.pagemanager.PageManagerManager;
import de.dfki.km.text20.browserplugin.services.persistentpreferences.PersistentPreferences;
import de.dfki.km.text20.browserplugin.services.sessionrecorder.SessionRecorder;
import de.dfki.km.text20.browserplugin.services.sessionrecorder.SessionRecorderManager;
import de.dfki.km.text20.services.pseudorenderer.Pseudorenderer;
import de.dfki.km.text20.services.pseudorenderer.PseudorendererManager;
import de.dfki.km.text20.services.trackingdevices.brain.BrainTrackingDevice;
import de.dfki.km.text20.services.trackingdevices.brain.BrainTrackingEvent;
import de.dfki.km.text20.services.trackingdevices.brain.BrainTrackingListener;
import de.dfki.km.text20.services.trackingdevices.eyes.EyeTrackingDevice;
import de.dfki.km.text20.services.trackingdevices.eyes.EyeTrackingEvent;
import de.dfki.km.text20.services.trackingdevices.eyes.EyeTrackingListener;
import de.dfki.km.text20.util.system.OS;

/**
 * Will be instantiated by the browser.
 * 
 * @author rb
 *
 */
public class BrowserPluginImpl extends Applet implements JSExecutor, BrowserAPI {
    private enum TransmitMode {
        ASYNC, DIRECT
    }

    /** */
    private static final long serialVersionUID = 8654743028251010225L;

    /** Appened to all live-connect callbacks */
    private String callbackPrefix;

    /** Keeps reference to the tracking device */
    private TrackingDeviceManager deviceManager;

    /** Handles tracking events. */
    private MasterGazeHandler gazeHandler;

    /** Keeps a reference to the plugin manager, in order not to overload this class */
    private InformationBroker infoBroker;

    /** Manages the related webpage */
    private PageManager pageManager;

    /** Keeps a reference to the plugin manager, in order not to overload this class */
    private PluginManager pluginManager;

    /** Used to store persistent prefrences. */
    private PersistentPreferences preferences;

    /** */
    private Pseudorenderer pseudorender;

    /** */
    private ExtensionManager extensionManager;

    /** Instance id if this plugin */
    final int instanceID = new Random().nextInt();

    /** */
    final BatchHandler batchHandler = new BatchHandler(this);

    /** */
    final Logger logger = Logger.getLogger(this.getClass().getName());

    /** Sets up logging */
    MasterLoggingHandler loggingHandler;

    /** Master file path */
    String masterFilePath = "/tmp";

    /** If set, getParameter will use this object to return values */
    Map<String, String> parameterOverride = null;

    /** Records screenshots and gaze points */
    SessionRecorder sessionRecorder;

    /** Executor to call javascript */
    final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

    /** Are we a true applet inside a browser, or a we made to believe we're only an applet but are run as an application */
    boolean thisIsTheMatrix = false;

    /** If we should keep a session record */
    boolean recordingEnabled = true;

    /** How to call JavaScript */
    TransmitMode transmitMode = TransmitMode.DIRECT;

    /** Browser window */
    JSObject window;

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#batch(java.lang.String)
     */
    public void batch(final String call) {
        this.batchHandler.batch(call);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#callFunction(java.lang.String, java.lang.String)
     */
    public Object callFunction(final String function) {
        try {
            if (this.sessionRecorder != null) {
                this.sessionRecorder.callFunction(function);
            }

            final Pattern p = Pattern.compile("(\\w*)\\(([^\\)]*)\\).");
            final Matcher matcher = p.matcher(function);

            boolean matches = matcher.matches();

            if (!matches) {
                this.logger.warning("No match found for " + function);
                return null;
            }

            final String name = matcher.group(1);
            final String args = matcher.group(2);

            // Execute the proper extension ...
            if (this.extensionManager.getExtensions().contains(name)) {
                final Object rval = this.extensionManager.executeFunction(name, args);

                // Log the result 
                if (rval != null) {
                    this.logger.fine("Returning object of type " + rval.getClass() + " with toString() value of '" + rval.toString() + "'");
                } else {
                    this.logger.fine("Returning null value");
                }

                return rval;
            }

        } catch (final Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#destroy()
     */
    @Override
    public void destroy() {
        this.pluginManager.shutdown();
        this.loggingHandler.shutdown();
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#getParameter(java.lang.String)
     */

    /**
     * Execute a script inside the browser
     *
     * (non-Javadoc)
     * @see de.dfki.km.text20.browserplugin.browser.browserplugin.JSExecutor#executeJSFunction(java.lang.String, java.lang.Object[])
     */
    public Object executeJSFunction(final String _function, final Object... args) {
        // In case we're in the matrix, dont try to call applet function, otherwise Agent Smith will
        // roundhouse kick your butt.
        if (this.thisIsTheMatrix) { return null; }

        // Append the callback prefix to the function.
        final String function = this.callbackPrefix + _function;

        //if (true) this.logger.info("Calling function + '" + function + "'");

        tryGetWindow();

        this.sessionRecorder.executeJSFunction(function, args);

        // If the window is still null, we try our fallback solution (maybe slow ...)
        if (this.window == null) {
            this.logger.warning("Unable to execute JS function : " + function + ". Did you forget to specifiy mayscript='yes' in the applet-tag?");

            int ctr = 0;

            final StringBuilder sb = new StringBuilder();
            sb.append("javascript:");
            sb.append(function);
            sb.append("(");
            for (final Object object : args) {
                sb.append("'");
                sb.append(object);
                sb.append("'");

                if (ctr++ < args.length - 1) {
                    sb.append(",");
                }
            }
            sb.append(");");

            this.logger.warning("Trying dirty fallback, calling : " + sb.toString());

            try {
                final AppletContext appletContext = getAppletContext();
                appletContext.showDocument(new URL(sb.toString()));
            } catch (final MalformedURLException e) {
                e.printStackTrace();
            }

            return null;
        }

        // This is the ugly way (Safari likes it)
        if (this.transmitMode.equals(TransmitMode.ASYNC)) {
            this.singleThreadExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        BrowserPluginImpl.this.window.call(function, args);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }

        // This is the nice way (Firefox likes it)
        if (this.transmitMode.equals(TransmitMode.DIRECT)) {
            try {
                return this.window.call(function, args);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#getParameter(java.lang.String)
     */
    @Override
    public String getParameter(final String key) {
        if (this.parameterOverride == null) { return super.getParameter(key); }

        return this.parameterOverride.get(key);
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#getParameterInfo()
     */
    @Override
    public String[][] getParameterInfo() {
        return new String[][] { { "transmitmode", "string", "What to use to call JavaScript" }, { "trackingdevice", "string", "Identifies the device handler" }, { "trackingconnection", "url", "If it is a remote device, where to contact?" }, { "sessionpath", "string", "Save all stuff to what?" }, { "callbackprefix", "string", "Appended to all callbacks from the applet via liveconnect." } };
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#getPreference(java.lang.String, java.lang.String)
     */
    public String getPreference(final String key, final String deflt) {
        this.sessionRecorder.getPreference(key, deflt);
        return this.preferences.getString(key, deflt);
    }

    /**
     * Wird seltener aufgerufen als start, sollte hier Verbindung mit dem Eye-Tracker-
     * Server aufnehmen und das Pluginframework initialisieren.
     */

    /* (non-Javadoc)
     * @see java.applet.Applet#init()
     */
    @Override
    public void init() {
        // We want to save from the first second 
        obtainMasterFilePath();

        final JSPFProperties props = new JSPFProperties();
        props.setProperty(RemoteAPI.class, "proxy.timeout", "1000");
        props.setProperty(RemoteDiscovery.class, "startup.locktime", "1000");
        props.setProperty(UpdateCheck.class, "update.url", "http://api.text20.net/common/versioncheck/");
        props.setProperty(UpdateCheck.class, "update.enabled", "true");
        props.setProperty(UpdateCheck.class, "product.name", "text20.plugin");
        props.setProperty(UpdateCheck.class, "product.version", "0.9");

        setupEarlyLogging(props);

        this.logger.info("ID of this instance " + this.instanceID + " init()");

        this.pluginManager = new FrameworkManager(props).getPluginManager();

        this.extensionManager = this.pluginManager.getPlugin(ExtensionManager.class);
        this.sessionRecorder = this.pluginManager.getPlugin(SessionRecorderManager.class).createSessionRecorder();
        this.deviceManager = this.pluginManager.getPlugin(TrackingDeviceManager.class);
        this.infoBroker = this.pluginManager.getPlugin(InformationBroker.class);
        this.preferences = this.pluginManager.getPlugin(PersistentPreferences.class);
        this.pseudorender = this.pluginManager.getPlugin(PseudorendererManager.class).createPseudorenderer();
        this.gazeHandler = this.pluginManager.getPlugin(MasterGazeHandlerManager.class).createMasterGazeHandler(this, this.pseudorender);
        this.pageManager = this.pluginManager.getPlugin(PageManagerManager.class).createPageManager(this.pseudorender);

        // Store parameters
        storeParameters();

        // Evaluate additional parameter
        processAdditionalParameter();

        // setup extensions
        setupExtensions();

        // Setup gaze recording
        initTrackingDevice();

        // Setup mouse recording
        initMouseRecording();

        // Publish items of the information broker
        publishBrokerItems();

        // Try to get the window
        tryGetWindow();

        // Provide the tracking device to other listeners.
        this.gazeHandler.setTrackingDevice(this.deviceManager.getEyeTrackingDevice());

        tellJSStatus("INITIALIZED");
    }

    /**
     * @param props
     */
    private void setupEarlyLogging(final JSPFProperties props) {

        Level level = null;

        // Setup JSPF logging level
        final String logging = getParameter("logging");

        if (logging != null && !logging.equals("default")) {
            level = Level.parse(logging);
        }

        // Initialize logging handler and others ...
        try {
            this.loggingHandler = new MasterLoggingHandler(this.masterFilePath, level);
        } catch (final SecurityException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Setup all known extensions
     */
    private void setupExtensions() {
        for (final SetupParameter parameter : SetupParameter.values()) {

            Object value = null;

            switch (parameter) {
            case SESSION_RECORDER:
                value = this.sessionRecorder;
                break;

            case BROWSER_API:
                value = this;
                break;

            case GAZE_HANDLER:
                value = this.gazeHandler;
                break;

            case JAVASCRIPT_EXECUTOR:
                value = this;
                break;

            case PSEUDO_RENDERER:
                value = this.pseudorender;
                break;

            default:
                this.logger.warning("Warning. Parameter " + parameter + " not passed to extensions!");
            }

            // Set the given parameter
            this.extensionManager.setParameter(parameter, value);
        }
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#logString(java.lang.String)
     */
    public void logString(final String toLog) {
        this.logger.info(toLog);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#registerListener(java.lang.String, java.lang.String)
     */
    public void registerListener(final String type, final String listener) {
        this.logger.info("Registering listener of type " + type + " with name " + listener);
        this.sessionRecorder.registerListener(type, listener);
        this.gazeHandler.registerJSCallback(type, listener);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#removeListener(java.lang.String)
     */
    public void removeListener(final String listener) {
        this.sessionRecorder.removeListener(listener);
        this.gazeHandler.removeJSCallback(listener);
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#destroy()
     */

    /**
     * Call this if you're in applet mode
     * 
     * @param value
     */
    public void setBrowserImitation(final boolean value) {
        this.thisIsTheMatrix = value;
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#stop()
     */

    /**
     * Sets an override for parameters
     * 
     * @param override
     */
    public void setParameterOverride(final Map<String, String> override) {
        this.parameterOverride = override;
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#start()
     */

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#setPreference(java.lang.String, java.lang.String)
     */
    public void setPreference(final String key, final String value) {
        this.sessionRecorder.setPreference(key, value);
        this.preferences.setString(key, value);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#setSessionParameter(java.lang.String, java.lang.String)
     */
    public void setSessionParameter(final String key, final String value) {
        this.sessionRecorder.setParameter("#sessionparameter." + key, value);
    }

    /**
     * @param one
     * @param two
     * @param three
     * @param four
     */
    public void simpleBenchmark(final String one, final String two, final String three,
                                final String four) {
        this.logger.info("new couple: " + one + " " + two + " " + three + " " + four);
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#start()
     */
    @Override
    public void start() {
        if (this.recordingEnabled) {
            this.sessionRecorder.start();
        }
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#stop()
     */
    @Override
    public void stop() {
        if (this.recordingEnabled) {
            this.sessionRecorder.stop();
        }
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#testBasicFunctionality(java.lang.String)
     */
    public void testBasicFunctionality(final String callback) {
        this.logger.info("testBasicFunctionality() reached!");

        executeJSFunction(callback, "Roundtrip communication appears to be working. This means your browser successfully contacted the plugin, and the plugin was able to call the browser.");
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#updateBrowserGeometry(int, int, int, int)
     */
    public void updateBrowserGeometry(final int x, final int y, final int w, final int h) {
        this.sessionRecorder.updateGeometry(new Rectangle(x, y, w, h));
        this.pageManager.updateBrowserGeometry(x, y, w, h);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#updateDocumentViewport(int, int)
     */
    public void updateDocumentViewport(final int x, final int y) {
        this.sessionRecorder.updateViewport(new Point(x, y));
        this.pageManager.updateDocumentViewport(x, y);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#updateElementFlag(java.lang.String, java.lang.String, boolean)
     */
    public void updateElementFlag(final String id, final String flag, final boolean value) {
        this.sessionRecorder.updateElementFlag(id, flag, value);
        this.pageManager.updateElementFlag(id, flag, value);
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.impl.BrowserAPI#updateBrowserGeometry(int, int, int, int)
     */
    public void updateElementGeometry(final String id, final String type,
                                      final String content, final int x, final int y,
                                      final int w, final int h) {
        this.sessionRecorder.updateElementGeometry(id, type, content, new Rectangle(x, y, w, h));
        this.pageManager.updateElementGeometry(id, type, content, x, y, w, h);
    }

    /**
     * Initializes the tracking devices
     */
    private void initTrackingDevice() {

        // Setup brain tracking device
        this.deviceManager.initEyeTrackerConnection(getParameter("trackingdevice"), getParameter("trackingconnection"));
        this.deviceManager.getEyeTrackingDevice().addTrackingListener(new EyeTrackingListener() {

            public void newTrackingEvent(final EyeTrackingEvent event) {
                BrowserPluginImpl.this.sessionRecorder.newTrackingEvent(event);
            }
        });

        // Store the device info
        final EyeTrackingDevice trackingDevice = this.deviceManager.getEyeTrackingDevice();
        this.sessionRecorder.storeDeviceInfo(trackingDevice.getDeviceInfo());

        // Setup eye tracking device
        if (getParameter("enablebraintracker") != null && getParameter("enablebraintracker").equals("true")) {
            this.logger.info("Enabling Brain Tracker");
            this.deviceManager.initBrainTrackerConnection(null, getParameter("braintrackingconnection"));

            final BrainTrackingDevice device = this.deviceManager.getBrainTrackingDevice();

            if (device != null) {
                device.addTrackingListener(new BrainTrackingListener() {

                    @Override
                    public void newTrackingEvent(BrainTrackingEvent event) {
                        BrowserPluginImpl.this.sessionRecorder.newBrainTrackingEvent(event);
                    }
                });
            }
        }
    }

    /** 
     * Initializes recording of the mouse position
     */
    private void initMouseRecording() {
        // Start a background thread to record the current mouse position. 
        final Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    final SessionRecorder sr = BrowserPluginImpl.this.sessionRecorder;
                    final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
                    final Point point = pointerInfo.getLocation();

                    if (sr != null) sr.updateMousePosition(point.x, point.y);

                    try {
                        Thread.sleep(25);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Gets plugin parameter specifying master file path
     */
    private void obtainMasterFilePath() {
        // Set session path in preferences.
        final String root = getParameter("sessionpath");
        String path = "/" + System.currentTimeMillis() + "/";
        if (root != null) {
            path = root + path;
        } else {
            path = "/tmp/" + path;
        }

        this.masterFilePath = path + "/";

        final String finalPath = path;

        try {
            AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @SuppressWarnings("boxing")
                public Boolean run() {
                    // Create the dir
                    return new File(finalPath).mkdirs();
                }
            });
        } catch (final Exception e) {
            this.logger.warning("Applet security permissions denied creating session directory. You probably forgot to grant the applet some permissions.");
            e.printStackTrace();
        }

        final String recording = getParameter("recordingenabled");
        if (recording != null) {
            this.recordingEnabled = Boolean.parseBoolean(recording);
        }
    }

    /**
     * Obtain additional parameters
     */
    private void processAdditionalParameter() {
        // Initialize the transmission mode. Determines how Java calls Javascript.
        final String tm = getParameter("transmitmode");
        if (tm != null) {
            this.transmitMode = TransmitMode.valueOf(tm.toUpperCase());
        }

        final String cp = getParameter("callbackprefix");
        if (cp != null) {
            this.callbackPrefix = cp;
        } else {
            this.callbackPrefix = "";
        }

        final String extensions = getParameter("extensions");
        if (extensions != null) {

            String extensionpaths[] = new String[] {};

            // Parse extensions
            if (extensions.contains(";")) {
                extensionpaths = extensions.split(";");
            } else {
                if (extensions.length() > 0) {
                    extensionpaths = new String[] { extensions };
                }
            }

            // Use them
            for (final String path : extensionpaths) {
                final URI uri = OS.absoluteBrowserPathToURI(path);
                // final URI uri = new File(path).toURI();
                this.logger.info("Trying to load user defined extension at " + uri);
                this.pluginManager.addPluginsFrom(uri);
            }
        }
    }

    /**
     * 
     */
    private void publishBrokerItems() {
        // Publish information broker items
        this.infoBroker.publish(new StringItem("global:transmitMode", this.transmitMode.toString()));
        this.infoBroker.publish(new StringItem("global:sessionDir", this.masterFilePath));
    }

    /**
     * Store applet paramaters
     */
    private void storeParameters() {
        for (final String[] elem : getParameterInfo()) {
            this.sessionRecorder.setParameter(elem[0], getParameter(elem[0]));
        }
    }

    /**
     * Tell JS some status ...
     * 
     * @param status
     */
    private void tellJSStatus(final String status) {
        executeJSFunction("_augmentedTextStatusFunction", status);
    }

    /**
     * Try to get a connection to the webpage.
     */
    private void tryGetWindow() {
        if (this.window == null) {
            try {
                this.window = JSObject.getWindow(this);
            } catch (final Exception e) {
                //
            }
        }
        return;
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#getExtensions()
     */
    public List<String> getExtensions() {
        return this.extensionManager.getExtensions();
    }

    /* (non-Javadoc)
     * @see de.dfki.km.augmentedtext.browserplugin.browser.browserplugin.BrowserAPI#updateElementMetaInformation(java.lang.String, java.lang.String, java.lang.String)
     */
    public void updateElementMetaInformation(final String id, final String key,
                                             final String value) {
        this.sessionRecorder.updateElementMetaInformation(id, key, value);
        this.pageManager.updateElementMetaInformation(id, key, value);
    }
}