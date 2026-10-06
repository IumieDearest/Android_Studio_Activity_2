// =====================================================================
//  SERVER (server/server.js)  -  STEP 2: LOGIN + OTP
//
//  Routes:
//    GET  /            -> check if the server is running (open it in a browser)
//    POST /login       -> check the username and password, then create an OTP
//    POST /resend-otp  -> create a brand new OTP (the old one stops working)
//    POST /verify-otp  -> check the OTP the user typed
//
//  Accounts and OTPs are saved ONLY in memory, as instructed.
//  They reset whenever the server restarts or Render puts it to sleep.
// =====================================================================

// Express is a library that makes it easy to create a web server in Node.js
const express = require("express");

// crypto is built into Node.js. We use it to make random OTP numbers.
const crypto = require("crypto");

const app = express();

// Lets the server read JSON data sent by the Android app
app.use(express.json());

// Render gives us the port number in process.env.PORT.
// When running on your own laptop, it uses 3000 instead.
const PORT = process.env.PORT || 3000;

// How long an OTP stays valid: 30 seconds (written in milliseconds)
const OTP_DURATION_MS = 30 * 1000;

// ---------------------------------------------------------------------
//  REGISTERED ACCOUNTS
//  Only these accounts can log in. Add more by copying one { ... } line.
// ---------------------------------------------------------------------
const users = [
  { username: "student", password: "password123" },
  { username: "admin", password: "admin1234" }
];

// ---------------------------------------------------------------------
//  SAVED OTPs (in memory)
//  Each key is a username, and each value is the code and when it expires.
//  Example:  otps["student"] = { code: "4821", expiresAt: 1790000030000 }
// ---------------------------------------------------------------------
const otps = {};

// ---------------------------------------------------------------------
//  HELPER: creates a new 4-digit OTP for a user and saves it in memory.
//  Saving a new one automatically replaces the old one.
// ---------------------------------------------------------------------
function createOtp(username) {
  // randomInt(1000, 10000) gives a number from 1000 to 9999, so it's always 4 digits
  const code = crypto.randomInt(1000, 10000).toString();

  // Date.now() is the current time in milliseconds,
  // so the code expires 30 seconds from right now
  otps[username] = {
    code: code,
    expiresAt: Date.now() + OTP_DURATION_MS
  };

  // Also shows the code in Render's Logs tab, which is handy for checking
  console.log("OTP for " + username + ": " + code);

  return code;
}

// ---------------------------------------------------------------------
//  GET /
// ---------------------------------------------------------------------
app.get("/", (req, res) => {
  res.json({ message: "Server is running!" });
});

// ---------------------------------------------------------------------
//  POST /login
//  Expects: { username, password }
// ---------------------------------------------------------------------
app.post("/login", (req, res) => {
  // "|| ''" means: if a value is missing, use an empty string instead
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // 1. Check that no field is empty
  if (username === "" || password === "") {
    // 400 = "Bad Request"
    return res.status(400).json({ success: false, message: "Please enter your username and password." });
  }

  // 2. Look for a registered user with this username (not case-sensitive)
  const user = users.find(u => u.username.toLowerCase() === username.toLowerCase());

  // 3. Reject if the user isn't registered or the password is wrong
  if (!user || user.password !== password) {
    // 401 = "Unauthorized"
    return res.status(401).json({ success: false, message: "Incorrect username or password." });
  }

  // 4. Login is correct, so create an OTP for this user
  const otp = createOtp(user.username);

  // 5. Send back the OTP. Since we aren't sending real text messages,
  //    the app will display the code on the OTP screen.
  return res.status(200).json({
    success: true,
    message: "Login successful! Please enter your code.",
    username: user.username,
    otp: otp
  });
});

// ---------------------------------------------------------------------
//  POST /resend-otp
//  Expects: { username }
// ---------------------------------------------------------------------
app.post("/resend-otp", (req, res) => {
  const username = (req.body.username || "").trim();

  // Only registered users can get a code
  const user = users.find(u => u.username === username);
  if (!user) {
    // 404 = "Not Found"
    return res.status(404).json({ success: false, message: "Account not found. Please log in again." });
  }

  const otp = createOtp(user.username);

  return res.status(200).json({
    success: true,
    message: "A new code has been sent.",
    otp: otp
  });
});

// ---------------------------------------------------------------------
//  POST /verify-otp
//  Expects: { username, otp }
// ---------------------------------------------------------------------
app.post("/verify-otp", (req, res) => {
  const username = (req.body.username || "").trim();
  const typedCode = (req.body.otp || "").trim();

  // 1. Check that exactly 4 digits were typed
  if (!/^\d{4}$/.test(typedCode)) {
    return res.status(400).json({ success: false, message: "Please enter the 4-digit code." });
  }

  // 2. Get the saved OTP for this user
  const savedOtp = otps[username];
  if (!savedOtp) {
    return res.status(400).json({ success: false, message: "No code found. Please tap Resend." });
  }

  // 3. Check if 30 seconds have already passed
  if (Date.now() > savedOtp.expiresAt) {
    delete otps[username]; // remove the expired code from memory
    return res.status(400).json({ success: false, message: "This code has expired. Please tap Resend." });
  }

  // 4. Check if the code matches
  if (typedCode !== savedOtp.code) {
    return res.status(400).json({ success: false, message: "Incorrect code. Please try again." });
  }

  // 5. Correct! Delete the code so it can't be used again.
  delete otps[username];

  return res.status(200).json({ success: true, message: "Verified! Welcome, " + username + "." });
});

// ---------------------------------------------------------------------
//  Start the server
// ---------------------------------------------------------------------
app.listen(PORT, () => {
  console.log("Server is running on port " + PORT);
});