/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchDrug;

import ehr.EHRViewController;
import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import drugDetails.drugDetailsViewController;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
public class SearchDrugViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(SearchDrugViewController.class);

   @FXML
   private ComboBox<String> cbTopDrugsCategory;
   @FXML
   private TextField tfDrugName;
   @FXML
   private TableView<SearchDrugTableEntry> tvDrugs = new TableView<>();

   private MarkLogicDatasetGraph mldg;
   private ObservableList<SearchDrugTableEntry> searchResults;
   private String categoryCode = "";

   @FXML
   private void handleBackToMenu(ActionEvent event) {
      Redirecter.getInstance().redirect(this.tvDrugs.getScene(), Redirecter.MAIN_MENU_WIN, true);
   }

   @FXML
   private void handleResetBut(ActionEvent event) {
      this.cbTopDrugsCategory.getSelectionModel().select(0);
      this.tfDrugName.setText("");
      this.searchResults.clear();
   }

   @FXML
   private void handleSearchBut(ActionEvent event) {
      
      String finalQuery = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
              + "prefix gdrug: <http://clinicaldb/dron> \n"
              + "prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
              + "select ?drug ?drugName \n" 
              + "where { \n";
      String whereClause = 
                    "graph gdrug: { \n";
        
      //partial name to seach
      String partialName = this.tfDrugName.getText();

      //drug category to search
      String selectedDrugsCategory = "";
        if (!this.cbTopDrugsCategory.getSelectionModel().getSelectedItem().equals("--Nulla--")) {
           selectedDrugsCategory = this.cbTopDrugsCategory.getSelectionModel().getSelectedItem();
        } else {
         LOGGER.debug("nessuna selezione nella combo box");
        }

      LOGGER.debug("building search query...");
      LOGGER.debug("nome: " + partialName);
      LOGGER.debug("dis: " + selectedDrugsCategory);

      if (!selectedDrugsCategory.isEmpty()) {
          categoryCode = selectedDrugsCategory.substring(4, 6);
          whereClause = whereClause.concat("?drug rdfs:subClassOf dron:000000"+categoryCode+" . \n"
                  + "?drug rdfs:label ?drugName \n");
      }
      
      //if partial name is not empty add the filter to the query
      if (!partialName.isEmpty()) {
         whereClause = whereClause.concat("FILTER NOT EXISTS { ?drug rdfs:subClassOf/rdfs:subClassOf+ ?drugName }"+"filter regex(?drugName, \"" + partialName + "\", \"i\") . \n");
      }

      finalQuery = finalQuery.concat(whereClause + " }\n" +
                                                "}\n" +
                                                "order by ?drugName ");
      LOGGER.debug(finalQuery);

      //execute the query
      //this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();

         this.searchResults.clear();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String name = sol.getLiteral("drugName").getString();
            String codDrug = sol.getResource("drug").getURI();
            codDrug = codDrug.substring(codDrug.length() - 8, codDrug.length());
            this.searchResults.add(new SearchDrugTableEntry(codDrug, name));
         }
      } finally {
         this.mldg.setRulesets();
      }
   }

   @Override
   public void initialize(URL url, ResourceBundle rb) {

      //load top diseases classes
      this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
      String queryStr = QueryUtils.loadQueryFromFile("getDrugTopClass.txt");

      if (queryStr != null) {
         this.loadValuesAndPopulateComboBox(queryStr, this.cbTopDrugsCategory);
      } else {
         PopUps.showError("Error", "Errore nel recupero della richiesta per i farmaci");
      }
      
      this.setColsSearchTable();
      this.setDoubleClickHandler();

   }

   /**
    * *
    * *
    * Executes the input query and puts the results in the specified combobox.
    * The result variable must be "drugName".
    *
    * @param query the query tu execute. Must be Select.
    * @param cb the combobox to populate.
    */
   private void loadValuesAndPopulateComboBox(String query, ComboBox<String> cb) {
      try (QueryExecution execution = QueryExecutionFactory.create(query, this.mldg.toDataset())) {
         ObservableList<String> disList = FXCollections.observableArrayList();
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String catName = sol.getLiteral("catName").getString();
            String catCode = sol.getResource("category").getURI();
            categoryCode = catCode.substring(catCode.length() - 2, catCode.length());
            disList.add("ID: "+categoryCode + " - " +catName);
         }
         disList.add(0, "Seleziona categoria ..");
         cb.setItems(disList);
         cb.getSelectionModel().select(0);
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      }
   }

   
   /**
    * *
    * creates the table columns, sets cells factory, and binds table to the list
    * of results
    */
   private void setColsSearchTable() {
       
      TableColumn<SearchDrugTableEntry, String> nameCol = new TableColumn("Nome");
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      
      
      this.tvDrugs.getColumns().addAll(
              nameCol
      );
      this.searchResults = FXCollections.observableArrayList();
      this.tvDrugs.setItems(this.searchResults);
   }
   
   
    /**
    * *
    * double click on table row opens a new window with details about the selected disease
    * @param table
    */
   private void setDoubleClickHandler() {
       this.tvDrugs.setRowFactory(tr -> {
           TableRow<SearchDrugTableEntry> row = new TableRow<>();
           row.setOnMouseClicked(event -> {
               if (event.getClickCount() == 2 && (!row.isEmpty())) {
                   String drugName = row.getItem().getName();
                   String drugCod = row.getItem().getCod();
                   LOGGER.debug(drugName);
                   LOGGER.debug(drugCod);
                   //load disease detail window
                   try {
                       FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.DRUG_DET_WIN));
                       Parent view = (Parent) loader.load();
                       //set the drug in the new window controller
                       loader.<drugDetailsViewController>getController().setDrugAndInit(drugCod, drugName);
                       
                       Scene scene = new Scene(view);
                       Stage newStage = new Stage();
                       newStage.setScene(scene);
                       newStage.setTitle(drugName);
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
}
