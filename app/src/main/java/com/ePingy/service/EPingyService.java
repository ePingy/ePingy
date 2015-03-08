package com.ePingy.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.ePingy.ChatInterface;
import com.ePingy.R;
import com.ePingy.app.EPingyApplication;
import com.ePingy.model.Session;
import com.ePingy.app.AppConstants;
import com.ePingy.ui.activity.DeviceListActivity;
import com.ePingy.ui.activity.SplashActivity;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.MessageContext;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by jhansi on 06/03/15.
 */
public class EPingyService extends Service {

    Map<String, Session> sessions = new HashMap<>();
    private EPingyApplication application;
    private final static String TAG = EPingyService.class.toString();
    private final DeviceConnectBinder binder = new DeviceConnectBinder();
    private BusAttachment mBus = new BusAttachment(AppConstants.PACKAGE_NAME, BusAttachment.RemoteMessage.Receive);
    private BusAttachmentState mBusAttachmentState = BusAttachmentState.DISCONNECTED;
    private ChatBusListener mBusListener = new ChatBusListener();
    private int mHostSessionId = -1;
    private static final String OBJECT_PATH = "/pingService";
    private static final String NAME_PREFIX = "com.epingy.ping";
    private static final short CONTACT_PORT = 27;
    private HostChannelState mHostChannelState = HostChannelState.IDLE;
    private ChatInterface mHostChatInterface = null;
    private ChatService mChatService = new ChatService();
    private Map<Integer, String> predefinedMessageSignals = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        application = (EPingyApplication) getApplication();
        startNotification();
        connect();
        startDiscovery();
        initMessageSignals();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binder.leaveFromNetwork(application.getDeviceName());
    }

    @Override
    public boolean onUnbind(Intent intent) {
        binder.leaveFromNetwork(application.getDeviceName());
        return super.onUnbind(intent);
    }

    private void initMessageSignals() {
        predefinedMessageSignals.put(AppConstants.COFFEE_MSG_ID, getString(R.string.coffee));
        predefinedMessageSignals.put(AppConstants.LUNCH_MSG_ID, getString(R.string.lunch));
        predefinedMessageSignals.put(AppConstants.WAITING_MSG_ID, getString(R.string.waiting));
        predefinedMessageSignals.put(AppConstants.COMETOMYDESK_MSG_ID, getString(R.string.comeToMyDesk));
        predefinedMessageSignals.put(AppConstants.BEER_MSG_ID, getString(R.string.beer));
        predefinedMessageSignals.put(AppConstants.SHOPPING_MSG_ID, getString(R.string.shopping));
        predefinedMessageSignals.put(AppConstants.MOVIE_TONIGHT_MSG_ID, getString(R.string.movieTonight));
    }

    private void startNotification() {
        CharSequence title = "EasyReach";
        CharSequence message = "Easy ping service.";
        Intent intent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new Notification(R.drawable.ic_launcher, null, System.currentTimeMillis());
        notification.setLatestEventInfo(this, title, message, pendingIntent);
        notification.flags |= Notification.DEFAULT_SOUND | Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
        Log.i(TAG, "onCreate(): startForeground()");
        startForeground(AppConstants.NOTIFICATION_ID, notification);
    }

    private void connect() {
        Log.i(TAG, "doConnect()");
        org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
        mBus.useOSLogging(true);
        mBus.setDebugLevel("ALLJOYN_JAVA", 7);
        mBus.registerBusListener(mBusListener);

        /*
         * To make a service available to other AllJoyn peers, first
         * register a BusObject with the BusAttachment at a specific
         * object path.  Our service is implemented by the ChatService
         * BusObject found at the "/chatService" object path.
         */
        Status status = mBus.registerBusObject(mChatService, OBJECT_PATH);
        if (Status.OK != status) {
            application.alljoynError(EPingyApplication.Module.HOST, "Unable to register the chat bus object: (" + status + ")");
            return;
        }

        status = mBus.connect();
        if (status != Status.OK) {
            application.alljoynError(EPingyApplication.Module.GENERAL, "Unable to connect to the bus: (" + status + ")");
            return;
        }

        status = mBus.registerSignalHandlers(this);
        if (status != Status.OK) {
            application.alljoynError(EPingyApplication.Module.GENERAL, "Unable to register signal handlers: (" + status + ")");
            return;
        }

        mBusAttachmentState = BusAttachmentState.CONNECTED;
    }

    private void disconnect() {

    }

    private void startDiscovery() {
        Log.i(TAG, "doStartDiscovery()");
        assert (mBusAttachmentState == BusAttachmentState.CONNECTED);
        Status status = mBus.findAdvertisedName(NAME_PREFIX);
        if (status == Status.OK) {
            mBusAttachmentState = BusAttachmentState.DISCOVERING;
            return;
        } else {
            application.alljoynError(EPingyApplication.Module.USE, "Unable to start finding advertised names: (" + status + ")");
            return;
        }
    }

    public static enum BusAttachmentState {
        DISCONNECTED, /**
         * The bus attachment is not connected to the AllJoyn bus
         */
        CONNECTED, /**
         * The  bus attachment is connected to the AllJoyn bus
         */
        DISCOVERING        /** The bus attachment is discovering remote attachments hosting chat channels */
    }

    public class DeviceConnectBinder extends Binder {

        public void joinNetwork(String deviceName) {
            requestName(deviceName);
            bindSession();
            advertise(deviceName);
        }

        public void leaveFromNetwork(String deviceName) {
            cancelAdvertise(deviceName);
            unBindSession(deviceName);
            releaseName(deviceName);
        }

        public void pingDevice(String targetDevice, int messageId, String senderDevice) {
            if (sessions.containsKey(targetDevice) && isSessionActive(targetDevice)) {
                pingJoinedDevice(targetDevice, messageId, senderDevice);
            } else {
                joinSession(targetDevice);
                if (isSessionActive(targetDevice)) {
                    pingJoinedDevice(targetDevice, messageId, senderDevice);
                }
            }
        }

        private void joinSession(String targetDevice) {

            String wellKnownName = NAME_PREFIX + "." + targetDevice;
            short contactPort = CONTACT_PORT;
            SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);
            Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

            Status status = mBus.joinSession(wellKnownName, contactPort, sessionId, sessionOpts, new SessionListenerImpl(targetDevice));

            int mUseSessionId = -1;
            if (status == Status.OK) {
                mUseSessionId = sessionId.value;
                Log.i(TAG, "doJoinSession(): use sessionId is " + mUseSessionId);
            } else {
                application.alljoynError(EPingyApplication.Module.USE, "Unable to join chat session: (" + status + ")");
                return;
            }

            SignalEmitter emitter = new SignalEmitter(mChatService, mUseSessionId, SignalEmitter.GlobalBroadcast.Off);
            ChatInterface mChatInterface = emitter.getInterface(ChatInterface.class);
            UseChannelState mUseChannelState = UseChannelState.JOINED;

            Session session = new Session();
            session.setDeviceName(targetDevice);
            session.setChatInterface(mChatInterface);
            session.setUseChannelState(mUseChannelState);
            session.setId(mUseSessionId);
            sessions.put(targetDevice, session);
        }

        private boolean isSessionActive(String targetDevice) {
            Session session = sessions.get(targetDevice);
            return session.getUseChannelState() == UseChannelState.JOINED;
        }

        private void pingJoinedDevice(String targetDevice, int messageId, String senderDevice) {
            try {
                Session session = sessions.get(targetDevice);
                session.getChatInterface().Ping(messageId, senderDevice);
            } catch (BusException e) {
                application.alljoynError(EPingyApplication.Module.USE, "Bus exception while sending message: (" + e + ")");
            }
        }

        private void requestName(String deviceName) {
            Log.i(TAG, "doRequestName()");

        /*
         * In order to request a name, the bus attachment must at least be
         * connected.
         */
            int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
            assert (stateRelation >= 0);

        /*
         * We depend on the user interface and model to work together to not
         * get this process started until a valid name is set in the channel name.
         */
            String wellKnownName = NAME_PREFIX + "." + deviceName;
            Status status = mBus.requestName(wellKnownName, BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE);
            if (status == Status.OK) {
                mHostChannelState = HostChannelState.NAMED;
                application.hostSetChannelState(mHostChannelState);
            } else {
                application.alljoynError(EPingyApplication.Module.USE, "Unable to acquire well-known name: (" + status + ")");
            }
        }

        private void bindSession() {
            Log.i(TAG, "doBindSession()");
            Mutable.ShortValue contactPort = new Mutable.ShortValue(CONTACT_PORT);
            SessionOpts sessionOpts = new SessionOpts(SessionOpts.TRAFFIC_MESSAGES, true, SessionOpts.PROXIMITY_ANY, SessionOpts.TRANSPORT_ANY);

            Status status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {

                public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                    Log.i(TAG, "SessionPortListener.acceptSessionJoiner(" + sessionPort + ", " + joiner + ", " + sessionOpts.toString() + ")");
                /*
                 * Accept anyone who can get our contact port correct.
                 */
                    if (sessionPort == CONTACT_PORT) {
                        return true;
                    }
                    return false;
                }

                public void sessionJoined(short sessionPort, int id, String joiner) {
                    Log.i(TAG, "SessionPortListener.sessionJoined(" + sessionPort + ", " + id + ", " + joiner + ")");
                    mHostSessionId = id;
                    SignalEmitter emitter = new SignalEmitter(mChatService, id, SignalEmitter.GlobalBroadcast.Off);
                    mHostChatInterface = emitter.getInterface(ChatInterface.class);
                }
            });

            if (status == Status.OK) {
                mHostChannelState = HostChannelState.BOUND;
                application.hostSetChannelState(mHostChannelState);
            } else {
                application.alljoynError(EPingyApplication.Module.HOST, "Unable to bind session contact port: (" + status + ")");
                return;
            }
        }

        private void advertise(String deviceName) {
            Log.i(TAG, "doAdvertise()");

        /*
         * We depend on the user interface and model to work together to not
         * change the name out from under us while we are running.
         */
            String wellKnownName = NAME_PREFIX + "." + deviceName;
            Status status = mBus.advertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);

            if (status == Status.OK) {
                mHostChannelState = HostChannelState.ADVERTISED;
                application.hostSetChannelState(mHostChannelState);
            } else {
                application.alljoynError(EPingyApplication.Module.HOST, "Unable to advertise well-known name: (" + status + ")");
                return;
            }
        }

        private void cancelAdvertise(String deviceName) {
            String wellKnownName = NAME_PREFIX + "." + deviceName;
            Status status = mBus.cancelAdvertiseName(wellKnownName, SessionOpts.TRANSPORT_ANY);
            if (status != Status.OK) {
                application.alljoynError(EPingyApplication.Module.HOST, "Unable to cancel advertisement of well-known name: (" + status + ")");
                return;
            }
            mHostChannelState = HostChannelState.BOUND;
            application.hostSetChannelState(mHostChannelState);
        }

        private void unBindSession(String deviceName) {
            mBus.unbindSessionPort(CONTACT_PORT);
            mHostChatInterface = null;
            mHostChannelState = HostChannelState.NAMED;
            application.hostSetChannelState(mHostChannelState);
        }

        private void releaseName(String deviceName) {
            int stateRelation = mBusAttachmentState.compareTo(BusAttachmentState.DISCONNECTED);
            String wellKnownName = NAME_PREFIX + "." + deviceName;
            mBus.releaseName(wellKnownName);
            mHostChannelState = HostChannelState.IDLE;
            application.hostSetChannelState(mHostChannelState);
        }
    }

    private class SessionListenerImpl extends SessionListener {
        private String deviceName;

        public SessionListenerImpl(String deviceName) {
            this.deviceName = deviceName;
        }

        @Override
        public void sessionLost(int sessionId, int reason) {
            Log.i(TAG, "BusListener.sessionLost(sessionId=" + sessionId + ",reason=" + reason + ")");
            sessions.remove(deviceName);
            application.alljoynError(EPingyApplication.Module.USE, "The chat session has been lost");
        }
    }

    private class ChatBusListener extends BusListener {

        public void foundAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.foundAdvertisedName(" + name + ")");
            application.addDevice(name);
        }


        public void lostAdvertisedName(String name, short transport, String namePrefix) {
            Log.i(TAG, "mBusListener.lostAdvertisedName(" + name + ")");
            application.removeDevice(name);
        }
    }

    /**
     * Our chat messages are going to be Bus Signals multicast out onto an
     * associated session.  In order to send signals, we need to define an
     * AllJoyn bus object that will allow us to instantiate a signal emmiter.
     */
    class ChatService implements ChatInterface, BusObject {
        /**
         * Intentionally empty implementation of Chat method.  Since this
         * method is only used as a signal emitter, it will never be called
         * directly.
         */
        public void Ping(int MessageId, String deviceName) throws BusException {
            Log.d("", "***Got a message:" + deviceName);
        }
    }


    /**
     * The signal handler for messages received from the AllJoyn bus.
     * <p/>
     * Since the messages sent on a chat channel will be sent using a bus
     * signal, we need to provide a signal handler to receive those signals.
     * This is it.  Note that the name of the signal handler has the first
     * letter capitalized to conform with the DBus convention for signal
     * handler names.
     */
    @BusSignalHandler(iface = "com.epingy.ping", signal = "Ping")
    public void Ping(int MessageId, String deviceName) {
        String uniqueName = mBus.getUniqueName();
        MessageContext ctx = mBus.getMessageContext();
        Log.i(TAG, "Chat(): message sessionId is " + ctx.sessionId);

        /*
         * Always drop our own signals which may be echoed back from the system.
         */
        if (ctx.sender.equals(uniqueName)) {
            Log.i(TAG, "Chat(): dropped our own signal received on session " + ctx.sessionId);
            return;
        }

        /*
         * Notify ping from other contacts.
         */
        if (ctx.sessionId == mHostSessionId) {
            Log.i(TAG, "Chat(): uniqueName pinged you on hosted session " + ctx.sessionId);
            if (predefinedMessageSignals.containsKey(MessageId)) {
                String message = deviceName + ", " + predefinedMessageSignals.get(MessageId);
                notifyPing(message);
            }
            return;
        }
    }

    private void notifyPing(String message) {
        Intent myIntent = new Intent(this, DeviceListActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                myIntent,
                0);

        Notification myNotification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(com.ePingy.R.string.newMessage))
                .setContentText(message)
                .setTicker(getString(com.ePingy.R.string.notification))
                .setWhen(System.currentTimeMillis())
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .build();


        int MY_NOTIFICATION_ID = generateRandomNumber();
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(MY_NOTIFICATION_ID, myNotification);
    }

    public static enum HostChannelState {
        IDLE, /**
         * There is no hosted chat channel
         */
        NAMED, /**
         * The well-known name for the channel has been successfully acquired
         */
        BOUND, /**
         * A session port has been bound for the channel
         */
        ADVERTISED, /**
         * The bus attachment has advertised itself as hosting an chat channel
         */
        CONNECTED       /** At least one remote device has connected to a session on the channel */
    }

    public static enum UseChannelState {
        IDLE, /**
         * There is no used chat channel
         */
        JOINED,            /** The session for the channel has been successfully joined */
    }

    private int generateRandomNumber() {
        Random random = new Random();
        return random.nextInt();
    }

    static {
        Log.i(TAG, "System.loadLibrary(\"alljoyn_java\")");
        System.loadLibrary("alljoyn_java");
    }

}