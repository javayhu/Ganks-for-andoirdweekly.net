package tool;

import com.alibaba.fastjson.JSONArray;
import data.DataHelper;
import model.WeeklyIssue;
import model.WeeklyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Statistics统计工具
 * <p>
 * hujiawei 16/4/28
 */
public class StatTool {

    static Logger logger = LoggerFactory.getLogger(StatTool.class);

    public static void main(String[] args) {
        try {
            StatTool.count();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 统计周报中各种类型item的数量
     */
    public static CountInfo count() {
        List<WeeklyIssue> issueList = getIssues();

        HashMap<String, Integer> map = new HashMap<String, Integer>();
        CountInfo countInfo = new CountInfo();
        if (issueList != null) {
            for (WeeklyIssue issue : issueList) {
                //logger.info(issue.getTitle() + " " + issue.getItems().size());
                countInfo.setItemCount(countInfo.getItemCount() + issueList.size());
                for (WeeklyItem item : issue.getItems()) {
                    if (map.containsKey(item.getType())) {
                        map.put(item.getType(), map.get(item.getType()) + 1);
                    } else {
                        map.put(item.getType(), 1);
                    }
                    if (null == item.getContent() || "".equalsIgnoreCase(item.getContent().trim())) {
                        countInfo.setEmptyItemCount(countInfo.getEmptyItemCount() + 1);
                    }
                    if (item.getType().toLowerCase().contains("article")) {//
                        countInfo.setArticleCount(countInfo.getArticleCount() + 1);
                    } else if (item.getType().toLowerCase().contains("lib")) {//
                        countInfo.setLibraryCount(countInfo.getLibraryCount() + 1);
                    } else {
                        countInfo.setOtherCount(countInfo.getOtherCount() + 1);
                    }
                }
            }
        }

        logger.info(map.toString());//{Design=57, Libraries & Code=362, Articles & Tutorials=625, News=23, Tools=58, Books=2}
        logger.info(countInfo.toString());//CountInfo{itemCount=40804, emptyItemCount=101, articleCount=625, libraryCount=362, otherCount=140}

        return countInfo;
    }

    /**
     * 得到周报列表
     */
    public static List<WeeklyIssue> getIssues() {
        List<WeeklyIssue> issueList = null;
        try {
            issueList = JSONArray.parseArray(CommonTool.getFileContent(new File(DataHelper.ANDROIDWEEKLYNET_JSON)), WeeklyIssue.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return issueList;
    }

    /**
     * 得到所有的item数据
     */
    public static List<WeeklyItem> getItems() {
        List<WeeklyItem> itemList = new ArrayList<WeeklyItem>();
        List<WeeklyIssue> issueList = getIssues();
        if (issueList != null) {
            for (WeeklyIssue issue : issueList) {
                itemList.addAll(issue.getItems());//.stream().collect(Collectors.toList())
            }
        }
        return itemList;
    }

}
