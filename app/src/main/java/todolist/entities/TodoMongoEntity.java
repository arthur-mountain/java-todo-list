package todolist.entities;

import org.bson.types.ObjectId;

public class TodoMongoEntity {
  public ObjectId id;
  public String title;
  public String description;
  public boolean completed;

  public TodoMongoEntity(ObjectId id, String title, String description, boolean completed) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.completed = completed;
  }
}
