/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recommendedSympExams;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Mattia
 */
public class TableEntry {
     private final SimpleStringProperty name;
    private final SimpleStringProperty cod;

    public TableEntry(final String name, final String cod) {
       this.cod = new SimpleStringProperty(cod);
       this.name = new SimpleStringProperty(name);
    }

   public String getCod() {
       return this.cod.get();
   }
   public String getName() {
      return this.name.get();
   }
   
   public SimpleStringProperty codProperty() {
       return this.cod;
   }
   public SimpleStringProperty nameProperty() {
      return this.name;
   }
   
}
