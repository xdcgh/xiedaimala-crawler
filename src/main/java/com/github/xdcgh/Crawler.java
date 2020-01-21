package com.github.xdcgh;

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

public class Crawler extends Thread{
    private final CrawlerDao dao;

    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    @Override
    public void run() {
        try {
            String link;

            // 从数据库中加载下一个链接，如果能加载到，则进行循环
            while ((link = dao.getNextLinkThenDelete()) != null) {
                // 询问数据库，当前链接是否被处理过了
                if (dao.isLinkProcessed(link)) {
                    continue;
                }

                if (isInterestingLink(link)) {
                    System.out.println(link);

                    Document doc = httpGetAndParseHtml(link);

                    parseUrlsFromPageAndStoreIntoDatabase(doc);

                    storeIntoDatabaseIfItIsNewsPage(doc, link);

                    dao.insertProcessedLink(link);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 把doc 页面内所有的<a> 标签，链接（href）仍进连接池
    private void parseUrlsFromPageAndStoreIntoDatabase(Document doc) {
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
                dao.insertLinkToBeProcessed(href);
            }

        }
    }

    // 假如这是一个新闻的详情页面，就存入数据库，否则，就什么都不做
    private void storeIntoDatabaseIfItIsNewsPage(Document doc, String link) throws SQLException {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag :
                    articleTags) {
                String title = articleTag.child(0).text();
                String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));

                System.out.println(title);      // 测试用

                dao.insertNewsIntoDatabase(link, title, content);
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
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link) && !link.contains("\\"); // 注意，要处理掉含\的情况
//        return isIndexPage(link) || (isNotLoginPage(link) && !link.contains("\\") && !link.contains("javascript"));
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
