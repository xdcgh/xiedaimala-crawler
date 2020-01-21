package com.github.xdcgh;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() {
        try {
            String resource = "db/mybatis/config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getNextLinkThenDelete() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String url = session.selectOne("com.github.xdcgh.MyMapper.selectNextAvailableLink");
            if (url != null) {
                session.delete("com.github.xdcgh.MyMapper.deleteLink", url);
            }
            return url;
        }
    }

    @Override
    public void insertNewsIntoDatabase(String url, String title, String content) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.xdcgh.MyMapper.insertNews", new News(url, title, content));
        }
    }

    @Override
    public Boolean isLinkProcessed(String link) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("com.github.xdcgh.MyMapper.countLink", link);
            return count != 0;
        }
    }

    @Override
    public void insertProcessedLink(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "LINKS_ALREADY_PROCESSED");
        param.put("link", link);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.xdcgh.MyMapper.insertLink", param);
        }
    }

    @Override
    public void insertLinkToBeProcessed(String link) {
        Map<String, Object> param = new HashMap<>();
        param.put("tableName", "LINKS_TO_BE_PROCESSED");
        param.put("link", link);

        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.xdcgh.MyMapper.insertLink", param);
        }
    }
}
