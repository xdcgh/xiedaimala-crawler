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
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        // 待处理的连接池
        List<String> linkPool = new ArrayList<>();
        // 已经处理的连接池
        Set<String> processedLinks = new HashSet<>();
        linkPool.add("https://sina.cn");

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            // ArrayList从尾部删除更有效率
            String link = linkPool.remove(linkPool.size() - 1);
            if (processedLinks.contains(link)) {
                continue;
            }
            // 这是我们感兴趣的链接
            if (isInterestingLink(link)) {
                Document doc = httpGetAndParseHtml(link);
                // 把doc 页面内所有的<a> 标签，链接（href）仍进连接池
                doc.select("a").stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
                // 假如这是一个新闻的详情页面，就存入数据库，否则，就什么都不做
                storeIntoDatabaseIfItIsNewsPage(doc);
                // 把以处理的链接加入已处理集合中
                processedLinks.add(link);
            }
        }
    }

    private static void storeIntoDatabaseIfItIsNewsPage(Document doc) {
        ArrayList<Element> articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag :
                    articleTags) {
                String title = articleTag.child(0).text();
                System.out.println(title);
            }
        }
    }

    private static Document httpGetAndParseHtml(String link) throws IOException {
        // 这是我们感兴趣的，目前我们只处理新浪站内的连接
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // 特殊链接，特殊处理
        if (link.startsWith("//")) {
            link = "https:" + link;
            System.out.println(link);
        }

        // 获取链接请求
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:72.0) Gecko/20100101 Firefox/72.0");

        try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();

            String html = EntityUtils.toString(entity1);

            return Jsoup.parse(html);
        }
    }

    private static boolean isInterestingLink(String link) {
        return (isNewsPage(link) || isIndexPage(link)) && isNotLoginPage(link);
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
