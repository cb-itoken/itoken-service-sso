package com.cb.itoken.service.sso.controller;

import com.cb.itoken.common.domain.TbSysUser;
import com.cb.itoken.common.utils.CookieUtils;
import com.cb.itoken.common.utils.MapperUtils;
import com.cb.itoken.common.web.constants.WebConstants;
import com.cb.itoken.service.sso.service.LoginService;
import com.cb.itoken.service.sso.service.consumer.RedisService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private LoginService loginService;

    /**
     * 跳转登录页
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.GET)
    public String login(@RequestParam(required = false) String url,
                        HttpServletRequest request, Model model){
        TbSysUser tbSysUser = null;
        String token = CookieUtils.getCookieValue(request, WebConstants.SESSION_TOKEN);

        // token 不为空可能已登录
        if(StringUtils.isNotBlank(token)){
            String loginCode = redisService.get(token);
            if(StringUtils.isNotBlank(loginCode)){
                String json = redisService.get(loginCode);
                if(StringUtils.isNotBlank(json)){
                    try {
                        tbSysUser = MapperUtils.json2pojo(json, TbSysUser.class);
                        // 已登录
                        if(tbSysUser != null){
                            if(StringUtils.isNotBlank(url)){
                                return "redirect:" + url;
                            }
                        }
                        // 将登录信息传到登录页
                        model.addAttribute("tbSysUser", tbSysUser);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        if(StringUtils.isNotBlank(url)){
            model.addAttribute("url", url);
        }

        return "login";
    }

    /**
     * 登录业务
     * @param loginCode
     * @param password
     * @return
     */
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(@RequestParam(required = true) String loginCode,
                        @RequestParam(required = true) String password,
                        @RequestParam(required = false) String url,
                        HttpServletRequest request, HttpServletResponse response,
                        RedirectAttributes redirectAttributes){
        TbSysUser tbSysUser = loginService.login(loginCode, password);

        // 登录失败
        if(tbSysUser == null){
            redirectAttributes.addFlashAttribute("message", "用户名或密码错误, 请重新输入");
        }

        // 登录成功
        else {
            String token = UUID.randomUUID().toString();
            String result = redisService.put(token, loginCode, 60 * 60 * 24);

            // 将 Token 放入 Cookie (放入redis成功且没有被熔断的情况下)
            if(StringUtils.isNotBlank(result) && "ok".equals(result)){
                CookieUtils.setCookie(request, response, WebConstants.SESSION_TOKEN, token, 60 * 60 * 24);
                if (StringUtils.isNotBlank(url)) {
                    return "redirect:" + url;
                }
            }

            // 熔断处理
            else {
                redirectAttributes.addFlashAttribute("message", "服务器异常, 请稍后再试");
            }
        }

        // 如果使用return "redirect:login", 就会将common-web里配置的常量拦截器写入地址
        // 如果使用response.sendRedirect("login"), 则不会出现这种情况
        return "redirect:login";
    }

    /**
     * 注销
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(required = false) String url, Model model){
        CookieUtils.deleteCookie(request, response, WebConstants.SESSION_TOKEN);
        return login(url, request, model);
    }
}
