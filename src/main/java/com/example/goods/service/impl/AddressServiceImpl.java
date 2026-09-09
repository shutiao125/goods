package com.example.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.goods.goods.Address;
import com.example.goods.mapper.AddressMapper;
import com.example.goods.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressServiceImpl extends ServiceImpl<AddressMapper, Address> implements AddressService {

    @Autowired
    private AddressMapper addressMapper;

    @Override
    public List<Address> getAddressByUserId(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getCreatedAt);
        return addressMapper.selectList(wrapper);
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .last("LIMIT 1");
        return addressMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public boolean setDefaultAddress(Long addressId, Long userId) {
        LambdaUpdateWrapper<Address> clearDefault = new LambdaUpdateWrapper<>();
        clearDefault.eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 0);
        addressMapper.update(null, clearDefault);

        LambdaUpdateWrapper<Address> setNewDefault = new LambdaUpdateWrapper<>();
        setNewDefault.eq(Address::getId, addressId)
                .eq(Address::getUserId, userId)
                .set(Address::getIsDefault, 1);
        return addressMapper.update(null, setNewDefault) > 0;
    }
}
