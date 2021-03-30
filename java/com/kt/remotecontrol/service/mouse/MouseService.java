package com.kt.remotecontrol.service.mouse;

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.Log;
import com.kt.remotecontrol.interlock.ProxyManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class MouseService {

    private static final Log LOG = new Log("MouseService");

    private final String TYPE_AUTH = "auth";
    private final String TYPE_POINT = "point";
    private final String TYPE_KEEP = "keep";
    private final String TYPE_GAME = "game";

    private final int MAX_CONCURRENT_SERVICE = 10;

    private static int key = 57; // 기존 -> SAID 전부 XOR 한 값
    private static ServerSocket serverSocket;

    private boolean isAlive = true;
    private int sessionID = 0;
    private ThreadPool threadPool;

    public MouseService(int port) throws IOException {

        setEncryptDecryptKey();

        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(Integer.MAX_VALUE);

        threadPool = new ThreadPool(MAX_CONCURRENT_SERVICE, 1); // 10 개 쓰레드

        Thread t = new Thread(new Runnable() {

            public void run() {

                while (isAlive) {
//                    if (Log.INCLUDE) {
//                        LOG.message("Wait for client connected");
//                    }
                    try {
                        TCPSession session = new TCPSession(serverSocket.accept(), ++sessionID);

//                        if (Log.INCLUDE) {
//                            LOG.message("### one client connected, tcp session = " + session.toString());
//                        }

                        if (threadPool.isFull()) {
                            LOG.message("MouseServiceD thread pool maximum, skip");
                            session.cleanup();
                        } else {
                            threadPool.execute(session);
                        }
                    } catch (SocketTimeoutException ste) {
//                        if (Log.INCLUDE) {
//                            LOG.message("Wait Accept timed out");
//                        }
                    } catch (SocketException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                LOG.message("MouseServiceD thread End, now it is impossible to server remote control");
            }

        }, "MouseServiceD Listen thread");
        // t.setDaemon( true );
        t.start();

        LOG.message("create, started");
    }

    public void destroy() {
        LOG.message("destroy");
        isAlive = false; // 쓰레드 종료되도록.

        closeServerSockerWhenActive();

        threadPool.stop();
    }

    private void closeServerSockerWhenActive() {
        if (serverSocket == null) {
            return ;
        }

        try {
            serverSocket.close(); // 서버소켓 닫는다.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setEncryptDecryptKey() {

        String said = ProxyManager.otherHandler().getSAID();
        short result = 0;
        byte[] bytes = said.getBytes();

        for (int i = 0; i < bytes.length; i++) {
            if (i == 0) {
                result = bytes[i];
            } else {
                result = (short) (result ^ bytes[i]);
            }
        }

        key = result;

        if (Log.INCLUDE) {
            LOG.message("setEncryptDecryptKey, key binary=["
                    + Integer.toBinaryString(key) + "], hex=[" + Integer.toHexString(key)
                    + "], decimal=" + key);
        }
    }

    /**
     * Handles one session, i.e. parses the TCP request and returns the
     * response.
     */
    private class TCPSession implements Runnable {

        private static final int PACKET_SIZE = 64;

        private int sessionID;

        private OutputStream os;
        private InputStream is;
        private Socket clientSocket;;


        public TCPSession(Socket s, int sessionID) throws SocketException {

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] TCP session Created >>>>>>>>>>>>>>>>>>>>>>>, session=" + sessionID);
            }

            clientSocket = s;
            clientSocket.setKeepAlive(true);
            clientSocket.setSoTimeout(60000);

            this.sessionID = sessionID;
        }

        /**
         * 세션이 만들어 진 후 요청을 처리하는 루틴
         */
        public void run() {

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] TCP session START >>>>>>>>>>>>>>>>>>>>>>>, session="
                        + sessionID);
            }

            try {
                os = clientSocket.getOutputStream();
                is = clientSocket.getInputStream();

                while (true) {
                    processClientRequest();
                }
            } catch (IOException e) {
                if (Log.INCLUDE) {
                    LOG.message("[TCPSession] I/O error while processing client's request. seesionID="
                            + sessionID);
                }
            }

            cleanup();

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] TCP session END >>>>>>>>>>>>>>>>>>>>>>>, sessionID="
                        + sessionID);
            }
        }

        private void processClientRequest() throws IOException {

            int sofar = 0;
            byte[] buf = new byte[PACKET_SIZE];

            while (sofar < PACKET_SIZE) {

                try {
                    int read = is.read(buf, sofar, PACKET_SIZE - sofar);

                    if (Log.INCLUDE) {
                        LOG.message("[TCPSession] processClientRequest, read=" + read
                                + ", session=" + sessionID);
                    }

                    if (read == -1) {
                        throw new IOException("End of stream");
                    }

                    sofar += read;
                } catch (SocketTimeoutException e) {
                    if (Log.INCLUDE) {
                        LOG.message("[TCPSession] processClientRequest, SocketTimeoutException, retry, sessionID="
                            + sessionID);
                    }
                }
            }

            decode(buf);

            String msg = new String(buf, 0, PACKET_SIZE);

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] processClientRequest, input=[" + msg + "], session="
                        + sessionID);
            }

            processService(msg);
        }

        private void processService(String msg) throws IOException {

            StringTokenizer st = new StringTokenizer(msg, CharConstant.CHAR_VERTICAL);

            // 공통 (일련번호, 타임스탬프, 타입)
            String no = st.nextToken();
            String timestamp = st.nextToken();
            String type = st.nextToken();

            if (type.equalsIgnoreCase(TYPE_AUTH)) {
                String version = st.nextToken();
                String stbID = st.nextToken(); // SAID

                processAuth(stbID, no, timestamp, type, sessionID);
            } else if (type.equalsIgnoreCase(TYPE_POINT)) {
                String actionType = st.nextToken();

                processPoint(actionType, st.nextToken());
            } else if (type.equalsIgnoreCase(TYPE_KEEP)) {
                processKeep(no, timestamp, type);
            } else if (type.equalsIgnoreCase(TYPE_GAME)) {

                String player = st.nextToken();
                String keyType = st.nextToken();
                String keyValue = st.nextToken();

                processGame(keyType, keyValue);
            } else { // Uknown Type
                if (Log.INCLUDE) {
                    LOG.message("[TCPSession] processClientRequest, NOT Support MOUSE");
                }
            }
        }

        private void processAuth(String stbID, String no, String timestamp, String type, int sessionID) throws IOException {
            String said = com.kt.remotecontrol.interlock.ProxyManager.otherHandler().getSAID();

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] processClientRequest, stbID=" + stbID + ", SAID="
                        + said + ", session=" + sessionID);
            }

            // send Auth
            int result = 0;
            if (!stbID.equalsIgnoreCase(said)) {
                result = 1;
            }

            StringBuffer sb = new StringBuffer();
            sb.append(no).append(CharConstant.CHAR_VERTICAL);
            sb.append(timestamp).append(CharConstant.CHAR_VERTICAL);
            sb.append(type).append(CharConstant.CHAR_VERTICAL);
            sb.append(result).append(CharConstant.CHAR_VERTICAL);

            sendMessage(sb);

            if (result != 0) {
                throw new IOException("AuthFail!!, session=" + sessionID);
            }
        }

        private void processPoint(String type, String position) {
            if (OTGController.getInstance().isNotSupportMouse()) {
                if (Log.INCLUDE) {
                    LOG.message("[TCPSession] processClientRequest, not mouse support");
                }
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(position, CharConstant.CHAR_COMMA);
            String deltaX = tokenizer.nextToken();
            String deltaY = tokenizer.nextToken();

            boolean success = OTGController.getInstance().sendPointMessage(type, deltaX, deltaY);
        }

        private void processKeep(String no, String timestamp, String type) throws IOException {
            StringBuffer sb = new StringBuffer();
            sb.append(no).append(CharConstant.CHAR_VERTICAL);
            sb.append(timestamp).append(CharConstant.CHAR_VERTICAL);
            sb.append(type).append(CharConstant.CHAR_VERTICAL);
            sb.append(0).append(CharConstant.CHAR_VERTICAL);

            sendMessage(sb);
        }

        private void processGame(String type, String value) {

            try {
                int btntype = Integer.parseInt(type);
                int code = Integer.parseInt(value);
                boolean success = false;

                if (btntype == 1) {
                    success = com.kt.remotecontrol.interlock.ProxyManager.eventGenerator().pressedKey(code);
                } else if (btntype == 2) {
                    success = ProxyManager.eventGenerator().releasedKey(code);
                } else {
                    if (Log.INCLUDE) {
                        LOG.message("[TCPSession] processClientRequest, unknown type=" + type);
                    }
                }

                if (Log.INCLUDE) {
                    LOG.message("[TCPSession] processClientRequest, key=" + code + ", send=" + success);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(StringBuffer sb) throws IOException {

            if (Log.INCLUDE) {
                LOG.message("[TCPSession] sendMessage, msg=[" + sb.toString() + "], session=" + sessionID);
            }

            char[] dst = new char[PACKET_SIZE];
            sb.getChars(0, sb.length(), dst, 0);

            encode(dst);

            OutputStreamWriter osw = new OutputStreamWriter(os);
            osw.write(dst);
            osw.flush();
        }

        private void cleanup() {
            if (Log.INCLUDE) {
                LOG.message("[TCPSession] cleanup, session=" + sessionID);
            }

            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOG.error("[TCPSession] cleanup: I/O error while closing inputStream. sessionID="
                            + sessionID);
                    e.printStackTrace();
                }
            }

            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    LOG.error("[TCPSession] cleanup: I/O error while closing outputStream. sessionID="
                            + sessionID);
                    e.printStackTrace();
                }
            }

            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOG.error("[TCPSession] cleanup: I/O error while closing clientSocket. sessionID="
                            + sessionID);
                    e.printStackTrace();
                }
            }
        }

        private void encode(char[] buf) {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (char) ((buf[i]) ^ key);
            }
        }

        private void decode(byte[] buf) {
            for (int i = 0; i < buf.length; i++) {
                buf[i] = (byte) (buf[i] ^ key);
            }
        }
    };
}

class ThreadPool {

    private static final Log LOG = new Log("ThreadPool");

    private BlockingQueue taskQueue = null;
    private HashSet/*<PoolThread>*/threads = new HashSet/*<PoolThread>*/();
    private boolean isStopped = false;

    private int noOfThreads;

    public ThreadPool(int noOfThreads, int maxNoOfTasks) {
        taskQueue = new BlockingQueue(maxNoOfTasks);

        this.noOfThreads = noOfThreads;
    }

    public synchronized void execute(Runnable task) {
        if (this.isStopped) {
            throw new IllegalStateException("ThreadPool is stopped");
        }

        int size = threads.size();
        if (size < noOfThreads) {
            PoolThread thread = new PoolThread(size + "", taskQueue, this);
            threads.add(thread);

            LOG.message("execute, add thread : " + thread);
            thread.start();

            if (OTGController.getInstance().isDisabledMouseDevice()) {
                OTGController.getInstance().enableMouseDevice();
            }
        }

        try {
            this.taskQueue.enqueue(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // remove stopped thread
    public void removeThread(PoolThread thread) {
        if (Log.INCLUDE) {
            LOG.message("removeThread : " + thread);
        }
        threads.remove(thread);

        if (threads.size() == 0) {
            OTGController.getInstance().disableMouseDevice();
        }
    }

    public synchronized void stop() {
        this.isStopped = true;

        Iterator it = threads.iterator();
        while (it.hasNext()) {
            PoolThread thread = (PoolThread) it.next();
            thread.interrupt();
        }

        threads.clear();
    }

    /**
     * 현재 쓰레드풀이 꽉 찼는가? (모든 쓰레드가 클라이언트와 통신을 하고 있는가?)
     * @return
     */
    public synchronized boolean isFull() {
        return threads.size() == noOfThreads;
    }
}

class BlockingQueue {
    private List queue = new LinkedList();
    private int limit = 10;

    public BlockingQueue(int limit) {
        this.limit = limit;
    }

    public synchronized void enqueue(Object item) throws InterruptedException {
        while (queue.size() == limit) {
            wait();
        }

        if (queue.size() == 0) {
            notifyAll();
        }

        queue.add(item);
    }

    public synchronized Object dequeue() throws InterruptedException {
        while (queue.size() == 0) {
            wait();
        }

        if (queue.size() == limit) {
            notifyAll();
        }

        return queue.remove(0);
    }

    public synchronized boolean isEmpty() {
        return queue.size() == 0;
    }
}

class PoolThread extends Thread {
    private static final Log LOG = new Log("PoolThread");

    private BlockingQueue taskQueue = null;
    private boolean isStopped = false;

    private ThreadPool threadPool;

    public PoolThread(String name, BlockingQueue queue, ThreadPool threadPool) {
        super(name);

        taskQueue = queue;
        this.threadPool = threadPool;
    }

    public void run() {
        while (!isStopped()) {
            try {
                Runnable runnable = (Runnable) taskQueue.dequeue();
                runnable.run();

                if (taskQueue.isEmpty()) {
                    isStopped = true;
                    break;
                }
            } catch (Exception e) {
                // log or otherwise report exception, but keep pool thread alive.
                e.printStackTrace();
            }
        }

        LOG.message("run, end thread");

        threadPool.removeThread(this);
    }

    public synchronized void interrupt() {
        isStopped = true;
        super.interrupt(); //break pool thread out of dequeue() call.
    }

    public synchronized boolean isStopped() {
        return isStopped;
    }
}
