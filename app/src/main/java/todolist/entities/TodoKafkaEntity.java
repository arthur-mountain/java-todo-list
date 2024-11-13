package todolist.entities;

public class TodoKafkaEntity {
  public String id;
  public String title;
  public String description;
  public int price;

  public TodoKafkaEntity(String id, String title, String description, int price) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.price = price;
  }
}
