package com.milktea.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.milktea.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT COUNT(*) FROM users")
    Long countAllUsers();
}
