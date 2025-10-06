# 📘 K6 Load Test for Books API


This K6 test simulates users logging into the Books API and retrieving books by genre.  
It evaluates both **quantity metrics** (how many requests and users the system can handle) and **quality metrics** (response time, error rates, correctness of responses).

---

## 🗂️ Data Sources

The test loads data from two CSV files using the [`SharedArray`](https://k6.io/docs/javascript-api/k6-data/sharedarray/):

| File | Purpose | Example Fields |
|------|----------|----------------|
| `books_data.csv` | Contains book genres for GET requests | `title,authors,genre,description,isbn` |
| `user_data.csv` | Contains user credentials for POST request | `username,password` |

---


## ⚙️ Test Configuration

### Load Stages (Quantity Tests)

```js
stages: [
  { duration: "10s", target: 5 },  // ramp up to 5 virtual users
  { duration: "20s", target: 10 }, // steady load of 10 users
  { duration: "10s", target: 0 },  // ramp down
]
thresholds: {
  http_req_failed: ["rate<0.3"],      // <30% requests can fail
  http_req_duration: ["p(95)<600"],   // 95% of requests under 600ms
}
```
These stages test quantity — how the system behaves as concurrent user count increases.
It measures throughput, server stability, and scaling behavior.

---

## 🧪 Main Test Logic (Quality Tests)


### POST /login

Each virtual user:
* Picks a random user from user_data.csv
* Sends credentials to /api/public/login
* Checks for:
  * Status code 200
* Presence of the Authorization header (authentication success)
```js
const loginOK = check(loginRes, {
    "POST /login - Valid User Login Succeeds Status 200 ": (r) => r.status === 200 ,
    "POST /login - Contains Token": (r) => r.headers.Authorization,
  });
```
If login fails or no token is returned, the user session stops early.

### GET /books?genre=...
After a successful login:
* Picks a random book genre from books_data.csv
* Sends a GET request to /api/books?genre={genre}
* Checks for:
  * Status code 200
* Response body contains the requested genre
```js
check(getRes, {
  "GET /books - Valid Book Status 200": (r) => r.status === 200,
  "GET /books - Book Contains Genre": (r) => r.body && r.body.includes(book.genre),
});
```

## 🧩 Putting It Together

| Aspect   | Focus                              | Example in Script                                      |
|-----------|------------------------------------|--------------------------------------------------------|
| **Quality**  | Is it working correctly?            | `check()` assertions for status, headers, body         |
| **Quantity** | How well does it perform under load? | `stages` (VUs), `thresholds` (latency/failure rate)    |

## 🧾 Report Files

| File | Description |
|------|--------------|
| [1summary.html](/tests/1summary.html) | Results from the first test execution |
| [2summary.html](/tests/2summary.html) | Results from the second run |
| [3summary.html](/tests/3summary.html) | Results from the third run |

Tree tests were made because the gather of data is made with Random.

Each summary file contains:

| Category | Metric / Section | Why It’s Useful |
|-----------|------------------|-----------------|
| **Reliability** | `http_req_failed`, failed requests | Detects instability and backend errors |
| **Speed** | `http_req_duration` (p95, p99) | Shows how fast your API responds for most users |
| **Correctness** | Checks (pass/fail) | Confirms endpoints work correctly under load |
| **Performance SLA** | Threshold results | Automatic pass/fail per performance target |
| **Scalability** | VUs, iterations | Understand how load affects performance |
| **Capacity** | Requests per second | Measure how much traffic your system can handle |


## ⚠️ Why Some Tests Fail

Occasional failures (especially on **POST** requests) can occur due to **user does not exits** and the thresholds on metrics 'http_req_failed' is crossed but is normal. In this test there are only 5 user and 1 is fake so if the threshold is <30% it will would be good. 

---

## 🚨 Common Symptoms

- `POST /login` returns **401** (unauthorise error)  