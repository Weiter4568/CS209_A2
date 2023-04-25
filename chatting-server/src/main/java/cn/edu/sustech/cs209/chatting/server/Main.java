package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


public class Main{
    public static void main(String[] args) {
        System.out.println("Server is starting···");
        Server server = new Server();
        try {
            server.startServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Server {
        //PORT
        private static final int PORT = 8888;
        //ServerSocket
        private ServerSocket serverSocket = null;
        private Socket socket = null;
        public static ConcurrentHashMap<User, ObjectOutputStream> clients_list = new ConcurrentHashMap<User, ObjectOutputStream>();
        public static List<User> userList = new ArrayList<User>();
        public static ArrayList<ServerThread> ccList = new ArrayList<>();
        public static boolean isStart = false;

        public void startServer(){
            try{
                serverSocket = new ServerSocket(PORT);
                System.out.println("Server is available now!");
                isStart = true;
                while(isStart){
                    socket = serverSocket.accept();
                    System.out.println("A new client connected...");
                    ServerThread serverThread = new ServerThread(socket);
                    ccList.add(serverThread);
                    serverThread.start();
                }
            } catch (IOException ex){
                System.out.println("Server start failed!");
            }
        }

    }
}
