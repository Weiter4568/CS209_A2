package cn.edu.sustech.cs209.chatting.common;

import java.net.Socket;

public class User {
  private String name;
  private Socket socket;

  public User(String name, Socket socket) {
    this.name = name;
    this.socket = socket;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Socket getSocket() {
    return socket;
  }

  public void setSocket(Socket socket) {
    this.socket = socket;
  }
}
