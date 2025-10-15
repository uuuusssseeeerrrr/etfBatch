#!/bin/sh

set -e  # 오류 발생 시 즉시 종료

# /run/secrets 에서 시크릿 파일 읽어서 환경변수로 export
for secret in DB_URL DB_USERNAME DB_PASSWORD KIS_KEY KIS_SECRET BATCHTOKEN
do
  secret_path="/run/secrets/$secret"
  if [ -f "$secret_path" ]; then
    export "$secret"=$(cat "$secret_path")
  else
    echo "Warning: Secret file $secret_path not found!"
  fi
done

# 실제 앱 실행 (포트 번호 환경변수로 관리하고 싶으면 $PORT 사용 가능)
exec java -server -XX:TieredStopAtLevel=1 -jar build/libs/etfBatch.jar -port=7777
