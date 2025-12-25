#!/bin/bash

# Keycloak Setup Script for Delivery Platform
# This script imports the delivery-realm configuration into Keycloak

KEYCLOAK_URL="http://localhost:8180"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
REALM_FILE="delivery-realm.json"

echo "=========================================="
echo "Keycloak Setup for Delivery Platform"
echo "=========================================="

# Wait for Keycloak to be ready
echo "Waiting for Keycloak to be ready..."
until curl -s "$KEYCLOAK_URL/health/ready" > /dev/null 2>&1; do
    echo "Keycloak is not ready yet. Waiting..."
    sleep 5
done
echo "Keycloak is ready!"

# Get admin token
echo "Getting admin access token..."
TOKEN=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=$ADMIN_USER" \
    -d "password=$ADMIN_PASSWORD" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
    echo "ERROR: Failed to get admin token. Please check admin credentials."
    exit 1
fi

echo "Admin token obtained successfully!"

# Check if realm already exists
echo "Checking if delivery-realm already exists..."
REALM_EXISTS=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$KEYCLOAK_URL/admin/realms/delivery-realm")

if [ "$REALM_EXISTS" == "200" ]; then
    echo "Realm 'delivery-realm' already exists. Deleting it first..."
    curl -s -X DELETE \
        -H "Authorization: Bearer $TOKEN" \
        "$KEYCLOAK_URL/admin/realms/delivery-realm"
    echo "Existing realm deleted."
fi

# Import realm
echo "Importing delivery-realm..."
IMPORT_RESULT=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d @"$REALM_FILE" \
    "$KEYCLOAK_URL/admin/realms")

if [ "$IMPORT_RESULT" == "201" ]; then
    echo "Realm 'delivery-realm' imported successfully!"
else
    echo "ERROR: Failed to import realm. HTTP status: $IMPORT_RESULT"
    exit 1
fi

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="
echo ""
echo "Keycloak Admin Console: $KEYCLOAK_URL"
echo "Realm: delivery-realm"
echo ""
echo "Test Users:"
echo "  - customer1 / password123 (CUSTOMER role)"
echo "  - restaurant_owner1 / password123 (RESTAURANT_OWNER role)"
echo "  - courier1 / password123 (COURIER role)"
echo "  - admin / admin123 (ADMIN role)"
echo ""
echo "OAuth2 Endpoints:"
echo "  - Token: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/token"
echo "  - Auth: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/auth"
echo "  - JWKS: $KEYCLOAK_URL/realms/delivery-realm/protocol/openid-connect/certs"
echo ""
