package com.movision.mybatis.coupon.service;

import com.movision.mybatis.coupon.entity.Coupon;
import com.movision.mybatis.coupon.mapper.CouponMapper;
import com.movision.mybatis.couponDistributeManage.entity.CouponDistributeManage;
import com.movision.mybatis.couponDistributeManage.entity.CouponDistributeManageVo;
import com.movision.mybatis.couponDistributeManage.mapper.CouponDistributeManageMapper;
import com.movision.utils.pagination.model.Paging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author shuxf
 * @Date 2017/2/18 11:11
 */
@Service
@Transactional
public class CouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private CouponDistributeManageMapper couponDistributeManageMapper;

    private Logger log = LoggerFactory.getLogger(CouponService.class);

    public List<Coupon> queryCanUseCoupon(int userid) {
        try {
            log.info("根据用户id查询用户名下所有可用优惠券列表");
            return couponMapper.queryCanUseCoupon(userid);
        } catch (Exception e) {
            log.error("根据用户id查询用户名下所有可用优惠券列表失败");
            throw e;
        }
    }

    public List<Coupon> queryBeUsedCoupon(int userid) {
        try {
            log.info("根据用户id查询用户名下所有已用优惠券列表");
            return couponMapper.queryBeUsedCoupon(userid);
        } catch (Exception e) {
            log.error("根据用户id查询用户名下所有已用优惠券列表失败");
            throw e;
        }
    }

    public List<Coupon> queryHaveOverdueCoupon(int userid) {
        try {
            log.info("根据用户id查询用户名下所有过期失效的优惠券列表");
            return couponMapper.queryHaveOverdueCoupon(userid);
        } catch (Exception e) {
            log.error("根据用户id查询用户名下所有过期失效的优惠券列表失败");
            throw e;
        }
    }

    public List<CouponDistributeManageVo> queryCurReceiveCoupon() {
        try {
            log.info("查询当前可领取的优惠券列表");
            return couponDistributeManageMapper.queryCurReceiveCoupon();
        } catch (Exception e) {
            log.error("查询当前可领取的优惠券列表失败");
            throw e;
        }
    }

    public int checkCoupon(int id) {
        try {
            log.info("检查是否有合法的可领取优惠券");
            return couponDistributeManageMapper.checkCoupon(id);
        } catch (Exception e) {
            log.error("检查是否有合法的可领取优惠券失败");
            throw e;
        }
    }

    @Transactional
    public void getCoupon(Map<String, Object> parammap) throws Exception {
        try {
            log.info("用户领取优惠券");
            //首先扣减系统优惠券的剩余张数和已领张数
            couponDistributeManageMapper.deductCoupon(parammap);

            //给用户表里增加一条优惠券
            CouponDistributeManage couponDistributeManage = couponDistributeManageMapper.selectByPrimaryKey((int) parammap.get("id"));
            Coupon coupon = new Coupon();
            coupon.setUserid((int) parammap.get("userid"));
            coupon.setTitle(couponDistributeManage.getTitle());
            coupon.setContent(couponDistributeManage.getContent());
            coupon.setType(couponDistributeManage.getScope());
            coupon.setShopid(couponDistributeManage.getShopid());
            coupon.setCategory(-1);
            coupon.setStatue(0);
            coupon.setBegintime(couponDistributeManage.getStartdate());
            coupon.setEndtime(couponDistributeManage.getEnddate());
            coupon.setIntime(new Date());
            coupon.setTmoney(couponDistributeManage.getAmount());
            coupon.setUsemoney(couponDistributeManage.getFullamount());
            coupon.setIsdel(0);
            couponMapper.insertSelective(coupon);

            //记录一条用户的领取记录
            parammap.put("intime", new Date());
            couponDistributeManageMapper.insertGetRecord(parammap);

//            throw new RuntimeException("抛出个运行时异常！");//经测试抛运行时异常，事务能够正常全部回滚
        } catch (Exception e) {
            log.error("用户领取优惠券失败");
            throw e;
        }
    }

    public List<Coupon> findAllMyCouponList(Paging<Coupon> paging, Map map) {
        try {
            log.info("查询我的优惠券列表");
            return couponMapper.findAllMyCouponList(paging.getRowBounds(), map);
        } catch (Exception e) {
            log.error("查询我的优惠券列表失败", e);
            throw e;
        }
    }
}
