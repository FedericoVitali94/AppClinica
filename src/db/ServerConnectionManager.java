package db;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.SecurityContext;
import com.marklogic.semantics.jena.MarkLogicDatasetGraph;
import com.marklogic.semantics.jena.MarkLogicDatasetGraphFactory;

/**
 * *
 * Singleton, manages the connection to the marklogic server.
 *
 * @author den
 */
public class ServerConnectionManager {

   //istanza
   private static ServerConnectionManager server = null;
   private String host;
   private String username;
   private String password;
   private int port;
   private String dbName;
   private MarkLogicDatasetGraph dg;
   private DatabaseClient dc;

   //costruttore privato
   private ServerConnectionManager() {
   }

   /**
    * *
    * Getter for the singleton instance.
    *
    * @return ServerConnectionManager the instance.
    */
   public static ServerConnectionManager getInstance() {
      if (server == null) {
         synchronized (ServerConnectionManager.class) {
            if (server == null) {
               server = new ServerConnectionManager();
            }
         }
      }
      return server;
   }

   /**
    * *
    * Creates the connection client
    *
    * @return MarkLogicDatasetGraph connection client
    */
   private void createClient() {
      DatabaseClient client;
      SecurityContext auth = new DatabaseClientFactory.DigestAuthContext(this.username, this.password);
      if (this.dbName == null) {
         this.dc = DatabaseClientFactory.newClient(this.host, this.port, auth);
      } else {
         this.dc = DatabaseClientFactory.newClient(this.host, this.port, this.dbName, auth);
      }

      this.dg = MarkLogicDatasetGraphFactory.createDatasetGraph(this.dc);
   }

   /**
    * *
    * Getter for a MarklogicDatasetGraph. This method creates the dataset if it
    * is not yet initialized.
    *
    * @return MarklogicDatasetGraph dataset graph
    */
   public MarkLogicDatasetGraph getDatasetClient() {
      if (this.dg == null) {
         this.createClient();
      }

      return this.dg;
   }
   
   /**
    * *
    * Getter for a marklogic DatabaseClient. This method creates the dataset if it
    * is not yet initialized.
    *
    * @return DatabaseClient client
    */
   public DatabaseClient getDatabaseClient() {
      if (this.dg == null) {
         this.createClient();
      }

      return this.dc;
   }

   /**
    * *
    * Closes the connection.
    */
   public void close() {
      if (this.dg != null) {
         this.dg.close();
         this.dg = null;
      }
   }

   /**
    * *
    * Setter for the connection parameters.
    *
    * @param host the host as String
    * @param port the port as int
    * @param dbName the db name as String. If null, the connection will connect
    * to the server default db
    * @param username the username as String
    * @param password the password as String
    */
   public void setParameters(final String host, final int port,
                             final String dbName, final String username,
                             final String password) {
      this.setHost(host);
      this.setPort(port);
      this.setDbName(dbName);
      this.setUsername(username);
      this.setPassword(password);
   }

   /**
    * *
    * Getter for the connection parameter - host
    *
    * @return the host as String
    */
   public String getHost() {
      return host;
   }

   /**
    * *
    * Setter for the connection parameter - host
    *
    * @param host the host as String
    */
   public void setHost(final String host) {
      this.host = host;
   }

   /**
    * *
    * Getter for the connection parameter - username
    *
    * @return the username as String
    */
   public String getUsername() {
      return username;
   }

   /**
    * *
    * Setter for the connection parameter - username
    *
    * @param username the username as String
    */
   public void setUsername(final String username) {
      this.username = username;
   }

   /**
    * *
    * Getter for the connection parameter - password
    *
    * @return the password as String
    */
   public String getPassword() {
      return password;
   }

   /**
    * *
    * Setter for the connection parameter - password
    *
    * @param password the password as String
    */
   public void setPassword(final String password) {
      this.password = password;
   }

   /**
    * *
    * Getter for the connection parameter - port
    *
    * @return the port as int
    */
   public int getPort() {
      return port;
   }

   /**
    * *
    * Setter for the connection parameter - port
    *
    * @param port the port as int
    */
   public void setPort(final int port) {
      this.port = port;
   }

   /**
    * *
    * Getter for the connection parameter - db name
    *
    * @return the db name as String
    */
   public String getDbName() {
      return dbName;
   }

   /**
    * *
    * Setter for the connection parameter - db name
    *
    * @param dbName the db name as String
    */
   public void setDbName(final String dbName) {
      this.dbName = dbName;
   }
}
