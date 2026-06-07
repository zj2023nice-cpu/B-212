# 奶茶点餐系统 (Milk Tea Ordering System)

这是一个基于现代技术栈开发的奶茶在线点餐系统，具有极佳的视觉效果和完整的业务闭环。

## 🛠 技术栈

- **Frontend**: Vue 3 + Vite + Tailwind CSS + Element Plus
- **Backend**: Spring Boot 3 + Spring Security + JWT + MyBatis-Plus
- **Database**: MySQL 8.0
- **DevOps**: Docker + Docker Compose

## ✨ 核心模块

1. **用户认证**: 支持注册、登录，基于 JWT 的细粒度权限控制。
2. **菜单浏览**: 动态分类展示，精美商品卡片，支持规格自定义加购。
3. **购物车**: 实时同步持久化，支持数量增减与批量清理。
4. **订单结算**: 模拟真实下单流，支持备注信息填写。
5. **订单跟踪**: 可视化状态机跟踪（制作中、配送中、已送达）。
6. **评价反馈**: 完成订单后可进行五星好评与心得分享。

## 🚀 启动指南 (How to Run)

1. 在项目根目录执行：
   ```bash
   docker compose up --build
   ```
2. 等待容器构建并启动成功

## 🔗 服务地址 (Services)

- **前端页面**: [http://localhost:13000](http://localhost:13000)
- **后端 API**: [http://localhost:8080](http://localhost:8080)
- **Swagger 文档**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

## 注册账号

点击登陆页面的立即注册按钮，按照提示完成注册即可。
