/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package diseaseDetail;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import drugDetails.drugDetailsViewController;
import examDetails.ExamInfoTableEntry;
import examDetails.ExaminationInfoViewController;
import examDetails.ExaminationTableEntry;
import examDetails.ExaminationViewController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
 * @author den
 */
public class DisDetViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(DisDetViewController.class);

   @FXML
   private TextArea taDescription;
   @FXML
   private TableView<SimpleStringProperty> tbvIsA;
   @FXML
   private TableView<SimpleStringProperty> tbvSympt;
   
   @FXML
   private TableView<SearchDrugTableEntry> tbvTherapies;
   
   @FXML
   private TableView<ExamInfoTableEntry> tbvExams;

   private ObservableList<SimpleStringProperty> olIsA;
   private ObservableList<SimpleStringProperty> olSympt;
   private ObservableList<SearchDrugTableEntry> olTherapies;
   private ObservableList<ExamInfoTableEntry> olExams;
   private MarkLogicDatasetGraph mldg;
   private String disUri;

   /**
    * Initializes the controller class.
    * @param url
    * @param rb
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
      this.olIsA = FXCollections.observableArrayList();
      this.olSympt = FXCollections.observableArrayList();
      this.olTherapies = FXCollections.observableArrayList();
      this.olExams = FXCollections.observableArrayList();

      this.setCols("Appartenenza", this.olIsA, this.tbvIsA);
      this.setCols("Sintomi Comuni", this.olSympt, this.tbvSympt);
      this.setTherapyTableCol();
      this.setExamsTableCol();
      
      this.setDrugDoubleClickHandler();
      this.setExamsDoubleClickHandler();
   }

   /**
    * *
    * set the column and column cell factory
    *
    * @param colName
    * @param obList
    * @param table
    */
   private void setCols(final String colName,
                        ObservableList<SimpleStringProperty> obList,
                        TableView<SimpleStringProperty> table) {
      TableColumn<SimpleStringProperty, String> col = new TableColumn(colName);
      col.setCellValueFactory(
              new PropertyValueFactory<>("value"));

      table.getColumns().add(col);
      table.setItems(obList);
   }
   
   private void setTherapyTableCol(){
       TableColumn<SearchDrugTableEntry, String> col = new TableColumn("Farmaci Consigliati");
       col.setCellValueFactory(new PropertyValueFactory<>("name"));
       this.tbvTherapies.getColumns().add(col);
       this.tbvTherapies.setItems(this.olTherapies);
   }
   
   private void setExamsTableCol(){
       TableColumn<ExamInfoTableEntry, String> col = new TableColumn("Esami di controllo");
       col.setCellValueFactory(new PropertyValueFactory<>("name"));
       this.tbvExams.getColumns().add(col);
       this.tbvExams.setItems(this.olExams);
   }

   public void setDisAndInit(final String disName, final String disUri) {
     
      this.disUri = disUri;
        
      this.setBaseData(this.disUri);
      String queryStr = QueryUtils.loadQueryFromFile("getDiseaseHierarchy.txt");
      this.setTableData(this.disUri, this.olIsA, queryStr, SPARQLRuleset.SUBCLASS_OF);
      queryStr = QueryUtils.loadQueryFromFile("getDiseaseSymptoms.txt");      
      this.setTableData(this.disUri, this.olSympt, queryStr, SPARQLRuleset.OWL_HORST);
      
      this.setTherapyData(this.disUri, this.olTherapies);
      this.setExamsData(this.disUri, this.olExams);
   }
   
   /***
    * 
    * @param disUri 
    */
   private void setBaseData(final String disUri) {
      String queryStr = QueryUtils.loadQueryFromFile("getDiseaseBaseData.txt");
      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("dis", disUri);
      
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         if (res.hasNext()) {
            QuerySolution sol = res.next();
            this.taDescription.setText(sol.getLiteral("descr").getString());
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento dei dati base della malattia");
         LOGGER.error(exc.getMessage());
      }
   }
   
   /***
    * 
    * @param disUri
    * @param obList
    * @param queryStr 
    */
   private void setTableData(final String disUri,
                             ObservableList<SimpleStringProperty> obList,
                             final String queryStr,
                             SPARQLRuleset ruleset) {
      
      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("dis", disUri);

      this.mldg.setRulesets(ruleset);
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            obList.add(new SimpleStringProperty(sol.getLiteral("name").getString()));
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle tabelle");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }

   private void setTherapyData(final String disUri, ObservableList<SearchDrugTableEntry> olDrug){
       String query = "prefix cdata1: <http://www.clinicaldb.org/clinicaldata_>\n" +
                        "prefix cdata2: <http://www.clinicaldb.org#clinicaldata_>\n" +
                        "prefix foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select ?drugUri ?drugName\n" +
                        "where {\n" +
                        "  {\n" +
                        "<"+disUri+"> cdata1:HasTherapy ?drugUri .\n" +
                        "   ?drugUri rdfs:label ?drugName\n" +
                        "  }\n" +
                        "  UNION{\n" +
                        "<"+disUri+"> cdata2:HasTherapy ?drugUri .\n" +
                        "   ?drugUri rdfs:label ?drugName\n" +
                        "  }\n" +
                        "}";
       this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(query, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String drugName = sol.getLiteral("drugName").getString();
            String drugCod = sol.getResource("drugUri").getURI();
            olDrug.add(new SearchDrugTableEntry(drugCod, drugName));
            LOGGER.debug(drugName);
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle terapie");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }
   
   private void setExamsData(final String disUri, ObservableList<ExamInfoTableEntry> olExams){
       String query = "prefix cdata: <http://www.clinicaldb.org/clinicaldata_>\n" +
                        "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select ?examUri ?examName\n" +
                        "where \n" +
                        "  {\n" +
                        "<"+disUri+"> cdata:hasMedicalTest ?examUri .\n" +
                        "   ?examUri rdfs:label ?examName \n" +
                        "}";
       this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(query, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String examName = sol.getLiteral("examName").getString();
            String examCod = sol.getResource("examUri").getURI();
            olExams.add(new ExamInfoTableEntry(examCod, examName));
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento degli esami");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }


   private void setDrugDoubleClickHandler(){
        this.tbvTherapies.setRowFactory(tr -> {
           TableRow<SearchDrugTableEntry> row = new TableRow<>();
           row.setOnMouseClicked(event -> {
               if (event.getClickCount() == 2 && (!row.isEmpty())) {
                   String drugName = row.getItem().getName();
                   String drugCod = row.getItem().getCod();
                   drugCod = drugCod.substring(drugCod.length() - 8, drugCod.length());
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
                       PopUps.showError("Errore", "Impossibile caricare la pagina del farmaco");
                       LOGGER.error(exc.getMessage());
                   }
               }
           });
           return row;
       });
   }
   
   private void setExamsDoubleClickHandler(){
        this.tbvExams.setRowFactory(tr -> {
         TableRow<ExamInfoTableEntry> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               //load examination detail window
               try {
                  FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.EXAM_INFO_WIN));
                  Parent view = (Parent) loader.load();
                  loader.<ExaminationInfoViewController>getController().setExamAndInit(row.getItem().getCod(), row.getItem().getName());

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


}
