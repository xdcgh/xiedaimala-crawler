package com.github.xdcgh;

import java.sql.SQLException;

public interface CrawlerDao {
    String getNextLink(String sql) throws SQLException;

    String getNextLinkThenDelete() throws SQLException;

    void updateDatabase(String link, String sql) throws SQLException;

    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;

    Boolean isLinkProcessed(String link) throws SQLException;
}
