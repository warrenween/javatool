import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Database {
  //uses https://bitbucket.org/xerial/sqlite-jdbc
  static Logger logger = LoggerFactory.getLogger(Database.class);
  Connection connection = null;
  Statement statement = null;
  public static String dbFile = "./resources/db/" + Config.appName.toLowerCase()+"-"+Config.majorVersionDB.toString()+".db";  
  private static Database instance = null;

  public static Database getInstance() {
    if(instance == null) {
      instance = new Database();
    }
    return instance;
  }

  private Database() {
    Boolean dbExists = true;
    if (!(new File(dbFile)).exists()) {
      dbExists = false;
    }
    init();
    createTables();
  }

  public void init() {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (Exception e) {
    }
    try {
      connection = DriverManager.getConnection("jdbc:sqlite:"+dbFile);
      statement = connection.createStatement();
      statement.setQueryTimeout(30);
    } catch (SQLException e) {
    }
  }

  public void createTables() {
    try {
      // Blocks
      executeUpdate("CREATE TABLE IF NOT EXISTS blocks(block_index INTEGER PRIMARY KEY, block_hash TEXT UNIQUE, block_time INTEGER,block_nonce BIGINT)");
      executeUpdate("CREATE INDEX IF NOT EXISTS blocks_block_index_idx ON blocks (block_index)");

      // Transactions
      executeUpdate("CREATE TABLE IF NOT EXISTS transactions(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, block_time INTEGER, source TEXT, destination TEXT, btc_amount INTEGER, fee INTEGER, data BLOB, supported BOOL DEFAULT 1)");
      executeUpdate("CREATE INDEX IF NOT EXISTS transactions_block_index_idx ON transactions (block_index)");
      executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_index_idx ON transactions (tx_index)");
      executeUpdate("CREATE INDEX IF NOT EXISTS transactions_tx_hash_idx ON transactions (tx_hash)");

      // Balances
      executeUpdate("CREATE TABLE IF NOT EXISTS balances(address TEXT, asset TEXT, amount INTEGER)");
      executeUpdate("CREATE INDEX IF NOT EXISTS address_idx ON balances (address)");
      executeUpdate("CREATE INDEX IF NOT EXISTS asset_idx ON balances (asset)");

      // Sends
      executeUpdate("CREATE TABLE IF NOT EXISTS sends(tx_index INTEGER PRIMARY KEY, tx_hash TEXT UNIQUE, block_index INTEGER, source TEXT, destination TEXT, asset TEXT, amount INTEGER, validity TEXT)");
      executeUpdate("CREATE INDEX IF NOT EXISTS sends_block_index_idx ON sends (block_index)");

      // Messages
      executeUpdate("CREATE TABLE IF NOT EXISTS messages(message_index INTEGER PRIMARY KEY, block_index INTEGER, command TEXT, category TEXT, bindings TEXT)");
      executeUpdate("CREATE INDEX IF NOT EXISTS block_index_idx ON messages (block_index)");
      
      // Parameters
      executeUpdate("CREATE TABLE IF NOT EXISTS sys_parameters (para_name VARCHAR(32) PRIMARY KEY, para_value TEXT )");
      
      // ODIN
      Odin.createTables(this);

      updateMinorVersion();
    } catch (Exception e) {
      logger.error("Error during create tables: "+e.toString());
      e.printStackTrace();
    }
  }
  
  public void updateMinorVersion() {
    // Update minor version
    executeUpdate("PRAGMA user_version = "+Config.minorVersionDB.toString());
  }

  public void executeUpdate(String query) {
    try {
      (connection.createStatement()).executeUpdate(query);
      logger.info("Update/Insert query: "+query);
    } catch (Exception e) {
      logger.error(e.toString());
      logger.error("Offending query: "+query);
      //System.exit(0);            
    }
  }

  public ResultSet executeQuery(String query) {
    try {
      ResultSet rs = (connection.createStatement()).executeQuery(query);
      //logger.info("Select query: "+query);
      return rs;
    } catch (SQLException e) {
      logger.error(e.toString());
      logger.error("Offending query: "+query);
      //System.exit(0);            
    }
    return null;
  }

}
