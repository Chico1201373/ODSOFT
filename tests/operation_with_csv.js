import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import papaparse from 'https://jslib.k6.io/papaparse/5.1.1/index.js';
import { SharedArray } from 'k6/data';

// ======================================
// Load the CSV of books genra
// ======================================
const books = new SharedArray('books', function() {
  const csv = open('/tests/books_data.csv'); // path inside your Docker volume
  return papaparse.parse(csv, { header: true }).data;
});

// ======================================
// Test configuration
// ======================================
export let options = {
  stages: [
    { duration: '5s', target: 5 },
    { duration: '5s', target: 5 },
    { duration: '5s', target: 0 },
  ],
};

// ======================================
// Setup — login once to get token
// ======================================
export function setup() {
  const loginPayload = JSON.stringify({
    username: "manuel@gmail.com",
    password: "Manuelino123!"
  });

  const res = http.post('http://books-api:8080/api/public/login', loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  check(res, {
    'login succeeded': (r) => r.status === 200,
  });

  const token = res.headers.Authorization

  if (!token) {
    console.error(" Login response:", res.body);
    throw new Error(" No token returned from login");
  }

  return { token };
}

// ======================================
// Main test — query /books?genre=
// ======================================
export default function (data) {
  const token = data.token;

  for (let i = 0; i < books.length; i++) {
    const book = books[i];
    const genre = encodeURIComponent(book.genre);

    const res = http.get(`http://books-api:8080/api/books?genre=${genre}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
    });

    check(res, {
      'status is 200': (r) => r.status === 200,
      'response contains book genre': (r) => r.body && r.body.includes(book.genre),
    });

    sleep(1);
  }
}


// ======================================
// HTML summary report
// ======================================
export function handleSummary(data) {
  return {
    "/tests/summary.html": htmlReport(data),
  };
}
