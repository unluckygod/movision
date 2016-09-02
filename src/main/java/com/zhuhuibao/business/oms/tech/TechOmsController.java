package com.zhuhuibao.business.oms.tech;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.zhuhuibao.alipay.util.AlipayPropertiesLoader;
import com.zhuhuibao.common.Response;
import com.zhuhuibao.common.constant.MsgCodeConstant;
import com.zhuhuibao.common.constant.TechConstant;
import com.zhuhuibao.common.util.ShiroUtil;
import com.zhuhuibao.mybatis.tech.entity.DictionaryTechData;
import com.zhuhuibao.mybatis.tech.entity.TechCooperation;
import com.zhuhuibao.mybatis.tech.entity.TechData;
import com.zhuhuibao.mybatis.tech.service.DictionaryTechDataService;
import com.zhuhuibao.mybatis.tech.service.TechDataService;
import com.zhuhuibao.mybatis.tech.service.TechCooperationService;
import com.zhuhuibao.common.pojo.RefundItem;
import com.zhuhuibao.common.pojo.RefundReqBean;
import com.zhuhuibao.exception.AuthException;
import com.zhuhuibao.exception.BusinessException;
import com.zhuhuibao.mybatis.order.service.OrderService;
import com.zhuhuibao.mybatis.tech.service.TechDownloadDataService;
import com.zhuhuibao.service.order.ZHOrderService;
import com.zhuhuibao.utils.MsgPropertiesUtils;
import com.zhuhuibao.utils.file.FileUtil;
import com.zhuhuibao.utils.pagination.model.Paging;
import com.zhuhuibao.utils.pagination.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * 技术后台运营系统
 *
 * @version 2016/06/08
 * @Author pl
 */
@RestController
@RequestMapping("/rest/tech/oms")
@Api(value = "techOms", description = "技术后台运营系统")
public class TechOmsController {
    private static final Logger log = LoggerFactory.getLogger(TechOmsController.class);


    private static final String PARTNER = AlipayPropertiesLoader.getPropertyValue("partner");

    @Autowired
    ZHOrderService zhOrderService;

    @Autowired
    OrderService orderService;

    @Autowired
    TechCooperationService techService;

    @Autowired
    DictionaryTechDataService dicTDService;

    @Autowired
    TechDataService techDataService;

    @Autowired
    TechDownloadDataService dlService;

    @Autowired
    FileUtil fileUtil;
    /**
     * 单笔退款
     * {detail_data 退款详细数据 必填(支付宝交易号^退款金额^备注)}
     *
     * @param resp
     * @param data
     */
    @ApiOperation(value = "培训课程单笔退款", notes = "培训课程单笔退款", response = Response.class)
    @RequestMapping(value = "refund", method = RequestMethod.POST)
    public Response doRefund(HttpServletResponse resp, @ApiParam @ModelAttribute RefundItem data) throws Exception {
        Gson gson = new Gson();
        String json = gson.toJson(data);
        log.info("技术培训批量退款页面,请求参数:{}", json);
        Map paramMap = gson.fromJson(json, Map.class);
        paramMap.put("partner", PARTNER);// {partner = seller_id}   商家支付宝ID  合作伙伴身份ID 签约账号

        Long userId = ShiroUtil.getOmsCreateID();
        if (userId == null) {
            log.error("用户未登陆");
            throw new AuthException(MsgCodeConstant.un_login,
                    MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        paramMap.put("operatorId", String.valueOf(userId));

        String reason = data.getReason();
        if (reason.contains("^") || reason.contains("|") || reason.contains("$") ||
                reason.contains("#")) {
            log.error("退款理由中不能包含 '^' ,'|', '$' ,'#' 等特殊字符");
            throw new BusinessException(MsgCodeConstant.PARAMS_VALIDATE_ERROR,
                    "退款理由中不能包含 '^' ,'|', '$' ,'#' 等特殊字符");
        }

        log.debug("调用退款接口......");
        zhOrderService.requestRefund(resp, paramMap);

        return new Response();
    }

    /**
     * 批量退款接口
     * {detail_data 退款详细数据 必填(支付宝交易号^退款金额^备注)多笔请用#隔开}
     *
     * @param resp
     * @param data
     */
    @ApiOperation(value = "培训课程批量退款", notes = "培训课程批量退款", response = Response.class)
    @RequestMapping(value = "batch_refund", method = RequestMethod.POST)
    public Response doBatchRefund(HttpServletResponse resp, @ApiParam @ModelAttribute RefundReqBean data) throws Exception {
        log.debug("暂不支持批量退款");
        return new Response("暂不支持批量退款");
    }


    @RequestMapping(value = "coop/sel_tech_cooperation", method = RequestMethod.GET)
    @ApiOperation(value = "运营管理平台搜索技术合作(技术成果，技术需求)", notes = "运营管理平台技术合作(技术成果，技术需求)", response = Response.class)
    public Response findAllTechCooperationPager(@ApiParam(value = "系统分类") @RequestParam(required = false) String systemCategory,
                                                @ApiParam(value = "应用领域") @RequestParam(required = false) String applicationArea,
                                                @ApiParam(value = "标题") @RequestParam(required = false) String title,
                                                @ApiParam(value = "类型：1成果，2需求") @RequestParam(required = false) String type,
                                                @ApiParam(value = "状态") @RequestParam(required = false) String status,
                                                @ApiParam(value = "页码") @RequestParam(required = false) String pageNo,
                                                @ApiParam(value = "每页显示的数目") @RequestParam(required = false) String pageSize) {
        Response response = new Response();
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("systemCategory", systemCategory);
        condition.put("applicationArea", applicationArea);
        if (StringUtils.isEmpty(pageNo)) {
            pageNo = "1";
        }
        if (StringUtils.isEmpty(pageSize)) {
            pageSize = "10";
        }
        Paging<Map<String, String>> pager = new Paging<Map<String, String>>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        if (title != null && !"".equals(title)) {
            condition.put("title", title.replace("_", "\\_"));
        }
        condition.put("type", type);
        condition.put("status", status);
        List<Map<String, String>> techList = techService.findAllOMSTechCooperationPager(pager, condition);
        pager.result(techList);
        response.setData(pager);
        return response;
    }

    @RequestMapping(value = "coop/upd_tech_cooperation", method = RequestMethod.POST)
    @ApiOperation(value = "修改技术合作(技术成果，技术需求)", notes = "修改技术合作(技术成果，技术需求)", response = Response.class)
    public Response updateTechCooperation(@ApiParam(value = "技术合作：技术成果，技术需求") @ModelAttribute(value = "techCoop") TechCooperation techCoop) throws Exception {
        Response response = new Response();
        int result = techService.updateTechCooperation(techCoop);
        return response;
    }

    @RequestMapping(value = "coop/sel_tech_cooperation_detail", method = RequestMethod.GET)
    @ApiOperation(value = "查询技术合作(技术成果，技术需求)", notes = "查询技术合作(技术成果，技术需求)", response = Response.class)
    public Response selectTechCooperationById(@ApiParam(value = "技术合作成果、需求ID") @RequestParam String techCoopId) {
        Response response = new Response();
        TechCooperation techCoop = techService.selectTechCooperationById(techCoopId);
        response.setData(techCoop);
        return response;
    }

    @RequestMapping(value = "coop/del_tech_cooperation", method = RequestMethod.GET)
    @ApiOperation(value = "删除技术合作(技术成果，技术需求)", notes = "删除技术合作(技术成果，技术需求)", response = Response.class)
    public Response deleteTechCooperation(@ApiParam(value = "技术合作ID") @RequestParam() String techId) {
        Response response = new Response();
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("id", techId);
        condition.put("status", TechConstant.TechCooperationnStatus.DELETE.toString());
        int result = techService.deleteTechCooperation(condition);
        return response;
    }

    @RequestMapping(value = "data/del_tech_data", method = RequestMethod.GET)
    @ApiOperation(value = "删除技术资料(行业解决方案，技术文档，培训资料)", notes = "删除技术资料(行业解决方案，技术文档，培训资料)", response = Response.class)
    public Response deleteTechData(@ApiParam(value = "技术资料ID") @RequestParam() String techDataId) {
        Response response = new Response();
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("id", techDataId);
        condition.put("status", TechConstant.TechCooperationnStatus.DELETE.toString());
        int result = techDataService.deleteTechData(condition);
        return response;
    }

    @RequestMapping(value = "data/upd_tech_data", method = RequestMethod.POST)
    @ApiOperation(value = "修改技术资料(行业解决方案，技术文档，培训资料)", notes = "修改技术资料(行业解决方案，技术文档，培训资料)", response = Response.class)
    public Response updateTechData(@ApiParam(value = "技术合作：技术成果，技术需求") @ModelAttribute(value = "techData") TechData techData) {
        Response response = new Response();
        techDataService.updateOmsTechData(techData);
        return response;
    }

    @RequestMapping(value = "data/sel_frist_category", method = RequestMethod.GET)
    @ApiOperation(value = "查询技术资料一级分类", notes = "查询技术资料一级分类", response = Response.class)
    public Response selectFirstCategory() {
        Response response = new Response();
        List<DictionaryTechData> firstCategoryList = dicTDService.getFirstCategory();
        response.setData(firstCategoryList);
        return response;
    }

    @RequestMapping(value = "data/sel_second_category", method = RequestMethod.GET)
    @ApiOperation(value = "查询技术资料二级分类", notes = "查询技术资料二级分类", response = Response.class)
    public Response selectSecondCategoryByFirstId(@ApiParam(value = "一级分类ID") @RequestParam() String firstCategoryId) {
        Response response = new Response();
        List<DictionaryTechData> secondCategoryList = dicTDService.getSecondCategory(Integer.parseInt(firstCategoryId));
        response.setData(secondCategoryList);
        return response;
    }

    @RequestMapping(value = "data/sel_tech_data_detail", method = RequestMethod.GET)
    @ApiOperation(value = "查询技术资料详情(行业解决方案，技术文档，培训资料)", notes = "查询技术资料详情(行业解决方案，技术文档，培训资料)", response = Response.class)
    public Response selectTechDataDetail(@ApiParam(value = "技术资料ID") @RequestParam String techDataId) {
        TechData techData = techDataService.selectTechDataInfo(Long.parseLong(techDataId));
        Response response = new Response();
        response.setData(techData);
        return response;
    }

    @RequestMapping(value = "data/sel_tech_data", method = RequestMethod.GET)
    @ApiOperation(value = "运营管理平台搜索技术资料", notes = "运营管理平台搜索技术资料", response = Response.class)
    public Response findAllTechDataPager(@ApiParam(value = "一级分类") @RequestParam(required = false) String fCategory,
                                         @ApiParam(value = "二级分类") @RequestParam(required = false) String sCategory,
                                         @ApiParam(value = "标题") @RequestParam(required = false) String title,
                                         @ApiParam(value = "类型：1:普通资料，2：付费资料") @RequestParam(required = false) String type,
                                         @ApiParam(value = "状态") @RequestParam(required = false) String status,
                                         @ApiParam(value = "页码") @RequestParam(required = false) String pageNo,
                                         @ApiParam(value = "每页显示的数目") @RequestParam(required = false) String pageSize) {
        Response response = new Response();
        Map<String, Object> condition = new HashMap<String, Object>();
        condition.put("fCategory", fCategory);
        condition.put("sCategory", sCategory);
        if (StringUtils.isEmpty(pageNo)) {
            pageNo = "1";
        }
        if (StringUtils.isEmpty(pageSize)) {
            pageSize = "10";
        }
        Paging<Map<String, String>> pager = new Paging<Map<String, String>>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        if (title != null && !"".equals(title)) {
            condition.put("title", title.replace("_", "\\_"));
        }
        condition.put("type", type);
        condition.put("status", status);
        List<Map<String, String>> techList = techDataService.findAllOMSTechCooperationPager(pager, condition);
        pager.result(techList);
        response.setData(pager);
        return response;
    }

    @ApiOperation(value = "下载技术资料", notes = "下载技术资料", response = Response.class)
    @RequestMapping(value = "downloadFile", method = RequestMethod.GET)
    public Response downloadBill(HttpServletResponse response,
                                 @ApiParam(value = "技术资料ID") @RequestParam String techDataId) throws Exception {
        Response jsonResult = new Response();
        log.debug("OMS download tech data");
        try {
            Long createId = ShiroUtil.getOmsCreateID();
            if (createId != null) {
                String attachName = techDataService.selectTechDataAttachName(Long.parseLong(techDataId));
                response.setDateHeader("Expires", 0);
                response.setHeader("Cache-Control",
                        "no-store, no-cache, must-revalidate");
                response.addHeader("Cache-Control", "post-check=0, pre-check=0");
                response.setHeader("Content-disposition", "attachment;filename=" + attachName);
                response.setContentType("application/octet-stream");
                jsonResult = fileUtil.downloadObject(response, attachName, "doc", "tech");
                //插入我的下载资料
                dlService.insertDownloadData(techDataId, createId);

            } else {
                jsonResult.setCode(401);
                jsonResult.setMsgCode(MsgCodeConstant.un_login);
                jsonResult.setMessage( MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
                return  jsonResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download tech data error! ", e);
        }
        return jsonResult;
    }
}
