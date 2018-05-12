/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ehr;

import diseaseDetail.DisDetViewController;
import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
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
import javafx.scene.control.Label;
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
public class EHRViewController implements Initializable {

   private final static Logger LOGGER = Logger.getLogger(EHRViewController.class);

   @FXML
   private Label labAge;
   @FXML
   private Label labBloodType;
   @FXML
   private Label labName;
   @FXML
   private Label labSurname;
   @FXML
   private TableView<SimpleStringProperty> tbvAllergy;
   @FXML
   private TableView<SimpleStringProperty> tbvCurrentDiseases;
   @FXML
   private TableView<ExaminationTableEntry> tbvExamns;
   @FXML
   private TableView<SimpleStringProperty> tbvPastDiseases;

   private ObservableList<SimpleStringProperty> alList;
   private ObservableList<SimpleStringProperty> cDisList;
   private ObservableList<SimpleStringProperty> pDisList;
   private ObservableList<ExaminationTableEntry> examList;
   private MarkLogicDatasetGraph mldg;

   /**
    * *
    * Initializes the controller class.
    *
    * @param url
    * @param rb
    */
   @Override
   public void initialize(URL url, ResourceBundle rb) {
      this.mldg = ServerConnectionManager.getInstance().getDatasetClient();
      this.alList = FXCollections.observableArrayList();
      this.cDisList = FXCollections.observableArrayList();
      this.pDisList = FXCollections.observableArrayList();
      this.examList = FXCollections.observableArrayList();

      this.setCols("Allergie", this.alList, this.tbvAllergy);
      this.setCols("Malattie Passate", this.pDisList, this.tbvPastDiseases);
      this.setCols("Malattie Correnti", this.cDisList, this.tbvCurrentDiseases);
      this.setExamCols();

      //set double click event on table row
      this.setDoubleClickHandler(this.tbvAllergy);
      this.setDoubleClickHandler(this.tbvCurrentDiseases);
      this.setDoubleClickHandler(this.tbvPastDiseases);
      this.setExaminationDoubleClickHandler();
   }

   /**
    * *
    * double click on table row opens a new window with details about the selected disease
    * @param table
    */
   private void setDoubleClickHandler(TableView<SimpleStringProperty> table) {
      table.setRowFactory(tr -> {
         TableRow<SimpleStringProperty> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               String disName = row.getItem().getValue();
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
   
   /***
    * double click on table row opens a new window with details about the selected examination
    */
   private void setExaminationDoubleClickHandler() {
      this.tbvExamns.setRowFactory(tr -> {
         TableRow<ExaminationTableEntry> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               String imgUri = row.getItem().getUri();
               //load examination detail window
               try {
                  FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.EXAM_DET_WIN));
                  Parent view = (Parent) loader.load();
                  loader.<ExaminationViewController>getController().setImg(imgUri);

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
      col.setCellValueFactory(cellData -> {
         return cellData.getValue();
      });

      table.getColumns().add(col);
      table.setItems(obList);
   }

   /**
    * *
    * Sets the columns of the exams table
    */
   private void setExamCols() {
      TableColumn<ExaminationTableEntry, String> nameCol = new TableColumn("Esame");
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      TableColumn<ExaminationTableEntry, String> dateCol = new TableColumn("Data");
      dateCol.setCellValueFactory(cellData -> {
         return cellData.getValue().dateProperty();
      });

      this.tbvExamns.getColumns().addAll(
              nameCol,
              dateCol
      );
      this.tbvExamns.setItems(this.examList);
   }

   /**
    * *
    * Init the window.
    *
    * @param personUri
    */
   public void setPersonAndInit(String personUri) {
      LOGGER.debug("param: " + personUri);
      this.setBaseData(personUri);

      String query = QueryUtils.loadQueryFromFile("getPersonAllergies.txt");
      this.setTableData(personUri, this.alList, query);
      query = QueryUtils.loadQueryFromFile("getPersonCurrentDiseases.txt");
      this.setTableData(personUri, this.cDisList, query);
      query = QueryUtils.loadQueryFromFile("getPersonPastDiseases.txt");
      this.setTableData(personUri, this.pDisList, query);
      query = QueryUtils.loadQueryFromFile("getPersonExaminations.txt");
      this.setExaminationsTableData(personUri, query);
   }

   /**
    * *
    * loads the person base data
    *
    * @param pUri
    */
   private void setBaseData(final String pUri) {
      String queryStr = QueryUtils.loadQueryFromFile("getPersonBaseData.txt");

      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("puri", pUri);

      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         if (res.hasNext()) {
            QuerySolution sol = res.next();
            this.labName.setText(sol.getLiteral("givenName").getString());
            this.labSurname.setText(sol.getLiteral("surname").getString());
            this.labAge.setText(sol.getLiteral("age").getString());
            this.labBloodType.setText(sol.getLiteral("bt").getString());
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      }
   }

   /**
    * *
    * Loads the person data
    *
    * @param pUri
    * @param obList
    * @param query
    */
   private void setTableData(final String pUri,
                             ObservableList<SimpleStringProperty> obList,
                             final String queryStr) {

      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("puri", pUri);

      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            obList.add(new SimpleStringProperty(sol.getLiteral("name").getString()));
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento delle malattie");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }

   /***
    * Load the person examinations.
    * @param queryStr query to execute
    */
   private void setExaminationsTableData(final String pUri, final String queryStr) {
      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("puri", pUri);

      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String label = sol.getLiteral("label").getString();
            String date = sol.getLiteral("date").getString();
            String uri = sol.getLiteral("uri").getString();
            this.examList.add(new ExaminationTableEntry(label, date, uri));
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento degli esami");
         LOGGER.error(exc.getMessage());
      }
   }

}
