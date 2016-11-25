package roottemplate.chat.packet;

import roottemplate.chat.Packet;
import roottemplate.chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CUsersListRequest implements Packet {
    public CUsersListRequest() {}
    
    @Override
    public void read(DataInputStream dis) throws IOException {}

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_USERS_LIST_REQUEST);
        dos.flush();
    }
}
