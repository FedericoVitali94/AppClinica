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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
public class SearchDisViewController_uniqueQuery implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(SearchDisViewController_uniqueQuery.class);

   @FXML
   private ComboBox<CBPair> cbSymp1;
   @FXML
   private ComboBox<CBPair> cbSymp2;
   @FXML
   private ComboBox<CBPair> cbSymp3;
   @FXML
   private ComboBox<CBPair> cbSymp4;
   @FXML
   private TableView<DiseaseBySympTableEntry> table;

   private ObservableList<CBPair> symps;
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

      String queryStr = "prefix doidp: <http://purl.obolibrary.org/obo/DOID#>\n"
              + "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
              + "select ?disName (count(?symp) as ?nMatching)\n"
              + "where {\n";
      String unionClause = " {\n"
              + "    ?dis doidp:has_symptom ?symp .\n"
              + "    ?dis rdfs:label ?disName .\n"
              + "    ?symp rdfs:subClassOf* ";
   
      for (int i = 0; i < uris.size(); i++) {
         if (i != 0) {
            queryStr = queryStr.concat("union\n");
         }
         
         queryStr = queryStr.concat(unionClause + "<" + uris.get(i) + ">\n }\n");
      }
      queryStr = queryStr.concat("}\n"
              + "group by ?disName");
      
      LOGGER.debug(queryStr);
      
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();
      try (QueryExecution execution = QueryExecutionFactory.create(queryStr, mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         this.searchResults.clear();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String name = sol.getLiteral("disName").getString();
            int num = sol.getLiteral("nMatching").getInt();
            this.searchResults.add(new DiseaseBySympTableEntry(name, num));
         }
      }
   }

   /**
    * *
    * Return the uri of the chosen item in the combobox.
    *
    * @param cb
    *
    * @return the URI, "" if something is wrong.
    */
   private String getSelectedUri(ComboBox<CBPair> cb) {
      if (cb.isDisabled()) {
         return "";
      }

      CBPair item = cb.getSelectionModel().getSelectedItem();
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
      this.searchResults = FXCollections.observableArrayList();
      this.table.setItems(this.searchResults);
   }

   /**
    * *
    * load symptoms into cbs.
    */
   private void loadSymptomsAndSetCBValues() {
      String queryStr = QueryUtils.loadQueryFromFile("resources/queries/getAllSymptoms.txt");
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();

      mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(queryStr, mldg.toDataset())) {
         this.symps = FXCollections.observableArrayList();
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String sympName = sol.getLiteral("sympName").getString();
            String sympUri = sol.getResource("symp").getURI();
            this.symps.add(new CBPair(sympName, sympUri));
         }
         this.symps.add(0, new CBPair("--Nulla--", ""));
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
         LOGGER.error(exc.getCause());
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
   private void setCBCellFactory(ComboBox<CBPair> cb) {
      cb.setCellFactory((ListView<CBPair> p) -> {
         final ListCell<CBPair> cell = new ListCell<CBPair>() {

            @Override
            protected void updateItem(CBPair item, boolean bln) {
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
   private class CBPair {

      private final String sympName;
      private final String sympUri;

      public CBPair(final String name, final String uri) {
         this.sympName = name;
         this.sympUri = uri;
      }

      /**
       * *
       *
       * @return uri
       */
      public String getUri() {
         return this.sympUri;
      }

      /**
       * *
       *
       * @return
       */
      public String getName() {
         return this.sympName;
      }

      /**
       * *
       *
       * @return
       */
      @Override
      public String toString() {
         return this.sympName;
      }
   }
}
