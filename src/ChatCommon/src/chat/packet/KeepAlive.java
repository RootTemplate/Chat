package chat.packet;

import chat.Packet;
import chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KeepAlive implements Packet {
    private boolean answer;

    public KeepAlive() {
        this(false);
    }
    public KeepAlive(boolean answer) {
        this.answer = answer;
    }
    
    @Override
    public void read(DataInputStream dis) throws IOException {
        answer = dis.readBoolean();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_KEEPALIVE);
        dos.writeBoolean(answer);
        dos.flush();
    }
    
    public boolean isAnswer() {
        return answer;
    }
    
}
