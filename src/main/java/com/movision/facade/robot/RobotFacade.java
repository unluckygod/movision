package com.movision.facade.robot;

import com.movision.common.constant.ImConstant;
import com.movision.common.constant.MsgCodeConstant;
import com.movision.common.constant.PointConstant;
import com.movision.common.constant.RobotConstant;
import com.movision.exception.BusinessException;
import com.movision.facade.collection.CollectionFacade;
import com.movision.facade.im.ImFacade;
import com.movision.facade.index.FacadeHeatValue;
import com.movision.facade.index.FacadePost;
import com.movision.facade.pointRecord.PointRecordFacade;
import com.movision.fsearch.utils.StringUtil;
import com.movision.mybatis.comment.entity.CommentVo;
import com.movision.mybatis.comment.service.CommentService;
import com.movision.mybatis.imuser.entity.ImUser;
import com.movision.mybatis.personalizedSignature.entity.PersonalizedSignature;
import com.movision.mybatis.personalizedSignature.service.PersonalizedSignatureService;
import com.movision.mybatis.post.entity.PostVo;
import com.movision.mybatis.post.service.PostService;
import com.movision.mybatis.robotComment.entity.RobotComment;
import com.movision.mybatis.robotComment.service.RobotCommentService;
import com.movision.mybatis.robotNickname.service.RobotNicknameService;
import com.movision.mybatis.robotOperationJob.entity.RobotOperationJob;
import com.movision.mybatis.robotOperationJob.entity.RobotOperationJobPage;
import com.movision.mybatis.robotOperationJob.service.RobotOperationJobService;
import com.movision.mybatis.systemLayout.service.SystemLayoutService;
import com.movision.mybatis.user.entity.User;
import com.movision.mybatis.user.service.UserService;
import com.movision.mybatis.userPhoto.entity.UserPhoto;
import com.movision.mybatis.userRefreshRecord.entity.UserRefreshRecord;
import com.movision.mybatis.userRefreshRecord.service.UserRefreshRecordService;
import com.movision.utils.DateUtils;
import com.movision.utils.ListUtil;
import com.movision.utils.UUIDGenerator;
import com.movision.utils.im.CheckSumBuilder;
import com.movision.utils.pagination.model.Paging;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;


/**
 * @Author zhuangyuhao
 * @Date 2017/9/18 10:50
 */
@Service
public class RobotFacade {

    private static Logger log = LoggerFactory.getLogger(RobotFacade.class);

    @Autowired
    private RobotOperationJobService robotOperationJobService;

    @Autowired
    private UserService userService;

    @Autowired
    private PointRecordFacade pointRecordFacade;

    @Autowired
    private FacadeHeatValue facadeHeatValue;

    @Autowired
    private PostService postService;

    @Autowired
    private FacadePost facadePost;

    @Autowired
    private CollectionFacade collectionFacade;

    @Autowired
    private RobotCommentService robotCommentService;

    @Autowired
    private RobotNicknameService nicknameService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PersonalizedSignatureService personalizedSignatureService;

    @Autowired
    private UserRefreshRecordService userRefreshRecordService;

    @Autowired
    private SystemLayoutService systemLayoutService;

    @Autowired
    private ImFacade imFacade;

    /**
     * 创建n个robot用户
     *
     * @param num
     */
    public void batchAddRobotUser(int num) throws IOException {
        //1 先找到本次批次的最大的id, 在 10001-20000 之间
        Integer maxId = userService.selectMaxRobotId();
        int firstId = 10001;    //默认第一个id是10001
        if (null != maxId) {
            firstId = maxId + 1;    //如果存在最大id， 则第一个id是maxid+1
        }
        //2 查询头像库 num个
        List<UserPhoto> userPhotoList = userService.queryUserPhotos(num);
        //3 查询昵称库 num个
        List<String> robotNicknameList = nicknameService.queryRoboltNickname(num);
        //4 查询签名库 num个
        List<PersonalizedSignature> personalizedSignatures = personalizedSignatureService.queryRoboltSignature(num);

        /**
         * 3 循环新增机器人个人信息
         */
        for (int i = 0; i < num; i++) {
            //机器人id
            int uid = firstId + i;
            String photo = userPhotoList.get(i).getUrl();   //头像
            String nickname = robotNicknameList.get(i); //昵称
            String sign = personalizedSignatures.get(i).getSignature();//签名

            User robot = createRobot(uid, photo, nickname, sign);
            //1)新增用户（暂时不需要处理yw_im_device, yw_im_user）
            userService.insertSelective(robot);
            //2)增加新用户注册积分流水
            pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.new_user_register.getCode(), PointConstant.POINT.new_user_register.getCode(), uid);
            //3)增加绑定手机号积分流水
            pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.binding_phone.getCode(), PointConstant.POINT.binding_phone.getCode(), uid);
            //4)新增im信息
            ImUser imUser = new ImUser();
            imUser.setName(nickname);
            imUser.setIcon(photo);
            imUser.setSign(sign);
            imUser.setUserid(uid);
            imUser.setAccid(CheckSumBuilder.getAccid(String.valueOf(uid)));  //根据userid生成accid

            imFacade.registerImUserAndSave(imUser, imUser.getUserid(), ImConstant.TYPE_APP);

        }
    }

    /**
     * 从list中随机选出几个数，并按照原来的顺序排列（比如从list中随机选出n个数）
     *
     * @param list
     * @param n
     * @return
     */
    public List<T> selectRandomList(List<T> list, int n) {
        //若list.size()大于n套，随机产生n个对象，并按照原来的顺序排列
        //若list的对象为ListObject
        if (list.size() > n) {
            Random randomId = new Random();
            //对随机的n个对象排成原来的默认顺序
            List<Integer> indexes = new ArrayList<Integer>();
            while (indexes.size() < n) {
                //对象在list里的位置
                int index = randomId.nextInt(list.size());
                if (!indexes.contains(index)) {
                    indexes.add(index);
                }
            }
            //对indexes排序
            Collections.sort(indexes);
            //取出indexes对应的list放到newList
            List<T> newList = new ArrayList<>();
            for (int index : indexes) {
                newList.add(list.get(index));
            }
            list.clear();
            list.addAll(newList);
        }
        return list;
    }


    /**
     * 创建机器人信息
     *
     * @return
     */
    private User createRobot(int uid, String photo, String nickname, String sign) {
        User robot = new User();
        robot.setId(uid);   //id
        String phone = uid + "000000";  //手机号
        robot.setPhone(phone);
        robot.setInvitecode(UUIDGenerator.gen6Uuid());    //自己的邀请码
//        robot.setNickname("robot_" + uid);  //昵称
        robot.setNickname(nickname);
//        robot.setPhoto(UserConstants.DEFAULT_APPUSER_PHOTO);    //头像
        robot.setPhoto(photo);
        robot.setSign(sign);    //签名
        robot.setSex(0);    //性别 默认是女
        robot.setBirthday(DateUtils.getDefaultBirthday());  //1990-08-19
        robot.setProvince("上海");
        robot.setCity("上海市");
        robot.setDeviceno("robot_deviceno_" + uid);
        robot.setIntime(new Date());
        robot.setLoginTime(new Date());
        robot.setIsrecommend(0);
        robot.setHeat_value(35);
        robot.setIp_city("310100");
        return robot;
    }

    /**
     * 机器人帖子点赞操作
     * <p>
     * (这里不需要进行手机推送，防止骚扰到用户。
     * 因为，我们的目的，是想增加某个帖子的点赞数量！)
     *
     * @param postid
     * @param num
     */
    public void robotZanPost(int postid, int num) {

        //1 集合机器人大军， 注：一个机器人只能点赞同一个帖子一次
        Map map = new HashMap();
        map.put("postid", postid);
        map.put("number", num);
        List<User> robotArmy = userService.queryNotRepeatZanRobots(map);

        //2 循环进行帖子点赞操作， 需要注意，点赞不能在同一个时刻
        for (int i = 0; i < robotArmy.size(); i++) {
            int userid = robotArmy.get(i).getId();


        }
    }

    /**
     * 批量点赞帖子操作
     *
     * @param postids
     */
    public void robotZanBatchPost(String postids, int number) {

        validatePostidsAndNum(postids, number);

        String[] postidArr = postids.split(",");
        //循环对每个帖子操作点赞
        Random random = new Random();
        for (int i = 0; i < postidArr.length; i++) {

            addSingleRobotJobProcess(number, Integer.valueOf(postidArr[i]), null,
                    RobotConstant.ROBOT_JOB_TYPE.zan_post.getCode(), 0);
        }
    }

    /**
     * 批量帖子机器人点赞、收藏、评论操作
     *
     * @param postids
     * @param num
     */
    /*public void robotActionWithZanCollectComment(String postids, int num) {

        validatePostidsAndNum(postids, num);

        String[] postidArr = postids.split(",");

        Random random = new Random();
        for (int i = 0; i < postidArr.length; i++) {
            //1 调用【机器人帖子点赞操作】
            robotZanPost(Integer.valueOf(postidArr[i]), (int) ((Math.random() * num) + 1));
            //2 调用【机器人帖子收藏操作】
            robotCollectPost(Integer.valueOf(postidArr[i]), (int) ((Math.random() * num) + 1));
            //3 调用【机器人帖子评论操作】
            robotshuntComment((int) ((Math.random() * num) + 1), Integer.valueOf(postidArr[i]));
        }
    }*/

    /**
     * 分流操作帖子评论
     */
    public void robotshuntComment(Integer num, Integer post, Integer theme) {
        //查询机器人分隔
//        Integer number = systemLayoutService.queryRobotSeparate("robot_separate");  //200条
        if (num > 1) {//2:40% 4:30% 5:30% --2

            insertPostCommentByRobolt(post, num, 2, theme);

//        } else if (num > number) {//2:60% 4:20% 5:20% --3
//
//            insertPostCommentByRobolt(post, num, 3);

        } else if (num == 1) {//随机100% --1

            insertPostCommentByRobolt(post, num, 1, theme);
        }
    }

    /**
     * 批量收藏帖子
     *
     * @param postids
     */
    public void robotCollectBatchPost(String postids, int number) {

        validatePostidsAndNum(postids, number);

        String[] postidArr = postids.split(",");

        for (int i = 0; i < postidArr.length; i++) {

            addSingleRobotJobProcess(number, Integer.valueOf(postidArr[i]), null,
                    RobotConstant.ROBOT_JOB_TYPE.collect_post.getCode(), 0);
        }
    }


    /**
     * 校验传参 postids 和 num
     *
     * @param postids
     * @param num
     */
    private void validatePostidsAndNum(String postids, int num) {
        if (StringUtils.isEmpty(postids)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "帖子id不能为空");
        }
        if (num < 1) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "机器人数量至少是1个");
        }

    }


    /**
     * 集合机器人大军（弃用，数据量是万级别的时候 性能差）
     * 随机选取指定数量的机器人
     * 如：随机选取500个机器人
     *
     * @param num
     * @return
     */
    private List<User> assembleRobotArmy(int num) {
        //1 先查询机器人大军(性能差)
        List<User> robots = userService.selectRobotUser();
        if (ListUtil.isEmpty(robots)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "机器人用户数量为0");
        }
        //2 随机选取n个机器人
        List<User> randomRobots = (List<User>) ListUtil.randomList(robots); //打乱机器人列表
        List<User> robotArmy = new ArrayList<>();
        int size = robots.size();
        if (num <= size) {
            robotArmy = randomRobots.subList(0, num);
        } else {
            robotArmy = randomRobots;
        }
        return robotArmy;
    }

    /**
     * 处理机器人点赞帖子
     *
     *
     * @param postid
     * @param userid
     */
    private void processRobotZanPost(int postid, int userid) {
        Map<String, Object> parammap = new HashMap<>();
        parammap.put("postid", postid);
        parammap.put("userid", userid);
        parammap.put("intime", new Date());
        //查询当前用户是否已点赞该帖
        int count = postService.queryIsZanPost(parammap);
        if (count == 0) {
            //增加帖子热度
            facadeHeatValue.addHeatValue(postid, 3, userid);
            //插入点赞历史记录
            postService.insertZanRecord(parammap);
            //更新帖子点赞数量字段
            postService.updatePostByZanSum(postid);
        }
    }


    /**
     * 机器人收藏帖子操作
     *
     * @param postid
     * @param num
     */
    public void robotCollectPost(int postid, int num) {
        //1 集合机器人大军, 注：一个机器人只能对同一个帖子收藏一次
        Map map = new HashMap();
        map.put("postid", postid);
        map.put("number", num);
        List<User> robotArmy = userService.queryNotRepeatCollectRobots(map);

        //2 循环进行收藏帖子操作
        for (int i = 0; i < robotArmy.size(); i++) {
            int userid = robotArmy.get(i).getId();
            collectionFacade.collectionPost(String.valueOf(postid), String.valueOf(userid), String.valueOf(0));
        }
    }

    /**
     * 机器人关注用户操作
     *
     * @param userid 被关注的人
     * @param num
     */
    public void robotFollowUser(int userid, int num) {
        //1 集合机器人大军
        Map map = new HashMap();
        map.put("number", num);
        map.put("userid", userid);
        List<User> robotArmy = userService.queryNotRepeatFollowRandomRobots(map);

        //2 循环进行关注作者操作
        for (int i = 0; i < robotArmy.size(); i++) {
            int robotid = robotArmy.get(i).getId();
            facadePost.concernedAuthorUser(robotid, userid);
        }
    }

    /**
     * 批量关注用户
     *
     * @param userids
     * @param num
     */
    public void batchFollowUser(String userids, int num) {
        if (StringUtils.isEmpty(userids)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "被关注的用户不能为空");
        }
        if (num < 1) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "机器人数量至少是1个");
        }

        String[] useridArr = userids.split(",");
        //循环对每个帖子操作点赞
        Random random = new Random();
        for (int i = 0; i < useridArr.length; i++) {
            //随机取[0,num+1) 之间的整数，最小是1，最大是num
//            int n = (int) ((Math.random() * num) + 1);

            addSingleRobotJobProcess(num, 0, null, RobotConstant.ROBOT_JOB_TYPE.follow_user.getCode(), Integer.valueOf(useridArr[i]));
        }
    }

    /**
     * 查询机器人列表
     *
     * @param name
     * @param pag
     * @return
     */
    public List<User> QueryRobotByList(String name, Paging<User> pag) {
        List<User> list = userService.findAllQueryRobotByList(name, pag);
        return list;
    }

    /**
     * 查询全部机器人
     *
     * @return
     */
    public List<User> QueryRobotByList() {
        return userService.selectRobotUser();
    }

    /**
     * 根据id查询机器人详情
     *
     * @param id
     * @return
     */
    public User queryRobotById(String id) {
        return userService.queryRobotById(Integer.parseInt(id));
    }


    public void updateRoboltById(String id, String email, String nickname, String phone, String photo, String sex) {
        User user = new User();
        if (StringUtil.isNotEmpty(id)) {
            user.setId(Integer.parseInt(id));
        }
        if (StringUtil.isEmpty(email)) {
            user.setEmail(email);
        }
        if (StringUtil.isNotEmpty(nickname)) {
            user.setNickname(nickname);
        }
        if (StringUtil.isNotEmpty(phone)) {
            user.setPhone(phone);
        }
        if (StringUtil.isNotEmpty(photo)) {
            user.setPhoto(photo);
        }
        if (StringUtil.isNotEmpty(sex)) {
            user.setSex(Integer.parseInt(sex));
        }
        userService.updateByPrimaryKeySelective(user);
    }

    /**
     * 查询机器人评论列表
     *
     * @param type
     * @return
     */
    public List<RobotComment> findAllQueryRoboltComment(String type, Paging<RobotComment> pag) {
        RobotComment comment = new RobotComment();
        if (StringUtil.isNotEmpty(type)) {
            comment.setType(Integer.parseInt(type));
        }
        return robotCommentService.findAllQueryRoboltComment(comment, pag);
    }

    /**
     * 新增机器人评论
     *
     * @param content
     * @param type
     */
    public Map insertRoboltComment(String content, String type) {
        RobotComment robotComment = new RobotComment();
        Map map = new HashMap();
        if (StringUtil.isNotEmpty(content)) {
            robotComment.setContent(content);
        }
        if (StringUtil.isNotEmpty(type)) {
            robotComment.setType(Integer.parseInt(type));
        }
        //查询是否重复
        Integer resault = robotCommentService.queryComentMessage(robotComment);
        if (resault == 0) {
            robotCommentService.insertRoboltComment(robotComment);
            map.put("code", 200);
        } else {
            map.put("code", 400);
        }
        return map;
    }

    /**
     * 删除机器人评论
     *
     * @param id
     */
    @Transactional
    public void deleteRoboltComment(String id) {
        RobotComment robotComment = new RobotComment();
        robotComment.setId(Integer.parseInt(id));
        robotComment.setContent("该评论已经被管理员删除");
        robotCommentService.deleteRoboltComment(robotComment);
    }

    /**
     * 根据id查询机器人评论
     *
     * @param id
     * @return
     */
    public RobotComment queryRoboltCommentById(Integer id) {
        return robotCommentService.queryCommentById(id);
    }

    /**
     * 更新机器人评论
     *
     * @param id
     * @param content
     * @param type
     */
    public void updateRoboltComent(String id, String content, String type) {
        RobotComment robotComment = new RobotComment();
        if (StringUtil.isNotBlank(id)) {
            robotComment.setId(Integer.parseInt(id));
        }
        if (StringUtil.isNotEmpty(content)) {
            robotComment.setContent(content);
        }
        if (StringUtil.isNotEmpty(type)) {
            robotComment.setType(Integer.parseInt(type));
        }
        robotCommentService.updateRoboltComent(robotComment);
    }

    /**
     * 机器人评论帖子
     *
     * @param postid 帖子id
     * @param num 机器人的数量
     */
    @Transactional
    public void insertPostCommentByRobolt(Integer postid, Integer num, Integer type, Integer theme) {

        if (num < 1) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "机器人数量至少是1个");
        }
        //1 集合num个机器人大军
        List<User> users = userService.queryRandomUser(num);
        //2 随机查询num条评论内容
        List<RobotComment> content = randomRobotComment(type, users.size(), theme);
        //3 获取帖子发表时间
        Date date = postService.queryPostIdByDate(postid);

        for (int i = 0; i < users.size(); i++) {
            //机器人的id
            Integer userid = users.get(i).getId();
            //1 插入评论表
            System.out.println(date.getTime());
            //插入评论
            insertPostComment(postid, content, date, i, userid);
            //2 更新帖子表的评论次数字段
            postService.updatePostBycommentsum(postid);
            //3 增加被评论的帖子热度
            facadeHeatValue.addHeatValue(postid, 4, userid);
        }
    }

    /**
     * 随机查询num条评论内容
     *
     * @param type  处理类型 1 随机查询num条评论内容; 2 根据指定的权重来查询不同类型评论
     * @param num
     * @param theme 帖子主题，1：人像， 2：风光
     * @return
     */
    public List<RobotComment> randomRobotComment(Integer type, Integer num, Integer theme) {
        List<RobotComment> content = new ArrayList<>();
        List<RobotComment> contens = new ArrayList();

        if (type == 1) {
            Map map = new HashMap();
            map.put("number", num);
            map.put("type", 0);
            //随机查询num条评论内容
            content = robotCommentService.queryRoboltComment(map);
            contens.addAll(content);
        } else if (type == 2) {//<=200 2:40% 4:30% 5:30%
            //有小数向上进位  Math.ceil 返回大于参数x的最小整数,即对浮点数向上取整.
            //查询评论占比率
            Double number = 0.0d;
            int commentType = 0;
            if (theme == 1) {
                //人像 90%占比
                number = systemLayoutService.queryRobotPercentage("robot_comment_portrait_ratio");
                commentType = 3;
            } else {
                //风光 90%占比
                number = systemLayoutService.queryRobotPercentage("robot_comment_scenery_ratio");
                commentType = 2;
            }
            Map map = new HashMap();
            querySelectedComment(num, contens, map, number, commentType);

            Double number2 = systemLayoutService.queryRobotPercentage("robot_comment_5_ratio"); //5%占比
            querySelectedComment(num, contens, map, number2, 4);
            querySelectedComment(num, contens, map, number2, 5);

        } /*else if (type == 3) {//>200 2:60% 4:20% 5:20%
            Double number = systemLayoutService.queryRobotPercentage("robot_separate_60");
            int mm = (int) (Math.ceil(num * number));
            Map map = new HashMap();
            map.put("number", mm);
            map.put("type", 2);
            content = robotCommentService.queryRoboltComment(map);
            contens.addAll(content);
            Double number2 = systemLayoutService.queryRobotPercentage("robot_separate_20");
            mm = (int) (Math.ceil(num * number2));
            map.put("number", mm);
            map.put("type", 4);
            content = robotCommentService.queryRoboltComment(map);
            contens.addAll(content);
            mm = (int) (Math.ceil(num * number2));
            map.put("number", mm);
            map.put("type", 5);
            content = robotCommentService.queryRoboltComment(map);
            contens.addAll(content);
        }*/
        return contens;
    }

    /**
     * 查询指定类型的评论字典
     *
     * @param num
     * @param contens
     * @param map
     * @param number2
     * @param commentType 评论类型 ：0：普通 1：专业摄影 2：风光 3：人像 4：诗词 5：段子
     */
    private void querySelectedComment(Integer num, List<RobotComment> contens, Map map, Double number2, int commentType) {
        int mm;
        List<RobotComment> content;
        mm = (int) (Math.ceil(num * number2));
        map.put("number", mm);
        map.put("type", commentType);
        content = robotCommentService.queryRoboltComment(map);
        contens.addAll(content);
    }

    /**
     * 批量对帖子进行机器人评论操作
     *
     * @param postids
     * @param num     最大机器人数量
     */
    public void robotCommentBatchPost(String postids, int num, int theme) {
        //校验传参
        validatePostidsAndNum(postids, num);

        String[] postidArr = postids.split(",");
        //循环建立机器人评论任务
        for (int i = 0; i < postidArr.length; i++) {

            addSingleRobotJobProcess(num, Integer.valueOf(postidArr[i]), theme,
                    RobotConstant.ROBOT_JOB_TYPE.comment_post.getCode(), 0);
        }
    }

    /**
     * 新增机器人任务（帖子操作共用）
     *
     * @param num
     * @param postid
     * @param theme  需要评论的帖子的主题。1：人像， 2：风光
     * @param type   任务类型。1：点赞，2：收藏，3：评论，4：关注，5：投票
     * @param userid
     */
    public void addSingleRobotJobProcess(int num, int postid, Integer theme, int type, int userid) {
        //1 校验参数
        if (num < 1) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "输入数量至少是1个");
        }
        //2 获取最大批次--根据postid/userid 和 type 来判断
        int batch = getBatch(postid, type, userid);
        //3 新增机器人任务
        if (type == 3) {
            if (num > 20) {
                //如果是评论任务，且要求的评论数量大于20条的时候，则需要进行特殊处理，根据机器人数量进行拆分任务
                Double ratio = 0.0d;
                int commentType = 0;
                if (theme == 1) {
                    //人像 90%占比
                    ratio = systemLayoutService.queryRobotPercentage("robot_comment_portrait_ratio");
                    commentType = 3;
                } else {
                    //风光 90%占比
                    ratio = systemLayoutService.queryRobotPercentage("robot_comment_scenery_ratio");
                    commentType = 2;
                }
                int robotJobNum = (int) (Math.ceil(num * ratio));
                //拆分后的第一个job
                addRobotJob(robotJobNum, postid, theme, type, userid, batch, commentType);  //风光 或者 人像
                //拆分后的第二个job
                Double ratio2 = systemLayoutService.queryRobotPercentage("robot_comment_5_ratio"); //5%占比
                int job2Num = (int) (Math.ceil(num * ratio2));
                addRobotJob(job2Num, postid, theme, type, userid, batch, 4);    //诗词
                //拆分后的第三个job
                addRobotJob(job2Num, postid, theme, type, userid, batch, 5);    //段子
            } else {
                int commentType = 2;
                if (theme == 1) {
                    commentType = 3;
                }
                addRobotJob(num, postid, theme, type, userid, batch, commentType);
            }

        } else {
            addRobotJob(num, postid, theme, type, userid, batch, null);
        }

    }

    private int getBatch(int postid, int type, int userid) {
        RobotOperationJob currentJob;
        Map map = new HashMap();
        map.put("type", type);
        if (postid == 0) {
            map.put("userid", userid);
            currentJob = robotOperationJobService.selectCurrentUseridBatch(map);
        } else {
            map.put("postid", postid);
            currentJob = robotOperationJobService.selectCurrentPostidBatch(map);
        }
        int batch = 1;
        if (currentJob != null) {
            batch = currentJob.getBatch() + 1;
        }
        return batch;
    }


    /**
     * 新增机器人任务
     *
     * @param num
     * @param postid
     * @param theme
     * @param type
     * @param userid
     * @param batch
     * @param commentType
     */
    private void addRobotJob(int num, int postid, Integer theme, int type, int userid, int batch, Integer commentType) {
        RobotOperationJob job = new RobotOperationJob();
        job.setIntime(new Date());
        job.setCount(num);
        job.setNumber(num);
        job.setPostid(postid);
        job.setTheme(theme);    //评论的时候才用到
        job.setStatus(0);   //待处理
        job.setBatch(batch);
        job.setType(type);
        job.setUserid(userid);
        job.setCommentType(commentType);
        job.setImmediate(0);    //非立即执行状态

        robotOperationJobService.add(job);
    }

    /**
     * 插入评论表
     *
     * @param postid
     * @param content
     * @param date
     * @param i
     * @param userid
     */
    private void insertPostComment(Integer postid, List<RobotComment> content, Date date, int i, Integer userid) {
        CommentVo vo = new CommentVo();
        vo.setPostid(postid);
        vo.setContent(content.get(i).getContent());
        vo.setUserid(userid);
        vo.setZansum(0);
        vo.setIsdel("0");
        vo.setStatus(1);    //审核状态：0待审核 1审核通过 2审核不通过（iscontribute为1时不为空）
        vo.setIscontribute(0);  //是否为特邀嘉宾的评论：0否 1是
        //获取一个几万的随机数,变更评论时间
        Long d = getRandomDate(date);
        vo.setIntime(new Date(d));

        commentService.insertComment(vo);

    }

    /**
     * 获取一个随机的日期
     *
     * @param date
     * @return
     */
    private Long getRandomDate(Date date) {
        //获取随机事件的最大数 和最小数
        Integer max = systemLayoutService.queryRobotSeparate("robot_random_max_time");
        Integer min = systemLayoutService.queryRobotSeparate("robot_random_min_time");
        Random random = new Random();
        int s = random.nextInt(max) % (max - min + 1) + min;
        return date.getTime() + s;
    }

    /**
     * 批量修改机器人的头像
     *
     * @param userids
     */
    public void batchChangeRobotPhoto(String userids) {
        if (StringUtils.isEmpty(userids)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "请选择机器人");
        }

        String[] robotArr = userids.split(",");
        int num = robotArr.length;
        //头像列表
        List<UserPhoto> userPhotoList = userService.queryUserPhotos(num);
        for (int i = 0; i < num; i++) {
            String photo = userPhotoList.get(i).getUrl();

            User user = new User();
            user.setId(Integer.valueOf(robotArr[i]));
            user.setPhoto(photo);
            userService.updateByPrimaryKeySelective(user);
        }
    }

    /**
     * 批量修改机器人的昵称
     *
     * @param userids
     */
    public void batchChangeRobotNickname(String userids) {
        if (StringUtils.isEmpty(userids)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "请选择机器人");
        }

        String[] robotArr = userids.split(",");
        int num = robotArr.length;

        List<String> robotNicknameList = nicknameService.queryRoboltNickname(num);

        for (int i = 0; i < num; i++) {
            String nickname = robotNicknameList.get(i);

            User user = new User();
            user.setId(Integer.valueOf(robotArr[i]));
            user.setNickname(nickname);
            userService.updateByPrimaryKeySelective(user);
        }
    }

    public void batchChangeRobot(String userids) {
        if (StringUtils.isEmpty(userids)) {
            throw new BusinessException(MsgCodeConstant.SYSTEM_ERROR, "请选择机器人");
        }

        String[] robotArr = userids.split(",");
        int num = robotArr.length;

        List<String> robotNicknameList = nicknameService.queryRoboltNickname(num);
        List<UserPhoto> userPhotoList = userService.queryUserPhotos(num);
        List<PersonalizedSignature> personalizedSignatures = personalizedSignatureService.queryRoboltSignature(num);

        for (int i = 0; i < num; i++) {
            String nickname = robotNicknameList.get(i);     //昵称
            String photo = userPhotoList.get(i).getUrl();   //头像
            String sign = personalizedSignatures.get(i).getSignature();//签名

            User user = new User();
            user.setId(Integer.valueOf(robotArr[i]));
            user.setNickname(nickname);
            user.setSign(sign);
            user.setPhoto(photo);

            userService.updateByPrimaryKeySelective(user);
        }
    }


    public void allChangeRobotInfo() {

        List<User> userList = userService.selectRobotUser();
        String ids = "";
        for (User user : userList) {
            ids += user.getId() + ",";
        }
        String userids = ids.substring(0, ids.length() - 1);

        batchChangeRobot(userids);
    }

    public void insertMongoPostView(Integer num) {
        //1 查询所有的帖子（包括帖子所属的圈子id）
        List<PostVo> postList = postService.findAllPostListHeat();

        //2 遍历所有的帖子
        batchInsertPostViewRecord(num, postList);

    }


    /**
     * 给固定帖子增加浏览量
     * @param num
     * @param postid
     */
    public void insertMongoPostViewBy(Integer num,Integer postid){
        Random random = new Random();
             //1 随机取n个机器人
            //int n = random.nextInt(num + 1);
            //根据帖子id查询属于哪个圈子
            int circleid=postService.queryPostByCircleid(postid.toString());
             //2 查出n个机器人的信息
            //List<User> robotArmy = userService.queryRandomUser(n);
            //3 循环插入帖子浏览记录
            for (int j = 0; j < num; j++) {
                //int userid = robotArmy.get(j).getId();

                UserRefreshRecord userRefreshRecord = new UserRefreshRecord();
                userRefreshRecord.setId(UUID.randomUUID().toString().replaceAll("\\-", ""));
                userRefreshRecord.setUserid(-1);
                userRefreshRecord.setPostid(postid);
                userRefreshRecord.setCrileid(String.valueOf(circleid));
                userRefreshRecord.setIntime(DateUtils.date2Str(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
                userRefreshRecord.setType(1);
                userRefreshRecord.setLabelid(-1);
                userRefreshRecordService.insert(userRefreshRecord);
            }
            //修改帖子浏览量
        Map map = new HashMap();
        map.put("count",num);
        map.put("postid",postid);
            postService.updateRobotCountView(map);

    }


    public void insertSomeonePostView(Integer num, Integer uid) {
        //1 查询该用户所有帖子（包括帖子所属的圈子id）
        List<PostVo> postList = postService.findUserPost(uid);

        //2 遍历所有的帖子，封装成帖子浏览记录，并插入mongoDB
        batchInsertPostViewRecord(num, postList);
    }

    /**
     * 遍历所有的帖子，封装成帖子浏览记录，并插入mongoDB
     *
     * @param num
     * @param postList
     */
    private void batchInsertPostViewRecord(Integer num, List<PostVo> postList) {
        Random random = new Random();
        for (int i = 0; i < postList.size(); i++) {
            //1 随机取n个机器人
            int n = random.nextInt(num + 1);

            int postid = postList.get(i).getId();
            int circleid = postList.get(i).getCircleid() == null ? 0 : postList.get(i).getCircleid();
            //2 查出n个机器人的信息
            List<User> robotArmy = userService.queryRandomUser(n);
            //3 循环插入帖子浏览记录
            for (int j = 0; j < robotArmy.size(); j++) {
                int userid = robotArmy.get(j).getId();

                UserRefreshRecord userRefreshRecord = new UserRefreshRecord();
                userRefreshRecord.setId(UUID.randomUUID().toString().replaceAll("\\-", ""));
                userRefreshRecord.setUserid(userid);
                userRefreshRecord.setPostid(postid);
                userRefreshRecord.setCrileid(String.valueOf(circleid));
                userRefreshRecord.setIntime(DateUtils.date2Str(new Date(), "yyyy-MM-dd HH:mm:ss:SSS"));
                userRefreshRecord.setType(1);
                userRefreshRecord.setLabelid(-1);
                userRefreshRecordService.insert(userRefreshRecord);
            }
        }
    }

    public List<RobotOperationJobPage> findAllRobotJobPage(Integer type, Paging<RobotOperationJobPage> pagePaging) {
        Map map = new HashMap();
        if (type == null) {
            map.put("type", 0);
        } else {
            map.put("type", type);
        }
        List<RobotOperationJobPage> pageList = robotOperationJobService.findAllRobotJobPage(map, pagePaging);
        return pageList;
    }

    /**
     * 改变任务为立即执行
     *
     * @param jobid
     * @return
     */
    public int changeJobexecuteImmediately(Integer jobid) {

        RobotOperationJob job = robotOperationJobService.selectByPrimaryKey(jobid);
        int status = job.getStatus();
        if (status == 0) {    //状态是处理中的任务才出来
            job.setImmediate(1);    //立即执行
            return robotOperationJobService.updateSelective(job);   //返回1
        }
        return 0;
    }



}
