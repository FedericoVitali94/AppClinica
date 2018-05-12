package search;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author den
 */
public class SearchTableEntry {
   private final SimpleStringProperty name;
   private final SimpleStringProperty surname;
   private final SimpleIntegerProperty age;
   private final SimpleStringProperty bloodType;
   private final String persID;
   
   public SearchTableEntry(final String name, final String surname, 
                                                      final int age,
                                                      final String bloodType,
                                                      final String persUri) {
      
      this.name = new SimpleStringProperty(name);
      this.surname = new SimpleStringProperty(surname);
      this.age = new SimpleIntegerProperty(age);
      this.bloodType = new SimpleStringProperty(bloodType);
      this.persID = persUri;
   }

   public String getName() {
      return this.name.get();
   }

   public String getSurname() {
      return this.surname.get();
   }

   public Integer getAge() {
      return this.age.get();
   }

   public String getBloodType() {
      return this.bloodType.get();
   }
   
   public String getID() {
      return this.persID;
   }
   
   public SimpleStringProperty nameProperty() {
      return this.name;
   }

   public SimpleStringProperty surnameProperty() {
      return this.surname;
   }

   public SimpleIntegerProperty ageProperty() {
      return this.age;
   }

   public SimpleStringProperty bloodTypeProperty() {
      return this.bloodType;
   }
   
}
