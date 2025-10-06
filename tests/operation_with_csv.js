import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";

// ======================================
// Load the CSV of books and user
// ======================================
const books = new SharedArray("books", function () {
  const csv = open("/tests/books_data.csv"); // path inside your Docker volume
  return papaparse.parse(csv, { header: true }).data;
});
// --- Load users for POST /login ---
const users = new SharedArray("users", function () {
  const csv = open("/tests/user_data.csv");
  return papaparse.parse(csv, { header: true }).data;
});

// ======================================
// Test configuration (Quantity + Quality thresholds)
// ======================================
export const options = {
  stages: [
    { duration: "10s", target: 5 }, // ramp up
    { duration: "20s", target: 10 }, // steady load
    { duration: "10s", target: 0 }, // ramp down
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"], // <0.1% failures
    http_req_duration: ["p(95)<600"], // 95% requests under 600ms
  },
};

// ======================================
// Main Test — POST login + GET books
// ======================================
export default function () {
  // --- Random user login ---
  const user = users[Math.floor(Math.random() * users.length)];
  const loginPayload = JSON.stringify({
    username: user.username,
    password: user.password,
  });

const loginRes = http.post(
  "http://books-api:8080/api/public/login",
  loginPayload,
  { headers: { "Content-Type": "application/json" } }
);

  // Quality checks for login
  const loginOK = check(loginRes, {
    "POST /login - Valid User Login Succeeds Status 200 ": (r) => r.status === 200 ,
    "POST /login - Contains Token": (r) => r.headers.Authorization,
   });

   

  // Extract token for authenticated requests
  const token = loginRes.headers["Authorization"];

  if (!loginOK || !token) {
    sleep(1);
    return;
  }

  sleep(0.5);

  // --- GET /books?genre= test ---
  const book = books[Math.floor(Math.random() * books.length)];
  const genre = encodeURIComponent(book.genre);
  const getRes = http.get(`http://books-api:8080/api/books?genre=${genre}`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
      "Content-Type": "application/json",
    },
  });
  if (getRes.status != 200) {
  }
  check(getRes, {
    "GET /books - Valid Book Status 200": (r) => r.status === 200,
    "GET /books - Book Contains Genre": (r) => r.body && r.body.includes(book.genre),
  });

  sleep(1);
}

// ======================================
// HTML summary report
// ======================================
export function handleSummary(data) {
  return {
    "/tests/1summary.html": htmlReport(data),
  };
}
