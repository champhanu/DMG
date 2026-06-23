#!/usr/bin/env bash
# Seeds rich demo catalog, inventory, and orders in every state for video demos.
# Requires the app to be running on localhost:8080.

set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
ADMIN="${DEMO_ADMIN:-admin:admin123}"

echo "Seeding demo data via $BASE/api/admin/demo/seed ..."
curl -s -u "$ADMIN" -X POST "$BASE/api/admin/demo/seed?force=${FORCE:-false}" | python3 -m json.tool

echo ""
echo "Done! Try these in your demo:"
echo "  curl -u admin:admin123 $BASE/api/categories"
echo "  curl -u admin:admin123 '$BASE/api/products?search=DEMO'"
echo "  curl -u customer:customer123 '$BASE/api/orders?customerId=8'"
