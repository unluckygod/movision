package com.zhuhuibao.mobile.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.zhuhuibao.common.Response;
import com.zhuhuibao.common.constant.AdvertisingConstant;
import com.zhuhuibao.common.pojo.AskPriceResultBean;
import com.zhuhuibao.fsearch.pojo.spec.SupplierSearchSpec;
import com.zhuhuibao.mybatis.advertising.entity.SysAdvertising;
import com.zhuhuibao.mybatis.memCenter.entity.Member;
import com.zhuhuibao.mybatis.vip.entity.VipMemberInfo;
import com.zhuhuibao.service.*;
import com.zhuhuibao.utils.pagination.model.Paging;

/**
 * 供应商相关业务控制层
 *
 * @author liyang
 * @date 2016年10月13日
 */
@RestController
@RequestMapping("/rest/m/supplier/site/")
@Api(value = "MobileSupplier", description = "触屏端供应链频道")
public class MobileSupplierController {

    private static final Logger log = LoggerFactory.getLogger(MobileSupplierController.class);

    @Autowired
    private MobileSysAdvertisingService advertisingService;

    @Autowired
    private MobileEnquiryService enquiryService;

    @Autowired
    private MobileBrandService mobileBrandService;

    @Autowired
    private MobileProductService mobileProductService;

    @Autowired
    private MobileMemberService memberService;

    @Autowired
    private MobileCategoryService categoryService;

    @Autowired
    private MobileSuccessCaseService successCaseService;

    @Autowired
    private MobileVipInfoService vipInfoService;

    /**
     * 触屏端供应链首页
     * 
     * @return response 响应
     */
    @ApiOperation(value = "触屏端供应链首页", notes = "触屏端供应链首页")
    @RequestMapping(value = "index", method = RequestMethod.GET)
    public Response index(@ApiParam(value = "当前登陆人id") @RequestParam(required = false) String createId) {
        Response response = new Response();
        // banner位广告图片
        List<SysAdvertising> bannerAdvList = advertisingService.queryAdvertising(AdvertisingConstant.AdvertisingPosition.M_Supplychain_Banner.value);
        // 热门品牌广告图片
        List<SysAdvertising> hotBrandList = advertisingService.queryAdvertising(AdvertisingConstant.AdvertisingPosition.M_Supplychain_Hotbrand.value);
        // 热门厂商广告图片
        List<SysAdvertising> manufaList = advertisingService.queryAdvertising(AdvertisingConstant.AdvertisingPosition.M_Supplychain_Manufa.value);
        // 热门代理商广告图片
        List<SysAdvertising> agentfaList = advertisingService.queryAdvertising(AdvertisingConstant.AdvertisingPosition.M_Supplychain_Agent.value);
        // 热门渠道商广告图片
        List<SysAdvertising> canalsfaList = advertisingService.queryAdvertising(AdvertisingConstant.AdvertisingPosition.M_Supplychain_Canals.value);
        // 取最新6条公开询价数据
        List askPriceList = enquiryService.queryNewestAskPrice(6, createId);
        Map<String, List> dataList = new HashMap<>();
        dataList.put("banner", bannerAdvList);
        dataList.put("hotBrand", hotBrandList);
        dataList.put("manufaList", manufaList);
        dataList.put("agentfaList", agentfaList);
        dataList.put("canalsfaList", canalsfaList);
        dataList.put("askPriceList", askPriceList);
        response.setData(dataList);
        return response;
    }

    /**
     * 触屏端供应链-品牌馆
     *
     * @return response 响应
     */
    @ApiOperation(value = "触屏端供应链-所有品牌列表展示页", notes = "触屏端供应链-所有品牌列表展示页")
    @RequestMapping(value = "sel_hot_brand_list", method = RequestMethod.GET)
    public Response sel_hot_brand_list() {
        Response response = new Response();
        try {
            response.setData(mobileBrandService.selHotBrandList());
        } catch (Exception e) {
            log.error("sel_hot_brand_list error! ", e);
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 触屏端供应链-对应类别品牌展示列表页面
     * 
     * @param pageNo
     *            页码
     * @param pageSize
     *            每页显示条数
     * @return response 响应
     */
    @ApiOperation(value = "触屏端供应链-对应类别品牌展示列表页面", notes = "触屏端供应链-对应类别品牌展示列表页面")
    @RequestMapping(value = "sel_hot_brand_list_by_type", method = RequestMethod.GET)
    public Response sel_hot_brand_list_by_type(@ApiParam(value = "一级分类id") @RequestParam(required = true) String parentId, @ApiParam(value = "一级分类id") @RequestParam(required = false) String subTypeId,
            @ApiParam(value = "页码") @RequestParam(required = false, defaultValue = "1") int pageNo, @ApiParam(value = "每页显示的数目") @RequestParam(required = false, defaultValue = "10") int pageSize) {
        Response response = new Response();
        Paging<Map> pager = new Paging<>(pageNo, pageSize);
        try {
            response.setData(mobileBrandService.selHotBrandListByType(parentId, subTypeId, pager));
        } catch (Exception e) {
            log.error("sel_hot_brand_list_by_type error! ", e);
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 触屏端供应链-品牌信息详情页面
     * 
     * @param id
     *            品牌id
     * @return response 响应
     */
    @ApiOperation(value = "触屏端供应链-品牌信息详情页面", notes = "触屏端供应链-品牌信息详情页面")
    @RequestMapping(value = "sel_hot_brand_info", method = RequestMethod.GET)
    public Response sel_brand_info(@ApiParam(value = "品牌主键id") @RequestParam(required = true) String id, @ApiParam(value = "品牌所属类别id") @RequestParam(required = true) String scateId) {
        Response response = new Response();
        try {
            // 品牌信息
            Map brandInfo = mobileBrandService.selBrandInfo(id, scateId);
            // 该品牌类别下产品信息
            List productList = mobileProductService.findProductByBrandAndSubSystem(id, scateId);
            // 该品牌下产品类别信息
            List typeList = mobileProductService.findSubSystemByBrand(id);
            Map modelMap = new HashMap();
            modelMap.put("brandInfo", brandInfo);
            modelMap.put("typeList", typeList);
            modelMap.put("productList", productList);
            response.setData(modelMap);
        } catch (Exception e) {
            log.error("sel_brand_info error! ", e);
            e.printStackTrace();
        }
        return response;
    }

    /**
     * 触屏端搜索供应商信息
     *
     * @param spec
     *            查询条件
     * @return
     */
    @ApiOperation(value = "触屏端搜索供应商", notes = "触屏端搜索供应商", response = Response.class)
    @RequestMapping(value = { "sel_supplier_list" }, method = RequestMethod.GET)
    public Response sel_supplier_list(SupplierSearchSpec spec) {
        Response response = new Response();
        try {
            response.setData(memberService.searchSuppliers(spec));
        } catch (Exception e) {
            response.setMsgCode(0);
            response.setMessage("sel_supplier_list  error!" + e);
        }
        return response;
    }

    /**
     * 触屏端--询价馆
     * 
     * @param fcateid
     *            系统分类
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "触屏端--询价馆", notes = "触屏端--询价馆", response = Response.class)
    @RequestMapping(value = { "sel_enquiry_list" }, method = RequestMethod.GET)
    public Response sel_enquiry_list(@ApiParam(value = "系统分类") @RequestParam(required = false) String fcateid, @RequestParam(required = false, defaultValue = "1") String pageNo,
            @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<AskPriceResultBean> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        try {
            response.setData(enquiryService.selEnquiryList(fcateid, pager));
        } catch (Exception e) {
            log.error("sel_enquiry_list  error! ", e);
            response.setCode(400);
            response.setMessage("sel_enquiry_list  error!" + e);
        }
        return response;
    }

    /**
     * 触屏端--查看具体询价详情
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "触屏端--查看具体询价详情", notes = "触屏端--查看具体询价详情", response = Response.class)
    @RequestMapping(value = { "sel_enquiry_info" }, method = RequestMethod.GET)
    public Response sel_enquiry_info(@ApiParam(value = "询价主键id") @RequestParam(required = false) String id) {
        Response response = new Response();
        try {
            response.setData(enquiryService.queryAskPriceByID(id));
        } catch (Exception e) {
            response.setMessage("sel_enquiry_info  error!" + e);
        }
        return response;
    }

    /**
     * 触屏端--系统分类
     *
     * @return
     */
    @ApiOperation(value = "触屏端--系统分类", notes = "触屏端--系统分类", response = Response.class)
    @RequestMapping(value = { "sel_category" }, method = RequestMethod.GET)
    public Response sel_category() {
        Response response = new Response();
        try {
            response.setData(categoryService.selCategory());
        } catch (Exception e) {
            response.setMessage("sel_enquiry_info  error!" + e);
        }
        return response;
    }

    /**
     * 供应商成功案例
     * 
     * @param id
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "公司成功案例（分页）", notes = "公司成功案例（分页）")
    @RequestMapping(value = "sel_company_success_case_list", method = RequestMethod.GET)
    public Response sel_company_success_caseList(@ApiParam(value = "公司id") @RequestParam String id, @RequestParam(required = false, defaultValue = "1") String pageNo,
            @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<Map<String, String>> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<Map<String, String>> caseList = successCaseService.findAllSuccessCaseList(pager, id);
        pager.result(caseList);
        response.setData(pager);
        return response;
    }

    /**
     * 成功案例详情
     * 
     * @param id
     *            案例id
     * @return
     */
    @ApiOperation(value = "成功案例详情", notes = "成功案例详情")
    @RequestMapping(value = "sel_company_success_case", method = RequestMethod.GET)
    public Response sel_company_success_case(@ApiParam(value = "案例id") @RequestParam String id) {
        return new Response(successCaseService.querySuccessCaseById(id));
    }

    /**
     * 供应商详情页面
     *
     * @param id
     *            供应商id
     * @return
     */
    @ApiOperation(value = "供应商详情页面", notes = "供应商详情页面")
    @RequestMapping(value = "sel_supplier_details", method = RequestMethod.GET)
    public Response sel_supplier_details(@ApiParam(value = "供应商id") @RequestParam String id) {
        // 查询公司信息
        Member member = memberService.findMemById(id);

        // 查询公司vip信息
        VipMemberInfo vip = vipInfoService.findVipMemberInfoById(Long.parseLong(id));

        // 查询公司产品类别
        List<Map<String, String>> productTypeList = mobileProductService.queryProductTypeListByCompanyId(id);

        // 页面展示
        Map map = new HashMap();
        if (vip != null) {
            map.put("vipLevel", vip.getVipLevel());
        } else {
            map.put("vipLevel", 100);
        }

        map.put("logo", member.getEnterpriseLogo());
        map.put("companyName", member.getEnterpriseName());
        map.put("webSite", member.getEnterpriseWebSite());
        String provinceName = "";
        if (!StringUtils.isEmpty(member.getProvinceName())) {
            provinceName = member.getProvinceName();
        }
        String cityName = "";
        if (!StringUtils.isEmpty(member.getCityName())) {
            cityName = member.getCityName();
        }
        String areaName = "";
        if (!StringUtils.isEmpty(member.getAreaName())) {
            areaName = member.getAreaName();
        }
        if (member.getProvince() != null) {
            map.put("address", provinceName + cityName + areaName + member.getAddress());
        } else {
            map.put("address", "");
        }
        map.put("telephone", member.getEnterpriseTelephone());
        map.put("fax", member.getEnterpriseFox());
        map.put("introduce", member.getEnterpriseDesc());
        map.put("productTypeList", productTypeList);
        return new Response(map);
    }
}
