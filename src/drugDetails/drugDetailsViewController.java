/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package drugDetails;

import com.marklogic.client.semantics.SPARQLRuleset;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import db.ServerConnectionManager;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.log4j.Logger;
import searchDrug.SearchDrugTableEntry;


/**
 *
 * @author Mattia
 */
public class drugDetailsViewController implements Initializable {
    
    private final static Logger LOGGER = Logger.getLogger(drugDetailsViewController.class);
    private boolean bearer_set;
    
   @FXML
   private Label txtDrugName;
   @FXML
   private Label txtProperty0;
   @FXML
   private Label txtProperty1;
   @FXML
   private Label txtProperty2;
   @FXML
   private TextArea txtProperty0Desc;
   @FXML
   private TextArea txtProperty1Desc;
   @FXML 
   private TextArea txtProperty2Desc;
   @FXML
   private TableView<SearchDrugTableEntry> tvSubDrugs;

   private ObservableList<SearchDrugTableEntry> obsLSubDrugs;
   private MarkLogicDatasetGraph mldg;

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
      setDrugProperties(drugCod);
      setSubDrugs(drugCod);
      
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
                        ObservableList<SearchDrugTableEntry> obList,
                        TableView<SearchDrugTableEntry> table) {
      TableColumn<SearchDrugTableEntry, String> nameCol = new TableColumn(colName);
      nameCol.setCellValueFactory(cellData -> {
         return cellData.getValue().nameProperty();
      });
      
      this.tvSubDrugs.getColumns().addAll(
              nameCol
      );
      
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
      }
      
      if(!propertyCode.isEmpty()){
          this.txtProperty0.setText(propertyName.toUpperCase());
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
              this.txtProperty0Desc.setText(sol.getLiteral("propertyDescription").getString());
              bearer_set = true;
           }
        } finally {
           this.mldg.setRulesets();
        }
      }
      else{
          //se non c'è is_bearer_of
          bearer_set = false;
      }


    }


    private void setDrugProperties(final String drugCode){
        String finalQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n"
                            +"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
                            +"prefix owl: <http://www.w3.org/2002/07/owl#> \n"
                            +"prefix gdrug: <http://clinicaldb/dron> \n"
                            +"prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
                            +"select (SAMPLE(?h) AS ?H) (SAMPLE(?g) AS ?G)\n"
                            +"from named gdrug: \n"
                            +"where { \n"
                              +"graph gdrug: { \n";
      
        String whereClause = "";
      if (!drugCode.isEmpty()) {
          whereClause = whereClause.concat("dron:"+drugCode+" rdfs:subClassOf ?x .\n" +
                                            "    ?x owl:someValuesFrom ?z .\n" +
                                            "    ?z owl:intersectionOf ?e .\n" +
                                            "    ?e rdf:rest*/rdf:first ?f .\n" +
                                            "    ?f owl:someValuesFrom ?g . \n" +
                                            "    ?g rdfs:label ?h");
                                                  }
      
      finalQuery = finalQuery.concat(whereClause    +"} \n"
                                                    +"} \n"
                                                    +"group by ?f");
      
      LOGGER.debug(finalQuery);
      ObservableMap<String,String> observableMap;
      Map<String,String> mapResult = new HashMap<>();
      //execute the query
      this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
      try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
         ResultSet res = execution.execSelect();
        
         while (res.hasNext()) {
            QuerySolution sol = res.next();
            mapResult.put(sol.getResource("G").getURI(),sol.getLiteral("H").getString());          
         }
         LOGGER.debug(res);
      } finally {
         this.mldg.setRulesets();
      }
      
      if(!mapResult.isEmpty()){
          int i=0;
          for(String key : mapResult.keySet()){
              
            LOGGER.debug(key);
            LOGGER.debug(mapResult.get(key));
            if(!bearer_set){
                if(i==0){
                    this.txtProperty0.setText(mapResult.get(key).toUpperCase());
                }
                else if(i==1){
                    this.txtProperty1.setText(mapResult.get(key).toUpperCase());
                }else{
                    this.txtProperty2.setText(mapResult.get(key).toUpperCase());
                }
            }
            else{
                if(i==0){
                    this.txtProperty1.setText(mapResult.get(key).toUpperCase());
                }
                else if(i==1){
                    this.txtProperty2.setText(mapResult.get(key).toUpperCase());
                }
            }
            setPropertyDescription(key, i);
            i++;
            
            if(mapResult.size() < 3){
                this.txtProperty2.setVisible(false);
                this.txtProperty2Desc.setVisible(false);
            }
        }
        
      }
      else{
          this.txtProperty1.setVisible(false);
          this.txtProperty1Desc.setVisible(false);
          this.txtProperty2.setVisible(false);
          this.txtProperty2Desc.setVisible(false);
      }
      
    }
    
    private void setPropertyDescription(String key, int i){
        LOGGER.debug(key);
            String finalQuery = "prefix library: <http://purl.obolibrary.org/obo/>\n" +
                                "prefix iao: <http://purl.obolibrary.org/obo/IAO_> \n"
                                +"prefix gdrug: <http://clinicaldb/dron> \n"
                            +"select ?desc\n"
                            +"from named gdrug: \n"
                            +"where { \n"
                              +"graph gdrug: { \n";
      
            String whereClause = "";
            if (!key.isEmpty()) {
                whereClause = whereClause.concat("library:"+key.substring(31)+" iao:0000115 ?desc \n" );
                                                        }

            finalQuery = finalQuery.concat(whereClause    +"} \n"
                                                          +"} \n");

            LOGGER.debug(finalQuery);
            
            //execute the query
            this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
            try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
               ResultSet res = execution.execSelect();

               while (res.hasNext()) {
                  QuerySolution sol = res.next();
                  if(!bearer_set){
                      if(i==0){
                        this.txtProperty0Desc.setText(sol.getLiteral("desc").getString());
                    }
                    else if(i==1){
                        this.txtProperty1Desc.setText(sol.getLiteral("desc").getString());
                    }else{
                        this.txtProperty2Desc.setText(sol.getLiteral("desc").getString());
                    }
                  }else{
                      if(i==0){
                        this.txtProperty1Desc.setText(sol.getLiteral("desc").getString());
                    }
                    else if(i==1){
                        this.txtProperty2Desc.setText(sol.getLiteral("desc").getString());
                    }
                  }
                    
                    i++;      
               }
               LOGGER.debug(res);
            } finally {
               this.mldg.setRulesets();
            }
            
    }
    
    
    private void setSubDrugs(final String drugCode){
        String finalQuery = "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                                "prefix dron: <http://purl.obolibrary.org/obo/DRON_> \n"
                                +"prefix gdrug: <http://clinicaldb/dron> \n"
                            +"select ?codSubDrugs ?subDrugsName\n"
                            +"from named gdrug: \n"
                            +"where { \n"
                              +"graph gdrug: { \n";
      
            String whereClause = "";
            if (!drugCode.isEmpty()) {
                whereClause = whereClause.concat("?codSubDrugs rdfs:subClassOf dron:"+drugCode+" .\n" +
                                                 "?codSubDrugs rdfs:label ?subDrugsName\n" );
                                                        }

            finalQuery = finalQuery.concat(whereClause    +"} \n"
                                                          +"} \n");
            
            
            LOGGER.debug(finalQuery);
            this.mldg.setRulesets(SPARQLRuleset.SUBCLASS_OF);
            try (QueryExecution execution = QueryExecutionFactory.create(finalQuery, this.mldg.toDataset())) {
               ResultSet res = execution.execSelect();

               while (res.hasNext()) {
                  QuerySolution sol = res.next();
                  String name = sol.getLiteral("subDrugsName").getString();
                  String codDrug = sol.getResource("codSubDrugs").getURI();
                  codDrug = codDrug.substring(codDrug.length() - 8, codDrug.length());
                  this.obsLSubDrugs.add(new SearchDrugTableEntry(codDrug, name));
               }
            } finally {
               this.mldg.setRulesets();
            }
            
            this.tvSubDrugs.setItems(this.obsLSubDrugs);
    }
}
