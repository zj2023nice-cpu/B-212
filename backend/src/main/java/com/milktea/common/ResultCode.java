package com.milktea.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    
    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    ACCEPTED(202, "请求已接受"),
    NO_CONTENT(204, "无内容"),

    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权，请登录"),
    FORBIDDEN(403, "没有权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    REQUEST_TIMEOUT(408, "请求超时"),
    CONFLICT(409, "资源冲突"),
    GONE(410, "资源已删除"),
    UNPROCESSABLE_ENTITY(422, "请求参数校验失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),

    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    NOT_IMPLEMENTED(501, "接口未实现"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_ACCOUNT_DISABLED(1004, "账户已被禁用"),
    USER_ACCOUNT_LOCKED(1005, "账户已被锁定"),
    USER_NOT_LOGGED_IN(1006, "用户未登录"),
    USER_SESSION_EXPIRED(1007, "登录已过期，请重新登录"),
    USER_PERMISSION_DENIED(1008, "权限不足"),

    PRODUCT_NOT_FOUND(2001, "商品不存在"),
    PRODUCT_ALREADY_EXISTS(2002, "商品已存在"),
    PRODUCT_STOCK_INSUFFICIENT(2003, "商品库存不足"),
    PRODUCT_OFF_SHELF(2004, "商品已下架"),
    PRODUCT_INVALID(2005, "商品信息无效"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ALREADY_PAID(3002, "订单已支付"),
    ORDER_ALREADY_CANCELLED(3003, "订单已取消"),
    ORDER_STATUS_INVALID(3004, "订单状态无效"),
    ORDER_CREATE_FAILED(3005, "订单创建失败"),
    ORDER_PAY_FAILED(3006, "订单支付失败"),
    ORDER_CANCEL_FAILED(3007, "订单取消失败"),
    ORDER_NOT_AUTHORIZED(3008, "无权操作此订单"),

    CART_EMPTY(4001, "购物车为空"),
    CART_ITEM_NOT_FOUND(4002, "购物车项不存在"),
    CART_ADD_FAILED(4003, "添加购物车失败"),
    CART_UPDATE_FAILED(4004, "更新购物车失败"),

    CATEGORY_NOT_FOUND(5001, "分类不存在"),
    CATEGORY_ALREADY_EXISTS(5002, "分类已存在"),
    CATEGORY_HAS_PRODUCTS(5003, "分类下存在商品，无法删除"),

    TOKEN_INVALID(6001, "Token无效"),
    TOKEN_EXPIRED(6002, "Token已过期"),
    TOKEN_MISSING(6003, "Token缺失"),

    PARAM_MISSING(7001, "参数缺失"),
    PARAM_TYPE_ERROR(7002, "参数类型错误"),
    PARAM_FORMAT_ERROR(7003, "参数格式错误"),
    PARAM_RANGE_ERROR(7004, "参数范围错误"),

    FILE_UPLOAD_FAILED(8001, "文件上传失败"),
    FILE_TYPE_INVALID(8002, "文件类型无效"),
    FILE_SIZE_EXCEEDED(8003, "文件大小超出限制"),
    FILE_NOT_FOUND(8004, "文件不存在"),

    DATABASE_ERROR(9001, "数据库操作失败"),
    DUPLICATE_KEY_ERROR(9002, "数据已存在"),
    DATA_INTEGRITY_ERROR(9003, "数据完整性约束失败"),
    OPTIMISTIC_LOCK_ERROR(9004, "乐观锁冲突，请重试"),

    COUPON_NOT_FOUND(11001, "优惠券不存在"),
    COUPON_ALREADY_CLAIMED(11002, "优惠券已领取"),
    COUPON_STOCK_EXHAUSTED(11003, "优惠券已领完"),
    COUPON_EXPIRED(11004, "优惠券已过期"),
    COUPON_NOT_AVAILABLE(11005, "优惠券不可用"),
    COUPON_THRESHOLD_NOT_MET(11006, "未满足优惠券使用门槛"),
    COUPON_ALREADY_USED(11007, "优惠券已使用"),
    COUPON_INVALID(11008, "优惠券无效"),

    ADDRESS_NOT_FOUND(12001, "收货地址不存在"),
    ADDRESS_LIMIT_EXCEEDED(12002, "收货地址数量已达上限"),
    ADDRESS_NOT_AUTHORIZED(12003, "无权操作此地址"),

    PROMOTION_NOT_FOUND(14001, "促销活动不存在"),
    PROMOTION_DISABLED(14002, "促销活动已禁用"),
    PROMOTION_EXPIRED(14003, "促销活动已过期"),
    PROMOTION_NOT_STARTED(14004, "促销活动未开始"),
    PROMOTION_THRESHOLD_NOT_MET(14005, "未满足促销活动门槛"),

    FAVORITE_ALREADY_EXISTS(13001, "已收藏该商品"),
    FAVORITE_NOT_FOUND(13002, "收藏记录不存在"),

    THIRD_PARTY_ERROR(10001, "第三方服务调用失败"),
    SMS_SEND_FAILED(10002, "短信发送失败"),
    EMAIL_SEND_FAILED(10003, "邮件发送失败"),
    PAYMENT_FAILED(10004, "支付失败"),
    PAYMENT_TIMEOUT(10005, "支付超时");

    private final Integer code;
    private final String message;

    public static ResultCode fromCode(Integer code) {
        for (ResultCode resultCode : ResultCode.values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}
