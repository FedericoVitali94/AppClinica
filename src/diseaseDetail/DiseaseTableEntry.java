/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diseaseDetail;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Mattia
 */
public class DiseaseTableEntry {
    private final SimpleStringProperty name;
   private final SimpleStringProperty cod;
   
   public DiseaseTableEntry(final String cod, final String name) {
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
