package com.tp.action.log;

import java.util.List;

import org.apache.struts2.convention.annotation.Namespace;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.opensymphony.xwork2.ActionSupport;
import com.tp.entity.log.LogCountClientInstallWithContent;
import com.tp.orm.Page;
import com.tp.orm.PropertyFilter;
import com.tp.orm.PageRequest.Sort;
import com.tp.service.LogService;
import com.tp.utils.Struts2Utils;

@Namespace("/report")
public class ReportClientInstallWithContentAction extends ActionSupport {

	private static final long serialVersionUID = 1L;

	private LogService logService;
	private Page<LogCountClientInstallWithContent> page = new Page<LogCountClientInstallWithContent>();
	private List<Integer> sliders = Lists.newArrayList();

	public String execute() throws Exception {
		List<PropertyFilter> filters = PropertyFilter.buildFromHttpRequest(Struts2Utils.getRequest());
		if (!page.isOrderBySetted()) {
			page.setOrderBy("createTime");
			page.setOrderDir(Sort.DESC);
		}
		page = logService.searchCountClientInstallWithContent(page, filters);
		sliders = page.getSlider(10);
		return SUCCESS;
	}

	public Page<LogCountClientInstallWithContent> getPage() {
		return page;
	}

	public List<Integer> getSliders() {
		return sliders;
	}

	@Autowired
	public void setLogService(LogService logService) {
		this.logService = logService;
	}
}
