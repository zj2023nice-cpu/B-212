package com.milktea.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class OrderExportVO {
    @ExcelProperty("订单编号")
    @ColumnWidth(35)
    private String orderSn;

    @ExcelProperty("下单时间")
    @ColumnWidth(22)
    private String createTime;

    @ExcelProperty("用户名")
    @ColumnWidth(15)
    private String username;

    @ExcelProperty("用户昵称")
    @ColumnWidth(15)
    private String nickname;

    @ExcelProperty("用户手机号")
    @ColumnWidth(15)
    private String phone;

    @ExcelProperty("商品明细")
    @ColumnWidth(40)
    private String productDetail;

    @ExcelProperty("总金额")
    @ColumnWidth(12)
    private String totalAmount;

    @ExcelProperty("优惠金额")
    @ColumnWidth(12)
    private String discountAmount;

    @ExcelProperty("实付金额")
    @ColumnWidth(12)
    private String payAmount;

    @ExcelProperty("订单状态")
    @ColumnWidth(12)
    private String statusText;

    @ExcelProperty("收货联系人")
    @ColumnWidth(15)
    private String contactName;

    @ExcelProperty("收货手机号")
    @ColumnWidth(15)
    private String contactPhone;

    @ExcelProperty("收货地址")
    @ColumnWidth(40)
    private String addressFull;

    @ExcelProperty("备注")
    @ColumnWidth(25)
    private String remark;
}
