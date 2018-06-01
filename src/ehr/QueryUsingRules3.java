/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ehr;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.util.FileManager;




/**
 * Program infers that Dasaratha grandfatherOf  LavaKusa 
 * @author Ravi Sankar
 */
public class QueryUsingRules3 
{
    public static void main(String argv[])  
    {
   String inputFileName = "D:\\gitam\\Semantic Web Workshop\\RamayanaOntology2.owl";  
        Model model = FileManager.get().loadModel(inputFileName);
  
    //Setting up rules
  String rule = "[rule1:(?a http://www.ramayana.org/hasSon ?b)  " +
                "(?b http://www.ramayana.org/hasSon ?c)" +
              "->(?a http://www.ramayana.org/grandfatherOf ?c)]";
        
  //query String
  String queryString = "PREFIX Ram:<http://www.ramayana.org/>" +
"SELECT *"  +
"WHERE {?x ?y ?z}";
 
//set up reasoner
Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rule));

InfModel inf = ModelFactory.createInfModel(reasoner, model);

org.apache.jena.query.Query query = QueryFactory.create(queryString);
QueryExecution qe = QueryExecutionFactory.create(query, inf);
ResultSet results = qe.execSelect();

for ( ; results.hasNext() ; ) {
    QuerySolution soln = results.nextSolution() ;
    System.out.println(" ");
    System.out.print(soln.getResource("x").getLocalName());
    System.out.print("    ");
    System.out.print(soln.getResource("y").getLocalName());
    System.out.print("    ");
    System.out.println(soln.getResource("z").getLocalName());
}
 
 
/*output result*/
//ResultSetFormatter.out(System.out, results, query);
qe.close(); 
    
  }

    
}
