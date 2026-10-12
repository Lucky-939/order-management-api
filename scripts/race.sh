#!/usr/bin/env bash

# Race condition verification script for Linux / macOS
# Sends concurrent requests using background jobs and prints per-status-code summary.

MODE=""
COUNT=10
PRODUCT_ID=""
ORDER_ID=""
BASE_URL="http://localhost:8080"

while [[ $# -gt 0 ]]; do
  case "$1" in
    -m|--mode|-Mode)
      MODE="$2"
      shift 2
      ;;
    -c|--count|-Count)
      COUNT="$2"
      shift 2
      ;;
    -p|--product-id|-ProductId)
      PRODUCT_ID="$2"
      shift 2
      ;;
    -o|--order-id|-OrderId)
      ORDER_ID="$2"
      shift 2
      ;;
    -b|--base-url|-BaseUrl)
      BASE_URL="$2"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1"
      exit 1
      ;;
  esac
done

if [[ -z "$MODE" ]]; then
  echo "Usage: $0 -Mode <Place|Cancel|Patch> [-Count <count>] [-ProductId <id>] [-OrderId <id>] [-BaseUrl <url>]"
  exit 1
fi

TMP_DIR=$(mktemp -d 2>/dev/null || mktemp -d -t 'race')
trap 'rm -rf "$TMP_DIR"' EXIT

# Launch all requests concurrently in background subshells
for ((i=1; i<=COUNT; i++)); do
  (
    if [[ "$MODE" == "Place" ]]; then
      PAYLOAD="{\"customerName\":\"Racer $i\",\"items\":[{\"productId\":$PRODUCT_ID,\"quantity\":1}]}"
      curl -s -w "\n%{http_code}" -X POST "$BASE_URL/orders" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" > "$TMP_DIR/res_$i.txt"
    elif [[ "$MODE" == "Cancel" ]]; then
      curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/orders/$ORDER_ID" > "$TMP_DIR/res_$i.txt"
    elif [[ "$MODE" == "Patch" ]]; then
      PAYLOAD="{\"status\":\"SHIPPED\"}"
      curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/orders/$ORDER_ID/status" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD" > "$TMP_DIR/res_$i.txt"
    fi
  ) &
done

# Wait for all background requests to complete
wait

# Process outputs and print individual status lines
for ((i=1; i<=COUNT; i++)); do
  FILE="$TMP_DIR/res_$i.txt"
  if [[ -f "$FILE" ]]; then
    STATUS=$(tail -n 1 "$FILE")
    BODY=$(sed '$d' "$FILE")
    MSG=$(echo "$BODY" | grep -o '"message":"[^"]*"' | head -n 1 | cut -d'"' -f4)
    if [[ -n "$MSG" ]]; then
      echo "$STATUS $MSG"
    else
      echo "$STATUS"
    fi
    echo "$STATUS" >> "$TMP_DIR/statuses.txt"
  fi
done

# Output count summary per status code
echo ""
echo "--- Summary ---"
if [[ -f "$TMP_DIR/statuses.txt" ]]; then
  sort "$TMP_DIR/statuses.txt" | uniq -c | while read -r count code; do
    echo "$code : $count"
  done
fi
