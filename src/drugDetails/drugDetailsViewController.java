/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drugDetails;

import com.marklogic.client.MarkLogicServerException;
import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import util.PopUps;
import util.QueryUtils;


/**
 *
 * @author Mattia
 */
public class drugDetailsViewController implements Initializable {
    
    private final static Logger LOGGER = Logger.getLogger(drugDetailsViewController.class);

   @FXML
   private Label txtDrugName;
   @FXML
   private Label txtDrugFunction;
   @FXML
   private TextArea txtDrugFunctionDescription;
   @FXML
   private TableView<SimpleStringProperty> tvSubDrugs;

   private ObservableList<SimpleStringProperty> obsLSubDrugs;
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
      this.obsLSubDrugs = FXCollections.observableArrayList();

      this.setCols("Dosaggio", this.obsLSubDrugs, this.tvSubDrugs);
   }
    
    public void setDrugAndInit(final String drugCod, final String drugName) {
      this.txtDrugName.setText(drugName);
      LOGGER.debug(drugCod);
      LOGGER.debug(drugName);
      
      setDrugFunction(drugCod);
      
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

    private void setDrugFunction(final String drugCode){
    String finalQuery = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                        +"prefix gdrug: <http://clinicaldb/dron> \n"
                        +"prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
                        +"prefix bfo: <http://purl.obolibrary.org/obo/BFO_> \n"
                        +"select ?propertyCode (SAMPLE(?name) AS ?NAME) \n" 
                        +"from named gdrug: \n"
                        +"where { \n";
      String whereClause = 
                    "graph gdrug: { \n";
      

      if (!drugCode.isEmpty()) {
          whereClause = whereClause.concat("dron:"+drugCode+" bfo:0000053 ?propertyCode .\n");
      }
      
      finalQuery = finalQuery.concat(whereClause + "?propertyCode rdfs:label ?name \n" 
                                                    +"} \n"
                                                    +"} \n"
                                                    +"group by ?propertyCode");
      LOGGER.debug(finalQuery);
      String propertyName = "";
      String propertyCode = "";
      //execute the query
      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();

         while (res.hasNext()) {
            QuerySolution sol = res.next();
            propertyName = sol.getLiteral("NAME").getString();
            propertyCode = sol.getResource("propertyCode").getURI();
            LOGGER.debug(propertyName);            
         }
      } finally {
         this.mldg.setRulesets();
         this.txtDrugFunction.setText(propertyName);
      }
      
      
      finalQuery = "prefix gdrug: <http://clinicaldb/dron> \n"
                        +"prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
                        +"prefix obo: <http://purl.obolibrary.org/obo/IAO_> \n"
                        +"select ?propertyDescription \n" 
                        +"from named gdrug: \n"
                        +"where { \n";
      
       whereClause = "graph gdrug: { \n";
      if (!propertyCode.isEmpty()) {
          whereClause = whereClause.concat("dron:"+propertyCode.substring(propertyCode.length()-8, propertyCode.length())+" obo:0000115 ?propertyDescription \n");
      }
      
      finalQuery = finalQuery.concat(whereClause +"} \n"
                                                 +"} ");
      
      LOGGER.debug(finalQuery);
      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();

         while (res.hasNext()) {
            QuerySolution sol = res.next();
            this.txtDrugFunctionDescription.setText(sol.getLiteral("propertyDescription").getString());
         }
      } finally {
         this.mldg.setRulesets();
      }


    }


    
}
