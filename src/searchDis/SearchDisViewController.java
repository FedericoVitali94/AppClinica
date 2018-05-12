/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchDis;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import diseaseDetail.DisDetViewController;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import util.PopUps;
import util.QueryUtils;
import util.Redirecter;

/**
 * FXML Controller class
 *
 * @author den
 */
public class SearchDisViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(SearchDisViewController.class);

   @FXML
   private ComboBox<NameUriPair> cbSymp1;
   @FXML
   private ComboBox<NameUriPair> cbSymp2;
   @FXML
   private ComboBox<NameUriPair> cbSymp3;
   @FXML
   private ComboBox<NameUriPair> cbSymp4;
   @FXML
   private TableView<DiseaseBySympTableEntry> table;

   private ObservableList<NameUriPair> symps;
   private ObservableList<DiseaseBySympTableEntry> searchResults;

   @FXML
   private void handleBackToMenu(ActionEvent event) {
      Redirecter.getInstance().redirect(this.table.getScene(), Redirecter.MAIN_MENU_WIN, true);
   }

   @FXML
   private void handleReset(ActionEvent event) {
      this.cbSymp1.getSelectionModel().select(0);
      this.cbSymp2.getSelectionModel().select(0);
      this.cbSymp3.getSelectionModel().select(0);
      this.cbSymp4.getSelectionModel().select(0);
      this.searchResults.clear();
   }

   @FXML
   private void handleSearch(ActionEvent event) {
      // create a list of selected symptoms uris
      List<String> uris = new ArrayList<>();
      uris.add(this.getSelectedUri(this.cbSymp1));
      uris.add(this.getSelectedUri(this.cbSymp2));
      uris.add(this.getSelectedUri(this.cbSymp3));
      uris.add(this.getSelectedUri(this.cbSymp4));

      LOGGER.debug(uris.toString());
      //remove null or "" uris from the list
      uris.removeIf(item -> {
         return this.isNullOrEmpty(item);
      });

      String queryStr = QueryUtils.loadQueryFromFile("getDiseasesBySymptom.txt");
      Map<NameUriPair, Integer> matches = new HashMap<>();

      //for each symptom uri we query the db for diseases which have the property has_symptom
      //matching the selected symptom or one of his sub classes
      uris.stream().forEach(item -> {
         ParameterizedSparqlString query = new ParameterizedSparqlString();
         query.setCommandText(queryStr);
         query.setIri("sympUri", item);
         LOGGER.debug(query.toString());

         MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();
         try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), mldg.toDataset())) {
            ResultSet res = execution.execSelect();
            while (res.hasNext()) {
               QuerySolution sol = res.next();
               String name = sol.getLiteral("disName").getString();
               String uri = sol.getResource("dis").getURI();
               NameUriPair pair = new NameUriPair(name, uri);

               //create a map like <disease, numerber of matching symptomns>
               Integer oldValue = matches.putIfAbsent(pair, 1);
               if (oldValue != null) {
                  matches.replace(pair, oldValue + 1);
               }
            }
         }
      });

      //put the map data into the search result observable list to update the table
      this.searchResults.clear();
      matches.entrySet().stream().forEach(entry -> {
         this.searchResults.add(new DiseaseBySympTableEntry(entry.getKey().getName(), entry.getValue()));
      });
   }

   /**
    * *
    * Return the uri of the chosen item in the combobox.
    *
    * @param cb
    *
    * @return the URI, "" if something is wrong.
    */
   private String getSelectedUri(ComboBox<NameUriPair> cb) {
      if (cb.isDisabled()) {
         return "";
      }

      NameUriPair item = cb.getSelectionModel().getSelectedItem();
      if (item != null && !this.isNullOrEmpty(item.getName())
              && !item.getName().equals("--Nulla--")
              && !this.isNullOrEmpty(item.getUri())) {
         return item.getUri();
      }

      return "";
   }

   /**
    * *
    * Return true if the input String is null or "".
    *
    * @param str
    *
    * @return boolean
    */
   private boolean isNullOrEmpty(final String str) {
      return str == null || str.isEmpty();
   }

   /**
    * *
    *
    * @param url
    * @param rb
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.searchResults = FXCollections.observableArrayList();

      this.setCBCellFactory(this.cbSymp1);
      this.setCBCellFactory(this.cbSymp2);
      this.setCBCellFactory(this.cbSymp3);
      this.setCBCellFactory(this.cbSymp4);

      //loads symptoms
      this.loadSymptomsAndSetCBValues();

      //setup the table
      this.setColsSearchTable();
      
      this.setDoubleClickHandler(this.table);
   }
   
   /**
    * *
    * double click on table row opens a new window with details about the selected disease
    * @param table
    */
   private void setDoubleClickHandler(TableView<DiseaseBySympTableEntry> table) {
      table.setRowFactory(tr -> {
         TableRow<DiseaseBySympTableEntry> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               String disName = row.getItem().getDiseaseName();
               //load disease detail window
               try {
                  FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.DISEASE_DET_WIN));
                  Parent view = (Parent) loader.load();
                  //set the person in the new window controller
                  loader.<DisDetViewController>getController().setDisAndInit(disName);

                  Scene scene = new Scene(view);
                  Stage newStage = new Stage();
                  newStage.setScene(scene);
                  newStage.setTitle(disName);
                  newStage.show();
               } catch (IOException exc) {
                  PopUps.showError("Errore", "Impossibile caricare la pagina");
                  LOGGER.error(exc.getMessage());
               }
            }
         });
         return row;
      });
   }

   /**
    * *
    * creates the table columns, sets cells factory, and binds table to the list
    * of results
    */
   private void setColsSearchTable() {
      TableColumn<DiseaseBySympTableEntry, String> nameCol = new TableColumn("Nome Malattia");
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().diseaseNameProperty();
      });
      TableColumn<DiseaseBySympTableEntry, Number> matchCol = new TableColumn("Numero di Sintomi che Combaciano");
      matchCol.setCellValueFactory(cellData -> {
         return cellData.getValue().matchingSymptomsProperty();
      });
      
      this.table.getColumns().addAll(
              nameCol,
              matchCol
      );
      
      this.table.setItems(new SortedList<>(this.searchResults, (o1, o2) ->{
         return o2.getMatchingSympoms() - o1.getMatchingSympoms();
      }));
   }

   /**
    * *
    * load symptoms into cbs.
    */
   private void loadSymptomsAndSetCBValues() {
      String queryStr = QueryUtils.loadQueryFromFile("getAllSymptoms.txt");
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();

      mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(queryStr, mldg.toDataset())) {
         this.symps = FXCollections.observableArrayList();
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String sympName = sol.getLiteral("sympName").getString();
            String sympUri = sol.getResource("symp").getURI();
            this.symps.add(new NameUriPair(sympName, sympUri));
         }
         this.symps.add(0, new NameUriPair("--Nulla--", ""));
         this.cbSymp1.setItems(this.symps);
         this.cbSymp2.setItems(this.symps);
         this.cbSymp3.setItems(this.symps);
         this.cbSymp4.setItems(this.symps);
         this.cbSymp1.getSelectionModel().select(0);
         this.cbSymp2.getSelectionModel().select(0);
         this.cbSymp3.getSelectionModel().select(0);
         this.cbSymp4.getSelectionModel().select(0);
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      } finally {
         mldg.setRulesets();
      }
   }

   /**
    * *
    * Sets the cb cell factory to display symptom name.
    *
    * @param cb combo box
    */
   private void setCBCellFactory(ComboBox<NameUriPair> cb) {
      cb.setCellFactory((ListView<NameUriPair> p) -> {
         final ListCell<NameUriPair> cell = new ListCell<NameUriPair>() {

            @Override
            protected void updateItem(NameUriPair item, boolean bln) {
               super.updateItem(item, bln);

               if (item != null) {
                  setText(item.toString());
               } else {
                  setText(null);
               }
            }

         };

         return cell;
      });
   }

   /**
    * *
    * wrap the symptom name and uri.
    */
   private class NameUriPair {

      private final String objName;
      private final String objUri;

      public NameUriPair(final String name, final String uri) {
         this.objName = name;
         this.objUri = uri;
      }

      /**
       * *
       *
       * @return uri
       */
      public String getUri() {
         return this.objUri;
      }

      /**
       * *
       *
       * @return
       */
      public String getName() {
         return this.objName;
      }

      /**
       * *
       *
       * @return
       */
      @Override
      public String toString() {
         return this.objName;
      }

      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null) {
            return false;
         }
         if (getClass() != obj.getClass()) {
            return false;
         }
         NameUriPair other = (NameUriPair) obj;
         if (this.objUri == null) {
            if (other.getUri() != null) {
               return false;
            }
         } else if (!this.objUri.equals(other.getUri())) {
            return false;
         }
         return true;
      }

      @Override
      public int hashCode() {
         int hash = 7;
         hash = 79 * hash + Objects.hashCode(this.objUri);
         return hash;
      }
   }
}
