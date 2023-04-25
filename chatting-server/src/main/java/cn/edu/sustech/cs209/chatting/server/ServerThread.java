package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Type;
import cn.edu.sustech.cs209.chatting.common.User;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ServerThread extends Thread {

  Socket s = null;
  private ObjectOutputStream obout; // send message to client
  private ObjectInputStream obin = null;
  private User user = null;

  public ServerThread(Socket s) throws IOException {
    this.s = s;
  }

  public static <K, V> K getKeyByValue(ConcurrentHashMap<K, V> map, V value) {
    for (Map.Entry<K, V> entry : map.entrySet()) {
      if (Objects.equals(value, entry.getValue())) {
        return entry.getKey();
      }
    }
    return null;
  }

  public User getUser() {
    return user;
  }

  @Override
  public void run() {
    try {
      Message message = null;
      obout = new ObjectOutputStream(s.getOutputStream());
      obin = new ObjectInputStream(s.getInputStream());

      while (true) {
        try {
          message = (Message) obin.readObject();
        } catch (ClassNotFoundException | IOException e) {
          continue;
        }
        if (message.getType() == Type.Login) {
          boolean sta = true;
          Iterator<User> iterator = Main.Server.userList.listIterator();
          while (iterator.hasNext()) {
            User user1 = iterator.next();
            if (user1.getName().equals(message.getSentBy())) {
              sta = false;
              Message message1 = new Message();
              message1.setType(Type.Login);
              message1.setData("No");
              SendMessageToClient(message1);
            }
          }
          if (sta) {
            user = new User(message.getSentBy(), s);
            Main.Server.userList.add(user);
            Main.Server.clients_list.putIfAbsent(user, obout);
            Message message1 = new Message(null, null, null, "Yes", Type.Login);
            SendMessageToClient(message1);
          }
        } else if (message.getType() == Type.Logout) {
          System.out.println(message.toString());
          Main.Server.userList.remove(user);
          Main.Server.ccList.remove(this);
          responseUserList();
        }
        // 处理userList
        else if (message.getType() == Type.UserList) {
          responseUserList();
        }
        // 处理ChatMsg（多人单人在一起）
        else if (message.getType() == Type.ChatMsg) {
          System.out.println("Received a message");
          System.out.println(message.toString());
          List<String> sigList = getToUsers(message.getSendTo());
          Iterator<ServerThread> iterator = Main.Server.ccList.iterator();
          while (iterator.hasNext()) {
            ServerThread serverThread = iterator.next();
            if (serverThread.getUser() != null) {
              if (sigList.contains(serverThread.getUser().getName())) {
                serverThread.SendMessageToClient(message);
              }
            }
          }
        }
        // 处理客户端退出
        else if (message.getType() == Type.Logout) {
          System.out.println("-----A client quits!-----");
          System.out.println(message.toString());
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      User user1 = (User) getKeyByValue(Main.Server.clients_list, obout);
      if (user1 != null) {
        Main.Server.clients_list.remove(user1);
        Main.Server.userList.remove(user1);
        System.out.println("A client disconnects！");
      }

      try {
        if (s != null) s.close();
        if (obout != null) obout.close();
        if (obin != null) obin.close();
      } catch (IOException e1) {

      }
    }
  }

  private void responseUserList() {
    Iterator<User> iterator = Main.Server.userList.listIterator();
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(Main.Server.userList.size());
    stringBuilder.append("@");
    while (iterator.hasNext()) {
      User user1 = iterator.next();
      stringBuilder.append(user1.getName());
      stringBuilder.append("&");
      stringBuilder.append(user1.getSocket().getPort());
      stringBuilder.append("@");
    }
    Iterator<ServerThread> iterator1 = Main.Server.ccList.iterator();
    while (iterator1.hasNext()) {
      ServerThread serverThread = iterator1.next();
      serverThread.SendMessageToClient(
          new Message(null, null, null, stringBuilder.toString(), Type.UserList));
    }
  }

  public void SendMessageToClient(Message message) {
    try {
      obout.writeObject(message);
      obout.flush();
    } catch (IOException e) {
      System.out.println("Message sends failed");
    }
  }

  public List<String> getToUsers(String str) {
    String[] strings = str.split("@");
    for (int i = 0; i < strings.length; i++) {
      if (!strings[i].isEmpty()) {
        strings[i] = strings[i].split("&")[0];
      }
    }
    return Arrays.stream(strings).collect(Collectors.toList());
  }
}
