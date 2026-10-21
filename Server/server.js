const express = require("express");

const crypto = require("crypto");

// NEW: the Supabase library, used to talk to our PostgreSQL database on Supabase
const { createClient } = require("@supabase/supabase-js");

const app = express();

app.use(express.json());

const PORT = process.env.PORT || 3000;
const OTP_DURATION_MS = 30 * 1000;

// ---------------------------------------------------------------------
//  NEW: CONNECT TO SUPABASE
//  The URL and key are NOT written in the code. They are saved on Render as
//  "Environment Variables", so the secret key never ends up on GitHub.
//  process.env.NAME reads an environment variable.
// ---------------------------------------------------------------------
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY = process.env.SUPABASE_KEY;

// Only connect if both values exist, so the login routes still work without them
let supabase = null;
if (SUPABASE_URL && SUPABASE_KEY) {
  supabase = createClient(SUPABASE_URL, SUPABASE_KEY);
  console.log("Connected to Supabase");
} else {
  console.log("SUPABASE_URL or SUPABASE_KEY is missing. The /customers route will not work.");
}

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

// ---------------------------------------------------------------------
//  NEW: POST /customers  (INSERT API)
//  Saves one customer in the "customer" table on Supabase.
//  Expects: { customerid, lastname, firstname, mobileno, address }
//
//  "async" lets us use "await", which waits for Supabase to answer
//  before going to the next line.
// ---------------------------------------------------------------------
app.post("/customers", async (req, res) => {
  // 0. Stop early if the server isn't connected to Supabase
  if (!supabase) {
    // 500 = "Internal Server Error" (a problem on the server, not the user's fault)
    return res.status(500).json({ success: false, message: "Server is not connected to the database." });
  }

  // 1. Take the values out of the request.
  //    String(...) turns numbers into text so .trim() works on everything.
  //    "?? ''" means: if a value is missing, use an empty string instead.
  const customerid = String(req.body.customerid ?? "").trim();
  const lastname = String(req.body.lastname ?? "").trim();
  const firstname = String(req.body.firstname ?? "").trim();
  const mobileno = String(req.body.mobileno ?? "").trim();
  const address = String(req.body.address ?? "").trim();

  // 2. Check that no field is empty
  if (customerid === "" || lastname === "" || firstname === "" || mobileno === "" || address === "") {
    // 400 = "Bad Request"
    return res.status(400).json({ success: false, message: "Please fill in all fields: customerid, lastname, firstname, mobileno, address." });
  }

  // 3. customerid must be a whole number greater than 0 (only digits)
  if (!/^\d+$/.test(customerid) || Number(customerid) <= 0) {
    return res.status(400).json({ success: false, message: "customerid must be a whole number greater than 0." });
  }

  // 4. mobileno must be 7 to 15 digits (a "+" at the start is allowed)
  if (!/^\+?\d{7,15}$/.test(mobileno)) {
    return res.status(400).json({ success: false, message: "mobileno must contain 7 to 15 digits only." });
  }

  // 5. INSERT into the customer table.
  //    .insert() adds the row, and .select() asks Supabase to send the new row back.
  //    This is the same as the SQL:
  //    INSERT INTO customer (customerid, lastname, firstname, mobileno, address) VALUES (...);
  const { data, error } = await supabase
    .from("customer")
    .insert({
      customerid: Number(customerid),
      lastname: lastname,
      firstname: firstname,
      mobileno: mobileno,
      address: address
    })
    .select();

  // 6. If Supabase returned an error, explain it
  if (error) {
    console.log("Insert error:", error.message);

    // "23505" is PostgreSQL's code for a duplicate primary key
    if (error.code === "23505") {
      // 409 = "Conflict"
      return res.status(409).json({ success: false, message: "Customer ID " + customerid + " already exists." });
    }

    return res.status(500).json({ success: false, message: "Database error: " + error.message });
  }

  // 7. Success! 201 = "Created". Send back the saved row so we can see it in Postman.
  return res.status(201).json({
    success: true,
    message: "Customer added successfully.",
    customer: data[0]
  });
});

// ---------------------------------------------------------------------
//  NEW: GET /customers  (CUSTOMER LIST)
//  Sends back every customer from the "customer" table on Supabase,
//  sorted by customerid (1, 2, 3...). Used by the Customer List in the app.
//  This is a GET route, so you can also open it in your browser to test it.
// ---------------------------------------------------------------------
app.get("/customers", async (req, res) => {
  // Stop early if the server isn't connected to Supabase
  if (!supabase) {
    return res.status(500).json({ success: false, message: "Server is not connected to the database." });
  }

  // SELECT all rows. This is the same as the SQL:
  // SELECT * FROM customer ORDER BY customerid ASC;
  const { data, error } = await supabase
    .from("customer")
    .select("*")
    .order("customerid", { ascending: true });

  if (error) {
    console.log("Select error:", error.message);
    return res.status(500).json({ success: false, message: "Database error: " + error.message });
  }

  return res.status(200).json({
    success: true,
    count: data.length,
    customers: data
  });
});

app.listen(PORT, () => {
  console.log("Server is running on port " + PORT);
});