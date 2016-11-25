package roottemplate.chat.packet;

import roottemplate.chat.Packet;
import roottemplate.chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CSendMessage implements Packet {
    private String message;
    
    public CSendMessage() {}
    public CSendMessage(String msg) {
        this.message = msg;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        message = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_SEND_MESSAGE);
        dos.writeUTF(message);
        dos.flush();
    }
    
    public String getMessage() {
        return message;
    }
    
}
