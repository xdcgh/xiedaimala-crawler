package com.github.xdcgh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.sql.*;

public class DatabaseAccessObject {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "1234";

    private final Connection connection;

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public DatabaseAccessObject() {
        try {
            this.connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/14344/IdeaProjects/xiedaimala-crawler/news", USER_NAME, PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getNextLink(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }

        return null;
    }

    public String getNextLinkThenDelete() throws SQLException {
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED");

        if (link != null) {
            updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED WHERE LINK = ?");
        }

        return link;
    }

    public void updateDatabase(String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO NEWS (TITLE, CONTENT, URL, CREATED_AT, MODIFIED_AT) VALUES (?, ?, ?, NOW(), NOW())")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, link);

            statement.executeUpdate();
        }
    }

    public Boolean isLinkProcessed(String link) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED WHERE LINK = ?")) {
            statement.setString(1, link);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return false;
    }
}
