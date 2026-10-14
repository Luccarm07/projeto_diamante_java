#!/usr/bin/env bash
# Sends 300 reviews at once (all answered 202), waits for the 5s flush and shows the ranking.
ORDER_URL="${1:-http://localhost:8080}"
REVIEW_URL="${2:-http://localhost:8083}"

seq 1 300 | xargs -P 50 -I{} sh -c '
  dish=$(( ({} % 5) + 1 )); rating=$(( ({} % 5) + 1 ))
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "'"$ORDER_URL"'/reviews" \
    -H "Content-Type: application/json" \
    -d "{\"dishId\":$dish,\"rating\":$rating,\"comment\":\"load test\"}"' | sort | uniq -c
echo "Waiting for the scheduled flush..."
sleep 6
curl -s "$REVIEW_URL/reviews/ranking"; echo
