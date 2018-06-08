/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ehr;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import diseaseDetail.DisDetViewController;
import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.eval.EvalResult;
import com.marklogic.client.eval.EvalResultIterator;
import com.marklogic.client.eval.ServerEvaluationCall;
import com.marklogic.client.io.JacksonHandle;
import com.marklogic.client.io.marker.JSONReadHandle;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import diseaseDetail.DiseaseTableEntry;
import drugDetails.drugDetailsViewController;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.FileManager;
import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.ModelExtractor;
import org.mindswap.pellet.jena.PelletReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import searchDrug.SearchDrugTableEntry;
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
   private TableView<DiseaseTableEntry> tbvAllergy;
   @FXML
   private TableView<DiseaseTableEntry> tbvCurrentDiseases;
   @FXML
   private TableView<ExaminationTableEntry> tbvExamns;
   @FXML
   private TableView<DiseaseTableEntry> tbvPastDiseases;
   @FXML
   private TableView<SearchDrugTableEntry> tbvRecommendedDrugs;
   @FXML
   private TableView<SearchDrugTableEntry> tbvAssumedDrugs;

   private ObservableList<DiseaseTableEntry> alList;
   private ObservableList<DiseaseTableEntry> cDisList;
   private ObservableList<DiseaseTableEntry> pDisList;
   private ObservableList<ExaminationTableEntry> examList;
   private ObservableList<SearchDrugTableEntry> recommendedDrugsList;
   private ObservableList<SearchDrugTableEntry> assumedDrugsList;
   private MarkLogicDatasetGraph mldg;
   private String personURI;

   
   private void reasoningRecommendedDrugs(){
       //String inputFileName = "C:\\Users\\Mattia\\Desktop\\myOntology.owl";  
       // Model model = FileManager.get().loadModel(inputFileName);
       Model model = ModelFactory.createModelForGraph(this.mldg.getGraph(Node.ANY));
  
       //Dataset dataset = this.mldg.toDataset();
       LOGGER.debug("minne "+ model.size());
       //this.mldg.setRulesets(SPARQLRuleset.ruleset("C:\\Program Files\\MarkLogic\\Config\\recommendedDrug.rules"));
            //Setting up rules
          String rule = "[rule1:(?a http://www.clinicaldb.org/clinicaldata_SuffersFrom ?b)  " +
                        "(?b http://www.clinicaldb.org/clinicaldata_HasTherapy ?c)" +
                      "->(?a http://www.clinicaldb.org/clinicaldata_RecommendedDrug ?c)]";

          //query String
          String queryString = "PREFIX cdata:<http://www.clinicaldb.org/>" +
        "SELECT *"  +
        "WHERE {?x <http://www.clinicaldb.org/clinicaldata_RecommendedDrug> ?z}";

        //set up reasoner
        Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rule));

        InfModel inf = ModelFactory.createInfModel(reasoner, model);

        Query query = QueryFactory.create(queryString);
        QueryExecution qe = QueryExecutionFactory.create(query, inf);
        ResultSet results = qe.execSelect();

        for ( ; results.hasNext() ; ) {
            QuerySolution soln = results.nextSolution() ;
            System.out.println("minne ");
            System.out.print("minne "+soln.getResource("x").getURI());
            System.out.print("minne    ");
            System.out.println("minne "+soln.getResource("z").getURI());
        }


        /*output result*/
        //ResultSetFormatter.out(System.out, results, recommendedDrugsQuery);
        qe.close(); 
       
   }
  
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
      this.recommendedDrugsList = FXCollections.observableArrayList();
      this.assumedDrugsList = FXCollections.observableArrayList();

      this.setCols("Allergie", this.alList, this.tbvAllergy);
      this.setCols("Malattie Passate", this.pDisList, this.tbvPastDiseases);
      this.setCols("Malattie Correnti", this.cDisList, this.tbvCurrentDiseases);
      this.setDrugsCols();
      this.setExamCols();

      
      //set double click event on table row
      this.setDoubleClickHandler(this.tbvAllergy);
      this.setDoubleClickHandler(this.tbvCurrentDiseases);
      this.setDoubleClickHandler(this.tbvPastDiseases);
      this.setDrugDoubleClickHandler();
      this.setExaminationDoubleClickHandler();
       
   }

   /**
    * *
    * double click on table row opens a new window with details about the selected disease
    * @param table
    */
   private void setDoubleClickHandler(TableView<DiseaseTableEntry> table) {
      table.setRowFactory(tr -> {
         TableRow<DiseaseTableEntry> row = new TableRow<>();
         row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && (!row.isEmpty())) {
               String disName = row.getItem().getName();
               String disCod = row.getItem().getCod();
               //load disease detail window
               try {
                   FXMLLoader loader;
                   Parent view = null;
                    loader = new FXMLLoader(getClass().getClassLoader().getResource(Redirecter.DISEASE_DET_WIN));
                    view = (Parent) loader.load();
                  //set the person in the new window controller
                  loader.<DisDetViewController>getController().setDisAndInit(disName, disCod);
                   

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

   private void setDrugDoubleClickHandler(){
       this.tbvRecommendedDrugs.setRowFactory(tr -> {
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
       
       this.tbvAssumedDrugs.setRowFactory(tr -> {
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
   /**
    * *
    * set the column and column cell factory
    *
    * @param colName
    * @param obList
    * @param table
    */
   private void setCols(final String colName,
                        ObservableList<DiseaseTableEntry> obList,
                        TableView<DiseaseTableEntry> table) {
      TableColumn<DiseaseTableEntry, String> col = new TableColumn(colName);
      col.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
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
      this.personURI = personUri;
      String query = QueryUtils.loadQueryFromFile("getPersonAllergies.txt");
      this.setTableData(personUri, this.alList, query);
      query = QueryUtils.loadQueryFromFile("getPersonCurrentDiseases.txt");
      this.setTableData(personUri, this.cDisList, query);
      query = QueryUtils.loadQueryFromFile("getPersonPastDiseases.txt");
      this.setTableData(personUri, this.pDisList, query);
      query = QueryUtils.loadQueryFromFile("getPersonExaminations.txt");
      this.setExaminationsTableData(personUri, query);
      
     this.reasoning();
   }
   
   private void reasoning(){
        
       DatabaseClient client = DatabaseClientFactory.newClient("localhost",8000,new DigestAuthContext("moriani", "andresilva"));
       ServerEvaluationCall theCallForRecommendedDrugs = client.newServerEval();
       ServerEvaluationCall theCallForAssumedDrugs = client.newServerEval();
        String recommendedDrugsQuery = "let $my-store := sem:ruleset-store(\"/rules/myRules.rules\", sem:store() )\n" +
                        "return\n" +
                        " sem:sparql('\n" +
                        "prefix cdata: <http://www.clinicaldb.org/clinicaldata_>\n" +
                        "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                        "select *\n" +
                        "where{\n" +
                        "  <"+personURI+"> cdata:RecommendedDrug ?drugCod . \n" +
                        "  ?drugCod rdfs:label ?drugName\n" +
                        "}\n" +
                        "', (), (),\n" +
                        " $my-store\n" +
                        " )";
        
         String assumedDrugsQuery = "let $my-store := sem:ruleset-store(\"/rules/myRules.rules\", sem:store() )\n" +
                                    "return\n" +
                                    " sem:sparql('\n" +
                                    "prefix cdata: <http://www.clinicaldb.org/clinicaldata_>\n" +
                                    "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                    "select *\n" +
                                    "where{\n" +
                                    "  <"+personURI+"> cdata:TakesMedication ?drugCod . \n" +
                                    "  ?drugCod rdfs:label ?drugName\n" +
                                    "}\n" +
                                    "', (), (),\n" +
                                    " $my-store\n" +
                                    " )";
         
        theCallForRecommendedDrugs.xquery(recommendedDrugsQuery);
        EvalResultIterator recommendedDrugsResult = theCallForRecommendedDrugs.eval();
        while (recommendedDrugsResult.hasNext()) {
            EvalResult sol = recommendedDrugsResult.next();
            JacksonHandle handle = sol.get(new JacksonHandle());
            this.recommendedDrugsList.add(new SearchDrugTableEntry(handle.get().get("drugCod").asText(), handle.get().get("drugName").asText()));
         }
        
        
         theCallForAssumedDrugs.xquery(assumedDrugsQuery);
        EvalResultIterator assumedDrugsResult = theCallForAssumedDrugs.eval();
        while (assumedDrugsResult.hasNext()) {
            EvalResult sol = assumedDrugsResult.next();
            JacksonHandle handle = sol.get(new JacksonHandle());
            this.assumedDrugsList.add(new SearchDrugTableEntry(handle.get().get("drugCod").asText(), handle.get().get("drugName").asText()));
         }
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
                             ObservableList<DiseaseTableEntry> obList,
                             final String queryStr) {

      ParameterizedSparqlString query = new ParameterizedSparqlString();
      query.setCommandText(queryStr);
      query.setIri("puri", pUri);

      LOGGER.debug(query);
      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(query.asQuery(), this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            String name = sol.getLiteral("name").getString();
            String uri = sol.getResource("dis").getURI();
            obList.add(new DiseaseTableEntry(uri, name));
            
         }
      } catch (MarkLogicServerException exc) {
         PopUps.showError("Errore", "Errore del server durante il caricamento dei dati");
         LOGGER.error(exc.getMessage());
      } finally {
         this.mldg.setRulesets();
      }
   }

   /***
    * Load the person examinations.
    * @param queryStr recommendedDrugsQuery to execute
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
   
   private void setDrugsCols(){
       TableColumn<SearchDrugTableEntry, String> recDrugsCol = new TableColumn("Farmaci Consigliati");
      recDrugsCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      
      
      this.tbvRecommendedDrugs.getColumns().addAll(
              recDrugsCol
      );
      this.tbvRecommendedDrugs.setItems(this.recommendedDrugsList);
      
      
      TableColumn<SearchDrugTableEntry, String> assDrugsCol = new TableColumn("Farmaci Assunti");
      assDrugsCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      
      
      this.tbvAssumedDrugs.getColumns().addAll(
              assDrugsCol
      );
      this.tbvAssumedDrugs.setItems(this.assumedDrugsList);
   }

}
