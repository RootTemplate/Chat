package roottemplate.chat.packet;

import roottemplate.chat.Packet;
import roottemplate.chat.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SReceiveMessage implements Packet {
    private String sender;
    private String message;
    
    public SReceiveMessage() {}
    public SReceiveMessage(String sender, String msg) {
        this.sender = sender;
        this.message = msg;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        sender = dis.readUTF();
        message = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_RECEIVE_MESSAGE);
        dos.writeUTF(sender);
        dos.writeUTF(message);
        dos.flush();
    }
    
    public String getSender() {
        return sender;
    }
    public String getMessage() {
        return message;
    }
}
