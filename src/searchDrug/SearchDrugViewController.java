/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package searchDrug;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import drugDetails.drugDetailsViewController;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.ResourceBundle;
import javafx.beans.value.ObservableValue;
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
public class SearchDrugViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(SearchDrugViewController.class);

   @FXML
   private ComboBox<String> cbTopDrugsCategory;
   @FXML
   private TextField tfDrugName;
   @FXML
   private TableView<SearchDrugTableEntry> tvDrugs = new TableView<>();

   private MarkLogicDatasetGraph mldg;
   private Map<String, String> drugTypeList;
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
      
        String selectedDrugsCategory = this.cbTopDrugsCategory.getSelectionModel().getSelectedItem();
        for (Entry<String, String> entry : this.drugTypeList.entrySet()) {
                if (Objects.equals(selectedDrugsCategory, entry.getValue())) {
                    categoryCode = entry.getKey();
                }
            }
      executeSearchByCategory(categoryCode);
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
       this.drugTypeList = new HashMap<String,String>();
      try (QueryExecution execution = QueryExecutionFactory.create(query, this.mldg.toDataset())) {
         ObservableList<String> disList = FXCollections.observableArrayList();
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String catName = sol.getLiteral("catName").getString();
            String catCode = sol.getResource("category").getURI();
            categoryCode = catCode.substring(catCode.length() - 2, catCode.length());
            this.drugTypeList.put(categoryCode, catName);
         }
         for(String k : this.drugTypeList.keySet()){
             disList.add(this.drugTypeList.get(k));
         }
         cb.setItems(disList);
         cb.getSelectionModel().select(0);
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      }
      
      /*
       //the specific disease combobox listen for changes in the primary combobox
      //selection and loads the diseases of the specified type
      this.cbTopDrugsCategory.getSelectionModel()
              .selectedItemProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                     for (Entry<String, String> entry : this.drugTypeList.entrySet()) {
                        if (Objects.equals(newValue, entry.getValue())) {
                            executeSearchByCategory(entry.getKey());
                        }
                    return;
                 }});
      */
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
   
   
   private void executeSearchByCategory(String categoryCod){
       String finalQuery = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
              + "prefix gdrug: <http://clinicaldb/dron> \n"
              + "prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
              + "select ?drug ?drugName \n" 
              + "where { \n";
      String whereClause = 
                    "graph gdrug: { \n";
        
      //partial name to seach
      String partialName = this.tfDrugName.getText();

      //drug categoryCod to search

      LOGGER.debug("building search query...");
      LOGGER.debug("nome: " + partialName);
      LOGGER.debug("dis: " + categoryCod);

      if (!categoryCod.isEmpty()) {
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
}
