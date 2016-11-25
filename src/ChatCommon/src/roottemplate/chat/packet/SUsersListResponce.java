package roottemplate.chat.packet;

import roottemplate.chat.Packet;
import roottemplate.chat.Protocol;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SUsersListResponce implements Packet {
    private String[] users;
    
    public SUsersListResponce() {}
    public SUsersListResponce(String[] users) {
        this.users = users;
    }

    @Override
    public void read(DataInputStream dis) throws IOException {
        int length = dis.readInt();
        users = new String[length];
        for(int i = 0; i < length; i++)
            users[i] = dis.readUTF();
    }

    @Override
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(Protocol.CHAT_ACTION_USERS_LIST_RESPONCE);
        dos.writeInt(users.length);
        for(int i = 0; i < users.length; i++)
            dos.writeUTF(users[i]);
        dos.flush();
    }
    
    public String[] getUsers() {
        return users;
    }
}
