const express = require("express");

const crypto = require("crypto");

const app = express();

app.use(express.json());

const PORT = process.env.PORT || 3000;
const OTP_DURATION_MS = 30 * 1000;

const users = [
  { username: "student", firstName: "Test", lastName: "Student", email: "student@example.com", password: "password123" },
  { username: "admin", firstName: "Test", lastName: "Admin", email: "admin@example.com", password: "admin1234" }
];

const otps = {};

function createOtp(email) {
  const code = crypto.randomInt(1000, 10000).toString();

  otps[email] = {
    code: code,
    expiresAt: Date.now() + OTP_DURATION_MS
  };

  console.log("OTP for " + email + ": " + code);

  return code;
}

app.get("/", (req, res) => {
  res.json({ message: "Server is running!" });
});

app.post("/register", (req, res) => {
  const firstName = (req.body.firstName || "").trim();
  const lastName = (req.body.lastName || "").trim();
  const email = (req.body.email || "").trim().toLowerCase();
  const confirmEmail = (req.body.confirmEmail || "").trim().toLowerCase();
  const password = req.body.password || "";
  const confirmPassword = req.body.confirmPassword || "";

  if (firstName === "" || lastName === "" || email === "" ||
      confirmEmail === "" || password === "" || confirmPassword === "") {
    return res.status(400).json({ success: false, message: "Please fill in all fields." });
  }

  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailPattern.test(email)) {
    return res.status(400).json({ success: false, message: "Please enter a valid email address." });
  }

  if (email !== confirmEmail) {
    return res.status(400).json({ success: false, message: "Emails do not match." });
  }

  if (password.length < 8) {
    return res.status(400).json({ success: false, message: "Password must be at least 8 characters." });
  }

  if (password !== confirmPassword) {
    return res.status(400).json({ success: false, message: "Passwords do not match." });
  }

  const existingUser = users.find(u => u.email === email);
  if (existingUser) {
    return res.status(409).json({ success: false, message: "This email is already registered." });
  }

  users.push({
    username: email,
    firstName: firstName,
    lastName: lastName,
    email: email,
    password: password
  });

  console.log("New user registered: " + email);

  return res.status(201).json({
    success: true,
    message: "Account created! You can now sign in with your email."
  });
});

app.post("/login", (req, res) => {
  const username = (req.body.username || "").trim().toLowerCase();
  const password = req.body.password || "";

  if (username === "" || password === "") {
    return res.status(400).json({ success: false, message: "Please enter your username and password." });
  }

  const user = users.find(u =>
    u.username.toLowerCase() === username || u.email === username
  );

  if (!user || user.password !== password) {
    return res.status(401).json({ success: false, message: "Incorrect username or password." });
  }

  const otp = createOtp(user.email);

  return res.status(200).json({
    success: true,
    message: "Welcome, " + user.firstName + "! Please enter your code.",
    username: user.email,
    otp: otp
  });
});

app.post("/resend-otp", (req, res) => {
  const email = (req.body.username || "").trim().toLowerCase();

  const user = users.find(u => u.email === email);
  if (!user) {
    return res.status(404).json({ success: false, message: "Account not found. Please log in again." });
  }

  const otp = createOtp(user.email);

  return res.status(200).json({
    success: true,
    message: "A new code has been sent.",
    otp: otp
  });
});

app.post("/verify-otp", (req, res) => {
  const email = (req.body.username || "").trim().toLowerCase();
  const typedCode = (req.body.otp || "").trim();

  if (!/^\d{4}$/.test(typedCode)) {
    return res.status(400).json({ success: false, message: "Please enter the 4-digit code." });
  }

  const savedOtp = otps[email];
  if (!savedOtp) {
    return res.status(400).json({ success: false, message: "No code found. Please tap Resend." });
  }

  if (Date.now() > savedOtp.expiresAt) {
    return res.status(400).json({ success: false, message: "This code has expired. Please tap Resend." });
  }

  if (typedCode !== savedOtp.code) {
    return res.status(400).json({ success: false, message: "Incorrect code. Please try again." });
  }

  delete otps[email];

  const user = users.find(u => u.email === email);
  const name = user ? user.firstName : "";

  return res.status(200).json({ success: true, message: "Verified! Welcome, " + name + "." });
});

app.get("/users", (req, res) => {
  const userList = users.map(u => ({
    firstName: u.firstName,
    lastName: u.lastName,
    email: u.email
  }));

  return res.status(200).json({
    success: true,
    count: userList.length,
    users: userList
  });
});

app.listen(PORT, () => {
  console.log("Server is running on port " + PORT);
});