package com.tp.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ActionSupport;
import com.tp.dao.log.LogCountContentDao;
import com.tp.entity.Category;
import com.tp.entity.CategoryInfo;
import com.tp.entity.DownloadType;
import com.tp.entity.FileMarketValue;
import com.tp.entity.FileStoreInfo;
import com.tp.entity.Market;
import com.tp.entity.Shelf;
import com.tp.entity.ThemeFile;
import com.tp.orm.Page;
import com.tp.service.CategoryInfoManager;
import com.tp.service.CategoryManager;
import com.tp.service.FileManager;
import com.tp.service.MarketManager;
import com.tp.utils.Constants;
import com.tp.utils.ServletUtils;
import com.tp.utils.Struts2Utils;

@Results({ @Result(name = "reload", location = "home.action", type = "redirect") })
public class HomeAction extends ActionSupport {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(getClass());

	private CategoryManager categoryManager;
	private CategoryInfoManager categoryInfoManager;
	private FileManager fileManager;
	private MarketManager marketManager;

	private LogCountContentDao countContentDao;

	private Page<FileStoreInfo> hottestPage = new Page<FileStoreInfo>();
	private Page<FileStoreInfo> recommendPage = new Page<FileStoreInfo>();

	private Page<FileStoreInfo> newestPage = new Page<FileStoreInfo>();
	private Page<FileStoreInfo> catePage = new Page<FileStoreInfo>();

	private Long id;
	private String totalDown;
	private FileStoreInfo info;

	private List<CategoryInfo> cateInfos;
	private Long categoryId;
	private ThemeFile adFile;

	private String categoryName;
	private String language;
	private List<Category> categories;

	@Override
	public String execute() throws Exception {

		return list();
	}

	/**
	 * 商店首页显示列表
	 * @return
	 * @throws Exception
	 */
	public String list() throws Exception {

		HttpSession session = Struts2Utils.getSession();

		language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
		Long storeId = chooseStoreId(session);

		hottestPage.setPageSize(16);
		hottestPage = fileManager.searchStoreInfoInShelf(hottestPage, Shelf.Type.HOTTEST, storeId, language);

		newestPage = fileManager.searchStoreInfoInShelf(newestPage, Shelf.Type.NEWEST, storeId, language);
		if (visitByBrowse(session)) {
			recommendPage = fileManager.searchStoreInfoInShelf(recommendPage, Shelf.Type.RECOMMEND, storeId, language);
			List<FileStoreInfo> recommendFiles = recommendPage.getResult();
			if (recommendFiles.size() > 0) {
				Collections.shuffle(recommendFiles);
				adFile = recommendFiles.get(0).getTheme();
			}
		}
		return SUCCESS;
	}

	@Deprecated
	public String newest() throws Exception {
		HttpSession session = Struts2Utils.getSession();

		language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
		Long storeId = chooseStoreId(session);
		newestPage = fileManager.searchByStore(newestPage, storeId, language);
		return "newest";
	}
	
	public String diy()throws Exception{
		HttpSession session = Struts2Utils.getSession();

		language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
		Long storeId = chooseStoreId(session);
		newestPage = fileManager.searchDiyTemplate(newestPage, storeId, language);
		return "diy";
	}

	public String category() throws Exception {
		categories = categoryManager.getCategories();
		return "category";
	}

	private boolean visitByBrowse(HttpSession session) {

		return (String) session.getAttribute(Constants.PARA_RESOLUTION) == null;
	}

	private Long chooseStoreId(HttpSession session) {
		String storeType = (String) session.getAttribute(Constants.PARA_STORE_TYPE);
		Long storeId = (Long) session.getAttribute(Constants.ID_LOCK);
		if (storeType != null && storeId != null && storeType.equals(Constants.ST_LOCK)) {
			return storeId;
		} else {
			storeId = categoryManager.getStoreByValue(Constants.ST_LOCK).getId();
			session.setAttribute(Constants.ID_LOCK, storeId);
			return storeId;
		}

	}

	/**
	 * 输出5个广告xml
	 * @return
	 * @throws Exception
	 */
	public String adXml() throws Exception {
		Long storeId = categoryManager.getStoreByValue(Constants.ST_LOCK).getId();
		Page<ThemeFile> adPage = new Page<ThemeFile>();
		adPage = fileManager.searchFileByShelf(adPage, Shelf.Type.RECOMMEND, storeId);
		String domain = Constants.getDomain();
		String detailsURL = "/home!details.action?id=";
		String xml = fileManager.adXml(adPage.getResult(), domain, detailsURL);
		Struts2Utils.renderXml(xml);
		return null;
	}

	public String details() throws Exception {
		try {

			HttpSession session = Struts2Utils.getSession();

			language = (String) session.getAttribute(Constants.PARA_LANGUAGE);
			Long storeId = chooseStoreId(session);
			info = fileManager.getStoreInfoBy(storeId, id, language);
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
			setDownloadType(session, cate.getDescription());
			long count = countContentDao.queryTotalDownload(info.getTheme().getTitle());
			totalDown = convert(count);
			catePage = fileManager.searchInfoByCategoryAndStore(catePage, cate.getId(), storeId, language);
			List<FileStoreInfo> fileinfos = catePage.getResult();
			fileinfos.remove(info);
			Collections.shuffle(fileinfos);
			if (fileinfos.size() > 3) {
				fileinfos = fileinfos.subList(0, 3);
			}
			catePage.setResult(fileinfos);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return "reload";
		}
		return "details";
	}

	private String convert(long count) {
		count = count * 10;

		if (count < 10000) {
			if (count < 1000) {
				return "1000以下";
			} else {
				String number = StringUtils.substring(String.valueOf(count), 0, 1);
				for (int i = 1; i < 10; i++) {
					if (Integer.valueOf(number).equals(i)) {
						return i + "000+";
					}
				}
			}
		} else {
			String number = StringUtils.substring(String.valueOf(count), 0, 1);
			String tenThousandPosition = StringUtils.substring(String.valueOf(count), 0,
					String.valueOf(count).length() - 4);

			for (int i = 1; i < 10; i++) {
				if (Integer.valueOf(number).equals(i)) {

					return tenThousandPosition + "万+";
				}
			}

		}
		return "";
	}

	private void setDownloadType(HttpSession session, String category) throws UnsupportedEncodingException {

		String fromMarket = (String) session.getAttribute(Constants.PARA_FROM_MARKET);
		String downType = (String) session.getAttribute(Constants.PARA_DOWNLOAD_METHOD);
		StringBuilder httpBuffer = new StringBuilder();
		if (category.equalsIgnoreCase("other")) {
			httpBuffer.append("browerhttp://"+StringUtils.remove(Constants.getDomain(),"http://")+"/");
		}
		httpBuffer.append("file-download.action?id=");
		httpBuffer.append(info.getTheme().getId());
		httpBuffer.append("&inputPath=");
		if(StringUtils.isNotBlank(info.getTheme().getUxPath())){
			httpBuffer.append(URLEncoder.encode(info.getTheme().getUxPath(), "utf-8"));
		}else{
			httpBuffer.append(URLEncoder.encode(info.getTheme().getApkPath(), "utf-8"));
		}
	
		httpBuffer.append("&title=" + URLEncoder.encode(info.getTitle(), "utf-8"));
		httpBuffer.append(URLEncoder.encode("|", "utf-8")).append(
				URLEncoder.encode(info.getTheme().getTitle(), "utf-8"));
		httpBuffer.append("&");
		if (downType.equals(DownloadType.MARKET.getValue())) {
			marketDownload(fromMarket, httpBuffer.toString());
		} else {
			info.getTheme().setDownloadURL(httpBuffer.toString());
		}
	}

	private void marketDownload(String fromMarket, String http) {
		Market market = marketManager.findByPkName(fromMarket);
		if (market == null || market.getMarketKey().isEmpty()) {
			info.getTheme().setDownloadURL(http);
		} else {
			fileInMarket(market, http);
		}
	}

	private void fileInMarket(Market market, String http) {
		List<ThemeFile> files = market.getThemes();
		if (files.contains(info.getTheme())) {
			String uri = market.getMarketKey() + info.getTheme().getMarketURL();
			if (market.getPkName().equals(Constants.LENVOL_STORE)) {
				uri += ("&versioncode=" + info.getTheme().getVersion());
			}
			if (market.getPkName().equals(Constants.OPPO_NEARME)) {
				List<FileMarketValue> fvs = info.getTheme().getMarketValues();
				for (FileMarketValue fm : fvs) {
					if (market.getId().equals(fm.getMarket().getId())) {
						uri += "&" + (fm.getKeyName() + "=" + fm.getKeyValue());
					}
				}
			}
			info.getTheme().setDownloadURL(uri);
		} else {
			info.getTheme().setDownloadURL(http);
		}
	}

	public String more() throws Exception {

		HttpSession session = Struts2Utils.getSession();
		HttpServletRequest request = Struts2Utils.getRequest();
		language = (String) session.getAttribute(Constants.PARA_LANGUAGE);

		Long storeId = chooseStoreId(session);
		categoryId = Long.valueOf(Struts2Utils.getParameter("cid"));

		//		cateInfos = categoryInfoManager.getInfosBylanguage(language);
		try {
			categoryName = categoryManager.getCategory(categoryId).getName();
		} catch (Exception e) {
			String userAgent = request.getHeader("User-Agent");
			String ip = ServletUtils.getIpAddr(request);
			logger.warn("com.tp.entity.Category:#{}不存在,ip:" + ip + ",User-Agent:" + userAgent+" param:"+ session.getAttribute(Constants.QUERY_STRING), categoryId);
			Struts2Utils.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST, "parametter is incorrect.");
			return null;
		}
		catePage = fileManager.searchInfoByCategoryAndStore(catePage, categoryId, storeId, language);

		return "more";
	}

	@Autowired
	public void setFileManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}

	@Autowired
	public void setCategoryManager(CategoryManager categoryManager) {
		this.categoryManager = categoryManager;
	}

	@Autowired
	public void setMarketManager(MarketManager marketManager) {
		this.marketManager = marketManager;
	}

	@Autowired
	public void setCategoryInfoManager(CategoryInfoManager categoryInfoManager) {
		this.categoryInfoManager = categoryInfoManager;
	}

	public Page<FileStoreInfo> getHottestPage() {
		return hottestPage;
	}

	public Page<FileStoreInfo> getNewestPage() {
		return newestPage;
	}

	public Page<FileStoreInfo> getRecommendPage() {
		return recommendPage;
	}

	public Page<FileStoreInfo> getCatePage() {
		return catePage;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public FileStoreInfo getInfo() {
		return info;
	}

	public void setInfo(FileStoreInfo info) {
		this.info = info;
	}

	public List<CategoryInfo> getCateInfos() {
		return cateInfos;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public ThemeFile getAdFile() {
		return adFile;
	}

	public String getCategoryName() {
		return categoryName;
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

	@Autowired
	public void setCountContentDao(LogCountContentDao countContentDao) {
		this.countContentDao = countContentDao;
	}
}
