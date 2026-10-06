// =====================================================================
//  SERVER (server/server.js)  -  STEP 1: LOGIN ONLY
//  A small Express server that the Android app calls to check a login.
//
//  Routes:
//    GET  /       -> check if the server is running (open it in a browser)
//    POST /login  -> check if the username and password are registered
//
//  The registered accounts are saved only in memory (in the array below).
//  The OTP feature will be added here in the next step.
// =====================================================================

// Express is a library that makes it easy to create a web server in Node.js
const express = require("express");
const app = express();

// Lets the server read JSON data sent by the Android app
// (for example: { "username": "student", "password": "password123" })
app.use(express.json());

// Render gives us the port number in process.env.PORT.
// When running on your own laptop, it uses 3000 instead.
const PORT = process.env.PORT || 3000;

// ---------------------------------------------------------------------
//  REGISTERED ACCOUNTS
//  Only these accounts can log in. Add more by copying one { ... } block.
// ---------------------------------------------------------------------
const users = [
  { username: "student", password: "password123" },
  { username: "admin", password: "admin1234" }
];

// ---------------------------------------------------------------------
//  GET /
//  Open your Render link in a browser to see if the server is running.
// ---------------------------------------------------------------------
app.get("/", (req, res) => {
  res.json({ message: "Server is running!" });
});

// ---------------------------------------------------------------------
//  POST /login
//  Expects: { username, password }
// ---------------------------------------------------------------------
app.post("/login", (req, res) => {
  // Take the values out of the request.
  // "|| ''" means: if a value is missing, use an empty string instead.
  const username = (req.body.username || "").trim();
  const password = req.body.password || "";

  // 1. Check that no field is empty
  if (username === "" || password === "") {
    // 400 = "Bad Request" (the user sent incomplete data)
    return res.status(400).json({ success: false, message: "Please enter your username and password." });
  }

  // 2. Look for a registered user with this username.
  //    .find() loops through the array and returns the first match (or undefined).
  //    toLowerCase() makes "Student" and "student" count as the same username.
  const user = users.find(u => u.username.toLowerCase() === username.toLowerCase());

  // 3. If the user isn't registered, or the password is wrong, reject the login.
  //    The same message is used for both, so nobody can guess which usernames exist.
  if (!user || user.password !== password) {
    // 401 = "Unauthorized"
    return res.status(401).json({ success: false, message: "Incorrect username or password." });
  }

  // 4. Login successful (200 = "OK")
  return res.status(200).json({ success: true, message: "Login successful! Welcome, " + user.username + "." });
});

// ---------------------------------------------------------------------
//  Start the server
// ---------------------------------------------------------------------
app.listen(PORT, () => {
  console.log("Server is running on port " + PORT);
});