import { Router } from "express";
import { authMiddleware } from "../middleware/auth.middleware";
import {
  createBlog,
  addComment,
  updateComment,
} from "../controllers/blog.controller";

const router = Router();

// Create a new blog post
router.post("/", authMiddleware, createBlog);

// Add a comment to a blog post
router.post("/:id/comments", authMiddleware, addComment);

// Update an existing comment
router.put("/:id/comments/:commentId", authMiddleware, updateComment);

export default router;
