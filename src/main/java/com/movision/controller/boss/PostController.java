package com.movision.controller.boss;

import com.movision.common.Response;
import com.movision.facade.boss.PostFacade;
import com.movision.mybatis.activePart.entity.ActivePartList;
import com.movision.mybatis.circle.entity.Circle;
import com.movision.mybatis.comment.entity.Comment;
import com.movision.mybatis.comment.entity.CommentVo;
import com.movision.mybatis.post.entity.*;
import com.movision.mybatis.rewarded.entity.RewardedVo;
import com.movision.mybatis.share.entity.SharesVo;
import com.movision.mybatis.user.entity.User;
import com.movision.mybatis.user.entity.UserLike;
import com.movision.mybatis.user.entity.Validateinfo;
import com.movision.utils.pagination.model.Paging;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * @Author zhurui
 * @Date 2017/2/7 9:05
 */
@RestController
@RequestMapping("/boss/post")
public class PostController {
    @Autowired
    PostFacade postFacade;

    /**
     * 后台管理-查询帖子列表
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查询帖子列表", notes = "查询帖子列表", response = Response.class)
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public Response queryPostByList(@RequestParam(required = false, defaultValue = "1") String pageNo,
                                    @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<PostList> pager = new Paging<PostList>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<PostList> list = postFacade.queryPostByList(pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 后台管理-查询活动列表
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查询活动列表", notes = "查询活动列表", response = Response.class)
    @RequestMapping(value = "/list_active_list", method = RequestMethod.POST)
    public Response queryPostActiveToByList(@RequestParam(required = false, defaultValue = "1") String pageNo,
                                            @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<PostActiveList> pager = new Paging<PostActiveList>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<PostActiveList> list = postFacade.queryPostActiveToByList(pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 后台管理-查询报名列表
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查询报名列表", notes = "查询报名列表", response = Response.class)
    @RequestMapping(value = "/list_call_list", method = RequestMethod.POST)
    public Response queyPostCallActive(@RequestParam(required = false, defaultValue = "1") String pageNo,
                                       @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<ActivePartList> pager = new Paging<ActivePartList>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<ActivePartList> list = postFacade.queyPostCallActive(pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }
    /**
     * 查询发帖人信息
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "查询发帖人信息", notes = "查询发帖人信息", response = Response.class)
    @RequestMapping(value = "/query_posted_man", method = RequestMethod.POST)
    public Response queryPostByPosted(@ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        User user = postFacade.queryPostByPosted(postid);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        response.setData(user);
        return response;
    }

    /**
     * 删除帖子
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "逻辑删除帖子", notes = "逻辑删除帖子", response = Response.class)
    @RequestMapping(value = "/delete_post", method = RequestMethod.POST)
    public Response deletePost(@ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        Map<String, Integer> map = postFacade.deletePost(postid);
        if (response.getCode() == 200) {
            response.setMessage("删除成功");
        }
        response.setData(map);
        return response;
    }

    /**
     * 后台管理-帖子列表-查看评论
     *
     * @param postid
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查看帖子评论", notes = "查看帖子评论", response = Response.class)
    @RequestMapping(value = "/query_post_appraise", method = RequestMethod.POST)
    public Response queryPostAppraise(@ApiParam(value = "帖子id") @RequestParam String postid,
                                      @RequestParam(required = false, defaultValue = "1") String pageNo,
                                      @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<CommentVo> pager = new Paging<CommentVo>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<CommentVo> list = postFacade.queryPostAppraise(postid, pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 后台管理-帖子列表-查看评论-评论详情列表
     *
     * @param commentid
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查询帖子评论详情", notes = "用于查询帖子评论详情接口", response = Response.class)
    @RequestMapping(value = "/query_post_comment_particulars", method = RequestMethod.POST)
    public Response queryPostByCommentParticulars(@ApiParam(value = "评论id") @RequestParam String commentid,
                                                  @RequestParam(required = false, defaultValue = "1") String pageNo,
                                                  @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<CommentVo> pager = new Paging<CommentVo>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<CommentVo> list = postFacade.queryPostByCommentParticulars(commentid, pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 添加评论接口
     *
     * @param postid
     * @param userid
     * @param content
     * @return
     */
    @ApiOperation(value = "添加评论", notes = "用于添加评论接口", response = Response.class)
    @RequestMapping(value = "add_post_comment", method = RequestMethod.POST)
    public Response addPostAppraise(@ApiParam(value = "帖子id") @RequestParam String postid,
                                    @ApiParam(value = "评论人") @RequestParam String userid,
                                    @ApiParam(value = "评论内容") @RequestParam String content) {
        Response response = new Response();
        Map<String, Integer> status = postFacade.addPostAppraise(postid, userid, content);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(status);
        return response;
    }

    /**
     * 后台管理-帖子列表-查看评论-删除帖子评论
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "删除帖子评论", notes = "删除帖子评论", response = Response.class)
    @RequestMapping(value = "/delete_post_appraise", method = RequestMethod.POST)
    public Response deletePostAppraise(@ApiParam(value = "评论id") @RequestParam String id) {
        Response response = new Response();
        postFacade.deletePostAppraise(id);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        return response;
    }

    /**
     * 后台管理-帖子列表-帖子打赏
     *
     * @param postid
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "查看帖子打赏列表", notes = "查看帖子打赏列表", response = Response.class)
    @RequestMapping(value = "/query_post_award", method = RequestMethod.POST)
    public Response queryPostAward(@ApiParam(value = "帖子id") @RequestParam String postid,
                                   @RequestParam(required = false, defaultValue = "1") String pageNo,
                                   @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<RewardedVo> pager = new Paging<>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<RewardedVo> list = postFacade.queryPostAward(postid, pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 后台管理-帖子列表-帖子预览
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "帖子预览", notes = "帖子预览", response = Response.class)
    @RequestMapping(value = "/query_post_particulars", method = RequestMethod.POST)
    public Response queryPostParticulars(@ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        PostList list = postFacade.queryPostParticulars(postid);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        response.setData(list);
        return response;
    }


    /**
     * 后台管理-帖子列表-活动预览
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "活动预览", notes = "活动预览", response = Response.class)
    @RequestMapping(value = "/query_post_active", method = RequestMethod.POST)
    public Response queryPostActiveQ(@ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        PostList list = postFacade.queryPostActiveQ(postid);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        response.setData(list);
        return response;
    }
    /**
     * 后台管理-添加帖子
     *
     * @param title
     * @param subtitle
     * @param type
     * @param circleid
     * @param coverimg
     * @param postcontent
     * @param isessence
     * @param time
     * @return
     */
    @ApiOperation(value = "添加帖子", notes = "添加帖子", response = Response.class)
    @RequestMapping(value = "/add_post", method = RequestMethod.POST)
    public Response addPost(HttpServletRequest request,
                            @ApiParam(value = "帖子标题") @RequestParam String title,//帖子标题
                            @ApiParam(value = "帖子副标题") @RequestParam String subtitle,//帖子副标题
                            @ApiParam(value = "帖子类型 0 普通帖 1 原生视频帖") @RequestParam String type,//帖子类型
                            @ApiParam(value = "圈子id") @RequestParam String circleid,//圈子id
                            @ApiParam(value = "发帖人") @RequestParam String userid,//发帖人
                            @ApiParam(value = "帖子封面(需要上传的文件)") @RequestParam(required = false, value = "coverimg") MultipartFile coverimg,//帖子封面
                            @ApiParam(value = "视频地址") @RequestParam(required = false, value = "vid") MultipartFile vid,//视频url
                            @ApiParam(value = "视频文件") @RequestParam(required = false, value = "bannerimgurl") MultipartFile bannerimgurl,//视频图片
                            @ApiParam(value = "帖子内容") @RequestParam String postcontent,//帖子内容
                            @ApiParam(value = "首页精选") @RequestParam(required = false) String isessence,//首页精选
                            @ApiParam(value = "圈子精选") @RequestParam(required = false) String ishot,//精选池中的帖子圈子精选贴
                            @ApiParam(value = "精选排序(0-9数字)") @RequestParam(required = false) String orderid,//精选排序
                            @ApiParam(value = "精选日期 yyyy-MM-dd HH:mm:ss") @RequestParam(required = false) String time) {//精选日期
        Response response = new Response();
        Map<String, Integer> resaut = postFacade.addPost(request, title, subtitle, type, circleid, vid, bannerimgurl, userid, coverimg, postcontent, isessence, ishot, orderid, time);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(resaut);
        return response;
    }

    /**
     * 后台管理-添加活动帖子
     * @param title
     * @param subtitle
     * @param type
     * @param money
     * @param coverimg
     * @param postcontent
     * @param isessence
     * @param orderid
     * @param time
     * @param begintime
     * @param endtime
     * @param userid
     * @return
     */
    @ApiOperation(value = "添加活动帖子", notes = "添加活动帖子", response = Response.class)
    @RequestMapping(value = "/add_active_post", method = RequestMethod.POST)
    public Response addPostActiveList(HttpServletRequest request,
                                      @ApiParam(value = "帖子标题") @RequestParam String title,
                                      @ApiParam(value = "帖子副标题") @RequestParam String subtitle,
                                      @ApiParam(value = "帖子类型") @RequestParam String type,
                                      @ApiParam(value = "单价") @RequestParam String money,
                                      @ApiParam(value = "帖子封面") @RequestParam(required = false) MultipartFile coverimg,
                                      @ApiParam(value = "内容") @RequestParam String postcontent,
                                      @ApiParam(value = "首页精选") @RequestParam(required = false) String isessence,
                                      @ApiParam(value = "精选排序") @RequestParam(required = false) String orderid,
                                      @ApiParam(value = "精选日期") @RequestParam(required = false) String time,
                                      @ApiParam(value = "活动开始日期") @RequestParam String begintime,
                                      @ApiParam(value = "活动结束日期") @RequestParam String endtime,
                                      @ApiParam(value = "发帖人") @RequestParam String userid){
        Response response = new Response();
         Map<String,Integer> result= postFacade.addPostActive(request,title,subtitle,type,money,coverimg,postcontent,isessence,orderid,time,begintime,endtime,userid);
        if(response.getCode()==200){
            response.setMessage("添加成功");
        }
        response.setData(result);
        return  response;
    }

    /**
     * 后台管理-帖子列表-帖子加精
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "帖子加精", notes = "用于帖子加精接口", response = Response.class)
    @RequestMapping(value = "/add_post_choiceness", method = RequestMethod.POST)
    public Response addPostChoiceness(@ApiParam(value = "帖子id") @RequestParam String postid,
                                      @ApiParam(value = "帖子副标题") @RequestParam(required = false) String subtitle,
                                      @ApiParam(value = "精选日期(加精时填)") @RequestParam(required = false) String essencedate,
                                      @ApiParam(value = "精选排序") @RequestParam String orderid) {
        Response response = new Response();
        Map<String, Integer> result = postFacade.addPostChoiceness(postid, subtitle, essencedate, orderid);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(result);
        return response;
    }


    /**
     * 帖子加精数据回显
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "帖子加精数据回显", notes = "用于帖子加精时，数据回填接口", response = Response.class)
    @RequestMapping(value = "/query_post_choiceness", method = RequestMethod.POST)
    public Response queryPostChoiceness(@ApiParam(value = "精选日期") @RequestParam String essencedate,
                                        @ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        PostChoiceness list = postFacade.queryPostChoiceness(postid, essencedate);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(list);
        return response;
    }


    /**
     * 后台管理-帖子列表-帖子分享列表
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "帖子分享列表", notes = "用于帖子分享列表展示接口", response = Response.class)
    @RequestMapping(value = "/query_post_share_list", method = RequestMethod.POST)
    public Response queryPostShareList(@ApiParam(value = "帖子id") @RequestParam String postid,
                                       @RequestParam(required = false, defaultValue = "1") String pageNo,
                                       @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<SharesVo> pager = new Paging<SharesVo>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<SharesVo> list = postFacade.queryPostShareList(postid, pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 模糊查询发帖人
     *
     * @param name
     * @param pageNo
     * @param pageSize
     * @return
     */
    @ApiOperation(value = "模糊查询发帖人", notes = "用于模糊查询发帖人接口", response = Response.class)
    @RequestMapping(value = "/like_query_post_nickname", method = RequestMethod.POST)
    public Response likeQueryPostByNickname(@ApiParam(value = "关键字") @RequestParam String name,
                                            @RequestParam(required = false, defaultValue = "1") String pageNo,
                                            @RequestParam(required = false, defaultValue = "10") String pageSize) {
        Response response = new Response();
        Paging<UserLike> pager = new Paging<UserLike>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<UserLike> list = postFacade.likeQueryPostByNickname(name, pager);
        if (response.getCode() == 200) {
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }

    /**
     * 选择圈子类型
     *
     * @return
     */
    @ApiOperation(value = "选择圈子类型", notes = "用于选择圈子类型接口", response = Response.class)
    @RequestMapping(value = "/query_list_circle_type", method = RequestMethod.POST)
    public Response queryListByCircleType() {
        Response response = new Response();
        Map<String, Object> list = postFacade.queryListByCircleType();
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(list);
        return response;
    }


    /**
     * 编辑帖子-数据回显
     *
     * @param postid
     * @return
     */
    @ApiOperation(value = "编辑帖子数据回显", notes = "用于编辑帖子时数据回显", response = Response.class)
    @RequestMapping(value = "query_post_echo", method = RequestMethod.POST)
    public Response queryPostByIdEcho(@ApiParam(value = "帖子id") @RequestParam String postid) {
        Response response = new Response();
        PostCompile postCompile = postFacade.queryPostByIdEcho(postid);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(postCompile);
        return response;
    }

    /**
     * 后台管理-帖子编辑
     * @param request
     * @param title
     * @param subtitle
     * @param type
     * @param userid
     * @param circleid
     * @param coverimg
     * @param vid
     * @param postcontent
     * @param isessence
     * @param ishot
     * @param orderid
     * @param time
     * @return
     */
    @ApiOperation(value = "编辑帖子", notes = "用于帖子编辑接口", response = Response.class)
    @RequestMapping(value = "update_post", method = RequestMethod.POST)
    public Response updatePostById(HttpServletRequest request,
                                   @ApiParam(value = "帖子id（必填）") @RequestParam String id,
                                   @ApiParam(value = "帖子标题") @RequestParam(required = false) String title,//帖子标题
                                   @ApiParam(value = "帖子副标题") @RequestParam(required = false) String subtitle,//帖子副标题
                                   @ApiParam(value = "帖子类型 0 普通帖 1 原生视频帖") @RequestParam(required = false) String type,//帖子类型
                                   @ApiParam(value = "发帖人（必填且必须是管理员-1）") @RequestParam String userid,//发帖人
                                   @ApiParam(value = "圈子id") @RequestParam(required = false) String circleid,//圈子id
                                   @ApiParam(value = "帖子封面(需要上传的文件)") @RequestParam(required = false, value = "coverimg") MultipartFile coverimg,//帖子封面
                                   @ApiParam(value = "视频地址") @RequestParam(required = false, value = "vid") MultipartFile vid,//视频url
                                   @ApiParam(value = "视频封面地址url") @RequestParam(required = false, value = "bannerimgurl") MultipartFile bannerimgurl,//视频封面url
                                   @ApiParam(value = "帖子内容（必填）") @RequestParam String postcontent,//帖子内容
                                   @ApiParam(value = "首页精选") @RequestParam(required = false) String isessence,//首页精选
                                   @ApiParam(value = "圈子精选") @RequestParam(required = false) String ishot,//本圈精华
                                   @ApiParam(value = "精选排序(0-9数字)") @RequestParam(required = false) String orderid,//精选排序
                                   @ApiParam(value = "精选日期 yyyyMMddHHmmss") @RequestParam(required = false) String time) {
        Response response = new Response();
        Map<String, Integer> map = postFacade.updatePostById(request, id, title, subtitle, type, userid, circleid, vid, bannerimgurl, coverimg, postcontent, isessence, ishot, orderid, time);
        if (response.getCode() == 200) {
            response.setMessage("操作成功");
        }
        response.setData(map);
        return response;
    }


    /**
     * 帖子模糊查询
     *
     * @param pageNo
     * @param pageSize
     * @param title
     * @param circleid
     * @return
     */
    @ApiOperation(value = "帖子联合条件搜索", notes = "用于精确查找帖子搜索接口", response = Response.class)
    @RequestMapping(value = "/post_search",method = RequestMethod.POST)
    public Response postSearch(@ApiParam(value = "当前页") @RequestParam(required = false, defaultValue = "1") String pageNo,
                               @ApiParam(value = "每页几条") @RequestParam(required = false, defaultValue = "10") String pageSize,
                               @ApiParam(value = "帖子标题")@RequestParam(required = false) String title,
                               @ApiParam(value = "圈子id")@RequestParam(required = false) String circleid,
                               @ApiParam(value = "发帖人") @RequestParam(required = false) String nickname,
                               @ApiParam(value = "帖子内容") @RequestParam(required = false) String postcontent,
                               @ApiParam(value = "结束时间") @RequestParam(required = false) String endtime,
                               @ApiParam(value = "开始时间") @RequestParam(required = false) String begintime,
                               @ApiParam(value = "精选日期") @RequestParam(required = false) String essencedate) {
        Response response=new Response();
        Paging<PostList> pager = new Paging<PostList>(Integer.valueOf(pageNo), Integer.valueOf(pageSize));
        List<PostList> list = postFacade.postSearch(title, circleid, nickname, postcontent, endtime, begintime, essencedate, pager);
        if (response.getCode()==200){
            response.setMessage("查询成功");
        }
        pager.result(list);
        response.setData(pager);
        return response;
    }


}
