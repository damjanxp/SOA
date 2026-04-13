import { Router } from "express";
import { authMiddleware } from "../middleware/auth.middleware";
import {
  createBlog,
  getBlogs,
  getBlogById,
  addComment,
  updateComment,
} from "../controllers/blog.controller";
import { likeBlog, unlikeBlog } from "../controllers/like.controller";

const router = Router();


router.post("/", authMiddleware, createBlog);


router.get("/", authMiddleware, getBlogs);


router.get("/:id", authMiddleware, getBlogById);


router.post("/:id/comments", authMiddleware, addComment);


router.put("/:id/comments/:commentId", authMiddleware, updateComment);


router.post("/:id/like", authMiddleware, likeBlog);


router.delete("/:id/like", authMiddleware, unlikeBlog);

export default router;
