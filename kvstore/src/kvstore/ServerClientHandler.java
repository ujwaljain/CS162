package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * This NetworkHandler will asynchronously handle the socket connections.
 * Uses a thread pool to ensure that none of its methods are blocking.
 */
public class ServerClientHandler implements NetworkHandler {

    public KVServer kvServer;
    public ThreadPool threadPool;

    /**
     * Constructs a ServerClientHandler with ThreadPool of a single thread.
     *
     * @param kvServer KVServer to carry out requests
     */
    public ServerClientHandler(KVServer kvServer) {
        this(kvServer, 1);
    }

    /**
     * Constructs a ServerClientHandler with ThreadPool of thread equal to
     * the number passed in as connections.
     *
     * @param kvServer KVServer to carry out requests
     * @param connections number of threads in threadPool to service requests
     */
    public ServerClientHandler(KVServer kvServer, int connections) {
        this.kvServer = kvServer;
        this.threadPool = new ThreadPool(connections);
    }

    /**
     * Creates a job to service the request for a socket and enqueues that job
     * in the thread pool. Ignore any InterruptedExceptions.
     *
     * @param client Socket connected to the client with the request
     */
    @Override
    public void handle(Socket client) {
        try {
            threadPool.addJob(new handleJob(client));
        } catch (Exception e) {
            System.out.println("Error handling socket: ");
        }
    }
    
    private class handleJob implements Runnable {
        private Socket client;

        public handleJob(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            KVMessage req = null;
            KVMessage resp = new KVMessage(RESP);
            try {
                req = new KVMessage(client);
                if (PUT_REQ.equals(req.getMsgType())) {
                    kvServer.put(req.getKey(), req.getValue());
                    resp.setMessage(SUCCESS);
                } else if (GET_REQ.equals(req.getMsgType())) {
                    String value = kvServer.get(req.getKey());
                    resp.setKey(req.getKey());
                    resp.setValue(value);
                } else if (DEL_REQ.equals(req.getMsgType())) {
                    kvServer.del(req.getKey());
                    resp.setMessage(SUCCESS);
                }
            } catch (KVException ex) {
                resp = ex.getKVMessage();
            }

            try {
                resp.sendMessage(client);
            } catch (KVException ex) {
                ex.printStackTrace();
            }
        }
    }
}
