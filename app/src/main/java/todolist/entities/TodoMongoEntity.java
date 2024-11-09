package todolist.entities;

public class TodoMongoEntity {
  public String id;
  public String title;
  public String description;
  public boolean completed;

  public TodoMongoEntity(String id, String title, String description, boolean completed) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.completed = completed;
  }
}
