export const SERVICES = {
  USER: {
    name: 'User & Identity Service',
    defaultPort: 8081,
    defaultUrl: 'http://localhost:8081',
    healthPath: '/actuator/health',
    description: 'Stateless JWT, BCrypt, RBAC, Sliding-Window Redis Rate Limiting'
  },
  CATALOG: {
    name: 'Restaurant Catalog Service',
    defaultPort: 8082,
    defaultUrl: 'http://localhost:8082',
    healthPath: '/actuator/health',
    description: 'Menu Catalog, EntityGraph N+1 Prevention, 15-min Redis Cache'
  },
  ORDER: {
    name: 'Order Lifecycle Service',
    defaultPort: 8083,
    defaultUrl: 'http://localhost:8083',
    healthPath: '/actuator/health',
    description: 'Order State Machine, Optimistic Locking, Outbox Sagas, Resilience4j'
  },
  DISPATCH: {
    name: 'Dispatch & Live Tracking Service',
    defaultPort: 8084,
    defaultUrl: 'http://localhost:8084',
    healthPath: '/actuator/health',
    wsUrl: 'ws://localhost:8084/ws-tracking',
    description: 'Redis Geospatial Dispatching, STOMP WebSockets, Pub/Sub Backplane'
  }
} as const;

export const QUERY_KEYS = {
  HEALTH: ['services', 'health'] as const,
  AUTH_ME: ['auth', 'me'] as const,
  RESTAURANTS: ['catalog', 'restaurants'] as const,
  RESTAURANT: (id: string) => ['catalog', 'restaurant', id] as const,
  ORDERS: ['orders'] as const,
  ORDER: (id: string) => ['orders', id] as const,
  TRACKING: (orderId: string) => ['tracking', orderId] as const,
  NEARBY_RIDERS: (lat: number, lng: number) => ['riders', 'nearby', lat, lng] as const
} as const;
