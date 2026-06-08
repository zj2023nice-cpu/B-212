import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/order/:id/receipt',
    name: 'OrderReceipt',
    component: () => import('../views/ReceiptView.vue')
  },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/Home.vue')
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('../views/Cart.vue')
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/Orders.vue')
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('../views/OrderDetail.vue')
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('../views/ProductDetail.vue')
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('../views/Coupons.vue')
      },
      {
        path: 'my-coupons',
        name: 'MyCoupons',
        component: () => import('../views/MyCoupons.vue')
      },
      {
        path: 'member',
        name: 'MemberCenter',
        component: () => import('../views/MemberCenter.vue')
      },
      {
        path: 'coupon-admin',
        name: 'CouponAdmin',
        component: () => import('../views/CouponAdmin.vue')
      },
      {
        path: 'notifications',
        name: 'NotificationCenter',
        component: () => import('../views/NotificationCenter.vue')
      },
      {
        path: 'favorites',
        name: 'MyFavorites',
        component: () => import('../views/MyFavorites.vue')
      },
      {
        path: 'admin/dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { requiresAdmin: true }
      },
      {
        path: 'admin/products',
        name: 'ProductAdmin',
        component: () => import('../views/ProductAdmin.vue'),
        meta: { requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (['Login', 'Register'].includes(to.name)) {
    next()
  } else if (!token) {
    next({ name: 'Login' })
  } else if (to.meta?.requiresAdmin) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (user?.role === 'ADMIN') {
      next()
    } else {
      next({ name: 'Home' })
    }
  } else {
    next()
  }
})

export default router
