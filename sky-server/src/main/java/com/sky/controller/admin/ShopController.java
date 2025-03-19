package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ShopService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "店铺相关接口")
@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
public class ShopController {

    @Autowired
    private ShopService shopService;

    @ApiOperation("修改店铺状态")
    @PutMapping("/{status}")
    public Result setStatus(@PathVariable Integer status) {
        log.info("修改店铺状态:{}", status == 1 ? "开启" : "关闭");
        shopService.setStatus(status);

        return Result.success();
    }
    @ApiOperation("查询店铺状态")
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        Integer status = shopService.getStatus();
        log.info("查询店铺状态:{}", status == 1 ? "开启" : "关闭");
        return Result.success(status);
    }
}
