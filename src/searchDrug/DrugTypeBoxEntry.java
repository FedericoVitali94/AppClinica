/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchDrug;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Mattia
 */
public class DrugTypeBoxEntry {
    private final SimpleStringProperty id;
    private final SimpleStringProperty name;
   
   public DrugTypeBoxEntry(final String id, final String name) {
      this.id = new SimpleStringProperty(id);
      this.name = new SimpleStringProperty(name);
   }

   public String getId() {
       return this.id.get();
   }
   public String getName() {
      return this.name.get();
   }
   
   public SimpleStringProperty idProperty() {
       return this.id;
   }
   public SimpleStringProperty nameProperty() {
      return this.name;
   }
   
}
