package cn.edu.sustech.cs209.chatting.client;

import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectDialog<T> extends Dialog<List<T>> {

  private final ListView<T> listView;

  public MultiSelectDialog(String title, List<T> choices) {
    setTitle(title);

    ButtonType confirmButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

    listView = new ListView<>();
    listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    listView.getItems().addAll(choices);

    getDialogPane().setContent(listView);

    setResultConverter(
        dialogButton -> {
          if (dialogButton == confirmButtonType) {
            return new ArrayList<>(listView.getSelectionModel().getSelectedItems());
          }
          return null;
        });
  }
}
