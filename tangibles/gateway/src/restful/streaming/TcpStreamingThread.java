/*
 * Master-Thesis work: see https://sites.google.com/site/sifthesis/
 */
package restful.streaming;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tangible.protocols.AbsJsonTCPProtocol;
import tangible.utils.JsonProtocolHelper;
import tangible.utils.exceptions.WrongProtocolJsonSyntaxException;

/**
 *
 * @author leo
 */
public class TcpStreamingThread extends AbstractStreamingThread {

    protected class TcpEventStreamingProtocol
            extends AbsJsonTCPProtocol implements EventStreaming {

        public TcpEventStreamingProtocol(Socket s) throws IOException {
            super(s);
        }

        @Override
        public void sendEvent(JsonElement event) {
            super.sendJsonEventMsg(event);
        }

        @Override
        public void sendEvent(Object event) {
            this.sendEvent(new Gson().toJsonTree(event));
        }

        @Override
        protected JsonElement readJSON() {
            return super.readJSON();
        }

        @Override
        protected void sendJsonCtrlMsg(JsonElement o) {
            super.sendJsonCtrlMsg(o);
        }

        @Override
        protected void handleDisconnection() {
            Logger.getLogger(TcpEventStreamingProtocol.class.getName()).log(Level.INFO, "This should handle a proper disconnection!");
        }
    }
    private ServerSocket _sock;
    private UUID _appuuid;
    private StreamingHolder _streamholder;

    public TcpStreamingThread(UUID appuuid, ServerSocket sSock, StreamingHolder streamHolder) throws IOException {
        super();
        _sock = sSock;
        _appuuid = appuuid;
        _streamholder = streamHolder;
    }

    @Override
    public void run() {
        try {
            Socket clientSock = _sock.accept();
            EventStreaming oldTalk = _talk;
            TcpEventStreamingProtocol tempTalk = new TcpEventStreamingProtocol(clientSock);
            //first read and make sure that this is the correct appuuid
            JsonElement elm = tempTalk.readJSON();
            try {
                elm = JsonProtocolHelper.assertCtrlMsg(elm);
                String uuidStr = JsonProtocolHelper.assertString(elm);
                if (!_appuuid.equals(UUID.fromString(uuidStr))) {
                    tempTalk.sendJsonCtrlMsg(new JsonPrimitive("The given UUID doesn't match the expected one"));
                    Logger.getLogger(TcpStreamingThread.class.getName()).log(Level.INFO, "The given UUID doesn't match the expected one");
                    unregister();
                }
                this._talk = tempTalk;
                this.setupCompleted(oldTalk);
            } catch (WrongProtocolJsonSyntaxException ex) {
                Logger.getLogger(TcpStreamingThread.class.getName()).log(Level.INFO, "a bad json was received");
                tempTalk.sendJsonCtrlMsg(new JsonPrimitive("the received UUID was not the expected one!"));
                unregister();
            }
        } catch (JsonSyntaxException ex) {
            Logger.getLogger(TcpStreamingThread.class.getName()).log(Level.INFO, "received something else than Json");
            unregister();
        } catch (SocketTimeoutException ex) {
            Logger.getLogger(TcpStreamingThread.class.getName()).log(Level.INFO, "StreamingThread timed out!");
        } catch (IOException ex) {
            Logger.getLogger(TcpStreamingThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getPort() {
        return _sock.getLocalPort();
    }

    @Override
    public void unregister() {
        _streamholder.deleteTcpStream(_appuuid);
        // Stop the _talk
    }
}
