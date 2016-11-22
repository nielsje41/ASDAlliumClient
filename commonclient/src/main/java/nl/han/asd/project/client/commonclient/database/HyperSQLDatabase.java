package nl.han.asd.project.client.commonclient.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HyperSQL Database implementation.
 *
 * @version 1.0
 */
public class HyperSQLDatabase implements IDatabase {

    private static final String DATABASE_USERNAME = "sa";
    private static final String ENCRYPTION_ALGORITHM = "SHA-256";
    private static final String DATABASE_PASSWORD = "e1Gu3vX7";
    private static final Logger LOGGER = LoggerFactory.getLogger(HyperSQLDatabase.class);
    private Connection connection;
    public static String databasePathPrepend = "";

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(String username, String password) throws SQLException {
        LOGGER.trace("database started");
        final String key = generateKey(username, password);
        connection = DriverManager.getConnection("jdbc:hsqldb:" + databasePathPrepend + username + "_db;crypt_key=" + key + ";crypt_type=AES",
                DATABASE_USERNAME, DATABASE_PASSWORD);
        initializeDatabase();
    }

    private static String generateKey(String username, String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(ENCRYPTION_ALGORITHM);
            return String
                    .format("%064x",
                            new java.math.BigInteger(1, messageDigest.digest((username + password).getBytes())))
                    .substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean resetDatabase() throws SQLException {
        final boolean databaseIsReset = query("DROP TABLE IF EXISTS Contact")
                && query("DROP TABLE IF EXISTS Message") && query("DROP TABLE IF EXISTS Script")
                && query("DROP TABLE IF EXISTS Path");
        if (databaseIsReset) {
            initializeDatabase();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeDatabase() throws SQLException {
        query("CREATE TABLE IF NOT EXISTS Contact (id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1), username VARCHAR(256), PRIMARY KEY(id))");
        query("CREATE TABLE IF NOT EXISTS Message (id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1), messageId VARCHAR(256), sender VARCHAR(256), receiver VARCHAR(256), timestamp TIMESTAMP, message NVARCHAR(1024), confirmed BOOLEAN, PRIMARY KEY(id))");
        query("CREATE TABLE IF NOT EXISTS Script  (id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1), scriptName VARCHAR(256), scriptContent NVARCHAR(4096), PRIMARY KEY(id))");
        query("CREATE TABLE IF NOT EXISTS Path    (id INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1), messageId VARCHAR(256), nodeId VARCHAR(256), sequenceNumber INTEGER, PRIMARY KEY(messageId, nodeId, sequenceNumber))");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean query(final String sqlQuery) throws SQLException {
        if (!isOpen()) {
            return false;
        }
        final Statement statement = connection.createStatement();
        final int result = statement.executeUpdate(sqlQuery);
        statement.close();
        return result != -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultSet select(final String sqlQuery) throws SQLException {
        if (!isOpen()) {
            return null;
        }
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery(sqlQuery);
        statement.close();
        return resultSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() throws SQLException {
        return connection != null && !connection.isClosed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String query) throws SQLException {
        return connection.prepareStatement(query);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        if (!isOpen()) {
            return;
        }

        LOGGER.trace("database closed");

        final Statement statement = connection.createStatement();
        statement.execute("SHUTDOWN");
        statement.close();
        connection.close();
    }

}
