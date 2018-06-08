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
public class ExaminationResultEntry {
     private final SimpleStringProperty name;
     private final SimpleStringProperty cod;
   
   public ExaminationResultEntry(final String cod, final String name) {
      this.name = new SimpleStringProperty(name);
      this.cod = new SimpleStringProperty(cod);
   }
   
   
   /***
    * 
    * @return cod
    */
   public String getCod() {
      return this.cod.get();
   }
   /***
    * 
    * @return name
    */
   public String getName() {
      return this.name.get();
   }
   
   /***
    * 
    * @return cod property
    */
   public SimpleStringProperty codProperty() {
      return this.cod;
   }
   /***
    * 
    * @return name property
    */
   public SimpleStringProperty nameProperty() {
      return this.name;
   }
   
}
