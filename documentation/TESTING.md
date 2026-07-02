# ReY Platform Testing Specifications

This document outlines the testing layout and test suites definitions.

## 1. Backend Testing

### Unit Testing Endpoint Schema
We verify:
1. Authorization tokens validations.
2. Form fields schema verifications (CORS headers, payload requirements).
3. Session revoking logs.

To execute tests on the worker:
```bash
cd backend
npm run test
```

---

## 2. Website Testing

### Jest / Vitest UI Testing
We verify:
1. Lock routes and session checks redirect.
2. Chat bubble formatting render (Markdown matches).
3. Responsive navbar adapts correctly on mobile screen ratios.

To run:
```bash
cd website
npm run test
```

---

## 3. Android Testing

### Room / SQLite Test Suites
We verify:
1. Encryption keys PBKDF2 matches.
2. Cache insertions during folder modifications correctly trigger uploads.
3. SMS dispatch logs database entries.

To compile and verify testing scripts:
```bash
cd android
./gradlew test
```
