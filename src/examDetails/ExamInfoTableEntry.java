/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package examDetails;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Mattia
 */
public class ExamInfoTableEntry {
    private final SimpleStringProperty name;
   private final SimpleStringProperty cod;
   
   public ExamInfoTableEntry(final String examCod, final String examName){
      this.name = new SimpleStringProperty(examName);
      this.cod = new SimpleStringProperty(examCod);
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
    * @return cod
    */
   public String getCod() {
      return this.cod.get();
   }
   
   /***
    * 
    * @return name property
    */
   public SimpleStringProperty nameProperty() {
      return this.name;
   }
   
   /***
    * 
    * @return cod property
    */
   public SimpleStringProperty codProperty() {
      return this.cod;
   }
}
