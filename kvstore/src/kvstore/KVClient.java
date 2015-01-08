package kvstore;

import static kvstore.KVConstants.DEL_REQ;
import static kvstore.KVConstants.ERROR_COULD_NOT_CONNECT;
import static kvstore.KVConstants.ERROR_COULD_NOT_CREATE_SOCKET;
import static kvstore.KVConstants.ERROR_INVALID_KEY;
import static kvstore.KVConstants.ERROR_INVALID_VALUE;
import static kvstore.KVConstants.GET_REQ;
import static kvstore.KVConstants.PUT_REQ;
import static kvstore.KVConstants.RESP;
import static kvstore.KVConstants.SUCCESS;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Client API used to issue requests to key-value server.
 */
public class KVClient implements KeyValueInterface {

    public String server;
    public int port;

    /**
     * Constructs a KVClient connected to a server.
     *
     * @param server is the DNS reference to the server
     * @param port is the port on which the server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    /**
     * Creates a socket connected to the server to make a request.
     *
     * @return Socket connected to server
     * @throws KVException if unable to create or connect socket
     */
    public Socket connectHost() throws KVException {
        Socket socket;
        try {
            socket = new Socket(server, port);
        } catch (Exception ex) {
            throw new KVException(KVConstants.ERROR_COULD_NOT_CONNECT);
        }
        return socket;
    }

    /**
     * Closes a socket.
     * Best effort, ignores error since the response has already been received.
     *
     * @param  sock Socket to be closed
     */
    public void closeHost(Socket sock) {
        try {
            sock.close();
        } catch (Exception e) {
            // TODO: handle IOException
        }
    }

    /**
     * Issues a PUT request to the server.
     *
     * @param  key String to put in server as key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void put(String key, String value) throws KVException {
        Socket socket = null;
        try {
            socket = connectHost();
            KVMessage kvm = new KVMessage(KVConstants.PUT_REQ);
            kvm.setKey(key);
            kvm.setValue(value);
            kvm.sendMessage(socket);

            // get response from socket.
            KVMessage resp = new KVMessage(socket);
        } catch (KVException ex) {
            throw ex;
        } finally {
            closeHost(socket);
        }
    }

    /**
     * Issues a GET request to the server.
     *
     * @param  key String to get value for in server
     * @return String value associated with key
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public String get(String key) throws KVException {
        Socket socket = null;
        try {
            socket = connectHost();
            KVMessage kvm = new KVMessage(KVConstants.GET_REQ);
            kvm.setKey(key);
            kvm.sendMessage(socket);

            // get response from socket.
            KVMessage resp = new KVMessage(socket);
            if (resp.getMessage() != null)
                throw new KVException(resp);
            return resp.getValue();
        } catch (KVException ex) {
            throw ex;
        } finally {
            closeHost(socket);
        }
    }

    /**
     * Issues a DEL request to the server.
     *
     * @param  key String to delete value for in server
     * @throws KVException if the request was not successful in any way
     */
    @Override
    public void del(String key) throws KVException {
        Socket socket = null;
        try {
            socket = connectHost();
            KVMessage kvm = new KVMessage(KVConstants.DEL_REQ);
            kvm.setKey(key);
            kvm.sendMessage(socket);

            // get response from socket
            KVMessage resp = new KVMessage(socket);
            String msg = resp.getMessage();
            if (msg == null || !msg.equals(KVConstants.SUCCESS))
                throw new KVException(resp);
        } catch (KVException ex) {
            throw ex;
        } finally {
            closeHost(socket);
        }
    }
}
