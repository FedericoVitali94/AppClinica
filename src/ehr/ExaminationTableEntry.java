/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ehr;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author den
 */
public class ExaminationTableEntry {
   private final SimpleStringProperty name;
   private final SimpleStringProperty date;
   private final String uri;
   
   public ExaminationTableEntry(final String name, final String date, final String uri) {
      this.name = new SimpleStringProperty(name);
      this.date = new SimpleStringProperty(date);
      this.uri = uri;
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
    * @return date
    */
   public String getDate() {
      return this.date.get();
   }
   
   /**
    * 
    * @return uri
    */
   public String getUri() {
      return this.uri;
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
    * @return date property
    */
   public SimpleStringProperty dateProperty() {
      return this.date;
   }
}
