package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "店铺相关接口")
@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = shopService.getStatus();
        log.info("查询店铺状态:{}", status == 1 ? "开启" : "关闭");
        return Result.success(status);
    }
}
