package com.movision.controller.app2;

import com.movision.common.Response;
import com.movision.common.util.ShiroUtil;
import com.movision.facade.msgCenter.MsgCenterFacade;
import com.movision.mybatis.imSystemInform.entity.ImSystemInformVo;
import com.movision.utils.pagination.model.Paging;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author zhuangyuhao
 * @Date 2017/8/2 11:22
 */
@RestController
@RequestMapping("app/msg_center")
public class AppMsgCenterController {

    @Autowired
    private MsgCenterFacade msgCenterFacade;


    @ApiOperation(value = "获取我的消息中心的系统通知列表", notes = "获取我的消息中心的系统通知列表", response = Response.class)
    @RequestMapping(value = {"/get_my_msg_center_system_list"}, method = RequestMethod.GET)
    public Response getMyMsgCenterInformationList(@ApiParam(value = "第几页") @RequestParam(required = false, defaultValue = "1") String pageNo,
                                                  @ApiParam(value = "每页多少条") @RequestParam(required = false, defaultValue = "10") String pageSize
    ) {
        Response response = new Response();
        Paging<ImSystemInformVo> paging = new Paging<ImSystemInformVo>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<ImSystemInformVo> list = msgCenterFacade.getMsgInformationList(ShiroUtil.getAppUserID(), ShiroUtil.getAppUser().getRegisterTime(), paging);
        paging.result(list);
        response.setData(paging);
        return response;
    }

    /**
     * 查询通知详情接口
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "查询通知详情", notes = "用于查询通知详情接口", response = Response.class)
    @RequestMapping(value = "queryMyMsgInforDetails", method = RequestMethod.POST)
    public Response queryMyMsgInforDetails(@ApiParam(value = "通知id") @RequestParam String id) {
        Response response = new Response();
        ImSystemInformVo im = msgCenterFacade.queryMyMsgInforDetails(id);
        response.setMessage("查询成功");
        response.setData(im);
        return response;
    }


}