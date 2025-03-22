package com.sky.controller.user;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.properties.JwtProperties;
import com.sky.service.UserService;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import com.sky.websocket.WebSocketServer;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.sky.result.Result;
import com.sky.entity.User;

import java.util.HashMap;
import java.util.Map;

import static com.sky.constant.JwtClaimsConstant.USER_ID;

@Api(tags = "用户相关接口")
@RestController
@RequestMapping("/user/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtProperties jwtProperties;


    @RequestMapping("/login")
    @PostMapping
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("登入用户 {}", userLoginDTO.getCode());
        User user = userService.wxLogin(userLoginDTO);

        //生产JWY
        Map<String, Object> claims = new HashMap<>();
        claims.put(USER_ID, user.getId());
        String jwt = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(jwt)
                .build();

        return Result.success(userLoginVO);
    }


}
