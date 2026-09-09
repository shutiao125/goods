package com.example.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.goods.goods.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    List<Address> getAddressByUserId(Long userId);

    Address getDefaultAddress(Long userId);

    boolean setDefaultAddress(Long addressId, Long userId);
}
