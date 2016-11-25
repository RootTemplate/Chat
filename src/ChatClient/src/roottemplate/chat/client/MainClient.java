package roottemplate.chat.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import roottemplate.chat.Protocol;
import roottemplate.chat.ExitException;
import roottemplate.chat.NotVerifiedException;
import roottemplate.chat.Packet;
import roottemplate.chat.Util;
import roottemplate.chat.packet.*;

import java.util.Arrays;

public class MainClient {
    public static final int VERSION = 1;
    private static ConsoleFrame console;
    
    private static boolean doPrintServerAddr() {
        return !System.getProperty("chat.printServerAddr", "true").equalsIgnoreCase("false");
    }
    
    public static void main(String[] args) {
        console = ConsoleFrame.launch();
        
        String host = null;
        int port = -1;
        String name = null;
        try {
            String address;
            if(args.length < 1) {
                console.out("Адрес сервера: ");
                address = console.in();
                if(doPrintServerAddr())
                    console.outln(address);
                else {
                    console.outln();
                    console.removeLastHistoryEntry();
                }
            } else {
                address = args[0];
            }
            int portIndex = address.indexOf(":");
            if(portIndex != -1) {
                host = address.substring(0, portIndex);
                port = Util.parseUnsignedInt(address.substring(portIndex + 1));
            } else {
                host = address;
                port = 8079;
            }
            
            if(args.length < 2) {
                console.out("Ваше имя в чате: ");
                name = console.in();
                console.outln(name);
            } else
                name = args[1];
        } catch (InterruptedException ex) {
            console.outln("Невозможно считать информацию с консоли");
            console.exit(0);
        } catch (NumberFormatException ex) {
            console.outln("Указанный порт не является числом");
            console.exit(0);
        }
        if(args.length < 2)
            console.outln();
        
        Socket server = null;
        DataOutputStream request = null;
        DataInputStream responce = null;
        
        if(doPrintServerAddr()) {
            console.outln("Подключение к серверу " + host + ":" + port);
            console.outln();
        }
        
        try {
            server = new Socket(host, port);
        } catch (IOException ex) {
            log("Не удалось подключится к серверу");
            ex.printStackTrace();
            console.exit(0);
        }
            
        boolean exitNoWait = false;
        try {
            request = new DataOutputStream(server.getOutputStream());
            responce = new DataInputStream(server.getInputStream());
            
            new CHandshake(VERSION, name).write(request);
            
            Protocol.verifyChatSocket(responce);
            SHandshake result = new SHandshake();
            result.read(responce);
            if(!result.getResult()) {
                log("Вам отказано в присоединении к чату");
                log(result.getError());
                throw new ExitException();
            } else
                console.setMaxMessageLength(result.getMaxMessageLength());
            
            console.outln("Введите \"#exit\", чтобы выйти");
            console.outln("Введите \"#list\", чтобы вывести пользователей чата");
            console.outln("Введите \"#notify on/off\", чтобы включить/выключить опосвещения");
            console.outln();
            
            long lastPacketTime = System.currentTimeMillis();
            int keepaliveTimesSent = 0;
            boolean running = true;
            while(running) {
                if(responce.available() > 0) {
                    byte action = responce.readByte();
                    Packet p = Protocol.getPacketInstanceClient(action);
                    if(p == null) {
                        log("Сервер просит что-то непонятное. Код просьбы = " + action);
                    } else {
                        lastPacketTime = System.currentTimeMillis();
                        keepaliveTimesSent = 0;
                        p.read(responce);
                        switch(action) {
                            case Protocol.CHAT_ACTION_KEEPALIVE:
                                if( !((KeepAlive) p).isAnswer() )
                                    new KeepAlive(true).write(request);
                                break;
                            case Protocol.CHAT_ACTION_RECEIVE_MESSAGE:
                                SReceiveMessage msg = (SReceiveMessage) p;
                                log("[" + msg.getSender() + "] " + msg.getMessage());
                                console.onNewMessageReceived();
                                break;
                            case Protocol.CHAT_ACTION_RESULT:
                                SResult res = (SResult) p;
                                if(!res.getResult())
                                    log("Отказано. " + res.getError());
                                break;
                            case Protocol.CHAT_ACTION_LOGIN:
                                SLogin loginP = (SLogin) p;
                                log("{" + loginP.getLoggedName() + "} вошел в чат");
                                break;
                            case Protocol.CHAT_ACTION_LOGOUT:
                                SLogout logoutP = (SLogout) p;
                                log("{" + logoutP.getExitedName() + "} вышел из чата");
                                break;
                            case Protocol.CHAT_ACTION_USERS_LIST_RESPONCE:
                                log( "Пользователи: " + Arrays.toString(((SUsersListResponce) p).getUsers()) );
                                break;
                            default:
                                throw new IllegalArgumentException("Action = " + action);
                        }
                    }
                }
                
                if(Math.floor((System.currentTimeMillis() - lastPacketTime) / Protocol.KEEPALIVE_MAX_TIMEOUT) > keepaliveTimesSent) {
                    if(keepaliveTimesSent >= Protocol.KEEPALIVE_MAX_TRYES) {
                        log("Связь с сервером потеряна");
                        throw new ExitException();
                    } else {
                        new KeepAlive().write(request);
                        keepaliveTimesSent++;
                    }
                }
                
                String msg;
                while((msg = console.pollIn()) != null) {
                    if(msg.startsWith("#") || msg.startsWith("№")) {
                        if(msg.equalsIgnoreCase("#exit") || msg.equalsIgnoreCase("№учше")) {
                            running = false;
                            exitNoWait = true;
                        } else if(msg.equalsIgnoreCase("#list") || msg.equalsIgnoreCase("№дшые"))
                            new CUsersListRequest().write(request);
                        else if(msg.startsWith("#notify") || msg.startsWith("№тщешан")) {
                            if(msg.endsWith("on") || msg.endsWith("щт")) {
                                console.setNotifyAboutNewMessages(true);
                                console.outln("Опосвещения включены");
                            } else {
                                console.setNotifyAboutNewMessages(false);
                                console.outln("Опосвещения выключены");
                            }
                        } else
                            console.outln("Неизвестная #-команда: \"" + msg + "\"");
                    } else {
                        if(!msg.isEmpty())
                            new CSendMessage(msg).write(request);
                    }
                }
                
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        } catch (IOException ex) {
            log("Соединение оборвано");
            log("Ошибка: " + ex.getMessage());
            ex.printStackTrace();
        } catch (NotVerifiedException ex) {
            log("Кривой сервер");
            log("Ошибка: " + ex.getMessage());
            ex.printStackTrace();
        } catch (ExitException ex) {}
        catch (IllegalArgumentException ex) {
            log("Кривой сервер");
            log("Ошибка: " + ex.getMessage());
            ex.printStackTrace();
        }
        finally {
            try {
                if(request != null) {
                    new CExit().write(request);
                    request.close();
                }
            } catch (IOException ex) {}
            
            try {
                if(responce != null)
                    responce.close();
            } catch (IOException ex) {}
            
            try {
                if(server != null)
                    server.close();
            } catch (IOException ex) {}
            
            if(!exitNoWait)
                console.exit(0);
            else
                System.exit(0);
        }
    }
    
    private static void log(String text) {
        console.outln("[" + Util.timeString() + "] " + text);
    }
}

