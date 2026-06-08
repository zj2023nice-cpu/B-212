package com.milktea.controller;

import com.milktea.common.Result;
import com.milktea.entity.Address;
import com.milktea.service.AddressService;
import com.milktea.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    private Long getCurrentUserId() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getByUsername(username).getId();
    }

    @GetMapping
    public Result<List<Address>> list() {
        Long userId = getCurrentUserId();
        return Result.success(addressService.listByUserId(userId));
    }

    @GetMapping("/{id}")
    public Result<Address> get(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return Result.success(addressService.getByIdAndUserId(id, userId));
    }

    @PostMapping
    public Result<Address> add(@RequestBody Address address) {
        Long userId = getCurrentUserId();
        address.setUserId(userId);
        return Result.success(addressService.addAddress(address));
    }

    @PutMapping("/{id}")
    public Result<Address> update(@PathVariable Long id, @RequestBody Address address) {
        Long userId = getCurrentUserId();
        address.setId(id);
        address.setUserId(userId);
        return Result.success(addressService.updateAddress(address));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        addressService.deleteAddress(id, userId);
        return Result.success("删除成功");
    }

    @PutMapping("/{id}/default")
    public Result<String> setDefault(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        addressService.setDefault(id, userId);
        return Result.success("设置成功");
    }
}
