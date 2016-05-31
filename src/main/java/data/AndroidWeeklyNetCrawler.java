package data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import model.WeeklyIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tool.CommonTool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 爬取androidweekly.net周报网页的工具
 * <p>
 * hujiawei 16/4/27
 */
public class AndroidWeeklyNetCrawler extends WebCrawler {

    static Logger logger = LoggerFactory.getLogger(AndroidWeeklyNetCrawler.class);

    //页面缓存路径
    public static final String FOLDER_CACHE = "src/main/resources/androidweeklynetarchive";

    //爬虫的起始页面模式
    public static final String BASE_URL = "http://androidweekly.net/archive";

    //需要访问的页面的前缀
    public static final String PREFIX_URL = "http://androidweekly.net/issues/issue-";

    //爬虫的数目
    public static final int NUMBER_OF_CRAWLER = 1;

    private List<WeeklyIssue> issues;//抓取的周报
    private HashSet<String> issueUrls;

    public AndroidWeeklyNetCrawler() {
        this.issues = restoreIssues();//爬虫是多线程的 --> 支持多线程爬取
        this.issueUrls = new HashSet<String>(issues.size());
        issues.stream().forEach(issue -> {
            issueUrls.add(issue.getUrl());
        });
    }

    /**
     * 从保存的文件中恢复出哪些issue已经抓取并下载了
     */
    public List<WeeklyIssue> restoreIssues() {
        File file = new File(DataHelper.ANDROIDWEEKLYNET_JSON);
        if (file.exists()) {//文件存在就解析
            try {
                return JSON.parseArray(CommonTool.getFileContent(file), WeeklyIssue.class);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<WeeklyIssue>();
    }

    /**
     * 启动爬虫开始下载并保存网页文档
     */
    public void startCrawl() throws Exception {
        //控制爬虫的缓存目录
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(FOLDER_CACHE);//如果目录不存在会创建

        config.setPolitenessDelay(1000);//控制请求之间的延时
        config.setIncludeBinaryContentInCrawling(false);//控制是否爬取二进制文件，例如图片、pdf等
        config.setResumableCrawling(false);//控制爬虫是否能够从中断中恢复 -> 设置为true的话重新运行将恢复到原来的进度
        //这里改为自己控制爬虫，设置为false的话每次会删除frontier文件夹，然后重新抓取

        //创建爬虫控制器
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        //添加爬虫的入口地址
        controller.addSeed(BASE_URL);

        //指定数目并启动爬虫
        controller.start(AndroidWeeklyNetCrawler.class, NUMBER_OF_CRAWLER);
    }

    /**
     * 控制某些url页面是否需要访问
     * <p>
     * 将判断逻辑放在这里提高速度，之后运行耗时一般1min之内（41 seconds）
     *
     * @param page page
     * @param url  url
     * @return 返回true表示需要访问，false表示不需要访问
     */
    @Override
    public boolean shouldVisit(Page page, WebURL url) {
        return url.getURL().startsWith(PREFIX_URL) && !issueUrls.contains(url.getURL());
    }

    /**
     * 控制抓取到的页面的处理方式
     *
     * @param page page
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        if (url.equalsIgnoreCase(BASE_URL)) return;
        logger.info("visit url: " + url);

        if (page.getParseData() instanceof HtmlParseData) {
            byte[] content = page.getContentData();
            String file = FOLDER_CACHE + File.separator + parseId(url) + ".html";
            DataHelper.savePage(file, content);

            WeeklyIssue issue = new WeeklyIssue();
            issue.setUrl(url);
            issue.setFile(file);
            issue.setId(CommonTool.generatId(url));
            issue.setNum(Integer.parseInt(parseId(url)));
            issue.setTitle("Android Weekly Issue #" + issue.getNum());//由于名称存在特例，所以这里自定义title

            issues.add(issue);

            logger.info(issue.toString());
        }
    }

    @Override
    public void onBeforeExit() {
        super.onBeforeExit();

        //爬虫结束之前保存已经爬取的数据
        try {
            JSON.writeJSONStringTo(issues, new FileWriter(DataHelper.ANDROIDWEEKLYNET_JSON), SerializerFeature.PrettyFormat);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //从url中获取id，考虑到特例的情况取issus后面的第一个数字
    //特例1：http://androidweekly.net/issues/issue-138-001
    //特例2：http://androidweekly.net/issues/issue-43-birthday-issue
    //特例3：http://androidweekly.net/issues/issue-108-1
    private String parseId(String url) {
        if (null == url || url.equalsIgnoreCase("")) return "";
        Pattern pattern = Pattern.compile("[0-9]\\d*");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group();
        }
        return ""; //
    }

    public List<WeeklyIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<WeeklyIssue> issues) {
        this.issues = issues;
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            AndroidWeeklyNetCrawler crawler = new AndroidWeeklyNetCrawler();
            crawler.startCrawl();

            //单线程和多线程耗时差不多，改为单线程形式
            //1 threads: 4 minutes 53 seconds
            //4 threads: 4 minutes 3 seconds
        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println(CommonTool.formatTime(endTime - startTime));
    }
}
