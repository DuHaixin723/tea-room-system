# Demo Accounts

Backend uses **MySQL + Flyway**. On first startup it will run:
- `backend/src/main/resources/db/migration/V3__seed_demo_accounts.sql`
- `backend/src/main/resources/db/migration/V4__seed_demo_data.sql`

## Frontend Ports

- Client portal (USER + STAFF): `http://localhost:5713`
  - entry: `/login`
  - USER login: `/login/user`
  - STAFF login: `/login/staff`
- Admin portal (ADMIN only): `http://localhost:5714/login`

Start dev servers:
- client: `cd frontend; npm run dev:client`
- admin: `cd frontend; npm run dev:admin`

## Credentials

- ADMIN (5714): `admin / 123456`
- STAFF (5713): `staff1 / 123456`
- USER  (5713): `user1 / 123456`

## If password is wrong

Most common reason: Flyway migrations did not run successfully (e.g. an old/broken DB).
Easiest fix for local dev: drop & recreate a clean database, then start the backend again.

