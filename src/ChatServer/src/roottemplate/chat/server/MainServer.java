package roottemplate.chat.server;

import roottemplate.chat.ExitException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import roottemplate.chat.Packet;
import roottemplate.chat.Util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;

public class MainServer {
    public static final int VERSION = 1;
    public static int NAME_MAX_LENGTH = 22;
    public static int MESSAGE_MAX_LENGTH = 80;
    public static int MAX_USERS = 50;
    
    private static final HashMap<String, ClientThread> chatUsers = new HashMap<String, ClientThread>();
    private static final Queue<Packet> broadcastQueue = new ArrayDeque<Packet>();
    private static final MessageSenderThread msgSender = new MessageSenderThread();
    
    public static void main(String[] args) throws IOException, ExitException {
        NAME_MAX_LENGTH = Util.getPropertyUnsignedInt("chat.maxNameLength", NAME_MAX_LENGTH);
        MESSAGE_MAX_LENGTH = Util.getPropertyUnsignedInt("chat.maxMessageLength", MESSAGE_MAX_LENGTH);
        MAX_USERS = Util.getPropertyUnsignedInt("chat.maxChatUsers", MAX_USERS);
        
        
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        
        int port = -1;
        try {
            if(args.length < 1) {
                System.out.print("Порт сервера (нажмите Enter, чтобы поставить порт по умочанию): ");
                String port_ = console.readLine();
                if(port_.isEmpty())
                    port = 8079;
                else
                    port = Util.parseUnsignedInt(port_);
            } else {
                if(args[0].equals("DEF"))
                    port = 8079;
                else
                    port = Util.parseUnsignedInt(args[0]);
            }
        } catch (IOException ex) {
            System.out.println("Невозможно считать информацию с консоли");
            System.exit(0);
        } catch (NumberFormatException ex) {
            System.out.println("Указанный порт не является числом");
            System.exit(0);
        }
        
        ServerSocket ss = new ServerSocket(port);
        System.out.println("Сервер работает на " + InetAddress.getLocalHost().getHostAddress() + ":" + port);
        msgSender.start();
        
        while(true) {
            Socket client = ss.accept();
            ClientThread thr = new ClientThread(client);
            thr.start();
        }
    }
    
    public static void addChatUser(ClientThread thr) {
        synchronized(chatUsers) {
            chatUsers.put(thr.getUserName(), thr);
        }
    }
    
    public static void removeChatUser(ClientThread thr) {
        synchronized(chatUsers) {
            chatUsers.remove(thr.getUserName());
        }
    }
    
    public static boolean containsChatUser(String name) {
        synchronized(chatUsers) {
            return chatUsers.containsKey(name);
        }
    }
    
    public static int getUsersCount() {
        synchronized(chatUsers) {
            return chatUsers.size();
        }
    }
    
    public static String[] getUserNames() {
        synchronized(chatUsers) {
            String[] users = new String[chatUsers.size()];
            int i = 0;
            for(ClientThread clThr : chatUsers.values()) {
                users[i++] = clThr.getUserName();
            }
            return users;
        }
    }
    
    public static void broadcast(Packet packet) {
        synchronized(broadcastQueue) {
            broadcastQueue.add(packet);
        }
    }
    
    private static class MessageSenderThread extends Thread {
        @Override
        public void run() {
            while(!isInterrupted()) {
                Packet p;
                while((p = broadcastQueue.poll()) != null) {
                    for(ClientThread client : chatUsers.values()) {
                        client.sendPacket(p);
                    }
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }
}
