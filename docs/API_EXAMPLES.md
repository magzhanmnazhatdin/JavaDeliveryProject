# API Examples

This document provides practical examples of API requests for all microservices using cURL and HTTPie.

---

## Table of Contents

- [Authentication](#authentication)
- [User Service](#user-service)
- [Restaurant Service](#restaurant-service)
- [Order Service](#order-service)
- [Delivery Service](#delivery-service)

---

## Authentication

### Get Access Token

```bash
# Using cURL
curl -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=customer1" \
  -d "password=password123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "scope": "openid profile email"
}
```

### Refresh Token

```bash
curl -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token" \
  -d "client_id=delivery-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "refresh_token=YOUR_REFRESH_TOKEN"
```

### Set Token Variable (for subsequent requests)

```bash
# Save token to variable
TOKEN=$(curl -s -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "client_secret=YOUR_CLIENT_SECRET" \
  -d "username=customer1" \
  -d "password=password123" | jq -r '.access_token')

echo $TOKEN
```

---

## User Service

Base URL: `http://localhost:8080/api` (via Gateway) or `http://localhost:8083/api` (direct)

### Get Current User Profile

```bash
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "id": 1,
  "keycloakId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "customer1@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890",
  "role": "CUSTOMER",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Update User Profile

```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Smith",
    "phone": "+1987654321"
  }'
```

### Get All Users (Admin Only)

```bash
curl -X GET http://localhost:8080/api/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

---

### Address Management

#### Add New Address

```bash
curl -X POST http://localhost:8080/api/addresses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Home",
    "street": "123 Main Street, Apt 4B",
    "city": "New York",
    "postalCode": "10001",
    "country": "USA",
    "latitude": 40.7128,
    "longitude": -74.0060,
    "isDefault": true
  }'
```

**Response:**
```json
{
  "id": 1,
  "label": "Home",
  "street": "123 Main Street, Apt 4B",
  "city": "New York",
  "postalCode": "10001",
  "country": "USA",
  "latitude": 40.7128,
  "longitude": -74.0060,
  "isDefault": true,
  "createdAt": "2024-01-15T10:30:00"
}
```

#### Get My Addresses

```bash
curl -X GET http://localhost:8080/api/addresses \
  -H "Authorization: Bearer $TOKEN"
```

#### Update Address

```bash
curl -X PUT http://localhost:8080/api/addresses/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "label": "Work",
    "street": "456 Business Ave, Floor 10",
    "city": "New York",
    "postalCode": "10002"
  }'
```

#### Set Default Address

```bash
curl -X POST http://localhost:8080/api/addresses/2/set-default \
  -H "Authorization: Bearer $TOKEN"
```

#### Delete Address

```bash
curl -X DELETE http://localhost:8080/api/addresses/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

### User Preferences

#### Get Preferences

```bash
curl -X GET http://localhost:8080/api/preferences \
  -H "Authorization: Bearer $TOKEN"
```

**Response:**
```json
{
  "id": 1,
  "emailNotifications": true,
  "pushNotifications": true,
  "smsNotifications": false,
  "preferredLanguage": "en",
  "preferredCurrency": "USD"
}
```

#### Update Preferences

```bash
curl -X PUT http://localhost:8080/api/preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "emailNotifications": true,
    "pushNotifications": false,
    "smsNotifications": true,
    "preferredLanguage": "ru",
    "preferredCurrency": "RUB"
  }'
```

---

### Favorite Restaurants

#### Get Favorite Restaurants

```bash
curl -X GET http://localhost:8080/api/favorites/restaurants \
  -H "Authorization: Bearer $TOKEN"
```

#### Add to Favorites

```bash
curl -X POST http://localhost:8080/api/favorites/restaurants/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### Remove from Favorites

```bash
curl -X DELETE http://localhost:8080/api/favorites/restaurants/1 \
  -H "Authorization: Bearer $TOKEN"
```

---

## Restaurant Service

Base URL: `http://localhost:8080/api` (via Gateway) or `http://localhost:8082/api` (direct)

### Get All Restaurants (Public)

```bash
curl -X GET "http://localhost:8080/api/restaurants?page=0&size=10"
```

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Pizza Palace",
      "description": "Best Italian pizza in town",
      "address": "789 Pizza Street",
      "city": "New York",
      "phone": "+1555123456",
      "email": "info@pizzapalace.com",
      "cuisineType": "Italian",
      "latitude": 40.7580,
      "longitude": -73.9855,
      "averageRating": 4.5,
      "totalReviews": 256,
      "openingHours": "10:00-22:00",
      "isActive": true
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0
}
```

### Get Restaurant by ID (Public)

```bash
curl -X GET http://localhost:8080/api/restaurants/1
```

### Search Restaurants (Public)

```bash
# Search by name
curl -X GET "http://localhost:8080/api/restaurants/search?name=pizza"

# Search by cuisine type
curl -X GET "http://localhost:8080/api/restaurants/search?cuisineType=Italian"

# Search by city
curl -X GET "http://localhost:8080/api/restaurants/search?city=New%20York"

# Combined search
curl -X GET "http://localhost:8080/api/restaurants/search?name=pizza&city=New%20York&cuisineType=Italian"
```

### Find Nearby Restaurants (Public)

```bash
curl -X GET "http://localhost:8080/api/restaurants/nearby?latitude=40.7128&longitude=-74.0060&radiusKm=5"
```

### Create Restaurant (Restaurant Owner/Admin)

```bash
curl -X POST http://localhost:8080/api/restaurants \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sushi Express",
    "description": "Fresh Japanese cuisine",
    "address": "321 Sushi Lane",
    "city": "New York",
    "phone": "+1555789012",
    "email": "contact@sushiexpress.com",
    "cuisineType": "Japanese",
    "latitude": 40.7489,
    "longitude": -73.9680,
    "openingHours": "11:00-23:00"
  }'
```

### Update Restaurant

```bash
curl -X PUT http://localhost:8080/api/restaurants/1 \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sushi Express Premium",
    "description": "Premium fresh Japanese cuisine",
    "openingHours": "10:00-00:00"
  }'
```

---

### Menu Items

#### Get Restaurant Menu (Public)

```bash
curl -X GET http://localhost:8080/api/restaurants/1/menu
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "Margherita Pizza",
    "description": "Classic tomato sauce, mozzarella, fresh basil",
    "price": 14.99,
    "category": "Pizza",
    "imageUrl": "https://example.com/margherita.jpg",
    "isAvailable": true,
    "isVegetarian": true,
    "isVegan": false,
    "preparationTime": 20
  },
  {
    "id": 2,
    "name": "Pepperoni Pizza",
    "description": "Tomato sauce, mozzarella, pepperoni",
    "price": 16.99,
    "category": "Pizza",
    "isAvailable": true,
    "isVegetarian": false,
    "isVegan": false,
    "preparationTime": 20
  }
]
```

#### Add Menu Item

```bash
curl -X POST http://localhost:8080/api/restaurants/1/menu \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Hawaiian Pizza",
    "description": "Ham, pineapple, mozzarella",
    "price": 17.99,
    "category": "Pizza",
    "isAvailable": true,
    "isVegetarian": false,
    "isVegan": false,
    "preparationTime": 25
  }'
```

#### Update Menu Item

```bash
curl -X PUT http://localhost:8080/api/menu-items/1 \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 15.99,
    "isAvailable": true
  }'
```

#### Delete Menu Item

```bash
curl -X DELETE http://localhost:8080/api/menu-items/1 \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

---

### Restaurant Orders

#### Get Pending Orders

```bash
curl -X GET http://localhost:8080/api/restaurant-orders/restaurant/1/pending \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

#### Get Active Orders

```bash
curl -X GET http://localhost:8080/api/restaurant-orders/restaurant/1/active \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

#### Accept Order

```bash
curl -X POST http://localhost:8080/api/restaurant-orders/1/accept \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "estimatedPrepTime": 30
  }'
```

#### Reject Order

```bash
curl -X POST http://localhost:8080/api/restaurant-orders/1/reject \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Kitchen is too busy"
  }'
```

#### Start Preparing

```bash
curl -X POST http://localhost:8080/api/restaurant-orders/1/start-preparing \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

#### Mark as Ready

```bash
curl -X POST http://localhost:8080/api/restaurant-orders/1/ready \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

#### Mark as Picked Up

```bash
curl -X POST http://localhost:8080/api/restaurant-orders/1/picked-up \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"
```

---

## Order Service

Base URL: `http://localhost:8080/api` (via Gateway) or `http://localhost:8081/api` (direct)

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "1",
    "deliveryAddress": "123 Main Street, Apt 4B, New York, NY 10001",
    "deliveryLatitude": 40.7128,
    "deliveryLongitude": -74.0060,
    "specialInstructions": "Please ring the doorbell twice",
    "items": [
      {
        "menuItemId": "1",
        "name": "Margherita Pizza",
        "quantity": 2,
        "price": 14.99,
        "specialInstructions": "Extra cheese"
      },
      {
        "menuItemId": "3",
        "name": "Garlic Bread",
        "quantity": 1,
        "price": 5.99
      }
    ]
  }'
```

**Response:**
```json
{
  "id": 1,
  "customerId": "550e8400-e29b-41d4-a716-446655440000",
  "restaurantId": "1",
  "status": "PENDING",
  "totalPrice": 35.97,
  "deliveryAddress": "123 Main Street, Apt 4B, New York, NY 10001",
  "deliveryLatitude": 40.7128,
  "deliveryLongitude": -74.0060,
  "specialInstructions": "Please ring the doorbell twice",
  "items": [
    {
      "id": 1,
      "menuItemId": "1",
      "name": "Margherita Pizza",
      "quantity": 2,
      "price": 14.99,
      "specialInstructions": "Extra cheese"
    },
    {
      "id": 2,
      "menuItemId": "3",
      "name": "Garlic Bread",
      "quantity": 1,
      "price": 5.99
    }
  ],
  "createdAt": "2024-01-15T12:00:00",
  "updatedAt": "2024-01-15T12:00:00"
}
```

### Get Order by ID

```bash
curl -X GET http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Get My Orders

```bash
curl -X GET http://localhost:8080/api/orders/my-orders \
  -H "Authorization: Bearer $TOKEN"
```

### Get Orders by Status (Admin)

```bash
curl -X GET http://localhost:8080/api/orders/status/PENDING \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Update Order Status

```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CONFIRMED"
  }'
```

### Cancel Order

```bash
curl -X POST http://localhost:8080/api/orders/1/cancel \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Changed my mind"
  }'
```

---

### Payments

#### Process Payment

```bash
curl -X POST http://localhost:8080/api/payments/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethod": "CREDIT_CARD",
    "cardNumber": "4111111111111111",
    "expiryMonth": 12,
    "expiryYear": 2025,
    "cvv": "123"
  }'
```

**Response:**
```json
{
  "id": 1,
  "orderId": 1,
  "amount": 35.97,
  "status": "COMPLETED",
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "txn_abc123xyz",
  "createdAt": "2024-01-15T12:05:00"
}
```

#### Get Payment by ID

```bash
curl -X GET http://localhost:8080/api/payments/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### Get Payment by Order ID

```bash
curl -X GET http://localhost:8080/api/payments/order/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### Get My Payments

```bash
curl -X GET http://localhost:8080/api/payments/my-payments \
  -H "Authorization: Bearer $TOKEN"
```

#### Refund Payment (Admin)

```bash
curl -X POST http://localhost:8080/api/payments/1/refund \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reason": "Customer requested refund"
  }'
```

---

## Delivery Service

Base URL: `http://localhost:8080/api` (via Gateway) or `http://localhost:8084/api` (direct)

### Couriers

#### Register Courier (Admin)

```bash
curl -X POST http://localhost:8080/api/couriers \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "keycloakId": "courier-uuid-here",
    "name": "Mike Johnson",
    "phone": "+1555111222",
    "email": "mike.courier@example.com",
    "vehicleType": "BICYCLE",
    "licensePlate": null
  }'
```

**Response:**
```json
{
  "id": 1,
  "keycloakId": "courier-uuid-here",
  "name": "Mike Johnson",
  "phone": "+1555111222",
  "email": "mike.courier@example.com",
  "vehicleType": "BICYCLE",
  "status": "AVAILABLE",
  "isAvailable": true,
  "averageRating": 0.0,
  "totalDeliveries": 0
}
```

#### Get Courier by ID

```bash
curl -X GET http://localhost:8080/api/couriers/1 \
  -H "Authorization: Bearer $COURIER_TOKEN"
```

#### Get Available Couriers (Admin)

```bash
curl -X GET http://localhost:8080/api/couriers/available \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Update Courier Status

```bash
curl -X PUT http://localhost:8080/api/couriers/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "ON_DELIVERY"
  }'
```

#### Update Courier Location

```bash
curl -X PUT http://localhost:8080/api/couriers/1/location \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 40.7489,
    "longitude": -73.9680
  }'
```

---

### Deliveries

#### Create Delivery (Admin/Restaurant)

```bash
curl -X POST http://localhost:8080/api/deliveries \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "1",
    "customerId": "customer-uuid",
    "restaurantId": "1",
    "pickupAddress": "789 Pizza Street, New York",
    "pickupLatitude": 40.7580,
    "pickupLongitude": -73.9855,
    "deliveryAddress": "123 Main Street, New York",
    "deliveryLatitude": 40.7128,
    "deliveryLongitude": -74.0060,
    "notes": "Call when arriving"
  }'
```

#### Get Delivery by ID

```bash
curl -X GET http://localhost:8080/api/deliveries/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### Get Delivery by Order ID

```bash
curl -X GET http://localhost:8080/api/deliveries/order/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### Get Courier's Deliveries

```bash
curl -X GET http://localhost:8080/api/deliveries/courier/1 \
  -H "Authorization: Bearer $COURIER_TOKEN"
```

#### Assign Courier Manually (Admin)

```bash
curl -X POST http://localhost:8080/api/deliveries/1/assign \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courierId": 1
  }'
```

#### Auto-assign Courier (Admin)

```bash
curl -X POST http://localhost:8080/api/deliveries/1/assign-auto \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Update Delivery Status (Courier)

```bash
# Mark as picked up
curl -X PUT http://localhost:8080/api/deliveries/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PICKED_UP"
  }'

# Mark as in transit
curl -X PUT http://localhost:8080/api/deliveries/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "IN_TRANSIT"
  }'

# Mark as delivered
curl -X PUT http://localhost:8080/api/deliveries/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED"
  }'
```

---

## Complete Order Flow Example

Here's a complete example of an order from creation to delivery:

```bash
# 1. Customer gets token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "username=customer1" \
  -d "password=password123" | jq -r '.access_token')

# 2. Customer browses restaurants
curl -X GET "http://localhost:8080/api/restaurants/search?cuisineType=Italian"

# 3. Customer views menu
curl -X GET http://localhost:8080/api/restaurants/1/menu

# 4. Customer creates order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "restaurantId": "1",
    "deliveryAddress": "123 Main Street, New York",
    "items": [{"menuItemId": "1", "name": "Margherita Pizza", "quantity": 2, "price": 14.99}]
  }')
ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')

# 5. Customer processes payment
curl -X POST http://localhost:8080/api/payments/process \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"orderId\": $ORDER_ID, \"paymentMethod\": \"CREDIT_CARD\"}"

# 6. Restaurant accepts order
RESTAURANT_TOKEN=$(curl -s -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "username=restaurant1" \
  -d "password=password123" | jq -r '.access_token')

curl -X POST http://localhost:8080/api/restaurant-orders/1/accept \
  -H "Authorization: Bearer $RESTAURANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estimatedPrepTime": 30}'

# 7. Restaurant marks as ready
curl -X POST http://localhost:8080/api/restaurant-orders/1/ready \
  -H "Authorization: Bearer $RESTAURANT_TOKEN"

# 8. Courier picks up and delivers
COURIER_TOKEN=$(curl -s -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "username=courier1" \
  -d "password=password123" | jq -r '.access_token')

curl -X PUT http://localhost:8080/api/deliveries/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "PICKED_UP"}'

curl -X PUT http://localhost:8080/api/deliveries/1/status \
  -H "Authorization: Bearer $COURIER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status": "DELIVERED"}'

# 9. Customer checks order status
curl -X GET http://localhost:8080/api/orders/$ORDER_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## Error Responses

### 400 Bad Request

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid order data: items cannot be empty",
  "path": "/api/orders"
}
```

### 401 Unauthorized

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required",
  "path": "/api/orders"
}
```

### 403 Forbidden

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied: insufficient privileges",
  "path": "/api/users"
}
```

### 404 Not Found

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Order not found with id: 999",
  "path": "/api/orders/999"
}
```

### 429 Too Many Requests

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again later.",
  "path": "/api/orders"
}
```

### 503 Service Unavailable (Circuit Breaker Open)

```json
{
  "timestamp": "2024-01-15T12:00:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Order service is temporarily unavailable",
  "path": "/api/orders"
}
```

---

## Postman Collection

Import the following environment variables in Postman:

```json
{
  "variables": [
    {"key": "baseUrl", "value": "http://localhost:8080"},
    {"key": "keycloakUrl", "value": "http://localhost:8180"},
    {"key": "realm", "value": "delivery-realm"},
    {"key": "clientId", "value": "delivery-api"},
    {"key": "clientSecret", "value": "YOUR_SECRET"},
    {"key": "accessToken", "value": ""}
  ]
}
```
