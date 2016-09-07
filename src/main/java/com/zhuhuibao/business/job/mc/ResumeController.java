package com.zhuhuibao.business.job.mc;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.zhuhuibao.aop.LoginAccess;
import com.zhuhuibao.common.Response;
import com.zhuhuibao.common.constant.*;
import com.zhuhuibao.common.util.ShiroUtil;
import com.zhuhuibao.exception.AuthException;
import com.zhuhuibao.exception.BusinessException;
import com.zhuhuibao.exception.PageNotFoundException;
import com.zhuhuibao.mybatis.memCenter.entity.ForbidKeyWords;
import com.zhuhuibao.mybatis.memCenter.entity.Job;
import com.zhuhuibao.mybatis.memCenter.entity.Resume;
import com.zhuhuibao.mybatis.memCenter.service.*;
import com.zhuhuibao.utils.MsgPropertiesUtils;
import com.zhuhuibao.utils.file.FileUtil;
import com.zhuhuibao.utils.oss.ZhbOssClient;
import com.zhuhuibao.utils.pagination.model.Paging;
import com.zhuhuibao.utils.pagination.util.StringUtils;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cxx on 2016/4/19 0019.
 */
@RestController
@RequestMapping("/rest/job/mc/resume")
@Api(value = "Resume", description = "会员中心-简历管理")
public class ResumeController {
    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JobPositionService jobService;

    @Autowired
    ApiConstants ApiConstants;

    @Autowired
    JobRelResumeService jrrService;

    @Autowired
    ZhbOssClient zhbOssClient;

    @Autowired
    FileUtil fileUtil;

    @Autowired
    ForbidKeyWordsService forbidKeyWordsService;

    @Autowired
    MemberService memberService;
    
    
   /* @LoginAccess
    @ApiOperation(value = "上传附件(简历编辑页面)", notes = "上传附件(简历编辑页面)", response = Response.class)
    @RequestMapping(value = "upload_resume_attach", method = RequestMethod.POST)
    public Response uploadResumeAttach(@RequestParam(value = "file", required = false) MultipartFile file) {
        Map<String, Object> map = new HashMap<>();
        try {
            String url = zhbOssClient.uploadObject(file, "doc", "resume");
            map.put(Constants.name, url);
            if (url.lastIndexOf(".") != -1) {
                map.put(ResumeConstants.FILE_FORMAT, url.substring(url.lastIndexOf(".")));
            } else {
                map.put(ResumeConstants.FILE_FORMAT, "");
            }
            map.put(ResumeConstants.FILE_SIZE, file.getSize());

        } catch (Exception e) {
            log.error("上传附件失败>>>", e);
            throw e;
        }

        return new Response(map);
    }*/
    

    /**
     * 发布简历
     */
    @ApiOperation(value = "发布简历", notes = "发布简历", response = Response.class)
    @RequestMapping(value = "add_resume", method = RequestMethod.POST)
    public Response setUpResume(@ModelAttribute Resume resume) throws IOException {
        Response response;
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {

            //判断用户是否存在简历
            List<String> ids = resumeService.selectIdsByCreateId(createid);
            if (ids != null && ids.size() >= 1) {
                throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "该用户已存在简历");
            }
            
            //期望工作地点不得超过5个
            int procount = 0,citycount = 0;
            String jobProvinces = resume.getJobProvince();
            if(!StringUtils.isEmpty(jobProvinces)){
                String[] provicnes = resume.getJobProvince().split(",");
                procount = provicnes.length;
            }
            String jobCities = resume.getJobCity();
            if(!StringUtils.isEmpty(jobCities)){
                String[] citys = resume.getJobCity().split(",");
                citycount = citys.length;
            }
            if ((procount + citycount) > 5) {
                throw new BusinessException(MsgCodeConstant.RESUME_JOB_COUNT_LIMIT,
                        MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.RESUME_JOB_COUNT_LIMIT)));
            }

            resume.setCreateid(createid.toString());
            response = resumeService.setUpResume(resume);
        } else {
            throw new AuthException(MsgCodeConstant.un_login,
                    MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    /**
     * 查询我创建的简历
     */
    @ApiOperation(value = "查询我创建的简历", notes = "查询我创建的简历", response = Response.class)
    @RequestMapping(value = "sel_my_resume", method = RequestMethod.GET)
    public Response searchMyResume() throws IOException {
        Response response;
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            response = resumeService.searchMyResume(createid.toString());
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    /**
     * 更新简历,刷新简历
     */
    @ApiOperation(value = "更新简历", notes = "更新简历", response = Response.class)
    @RequestMapping(value = "upd_resume", method = RequestMethod.POST)
    public Response updateResume(@ModelAttribute Resume resume) throws IOException {
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            return resumeService.updateResume(resume);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
    }

    /**
     * 预览简历
     */
    @ApiOperation(value = "预览简历", notes = "预览简历", response = Response.class)
    @RequestMapping(value = "preview_resume", method = RequestMethod.GET)
    public Response previewResume(@RequestParam String id) throws Exception {
        Response response = new Response();
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            Map<String,Object> map = new HashMap<>();
            map.put("id",id);
            map.put("createid",createid);
            Resume resume = resumeService.previewResume(map);
            if(resume!=null){
                response.setData(resume);
            }else {
                throw new PageNotFoundException(MsgCodeConstant.SYSTEM_ERROR,"页面不存在");
            }

        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    /**
     * 上传简历附件
     */
    @LoginAccess
    @ApiOperation(value = "上传简历附件", notes = "上传简历附件", response = Response.class)
    @RequestMapping(value = "upload_resume", method = RequestMethod.POST)
    public Response uploadResume(@RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        Response result = new Response();
        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession(false);
        if (null != session) {
            String url = zhbOssClient.uploadObject(file, "doc", "job");
            Map<String,Object> map = new HashMap<>();
            map.put(Constants.name, url);
            result.setData(map);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return result;
    }

    /**
     * 查询我创建的简历的全部信息
     */
    @ApiOperation(value = "查询我创建的简历的全部信息", notes = "查询我创建的简历的全部信息", response = Response.class)
    @RequestMapping(value = "sel_my_resume_info", method = RequestMethod.GET)
    public Response searchMyResumeAllInfo() throws IOException {
        Response response = new Response();
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            Map<String,Object> resume = resumeService.searchMyResumeAllInfo(createid.toString());
            response.setData(resume);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }


    /**
     * 老接口(之后废弃)
     * @param ids
     * @return
     */
    @ApiOperation(value = "简历批量设为已查看", notes = "简历批量设为已查看", response = Response.class)
    @RequestMapping(value = "upd_jobRelresume", method = RequestMethod.POST)
    public Response upd_jobRelresume(@ApiParam(value = "ids,逗号隔开") @RequestParam String ids) {
        Response response = new Response();
        String[] idList = ids.split(",");
        for (String id : idList) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("status", JobConstant.RESUME_STATUS_TWO);
            jrrService.updateJobRelResume(map);
        }
        return response;
    }

    /**
     * 下载简历附件
     */
    @ApiOperation(value = "下载简历附件", notes = "下载简历附件")
    @RequestMapping(value = "download_resume", method = RequestMethod.GET)
    public void downLoadResume(HttpServletResponse response, String id) throws IOException {
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            try {
                String fileurl = resumeService.downloadBill(id);
                response.setDateHeader("Expires", 0);
                response.setHeader("Cache-Control",
                        "no-store, no-cache, must-revalidate");
                response.addHeader("Cache-Control", "post-check=0, pre-check=0");
                response.setHeader("Content-disposition", "attachment;filename=" + fileurl);
                response.setContentType("application/octet-stream");

                fileUtil.downloadObject(response, fileurl, "doc", "job");
            } catch (Exception e) {
                log.error("download resume error! ", e);
                throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "操作失败");
            }
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
    }

    @ApiOperation(value = "HR通知列表", notes = "HR通知列表")
    @RequestMapping(value = "sel_myResumeLookRecord", method = RequestMethod.GET)
    public Response sel_myResumeLookRecord(@RequestParam(required = false,defaultValue = "1") String pageNo,
                                           @RequestParam(required = false,defaultValue = "10") String pageSize) {
        Response response = new Response();

        Paging<Map<String, String>> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        Long memberId = ShiroUtil.getCreateID();
        Map<String, Object> map = new HashMap<>();
        if (memberId != null) {
            map.put("id", memberId);
            List<Map<String, String>> list = resumeService.findAllMyResumeLookRecord(pager, map);
            pager.result(list);
            response.setData(pager);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    @ApiOperation(value = "增加屏蔽企业", notes = "增加屏蔽企业")
    @RequestMapping(value = "add_forbidKeyWords", method = RequestMethod.POST)
    public Response add_forbidKeyWords(@ApiParam(value = "公司id") @RequestParam(required = false) String company_id) {
        Response response = new Response();
        Long createid = ShiroUtil.getCreateID();
        ForbidKeyWords forbidKeyWords = new ForbidKeyWords();
        Map<String, Object> map = new HashMap<>();
        if (createid != null) {
            map.put("create_id", String.valueOf(createid));
            map.put("is_deleted", Constants.DeleteMark.NODELETE.toString());
            List<Map<String, String>> list = forbidKeyWordsService.queryKeyWordsList(map);
            if (list.size() == 10) {
                throw new BusinessException(MsgCodeConstant.FORBID_KEYWORDS_LIMIT, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.FORBID_KEYWORDS_LIMIT)));
            } else {
                for (Map<String, String> aList : list) {
                    Map map1 = (Map) aList;
                    String id = map1.get("company_id").toString();
                    if (id.equals(company_id)) {
                        throw new BusinessException(MsgCodeConstant.FORBID_KEYWORDS_REPEAT, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.FORBID_KEYWORDS_REPEAT)));
                    }
                }
                forbidKeyWords.setCreate_id(String.valueOf(createid));
                forbidKeyWords.setCompany_id(company_id);
                forbidKeyWordsService.addForbidKeyWords(forbidKeyWords);
            }
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    @ApiOperation(value = "查询屏蔽企业", notes = "查询屏蔽企业")
    @RequestMapping(value = "sel_forbidKeyWords", method = RequestMethod.GET)
    public Response sel_forbidKeyWords() {
        Response response = new Response();
        Long createid = ShiroUtil.getCreateID();
        Map<String, Object> map = new HashMap<>();
        if (createid != null) {
            map.put("create_id", String.valueOf(createid));
            map.put("is_deleted", Constants.DeleteMark.NODELETE.toString());
            List<Map<String, String>> list = forbidKeyWordsService.queryKeyWordsList(map);
            response.setData(list);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    @ApiOperation(value = "删除屏蔽企业", notes = "删除屏蔽企业")
    @RequestMapping(value = "del_forbidKeyWords", method = RequestMethod.POST)
    public Response del_forbidKeyWords(@RequestParam String id) {
        Response response = new Response();
        Long createid = ShiroUtil.getCreateID();
        if (createid != null) {
            ForbidKeyWords forbidKeyWords = new ForbidKeyWords();
            forbidKeyWords.setId(id);
            forbidKeyWords.setIs_deleted(Constants.DeleteMark.DELETE.toString());
            forbidKeyWordsService.deletleForbidKeyWords(forbidKeyWords);
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }

    @ApiOperation(value = "根据关键字获取企业", notes = "根据关键字获取企业")
    @RequestMapping(value = "sel_company_by_keywords", method = RequestMethod.GET)
    public Response sel_company_by_keywords(@RequestParam(required = false) String keywords) {
        Response response = new Response();
        List<Map<String, String>> list = memberService.queryCompanyByKeywords(keywords);
        response.setData(list);
        return response;
    }

    /**
     * 我申请的职位
     */
    @ApiOperation(value = "我申请的职位", notes = "我申请的职位", response = Response.class)
    @RequestMapping(value = "sel_my_position", method = RequestMethod.GET)
    public Response myApplyPosition(@RequestParam(required = false,defaultValue = "1") String pageNo,
                                    @RequestParam(required = false,defaultValue = "10") String pageSize) throws IOException {
        Long createid = ShiroUtil.getCreateID();
        Response response;
        if (createid != null) {
            Paging<Job> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
            response = jobService.myApplyPosition(pager, String.valueOf(createid));
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }


    /**
     * 我收到的简历    (老接口,之后废弃)
     */
    @ApiOperation(value = "我收到的简历", notes = "我收到的简历", response = Response.class)
    @RequestMapping(value = "sel_receive_resume", method = RequestMethod.GET)
    public Response receiveResume(@RequestParam(required = false,defaultValue = "1") String pageNo,
                                  @RequestParam(required = false,defaultValue = "10") String pageSize) throws IOException {

        Long createid = ShiroUtil.getCreateID();
        Response response;
        if (createid != null) {
            Paging<Map<String, String>> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
            response = resumeService.receiveResume(pager, createid.toString());
        } else {
            throw new AuthException(MsgCodeConstant.un_login, MsgPropertiesUtils.getValue(String.valueOf(MsgCodeConstant.un_login)));
        }
        return response;
    }
}
