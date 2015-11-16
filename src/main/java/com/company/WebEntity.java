package com.company;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebEntity implements Runnable {
    private String entityName;
    private String entityUrl;
    private String newsListPath;
    private String articleNamePath;
    private String articleDatePath;
    private String articleTextPath;
    private long refreshTimeout;
    static Log mLog = LogFactory.getLog("MainClassLogger");

    private Crawler myCrawler;

    private ArrayList<WebPage> webPages;

    public ArrayList<WebPage> getWebPages() {
        ArrayList<WebPage> result = webPages;
        webPages.clear();
        return result;
    }

    public void setMyCrawler(Crawler myCrawler) {
        this.myCrawler = myCrawler;
        webPages = new ArrayList<WebPage>();
    }

    public String getArticleTextPath() {
        return articleTextPath;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getEntityUrl() {
        return entityUrl;
    }

    public String getNewsListPath() {
        return newsListPath;
    }

    public String getArticleNamePath() {
        return articleNamePath;
    }

    public String getArticleDatePath() {
        return articleDatePath;
    }

    public long getRefreshTimeout() {
        return refreshTimeout;
    }

    //Single-configuration file
    public static WebEntity getEntityFromConfig(String cfgPath) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        WebEntity wEntity = mapper.readValue(new File(cfgPath), WebEntity.class);
        return wEntity;
    }

    //Able to load multi-configuration files.
    public static ArrayList<WebEntity> getEntityListFromConfig(String cfgPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ArrayList<WebEntity> webEntityList = mapper.readValue(new File(cfgPath),
                mapper.getTypeFactory().constructCollectionType(ArrayList.class, WebEntity.class));

        return webEntityList;
    }

    public String saveToConfig(IEntity entity) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        File conf = new File("config/myObj.json");
        mapper.writeValue(conf, entity);

        return conf.getAbsolutePath();
    }

    @Override
    public String toString() {
        return (this.entityName
                + "\n" + this.entityUrl
                + "\n" + this.newsListPath);
    }

    public ArrayList<WebPage> getLinksFromTheMainSite(String MainLink) throws Exception {
        Document doc = Jsoup.connect(entityUrl).get();
        Elements blockTitle = doc.select(newsListPath);
        Elements OnlyLinks = blockTitle.select("a[href]");
        ArrayList<WebPage> arrayOfWebPages = new ArrayList<WebPage>();
        String newsUrl = regExp(entityUrl);

        DBConnection mySqlConnection = new DBConnection();
        ArrayList<String> ArrayListNewsLinksInDB = new ArrayList<String>();
        ArrayListNewsLinksInDB = mySqlConnection.takeFromDB(entityName);
        System.out.println(ArrayListNewsLinksInDB.size());

        for (int i = 0; i < OnlyLinks.size() - 1; i++) {
            if (((OnlyLinks.get(i)).attr("href").toString()).indexOf("http") == -1) {
                String pageUrl = newsUrl.substring(0, newsUrl.length() - 1) + OnlyLinks.get(i).attr("href").toString();
                if (ArrayListNewsLinksInDB.size() == 0) {
                    WebPage newPage = new WebPage(this, pageUrl);
                    arrayOfWebPages.add(newPage);
                } else {
                    for (int j = 0; j < ArrayListNewsLinksInDB.size(); j++) {
                        if ((ArrayListNewsLinksInDB.get(j)).equals(pageUrl)) {
                            break;
                        } else {
                            if ((j == ArrayListNewsLinksInDB.size() - 1)) {
                                WebPage newPage = new WebPage(this, pageUrl);
                                arrayOfWebPages.add(newPage);
                            }
                        }
                    }
                }
            }
        }
        return arrayOfWebPages;
    }

    public void run() {
        WebPageParser pageParser = null;
        try {
            pageParser = new WebPageParser(getLinksFromTheMainSite(entityUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
        pageParser.parse();
        //ArrayList<WebPage> arrayOfWebPage =  getLinksFromTheMainSite(entityUrl);
            /*Thread[] threads = new Thread[arrayOfWebPage.size()];
            for (int i = 0; i < arrayOfWebPage.size(); i++) {
                threads[i] = new Thread(arrayOfWebPage.get(i));
                threads[i].start();
            }*/
            /*for (int i = 0; i < arrayOfWebPage.size(); i++) {
                arrayOfWebPage.get(i).parse();
            }*/

    }

    public void main(String[] args) throws Exception {
        this.run();
    }

    public static String regExp(String entityUrl) throws Exception {
        String LinkAfterRegExp = null;
        Pattern urlPattern = Pattern.compile("^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})\\/");
        Matcher urlMatcher = urlPattern.matcher(entityUrl);
        while (urlMatcher.find()) {
            LinkAfterRegExp = (entityUrl.substring(urlMatcher.start(), urlMatcher.end()) + "");
        }
        return LinkAfterRegExp;
    }

    private void addWebPageByLink(String link) {
        WebPage newPage = new WebPage(this, link);
        this.webPages.add(newPage);
    }

    public void addWebPage(String link) {
        String linkUrl = Crawler.getUrlStd(link);
        String entityUrl = Crawler.getUrlStd(getEntityUrl());

        if (linkUrl.equals(entityUrl)) {
            addWebPageByLink(link);
            System.out.println("Added page " + link + "\n for WebEntity " + entityUrl);
        } else {
            Map<String, WebEntity> webEntityMap = myCrawler.getWebEntityMap();
            if (webEntityMap.containsKey(linkUrl)) {
                WebEntity entityForLink = webEntityMap.get(linkUrl);
                entityForLink.addWebPage(link);
                System.out.println("Transmitted page " + link + "\n to WebEntity " + entityForLink.getEntityUrl());
            }
        }
    }
}