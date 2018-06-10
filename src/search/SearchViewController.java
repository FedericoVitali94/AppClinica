/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package search;

import ehr.EHRViewController;
import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
public class SearchViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(SearchViewController.class);

   @FXML
   private ComboBox<String> cbSpecDis;
   @FXML
   private ComboBox<String> cbTopDis;
   @FXML
   private ToggleGroup radioGroupWhen;
   @FXML
   private RadioButton radioNow;
   @FXML
   private RadioButton radioNowPast;
   @FXML
   private RadioButton radioPast;
   @FXML
   private TableView<SearchTableEntry> table ;
   @FXML
   private TextField tfAgeLB;
   @FXML
   private TextField tfAgeUB;
   @FXML
   private TextField tfName;
   @FXML
   private HBox vboxBloodType;

   private MarkLogicDatasetGraph mldg;
   private ObservableList<SearchTableEntry> searchResults;

   @FXML
   private void handleBackToMenu(ActionEvent event) {
      Redirecter.getInstance().redirect(this.table.getScene(), Redirecter.MAIN_MENU_WIN, true);
   }

   @FXML
   private void handleResetBut(ActionEvent event) {
      this.cbTopDis.getSelectionModel().select(0);
      this.cbSpecDis.getSelectionModel().select(0);
      this.radioNowPast.setSelected(true);
      this.tfAgeLB.setText("");
      this.tfAgeUB.setText("");
      this.tfName.setText("");
      this.vboxBloodType.getChildren().forEach((node) -> {
         ((CheckBox) node).setSelected(false);
      });
      this.searchResults.clear();
   }

   @FXML
   private void handleSearchBut(ActionEvent event) {
      String finalQuery = "prefix foaf: <http://xmlns.com/foaf/0.1/>\n"
              + "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
              + "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
              + "prefix cdata: <http://www.clinicaldb.org/clinicaldata_>\n"
              + "select distinct ?pers ?givenName ?surname ?age ?bt \n"
              + "where { \n";
      String whereClause = "?pers rdf:type foaf:Person . \n"
              + "?pers foaf:givenName ?givenName . \n"
              + "?pers foaf:surname ?surname . \n"
              + "?pers foaf:age ?age . \n"
              + "?pers cdata:BloodType ?bt . \n";

      //partial name to seach
      String partialName = this.tfName.getText();

      //list of blood types to search
      List<String> selectedBTs = new LinkedList<>();
      this.vboxBloodType.getChildren().stream()
               .filter((node) -> (((CheckBox) node).isSelected()))
               .forEachOrdered((node) -> {
                  selectedBTs.add(((CheckBox) node).getText());
               });

      //lower and upper age bound
      int lbAge = this.tfAgeLB.getText().isEmpty() ? -1 : Integer.parseInt(this.tfAgeLB.getText());
      int ubAge = this.tfAgeUB.getText().isEmpty() ? -1 : Integer.parseInt(this.tfAgeUB.getText());

      //disease to search
      String selectedDisease = "";
      if (this.cbSpecDis.isDisabled() || this.cbSpecDis.getSelectionModel().getSelectedItem().equals("--Nulla--")) {
         if (!this.cbTopDis.getSelectionModel().getSelectedItem().equals("--Nulla--")) {
            selectedDisease = this.cbTopDis.getSelectionModel().getSelectedItem();
         }
      } else {
         selectedDisease = this.cbSpecDis.getSelectionModel().getSelectedItem();
      }

      LOGGER.debug("building search query...");
      LOGGER.debug("nome: " + partialName);
      LOGGER.debug("bts: " + selectedBTs.toString());
      LOGGER.debug("age: " + lbAge + " - " + ubAge);
      LOGGER.debug("dis: " + selectedDisease);

      //if partial name is not empty add the filter to the query
      if (!partialName.isEmpty()) {
         whereClause = whereClause.concat("?pers foaf:name ?fullName . \n"
                 + "filter regex(?fullName, \"" + partialName + "\", \"i\") . \n");
      }

      //add the filter for blood types
      if (!selectedBTs.isEmpty()) {
         String bts = "";
         for (String bt : selectedBTs) {
            bts = bts.concat("\"" + bt + "\", ");
         }
         bts = bts.substring(0, bts.length() - 2);
         whereClause = whereClause.concat("filter (?bt in (" + bts + ")) . \n");
      }

      //add age check to query
      if (lbAge != -1 || ubAge != -1) {
         String ageCheck = "";
         if (lbAge != -1 && ubAge != -1) {
            ageCheck = ageCheck.concat("?age > " + lbAge + " && ?age < " + ubAge);
         } else if (lbAge != -1) {
            ageCheck = ageCheck.concat("?age > " + lbAge);
         } else if (ubAge != -1) {
            ageCheck = ageCheck.concat("?age < " + ubAge);
         }

         whereClause = whereClause.concat("filter (" + ageCheck + ") . \n");
      }

      if (!selectedDisease.isEmpty()) {
         String diseaseFilter = "?dis rdfs:label \"" + selectedDisease + "\" . \n";
         String diseaseSubFilter = "?disPers rdfs:subClassOf ?dis . \n";
         if (this.radioNow.isSelected()) {
            String firstUnion = whereClause;
            String secondUnion = whereClause;
            String firstBlock = "?pers cdata:SuffersFrom ?disPers . \n" + diseaseFilter + diseaseSubFilter;
            String secondBlock = "?pers cdata:SuffersFrom ?dis . \n" + diseaseFilter;
            firstUnion = "{ " + firstUnion + firstBlock + "} \n union \n";
            secondUnion = "{ " + secondUnion + secondBlock + "} \n";
            whereClause = firstUnion + secondUnion;
         } else if (this.radioPast.isSelected()) {
            String firstUnion = whereClause;
            String secondUnion = whereClause;
            String firstBlock = "?pers cdata:SufferedFrom ?disPers . \n" + diseaseFilter + diseaseSubFilter;
            String secondBlock = "?pers cdata:SufferedFrom ?dis . \n" + diseaseFilter;
            firstUnion = "{ " + firstUnion + firstBlock + "} \n union \n";
            secondUnion = "{ " + secondUnion + secondBlock + "} \n";
            whereClause = firstUnion + secondUnion;
         } else if (this.radioNowPast.isSelected()) {
            String firstUnion = whereClause;
            String secondUnion = whereClause;
            String firstBlock = "?pers cdata:SuffersFrom ?disPers . \n" + diseaseFilter + diseaseSubFilter;
            String secondBlock = "?pers cdata:SuffersFrom ?dis . \n" + diseaseFilter;
            firstUnion = "{ " + firstUnion + firstBlock + "} \n union \n";
            secondUnion = "{ " + secondUnion + secondBlock + "} \n";

            String firstBigBlock = firstUnion + secondUnion;
            
            firstUnion = whereClause;
            secondUnion = whereClause;
            firstBlock = "?pers cdata:SufferedFrom ?disPers . \n" + diseaseFilter + diseaseSubFilter;
            secondBlock = "?pers cdata:SufferedFrom ?dis . \n" + diseaseFilter;
            firstUnion = "{ \n" + firstUnion + firstBlock + "} \n union \n";
            secondUnion = "{ \n" + secondUnion + secondBlock + "} \n";
            
            String secondBigBlock = firstUnion + secondUnion;
            
            whereClause = "{\n" + firstBigBlock + "}\n union \n{\n" + secondBigBlock + "}\n";
         }
      }

      finalQuery = finalQuery.concat(whereClause + " } ");
      LOGGER.debug(finalQuery);

      //execute the query
      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();

         this.searchResults.clear();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String name = sol.getLiteral("givenName").getString();
            String surname = sol.getLiteral("surname").getString();
            int age = sol.getLiteral("age").getInt();
            String bt = sol.getLiteral("bt").getString();
            String persUri = sol.getResource("pers").getURI();
            this.searchResults.add(new SearchTableEntry(name, surname, age, bt, persUri));
         }
      } finally {
         this.mldg.setRulesets();
      }

   }

   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.setNumbersOnlyTextFields(this.tfAgeLB);
      this.setNumbersOnlyTextFields(this.tfAgeUB);

      //load top diseases classes
      this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
      String queryStr = QueryUtils.loadQueryFromFile("getTopDiseases.txt");

      if (queryStr != null) {
         this.loadValuesAndPopulateComboBox(queryStr, this.cbTopDis);
      } else {
         PopUps.showError("Error", "Errore nel recupero della richiesta per le malattie");
      }

      //the specific disease combobox listen for changes in the primary combobox
      //selection and loads the diseases of the specified type
      this.cbTopDis.getSelectionModel()
              .selectedItemProperty().addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                 if (newValue == null || newValue.equals("") || newValue.equals("disease") || newValue.equals("--Nulla--")) {
                    this.cbSpecDis.setDisable(true);
                    return;
                 }

                 this.cbSpecDis.setDisable(false);
                 String str = QueryUtils.loadQueryFromFile("getDiseasesByTopOne.txt");
                 if (str != null) {
                    //use subclass ruleset to get every sub disease of the selected one
                    this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
                    ParameterizedSparqlString query = new ParameterizedSparqlString();
                    query.setCommandText(str);
                    query.setLiteral("topDisName", newValue);

                    this.loadValuesAndPopulateComboBox(query.asQuery().toString(), this.cbSpecDis);
                 } else {
                    PopUps.showError("Error", "Errore nel recupero della richiesta per le malattie");
                 }

                 //clean the ruleset
                 this.mldg.setRulesets();
                 LOGGER.debug(this.mldg.getRulesets().length);
              });
      
      this.cbSpecDis.setDisable(true);

      //set datamodel for table
      this.setColsSearchTable();
      
      //set double click event on table row
      this.table.setRowFactory(tr -> {
         TableRow<SearchTableEntry> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               String personUri = row.getItem().getID();
               //load detail window
               try {
                  FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.EHR_WIN));
                  Parent view = (Parent) loader.load();
                  //set the person in the new window controller
                  loader.<EHRViewController>getController().setPersonAndInit(personUri);
                  
                  Scene scene = new Scene(view);
                  Stage newStage = new Stage();
                  newStage.setScene(scene);
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
    * Sets a Textfield to only accept numbers.
    *
    * @param tf the textfield
    */
   private void setNumbersOnlyTextFields(TextField tf) {
      //force age fields to only accept numerical values
      UnaryOperator<TextFormatter.Change> filter = change -> {
         String text = change.getText();

         if (text.matches("[0-9]*")) {
            return change;
         }

         return null;
      };

      tf.setTextFormatter(new TextFormatter<>(filter));
   }

   /**
    * *
    * *
    * Executes the input query and puts the results in the specified combobox.
    * The result variable must be "disName".
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
            String disName = sol.getLiteral("disName").getString();
            disList.add(disName);
         }
         disList.add(0, "--Nulla--");
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
      TableColumn<SearchTableEntry, String> nameCol = new TableColumn("Nome");
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      TableColumn<SearchTableEntry, String> surnameCol = new TableColumn("Cognome");
      surnameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().surnameProperty();
      });
      TableColumn<SearchTableEntry, Number> ageCol = new TableColumn("Età");
      ageCol.setCellValueFactory(cellData -> {
         return cellData.getValue().ageProperty();
      });
      TableColumn<SearchTableEntry, String> bloodTypeCol = new TableColumn("Gruppo Sanguigno");
      bloodTypeCol.setCellValueFactory(cellData -> {
         return cellData.getValue().bloodTypeProperty();
      });

      this.table.getColumns().addAll(
              nameCol,
              surnameCol,
              ageCol,
              bloodTypeCol
      );
      this.searchResults = FXCollections.observableArrayList();
      this.table.setItems(this.searchResults);
   }

}
