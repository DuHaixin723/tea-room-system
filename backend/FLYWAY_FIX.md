# Flyway 校验失败（checksum mismatch）如何处理

你看到的报错类似：

- `Migration checksum mismatch for migration version 2`
- `Migration checksum mismatch for migration version 4`

原因：**已经执行过的 Flyway 迁移脚本（例如 `V2__...sql`、`V4__...sql`）被修改了**。Flyway 会在启动时校验本地脚本的 checksum 与数据库 `flyway_schema_history` 记录是否一致；不一致就会直接启动失败。

## 方案 A（推荐，本地开发/演示）

直接重建一个干净数据库，让 Flyway 从 `V1` 开始按最新脚本完整执行（包含演示账号和测试数据）。

1) 停止后端应用

2) 在 MySQL 里执行（用你自己的 root 密码登录）：

```sql
DROP DATABASE IF EXISTS tea_room_management;
CREATE DATABASE tea_room_management
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;
```

3) 重新启动后端

这样会重新跑：
- `backend/src/main/resources/db/migration/V1__init_schema.sql`
- `backend/src/main/resources/db/migration/V2__extend_schema.sql`
- `backend/src/main/resources/db/migration/V3__seed_demo_accounts.sql`
- `backend/src/main/resources/db/migration/V4__seed_demo_data.sql`

## 方案 B（不想删库：repair）

如果你 **一定要保留现有库的数据**，可以用 repair 让 Flyway 接受新的 checksum：

- 方式 1：使用 Flyway CLI（推荐）
- 方式 2：在 Maven 中配置并运行 `flyway:repair`

注意：`repair` **只会更新 `flyway_schema_history` 里的 checksum**，不会把你修改后的 SQL “重新执行”到数据库里。
如果你修改迁移脚本是为了修 schema / 种子数据，那么 repair 后你仍然需要手工补数据，或者追加新的迁移版本（例如 `V5__...sql`）。

