package todolist;

public class TodoEntity {
  public int id;
  public String title;
  public String description;
  public boolean completed;

  public TodoEntity(int id, String title, String description, boolean completed) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.completed = completed;
  }
}
