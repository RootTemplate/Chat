package chat.packet;

import chat.Packet;
import chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SLogin implements Packet {
    private String loggedName;
    
    public SLogin() {}
    public SLogin(String loggedName) {
        this.loggedName = loggedName;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        loggedName = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_LOGIN);
        dos.writeUTF(loggedName);
        dos.flush();
    }
    
    public String getLoggedName() {
        return loggedName;
    }
}
