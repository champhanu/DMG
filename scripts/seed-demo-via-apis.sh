#!/usr/bin/env bash
# Idempotent demo seed via APIs — safe to re-run.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ADMIN="admin:admin123"
CUSTOMER="customer:customer123"
STAFF="staff:staff123"

get_or_create_category() {
  local name=$1 slug=$2 desc=$3
  local id
  id=$(curl -s -u $ADMIN "$BASE/api/categories" | python3 -c "
import sys,json
for c in json.load(sys.stdin):
    if c.get('slug') == '$slug':
        print(c['id']); break
" || true)
  if [[ -z "${id:-}" ]]; then
    id=$(curl -s -u $ADMIN -X POST "$BASE/api/categories" -H "Content-Type: application/json" \
      -d "{\"name\":\"$name\",\"slug\":\"$slug\",\"description\":\"$desc\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  fi
  echo "$id"
}

get_or_create_product() {
  local name=$1 sku=$2 desc=$3 price=$4 cat=$5
  local id
  id=$(curl -s -u $ADMIN "$BASE/api/products?search=$sku&size=50" | python3 -c "
import sys,json
data=json.load(sys.stdin)
for p in data.get('content', data if isinstance(data,list) else []):
    if p.get('sku') == '$sku':
        print(p['id']); break
" || true)
  if [[ -z "${id:-}" ]]; then
    local resp
    resp=$(curl -s -u $ADMIN -X POST "$BASE/api/products" -H "Content-Type: application/json" \
      -d "{\"name\":\"$name\",\"sku\":\"$sku\",\"description\":\"$desc\",\"price\":$price,\"categoryId\":$cat}")
    id=$(echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('id',''))" 2>/dev/null || true)
    if [[ -z "${id:-}" ]]; then
      echo "Failed to create product $sku: $resp" >&2
      exit 1
    fi
  fi
  echo "$id"
}

get_or_create_warehouse() {
  local name=$1 code=$2 loc=$3
  local id
  id=$(curl -s -u $ADMIN "$BASE/api/warehouses" | python3 -c "
import sys,json
for w in json.load(sys.stdin):
    if w.get('code') == '$code':
        print(w['id']); break
" || true)
  if [[ -z "${id:-}" ]]; then
    id=$(curl -s -u $ADMIN -X POST "$BASE/api/warehouses" -H "Content-Type: application/json" \
      -d "{\"name\":\"$name\",\"code\":\"$code\",\"location\":\"$loc\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
  fi
  echo "$id"
}

json_order() { python3 -c "import sys,json; print(json.load(sys.stdin)['orderId'])"; }

echo "=== Seeding demo catalog ==="

CAT_ELEC=$(get_or_create_category "Electronics" "demo-electronics" "Laptops, phones, audio, TVs")
CAT_CLO=$(get_or_create_category "Clothing & Apparel" "demo-clothing" "Fashion and footwear")
CAT_HOME=$(get_or_create_category "Home & Kitchen" "demo-home" "Appliances and essentials")
CAT_SPORT=$(get_or_create_category "Sports & Outdoors" "demo-sports" "Fitness and outdoor gear")

P1=$(get_or_create_product "MacBook Pro 14-inch" DEMO-LAP-001 "Apple M3 Pro laptop" 1999.99 $CAT_ELEC)
P2=$(get_or_create_product "iPhone 15 Pro" DEMO-PHN-001 "256GB smartphone" 999.99 $CAT_ELEC)
P3=$(get_or_create_product "Sony WH-1000XM5" DEMO-AUD-001 "Noise-cancelling headphones" 349.99 $CAT_ELEC)
P4=$(get_or_create_product "Samsung 55-inch 4K TV" DEMO-TV-001 "Crystal UHD smart TV" 799.99 $CAT_ELEC)
P5=$(get_or_create_product "Nike Air Zoom Pegasus" DEMO-SHO-001 "Running shoes" 129.99 $CAT_CLO)
P6=$(get_or_create_product "Levis 501 Jeans" DEMO-JNS-001 "Classic denim" 79.99 $CAT_CLO)
P7=$(get_or_create_product "North Face Jacket" DEMO-JKT-001 "Winter jacket" 199.99 $CAT_CLO)
P8=$(get_or_create_product "Dyson V15 Vacuum" DEMO-VAC-001 "Cordless vacuum" 449.99 $CAT_HOME)
P9=$(get_or_create_product "Instant Pot Duo" DEMO-POT-001 "7-in-1 cooker" 89.99 $CAT_HOME)
P10=$(get_or_create_product "Nespresso Vertuo" DEMO-COF-001 "Coffee machine" 179.99 $CAT_HOME)
P11=$(get_or_create_product "Manduka Yoga Mat" DEMO-YOG-001 "Eco yoga mat" 29.99 $CAT_SPORT)
P12=$(get_or_create_product "Bowflex Dumbbells" DEMO-DMB-001 "Adjustable set" 349.99 $CAT_SPORT)
P13=$(get_or_create_product "Wilson Tennis Racket" DEMO-TEN-001 "Pro graphite racket" 119.99 $CAT_SPORT)

echo "Products ready: $P1-$P13"

WH_E=$(get_or_create_warehouse "Demo East DC" DEMO-WH-EAST "New York, NY")
WH_W=$(get_or_create_warehouse "Demo West DC" DEMO-WH-WEST "Los Angeles, CA")
WH_C=$(get_or_create_warehouse "Demo Central DC" DEMO-WH-CENTRAL "Chicago, IL")

stock() {
  curl -s -u $ADMIN -X POST "$BASE/api/inventory" -H "Content-Type: application/json" \
    -d "{\"warehouseId\":$1,\"productId\":$2,\"quantity\":$3}" > /dev/null
}

for W in $WH_E $WH_W $WH_C; do
  stock $W $P1 40; stock $W $P2 60; stock $W $P3 80; stock $W $P4 20
  stock $W $P5 100; stock $W $P6 120; stock $W $P7 45
  stock $W $P8 25; stock $W $P9 150; stock $W $P10 55
  stock $W $P11 200; stock $W $P12 30; stock $W $P13 65
done

echo "=== Seeding orders ==="

checkout() {
  local CID=$1; shift
  for item in "$@"; do
    IFS=, read -r PID QTY <<< "$item"
    curl -s -u $CUSTOMER -X POST "$BASE/api/cart/items" -H "Content-Type: application/json" \
      -d "{\"customerId\":$CID,\"productId\":$PID,\"quantity\":$QTY}" > /dev/null
  done
  curl -s -u $CUSTOMER -X POST "$BASE/api/checkout" -H "Content-Type: application/json" \
    -d "{\"customerId\":$CID,\"paymentMethod\":\"CARD\"}" | json_order
}

patch_status() {
  curl -s -u $STAFF -X PATCH "$BASE/api/orders/$1/status" -H "Content-Type: application/json" \
    -d "{\"status\":\"$2\"}" > /dev/null
}

O_CONFIRMED=$(checkout 20 "$P2,1")
O_PACKED=$(checkout 21 "$P3,1"); patch_status $O_PACKED PACKED
O_SHIPPED=$(checkout 22 "$P4,1"); patch_status $O_SHIPPED PACKED; patch_status $O_SHIPPED SHIPPED
O_DELIVERED=$(checkout 23 "$P5,1"); patch_status $O_DELIVERED PACKED; patch_status $O_DELIVERED SHIPPED; patch_status $O_DELIVERED DELIVERED
O_RETURNED=$(checkout 24 "$P6,1"); patch_status $O_RETURNED PACKED; patch_status $O_RETURNED SHIPPED; patch_status $O_RETURNED DELIVERED
curl -s -u $CUSTOMER -X POST "$BASE/api/orders/$O_RETURNED/return" -H "Content-Type: application/json" \
  -d '{"customerId":24,"reason":"Demo return for video"}' > /dev/null
O_CANCELLED=$(checkout 25 "$P7,1")
curl -s -u $CUSTOMER -X POST "$BASE/api/orders/$O_CANCELLED/cancel" -H "Content-Type: application/json" \
  -d '{"customerId":25,"reason":"Demo cancellation for video"}' > /dev/null
O_MULTI1=$(checkout 26 "$P8,1" "$P9,2" "$P10,1" "$P11,1")
patch_status $O_MULTI1 PACKED; patch_status $O_MULTI1 SHIPPED; patch_status $O_MULTI1 DELIVERED
O_MULTI2=$(checkout 27 "$P1,1" "$P5,1" "$P12,3")
patch_status $O_MULTI2 PACKED; patch_status $O_MULTI2 SHIPPED; patch_status $O_MULTI2 DELIVERED

echo ""
echo "=== Demo seed complete ==="
echo "Categories: demo-electronics, demo-clothing, demo-home, demo-sports"
echo "13 products (DEMO-* SKUs), 3 warehouses, stock across all locations"
echo ""
echo "Orders by state:"
echo "  CONFIRMED=$O_CONFIRMED"
echo "  PACKED=$O_PACKED"
echo "  SHIPPED=$O_SHIPPED"
echo "  DELIVERED=$O_DELIVERED"
echo "  RETURNED=$O_RETURNED"
echo "  CANCELLED=$O_CANCELLED"
echo "  MULTI-ITEM #1 (4 products)=$O_MULTI1"
echo "  MULTI-ITEM #2 (3 products)=$O_MULTI2"
echo ""
echo "For CREATED state: restart app and run: curl -u admin:admin123 -X POST '$BASE/api/admin/demo/seed?force=true'"
