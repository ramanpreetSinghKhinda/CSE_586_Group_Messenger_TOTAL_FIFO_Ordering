package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RamanKey is the Key in the Key-Value pair for our ConcurrentHashMap
 * This is designed to be uniquely identify a particular message in the whole system
 */
class RamanKey {
    private String senderPort;
    private int msgNumber;

    RamanKey(String senderPort, int msgNumber) {
        this.senderPort = senderPort;
        this.msgNumber = msgNumber;
    }

    public void setSenderPort(String senderPort) {
        this.senderPort = senderPort;
    }

    public void setMessageNumber(int msgNumber) {
        this.msgNumber = msgNumber;
    }

    public String getSenderPort() {
        return senderPort;
    }


    public int getMessageNumber() {
        return msgNumber;
    }

    public String getKeyCode() {
        return senderPort + msgNumber;
    }

    public int hashCode() {
        int result = 0;
        int code = getKeyCode().hashCode();
        result = 37 * result + code;

        return result;
    }

    public boolean equals(Object obj) {
        if (obj instanceof RamanKey) {
            RamanKey ramanKey = (RamanKey) obj;
            return (ramanKey.getKeyCode().equals(this.getKeyCode()));
        } else {
            return false;
        }
    }
}

/**
 * RamanMessage is the Value in the Key-Value pair for our ConcurrentHashMap
 * This is designed to handle failures from multiple devices
 */
class RamanMessage {
    private float priority;
    private String message;
    private boolean canDeliverNow;
    private AtomicInteger receivedMsgNumber;
    private CopyOnWriteArrayList<String> receivedMsgPorts;

    RamanMessage(float priority, String message, AtomicInteger receivedMsgNumber, boolean canDeliverNow) {
        this.priority = priority;
        this.message = message;
        this.receivedMsgNumber = receivedMsgNumber;
        this.canDeliverNow = canDeliverNow;
        this.receivedMsgPorts = new CopyOnWriteArrayList<String>();
    }

    public void setPriority(float priority) {
        this.priority = priority;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setReceivedMsgNumber(AtomicInteger receivedMsgNumber) {
        this.receivedMsgNumber = receivedMsgNumber;
    }

    public void setCanDeliverNow(boolean canDeliverNow) {
        this.canDeliverNow = canDeliverNow;
    }

    public void initializePorts(CopyOnWriteArrayList<String> ports) {
        receivedMsgPorts.clear();
        receivedMsgPorts.addAll(ports);
    }

    public void removePort(String port) {
        receivedMsgPorts.remove(port);
    }

    public CopyOnWriteArrayList<String> getReceivedMsgPorts() {
        return receivedMsgPorts;
    }

    public float getPriority() {
        return priority;
    }

    public String getMessage() {
        return message;
    }

    public AtomicInteger getReceivedMsgNumber() {
        return receivedMsgNumber;
    }

    public boolean canDeliverNow() {
        return canDeliverNow;
    }
}

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity implements View.OnClickListener {
    private static final String TAG = GroupMessengerActivity.class.getName();

    private Button btnSend;
    private TextView textViewChat;
    private EditText editTextChat;
    private Resources res;

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    /**
     * An int value that may be updated atomically
     * It support lock-free thread-safe operations on a single variable
     * <p/>
     * http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/atomic/package-summary.html
     */
    private final AtomicInteger prioritySequence = new AtomicInteger(0);
    private final AtomicInteger msgSequence = new AtomicInteger(0);
    private final AtomicInteger storeSequence = new AtomicInteger(0);

    /**
     * Socket specific declarations
     */
    private String MY_PORT;
    private static final String REMOTE_PORT0 = "11108";
    private static final String REMOTE_PORT1 = "11112";
    private static final String REMOTE_PORT2 = "11116";
    private static final String REMOTE_PORT3 = "11120";
    private static final String REMOTE_PORT4 = "11124";

    private static final String[] ARRAY_REMOTE_PORTS = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
    private static final int SERVER_PORT = 10000;
    private static final int NUM_OF_DEVICES = 5;

    // For TOTAL-FIFO Ordering Guarantee
    private ConcurrentHashMap<RamanKey, RamanMessage> concurrentHashMap = new ConcurrentHashMap<RamanKey, RamanMessage>();

    // For failure handling
    private CopyOnWriteArrayList<String> REMOTE_PORTS = new CopyOnWriteArrayList<String>();
    private static final String KEY_SENT_MSG_NUMBER = "key_sent_msg_number";
    private static final int TIMEOUT_VALUE = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);
        res = getResources();
        textViewChat = (TextView) findViewById(R.id.text_view_chat);
        textViewChat.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "btn_test" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.btn_test).setOnClickListener(
                new OnPTestClickListener(textViewChat, getContentResolver()));

        /*
         * Registers OnPTestClickListener for "btn_raman_test" in the layout, which is the "Raman Test" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider for debugging purpose.
         */
        findViewById(R.id.btn_raman_test).setOnClickListener(
                new OnPTestClickListener(textViewChat, getContentResolver()));

        editTextChat = (EditText) findViewById(R.id.edit_text_chat);
        btnSend = (Button) findViewById(R.id.btn_send);

        /*
         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        MY_PORT = String.valueOf((Integer.parseInt(portStr) * 2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeRemotePorts();
        btnSend.setOnClickListener(this);

        try {
            //Create a server socket as well as a thread (AsyncTask) that listens on the server port.
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.v(TAG, "Can't create a ServerSocket");
            return;
        }

    }

    private void initializeRemotePorts() {
        for (String remote_port : ARRAY_REMOTE_PORTS) {
            REMOTE_PORTS.add(remote_port);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_send:
                String msgToSend = editTextChat.getText().toString();
                editTextChat.setText("");

                /**
                 * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                 * AsyncTask.THREAD_POOL_EXECUTOR as the ServerTask does.
                 *
                 * To understand the difference, please take a look at :-
                 * http://developer.android.com/reference/android/os/AsyncTask.html
                 */
                final int process = Arrays.asList(ARRAY_REMOTE_PORTS).indexOf(MY_PORT);
                final float priority = prioritySequence.incrementAndGet() + process / 10f;

                final RamanKey ramanKey = new RamanKey(MY_PORT, msgSequence.incrementAndGet());
                final RamanMessage ramanMessage = new RamanMessage(priority, msgToSend, new AtomicInteger(0), false);
                ramanMessage.initializePorts(REMOTE_PORTS);

                replyAll(ramanKey, ramanMessage, true);

                break;
        }
    }

    /**
     * A reply back message sent as a response for the suggesting proposed priority
     */
    public void replyBack(RamanKey ramanKey, RamanMessage ramanMessage) {
        new ClientTask(ramanKey, ramanMessage, false).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "true");
    }

    /**
     * A reply all message sent as a Multicast
     */
    public void replyAll(RamanKey ramanKey, RamanMessage ramanMessage, boolean isInitialMsg) {
        new ClientTask(ramanKey, ramanMessage, isInitialMsg).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "false");
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            /**
             * This class represents a server-side socket that waits for incoming client connections
             *
             * Reference : http://developer.android.com/reference/java/net/ServerSocket.html#ServerSocket()
             */
            ServerSocket serverSocket = sockets[0];
            Socket socket = null;

            while (true) {
                try {
                    Log.v(TAG, "Server is listening on the port : " + MY_PORT);
                    socket = serverSocket.accept();
                    socket.setTcpNoDelay(true);

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final String msg = bufferedReader.readLine();
                    final String receivedMsg[] = msg.split(" : ");

                    final String senderPort = receivedMsg[0];
                    final String receiverPort = receivedMsg[1];
                    final int msgNumber = Integer.parseInt(receivedMsg[2]);
                    final String msgToSend = receivedMsg[3];
                    float priority = Float.parseFloat(receivedMsg[4]);
                    boolean canDeliverNow = Boolean.parseBoolean(receivedMsg[5]);

                    Log.v(TAG, "Received msg :-  Sender Port : " + senderPort + ", Receiver Port : " + receiverPort + ",  Message Number : " + msgNumber + ", Message : " + msgToSend + ", Priority : " + priority + ", canDeliverNow : " + canDeliverNow);

                    final RamanKey ramanSenderKey = new RamanKey(senderPort.trim(), msgNumber);
                    final RamanKey ramanMyKey = new RamanKey(MY_PORT, msgNumber);

                    if (!canDeliverNow) {
                        // This logic corresponds to the Hack defined in client and will act as a both listener and responder
                        if (MY_PORT.contains(senderPort.trim())) {
                            if (null != concurrentHashMap.get(ramanMyKey)) {
                                concurrentHashMap.get(ramanMyKey).getReceivedMsgNumber().incrementAndGet();
                                float newPriority = concurrentHashMap.get(ramanMyKey).getPriority();

                                if (newPriority < priority) {
                                    newPriority = priority;
                                }

                                RamanMessage ramanMessage = new RamanMessage(newPriority, msgToSend, concurrentHashMap.get(ramanMyKey).getReceivedMsgNumber(), false);
                                concurrentHashMap.get(ramanMyKey).removePort(receiverPort.trim());
                                ramanMessage.initializePorts(concurrentHashMap.get(ramanMyKey).getReceivedMsgPorts());
                                concurrentHashMap.put(ramanMyKey, ramanMessage);

                                if (REMOTE_PORTS.size() == concurrentHashMap.get(ramanMyKey).getReceivedMsgNumber().get()) {
                                    Log.v(TAG, "Agreed Priority : " + concurrentHashMap.get(ramanMyKey).getPriority());
                                    replyAll(ramanMyKey, new RamanMessage(concurrentHashMap.get(ramanMyKey).getPriority(), msgToSend, concurrentHashMap.get(ramanMyKey).getReceivedMsgNumber(), true), false);
                                }
                            }
                        } else {
                            final int process = Arrays.asList(ARRAY_REMOTE_PORTS).indexOf(receiverPort.trim());
                            final float proposedPriority = prioritySequence.incrementAndGet() + process / 10f;

                            Log.v(TAG, "Proposed Priority : " + proposedPriority);

                            RamanMessage ramanMessage = new RamanMessage(proposedPriority, msgToSend, new AtomicInteger(0), false);
                            concurrentHashMap.put(ramanSenderKey, ramanMessage);
                            replyBack(ramanSenderKey, ramanMessage);
                        }
                    } else if (!MY_PORT.contains(senderPort.trim())) {
                        concurrentHashMap.put(ramanSenderKey, new RamanMessage(priority, msgToSend, new AtomicInteger(0), true));
                    }

                    boolean canDeliverMy;
                    if (null != concurrentHashMap && concurrentHashMap.size() > 0) {
                        Map<RamanKey, RamanMessage> sortedHashMap = sortByPriority(concurrentHashMap);
                        for (Map.Entry<RamanKey, RamanMessage> entry : sortedHashMap.entrySet()) {
                            Log.v(TAG, "Sorted Priority : " + entry.getValue().getPriority() + ", Sorted Message : " + entry.getValue().getMessage());

                            canDeliverMy = (entry.getValue()).canDeliverNow();
                            Log.v(TAG, "canDeliverMy : " + canDeliverMy);

                            if (canDeliverMy) {
                                String port = entry.getKey().getSenderPort();
                                String message = entry.getValue().getMessage();
                                concurrentHashMap.remove(entry.getKey());
                                publishProgress(new String[]{port, message});
                            } else if (!REMOTE_PORTS.contains(entry.getKey().getSenderPort())) {
                                //deleting msgs sent by failed port
                                concurrentHashMap.remove(entry.getKey());
                            } else {
                                break;
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    Log.v(TAG, "ServerTask SocketTimeoutException");
                } catch (IOException e) {
                    Log.v(TAG, "Error in Server Task IO Exception");
                } finally {
                    if (null != socket && !socket.isClosed()) {
                        Log.v(TAG, "Closing the socket accepted by the Server");
                        try {
                            socket.close();
                        } catch (IOException e) {
                            Log.v(TAG, "Error in Server Task IO Exception when closing socket");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        protected void onProgressUpdate(String... receivedMsg) {
            String senderPort = receivedMsg[0];
            String message = receivedMsg[1];

            ContentValues contentValues = new ContentValues();
            contentValues.put(KEY_FIELD, "" + storeSequence.getAndIncrement());
            contentValues.put(VALUE_FIELD, message);
            getContentResolver().insert(mUri, contentValues);

            int textColor = res.getColor(R.color.my_port);

            if (REMOTE_PORT0.contains(senderPort)) {
                textColor = res.getColor(R.color.remote_port0);
            } else if (REMOTE_PORT1.contains(senderPort)) {
                textColor = res.getColor(R.color.remote_port1);
            } else if (REMOTE_PORT2.contains(senderPort)) {
                textColor = res.getColor(R.color.remote_port2);
            } else if (REMOTE_PORT3.contains(senderPort)) {
                textColor = res.getColor(R.color.remote_port3);
            } else if (REMOTE_PORT4.contains(senderPort)) {
                textColor = res.getColor(R.color.remote_port4);
            }

            String displayedMsg = "Port : " + senderPort + ", Msg : " + message;

            // Displaying Color text so as to differentiate messages sent by different devices
            String colorStrReceived = "<font color='" + textColor + "'>" + displayedMsg + "</font>";
            textViewChat.append("\n ");
            textViewChat.append(Html.fromHtml(colorStrReceived));

            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        private RamanKey ramanKey;
        private RamanMessage ramanMessage;
        private boolean isInitialMsg;

        ClientTask(RamanKey ramanKey, RamanMessage ramanMessage, boolean isInitialMsg) {
            this.ramanKey = ramanKey;
            this.ramanMessage = ramanMessage;
            this.isInitialMsg = isInitialMsg;
        }

        @Override
        protected Void doInBackground(String... msgs) {
            boolean isReplyBackMsg = Boolean.parseBoolean(msgs[0]);

            try {
                if (isReplyBackMsg) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(ramanKey.getSenderPort()));

                    socket.setTcpNoDelay(true);

                    OutputStream outputStream = socket.getOutputStream();
                    PrintWriter printWriter = new PrintWriter(outputStream, true);

                    // Hack: Interchanging sender receiver port for reducing the complexity of logic on Server
                    printWriter.println(ramanKey.getSenderPort() + " : " + MY_PORT + " : " + ramanKey.getMessageNumber() + " : " + ramanMessage.getMessage() + " : " + ramanMessage.getPriority() + " : " + ramanMessage.canDeliverNow());

                    printWriter.close();
                    socket.close();
                } else {
                    concurrentHashMap.put(ramanKey, ramanMessage);
                    Socket[] sockets = new Socket[NUM_OF_DEVICES];

                    for (int i = 0; i < NUM_OF_DEVICES; i++) {
                        if (REMOTE_PORTS.contains(ARRAY_REMOTE_PORTS[i])) {
                            sockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(ARRAY_REMOTE_PORTS[i]));
                            sockets[i].setTcpNoDelay(true);

                            OutputStream outputStream = sockets[i].getOutputStream();
                            PrintWriter printWriter = new PrintWriter(outputStream, true);
                            printWriter.println(ramanKey.getSenderPort() + " : " + ARRAY_REMOTE_PORTS[i] + " : " + ramanKey.getMessageNumber() + " : " + ramanMessage.getMessage() + " : " + ramanMessage.getPriority() + " : " + ramanMessage.canDeliverNow());

                            Log.v(TAG, "Sent Message :-  Sender Port : " + MY_PORT + ", Receiver Port : " + ARRAY_REMOTE_PORTS[i] + ",  Message Number : " + ramanKey.getMessageNumber() + ", Message : " + ramanMessage.getMessage() + ", Priority : " + ramanMessage.getPriority() + ", canDeliverNow : " + ramanMessage.canDeliverNow());

                            printWriter.close();
                            sockets[i].close();
                        }
                    }
                }
            } catch (UnknownHostException e) {
                Log.v(TAG, "ClientTask UnknownHostException");
            } catch (SocketTimeoutException e) {
                Log.v(TAG, "ClientTask UnknownHostException");
            } catch (EOFException e) {
                Log.v(TAG, "ClientTask EOFException");
            } catch (IOException e) {
                Log.v(TAG, "ClientTask IOException");
            } finally {
                if (isInitialMsg) {
                    Message timerMessage = new Message();
                    Bundle bundle = new Bundle();
                    bundle.putInt(KEY_SENT_MSG_NUMBER, ramanKey.getMessageNumber());
                    timerMessage.setData(bundle);
                    myTimerHandler.sendMessageDelayed(timerMessage, TIMEOUT_VALUE);
                }
            }
            return null;
        }

    }

    private final Handler myTimerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (null != msg) {
                    final int msgNumber = msg.getData().getInt(KEY_SENT_MSG_NUMBER);
                    final RamanKey ramanKey = new RamanKey(MY_PORT, msgNumber);
                    if (null != concurrentHashMap.get(ramanKey)) {
                        // The arraylist logic will help us handle failure from multiple devices
                        final CopyOnWriteArrayList<String> failedPortArray = concurrentHashMap.get(ramanKey).getReceivedMsgPorts();

                        for (int i = 0; i < failedPortArray.size(); i++) {
                            Log.v(TAG, "Timer associated with : " + failedPortArray.get(i) + " expired. Declaring it as a failed port.");

                            //1. Declare failed port publicly
                            REMOTE_PORTS.remove(failedPortArray.get(i));
                        }

                        //2. Delete all messages from concurrentHashMap sent by failed port
                        Log.v(TAG, "Agreed Priority : " + concurrentHashMap.get(ramanKey).getPriority());

                        //3. Multicast all other ports to agree on current maximum priority
                        replyAll(ramanKey, new RamanMessage(concurrentHashMap.get(ramanKey).getPriority(), concurrentHashMap.get(ramanKey).getMessage(), concurrentHashMap.get(ramanKey).getReceivedMsgNumber(), true), false);
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "Exception occurred in myTimerHandler");
            }
        }
    };

    /**
     * buildUri() demonstrates how to build a URI for a ContentProvider.
     *
     * @param scheme
     * @param authority
     * @return the URI
     */
    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    private static Map<RamanKey, RamanMessage> sortByPriority(Map<RamanKey, RamanMessage> unSortedMap) {
        List<Map.Entry<RamanKey, RamanMessage>> list = new LinkedList<Map.Entry<RamanKey, RamanMessage>>(unSortedMap.entrySet());

        // we are comparing Map Values to sort with decreasing term frequencies
        Collections.sort(list, new Comparator<Map.Entry<RamanKey, RamanMessage>>() {
            public int compare(Map.Entry<RamanKey, RamanMessage> o1, Map.Entry<RamanKey, RamanMessage> o2) {
                if (o1.getValue().getPriority() < o2.getValue().getPriority()) {
                    return -1;
                } else if (o1.getValue().getPriority() > o2.getValue().getPriority()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        Map<RamanKey, RamanMessage> sortedMap = new LinkedHashMap<RamanKey, RamanMessage>();
        for (Iterator<Map.Entry<RamanKey, RamanMessage>> it = list.iterator(); it.hasNext(); ) {
            Map.Entry<RamanKey, RamanMessage> entry = it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }
}
