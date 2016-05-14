package data;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ctriposs.sdb.DBConfig;
import com.ctriposs.sdb.SDB;
import model.WeeklyIssue;
import model.WeeklyItem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tool.CommonTool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析androidweekly.net的网页数据
 * <p>
 * 由于对某个周报进行解析的过程中很可能出现各种情况导致出错无法进行或者异常退出，所以需要边解析边缓存，以便异常退出之后能够重新进入
 * <p>
 * hujiawei 16/4/27
 */
public class AndroidWeeklyNetParser {

    static Logger logger = LoggerFactory.getLogger(AndroidWeeklyNetParser.class);

    public static final String ANDROIDWEEKLYNETDB = "src/main/resources/androidweeklynetdb";

    private SDB cachedb;//使用sessiondb做数据缓存

    public AndroidWeeklyNetParser() {
        this.cachedb = new SDB(ANDROIDWEEKLYNETDB, DBConfig.DEFAULT);
    }

    /**
     * 开始解析下载的所有网页文档
     *
     * @param issues 周报列表
     */
    public void startParse(List<WeeklyIssue> issues) throws IOException {
        boolean flag = false;
        for (int i = 0; i < issues.size(); i++) {//处理所有的issue文档
            WeeklyIssue issue = issues.get(i);
            if (cachedb.get(issue.getUrl().getBytes()) == null) {//不使用issue.getItems().size() == 0，因为可能某期周报一个item都没提取出来
                logger.info("parse issue: " + issue.getTitle());
                issue.setItems(parseIssue(new File(issue.getFile())));//如果这里解析出错退出了的话，由于没有执行下面的put，下次依然能够重新进行解析

                flag = true;//发生过修改后面就重新生成json数据文件
                cachedb.put(issue.getUrl().getBytes(), JSON.toJSONString(issue).getBytes());//put一个数据，持久保存issue进行json化之后的字符串
            } else {
                logger.info("ignore issue: " + issue.getTitle());
                issue = JSON.parseObject(cachedb.get(issue.getUrl().getBytes()), WeeklyIssue.class);//直接从json化的字符串中提取出issue
                issues.set(i, issue);
            }
        }
        if (flag) {//如果issues有修改才重新生成json和excel文件
            logger.info("write issues to json file");
            JSON.writeJSONStringTo(issues, new FileWriter(DataHelper.ANDROIDWEEKLYNET_JSON), SerializerFeature.PrettyFormat);

            //logger.info("write issues to excel file");//数据量过大会导致保存失败，所以取消保存到excel文件中
            //writeWeeklyItems2Excel(issues, DataHelper.ANDROIDWEEKLYNET_EXCEL);//写入到excel表中，便于查看
        }

        cachedb.close();
    }

    /**
     * 解析该期android weekly的内容
     * <p>
     * 1-102 每个item使用的是p标签
     * 103-至今  每个item使用的是table布局
     * <p>
     * 第103期是2014年5月25日
     *
     * @param file 网页文档
     */
    private List<WeeklyItem> parseIssue(File file) throws IOException {
        int weekId = Integer.parseInt(file.getName().substring(0, file.getName().lastIndexOf(".")));
        List<WeeklyItem> items = new ArrayList<WeeklyItem>();
        if (weekId >= 103) {
            Document document = Jsoup.parse(file, "utf-8");
            Elements elements = document.getElementsByClass("issue");//得到<div class="issue">
            if (elements.size() <= 0) return items;
            Elements tables = elements.first().getElementsByTag("table");//得到下面所有的table

            String type = "";
            boolean needParse = false;//标示下一个item是否需要解析
            for (Element table : tables) {
                Elements elementsH = table.getElementsByTag("h2");//如果有h2说明是小标题，例如 ARTICLES & TUTORIALS
                if (elementsH.size() <= 0) {
                    elementsH = table.getElementsByTag("h3");//103-104期是h3，之后是h2
                }

                if (elementsH.size() > 0) {//重新设置tag
                    type = elementsH.first().text();
                    needParse = !DataHelper.isIgnoredType(type);
                    logger.info(type);
                } else {//解析item
                    if (!needParse) {//只解析需要解析的item
                        continue;
                    }

                    Element title = table.getElementsByClass("article-headline").first();
                    String url = title.attr("href");
                    if (DataHelper.isIgnoredUrl(url)) {//只抓取需要抓取的url，有些url无法访问
                        continue;
                    }
                    WeeklyItem item = new WeeklyItem();
                    item.setUrl(url);
                    item.setTitle(title.text());
                    Element summary = table.getElementsByTag("p").first();
                    item.setSummary(summary.text());
                    Element shortUrl = table.getElementsByTag("span").first();
                    item.setShortUrl(shortUrl.text());
                    item.setType(type);
                    item.setId(CommonTool.generatId(item.getUrl()));
                    item.setSource("Android Weekly Issue #" + weekId);//设置来源，用文字表示，因为链接可能有误 --> WeeklyIssue

                    //是否应该缓存提取的content？缓存粒度问题，这个可以不用，缓存的issue中有该份数据，所以可以忽略。
                    //是否应该统计content提取出错的部分item？查log可以解决
                    //一般第一次连接不上的，以后运行也是连接不上的，而且重复连接耗时太长，此处可做一次优化，将连接耗时的url缓存起来，下次碰到不再连接
                    //是否进行该优化也是粒度问题，暂时不处理，因为最多重新访问一次周报中的所有链接
                    item.setContent(DataHelper.extractContent(item.getUrl()));//内容如果没有为空仍然添加到列表中

                    items.add(item);//除了tags属性，其他属性值都填充了
                    logger.info(item.toString());
                }
            }
        }
        //TODO --> 103之前的不处理，内容太过于陈旧，可参考性不高
        //logger.info(JSON.toJSONString(items, true));
        return items;
    }

    /**
     * 将weekly item写入到excel文件
     *
     * @param issues   周报列表
     * @param filePath excel文件路径
     */
    private void writeWeeklyItems2Excel(List<WeeklyIssue> issues, String filePath) throws IOException {
        List<WeeklyItem> items = new ArrayList<WeeklyItem>();
        for (WeeklyIssue issue : issues) {
            items.addAll(issue.getItems());
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("androidweeklynet");
        String[] heads = new String[]{"id", "source", "type", "title", "shorturl", "tags", "url", "summary", "content"};
        Row headRow = sheet.createRow(0);

        for (int i = 0; i < heads.length; i++) {
            Cell cell = headRow.createCell(i, Cell.CELL_TYPE_STRING);
            cell.setCellValue(heads[i]);
        }

        for (int rowNum = 1; rowNum <= items.size(); rowNum++) {
            WeeklyItem item = items.get(rowNum - 1);
            Row row = sheet.createRow(rowNum);
            for (int i = 0; i < heads.length; i++) {
                Cell cell = row.createCell(i, Cell.CELL_TYPE_STRING);
                switch (i) {
                    case 0:
                        cell.setCellValue(item.getId());
                        break;
                    case 1:
                        cell.setCellValue(item.getSource());
                        break;
                    case 2:
                        cell.setCellValue(item.getType());
                        break;
                    case 3:
                        cell.setCellValue(item.getTitle());
                        break;
                    case 4:
                        cell.setCellValue(item.getShortUrl());
                        break;
                    case 5:
                        cell.setCellValue(item.getTags().toString());
                        break;
                    case 6:
                        cell.setCellValue(item.getUrl());
                        break;
                    case 7:
                        cell.setCellValue(item.getSummary());
                        break;
                    case 8:
                        cell.setCellValue(item.getContent());
                        break;
                }
            }
        }

        for (int i = 0; i < heads.length; i++) {
            sheet.autoSizeColumn(i);
        }

        FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
        outputStream.flush();
        outputStream.close();
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        try {
            //File file = new File("src/main/resources/androidweeklynetarchive/103.html");
            //new AndroidWeeklyNetParser().parseIssue(file);

            //AndroidWeeklyNetCrawler.FOLDER_CACHE = "src/main/resources/androidweeklytest/";
            AndroidWeeklyNetCrawler crawler = new AndroidWeeklyNetCrawler();
            List<WeeklyIssue> issueList = crawler.restoreIssues();

            //new AndroidWeeklyNetParser().saveIssues(issueList);
            new AndroidWeeklyNetParser().startParse(issueList);
            //new AndroidWeeklyNetParser().startParse(issueList.subList(0, 10));//使用前10个测试
        } catch (IOException e) {
            e.printStackTrace();
        }

        long endTime = System.currentTimeMillis();
        System.out.println(CommonTool.formatTime(endTime - startTime));
    }

}
