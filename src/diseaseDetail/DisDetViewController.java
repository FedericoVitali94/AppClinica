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
import ehr.ExaminationTableEntry;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import searchDrug.SearchDrugTableEntry;
import util.PopUps;
import util.QueryUtils;

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
   private TableView<ExaminationTableEntry> tbvExams;

   private ObservableList<SimpleStringProperty> olIsA;
   private ObservableList<SimpleStringProperty> olSympt;
   private ObservableList<SearchDrugTableEntry> olTherapies;
   private ObservableList<ExaminationTableEntry> olExams;
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
       TableColumn<ExaminationTableEntry, String> col = new TableColumn("Esami di controllo");
       col.setCellValueFactory(new PropertyValueFactory<>("name"));
       this.tbvExams.getColumns().add(col);
       this.tbvExams.setItems(this.olExams);
   }

   public void setDisAndInit(final String disName) {
      //get the disease uri
      String queryStr = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
              + "select ?dis "
              + "where {"
              + " ?dis rdfs:label ?disLab . "
              + " filter regex(?disLab, \"^" + disName + "$\", \"i\") . "
              + "}";
      
      try (QueryExecution execution = QueryExecutionFactory.create(queryStr, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         if (res.hasNext()) {
            QuerySolution sol = res.next();
            this.disUri = sol.getResource("dis").getURI();
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      }
      
      this.setBaseData(this.disUri);
      queryStr = QueryUtils.loadQueryFromFile("getDiseaseHierarchy.txt");
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
   
   private void setExamsData(final String disUri, ObservableList<ExaminationTableEntry> olExams){
       String query = "prefix cdata1: <http://www.clinicaldb.org#clinicaldata_>\n" +
                        "prefix cdata2: <http://www.clinicaldb.org/clinicaldata_>\n" +
                        "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
                        "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select ?examUri ?examName\n" +
                        "where {\n" +
                        "  {\n" +
                        "<"+disUri+"> cdata1:hasMedicalTest ?examUri .\n" +
                        "   ?examUri rdfs:label ?examName\n" +
                        "  }\n" +
                        "UNION\n" +
                        "  {\n" +
                        "<"+disUri+"> cdata2:hasMedicalTest ?examUri .\n" +
                        "   ?examUri rdfs:label ?examName\n" +
                        "  }\n" +
                        "}";
       this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(query, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String examName = sol.getLiteral("examName").getString();
            String examCod = sol.getResource("examUri").getURI();
            olExams.add(new ExaminationTableEntry(examName, "", examCod));
            LOGGER.debug(examName);
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento degli esami");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }
}
