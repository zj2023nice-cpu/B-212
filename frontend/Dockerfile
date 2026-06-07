# 使用多阶段构建
FROM node:20-alpine AS builder
WORKDIR /app
# 配置镜像源
RUN npm config set registry https://registry.npmmirror.com
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
