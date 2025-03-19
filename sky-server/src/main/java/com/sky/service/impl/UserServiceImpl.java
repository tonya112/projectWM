package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.sky.constant.MessageConstant.LOGIN_FAILED;


@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    private static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {

        //调用微信服务接口获取openid
        String openid = getOpenId(userLoginDTO.getCode());

        //openid为空，登录失败，抛出异常
        if(openid == null || openid.isEmpty()){
            throw new LoginFailedException(LOGIN_FAILED);
        }
        //openid不为空，判断当前用户是否为新用户，如果是新用户，自动完成注册，否则直接登录成功
        //检查openid是否存在于数据库中
        User user = userMapper.getByOpenid(openid);

        if(user == null){
            //创建新用户
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();

            userMapper.insert(user);
        }

        //返回用户对象
        return user;
    }

    private String getOpenId(String accessCode){
        //调用微信服务接口获取openid
        Map<String, String> loginMap = new HashMap<>();
        loginMap.put("appid", weChatProperties.getAppid());
        loginMap.put("secret", weChatProperties.getSecret());
        loginMap.put("js_code", accessCode);
        loginMap.put("grant_type", "authorization_code");
        String response = HttpClientUtil.doGet(WX_LOGIN_URL, loginMap);
        JSONObject jsonObject = JSON.parseObject(response);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
