package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Type;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Optional;

import static cn.edu.sustech.cs209.chatting.client.Controller.isConnected;
import static cn.edu.sustech.cs209.chatting.client.Controller.userNum;

public class Client extends Thread {
  static Controller controller;
  static ObjectInputStream objectInputStream;
  private Socket socket;

  Client(Controller controller1) {
    this.controller = controller1;
    this.socket = controller.socket;
  }

  @SuppressWarnings("unlikely-arg-type")
  public void run() {
    try {
      objectInputStream = new ObjectInputStream(socket.getInputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
    while (Controller.isConnected) {
      try {
        // 读取对象
        Message message = new Message();
        message = (Message) objectInputStream.readObject();

        System.out.println(message.toString());
        if (message.getType() == Type.Login) {
          if (message.getData().equals("Yes")) {
            controller.nameAvailable = true;
          }
        }
        // 处理接收到UserList的信息，更新列表和在线用户数量
        else if (message.getType() == Type.UserList) {
          // 默认最多连24个
          String[] str;
          str = message.getData().split("@");
          Controller.userNum = Integer.parseInt(str[0]);
          controller.upDateOnlineNum();
          controller.upDateOnlineUserList(message.getData());
        } else if (message.getType() == Type.ChatMsg) {
          controller.dealWithReceivedMessage(message);
        }
      } catch (IOException | ClassNotFoundException e) {
        System.out.println("Connection breaks!");
        Platform.runLater(
            () -> {
              Alert alert = new Alert(Alert.AlertType.WARNING);
              alert.setTitle("Warning");
              alert.setHeaderText("Server not Available!");
              alert.setContentText("The server is not available, please try again later!");
              Optional<ButtonType> result = alert.showAndWait();
              if (result.isPresent() && result.get() == ButtonType.OK) {
                System.exit(0);
              }
            });
        break;
      }
    }
  }
}
