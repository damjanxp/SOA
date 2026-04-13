import { Response } from "express";
import Blog from "../models/blog.model";
import { AuthRequest } from "../middleware/auth.middleware";


export async function likeBlog(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const id = req.params.id as string;
    const userId = req.user?.userId;

    if (!userId) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const blog = await Blog.findById(id);
    if (!blog) {
      res.status(404).json({ error: "Blog not found" });
      return;
    }

    if (blog.likes.includes(userId)) {
      res.status(409).json({ error: "You already liked this blog" });
      return;
    }

    const updatedBlog = await Blog.findByIdAndUpdate(
      id,
      { $addToSet: { likes: userId }, $inc: { likeCount: 1 } },
      { new: true }
    );

    if (!updatedBlog) {
      res.status(404).json({ error: "Blog not found" });
      return;
    }

    res
      .status(200)
      .json({ data: { likeCount: updatedBlog.likeCount }, message: "OK" });
  } catch (error) {
    res
      .status(500)
      .json({ error: "Failed to like blog", details: String(error) });
  }
}

export async function unlikeBlog(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const id = req.params.id as string;
    const userId = req.user?.userId;

    if (!userId) {
      res.status(401).json({ error: "Unauthorized" });
      return;
    }

    const blog = await Blog.findById(id);
    if (!blog) {
      res.status(404).json({ error: "Blog not found" });
      return;
    }

    if (!blog.likes.includes(userId)) {
      res.status(404).json({ error: "You have not liked this blog" });
      return;
    }

    const updatedBlog = await Blog.findByIdAndUpdate(
      id,
      { $pull: { likes: userId }, $inc: { likeCount: -1 } },
      { new: true }
    );

    if (!updatedBlog) {
      res.status(404).json({ error: "Blog not found" });
      return;
    }

    res
      .status(200)
      .json({ data: { likeCount: updatedBlog.likeCount }, message: "OK" });
  } catch (error) {
    res
      .status(500)
      .json({ error: "Failed to unlike blog", details: String(error) });
  }
}
