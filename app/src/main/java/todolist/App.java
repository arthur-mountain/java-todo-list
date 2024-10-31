package todolist;

import java.util.ArrayList;
import java.util.Scanner;

public class App {

  private ArrayList<String> todoList = new ArrayList<>();

  public static void main(String[] args) {
    App app = new App();
    app.run();
  }

  public void run() {
    Scanner scanner = new Scanner(System.in);
    boolean running = true;

    while (running) {
      System.out.println("\n待辦事項清單:");
      System.out.println("1. 新增事項");
      System.out.println("2. 列出所有事項");
      System.out.println("3. 刪除事項");
      System.out.println("4. 退出");

      System.out.print("請選擇操作 (1-4): ");
      int choice = scanner.nextInt();
      scanner.nextLine(); // 清除緩衝區

      switch (choice) {
        case 1:
          addTodoItem(scanner);
          break;
        case 2:
          System.out.print("List todo Items");
          listTodoItems();
          break;
        case 3:
          System.out.print("Delete todo Item");
          deleteTodoItem(scanner);
          break;
        case 4:
          System.out.println("已退出程式。");
          running = false; // 設定為 false 以退出迴圈
          break;
        default:
          System.out.println("無效的選擇，請重新輸入。");
          running = false; // 設定為 false 以退出迴圈
      }
    }

    scanner.close();
  }

  private void addTodoItem(Scanner scanner) {
    System.out.print("請輸入待辦事項: ");
    String item = scanner.nextLine();
    todoList.add(item);
    System.out.println("已新增事項: " + item);
  }

  private void listTodoItems() {
    if (todoList.isEmpty()) {
      System.out.println("目前沒有待辦事項。\n");
    } else {
      System.out.println("目前待辦事項:\n");
      for (int i = 0; i < todoList.size(); i++) {
        System.out.println((i + 1) + ". " + todoList.get(i));
      }
    }
  }

  private void deleteTodoItem(Scanner scanner) {
    if (todoList.isEmpty()) {
      return;
    }

    listTodoItems();
    System.out.print("請輸入要刪除的事項編號: ");
    int index = scanner.nextInt() - 1;

    if (index >= 0 && index < todoList.size()) {
      String removedItem = todoList.remove(index);
      System.out.println("已刪除事項: " + removedItem);
    } else {
      System.out.println("無效的編號。");
    }
  }
}
