package com.github.xdcgh;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Main {
    private static final String USER_NAME = "root";
    private static final String PASSWORD = "1234";

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/14344/IdeaProjects/xiedaimala-crawler/news", USER_NAME, PASSWORD);

        String link;

        // 从数据库中加载下一个链接，如果能加载到，则进行循环
        while ((link = getNextLinkThenDelete(connection)) != null) {
            // 询问数据库，当前链接是否被处理过了
            if (isLinkProcessed(connection, link)) {
                continue;
            }

            if (isInterestingLink(link)) {
                System.out.println(link);

                Document doc = httpGetAndParseHtml(link);

                parseUrlsFromPageAndStoreIntoDatabase(connection, doc);

                storeIntoDatabaseIfItIsNewsPage(connection, doc, link);

                updateDatabase(connection, link, "INSERT INTO LINKS_ALREADY_PROCESSED (LINK) VALUES (?)");
            }
        }
    }

    private static String getNextLinkThenDelete(Connection connection) throws SQLException {
        String link = getNextLink(connection, "select link from LINKS_TO_BE_PROCESSED");

        if (link != null) {
            updateDatabase(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED WHERE LINK = ?");
        }

        return link;
    }

    // 把doc 页面内所有的<a> 标签，链接（href）仍进连接池
    private static void parseUrlsFromPageAndStoreIntoDatabase(Connection connection, Document doc) throws SQLException {
        for (Element aTag : doc.select("a")) {
            String href = aTag.attr("href");

            if (href.startsWith("//")) {
                href = "https:" + href;
            }

            //过滤有问题的链接
            if (!href.toLowerCase().startsWith("javascript")
                    || !href.contains("\\/")
                    || !href.contains("#")
                    || !href.contains(" ")) {
                updateDatabase(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (LINK) VALUES (?)");
            }

        }
    }

    private static Boolean isLinkProcessed(Connection connection, String link) throws SQLException {
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

    private static void updateDatabase(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static String getNextLink(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }

        return null;
    }

    // 假如这是一个新闻的详情页面，就存入数据库，否则，就什么都不做
    private static void storeIntoDatabaseIfItIsNewsPage(Connection connection, Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag :
                    articleTags) {
                String title = articleTag.child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));

                System.out.println(title);      // 测试用

                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO NEWS (TITLE, CONTENT, URL, CREATED_AT, MODIFIED_AT) VALUES (?, ?, ?, NOW(), NOW())")) {
                    statement.setString(1, title);
                    statement.setString(2, content);
                    statement.setString(3, link);

                    statement.executeUpdate();
                }
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 这是我们感兴趣的，目前我们只处理新浪站内的连接
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // 获取链接请求
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:72.0) Gecko/20100101 Firefox/72.0");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            HttpEntity entity1 = response1.getEntity();

            String html = EntityUtils.toString(entity1);

            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link) && !link.contains("\\");
    }

    private static boolean isIndexPage(String link) {
        return "https://sina.cn".equals(link);
    }

    private static boolean isNewsPage(String link) {
        return link.contains("news.sina.cn");
    }

    private static boolean isNotLoginPage(String link) {
        return !link.contains("passport.sina.cn");
    }
}
