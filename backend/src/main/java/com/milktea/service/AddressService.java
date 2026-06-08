package com.milktea.service;

import com.milktea.entity.Address;
import java.util.List;

public interface AddressService {
    List<Address> listByUserId(Long userId);
    Address getByIdAndUserId(Long id, Long userId);
    Address addAddress(Address address);
    Address updateAddress(Address address);
    void deleteAddress(Long id, Long userId);
    void setDefault(Long id, Long userId);
}
