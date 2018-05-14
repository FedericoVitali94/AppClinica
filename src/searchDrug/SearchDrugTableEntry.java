package searchDrug;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author den
 */
public class SearchDrugTableEntry {
   private final SimpleStringProperty name;
   private final SimpleStringProperty cod;
   
   public SearchDrugTableEntry(final String cod, final String name) {
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
