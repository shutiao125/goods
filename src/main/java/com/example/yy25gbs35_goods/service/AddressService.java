package com.example.yy25gbs35_goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yy25gbs35_goods.goods.Address;

import java.util.List;

public interface AddressService extends IService<Address> {

    List<Address> getAddressByUserId(Long userId);

    Address getDefaultAddress(Long userId);

    boolean setDefaultAddress(Long addressId, Long userId);
}
