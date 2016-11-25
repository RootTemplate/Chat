package roottemplate.chat.packet;

import roottemplate.chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SHandshake extends SResult {
    private int maxMessageLength = -1;

    public SHandshake() {}
    public SHandshake(boolean result, int maxMessageLength) {
        super(result);
        this.maxMessageLength = maxMessageLength;
    }
    public SHandshake(boolean result, String error) {
        super(result, error);
    }
    
    @Override
    public void read(DataInputStream dis) throws IOException {
        dis.readByte(); // Hack to ignore response
        super.read(dis);
        if(getResult())
            maxMessageLength = dis.readInt();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(Protocol.CHAT_HANDSHAKE_MAGIC);
        super.write(dos);
        if(getResult())
            dos.writeInt(maxMessageLength);
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }
}
