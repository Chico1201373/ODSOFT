import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

// ======================================
// Load CSV Data
// ======================================
const books = new SharedArray("books", function () {
  const csv = open("/tests/books_data.csv");
  return papaparse.parse(csv, { header: true }).data;
});

const users = new SharedArray("users", function () {
  const csv = open("/tests/user_data.csv");
  return papaparse.parse(csv, { header: true }).data;
});

// ======================================
// Config via Environment Variables
// ======================================
const USERS = __ENV.USERS ? parseInt(__ENV.USERS) : 3000; 
const SUMMARY_NAME = __ENV.SUMMARY_NAME || "Summary.html";

export const options = {
  stages: [
    { duration: "10s", target: USERS / 10 },  // warm-up
    { duration: "30s", target: USERS },       // load
    { duration: "10s", target: 0 },           // ramp-down
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<600"],
    "checks{type:login_success}": ["rate>0.95"],
    "checks{type:book_success}": ["rate>0.95"],
  },
};

// ======================================
// Test Scenario
// ======================================
export default function () {
  // --- Random user login ---
  const user = users[Math.floor(Math.random() * users.length)];
  const loginPayload = JSON.stringify({
    username: user.username,
    password: user.password,
  });

  const loginRes = http.post(
    "http://books-api:8081/api/public/login",
    loginPayload,
    { headers: { "Content-Type": "application/json", "Accept": "application/json" } }
  );

  const loginChecks = check(loginRes, {
    "POST /login - Success 200": (r) => r.status === 200,
    "POST /login - Failed (401 Unauthorized)": (r) => r.status === 401,
    "POST /login - Contains Token": (r) => r.status === 200 && r.headers.Authorization,
  });

  const token = loginRes.headers["Authorization"];
  if (loginRes.status !== 200 || !token) {
    sleep(1);
    return;
  }

  sleep(0.5);

  // --- GET /books ---
  const book = books[Math.floor(Math.random() * books.length)];
  const genre = encodeURIComponent(book.genre);
  const getRes = http.get(`http://books-api:8081/api/books?genre=${genre}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
  });

  check(getRes, {
    "GET /books - Valid Book Status 200": (r) => r.status === 200,
    "GET /books - Book Contains Genre": (r) => r.body && r.body.includes(book.genre),
  });

  sleep(1);
}

// ======================================
// HTML Summary Report
// ======================================
export function handleSummary(data) {
  return {
    [`/tests/${SUMMARY_NAME}`]: htmlReport(data),
  };
}
