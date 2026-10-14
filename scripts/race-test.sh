#!/usr/bin/env bash
# Fires 50 simultaneous POST /orders for the flash-sale dish (id 1) and counts the status codes.
# Expected: 10 x 201 and the rest 409 (or 429 when the rate limit is on). Stock ends at 0.
BASE_URL="${1:-http://localhost:8080}"

echo "Stock before: $(curl -s "$BASE_URL/dishes/1")"
echo "Sending 50 simultaneous orders..."
seq 1 50 | xargs -P 50 -I{} curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST "$BASE_URL/orders" -H "Content-Type: application/json" \
  -d '{"dishId":1,"quantity":1}' | sort | uniq -c
echo "Stock after:  $(curl -s "$BASE_URL/dishes/1")"
