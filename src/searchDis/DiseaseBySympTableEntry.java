/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchDis;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.apache.log4j.Logger;

/**
 *
 * @author den
 */
public class DiseaseBySympTableEntry {
   private final static Logger LOGGER = Logger.getLogger(DiseaseBySympTableEntry.class);
   
   private final SimpleStringProperty diseaseName;
   private final SimpleIntegerProperty matchingSymptoms;
   
   /***
    * 
    * @param diseaseName
    * @param matchingSymptoms 
    */
   public DiseaseBySympTableEntry(final String diseaseName, final int matchingSymptoms) {
      this.diseaseName = new SimpleStringProperty(diseaseName);
      this.matchingSymptoms = new SimpleIntegerProperty(matchingSymptoms);
   }

   /***
    * 
    * @return disease name
    */
   public String getDiseaseName() {
      return this.diseaseName.get();
   }
   
   /***
    * 
    * @return number of matching symptoms
    */
   public Integer getMatchingSympoms() {
      return this.matchingSymptoms.get();
   }

   /***
    * Getter for matchingSymptoms property
    * @return 
    */
   public SimpleIntegerProperty matchingSymptomsProperty() {
      return this.matchingSymptoms;
   }
   
   /***
    * Getter for diseaseName property
    * @return 
    */
   public SimpleStringProperty diseaseNameProperty() {
      return this.diseaseName;
   }
}
