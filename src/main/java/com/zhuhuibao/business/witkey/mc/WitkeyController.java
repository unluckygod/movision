package com.zhuhuibao.business.witkey.mc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zhuhuibao.exception.PageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.zhuhuibao.common.Response;
import com.zhuhuibao.common.constant.MsgCodeConstant;
import com.zhuhuibao.common.util.ShiroUtil;
import com.zhuhuibao.exception.AuthException;
import com.zhuhuibao.mybatis.constants.service.ConstantService;
import com.zhuhuibao.mybatis.memCenter.entity.Member;
import com.zhuhuibao.mybatis.memCenter.service.MemberService;
import com.zhuhuibao.mybatis.witkey.entity.Cooperation;
import com.zhuhuibao.mybatis.witkey.service.CooperationService;
import com.zhuhuibao.utils.MsgPropertiesUtils;
import com.zhuhuibao.utils.pagination.model.Paging;
import com.zhuhuibao.utils.pagination.util.StringUtils;

/**
 * 威客接口 Created by cxx on 2016/5/4 0004.
 */
@RestController
@RequestMapping("/rest/witkey/mc/")
public class WitkeyController {
	private static final Logger log = LoggerFactory.getLogger(WitkeyController.class);

	@Autowired
	private CooperationService cooperationService;

	@Autowired
	private ConstantService constantService;

	@Autowired
	private MemberService memberService;

	/**
	 * 合作类型(大类，子类)
	 */
	@ApiOperation(value = "合作类型(大类，子类)", notes = "合作类型(大类，子类)", response = Response.class)
	@RequestMapping(value = "sel_witkeyTypeList", method = RequestMethod.GET)
	public Response cooperationType() {
		Response response = new Response();
		List list = cooperationService.cooperationType();
		response.setData(list);
		return response;
	}

	/**
	 * 合作类型(子类)
	 */
	@ApiOperation(value = "合作类型(子类)", notes = "合作类型(子类)", response = Response.class)
	@RequestMapping(value = "sel_subWitkeyTypeList", method = RequestMethod.GET)
	public Response subCooperationType() {
		Response response = new Response();
		List list = cooperationService.subCooperationType();
		response.setData(list);
		return response;
	}

	/**
	 * 项目类别
	 */
	@ApiOperation(value = "项目类别", notes = "项目类别", response = Response.class)
	@RequestMapping(value = "sel_witkeyCategoryList", method = RequestMethod.GET)
	public Response cooperationCategory() {
		Response response = new Response();
		// 项目类别type为9
		String type = "9";
		List<Map<String, String>> list = constantService.findByType(type);
		response.setData(list);
		return response;
	}

	/**
	 * 编辑任务
	 */
	@ApiOperation(value = "编辑任务", notes = "编辑任务", response = Response.class)
	@RequestMapping(value = "upd_witkey", method = RequestMethod.POST)
	public Response updateCooperation(Cooperation cooperation) {
		Response response = new Response();
		if (cooperation.getEndTime() != null) {
			cooperation.setEndTime(cooperation.getEndTime() + " 23:59:59");
		}
		if (cooperation.getPrice() == null) {
			cooperation.setPrice(Double.parseDouble("-1"));
		}
		cooperation.setStatus("0");
		cooperationService.updateCooperation(cooperation);
		return response;
	}

	/**
	 * 批量删除任务
	 */
	@ApiOperation(value = "批量删除任务", notes = "批量删除任务", response = Response.class)
	@RequestMapping(value = "del_witkey", method = RequestMethod.POST)
	public Response deleteCooperation(@RequestParam String ids) {
		Response response = new Response();
		cooperationService.deleteCooperation(ids);
		return response;
	}

	/**
	 * 查询一条任务的信息
	 */
	@ApiOperation(value="查询一条任务的信息",notes="查询一条任务的信息",response = Cooperation.class)
    @RequestMapping(value = "sel_witkey", method = RequestMethod.GET)
    public Response queryCooperationInfo(@RequestParam String id)  {
        Response response = new Response();
		Long createid = ShiroUtil.getCreateID();
		if(createid!=null){
			Map<String,Object> cooperation = cooperationService.queryCooperationInfoById(id);
			if(createid.equals(cooperation.get("createId"))){
				response.setData(cooperation);
			}else {
				throw new PageNotFoundException(MsgCodeConstant.SYSTEM_ERROR, "页面不存在");
			}
		}else {
			throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String
					.valueOf(MsgCodeConstant.un_login)));
		}
        return response;
    }

	/**
	 * 查询我发布的任务（分页）
	 */
	@ApiOperation(value = "查询我发布的任务（后台分页）", notes = "查询我发布的任务（后台分页）", response = Response.class)
	@RequestMapping(value = "sel_myWitkeyList", method = RequestMethod.GET)
	public Response findAllMyCooperationByPager(@RequestParam(required = false) String pageNo,
			@RequestParam(required = false) String pageSize,
			@ApiParam(value = "合作标题") @RequestParam(required = false) String title,
			@ApiParam(value = "合作类型") @RequestParam(required = false) String type,
			@ApiParam(value = "审核状态") @RequestParam(required = false) String status) {
		Long createId = ShiroUtil.getCreateID();
		if (StringUtils.isEmpty(pageNo)) {
			pageNo = "1";
		}
		if (StringUtils.isEmpty(pageSize)) {
			pageSize = "10";
		}
		Response response = new Response();
		Paging<Map<String, String>> pager = new Paging<Map<String, String>>(Integer.valueOf(pageNo),
				Integer.valueOf(pageSize));
		Cooperation cooperation = new Cooperation();
		cooperation.setCreateId(String.valueOf(createId));
		cooperation.setType(type);
		cooperation.setTitle(title);
		cooperation.setStatus(status);
		if (createId != null) {
			List<Map<String, String>> cooperationList = cooperationService
					.findAllCooperationByPager(pager, cooperation);
			pager.result(cooperationList);
			response.setData(pager);
		} else {
			throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String
					.valueOf(MsgCodeConstant.un_login)));
		}
		return response;
	}

	@ApiOperation(value = "我查看的威客任务", notes = "我查看的威客任务", response = Response.class)
	@RequestMapping(value = "sel_witkey_task", method = RequestMethod.GET)
	public Response sel_witkey_task(@RequestParam(required = false) String pageNo,
			@RequestParam(required = false) String pageSize,
			@ApiParam(value = "合作标题") @RequestParam(required = false) String title,
			@ApiParam(value = "合作类型") @RequestParam(required = false) String type) {
		Long createId = ShiroUtil.getCreateID();
		if (StringUtils.isEmpty(pageNo)) {
			pageNo = "1";
		}
		if (StringUtils.isEmpty(pageSize)) {
			pageSize = "10";
		}
		Response response = new Response();
		Paging<Map<String, String>> pager = new Paging<Map<String, String>>(Integer.valueOf(pageNo),
				Integer.valueOf(pageSize));
		Map<String, Object> map = new HashMap<>();
		map.put("title", title);
		map.put("type", type);
		if (createId != null) {
			Member member = memberService.findMemById(String.valueOf(createId));
			if ("100".equals(member.getWorkType())) {
				map.put("companyId", createId);
			} else {
				map.put("viewerId", createId);
			}
			List<Map<String, String>> cooperationList = cooperationService.findAllWitkeyTaskList(pager, map);
			pager.result(cooperationList);
			response.setData(pager);
		} else {
			throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String
					.valueOf(MsgCodeConstant.un_login)));
		}
		return response;
	}

	@ApiOperation(value = "批量删除我查看的威客任务", notes = "批量删除我查看的威客任务", response = Response.class)
	@RequestMapping(value = "del_witkey_task", method = RequestMethod.POST)
	public Response del_witkey_task(@RequestParam String ids) {
		Response response = new Response();
		String idlist[] = ids.split(",");
		for (String id : idlist) {
			cooperationService.deleteWitkeyTask(id);
		}
		return response;
	}
}
