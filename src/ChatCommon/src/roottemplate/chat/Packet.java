package roottemplate.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {
    void read(DataInputStream dis) throws IOException;
    void write(DataOutputStream dos) throws IOException;
}
