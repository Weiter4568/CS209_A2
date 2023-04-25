package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.Type;
import cn.edu.sustech.cs209.chatting.common.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable {

  private static final int PORT = 8888;
  protected static Socket socket;
  protected static boolean isConnected = false;
  protected static boolean nameAvailable = false;
  protected static List<String> onLineUserList = new ArrayList<String>();
  // 设置chatContentView的cell
  protected static int userNum = 1;
  protected static HashMap<String, ObservableList<Message>> chatContentMap = new HashMap<>();
  protected static HashMap<String, String> groupNameToUsers = new HashMap<>();
  static String username;
  private static String IP = "127.0.0.1";
  private static ObjectOutputStream outputStream;
  private static ObservableList<Message> messageList = FXCollections.observableArrayList();
  private static StringProperty curToUser = new SimpleStringProperty("Initial");
  @FXML ListView<Message> chatContentList;
  @FXML ListView<String> chatList;
  @FXML Label currentOnlineCnt;
  @FXML Label currentUsername;
  @FXML VBox wholeBox;
  @FXML AnchorPane backGround;
  @FXML ListView groupList;
  Client client;
  MultipleSelectionModel<String> selectionModel;
  ChangeListener<String> userChangeListener =
      new ChangeListener<String>() {
        @Override
        public void changed(
            ObservableValue<? extends String> observable, String oldValue, String newValue) {
          messageList = chatContentMap.get(newValue);
          chatContentList.setItems(messageList);
        }
      };
  @FXML TextArea inputArea;
  // 设置chatListView的cell
  private List<String> chatUserList = new ArrayList<>();
  ObservableList<String> userItems = FXCollections.observableArrayList(chatUserList);

  public static synchronized void CloseConnect() {
    Message message = new Message(null, username, null, null, Type.Logout);
    isConnected = false;
    SendMessage(message);
    try {
      if (outputStream != null) outputStream.close();
      if (socket != null) socket.close();
      if (Client.objectInputStream != null) {
        Client.objectInputStream.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    Platform.exit();
  }

  protected static void SendMessage(Message message) {
    try {
      outputStream.writeObject(message);
      outputStream.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    do {
      Optional<String> input = dialog.showAndWait();
      if (input.isPresent() && !input.get().isEmpty()) {
        username = input.get();
        System.out.println("Connecting to the server...");
        ConnectServer();
        // sendTo store the IP, data store the Port
        if (socket != null) {
          Message msg1 =
              new Message(null, username, IP, String.valueOf(socket.getPort()), Type.Login);
          SendMessage(msg1);
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      } else {
        System.out.println("Invalid username " + input + ", exiting");
        Platform.exit();
      }
      if (!nameAvailable) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText("Name Existed");
        alert.setContentText("Username already existed, please change another one!");
        alert.showAndWait();
      }
      dialog.getEditor().clear();
    } while (!nameAvailable);
    SendMessage(new Message(null, "test", null, "test", Type.UserList));
    currentUsername.setText(String.format("Current User: " + username));
    // 绑定chatContentList View
    chatContentList.setCellFactory(new MessageCellFactory());
    // 绑定chatList View
    chatList.setCellFactory(new ChatCellFactory());
    chatList.setItems(userItems);

    // 为curTOUser添加监听器
    curToUser.addListener(userChangeListener);
    chatList.getStylesheets().add("cell.css");
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();

    for (int i = 0; i < onLineUserList.size(); i++) {
      if (!onLineUserList.get(i).equals(username)) {
        userSel.getItems().add(onLineUserList.get(i));
      }
    }

    Button okBtn = new Button("OK");
    okBtn.setOnAction(
        e -> {
          user.set(userSel.getSelectionModel().getSelectedItem());
          if (chatContentMap.containsKey(String.valueOf(user))) {
            curToUser.set(String.valueOf(user));
          } else {
            chatContentMap.put(String.valueOf(user), FXCollections.observableArrayList());
            curToUser.set(String.valueOf(user));
          }
          if (!userItems.contains(String.valueOf(user))) {
            userItems.add(0, String.valueOf(user));
          }
          int index = userItems.indexOf(String.valueOf(user));
          chatList.scrollTo(index);
          selectionModel = chatList.getSelectionModel();
          selectionModel.select(index);
          groupList.getItems().clear();
          stage.close();
        });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   *
   * <p>The naming rule for group chats is similar to WeChat: If there are > 3 users: display the
   * first three usernames, sorted in lexicographic order, then use ellipsis with the number of
   * users, for example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the
   * ellipsis, for example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
    List<String> tmp = new ArrayList<>();
    tmp = onLineUserList;
    tmp.remove(username);
    MultiSelectDialog<String> multiSelectDialog = new MultiSelectDialog<>("Create Group Chat", tmp);

    Optional<List<String>> result = multiSelectDialog.showAndWait();
    StringBuilder stringBuilder = new StringBuilder();
    List<String> tmp1 = new ArrayList<>();
    tmp1.add(username);
    stringBuilder.append("@" + username);
    result.ifPresent(
        selectedOptions -> {
          System.out.println("Selected options: " + selectedOptions);
          for (String item : selectedOptions) {
            stringBuilder.append("@");
            stringBuilder.append(item);
            tmp1.add(item);
          }
        });

    if (chatContentMap.containsKey(String.valueOf(stringBuilder))) {
      curToUser.set(String.valueOf(stringBuilder));
    } else {
      chatContentMap.put(String.valueOf(stringBuilder), FXCollections.observableArrayList());
      curToUser.set(String.valueOf(stringBuilder));
    }
    String groupName = getGroupName(tmp1);
    groupNameToUsers.putIfAbsent(groupName, String.valueOf(stringBuilder));
    if (!userItems.contains(groupName)) {
      userItems.add(0, groupName);
    }
    int index = userItems.indexOf(groupName);
    chatList.scrollTo(index);
    selectionModel = chatList.getSelectionModel();
    selectionModel.select(index);
  }

  @FXML
  public void doSendMessage() {
    String text = inputArea.getText();
    System.out.println(curToUser.get());
    inputArea.clear();
    if (!isRealEmpty(text) && curToUser != null) {
      long currentTimestamp = System.currentTimeMillis();
      Message msg = new Message(currentTimestamp, username, curToUser.get(), text, Type.ChatMsg);
      chatContentMap.putIfAbsent(curToUser.get(), FXCollections.observableArrayList());
      chatContentMap.get(curToUser.get()).add(msg);
      chatContentList.setItems(chatContentMap.get(curToUser.get()));
      if (!curToUser.get().contains("@")) groupList.getItems().clear();
      SendMessage(msg);
    }
  }

  private void ConnectServer() {
    try {
      socket = new Socket(IP, PORT);
      outputStream = new ObjectOutputStream(socket.getOutputStream());
      System.out.println("Connected!");
      isConnected = true;
      client = new Client(this);
      client.start();
    } catch (Exception e) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Warning");
      alert.setHeaderText("Server not Available!");
      alert.setContentText("The server is not available, please try again later!");
      Optional<ButtonType> result = alert.showAndWait();
      if (result.isPresent() && result.get() == ButtonType.OK) {
        System.exit(0);
      }
    }
  }

  protected void upDateOnlineNum() {
    Platform.runLater(
        () -> {
          currentOnlineCnt.setText(String.format("Online: " + userNum));
        });
  }

  protected void upDateOnlineUserList(String str) {
    onLineUserList.clear();
    String[] userList = str.split("@");
    userNum = Integer.parseInt(userList[0]);
    for (int i = 1; i < userList.length; i++) {
      if (!userList[i].equals("")) {
        String[] name = userList[i].split("&");
        onLineUserList.add(name[0]);
      }
    }
  }

  // 判断字符串是否为空，忽略换行和空格
  private boolean isRealEmpty(String str) {
    if (str.trim().replaceAll("\\r\\n|\\r|\\n", "").isEmpty()) return true;
    return false;
  }

  private List<String> getToUsers(String str) {
    String[] strings = str.split("@");
    for (int i = 0; i < strings.length; i++) {
      if (!strings[i].isEmpty()) {
        strings[i] = strings[i].split("&")[0];
      }
    }
    return Arrays.stream(strings).collect(Collectors.toList());
  }

  private String getGroupName(List<String> strings) {
    StringBuilder stb = new StringBuilder();
    Collections.sort(strings);
    if (strings.size() <= 3) {
      for (int i = 0; i < strings.size(); i++) {
        stb.append(strings.get(i));
        if (i != strings.size() - 1) stb.append(", ");
      }
    } else {
      for (int i = 0; i < 3; i++) {
        stb.append(strings.get(i));
        if (i != 2) stb.append(", ");
      }
      stb.append("... (");
      stb.append(strings.size());
      stb.append(")");
    }
    return String.valueOf(stb);
  }

  protected void dealWithReceivedMessage(Message message) {
    List<String> toUsers = getToUsers(message.getSendTo());
    if (toUsers.size() == 1) {
      String fromUser = message.getSentBy();
      chatContentMap.putIfAbsent(fromUser, FXCollections.observableArrayList());
      Platform.runLater(
          () -> {
            chatContentMap.get(fromUser).add(message);
          });
      if (!chatList.getItems().contains(fromUser)) {
        chatList.getItems().add(fromUser);
      }
      Platform.runLater(
          () -> {
            selectionModel = chatList.getSelectionModel();
            int index = userItems.indexOf(fromUser);
            if (!selectionModel.isSelected(index)) selectionModel.select(index);
          });

      // 创建一个消息提示框

    } else {
      if (!message.getSentBy().equals(username)) {
        toUsers.remove("");
        Collections.sort(toUsers);
        String groupName = getGroupName(toUsers);
        groupNameToUsers.putIfAbsent(groupName, message.getSendTo());
        chatContentMap.putIfAbsent(
            groupNameToUsers.get(groupName), FXCollections.observableArrayList());
        Platform.runLater(
            () -> {
              chatContentMap.get(groupNameToUsers.get(groupName)).add(message);
            });
        if (!chatList.getItems().contains(groupName)) {
          chatList.getItems().add(groupName);
        }
        Platform.runLater(
            () -> {
              selectionModel = chatList.getSelectionModel();
              int index = userItems.indexOf(groupName);
              if (!selectionModel.isSelected(index)) selectionModel.select(index);
            });
      }
    }
    if (!message.getSentBy().equals(username)) {
      Platform.runLater(
          () -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("New Message");
            dialog.setHeaderText(null);
            String note = "You have a new message from: " + message.getSentBy();
            dialog.setContentText(note);
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(okButton);
            dialog.setResultConverter(
                dialogButton -> {
                  if (dialogButton == okButton) {
                    return note;
                  }
                  return null;
                });
            dialog.show();
          });
    }
  }

  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {
    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            setText(null);
            setGraphic(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }
          Platform.runLater(
              () -> {
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setGraphic(wrapper);
              });
        }
      };
    }
  }

  private class ChatCellFactory implements Callback<ListView<String>, ListCell<String>> {
    @Override
    public ListCell<String> call(ListView<String> param) {
      ListCell<String> cell =
          new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
              super.updateItem(item, empty);
              if (!empty && item != null) {
                Platform.runLater(
                    () -> {
                      setText(item);
                    });
              } else {
                setText(null);
              }
            }
          };
      cell.setOnMouseClicked(
          event -> {
            if (cell.getText() != null) {
              if (cell.getText().contains(",")) {
                String str = groupNameToUsers.get(cell.getText());
                curToUser.set(str);
                List<String> list = getToUsers(str);
                list.remove("");
                groupList.getItems().clear();
                groupList.getItems().addAll(list);
              } else {
                groupList.getItems().clear();
                curToUser.set(cell.getText());
              }
            }
          });
      return cell;
    }
  }
}
