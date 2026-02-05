// K6 Performance Test for Web3 Community API
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Custom metrics
export let errorRate = new Rate('errors');

// Test configuration
export let options = {
  stages: [
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 200 }, // Ramp up to 200 users
    { duration: '5m', target: 200 }, // Stay at 200 users
    { duration: '2m', target: 0 },   // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.1'],      // Error rate under 10%
    errors: ['rate<0.1'],
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8080';

// Test data
const users = [
  { email: 'test1@example.com', password: 'password123' },
  { email: 'test2@example.com', password: 'password123' },
  { email: 'test3@example.com', password: 'password123' },
];

const posts = [
  { title: 'Test Post 1', content: 'This is a test post content 1', categoryId: 1 },
  { title: 'Test Post 2', content: 'This is a test post content 2', categoryId: 2 },
  { title: 'Test Post 3', content: 'This is a test post content 3', categoryId: 1 },
];

let accessToken = '';
let refreshToken = '';

export function setup() {
  // Setup - authenticate and get tokens
  const loginResponse = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify({
    email: users[0].email,
    password: users[0].password
  }), {
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (loginResponse.status === 200) {
    const tokens = loginResponse.json();
    accessToken = tokens.accessToken;
    refreshToken = tokens.refreshToken;
  }

  return {
    accessToken,
    refreshToken,
  };
}

export default function(data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.accessToken}`,
  };

  // Test 1: Get all posts
  let postsResponse = http.get(`${BASE_URL}/api/v1/posts`, { headers });
  let success = check(postsResponse, {
    'posts status is 200': (r) => r.status === 200,
    'posts response time < 500ms': (r) => r.timings.duration < 500,
  });

  errorRate.add(!success);

  // Test 2: Get categories
  let categoriesResponse = http.get(`${BASE_URL}/api/v1/categories`, { headers });
  check(categoriesResponse, {
    'categories status is 200': (r) => r.status === 200,
  });

  // Test 3: Create a new post (25% of VUs)
  if (Math.random() < 0.25) {
    const randomPost = posts[Math.floor(Math.random() * posts.length)];
    let createResponse = http.post(`${BASE_URL}/api/v1/posts`, JSON.stringify(randomPost), { headers });
    check(createResponse, {
      'create post status is 201': (r) => r.status === 201,
      'create post response time < 1000ms': (r) => r.timings.duration < 1000,
    });
  }

  // Test 4: Get user profile (50% of VUs)
  if (Math.random() < 0.5) {
    let profileResponse = http.get(`${BASE_URL}/api/v1/users/profile`, { headers });
    check(profileResponse, {
      'profile status is 200': (r) => r.status === 200,
    });
  }

  // Test 5: Get comments for a post (30% of VUs)
  if (Math.random() < 0.3 && postsResponse.json().length > 0) {
    const firstPost = postsResponse.json()[0];
    let commentsResponse = http.get(`${BASE_URL}/api/v1/posts/${firstPost.id}/comments`, { headers });
    check(commentsResponse, {
      'comments status is 200': (r) => r.status === 200,
    });
  }

  // Test 6: Search posts (20% of VUs)
  if (Math.random() < 0.2) {
    let searchResponse = http.get(`${BASE_URL}/api/v1/posts/search?q=test`, { headers });
    check(searchResponse, {
      'search status is 200': (r) => r.status === 200,
    });
  }

  sleep(1);
}

export function teardown(data) {
  // Cleanup - logout if needed
  if (data.accessToken) {
    http.post(`${BASE_URL}/api/v1/auth/logout`, JSON.stringify({
      refreshToken: data.refreshToken
    }), {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.accessToken}`,
      },
    });
  }
}