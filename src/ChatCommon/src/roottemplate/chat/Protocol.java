package roottemplate.chat;

import java.io.DataInputStream;
import java.io.IOException;

import roottemplate.chat.packet.*;

public class Protocol {
    public static final int KEEPALIVE_MAX_TIMEOUT = 1500;
    public static final int KEEPALIVE_MAX_TRYES = 2;
    
    public static final String CHAT_HANDSHAKE_MAGIC = "CHAT";
    // Every CLIENT message receives RESULT!
    public static final byte CHAT_ACTION_KEEPALIVE = 0; // ANY SIDE. Args: <boolean> answer
    public static final byte CHAT_ACTION_SEND_MESSAGE = 1; // CLIENT. Args: <String> message.
    public static final byte CHAT_ACTION_RECEIVE_MESSAGE = 2; // SERVER. Args: <long> timestamp, <String> senderName, <String> message
    public static final byte CHAT_ACTION_EXIT = 3; // CLIENT, no responce. Args: <none>
    public static final byte CHAT_ACTION_RESULT = 4; // SERVER. Args: <boolean> result
    public static final byte CHAT_ACTION_LOGIN = 5; // SERVER. Args: <long> timestamp, <String> loggedName
    public static final byte CHAT_ACTION_LOGOUT = 6; // SERVER. Args: <long> timespamp, <String> exitedName
    public static final byte CHAT_ACTION_USERS_LIST_REQUEST = 7; // CLIENT. Args: <none>
    public static final byte CHAT_ACTION_USERS_LIST_RESPONCE = 8; // SERVER. Args: <String[]> users
    
    
    
    public static void verifyChatSocket(DataInputStream dis) throws NotVerifiedException {
        try {
            if(!dis.readUTF().equals(CHAT_HANDSHAKE_MAGIC))
                throw new NotVerifiedException("No magic");
        } catch (IOException ex) {
            throw new NotVerifiedException(ex);
        }
    }
    
    public static Packet getPacketInstanceClient(byte packetAction) {
        switch(packetAction) {
            case CHAT_ACTION_KEEPALIVE:
                return new KeepAlive();
            case CHAT_ACTION_RECEIVE_MESSAGE:
                return new SReceiveMessage();
            case CHAT_ACTION_RESULT:
                return new SResult();
            case CHAT_ACTION_LOGIN:
                return new SLogin();
            case CHAT_ACTION_LOGOUT:
                return new SLogout();
            case CHAT_ACTION_USERS_LIST_RESPONCE:
                return new SUsersListResponce();
            default:
                return null;
        }
    }
    public static Packet getPacketInstanceServer(byte packetAction) {
        switch(packetAction) {
            case CHAT_ACTION_KEEPALIVE:
                return new KeepAlive();
            case CHAT_ACTION_SEND_MESSAGE:
                return new CSendMessage();
            case CHAT_ACTION_EXIT:
                return new CExit();
            case CHAT_ACTION_USERS_LIST_REQUEST:
                return new CUsersListRequest();
            default:
                return null;
        }
    }
}
