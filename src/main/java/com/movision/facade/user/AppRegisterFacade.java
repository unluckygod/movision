package com.movision.facade.user;

import com.google.gson.Gson;
import com.movision.common.Response;
import com.movision.common.constant.*;
import com.movision.common.util.ShiroUtil;
import com.movision.exception.BusinessException;
import com.movision.facade.address.AddressFacade;
import com.movision.facade.im.ImFacade;
import com.movision.facade.pointRecord.PointRecordFacade;
import com.movision.mybatis.bossUser.entity.BossUser;
import com.movision.mybatis.bossUser.service.BossUserService;
import com.movision.mybatis.circle.entity.Circle;
import com.movision.mybatis.circle.service.CircleService;
import com.movision.mybatis.coupon.entity.Coupon;
import com.movision.mybatis.coupon.service.CouponService;
import com.movision.mybatis.couponTemp.entity.CouponTemp;
import com.movision.mybatis.imDevice.entity.ImDevice;
import com.movision.mybatis.imDevice.service.ImDeviceService;
import com.movision.mybatis.imuser.entity.ImUser;
import com.movision.mybatis.imuser.service.ImUserService;
import com.movision.mybatis.user.entity.LoginUser;
import com.movision.mybatis.user.entity.RegisterUser;
import com.movision.mybatis.user.entity.User;
import com.movision.mybatis.user.entity.Validateinfo;
import com.movision.mybatis.user.service.UserService;
import com.movision.mybatis.weixinguangzhu.service.WeixinGuangzhuService;
import com.movision.shiro.realm.ShiroRealm;
import com.movision.utils.*;
import com.movision.utils.im.CheckSumBuilder;
import com.movision.utils.propertiesLoader.MsgPropertiesLoader;
import com.movision.utils.propertiesLoader.PropertiesLoader;
import com.movision.utils.sms.SDKSendSms;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @Author zhuangyuhao
 * @Date 2017/2/3 18:03
 */
@Service
public class AppRegisterFacade {

    private static Logger log = LoggerFactory.getLogger(AppRegisterFacade.class);

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private ImFacade imFacade;

    @Autowired
    private CouponService couponService;

    @Autowired
    private WeixinGuangzhuService weixinGuangzhuService;

    @Autowired
    private UserService userService;

    @Autowired
    private CircleService circleService;

    @Autowired
    private BossUserService bossUserService;

    @Autowired
    private PointRecordFacade pointRecordFacade;

    @Autowired
    private ImDeviceService imDeviceService;

    @Autowired
    private AddressFacade addressFacade;

    @Autowired
    private ImUserService imUserService;

    /**
     * 1 校验登录用户信息：手机号+短信验证码
     * 2 若用户不存在，则新增用户信息；
     * 若用户存在，则更新用户token
     * 3 登录成功，则清除session中的验证码，
     *
     * @param member
     * @param validateinfo
     * @param session
     * @return {
     * token_detail:xxx,
     * token:yyy,
     * imuser:zzz
     * }
     */
    @Transactional
    public Map<String, Object> registeAppUserProcess(RegisterUser member, Validateinfo validateinfo, Session session) throws IOException {

        String phone = member.getPhone();                       //输入的手机号
        String verifyCode = validateinfo.getCheckCode();        //session中的验证码
        String mobileCheckCode = member.getMobileCheckCode();   //输入的验证码
        if (verifyCode != null) {
            Date currentTime = new Date();
            Date sendSMStime = DateUtils.date2Sub(DateUtils.str2Date(validateinfo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"), 12, 10);
            //校验是否在短信验证码有效期内
            if (currentTime.before(sendSMStime)) {
                log.debug("mobile verifyCode == " + mobileCheckCode);
                //比较服务器端session中的验证码和App端输入的验证码
                if (validateinfo.getCheckCode().equalsIgnoreCase(mobileCheckCode)) {
                    //1 生成token
                    UsernamePasswordToken newToken = new UsernamePasswordToken(phone, verifyCode.toCharArray());
                    Map<String, Object> result = new HashedMap();
                    //2 注册用户/修改用户信息
                    Gson gson = new Gson();
                    String json = gson.toJson(newToken);
                    member.setToken(json);

                    int userid = 0;
                    User user = userFacade.queryUserByPhone(phone);
                    if (null != user) {

                        //2.1 存在该用户,修改用户信息：token和设备号
                        this.updateAppRegisterUser(member);
                        userid = user.getId();

                    } else {
                        //2.1 手机号不存在,则新增用户信息
                        userid = this.registerMember(member);
                        //邀请成功查询出邀请人的openid给yw_weixin_guangzhu表的转盘次数+1
                        /** String openid = weixinGuangzhuService.selectOpenid(userid);
                        if (openid != null) {
                            //加1
                            weixinGuangzhuService.updateC(openid);
                         }*/
                        //2.2 增加新用户注册积分流水
                        pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.new_user_register.getCode(), PointConstant.POINT.new_user_register.getCode(), userid);
                        //2.3 增加绑定手机号积分流水
                        pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.binding_phone.getCode(), PointConstant.POINT.binding_phone.getCode(), userid);
                    }
                    log.info("【获取userid】:" + userid);

                    //3 如果用户当前手机号有领取过H5页面分享的优惠券，那么不管新老用户统一将优惠券临时表yw_coupon_temp中的优惠券信息全部放入优惠券正式表yw_coupon中
                    this.processCoupon(phone, userid);

                    //4 判断该userid是否存在一个im用户，若不存在，则注册im用户;若存在，则查询
                    this.getImuserForReturn(result, userid);

                    //5 登录成功则清除session中验证码的信息
                    session.removeAttribute("r" + validateinfo.getAccount());

                    //6 返回token
                    result.put("token_detail", newToken);
                    result.put("token", json);
                    return result;

                } else {
                    //不需要清除验证码
                    throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, "手机验证码不正确");
                }

            } else {
                //超过短信验证码有效期，则清除session信息
                session.removeAttribute("r" + validateinfo.getAccount());
                throw new BusinessException(MsgCodeConstant.member_mcode_sms_timeout, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_sms_timeout)));
            }

        } else {
            throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_mobile_validate_error)));
        }
    }

    @Transactional
    public void bindPhoneProcess(String phone, String code, Validateinfo validateinfo, Session session) throws IOException {
        //session中缓存的短信验证码
        String verifyCode = validateinfo.getCheckCode();
        if (verifyCode != null) {

            Date currentTime = new Date();
            Date sendSMStime = DateUtils.date2Sub(DateUtils.str2Date(validateinfo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"), 12, 10);
            //校验是否在短信验证码有效期内
            if (currentTime.before(sendSMStime)) {

                log.debug("mobile verifyCode == " + code);
                //比较服务器端session中的验证码和App端输入的验证码
                if (validateinfo.getCheckCode().equalsIgnoreCase(code)) {

                    int userid = ShiroUtil.getAppUserID();
                    log.info("【获取userid】:" + userid);

                    User user = userService.selectByPrimaryKey(userid);
                    if (null != user) {
                        //1 增加绑定手机号积分流水
                        pointRecordFacade.addPointRecordOnly(PointConstant.POINT_TYPE.binding_phone.getCode(), user.getId());

                        int newPoint = user.getPoints() + PointConstant.POINT.binding_phone.getCode();
                        user.setPoints(newPoint);   //积分
                        user.setPhone(phone);   //手机号
                        //2 修改用户基本信息
                        userService.updateByPrimaryKeySelective(user);
                        //3 更新session用户信息
                        ShiroUtil.updateAppuserPhoneAndPoints(phone, newPoint);

                        //4 如果用户当前手机号有领取过H5页面分享的优惠券，那么不管新老用户统一将优惠券临时表yw_coupon_temp中的优惠券信息全部放入优惠券正式表yw_coupon中
                        this.processCoupon(phone, userid);

                        //5 登录成功则清除session中验证码的信息
                        session.removeAttribute("bind" + validateinfo.getAccount());

                    } else {
                        throw new BusinessException(MsgCodeConstant.NOT_EXIST_APP_ACCOUNT, "不存在该APP用户");
                    }
                } else {
                    //不需要清除验证码
                    throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, "手机验证码不正确");
                }
            } else {
                //超过短信验证码有效期，则清除session信息
                session.removeAttribute("bind" + validateinfo.getAccount());
                throw new BusinessException(MsgCodeConstant.member_mcode_sms_timeout, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_sms_timeout)));
            }
        } else {
            throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_mobile_validate_error)));
        }
    }

    /**
     * 重新绑定新的手机号
     *
     * @param phone
     * @param code
     * @param validateinfo
     * @param session
     * @throws IOException
     */
    @Transactional
    public void bindNewPhoneProcess(String phone, String code, Validateinfo validateinfo, Session session) throws IOException {
        //session中缓存的短信验证码
        String verifyCode = validateinfo.getCheckCode();
        if (verifyCode != null) {

            Date currentTime = new Date();
            Date sendSMStime = DateUtils.date2Sub(DateUtils.str2Date(validateinfo.getCreateTime(), "yyyy-MM-dd HH:mm:ss"), 12, 10);
            //校验是否在短信验证码有效期内
            if (currentTime.before(sendSMStime)) {

                log.debug("mobile verifyCode == " + code);
                //比较服务器端session中的验证码和App端输入的验证码
                if (validateinfo.getCheckCode().equalsIgnoreCase(code)) {
                    //1 账户数据中绑定手机号
                    int userid = ShiroUtil.getAppUserID();
                    User user = userService.selectByPrimaryKey(userid);
                    if (null != user) {

                        //1.1 修改原手机号对应的所有圈子的手机号
                        batchUpdatePhoneInCircle(phone, user);
                        //1.2 查看该手机号是否注册了boss用户，若有，则替换手机号
                        updatePhoneInBossuser(phone, user);
                        //1.3 修改appuser信息中的手机号
                        user.setPhone(phone);
                        userService.updateByPrimaryKeySelective(user);

                    } else {
                        throw new BusinessException(MsgCodeConstant.NOT_EXIST_APP_ACCOUNT, "不存在该APP用户");
                    }
                    log.info("【获取userid】:" + userid);

                    //2 如果用户当前手机号有领取过H5页面分享的优惠券，那么不管新老用户统一将优惠券临时表yw_coupon_temp中的优惠券信息全部放入优惠券正式表yw_coupon中
                    this.processCoupon(phone, userid);

                    //3 登录成功则清除session中验证码的信息
                    session.removeAttribute("bind" + validateinfo.getAccount());

                    //4 修改session中的appuser信息
                    ShiroUtil.updateAppuserPhone(phone);

                } else {
                    //不需要清除验证码
                    throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, "手机验证码不正确");
                }
            } else {
                //超过短信验证码有效期，则清除session信息
                session.removeAttribute("bind" + validateinfo.getAccount());
                throw new BusinessException(MsgCodeConstant.member_mcode_sms_timeout, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_sms_timeout)));
            }
        } else {
            throw new BusinessException(MsgCodeConstant.member_mcode_mobile_validate_error, MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.member_mcode_mobile_validate_error)));
        }
    }

    private void updatePhoneInBossuser(String phone, User user) {
        BossUser bossUser = bossUserService.queryAdminUserByPhone(user.getPhone());
        if (null != bossUser) {
            bossUser.setPhone(phone);
            bossUserService.updateUser(bossUser);
        }
    }

    private void batchUpdatePhoneInCircle(String phone, User user) {
        List<Circle> circleList = circleService.queryCircleByPhone(user.getPhone());
        log.info("修改的手机号涉及的圈子有：" + circleList.toString());
        if (ListUtil.isNotEmpty(circleList)) {
            Map map = new HashedMap();
            int len = circleList.size();
            int[] arr = new int[len];
            for (int i = 0; i < len; i++) {
                arr[i] = circleList.get(i).getId();
            }
            map.put("ids", arr);
            map.put("phone", phone);
            circleService.batchUpdatePhoneInCircle(map);
        }
    }

    /**
     * 判断该userid是否存在一个im用户，若不存在，则注册im用户
     *
     * @param result
     * @throws IOException
     */
    private void getImuserForReturn(Map<String, Object> result, int userid) throws IOException {

        Boolean isExistImUser = imFacade.isExistAPPImuser(userid);
        if (!isExistImUser) {
            //若不存在，则注册im用户
            User user = userService.selectByPrimaryKey(userid);

            ImUser imUser = new ImUser();
            imUser.setUserid(userid);
            imUser.setAccid(CheckSumBuilder.getAccid(String.valueOf(userid)));  //根据userid生成accid
            imUser.setName(user.getNickname()); //同步app用户的昵称
            imUser.setIcon(user.getPhoto());    //同步app用户的头像

            ImUser newImUser = imFacade.AddImUser(imUser);
            result.put("imuser", newImUser);
        } else {
            //若存在，则查询im用户
            result.put("imuser", imUserService.selectByUserid(userid, ImConstant.TYPE_APP));
        }
    }

    /**
     * 判断该设备号是否注册过im用户，若注册过，则返回该im信息，否则注册
     *
     * @param deviceid
     * @param response
     * @return
     * @throws IOException
     */
    public ImDevice registerImDevice(String deviceid, Response response) throws IOException {

        //判断是否存该设备号的accid
        Boolean isExistDevice = imDeviceService.isExistDevice(deviceid);

        if (!isExistDevice) {
            //若不存在，则根据设备号注册云信账号
            ImDevice imDevice = new ImDevice();
            imDevice.setDeviceid(deviceid); //设备号
            imDevice.setAccid(CheckSumBuilder.getAccid(String.valueOf(deviceid)));
            imDevice.setName(StrUtil.genNickNameByDevice());    //im用户默认名称：mofo_时间戳
            imDevice.setIcon(UserConstants.DEFAULT_APPUSER_PHOTO); //设置im用户默认头像
            //注册
            imFacade.registerImDeviceAndSave(imDevice);
            response.setMessage("绑定设备号成功");
        } else {
            response.setMessage("已经存在该设备号的注册记录");
        }
        return imDeviceService.selectByDevice(deviceid);
    }


    /**
     * 如果当前手机号在分享的H5页面领取过优惠券，那么不管新老用户统一在这里将优惠券临时表中的数据同步到优惠券正式表中
     *
     * @param phone
     * @param userid
     */
    @Transactional
    void processCoupon(String phone, int userid) {
        //首先检查当前手机号是否领取过优惠券
        List<CouponTemp> couponTempList = couponService.checkIsGetCoupon(phone);
        List<Coupon> couponList = new ArrayList<>();
        if (couponTempList.size() > 0) {
            //遍历替换phone为userid，放入List<Coupon>
            for (int i = 0; i < couponTempList.size(); i++) {
                CouponTemp couponTemp = couponTempList.get(i);
                Coupon coupon = new Coupon();
                coupon.setUserid(userid);
                coupon.setTitle(couponTemp.getTitle());
                coupon.setContent(couponTemp.getContent());
                coupon.setType(couponTemp.getType());
                if (null != couponTemp.getShopid()) {
                    coupon.setShopid(couponTemp.getShopid());
                }
                coupon.setStatue(couponTemp.getStatue());
                coupon.setBegintime(couponTemp.getBegintime());
                coupon.setEndtime(couponTemp.getEndtime());
                coupon.setIntime(couponTemp.getIntime());
                coupon.setTmoney(couponTemp.getTmoney());
                coupon.setUsemoney(couponTemp.getUsemoney());
                coupon.setIsdel(couponTemp.getIsdel());
                couponList.add(coupon);
            }

            //插入优惠券列表
            couponService.insertCouponList(couponList);
            //删除临时表中的优惠券领取记录
            couponService.delCouponTemp(phone);
        }
    }

    /**
     * 注册用户，新增用户信息
     *
     * @param member
     * @return 新注册的用户id
     */
    public int registerMember(RegisterUser member) {
        log.debug("注册会员");
        int memberId = 0;
        try {
            if (member != null) {
                String phone = member.getPhone();

                User user = new User();
                user.setNickname(StrUtil.genDefaultNickNameByTime()); //昵称
                user.setPhone(phone);   //手机号
                user.setToken(member.getToken());   //token
                user.setDeviceno(member.getDeviceno()); //设备号
                user.setPoints(35); //积分：注册+绑定手机
                user.setPhoto(UserConstants.DEFAULT_APPUSER_PHOTO); //默认头像
                user.setIntime(new Date()); //注册时间

                //若有邀请码，则记录相关的邀请码
                if (StringUtils.isNotBlank(member.getReferrals())) {
                    user.setReferrals(member.getReferrals());
                }
                //生成自己的邀请码
                user.setInvitecode(UUIDGenerator.gen6Uuid());

                memberId = userService.insertSelective(user);
            }
        } catch (Exception e) {
            log.error("register member error", e);
        }
        return memberId;
    }

    /**
     * 修改app用户token和设备号
     *
     * @param member
     */
    public void updateAppRegisterUser(RegisterUser member) {
        log.debug("修改会员token");
        try {
            if (member != null) {
                userFacade.updateAccount(member);
            }
        } catch (Exception e) {
            log.error("register member error", e);
        }
    }

    /**
     * 注册QQ账号
     * <p>
     * 步骤：
     * 1 生成新的token
     * 2 判断qq是否注册过
     * 若qq从未注册过：a 则注册qq账号; b 注册im用户，
     * 若qq已经注册过：更新用户记录中的token（token根据openid和设备号生成）
     * <p>
     * <p>
     * 下面是token的数据结构
     * token:{
     * username: qq,
     * password: openid+deviceno,   //这个是变化的
     * rememberme: false
     * }
     *
     * @param flag
     * @param account
     * @param openid
     * @param deviceno
     * @return
     * @throws IOException
     */
    public Map<String, Object> registerQQAccount(Integer flag, String account, String openid, String deviceno,
                                                 String url, String nickname, String sex) throws IOException {

        Map<String, Object> result = new HashedMap();
        //1 生成新的token
        UsernamePasswordToken newToken = new UsernamePasswordToken(account, (openid + deviceno).toCharArray());
        result.put("token_detail", newToken);
        Gson gson = new Gson();
        String tokenJson = gson.toJson(newToken);
        result.put("token", tokenJson);

        //2 判断是否存在这条qq/wx/wb用户记录
        User originUser = queryExistThirdAccountAppUser(flag, account);

        if (null == originUser) {
            //3.1 根据第三方账号注册APP账号
            int userid = registerThirdAccountAndReturnUserid(flag, account, deviceno, tokenJson, url, nickname, sex);

            //3.2 根据该新的userid注册im用户
            ImUser newImUser = registerNewImuser(userid, nickname, url);
            result.put("imuser", newImUser);

            //3.3 新用户注册需要添加积分记录
            pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.new_user_register.getCode(), PointConstant.POINT.new_user_register.getCode(), userid);

        } else {
            /**
             * 存在场景：用户之前在设备A上用QQ注册了APP账户，现在用户换了一个设备B，下载APP，进行QQ登录
             */
            //3.1 更新原来的token
            updateUserInfo(deviceno, tokenJson, originUser);
            //3.2 返回已存在的im账户信息
            result.put("imuser", imFacade.getImuser(originUser.getId()));
        }

        return result;
    }

    private void updateUserInfo(String deviceno, String tokenJson, User originUser) {
        originUser.setToken(tokenJson);
        originUser.setDeviceno(deviceno);
        userService.updateByPrimaryKeySelective(originUser);
    }

    /**
     * 注册新的im用户
     *
     * @param userid
     * @param nickname
     * @param url
     * @return
     * @throws IOException
     */
    private ImUser registerNewImuser(int userid, String nickname, String url) throws IOException {

        ImUser imUser = new ImUser();
        imUser.setUserid(userid);
        imUser.setAccid(CheckSumBuilder.getAccid(String.valueOf(userid)));
        imUser.setName(nickname);   //同步第三方账号的名称
        imUser.setIcon(url);    //同步第三方账号的头像

        return imFacade.AddImUser(imUser);
    }

    /**
     * 查询是否存在第三方账号的APP用户
     *
     * @param flag
     * @param account
     * @return
     */
    public User queryExistThirdAccountAppUser(Integer flag, String account) {
        Map map = new HashedMap();
        String key = null;
        if (flag == 1) {
            key = "qq";
        } else if (flag == 2) {
            key = "wx";
        } else {
            key = "wb";
        }
        map.put(key, account);
        return userService.selectUserByThirdAccount(map);
    }

    /**
     * 注册第三方账号的App用户并返回userid
     *
     * @param flag
     * @param account
     * @param deviceno
     * @param tokenJson
     * @return
     */
    private int registerThirdAccountAndReturnUserid(Integer flag, String account, String deviceno, String tokenJson,
                                                    String url, String nickname, String sex) {
        User newUser = new User();
        newUser.setToken(tokenJson);    //token
        setUserThirdAccount(flag, account, newUser);

        newUser.setPhoto(url);  //头像
        newUser.setSex(Integer.valueOf(sex));   //性别
        newUser.setDeviceno(deviceno);  //设备号
        newUser.setPoints(25);  //积分：注册25分
        newUser.setInvitecode(UUIDGenerator.gen6Uuid());    //自己的邀请码
        newUser.setIntime(new Date());  //注册时间

        /**
         * 此处要判断是否具有相同的昵称
         * 比如：某人的qq已经注册过，nickname = a,
         * 当他的微信的nickname也是a的时候，如果他用微信注册，则把它的nickname修改成mofo_xxxxxxxxxx
         */
        if (userService.queryIsExistSameNickname(nickname)) {
            newUser.setNickname(StrUtil.genDefaultNickNameByTime());    //昵称
        } else {
            newUser.setNickname(nickname);    //昵称
        }

        return userService.insertSelective(newUser);
    }

    public void setUserThirdAccount(Integer flag, String account, User newUser) {
        if (flag == 1) {
            newUser.setQq(account); //qq
        } else if (flag == 2) {
            newUser.setOpenid(account); //微信
        } else {
            newUser.setSina(account);   //微博
        }
    }

    /**
     * 发送短信
     *
     * @param mobile
     * @param verifyCode
     */
    public void sendSms(String mobile, String verifyCode) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("code", verifyCode);
        map.put("min", Constants.sms_time);
        Gson gson = new Gson();
        String json = gson.toJson(map);

        //发送短信服务
        SDKSendSms.sendSMS(mobile, json, PropertiesLoader.getValue("login_app_sms_template_code"));
    }

    /**
     * 把短信验证的信息放入缓存
     *
     * @param mobile
     * @param verifyCode
     * @param sessionPrefix
     */
    public void putValidationInfoToSession(String mobile, String verifyCode, String sessionPrefix) {
        Subject currentUser = SecurityUtils.getSubject();
        Session session = currentUser.getSession(true);
        Validateinfo info = new Validateinfo();
        info.setCreateTime(DateUtils.date2Str(new Date(), "yyyy-MM-dd HH:mm:ss"));
        info.setCheckCode(verifyCode);
        info.setAccount(mobile);
        session.setAttribute(sessionPrefix + mobile, info); //缓存短信验证信息
        session.setAttribute("phone", mobile); //缓存接收短信验证码的手机号
    }


    public void shiroLogin(Response response, Subject currentUser, UsernamePasswordToken token) {
        try {
            //登录，即身份验证 , 开始进入shiro的认证流程
            currentUser.login(token);

        } catch (UnknownAccountException e) {
            log.warn("用户名不存在");
            response.setCode(400);
            response.setMessage(MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.app_user_not_exist)));
            response.setMsgCode(MsgCodeConstant.app_user_not_exist);
        } catch (LockedAccountException e) {
            log.warn("帐户状态异常");
            response.setCode(400);
            response.setMessage(MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.app_account_status_error)));
            response.setMsgCode(MsgCodeConstant.app_account_status_error);
        } catch (AuthenticationException e) {
            log.warn("用户名或验证码错误");
            response.setCode(400);
            response.setMessage(MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.app_account_name_error)));
            response.setMsgCode(MsgCodeConstant.app_account_name_error);
        }
    }

    /**
     * 处理登录过程
     *
     * @param appToken
     * @param response
     * @param user
     * @param ip
     * @param longitude
     * @param latitude
     */
    public void handleLoginProcess(String appToken, Response response, User user, String ip, String longitude,
                                   String latitude, Integer source)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        //1 校验appToken和serverToken非空
        String serverToken = this.validateAppTokenAndServerToken(appToken, response, user);

        //2 检验appToken和serverToken是否相等
        if (serverToken.equalsIgnoreCase(appToken)) {

            Subject currentUser = SecurityUtils.getSubject();
            Gson gson = new Gson();
            UsernamePasswordToken token = gson.fromJson(appToken, UsernamePasswordToken.class);

            Map returnMap = new HashedMap();
            //3 开始进入shiro的认证流程，对应 ShiroRealm.doGetAuthenticationInfo
            shiroLogin(response, currentUser, token);

            /**
             *  4 若shiro获取身份验证信息通过，则进行下面操作
             */
            if (currentUser.isAuthenticated()) {

                //5 验证通过则在session中缓存登录用户信息
                Session session = currentUser.getSession();
                //6 清除session中的boss用户信息
                session.removeAttribute(SessionConstant.BOSS_USER);
                session.removeAttribute(SessionConstant.WECHAT_USER);
                ShiroRealm.ShiroUser appuser = (ShiroRealm.ShiroUser) currentUser.getPrincipal();
                log.debug("当前登录的LoginUser信息=" + appuser.toString());
                session.setAttribute(SessionConstant.APP_USER, appuser);
                //用户id
                int appuserid = appuser.getId();
                //7 返回用户是否是第一次登录（根据登录时间和注册时间的间隔判断，若间隔小于10秒，则认为是第一次登录，否则不是）
                Date loginTime = calculateIsFirstLoginApp(returnMap, appuserid);
                //8 登录验证成功后，更新用户信息
                updateLoginUserInfo(appuserid, longitude, latitude, ip, loginTime, source);
                //9 返回登录人的信息
                LoginUser loginuser = userFacade.getLoginuserByUserid(appuserid);
                ShiroRealm.ShiroUser shiroUser = ShiroUtil.getShiroUserFromLoginUser(loginuser);
                session.setAttribute(SessionConstant.APP_USER, shiroUser);
                if (null == shiroUser) {
                    response.setMsgCode(0);
                    response.setMessage("登录失败");
                    returnMap.put("authorized", false);
                } else {
                    response.setMsgCode(1);
                    response.setMessage("登录成功");
                    returnMap.put("authorized", true);
                    returnMap.put("user", shiroUser);
                }
                //10 返回登录的用户当天是否签到
                if (pointRecordFacade.signToday()) {
                    returnMap.put("isSign", 1);
                } else {
                    pointRecordFacade.addPointRecord(PointConstant.POINT_TYPE.sign.getCode(), appuserid);
                    returnMap.put("isSign", 0);
                }

                response.setData(returnMap);
            } else {
                token.clear();
            }
        } else {
            log.warn("appToken和serverToken不相等");
            response.setCode(400);
            response.setMessage("appToken和serverToken不相等");
            response.setMsgCode(MsgCodeConstant.app_token_not_equal_server_token);
        }
    }

    /**
     * 判断用户是否属于第一次登录app
     *
     * @param returnMap
     * @param appuserid
     * @return
     */
    private Date calculateIsFirstLoginApp(Map returnMap, int appuserid) {
        Date loginTime = new Date();
        Map param = new HashMap();
        param.put("userid", appuserid);
        param.put("loginTime", loginTime);
        Map intervalMap = userService.selectIntervalBetweenLoginAndRegiste(param);
        if (MapUtil.isEmpty(intervalMap)) {
            //不存在登录与注册间隔10秒的这个用户，则说明这个用户不是第一次登录
            returnMap.put("isFirstLogin", 0);
        } else {
            //说明这个用户是第一次登录
            returnMap.put("isFirstLogin", 1);
        }
        return loginTime;
    }

    /**
     * 登录成功后，更新用户的部分信息：
     * 登录时间， ip， ip所在城市，经度， 纬度
     *
     * @param appuserid
     * @param longitude
     * @param latitude
     * @param ip
     */
    private void updateLoginUserInfo(int appuserid, String longitude, String latitude, String ip, Date loginTime, Integer source)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {

        User u = new User();
        u.setId(appuserid);
        u.setLoginTime(loginTime);

        u.setIp(ip);    //登录的ip
        u.setLongitude(longitude);  //登录的经度
        u.setLatitude(latitude);    //登录的纬度
        //登录时获取当前用户所在城市的编码
        String ip_city = getIpcityWhenLogin(longitude, latitude, ip);

        u.setIp_city(ip_city);  //登录的ip城市code
        String area = userService.areaname(ip_city);//市
        u.setCity(area);
        //查询省的name
        String provice = userService.provicename(ip_city);
        u.setProvince(provice);
        u.setSource(source);    //记录用户来源渠道

        userService.updateLoginappuserInfo(u);
    }

    /**
     * 登录时获取当前用户所在城市的编码
     *
     * @param longitude
     * @param latitude
     * @param ip
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    private String getIpcityWhenLogin(String longitude, String latitude, String ip) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Map<String, Object> map = addressFacade.getAddressByLatAndLng(latitude, longitude);
        int flag = Integer.valueOf(String.valueOf(map.get("flag")));
        String ip_city = null;
        if (flag == 1) {
            //根据经纬度获取城市code
            ip_city = String.valueOf(map.get("citycode"));
            log.info("根据经纬度获取城市code=" + ip_city);
        } else {
            //根据ip获取城市code
            ip_city = IpUtil.getCitycode("ip=" + ip, "utf-8");
            log.info("根据ip获取城市code=" + ip_city);
        }
        //防止获取不到ipcity的情况
        if (StringUtils.isEmpty(ip_city)) {
            ip_city = "320100";    //默认南京市
            log.warn("根据经纬度和ip都获取不到ip_city, 只能赋予默认值 320100 南京市");
        }
        return ip_city;
    }


    /**
     * 校验app_token和Server_token是否都存在
     *
     * @param appToken
     * @param response
     * @param user
     * @return
     */
    private String validateAppTokenAndServerToken(String appToken, Response response, User user) {
        if (StringUtils.isEmpty(appToken)) {
            log.warn("app本地的token丢失");
            response.setCode(400);
            response.setMessage(MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.app_token_missing)));
            response.setMsgCode(MsgCodeConstant.app_token_missing);
        }

        String serverToken = user.getToken();
        if (StringUtils.isEmpty(serverToken)) {
            log.warn("服务器存储的token丢失");
            response.setCode(400);
            response.setMessage(MsgPropertiesLoader.getValue(String.valueOf(MsgCodeConstant.server_token_missing)));
            response.setMsgCode(MsgCodeConstant.server_token_missing);
        }
        return serverToken;
    }

    /**
     * 校验手机号
     *
     * @param phone
     * @return
     */
    public static String phones = PropertiesLoader.getValue("phone");
    public static String phonesfour = PropertiesLoader.getValue("phones");

    public static String verifyCellPhoneNumber(String phone) {
        String result = "手机号不存在";
        String subphone = phone.substring(0, 3);
        String subfor = phone.substring(0, 4);
        String[] pho = phones.split(",");
        String[] phos = phonesfour.split(",");
        for (int i = 0; i < pho.length; i++) {
            if (subphone.equals(pho[i])) {
                result = "手机号存在";
                return result;
            }
        }
        for (int i = 0; i < phos.length; i++) {
            if (subfor.equals(phos[i])) {
                result = "手机号存在";
                return result;
            }
        }

        return result;
    }


}

