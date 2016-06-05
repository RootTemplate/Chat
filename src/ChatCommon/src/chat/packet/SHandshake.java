package chat.packet;

import chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SHandshake extends SResult {
    public SHandshake() {}
    public SHandshake(boolean result) {
        super(result);
    }
    public SHandshake(boolean result, String error) {
        super(result, error);
    }
    
    @Override
    public void read(DataInputStream dis) throws IOException {
        dis.readByte(); // Hack
        super.read(dis);
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(Protocol.CHAT_HANDSHAKE_MAGIC);
        super.write(dos);
    }
}
