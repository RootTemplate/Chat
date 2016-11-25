package roottemplate.chat.packet;

import roottemplate.chat.Packet;
import roottemplate.chat.Protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SResult implements Packet {
    private boolean result;
    private String error;
    
    public SResult() {}
    public SResult(boolean result) {
        this(result, null);
    }
    public SResult(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        result = dis.readBoolean();
        if(!result)
            error = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_RESULT);
        dos.writeBoolean(result);
        if(!result)
            dos.writeUTF(error);
        dos.flush();
    }
    
    public boolean getResult() {
        return result;
    }
    
    public String getError() {
        return error;
    }
}
