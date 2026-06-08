package com.milktea.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.milktea.common.ErrorCode;
import com.milktea.entity.Address;
import com.milktea.mapper.AddressMapper;
import com.milktea.exception.BusinessException;
import com.milktea.service.AddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private static final Logger logger = LoggerFactory.getLogger(AddressServiceImpl.class);
    private static final int MAX_ADDRESS_COUNT = 10;

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getUpdateTime));
    }

    @Override
    public Address getByIdAndUserId(Long id, Long userId) {
        Address address = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId));
        if (address == null) {
            throw new BusinessException(ErrorCode.B0020);
        }
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Address addAddress(Address address) {
        Long count = addressMapper.selectCount(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, address.getUserId()));
        if (count >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.B0021);
        }

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        } else {
            address.setIsDefault(0);
        }

        addressMapper.insert(address);
        logger.info("新增收货地址: userId={}, addressId={}", address.getUserId(), address.getId());
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Address updateAddress(Address address) {
        Address existing = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, address.getId())
                .eq(Address::getUserId, address.getUserId()));
        if (existing == null) {
            throw new BusinessException(ErrorCode.B0020);
        }

        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        }

        addressMapper.updateById(address);
        logger.info("更新收货地址: userId={}, addressId={}", address.getUserId(), address.getId());
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long id, Long userId) {
        Address existing = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(ErrorCode.B0020);
        }

        addressMapper.deleteById(id);
        logger.info("删除收货地址: userId={}, addressId={}", userId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        Address existing = addressMapper.selectOne(new LambdaQueryWrapper<Address>()
                .eq(Address::getId, id)
                .eq(Address::getUserId, userId));
        if (existing == null) {
            throw new BusinessException(ErrorCode.B0020);
        }

        clearDefault(userId);

        Address update = new Address();
        update.setId(id);
        update.setIsDefault(1);
        addressMapper.updateById(update);
        logger.info("设置默认地址: userId={}, addressId={}", userId, id);
    }

    private void clearDefault(Long userId) {
        Address clear = new Address();
        clear.setIsDefault(0);
        addressMapper.update(clear, new LambdaUpdateWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1));
    }
}
