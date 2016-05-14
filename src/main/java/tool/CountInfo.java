package tool;

/**
 * 各种类型的item的数量信息，StatTool的count方法返回的结果
 *
 * @author hujiawei
 */
public class CountInfo {
    private int itemCount = 0;//item总数
    private int emptyItemCount = 0;//内容为空的item总数
    private int articleCount = 0;//文章数目
    private int libraryCount = 0;//库和代码的数目
    private int otherCount = 0;//其他数目，包括新闻、工具、设计等等

    @Override
    public String toString() {
        return "CountInfo{" +
                "itemCount=" + itemCount +
                ", emptyItemCount=" + emptyItemCount +
                ", articleCount=" + articleCount +
                ", libraryCount=" + libraryCount +
                ", otherCount=" + otherCount +
                '}';
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getEmptyItemCount() {
        return emptyItemCount;
    }

    public void setEmptyItemCount(int emptyItemCount) {
        this.emptyItemCount = emptyItemCount;
    }

    public int getArticleCount() {
        return articleCount;
    }

    public void setArticleCount(int articleCount) {
        this.articleCount = articleCount;
    }

    public int getLibraryCount() {
        return libraryCount;
    }

    public void setLibraryCount(int libraryCount) {
        this.libraryCount = libraryCount;
    }

    public int getOtherCount() {
        return otherCount;
    }

    public void setOtherCount(int otherCount) {
        this.otherCount = otherCount;
    }
}