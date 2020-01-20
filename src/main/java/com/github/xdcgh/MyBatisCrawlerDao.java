package com.github.xdcgh;

import java.sql.SQLException;

public class MyBatisCrawlerDao implements CrawlerDao {
    @Override
    public String getNextLink(String sql) throws SQLException {
        return null;
    }

    @Override
    public String getNextLinkThenDelete() throws SQLException {
        return null;
    }

    @Override
    public void updateDatabase(String link, String sql) throws SQLException {

    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {

    }

    @Override
    public Boolean isLinkProcessed(String link) throws SQLException {
        return null;
    }
}
