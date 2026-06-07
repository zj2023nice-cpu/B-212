export const ResultCode = {
  SUCCESS: 200,
  CREATED: 201,
  ACCEPTED: 202,
  NO_CONTENT: 204,

  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  METHOD_NOT_ALLOWED: 405,
  REQUEST_TIMEOUT: 408,
  CONFLICT: 409,
  GONE: 410,
  UNPROCESSABLE_ENTITY: 422,
  TOO_MANY_REQUESTS: 429,

  INTERNAL_SERVER_ERROR: 500,
  NOT_IMPLEMENTED: 501,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504,

  USER_NOT_FOUND: 1001,
  USER_ALREADY_EXISTS: 1002,
  USER_PASSWORD_ERROR: 1003,
  USER_ACCOUNT_DISABLED: 1004,
  USER_ACCOUNT_LOCKED: 1005,
  USER_NOT_LOGGED_IN: 1006,
  USER_SESSION_EXPIRED: 1007,
  USER_PERMISSION_DENIED: 1008,

  PRODUCT_NOT_FOUND: 2001,
  PRODUCT_ALREADY_EXISTS: 2002,
  PRODUCT_STOCK_INSUFFICIENT: 2003,
  PRODUCT_OFF_SHELF: 2004,
  PRODUCT_INVALID: 2005,

  ORDER_NOT_FOUND: 3001,
  ORDER_ALREADY_PAID: 3002,
  ORDER_ALREADY_CANCELLED: 3003,
  ORDER_STATUS_INVALID: 3004,
  ORDER_CREATE_FAILED: 3005,
  ORDER_PAY_FAILED: 3006,
  ORDER_CANCEL_FAILED: 3007,
  ORDER_NOT_AUTHORIZED: 3008,

  CART_EMPTY: 4001,
  CART_ITEM_NOT_FOUND: 4002,
  CART_ADD_FAILED: 4003,
  CART_UPDATE_FAILED: 4004,

  CATEGORY_NOT_FOUND: 5001,
  CATEGORY_ALREADY_EXISTS: 5002,
  CATEGORY_HAS_PRODUCTS: 5003,

  TOKEN_INVALID: 6001,
  TOKEN_EXPIRED: 6002,
  TOKEN_MISSING: 6003,

  PARAM_MISSING: 7001,
  PARAM_TYPE_ERROR: 7002,
  PARAM_FORMAT_ERROR: 7003,
  PARAM_RANGE_ERROR: 7004,

  FILE_UPLOAD_FAILED: 8001,
  FILE_TYPE_INVALID: 8002,
  FILE_SIZE_EXCEEDED: 8003,
  FILE_NOT_FOUND: 8004,

  DATABASE_ERROR: 9001,
  DUPLICATE_KEY_ERROR: 9002,
  DATA_INTEGRITY_ERROR: 9003,
  OPTIMISTIC_LOCK_ERROR: 9004,

  THIRD_PARTY_ERROR: 10001,
  SMS_SEND_FAILED: 10002,
  EMAIL_SEND_FAILED: 10003,
  PAYMENT_FAILED: 10004,
  PAYMENT_TIMEOUT: 10005,
}

export const ResultCodeMessages = {
  [ResultCode.SUCCESS]: '操作成功',
  [ResultCode.CREATED]: '创建成功',
  [ResultCode.ACCEPTED]: '请求已接受',
  [ResultCode.NO_CONTENT]: '无内容',

  [ResultCode.BAD_REQUEST]: '请求参数错误',
  [ResultCode.UNAUTHORIZED]: '未授权，请登录',
  [ResultCode.FORBIDDEN]: '没有权限访问',
  [ResultCode.NOT_FOUND]: '资源不存在',
  [ResultCode.METHOD_NOT_ALLOWED]: '请求方法不允许',
  [ResultCode.REQUEST_TIMEOUT]: '请求超时',
  [ResultCode.CONFLICT]: '资源冲突',
  [ResultCode.GONE]: '资源已删除',
  [ResultCode.UNPROCESSABLE_ENTITY]: '请求参数校验失败',
  [ResultCode.TOO_MANY_REQUESTS]: '请求过于频繁，请稍后再试',

  [ResultCode.INTERNAL_SERVER_ERROR]: '服务器内部错误',
  [ResultCode.NOT_IMPLEMENTED]: '接口未实现',
  [ResultCode.SERVICE_UNAVAILABLE]: '服务不可用',
  [ResultCode.GATEWAY_TIMEOUT]: '网关超时',

  [ResultCode.USER_NOT_FOUND]: '用户不存在',
  [ResultCode.USER_ALREADY_EXISTS]: '用户已存在',
  [ResultCode.USER_PASSWORD_ERROR]: '密码错误',
  [ResultCode.USER_ACCOUNT_DISABLED]: '账户已被禁用',
  [ResultCode.USER_ACCOUNT_LOCKED]: '账户已被锁定',
  [ResultCode.USER_NOT_LOGGED_IN]: '用户未登录',
  [ResultCode.USER_SESSION_EXPIRED]: '登录已过期，请重新登录',
  [ResultCode.USER_PERMISSION_DENIED]: '权限不足',

  [ResultCode.PRODUCT_NOT_FOUND]: '商品不存在',
  [ResultCode.PRODUCT_ALREADY_EXISTS]: '商品已存在',
  [ResultCode.PRODUCT_STOCK_INSUFFICIENT]: '商品库存不足',
  [ResultCode.PRODUCT_OFF_SHELF]: '商品已下架',
  [ResultCode.PRODUCT_INVALID]: '商品信息无效',

  [ResultCode.ORDER_NOT_FOUND]: '订单不存在',
  [ResultCode.ORDER_ALREADY_PAID]: '订单已支付',
  [ResultCode.ORDER_ALREADY_CANCELLED]: '订单已取消',
  [ResultCode.ORDER_STATUS_INVALID]: '订单状态无效',
  [ResultCode.ORDER_CREATE_FAILED]: '订单创建失败',
  [ResultCode.ORDER_PAY_FAILED]: '订单支付失败',
  [ResultCode.ORDER_CANCEL_FAILED]: '订单取消失败',
  [ResultCode.ORDER_NOT_AUTHORIZED]: '无权操作此订单',

  [ResultCode.CART_EMPTY]: '购物车为空',
  [ResultCode.CART_ITEM_NOT_FOUND]: '购物车项不存在',
  [ResultCode.CART_ADD_FAILED]: '添加购物车失败',
  [ResultCode.CART_UPDATE_FAILED]: '更新购物车失败',

  [ResultCode.CATEGORY_NOT_FOUND]: '分类不存在',
  [ResultCode.CATEGORY_ALREADY_EXISTS]: '分类已存在',
  [ResultCode.CATEGORY_HAS_PRODUCTS]: '分类下存在商品，无法删除',

  [ResultCode.TOKEN_INVALID]: 'Token无效',
  [ResultCode.TOKEN_EXPIRED]: 'Token已过期',
  [ResultCode.TOKEN_MISSING]: 'Token缺失',

  [ResultCode.PARAM_MISSING]: '参数缺失',
  [ResultCode.PARAM_TYPE_ERROR]: '参数类型错误',
  [ResultCode.PARAM_FORMAT_ERROR]: '参数格式错误',
  [ResultCode.PARAM_RANGE_ERROR]: '参数范围错误',

  [ResultCode.FILE_UPLOAD_FAILED]: '文件上传失败',
  [ResultCode.FILE_TYPE_INVALID]: '文件类型无效',
  [ResultCode.FILE_SIZE_EXCEEDED]: '文件大小超出限制',
  [ResultCode.FILE_NOT_FOUND]: '文件不存在',

  [ResultCode.DATABASE_ERROR]: '数据库操作失败',
  [ResultCode.DUPLICATE_KEY_ERROR]: '数据已存在',
  [ResultCode.DATA_INTEGRITY_ERROR]: '数据完整性约束失败',
  [ResultCode.OPTIMISTIC_LOCK_ERROR]: '乐观锁冲突，请重试',

  [ResultCode.THIRD_PARTY_ERROR]: '第三方服务调用失败',
  [ResultCode.SMS_SEND_FAILED]: '短信发送失败',
  [ResultCode.EMAIL_SEND_FAILED]: '邮件发送失败',
  [ResultCode.PAYMENT_FAILED]: '支付失败',
  [ResultCode.PAYMENT_TIMEOUT]: '支付超时',
}

export function getMessage(code) {
  return ResultCodeMessages[code] || '未知错误'
}

export function isSuccess(code) {
  return code === ResultCode.SUCCESS
}

export function isError(code) {
  return !isSuccess(code)
}

export function isAuthError(code) {
  return code === ResultCode.UNAUTHORIZED ||
         code === ResultCode.TOKEN_INVALID ||
         code === ResultCode.TOKEN_EXPIRED ||
         code === ResultCode.TOKEN_MISSING ||
         code === ResultCode.USER_NOT_LOGGED_IN ||
         code === ResultCode.USER_SESSION_EXPIRED
}

export function isForbidden(code) {
  return code === ResultCode.FORBIDDEN ||
         code === ResultCode.USER_PERMISSION_DENIED
}

export function isServerError(code) {
  return code >= 500 && code < 600
}

export function isClientError(code) {
  return code >= 400 && code < 500
}
