# 项目重要文件定位

这份文档按“入口 -> 页面/组件 -> 前端接口 -> 后端控制器/服务 -> 数据层”的方式整理，目标是让你在改需求、排问题、写论文说明时，能快速定位当前系统的关键文件。

## 1. 当前系统结构总览

项目当前是前后端分离结构：

- 前端：`frontend`
  - 包含总入口、管理端入口、客户端入口
  - 基于 Vue 3 + Pinia + Vue Router + Element Plus
- 后端：`backend`
  - 基于 Spring Boot + Spring Security + JPA + Flyway
- 文档：`docs`
- 上传目录：`uploads`

## 2. 前端启动入口

当前前端不是单一入口，而是 3 个启动入口：

- `frontend/src/main.ts`
  - 总入口
  - 使用 `frontend/src/router/index.ts`
  - 对应总登录选择页、管理端入口页、客户端入口页

- `frontend/src/portals/admin/main.ts`
  - 管理端独立入口
  - 使用 `frontend/src/portals/admin/router.ts`
  - 默认登录页是 `/login`

- `frontend/src/portals/client/main.ts`
  - 客户端独立入口
  - 使用 `frontend/src/portals/client/router.ts`
  - 默认登录页是 `/login`

如果你发现“同一套页面为什么端口不同、路由不同”，优先先看这 3 个入口文件。

## 3. 路由与门户划分

### 3.1 总路由

- `frontend/src/router/index.ts`
  - 总入口路由
  - 负责：
    - `/login` 总登录选择页
    - `/login/admin` 管理端登录页
    - `/login/client` 客户端登录页
    - 管理端后台页面挂载
  - 包含登录态守卫和角色访问控制

### 3.2 管理端路由

- `frontend/src/portals/admin/router.ts`
  - 管理端专用路由
  - 登录页：`/login`
  - 默认首页：`/dashboard`
  - 管理员角色校验逻辑在这里

### 3.3 客户端路由

- `frontend/src/portals/client/router.ts`
  - 客户端专用路由
  - 登录页：`/login`
  - 用户登录页：`/login/user`
  - 茶室员登录页：`/login/staff`
  - 注册页：`/register/user`
  - 用户首页：`/home`
  - 茶室员工作台：`/staff`
  - 用户/茶室员角色校验逻辑在这里

### 3.4 菜单配置

- `frontend/src/config/menu.ts`
  - 左侧菜单与角色可见页面配置
  - 新增页面后，如果希望出现在菜单中，通常要同步修改这里

### 3.5 通用布局

- `frontend/src/layouts/AppLayout.vue`
  - 三端共用主布局
  - 负责：
    - 顶部工具栏
    - 左侧菜单
    - 用户信息区
    - 通知中心挂载
    - AI 助手挂载
    - 退出登录逻辑

## 4. 认证、登录、注册与权限

### 4.1 前端认证相关

- `frontend/src/views/auth/LoginPortalView.vue`
  - 总入口登录选择页
  - 用于区分管理端和客户端入口

- `frontend/src/views/auth/ClientLoginView.vue`
  - 客户端登录总入口
  - 可切换到管理端登录

- `frontend/src/views/auth/ClientPortalView.vue`
  - 客户端身份选择页
  - 用于区分普通用户入口和茶室员入口

- `frontend/src/views/auth/UserLoginView.vue`
  - 普通用户登录页

- `frontend/src/views/auth/StaffLoginView.vue`
  - 茶室员登录页

- `frontend/src/views/auth/AdminLoginView.vue`
  - 管理员登录页

- `frontend/src/views/auth/UserRegisterView.vue`
  - 用户/茶室员注册页
  - 当前注册逻辑主要已整合在这个页面内

- `frontend/src/views/auth/components/LoginFormCard.vue`
  - 登录表单核心组件
  - 登录提交、角色校验、登录成功后跳转逻辑在这里

- `frontend/src/views/auth/components/LoginShell.vue`
  - 登录页通用视觉壳组件
  - 登录页品牌名加载也会经过这里

- `frontend/src/views/auth/components/RegisterAccountDialog.vue`
  - 注册弹窗组件
  - 文件仍存在，但当前主注册流程已经主要落在 `UserRegisterView.vue`

- `frontend/src/api/modules/auth.ts`
  - 登录、注册接口封装

- `frontend/src/stores/auth.ts`
  - 登录态、当前用户、角色信息

- `frontend/src/api/http.ts`
  - Axios 实例
  - 请求头 token 注入
  - 401/403 统一处理
  - 登录重定向问题优先看这里

- `frontend/src/utils/storage.ts`
  - token 和用户信息本地存储

### 4.2 后端认证相关

- `backend/src/main/java/com/tea/management/controller/AuthController.java`
  - 登录、注册接口入口

- `backend/src/main/java/com/tea/management/service/AuthService.java`
  - 登录校验、注册逻辑、角色判断核心

- `backend/src/main/java/com/tea/management/service/JwtTokenService.java`
  - JWT 生成、解析

- `backend/src/main/java/com/tea/management/config/SecurityConfig.java`
  - Spring Security 总配置
  - 哪些接口匿名可访问、哪些接口要求登录，都在这里
  - 出现“未授权访问”“登录页被 401”时优先看这里

- `backend/src/main/java/com/tea/management/config/JwtAuthenticationFilter.java`
  - JWT 认证过滤器

- `backend/src/main/java/com/tea/management/dto/request/RegisterRequest.java`
  - 注册请求 DTO

## 5. 用户资料、头像、密码、会员账户

### 5.1 前端

- `frontend/src/views/profile/ProfileView.vue`
  - 个人中心页

- `frontend/src/stores/member.ts`
  - 会员账户余额、等级、充值记录缓存

- `frontend/src/api/modules/management.ts`
  - 相关接口：
    - `usersApi.getProfile`
    - `usersApi.profile`
    - `usersApi.password`
    - `usersApi.uploadAvatar`
    - `usersApi.account`
    - `usersApi.rechargeRecords`
    - `usersApi.recharge`

### 5.2 后端

- `backend/src/main/java/com/tea/management/controller/UserController.java`
- `backend/src/main/java/com/tea/management/service/UserService.java`
- `backend/src/main/java/com/tea/management/service/MemberLevelService.java`
- `backend/src/main/java/com/tea/management/domain/entity/User.java`
- `backend/src/main/java/com/tea/management/domain/entity/MemberAccount.java`
- `backend/src/main/java/com/tea/management/domain/entity/MemberRechargeRecord.java`

## 6. 茶室管理

### 6.1 前端

- `frontend/src/views/teaRooms/TeaRoomsView.vue`
  - 茶室列表、类型、启停用、编辑、分页

- `frontend/src/api/modules/management.ts`
  - `teaRoomApi.roomTypes`
  - `teaRoomApi.rooms`
  - `teaRoomApi.createRoom`
  - `teaRoomApi.updateRoom`
  - `teaRoomApi.removeRoom`

### 6.2 后端

- `backend/src/main/java/com/tea/management/controller/TeaRoomController.java`
- `backend/src/main/java/com/tea/management/service/TeaRoomService.java`
- `backend/src/main/java/com/tea/management/domain/entity/TeaRoom.java`
- `backend/src/main/java/com/tea/management/domain/entity/TeaRoomType.java`

## 7. 茶叶商城

### 7.1 前端

- `frontend/src/views/teas/TeasView.vue`
- `frontend/src/api/modules/management.ts`
  - `teaApi.page`
  - `teaApi.create`
  - `teaApi.update`
  - `teaApi.remove`

### 7.2 后端

- `backend/src/main/java/com/tea/management/controller/TeaController.java`
- `backend/src/main/java/com/tea/management/service/TeaService.java`
- `backend/src/main/java/com/tea/management/domain/entity/Tea.java`

## 8. 预约管理

### 8.1 前端

- `frontend/src/views/reservations/ReservationsView.vue`
  - 预约创建、状态变更、分页、角色共用页面

- `frontend/src/api/modules/management.ts`
  - `reservationApi.page`
  - `reservationApi.availability`
  - `reservationApi.create`
  - `reservationApi.status`

### 8.2 后端

- `backend/src/main/java/com/tea/management/controller/ReservationController.java`
- `backend/src/main/java/com/tea/management/service/ReservationService.java`
- `backend/src/main/java/com/tea/management/domain/entity/Reservation.java`

## 9. 订单中心与支付

### 9.1 前端

- `frontend/src/views/orders/OrdersView.vue`
  - 订单列表、支付、状态流转
  - 余额不足时跳转充值页的逻辑也在这里

- `frontend/src/api/modules/management.ts`
  - `orderApi.page`
  - `orderApi.create`
  - `orderApi.pay`
  - `orderApi.detail`
  - `orderApi.updateItems`
  - `orderApi.status`

### 9.2 后端

- `backend/src/main/java/com/tea/management/controller/OrderController.java`
- `backend/src/main/java/com/tea/management/service/OrderService.java`
- `backend/src/main/java/com/tea/management/service/OrderLifecycleService.java`
- `backend/src/main/java/com/tea/management/domain/entity/ProductOrder.java`
- `backend/src/main/java/com/tea/management/domain/entity/OrderItem.java`

## 10. 活动中心

### 10.1 前端

- `frontend/src/views/activities/ActivitiesView.vue`
  - 活动列表、报名、筛选、分页

- `frontend/src/api/modules/management.ts`
  - `activityApi.page`
  - `activityApi.create`
  - `activityApi.update`
  - `activityApi.status`
  - `activityApi.summary`
  - `activityApi.registrations`
  - `activityApi.register`
  - `activityApi.cancelRegistration`

### 10.2 后端

- `backend/src/main/java/com/tea/management/controller/ActivityController.java`
- `backend/src/main/java/com/tea/management/service/ActivityService.java`
- `backend/src/main/java/com/tea/management/domain/entity/Activity.java`
- `backend/src/main/java/com/tea/management/domain/entity/ActivityRegistration.java`

## 11. 评论、报障、投诉

### 11.1 评论

- 前端：`frontend/src/views/reviews/ReviewsView.vue`
- 后端：`backend/src/main/java/com/tea/management/controller/ReviewController.java`
- 服务：`backend/src/main/java/com/tea/management/service/ReviewService.java`
- 实体：`backend/src/main/java/com/tea/management/domain/entity/Review.java`

### 11.2 设备报障

- 前端：`frontend/src/views/reports/ReportsView.vue`
- 后端：`backend/src/main/java/com/tea/management/controller/EquipmentReportController.java`
- 服务：`backend/src/main/java/com/tea/management/service/EquipmentReportService.java`
- 实体：`backend/src/main/java/com/tea/management/domain/entity/EquipmentReport.java`

### 11.3 员工投诉

- 前端：`frontend/src/views/complaints/ComplaintsView.vue`
- 后端：`backend/src/main/java/com/tea/management/controller/StaffComplaintController.java`
- 服务：`backend/src/main/java/com/tea/management/service/StaffComplaintService.java`
- 实体：`backend/src/main/java/com/tea/management/domain/entity/StaffComplaint.java`

## 12. 收藏与推荐

### 12.1 收藏

- 前端：`frontend/src/views/favorites/FavoritesView.vue`
- 后端：`backend/src/main/java/com/tea/management/controller/FavoriteController.java`
- 服务：`backend/src/main/java/com/tea/management/service/FavoriteService.java`
- 实体：`backend/src/main/java/com/tea/management/domain/entity/Favorite.java`

### 12.2 推荐

- 前端：`frontend/src/views/recommendations/RecommendationsView.vue`
- 后端：`backend/src/main/java/com/tea/management/controller/RecommendationController.java`
- 服务：`backend/src/main/java/com/tea/management/service/RecommendationService.java`

## 13. 在线咨询与 WebSocket

### 13.1 前端

- `frontend/src/views/consultations/ConsultationsView.vue`
  - 咨询会话主页面

- `frontend/src/stores/consultation.ts`
  - 会话列表、未读数、消息状态管理

- `frontend/src/api/modules/management.ts`
  - `consultationApi.sessions`
  - `consultationApi.createSession`
  - `consultationApi.closeSession`
  - `consultationApi.deleteSession`
  - `consultationApi.detail`
  - `consultationApi.sendMessage`

### 13.2 后端

- `backend/src/main/java/com/tea/management/controller/ConsultationController.java`
- `backend/src/main/java/com/tea/management/controller/ConsultationSocketController.java`
- `backend/src/main/java/com/tea/management/service/ConsultationService.java`
- `backend/src/main/java/com/tea/management/domain/entity/ConsultationSession.java`
- `backend/src/main/java/com/tea/management/domain/entity/ConsultationMessage.java`

## 14. 充值、会员等级、优惠券

### 14.1 前端

- `frontend/src/views/recharge/RechargeView.vue`
  - 充值金额、优惠规则、到账预估等

- `frontend/src/views/coupons/CouponsView.vue`
  - 用户查看优惠券
  - 管理员发券

- `frontend/src/stores/member.ts`

- `frontend/src/api/modules/management.ts`
  - `usersApi.recharge`
  - `usersApi.rechargeRecords`
  - `couponApi.page`
  - `couponApi.dispatchWeekly`
  - `couponApi.dispatchByLevel`
  - `systemConfigApi.rechargePolicy`

### 14.2 后端

- `backend/src/main/java/com/tea/management/controller/UserCouponController.java`
- `backend/src/main/java/com/tea/management/service/UserCouponService.java`
- `backend/src/main/java/com/tea/management/service/NotificationService.java`
- `backend/src/main/java/com/tea/management/service/MemberLevelService.java`
- `backend/src/main/java/com/tea/management/domain/entity/UserCoupon.java`
- `backend/src/main/java/com/tea/management/dto/request/DispatchCouponByLevelRequest.java`

## 15. 通知中心

### 15.1 前端

- `frontend/src/stores/notification.ts`
  - 通知列表、未读数、轮询和实时推送状态

- `frontend/src/components/shared/NotificationCenter.vue`
  - 通知中心展示组件

### 15.2 后端

- `backend/src/main/java/com/tea/management/controller/NotificationController.java`
- `backend/src/main/java/com/tea/management/service/NotificationService.java`
- `backend/src/main/java/com/tea/management/domain/entity/Notification.java`
- `backend/src/main/java/com/tea/management/repository/NotificationRepository.java`

## 16. AI 助手

这一块是当前系统里真实存在的功能，旧文档没有覆盖，现补入。

### 16.1 前端

- `frontend/src/components/shared/AiAssistantWidget.vue`
  - AI 助手浮窗
  - 流式对话、拖拽、会话展示都在这里

- `frontend/src/utils/markdown.ts`
  - AI 返回内容的 Markdown 渲染

### 16.2 后端

- `backend/src/main/java/com/tea/management/controller/AiAssistantController.java`
  - AI 流式接口入口

- `backend/src/main/java/com/tea/management/service/AiAssistantService.java`
  - AI 调用与流式输出逻辑

- `backend/src/main/java/com/tea/management/config/AiAssistantProperties.java`
  - AI 相关配置

## 17. 仪表盘与统计

### 17.1 前端

- `frontend/src/views/dashboard/DashboardView.vue`
  - 管理端首页
  - 图表与统计展示

- `frontend/src/api/modules/dashboard.ts`
  - 仪表盘数据接口封装

### 17.2 后端

- `backend/src/main/java/com/tea/management/controller/StatisticController.java`
- `backend/src/main/java/com/tea/management/service/StatisticService.java`

## 18. 系统设置、品牌配置与上传

### 18.1 系统设置

- 前端：`frontend/src/views/settings/SettingsView.vue`
- 前端品牌加载：`frontend/src/composables/useSystemBranding.ts`
- 后端：`backend/src/main/java/com/tea/management/controller/SystemConfigController.java`
- 服务：`backend/src/main/java/com/tea/management/service/SystemConfigService.java`
- 实体：`backend/src/main/java/com/tea/management/domain/entity/SystemConfig.java`

补充说明：

- 登录页品牌名、站点标题等基础信息，实际也会走 `useSystemBranding.ts`
- 如果登录页出现“未授权访问”，除了看登录逻辑，也要检查：
  - `frontend/src/composables/useSystemBranding.ts`
  - `backend/src/main/java/com/tea/management/config/SecurityConfig.java`
  - `backend/src/main/java/com/tea/management/controller/SystemConfigController.java`

### 18.2 文件上传

- 前端：`frontend/src/api/modules/management.ts`
  - `fileApi.uploadImage`
  - `fileApi.uploadPublicImage`

- 后端：
  - `backend/src/main/java/com/tea/management/controller/FileController.java`
  - `backend/src/main/java/com/tea/management/service/ImageStorageService.java`
  - `backend/src/main/java/com/tea/management/service/AvatarStorageService.java`

## 19. DTO、映射、Repository、数据库迁移

### 19.1 DTO 与映射

- 请求 DTO：`backend/src/main/java/com/tea/management/dto/request`
- 响应 DTO：`backend/src/main/java/com/tea/management/dto/response`
- 响应映射：`backend/src/main/java/com/tea/management/dto/response/ResponseMapper.java`

如果出现这些问题，优先看 DTO 和映射：

- 前端传了字段但后端收不到
- 后端返回了字段但前端拿不到
- 新增字段后页面还是空

### 19.2 Repository

- 目录：`backend/src/main/java/com/tea/management/repository`

常见重点：

- `UserCouponRepository.java`
- `MemberAccountRepository.java`
- `NotificationRepository.java`
- `ProductOrderRepository.java`
- `ReservationRepository.java`
- `TeaRepository.java`
- `TeaRoomRepository.java`

### 19.3 Flyway

- 目录：`backend/src/main/resources/db/migration`
- 当前迁移已到：`V39__rename_attendant_label_to_room_attendant.sql`

和当前业务结构关联较强的后期迁移：

- `V24__add_staff_registration_approval_fields.sql`
- `V25__support_multiple_staff_certification_images.sql`
- `V26__add_business_hours_to_tea_room.sql`
- `V27__add_activity_creator_and_summary.sql`
- `V28__create_notification_table.sql`
- `V29__link_review_to_reservation.sql`
- `V30__add_public_tea_room_seed.sql`
- `V31__add_teas.sql`
- `V32__add_tea_rooms.sql`
- `V33__localize_tea_rooms.sql`
- `V34__rename_tea_rooms_consistently.sql`
- `V35__normalize_new_tea_categories.sql`
- `V36__seed_more_users_and_staff.sql`
- `V37__seed_equipment_reports_for_each_tea_room.sql`
- `V38__add_column_comments.sql`
- `V39__rename_attendant_label_to_room_attendant.sql`

### 19.4 全量 SQL

- `backend/docs/schema_mysql_complete.sql`

## 20. 后端总配置

- `backend/src/main/resources/application.yml`
  - 数据库连接
  - JWT 配置
  - AI 配置
  - 上传目录配置
  - 其他系统级参数

## 21. 建议的排查顺序

### 21.1 页面问题

1. 先看页面文件 `frontend/src/views/...`
2. 再看共用组件 `frontend/src/components/...` 或 `frontend/src/views/.../components/...`
3. 再看接口封装 `frontend/src/api/modules/...`
4. 再看状态管理 `frontend/src/stores/...`
5. 再看后端控制器 `backend/.../controller/...`
6. 再看后端服务 `backend/.../service/...`
7. 最后看 DTO、实体、Repository、Flyway

### 21.2 登录/权限问题

优先顺序：

1. `frontend/src/api/http.ts`
2. `frontend/src/stores/auth.ts`
3. 对应入口路由：
   - `frontend/src/router/index.ts`
   - `frontend/src/portals/admin/router.ts`
   - `frontend/src/portals/client/router.ts`
4. `backend/src/main/java/com/tea/management/config/SecurityConfig.java`
5. `backend/src/main/java/com/tea/management/controller/AuthController.java`
6. `backend/src/main/java/com/tea/management/service/AuthService.java`

### 21.3 数据结构问题

优先顺序：

1. `backend/src/main/java/com/tea/management/domain/entity/...`
2. `backend/src/main/java/com/tea/management/dto/...`
3. `backend/src/main/java/com/tea/management/repository/...`
4. `backend/src/main/resources/db/migration/...`
5. `backend/docs/schema_mysql_complete.sql`

## 22. 备注

这份文档对应的是当前仓库结构，不再对应早期单入口、单登录页版本。

如果后续你新增了：

- 新页面
- 新角色入口
- 新接口模块
- 新数据库迁移
- 新的共享组件或系统级功能

建议同步更新这份文档，否则它会很快再次失效。
