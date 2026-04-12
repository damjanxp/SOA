import dotenv from "dotenv";
dotenv.config();

import express from "express";
import mongoose from "mongoose";
import blogRoutes from "./routes/blog.routes";

const app = express();
const PORT = process.env.PORT || 3000;
const MONGO_URI = process.env.MONGO_URI || "mongodb://localhost:27017/blogdb";

// Parse JSON request bodies
app.use(express.json());

// Mount blog routes
app.use("/api/blogs", blogRoutes);

// Health check endpoint
app.get("/health", (_req, res) => {
  res.status(200).json({ status: "OK" });
});

// Connect to MongoDB and start the server
mongoose
  .connect(MONGO_URI)
  .then(() => {
    console.log("Connected to MongoDB");
    app.listen(PORT, () => {
      console.log(`Blog service running on port ${PORT}`);
    });
  })
  .catch((err) => {
    console.error("Failed to connect to MongoDB:", err);
    process.exit(1);
  });

export default app;
