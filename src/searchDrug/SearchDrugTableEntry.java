package searchDrug;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author den
 */
public class SearchDrugTableEntry {
   private final SimpleStringProperty name;
   
   public SearchDrugTableEntry(final String name) {
      
      this.name = new SimpleStringProperty(name);
   }

   public String getName() {
      return this.name.get();
   }
   
   public SimpleStringProperty nameProperty() {
      return this.name;
   }
   
}
