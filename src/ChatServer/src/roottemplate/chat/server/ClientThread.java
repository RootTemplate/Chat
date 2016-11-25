package roottemplate.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Queue;
import roottemplate.chat.Protocol;
import roottemplate.chat.ExitException;
import roottemplate.chat.NotVerifiedException;
import roottemplate.chat.Packet;
import roottemplate.chat.Util;
import roottemplate.chat.packet.*;

public class ClientThread extends Thread {
    public final Socket client;
    private final Queue<Packet> packetsQueue = new ArrayDeque<Packet>();
    private String name = null;
    
    public ClientThread(Socket client) {
        this.client = client;
    }
    
    public String getUserName() {
        return name;
    }
    
    @Override
    public void run() {
        DataOutputStream out = null;
        DataInputStream in = null;
        try {
            out = new DataOutputStream(client.getOutputStream());
            in = new DataInputStream(client.getInputStream());
            
            Protocol.verifyChatSocket(in);
            CHandshake info = new CHandshake();
            info.read(in);
            
            if(MainServer.getUsersCount() >= MainServer.MAX_USERS) {
                //log("Попытка войти на переполненный сервер"); // No log for better performance :)
                new SHandshake(false, "Сервер переполнен").write(out);
                throw new ExitException();
            }
            if(info.getVersion() != MainServer.VERSION) {
                log("Попытка войти на сервер с версией = " + info.getVersion() + ". Имя: " + info.getChatName());
                new SHandshake(false, "Версия не совпадает с версией сервера. Версия сервера = " + MainServer.VERSION)
                        .write(out);
                throw new ExitException();
            }
            if(info.getChatName().length() > MainServer.NAME_MAX_LENGTH) {
                log("Попытка войти на сервер с именем длинее максимального (" + MainServer.NAME_MAX_LENGTH + "): " +
                        info.getChatName().substring(0, MainServer.NAME_MAX_LENGTH) + "...");
                new SHandshake(false, "Имя не может быть длинее разрешенного (" + MainServer.NAME_MAX_LENGTH + " символов)").write(out);
                throw new ExitException();
            }
            if(MainServer.containsChatUser(info.getChatName())) {
                log("Попытка войти на сервер с занятым именем: " + info.getChatName());
                new SHandshake(false, "Пользователь с этим именем уже вошел").write(out);
                throw new ExitException();
            }
            
            name = info.getChatName();
            MainServer.addChatUser(this);
            new SHandshake(true, MainServer.MESSAGE_MAX_LENGTH).write(out);
            MainServer.broadcast(new SLogin(name));
            log("Пользователь вошел в чат");
            
            long lastPacketTime = System.currentTimeMillis();
            int keepaliveTimesSent = 0;
            while(!isInterrupted()) {
                if(in.available() > 0) {
                    byte action = in.readByte();
                    Packet packet = Protocol.getPacketInstanceServer(action);
                    if(packet == null) {
                        new SResult(false, "Неизвестная операция").write(out);
                    } else {
                        lastPacketTime = System.currentTimeMillis();
                        keepaliveTimesSent = 0;
                        packet.read(in);
                        switch(action) {
                            case Protocol.CHAT_ACTION_KEEPALIVE:
                                if( !((KeepAlive) packet).isAnswer() )
                                    new KeepAlive(true).write(out);
                                break;
                            case Protocol.CHAT_ACTION_SEND_MESSAGE:
                                CSendMessage message = (CSendMessage) packet;
                                if(message.getMessage().length() > MainServer.MESSAGE_MAX_LENGTH) {
                                    new SResult(false, "Длина сообщения превышает " + MainServer.MESSAGE_MAX_LENGTH).write(out);
                                } else {
                                    String messageText = message.getMessage();
                                    Packet msgP = new SReceiveMessage(name, messageText);
                                    MainServer.broadcast(msgP);

                                    log("> " + messageText);
                                    new SResult(true).write(out);
                                }
                                break;
                            case Protocol.CHAT_ACTION_EXIT:
                                this.interrupt();
                                break;
                            case Protocol.CHAT_ACTION_USERS_LIST_REQUEST:
                                log("Пользователь запросил список пользователей чата");
                                String[] users = MainServer.getUserNames();
                                new SUsersListResponce(users).write(out);
                                break;
                            default:
                                throw new IllegalArgumentException("Action = " + action);
                        }
                    }
                }
                
                Packet p;
                while((p = packetsQueue.poll()) != null) {
                    p.write(out);
                }
                
                if(Math.floor((System.currentTimeMillis() - lastPacketTime) / Protocol.KEEPALIVE_MAX_TIMEOUT) > keepaliveTimesSent) {
                    if(keepaliveTimesSent >= Protocol.KEEPALIVE_MAX_TRYES) {
                        log("Связь с пользователем потеряна");
                        this.interrupt();
                    } else {
                        new KeepAlive().write(out);
                        keepaliveTimesSent++;
                    }
                }
                
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } catch (IOException ex) {
            log("Ошибка");
            ex.printStackTrace();
        } catch (NotVerifiedException ex) {
            log("Ошибка");
            log("Кривой клиент");
            ex.printStackTrace();
        } catch (ExitException ex) {}
        catch(IllegalArgumentException ex) {
            log("Ошибка");
            log("Кривой клиент");
            ex.printStackTrace();
        } finally {
            try {
                if(out != null)
                    out.close();
            } catch (IOException ex) {}
            
            try {
                if(in != null)
                    in.close();
            } catch (IOException ex) {}
            
            try {
                if(client != null)
                    client.close();
            } catch (IOException ex) {}
            
            MainServer.broadcast(new SLogout(name));
            MainServer.removeChatUser(this);
            log("Пользователь вышел из чата");
        }
    }
    
    public void sendPacket(Packet packet) {
        synchronized(packetsQueue) {
            packetsQueue.add(packet);
        }
    }
    
    private void log(String text) {
        log(text, System.currentTimeMillis());
    }
    private void log(String text, long timestamp) {
        System.out.println("[" + Util.timestampToString(timestamp) + "] [" + client.getInetAddress() + ": " + name + "] " + text);
    }
}
