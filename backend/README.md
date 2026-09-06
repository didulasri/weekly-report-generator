# Weekly Report Generator & Team Dashboard — Backend

Spring Boot backend. Database schema and JPA mapping rules are specified in
[`docs/DATABASE_SCHEMA.md`](../docs/DATABASE_SCHEMA.md).

## Seeded accounts

`V14__seed_data.sql` creates the following accounts for local development. All
of them share the same password.

| Role          | Email                       | Password        |
| ------------- | ---------------------------- | --------------- |
| ADMIN         | admin@weeklyreport.com       | `Password123!`  |
| MANAGER       | manager@weeklyreport.com     | `Password123!`  |
| TEAM_MEMBER   | member1@weeklyreport.com     | `Password123!`  |
| TEAM_MEMBER   | member2@weeklyreport.com     | `Password123!`  |
| TEAM_MEMBER   | member3@weeklyreport.com     | `Password123!`  |
| TEAM_MEMBER   | member4@weeklyreport.com     | `Password123!`  |
| TEAM_MEMBER   | member5@weeklyreport.com     | `Password123!`  |

The stored `password` column holds the BCrypt hash of `Password123!`
(`$2b$10$nK2ikhrBxxhllr8.RjO6KuKiMZ6TpINWa50gn4AAXUrkV7IvT7RPu`), generated
with 10 salt rounds. Use `BCryptPasswordEncoder` to verify/re-hash — it
accepts both `$2a$` and `$2b$` prefixes.
