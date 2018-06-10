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
import examDetails.ExamInfoTableEntry;
import examDetails.ExaminationInfoViewController;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
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
 * @author Mattia
 */
public class ArtificialDiagnosisViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(ArtificialDiagnosisViewController.class);
   
   @FXML
   private TableView<TableEntry> tableDisease = new TableView<>();
   @FXML
   private TableView<TableEntry> tableExams = new TableView<>();
   @FXML
   private ComboBox<NameUriPair> cbSymp1;
   @FXML
   private ComboBox<NameUriPair> cbSymp2;

   private ObservableList<NameUriPair> symps;
   private ObservableList<TableEntry> disease;
   private ObservableList<TableEntry> exams;
   
    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

      //setup the table
      this.setColsSearchTable();
      this.setCBCellFactory(this.cbSymp1);
      this.setCBCellFactory(this.cbSymp2);
      this.loadSymptomsAndSetCBValues();
      this.setDiseaseDoubleClickHandler(this.tableDisease);
      this.setExamsDoubleClickHandler(this.tableExams);
    }    
    
   @FXML
   private void handleReset(ActionEvent event){
       this.exams.clear();
       this.disease.clear();
       this.cbSymp1.getSelectionModel().select(0);
       this.cbSymp2.getSelectionModel().select(0);
   }
    @FXML
   private void handleBackToMenu(ActionEvent event) {
      Redirecter.getInstance().redirect(this.tableDisease.getScene(), Redirecter.MAIN_MENU_WIN, true);
   }
   
   @FXML
   private void handleSearch(ActionEvent event){
        this.exams.clear();
       this.disease.clear();
       // create a list of selected symptoms uris
      List<String> uris = new ArrayList<>();
      uris.add(this.getSelectedUri(this.cbSymp1));
      uris.add(this.getSelectedUri(this.cbSymp2));

      LOGGER.debug(uris.toString());
      //remove null or "" uris from the list
      uris.removeIf(item -> {
         return this.isNullOrEmpty(item);
      });

      String queryStr = QueryUtils.loadQueryFromFile("getDiseasesBySymptom.txt");
      //for each symptom uri we query the db for diseases which have the property has_symptom
      //matching the selected symptom or one of his sub classes
      uris.stream().forEach((String item) -> {
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
               this.disease.add(new TableEntry(name, uri));
            }
         }
      });

   }
   
    private void setColsSearchTable() {
      
      TableColumn<TableEntry, String> disCol = new TableColumn("Diseases");
      disCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      this.tableDisease.getColumns().addAll(disCol);
      this.disease = FXCollections.observableArrayList();
      this.tableDisease.setItems(this.disease);
      
      
      TableColumn<TableEntry, String> examsCol = new TableColumn("Recommended Exams");
      examsCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      this.tableExams.getColumns().addAll(examsCol);
      this.exams = FXCollections.observableArrayList();
      this.tableExams.setItems(this.exams);
   }

    private void loadSymptomsAndSetCBValues() {
      String queryStr = QueryUtils.loadQueryFromFile("getAllUsedSymptoms.txt");
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
         this.cbSymp1.getSelectionModel().select(0);
         this.cbSymp2.getSelectionModel().select(0);
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      } finally {
         mldg.setRulesets();
      }
   }
    
    private void setDiseaseDoubleClickHandler(TableView<TableEntry> table){
       this.tableDisease.setRowFactory(tr -> {
           TableRow<TableEntry> row = new TableRow<>();
           row.setOnMouseClicked(event -> {
               if (event.getClickCount() == 2 && (!row.isEmpty())) {
                   String disCod = row.getItem().getCod();
                   
                   this.exams.clear();
                   populateExaminationsTable(disCod);
               }
           });
           return row;
       });
    }
 
    private void populateExaminationsTable(String disCod){
        String queryStr = QueryUtils.loadQueryFromFile("getExamsByDisease.txt");
      MarkLogicDatasetGraph mldg = ServerConnectionManager.getInstance().getDatasetClient();
      mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      ParameterizedSparqlString query = new ParameterizedSparqlString();
        query.setCommandText(queryStr);
        query.setIri("disUri", disCod);
        LOGGER.debug(query.toString());
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String disName = sol.getLiteral("examName").getString();
            String disUri = sol.getResource("examCod").getURI();
            this.exams.add(new TableEntry(disName, disUri));
            
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
//         LOGGER.error(exc.getMessage());
      } finally {
         mldg.setRulesets();
      }
       /* DatabaseClient client = DatabaseClientFactory.newClient("localhost",8000,new DatabaseClientFactory.DigestAuthContext("moriani", "andresilva"));
       ServerEvaluationCall theCall = client.newServerEval();
        String query = "let $my-store := sem:ruleset-store(\"/rules/myRules2.rules\", sem:store() )\n" +
                        "return\n" +
                        " sem:sparql('\n" +
                        "prefix cdata: <http://www.clinicaldb.org/clinicaldata_>\n" +
                        "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select *\n" +
                        "where{\n" +
                        "  <"+sympCod+"> cdata:RecommendedExamination ?examination . \n" +
                        "  ?examination rdfs:label ?examinationName\n" +
                        "}\n" +
                        "', (), (),\n" +
                        " $my-store\n" +
                        " )";
        theCall.xquery(query);
        EvalResultIterator result = theCall.eval();
        while (result.hasNext()) {
            EvalResult sol = result.next();
            JacksonHandle handle = sol.get(new JacksonHandle());
            this.exams.add(new TableEntry(handle.get().get("examinationName").asText(), handle.get().get("examination").asText()));
         }
*/
    }
    
    
    private void setExamsDoubleClickHandler(TableView<TableEntry> table){
        this.tableExams.setRowFactory(tr -> {
         TableRow<TableEntry> row = new TableRow<>();
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
   }
    
}
