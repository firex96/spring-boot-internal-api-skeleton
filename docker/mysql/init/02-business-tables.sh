#!/bin/bash
# 업무 테이블 — 정본은 src/main/resources/db/manual/create-*.sql (Testcontainers 도 같은 파일을 쓴다)
set -euo pipefail
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" skeleton_main < /manual/create-main.sql
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" skeleton_sub < /manual/create-sub.sql
