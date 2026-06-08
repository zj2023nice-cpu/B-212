import request from './request'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getMe = () => request.get('/auth/me')

export const getCategories = () => request.get('/categories')
export const getProducts = (params) => request.get('/products', { params })

export const getCart = () => request.get('/cart')
export const addToCart = (data) => request.post('/cart', data)
export const updateCartItem = (id, quantity) => request.put(`/cart/${id}`, null, { params: { quantity } })
export const removeCartItem = (id) => request.delete(`/cart/${id}`)
export const clearCart = () => request.delete('/cart/clear')

export const createOrder = (data) => request.post('/orders', data)
export const getMyOrders = (page = 1, pageSize = 10) => request.get('/orders', { params: { page, pageSize } })
export const getOrderDetail = (id) => request.get(`/orders/${id}`)
export const getOrderItems = (id) => request.get(`/orders/${id}/items`)
export const updateOrderStatus = (id, status) => request.put(`/orders/${id}/status`, null, { params: { status } })

export const submitFeedback = (data) => request.post('/feedbacks', data)

export const getCoupons = (params) => request.get('/coupons', { params })
export const createCoupon = (data) => request.post('/coupons', data)
export const updateCouponStatus = (id, status) => request.put(`/coupons/${id}/status`, null, { params: { status } })
export const claimCoupon = (id) => request.post(`/coupons/${id}/claim`)
export const getMyCoupons = (params) => request.get('/coupons/mine', { params })
export const getAvailableCoupons = (params) => request.get('/coupons/available', { params })
export const applyCoupon = (data) => request.post('/coupons/apply', data)

export const getMemberLevel = () => request.get('/member/level')
export const getPointsRecords = (params) => request.get('/member/points', { params })
export const getMemberDiscount = (params) => request.get('/member/discount', { params })
