package com.milktea.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("export_audit_logs")
public class ExportAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operatorName;
    private String exportType;
    private String filterStartDate;
    private String filterEndDate;
    private String filterStatus;
    private Integer exportCount;
    private LocalDateTime createTime;
}
