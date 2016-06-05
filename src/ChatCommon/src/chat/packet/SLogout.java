package chat.packet;

import chat.Packet;
import chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SLogout implements Packet {
    private String exitedName;
    
    public SLogout() {}
    public SLogout(String exitedName) {
        this.exitedName = exitedName;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        exitedName = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_LOGOUT);
        dos.writeUTF(exitedName);
        dos.flush();
    }
    
    public String getExitedName() {
        return exitedName;
    }
}