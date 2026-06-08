import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

export const ErrorCode = {
  S0000: 'S0000',

  A0001: 'A0001',
  A0002: 'A0002',
  A0003: 'A0003',
  A0004: 'A0004',
  A0005: 'A0005',
  A0006: 'A0006',
  A0007: 'A0007',
  A0008: 'A0008',
  A0009: 'A0009',
  A0010: 'A0010',
  A0011: 'A0011',
  A0012: 'A0012',
  A0013: 'A0013',
  A0014: 'A0014',
  A0015: 'A0015',
  A0016: 'A0016',

  B0001: 'B0001',
  B0002: 'B0002',
  B0003: 'B0003',
  B0004: 'B0004',
  B0005: 'B0005',
  B0006: 'B0006',
  B0007: 'B0007',
  B0008: 'B0008',
  B0009: 'B0009',
  B0010: 'B0010',
  B0011: 'B0011',
  B0012: 'B0012',
  B0013: 'B0013',
  B0014: 'B0014',
  B0015: 'B0015',
  B0016: 'B0016',
  B0017: 'B0017',
  B0018: 'B0018',
  B0019: 'B0019',
  B0020: 'B0020',
  B0021: 'B0021',
  B0022: 'B0022',
  B0023: 'B0023',
  B0024: 'B0024',
  B0025: 'B0025',
  B0026: 'B0026',
  B0027: 'B0027',
  B0028: 'B0028',
  B0029: 'B0029',
  B0030: 'B0030',
  B0031: 'B0031',
  B0032: 'B0032',
  B0033: 'B0033',
  B0034: 'B0034',
  B0035: 'B0035',
  B0036: 'B0036',
  B0037: 'B0037',
  B0038: 'B0038',
  B0039: 'B0039',
  B0040: 'B0040',
  B0041: 'B0041',
  B0042: 'B0042',
  B0043: 'B0043',
  B0044: 'B0044',
  B0045: 'B0045',
  B0046: 'B0046',
  B0047: 'B0047',
  B0048: 'B0048',
  B0049: 'B0049',
  B0050: 'B0050',
  B0051: 'B0051',
  B0052: 'B0052',

  C0001: 'C0001',
  C0002: 'C0002',
  C0003: 'C0003',
  C0004: 'C0004',
  C0005: 'C0005',
  C0006: 'C0006',
  C0007: 'C0007',
  C0008: 'C0008',
  C0009: 'C0009',
  C0010: 'C0010',

  D0001: 'D0001',
  D0002: 'D0002',
  D0003: 'D0003',
  D0004: 'D0004',
  D0005: 'D0005',
  D0006: 'D0006',
  D0007: 'D0007',
  D0008: 'D0008',
  D0009: 'D0009',
  D0010: 'D0010',
  D0011: 'D0011',
  D0012: 'D0012',
  D0013: 'D0013',
  D0014: 'D0014',
  D0015: 'D0015',
  D0016: 'D0016',
  D0017: 'D0017',
  D0018: 'D0018',
  D0019: 'D0019',
  D0020: 'D0020',
  D0021: 'D0021',
  D0022: 'D0022',
  D0023: 'D0023',
  D0024: 'D0024',
  D0025: 'D0025',
  D0026: 'D0026',
  D0027: 'D0027',

  E0001: 'E0001',
  E0002: 'E0002',
  E0003: 'E0003',
  E0004: 'E0004',
  E0005: 'E0005',
}

export const ErrorMessage = {
  [ErrorCode.A0001]: '未授权，请登录',
  [ErrorCode.A0002]: 'Token无效',
  [ErrorCode.A0003]: 'Token已过期',
  [ErrorCode.A0004]: 'Token缺失',
  [ErrorCode.A0005]: '用户名或密码错误',
  [ErrorCode.A0006]: '用户不存在',
  [ErrorCode.A0007]: '用户已存在',
  [ErrorCode.A0008]: '账户已被禁用',
  [ErrorCode.A0009]: '账户已被锁定',
  [ErrorCode.A0010]: '用户未登录',
  [ErrorCode.A0011]: '登录已过期，请重新登录',
  [ErrorCode.A0012]: '没有权限访问此资源',
  [ErrorCode.A0013]: '无权操作此资源',
  [ErrorCode.A0014]: '无权操作此订单',
  [ErrorCode.A0015]: '无权操作此地址',
  [ErrorCode.A0016]: '认证失败，请重新登录',

  [ErrorCode.B0001]: '购物车为空',
  [ErrorCode.B0002]: '购物车项不存在',
  [ErrorCode.B0003]: '添加购物车失败',
  [ErrorCode.B0004]: '更新购物车失败',
  [ErrorCode.B0005]: '订单不存在',
  [ErrorCode.B0006]: '订单已支付',
  [ErrorCode.B0007]: '订单已取消',
  [ErrorCode.B0008]: '订单状态无效',
  [ErrorCode.B0009]: '订单创建失败',
  [ErrorCode.B0010]: '订单支付失败',
  [ErrorCode.B0011]: '订单取消失败',
  [ErrorCode.B0012]: '优惠券不存在',
  [ErrorCode.B0013]: '优惠券已领取',
  [ErrorCode.B0014]: '优惠券已领完',
  [ErrorCode.B0015]: '优惠券已过期',
  [ErrorCode.B0016]: '优惠券不可用',
  [ErrorCode.B0017]: '未满足优惠券使用门槛',
  [ErrorCode.B0018]: '优惠券已使用',
  [ErrorCode.B0019]: '优惠券无效',
  [ErrorCode.B0020]: '收货地址不存在',
  [ErrorCode.B0021]: '收货地址数量已达上限',
  [ErrorCode.B0022]: '促销活动不存在',
  [ErrorCode.B0023]: '促销活动已禁用',
  [ErrorCode.B0024]: '促销活动已过期',
  [ErrorCode.B0025]: '促销活动未开始',
  [ErrorCode.B0026]: '未满足促销活动门槛',
  [ErrorCode.B0027]: '已收藏该商品',
  [ErrorCode.B0028]: '收藏记录不存在',
  [ErrorCode.B0029]: '评价列表不能为空',
  [ErrorCode.B0030]: '订单ID不能为空',
  [ErrorCode.B0031]: '该订单已评价，不能重复评价',
  [ErrorCode.B0032]: '评价列表中的订单ID不一致',
  [ErrorCode.B0033]: '回复内容不能为空',
  [ErrorCode.B0034]: '评价不存在',
  [ErrorCode.B0035]: '请选择要上传的图片',
  [ErrorCode.B0036]: '只能上传图片文件',
  [ErrorCode.B0037]: '图片上传失败',
  [ErrorCode.B0038]: '仅已完成订单可评价',
  [ErrorCode.B0039]: '评分必须在1-5之间',
  [ErrorCode.B0040]: '商品ID不能为空',
  [ErrorCode.B0041]: '评价图片最多上传3张',
  [ErrorCode.B0042]: '外卖配送请选择收货地址',
  [ErrorCode.B0043]: '收货地址信息获取失败，请重新选择',
  [ErrorCode.B0044]: '门店自提请选择自提门店',
  [ErrorCode.B0045]: '门店自提请选择预计自提时间',
  [ErrorCode.B0046]: '自提时间需在营业时间内',
  [ErrorCode.B0047]: '自提时间需在30分钟之后',
  [ErrorCode.B0048]: '订单状态已变更，请刷新',
  [ErrorCode.B0049]: '数量必须大于0',
  [ErrorCode.B0050]: '库存冲突，请稍后重试',
  [ErrorCode.B0051]: '仅管理员可创建优惠券',
  [ErrorCode.B0052]: '仅管理员可修改优惠券状态',

  [ErrorCode.C0001]: '商品不存在',
  [ErrorCode.C0002]: '商品已存在',
  [ErrorCode.C0003]: '商品库存不足',
  [ErrorCode.C0004]: '商品已下架',
  [ErrorCode.C0005]: '商品信息无效',
  [ErrorCode.C0006]: '分类不存在',
  [ErrorCode.C0007]: '分类已存在',
  [ErrorCode.C0008]: '分类下存在商品，无法删除',
  [ErrorCode.C0009]: '价格校验失败，请刷新后重试',
  [ErrorCode.C0010]: '商品价格已变动，请刷新购物车后重试',

  [ErrorCode.D0001]: '请求参数错误',
  [ErrorCode.D0002]: '参数缺失',
  [ErrorCode.D0003]: '参数类型错误',
  [ErrorCode.D0004]: '参数格式错误',
  [ErrorCode.D0005]: '参数范围错误',
  [ErrorCode.D0006]: '参数校验失败',
  [ErrorCode.D0007]: '请求的资源不存在',
  [ErrorCode.D0008]: '请求方法不允许',
  [ErrorCode.D0009]: '资源冲突',
  [ErrorCode.D0010]: '数据已存在',
  [ErrorCode.D0011]: '数据完整性约束失败',
  [ErrorCode.D0012]: '乐观锁冲突，请重试',
  [ErrorCode.D0013]: '数据库操作失败',
  [ErrorCode.D0014]: '文件上传失败',
  [ErrorCode.D0015]: '文件类型无效',
  [ErrorCode.D0016]: '文件大小超出限制',
  [ErrorCode.D0017]: '文件不存在',
  [ErrorCode.D0018]: '系统内部错误',
  [ErrorCode.D0019]: '服务不可用',
  [ErrorCode.D0020]: '网关超时',
  [ErrorCode.D0021]: '请选择要上传的头像文件',
  [ErrorCode.D0022]: '头像文件大小不能超过2MB',
  [ErrorCode.D0023]: '文件名不能为空',
  [ErrorCode.D0024]: '只支持JPG和PNG格式的图片',
  [ErrorCode.D0025]: '文件内容与声明的图片类型不匹配',
  [ErrorCode.D0026]: '非法文件路径',
  [ErrorCode.D0027]: '请求数量必须大于0',

  [ErrorCode.E0001]: '第三方服务调用失败',
  [ErrorCode.E0002]: '短信发送失败',
  [ErrorCode.E0003]: '邮件发送失败',
  [ErrorCode.E0004]: '支付失败',
  [ErrorCode.E0005]: '支付超时',
}

const AUTH_ERROR_CODES = new Set([
  ErrorCode.A0001, ErrorCode.A0002, ErrorCode.A0003, ErrorCode.A0004,
  ErrorCode.A0005, ErrorCode.A0010, ErrorCode.A0011, ErrorCode.A0016,
])

const FORBIDDEN_ERROR_CODES = new Set([
  ErrorCode.A0008, ErrorCode.A0009, ErrorCode.A0012, ErrorCode.A0013,
  ErrorCode.A0014, ErrorCode.A0015,
])

const STOCK_ERROR_CODES = new Set([
  ErrorCode.C0003, ErrorCode.B0050,
])

export function getMessage(errorCode) {
  return ErrorMessage[errorCode] || '未知错误'
}

export function isAuthError(errorCode) {
  return AUTH_ERROR_CODES.has(errorCode)
}

export function isForbiddenError(errorCode) {
  return FORBIDDEN_ERROR_CODES.has(errorCode)
}

export function isStockError(errorCode) {
  return STOCK_ERROR_CODES.has(errorCode)
}

export function getErrorModule(errorCode) {
  if (!errorCode || typeof errorCode !== 'string') return 'unknown'
  const prefix = errorCode.charAt(0).toUpperCase()
  const moduleMap = {
    A: '认证授权',
    B: '业务逻辑',
    C: '商品模块',
    D: '系统数据',
    E: '外部服务',
  }
  return moduleMap[prefix] || 'unknown'
}

let authRedirecting = false

export function handleAuthError(errorCode, errorMessage) {
  if (authRedirecting) return
  authRedirecting = true

  const message = errorMessage || getMessage(errorCode) || '登录已过期，请重新登录'

  localStorage.removeItem('token')
  localStorage.removeItem('user')

  ElMessageBox.confirm(message, '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    router.push('/login')
  }).catch(() => {
    router.push('/login')
  }).finally(() => {
    setTimeout(() => { authRedirecting = false }, 500)
  })
}

export function handleStockError(errorCode, errorMessage) {
  const message = errorMessage || getMessage(errorCode) || '库存不足'
  ElMessageBox.alert(message, '库存提示', {
    confirmButtonText: '我知道了',
    type: 'warning',
  })
}

export function handleForbiddenError(errorCode, errorMessage) {
  const message = errorMessage || getMessage(errorCode) || '没有权限访问此资源'
  ElMessage.error(message)
}

export function handleBusinessError(errorCode, errorMessage) {
  const message = errorMessage || getMessage(errorCode) || '操作失败'
  ElMessage.error(message)
}

export function handleError(errorCode, errorMessage) {
  if (!errorCode) {
    ElMessage.error(errorMessage || '操作失败')
    return
  }

  if (isAuthError(errorCode)) {
    handleAuthError(errorCode, errorMessage)
    return
  }

  if (isStockError(errorCode)) {
    handleStockError(errorCode, errorMessage)
    return
  }

  if (isForbiddenError(errorCode)) {
    handleForbiddenError(errorCode, errorMessage)
    return
  }

  handleBusinessError(errorCode, errorMessage)
}
