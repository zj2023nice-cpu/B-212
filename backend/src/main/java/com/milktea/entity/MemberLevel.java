package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("member_levels")
public class MemberLevel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer level;
    private Integer totalPoints;
    private Integer currentPoints;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
