package com.tp.action;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.opensymphony.xwork2.ActionSupport;
import com.tp.dao.log.LogCountContentDao;
import com.tp.dao.log.LogJdbcDao;
import com.tp.entity.*;
import com.tp.orm.Page;
import com.tp.service.*;
import com.tp.utils.Constants;
import com.tp.utils.FileUtils;
import com.tp.utils.ServletUtils;
import com.tp.utils.Struts2Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Results({@Result(name = "reload", location = "home.action", type = "redirect")})
public class HomeAction extends ActionSupport {

    private static final long serialVersionUID = 1L;
    private Logger logger = LoggerFactory.getLogger(getClass());

    private CategoryManager categoryManager;
    private FileManager fileManager;
    private AdvertisementService advertisementService;
    private TopicService topicService;

    private LogCountContentDao countContentDao;
    private LogJdbcDao logJdbcDao;
    private Page<FileInfo> newestPage = new Page<FileInfo>(10);
    private Page<FileInfo> catePage = new Page<FileInfo>(10);

    private Long id;
    private Long pageNo = 1L;
    private String totalDown;
    private FileInfo info;
    private List<FileInfo> gameInfo;
    private List<FileInfo> appInfo;

    private Long categoryId;

    private String categoryName;
    private String language;
    private List<Category> categories;

    private String bars;

    private List<Map<String, Object>> sorts;
    private List<Topic> topics;

    private String title;
    private String topicDescription;
    private Long offset;

    @Override
    public String execute() throws Exception {

        return list();
    }

    public String list() throws Exception {

        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        Long storeId = chooseStoreId(session);

        String type = Struts2Utils.getParameter("g");
        if (StringUtils.isBlank(type)) {
            type = "female";
        }
        if (pageNo.intValue() > 1) {
            newestPage.setPageNo(pageNo.intValue());
            newestPage = fileManager.searchStoreInfoInShelf(newestPage, type, storeId, language);
            String json = fileManager.getFileInfoPageJson(newestPage.getResult(), session);
            Struts2Utils.renderJson(json);
            return null;
        } else {
            newestPage = fileManager.searchStoreInfoInShelf(newestPage, type, storeId, language);
            bars = advertisementService.getJsonByType("store");
            return SUCCESS;
        }
    }

    private void setEachDownloadURl(Page<FileInfo> page, HttpSession session) throws Exception {
        Iterator<FileInfo> it = page.getResult().iterator();
        while (it.hasNext()) {
            FileInfo info = it.next();
            if (info.getTheme().getTagNames().contains("隐藏")) {
                it.remove();
            } else {
                String category = info.getTheme().getCategories().get(0).getDescription();
                fileManager.setDownloadType(session, category, info);
            }
        }
    }

    public String topicList() throws Exception {
        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        String topicId = Struts2Utils.getParameter("topicId");
        if (StringUtils.isBlank(topicId)) {
            topicId = "";
        }
        topicId = FileUtils.getIconLevel(topicId);//兼容莫名的参数格式:1#
        Long id = Long.valueOf(topicId);
        Topic topic = topicService.getTopic(id);
        if (pageNo > 1) {
            newestPage.setPageNo(pageNo.intValue());
            newestPage = fileManager.searchTopicFile(newestPage, id, language);
            String json = fileManager.getFileInfoPageJson(getNewestPage().getResult(), session);
            Struts2Utils.renderJson(json);
            return null;
        } else {
            title = topic.getName();
            topicDescription = topic.getDescription();
            newestPage = fileManager.searchTopicFile(newestPage, id, language);
            return "topic-list";
        }
    }

    public String cate() throws Exception {
        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        Long storeId = chooseStoreId(session);

        String type = Struts2Utils.getParameter("g");

        if (StringUtils.isBlank(type)) {
            type = "female";
            title = "女生专区";
        } else {
            if (type.equals("male")) {
                title = "男生专区";
            } else if (type.equals("female")) {
                title = "女生专区";
            }
        }

        newestPage = fileManager.searchStoreInfoInShelf(newestPage, type, storeId, language);

        return "cate";
    }

    public String shelf() throws Exception {
        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        Long storeId = chooseStoreId(session);
        String sf = Struts2Utils.getParameter("sf");

        Store store = categoryManager.getStore(storeId);
        for (Shelf s : store.getShelfs()) {
            if (s.getValue().equals(sf)) {
                categoryName = s.getName();
                title = s.getName();
                break;
            }
        }

        newestPage = fileManager.searchStoreInfoInShelf(newestPage, sf, storeId, language);
        setEachDownloadURl(newestPage, session);
        return "shelf";
    }

    public String diy() throws Exception {
        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        Long storeId = chooseStoreId(session);
        newestPage = fileManager.searchStoreInfoInShelf(newestPage, "diy", storeId, language);
        bars = advertisementService.getJsonByType("diy");

        return "diy";
    }

    public String hottest() throws Exception {
        HttpSession session = Struts2Utils.getSession();

        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        Long storeId = chooseStoreId(session);
        sorts = logJdbcDao.countThemeFileDownload(language, storeId, pageNo);
        if (pageNo > 1) {
            String queryString = (String) session.getAttribute("queryString");
            String json = fileManager.getHottestJson(sorts, queryString);
            Struts2Utils.renderJson(json);
            return null;
        }
        return "hottest";
    }

    public String category() throws Exception {
        List<Category> lists = categoryManager.getCategories();
        categories = Lists.newArrayList();
        for (Category category : lists) {
            if (StringUtils.contains(category.getDescription(), "hidden")) {
                continue;
            }
            categories.add(category);
        }
        return "category";
    }

    private Long chooseStoreId(HttpSession session) throws Exception {
        String storeType = (String) session.getAttribute(Constants.PARA_STORE_TYPE);
        Long storeId = (Long) session.getAttribute(Constants.ID_LOCK);
        if (StringUtils.isBlank(storeType)) {
            storeType = Constants.ST_LOCK;
        }
        if (storeId != null) {
            return storeId;
        } else {
            try {
                storeId = categoryManager.getStoreByValue(storeType).getId();
            } catch (Exception e) {
                logger.warn("商店{}不存在，请求错误～", storeType);
                Struts2Utils.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "parametter is incorrect.");
            }
            session.setAttribute(Constants.ID_LOCK, storeId);
            return storeId;
        }
    }

    /**
     * 输出5个广告xml
     */
    public String adXml() throws Exception {

        HttpServletRequest request = Struts2Utils.getRequest();
        HttpServletResponse response = Struts2Utils.getResponse();
        request.getRequestDispatcher("/poll/advertisement!generateXml.action?st=lock").forward(request, response);
        return null;
    }

    public String details() throws Exception {
        try {

            HttpSession session = Struts2Utils.getSession();

            language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
            Long storeId = chooseStoreId(session);
            info = fileManager.getFileInfoByFileAndLanguage(id, language);
            if (info == null)
                return "reload";

            Category cate = info.getTheme().getCategories().get(0);
            List<CategoryInfo> cateInfos = cate.getInfos();
            for (CategoryInfo ci : cateInfos) {
                if (ci.getDescription().equals(language)) {
                    categoryName = ci.getName();
                    break;
                }
            }
            catePage.setPageSize(100);
            if (cate.getDescription().contains("other")) {
                if (cate.getName().contains("游戏")) {
                    catePage = fileManager.searchStoreInfoInShelf(catePage, Shelf.Type.GAME.getValue(), storeId,
                            language);
                } else if (cate.getName().contains("推荐")) {
                    catePage = fileManager.searchStoreInfoInShelf(catePage, "app", storeId, language);
                }
            } else {
                catePage = fileManager.searchInfoByCategoryAndStore(catePage, cate.getId(), storeId, language);
            }

            List<FileInfo> fileinfos = catePage.getResult();

            if (offset != null) {
                Map<Long, FileInfo> offsetInfo = getOffsetInfo(fileinfos);
                long size = offsetInfo.size();
                if (offset <= 0L) {
                    offset = size;
                } else if (offset > size) {
                    offset = 1L;
                }
                info = offsetInfo.get(offset);
                offset = info.getOffset();
            }
            totalDown = countContentDao.queryTotalDownload(info.getTheme().getTitle(), language);
            fileinfos.remove(info);
            Collections.shuffle(fileinfos);
            if (fileinfos.size() > 2) {
                fileinfos = fileinfos.subList(0, 2);
            }
            catePage.setResult(fileinfos);
            shuffleGame(info, storeId, session);
            shuffleApp(info, storeId, session);
            fileManager.setDownloadType(session, cate.getDescription(), info);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return "reload";
        }
        return "details";
    }

    private Map<Long, FileInfo> getOffsetInfo(List<FileInfo> infos) {
        long i = 0;
        Map<Long, FileInfo> offsetMap = Maps.newHashMap();
        for (FileInfo info : infos) {
            ++i;
            info.setOffset(i);
            offsetMap.put(i, info);
        }
        return offsetMap;
    }

    private void shuffleGame(FileInfo current, Long storeId, HttpSession session) throws Exception {
        gameInfo = shuffle(current, "game", storeId, session);
    }

    private void shuffleApp(FileInfo current, Long storeId, HttpSession session) throws Exception {
        appInfo = shuffle(current, "app", storeId, session);
    }

    private List<FileInfo> shuffle(FileInfo currentInfo, String type, Long storeId, HttpSession session)
            throws Exception {
        String g = Struts2Utils.getParameter("g");
        if (StringUtils.isNotBlank(g)) {
            if (g.equals("female")) {
                g = "女";
            } else {
                g = "男";
            }
        }
        newestPage.setPageSize(100);
        newestPage = fileManager.searchStoreInfoInShelf(newestPage, type, storeId, language);
        List<FileInfo> fileInfos = newestPage.getResult();
        List<FileInfo> subInfo = Lists.newArrayList();
        fileInfos.remove(currentInfo);
        for (FileInfo info : fileInfos) {
            String tags = info.getTheme().getTagNames();

            if (tags != null && g != null && tags.contains(g)) {
                subInfo.add(info);
            }
        }

        if (subInfo.size() == 0) {
            subInfo.addAll(fileInfos);
        }

        subInfo = fileManager.shuffInfos(subInfo);
        if (subInfo.size() > 1) {

            for (FileInfo info : subInfo) {
                fileManager.setDownloadType(session, info.getTheme().getCategories().get(0).getDescription(), info);
            }
        }
        return subInfo;
    }

    public String more() throws Exception {

        HttpSession session = Struts2Utils.getSession();
        HttpServletRequest request = Struts2Utils.getRequest();
        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);

        Long storeId = chooseStoreId(session);
        try {
            categoryId = Long.valueOf(Struts2Utils.getParameter("cid"));
            categoryName = categoryManager.getCategory(categoryId).getName();
        } catch (Exception e) {
            String userAgent = request.getHeader("User-Agent");
            String ip = ServletUtils.getIpAddr(request);
            logger.warn(
                    "com.tp.entity.Category:#{}不存在,ip:" + ip + ",User-Agent:" + userAgent + " param:"
                            + session.getAttribute(Constants.QUERY_STRING), categoryId);
            Struts2Utils.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND, "parametter is incorrect.");
            return null;
        }

        catePage = fileManager.searchInfoByCategoryAndStore(catePage, categoryId, storeId, language);

        return "more";
    }

    public String categoryMore() throws Exception {

        HttpSession session = Struts2Utils.getSession();
        language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
        categoryId = Long.valueOf(Struts2Utils.getParameter("cid"));
        Long storeId = chooseStoreId(session);

        catePage.setPageNo(pageNo.intValue());
        catePage = fileManager.searchInfoByCategoryAndStore(catePage, categoryId, storeId, language);
        String json = fileManager.getFileInfoPageJson(catePage.getResult(), session);
        Struts2Utils.renderJson(json);
        return null;
    }

    public String topic() throws Exception {
        topics = topicService.getAllTopics();
        return "topic";
    }

    @Autowired
    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Autowired
    public void setCategoryManager(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    public Page<FileInfo> getNewestPage() {
        return newestPage;
    }

    public List<FileInfo> getGameInfo() {
        return gameInfo;
    }

    public List<FileInfo> getAppInfo() {
        return appInfo;
    }

    public Page<FileInfo> getCatePage() {
        return catePage;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileInfo getInfo() {
        return info;
    }

    public void setInfo(FileInfo info) {
        this.info = info;
    }

    public String getTopicDescription() {
        return topicDescription;
    }

    public String getTitle() {
        return title;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public String getLanguage() {
        return language;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public String getTotalDown() {
        return totalDown;
    }

    public String getBars() {
        return bars;
    }

    public List<Map<String, Object>> getSorts() {
        return sorts;
    }

    public Long getPageNo() {
        return pageNo;
    }

    public void setPageNo(Long pageNo) {
        this.pageNo = pageNo;
    }

    public Long getNextPage() {
        return pageNo + 1;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    @Autowired
    public void setCountContentDao(LogCountContentDao countContentDao) {
        this.countContentDao = countContentDao;
    }

    @Autowired
    public void setAdvertisementService(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @Autowired
    public void setLogJdbcDao(LogJdbcDao logJdbcDao) {
        this.logJdbcDao = logJdbcDao;
    }

    @Autowired
    public void setTopicService(TopicService topicService) {
        this.topicService = topicService;
    }
}
