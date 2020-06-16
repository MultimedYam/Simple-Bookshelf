package io.yamil.bookshelf;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

public final class Bookshelf extends JavaPlugin {

    public static Bookshelf instance;
    public Connection connection;
    private String host, database, username, password;
    private int port;

    private PlayerListener playerListener = new PlayerListener();

    public Bookshelf()
    {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            openConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void openConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) return;

        synchronized (this) {
            if (connection != null && !connection.isClosed()) return;

            try {
                // db parameters
                String url = "jdbc:sqlite:plugins/_bookshelves.db";
                // create a connection to the database
                connection = DriverManager.getConnection(url);

                for (World world : Bukkit.getWorlds()) {
                    String worldName = world.getName();
                    Statement stmt = connection.createStatement();

                    String sql = "CREATE TABLE IF NOT EXISTS "+ worldName +"_bookshelves (" +
                            "       id varchar(255) PRIMARY KEY UNIQUE," +
                            "       posX INTEGER NOT NULL," +
                            "       posY INTEGER NOT NULL," +
                            "       posZ INTEGER NOT NULL," +
                            "       author varchar(255)," +
                            "       title varchar(255)," +
                            "       pages INTEGER" +
                            ");";
                    stmt.executeUpdate(sql);

                    sql = "CREATE TABLE IF NOT EXISTS " + worldName + "_books (" +
                        "   id varchar(255) PRIMARY KEY," +
                        "   bookId varchar(255)," +
                        "   page INTEGER," +
                        "   content varchar(1023)" +
                        ");";
                    stmt.executeUpdate(sql);
                }
                System.out.println("Connection to SQLite has been established.");

            } catch (SQLException e) {
                System.out.println(e.getMessage());
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }
}
