/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package recommendedSympExams;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import drugDetails.drugDetailsViewController;
import java.io.IOException;
import java.net.URL;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import searchDrug.SearchDrugTableEntry;
import util.PopUps;
import util.QueryUtils;
import util.Redirecter;

/**
 * FXML Controller class
 *
 * @author Mattia
 */
public class ArtificialDiagnosisViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(ArtificialDiagnosisViewController.class);
   
   @FXML
   private TableView<TableEntry> tableSymptoms = new TableView<>();
   @FXML
   private TableView<TableEntry> tableDisease = new TableView<>();
   @FXML
   private TableView<TableEntry> tableExaminations = new TableView<>();
   

   private ObservableList<TableEntry> symps;
   private ObservableList<TableEntry> disease;
   private ObservableList<TableEntry> exams;
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

      //setup the table
      this.setColsSearchTable();
      this.populateSymptomsTable();
      this.setSymptomDoubleClickHandler(this.tableSymptoms);
      this.setDiseaseDoubleClickHandler(this.tableDisease);
    }    
    
    @FXML
   private void handleBackToMenu(ActionEvent event) {
      Redirecter.getInstance().redirect(this.tableSymptoms.getScene(), Redirecter.MAIN_MENU_WIN, true);
   }
   
    private void setColsSearchTable() {
      
      TableColumn<TableEntry, String> nameCol = new TableColumn("Symptoms");
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      this.tableSymptoms.getColumns().addAll(nameCol);
      this.symps = FXCollections.observableArrayList();
      this.tableSymptoms.setItems(this.symps);
      
      
      TableColumn<TableEntry, String> disCol = new TableColumn("Diseases");
      disCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      this.tableDisease.getColumns().addAll(disCol);
      this.disease = FXCollections.observableArrayList();
      this.tableDisease.setItems(this.disease);
      
      
      
      TableColumn<TableEntry, String> examsCol = new TableColumn("Recommended Examinations");
      examsCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      this.tableExaminations.getColumns().addAll(examsCol);
      this.exams = FXCollections.observableArrayList();
      this.tableExaminations.setItems(this.exams);
   }
    
    private void populateSymptomsTable(){
        String queryStr = QueryUtils.loadQueryFromFile("getAllUsedSymptoms.txt");
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();

      mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(queryStr, mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String sympName = sol.getLiteral("sympName").getString();
            String sympUri = sol.getResource("symp").getURI();
            this.symps.add(new TableEntry(sympName, sympUri));
            
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
//         LOGGER.error(exc.getMessage());
      } finally {
         mldg.setRulesets();
      }
    }
    
    
    private void setSymptomDoubleClickHandler(TableView<TableEntry> table){
        this.tableSymptoms.setRowFactory(tr -> {
           TableRow<TableEntry> row = new TableRow<>();
           row.setOnMouseClicked(event -> {
               if (event.getClickCount() == 2 && (!row.isEmpty())) {
                   String symptomName = row.getItem().getName();
                   String symptomCod = row.getItem().getCod();
                   LOGGER.debug(symptomName);
                   LOGGER.debug(symptomCod);
                   
                   this.disease.clear();
                   populateDiseasesTable(symptomCod);
               }
           });
           return row;
       });
    }
    
    private void populateDiseasesTable(String symptomCod){
        String queryStr = QueryUtils.loadQueryFromFile("getDiseasesBySymptom.txt");
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();
      mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      ParameterizedSparqlString query = new ParameterizedSparqlString();
        query.setCommandText(queryStr);
        query.setIri("sympUri", symptomCod);
        LOGGER.debug(query.toString());
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String disName = sol.getLiteral("disName").getString();
            String disUri = sol.getResource("dis").getURI();
            this.disease.add(new TableEntry(disName, disUri));
            
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
//         LOGGER.error(exc.getMessage());
      } finally {
         mldg.setRulesets();
      }
    }
    private void setDiseaseDoubleClickHandler(TableView<TableEntry> table){
         this.tableDisease.setRowFactory(tr -> {
           TableRow<TableEntry> row = new TableRow<>();
           row.setOnMouseClicked(event -> {
               if (event.getClickCount() == 2 && (!row.isEmpty())) {
                   String disName = row.getItem().getName();
                   String disCod = row.getItem().getCod();
                   LOGGER.debug(disName);
                   LOGGER.debug(disCod);
                   
                   this.exams.clear();
                   populateExaminationsTable(disCod);
               }
           });
           return row;
       });
    }
 
    private void populateExaminationsTable(String disCod){
        
    }
    
}
