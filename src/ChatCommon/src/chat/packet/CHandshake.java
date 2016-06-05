package chat.packet;

import chat.Packet;
import chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CHandshake implements Packet {
    private int version;
    private String chatName;
    
    public CHandshake() {}
    public CHandshake(int version, String name) {
        this.version = version;
        this.chatName = name;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        version = dis.readInt();
        chatName = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(Protocol.CHAT_HANDSHAKE_MAGIC);
        dos.writeInt(version);
        dos.writeUTF(chatName);
        dos.flush();
    }
    
    public int getVersion() {
        return version;
    }
    public String getChatName() {
        return chatName;
    }
    
}
