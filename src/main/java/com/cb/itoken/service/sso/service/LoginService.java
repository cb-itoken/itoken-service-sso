package com.cb.itoken.service.sso.service;

import com.cb.itoken.common.domain.TbSysUser;

/**
 * @Author:congbing
 * @Description 登录业务
 * @Date: 19:38 2019/1/23
 **/
public interface LoginService {

    /**
     * 登录
     * @param loginCode
     * @param plantPassword
     * @return
     */
    public TbSysUser login(String loginCode, String plantPassword);
}
