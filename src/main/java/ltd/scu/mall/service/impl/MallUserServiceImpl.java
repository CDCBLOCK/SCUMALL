/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本系统已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2020 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package ltd.scu.mall.service.impl;

import jakarta.servlet.http.HttpSession;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.common.ServiceResultEnum;
import ltd.scu.mall.controller.vo.MallUserVO;
import ltd.scu.mall.dao.MallUserMapper;
import ltd.scu.mall.dao.CouponMapper;
import ltd.scu.mall.dao.MallUserCouponRecordMapper;
import ltd.scu.mall.entity.MallCoupon;
import ltd.scu.mall.entity.MallUser;
import ltd.scu.mall.entity.MallUserCouponRecord;
import ltd.scu.mall.exception.NewBeeMallException;
import ltd.scu.mall.service.MallUserService;
import ltd.scu.mall.util.*;
import ltd.scu.mall.util.MD5Util;
import ltd.scu.mall.util.MallUtils;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MallUserServiceImpl implements MallUserService {

    @Autowired
    private MallUserMapper mallUserMapper;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private MallUserCouponRecordMapper mallUserCouponRecordMapper;

    @Override
    public PageResult getNewBeeMallUsersPage(PageQueryUtil pageUtil) {
        List<MallUser> mallUsers = mallUserMapper.findMallUserList(pageUtil);
        int total = mallUserMapper.getTotalMallUsers(pageUtil);
        return new PageResult(mallUsers, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String register(String loginName, String password) {
        if (mallUserMapper.selectByLoginName(loginName) != null) {
            return ServiceResultEnum.SAME_LOGIN_NAME_EXIST.getResult();
        }
        MallUser registerUser = new MallUser();
        registerUser.setLoginName(loginName);
        registerUser.setNickName(loginName);

        String passwordMD5 = MD5Util.MD5Encode(password, Constants.UTF_ENCODING);
        registerUser.setPasswordMd5(passwordMD5);
        if (mallUserMapper.insertSelective(registerUser) <= 0) {
            return ServiceResultEnum.DB_ERROR.getResult();
        }
        // 添加注册赠券
        List<MallCoupon> mallMallCoupons = couponMapper.selectAvailableGiveCoupon();
        for (MallCoupon mallCoupon : mallMallCoupons) {
            MallUserCouponRecord couponUser = new MallUserCouponRecord();
            couponUser.setUserId(registerUser.getUserId());
            couponUser.setCouponId(mallCoupon.getCouponId());
            mallUserCouponRecordMapper.insertSelective(couponUser);
        }
        return ServiceResultEnum.SUCCESS.getResult();
    }

    @Override
    public String login(String loginName, String passwordMD5, HttpSession httpSession) {
        MallUser user = mallUserMapper.selectByLoginNameAndPasswd(loginName, passwordMD5);

        if (user.getUserState() != null && user.getUserState().intValue() == 1) {
            // 用户已经登录，返回已登录的标识
            return ServiceResultEnum.LOGIN_ALREADY.getResult();
        }

        if (user != null && httpSession != null) {

            if (user.getLockedFlag() == 1) {
                return ServiceResultEnum.LOGIN_USER_LOCKED.getResult();
            }

            // 昵称太长 影响页面展示
            if (user.getNickName() != null && user.getNickName().length() > 7) {
                String tempNickName = user.getNickName().substring(0, 7) + "..";
                user.setNickName(tempNickName);
            }

            user.setUserState(1);

            //用来修饰 user里面的一些值，提取相对重要的信息保存到新的对象VO中
            MallUserVO mallUserVO = new MallUserVO();
            BeanUtil.copyProperties(user, mallUserVO);


            mallUserMapper.updateByPrimaryKeySelective(user);

            // 设置购物车中的数量
            httpSession.setAttribute(Constants.MALL_USER_SESSION_KEY, mallUserVO);
            return ServiceResultEnum.SUCCESS.getResult();
        }
        return ServiceResultEnum.LOGIN_ERROR.getResult();
    }

    @Override
    public MallUserVO updateUserInfo(MallUser mallUser, HttpSession httpSession) {
        MallUserVO userTemp = (MallUserVO) httpSession.getAttribute(Constants.MALL_USER_SESSION_KEY);
        MallUser userFromDB = mallUserMapper.selectByPrimaryKey(userTemp.getUserId());
        if (userFromDB == null) {
            return null;
        }

        if (StringUtils.equals(mallUser.getNickName(), userFromDB.getNickName())
                && StringUtils.equals(mallUser.getAddress(), userFromDB.getAddress())
                && StringUtils.equals(mallUser.getIntroduceSign(), userFromDB.getIntroduceSign())) {
            throw new NewBeeMallException("个人信息无变更！");
        }

        if (StringUtils.equals(mallUser.getAddress(), userFromDB.getAddress())
                && mallUser.getNickName() == null
                && mallUser.getIntroduceSign() == null) {
            throw new NewBeeMallException("个人信息无变更！");
        }

        if (!StringUtils.isEmpty(mallUser.getNickName())) {
            userFromDB.setNickName(MallUtils.cleanString(mallUser.getNickName()));
        }
        if (!StringUtils.isEmpty(mallUser.getAddress())) {
            userFromDB.setAddress(MallUtils.cleanString(mallUser.getAddress()));
        }
        if (!StringUtils.isEmpty(mallUser.getIntroduceSign())) {
            userFromDB.setIntroduceSign(MallUtils.cleanString(mallUser.getIntroduceSign()));
        }
        if (mallUser.getUserState() != null) {
            userFromDB.setUserState(mallUser.getUserState());
        }

        if (mallUserMapper.updateByPrimaryKeySelective(userFromDB) > 0) {
            MallUserVO mallUserVO = new MallUserVO();
            BeanUtil.copyProperties(userFromDB, mallUserVO);
            httpSession.setAttribute(Constants.MALL_USER_SESSION_KEY, mallUserVO);
            return mallUserVO;
        }
        return null;
    }

    @Override
    public Boolean lockUsers(Integer[] ids, int lockStatus) {
        if (ids.length < 1) {
            return false;
        }
        return mallUserMapper.lockUserBatch(ids, lockStatus) > 0;
    }

    @Override
    public String logoutuser(Long userId) {
        // 根据用户唯一标识从数据库中获取用户信息
        MallUser user = mallUserMapper.selectByPrimaryKey(userId);

        if (user != null) {
            // 将用户的状态置为 0（未登录）
            user.setUserState(0);
            mallUserMapper.updateByPrimaryKeySelective(user);
        }
        // 返回登出成功的消息或其他适当的结果
        return "Logout successful";
    }

    @Override
    public int searchStatusByLoginName(String name) {
        // 根据用户唯一标识从数据库中获取用户信息
        int result = mallUserMapper.searchStatusByLoginName(name);

        return result;
    }
}
