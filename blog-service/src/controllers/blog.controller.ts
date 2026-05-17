import { Response } from "express";
import axios from "axios";
import Blog from "../models/blog.model";
import { AuthRequest } from "../middleware/auth.middleware";

const FOLLOWER_SERVICE_URL =
  process.env.FOLLOWER_SERVICE_URL || "http://follower-service:8084";

function parsePositiveInt(value: unknown, fallback: number): number {
  if (typeof value !== "string") {
    return fallback;
  }

  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed) || parsed < 1) {
    return fallback;
  }

  return parsed;
}

/**
 * POST /api/blogs
 * Creates a new blog post. Requires authentication.
 */
export async function createBlog(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const { title, description, images } = req.body;

    if (!title || !description) {
      res
        .status(400)
        .json({ message: "Title and description are required" });
      return;
    }

    const blog = new Blog({
      authorId: req.user!.userId,
      title,
      description,
      images: images || [],
    });

    const savedBlog = await blog.save();
    res.status(201).json(savedBlog);
  } catch (error) {
    res.status(500).json({ message: "Failed to create blog", error });
  }
}

/**
 * POST /api/blogs/:id/comments
 * Adds a comment to an existing blog post. Requires authentication.
 */
export async function addComment(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const { id } = req.params;
    const { text } = req.body;

    if (!text) {
      res.status(400).json({ message: "Comment text is required" });
      return;
    }

    const blog = await Blog.findById(id);
    if (!blog) {
      res.status(404).json({ message: "Blog not found" });
      return;
    }

    // Check that the requesting user follows the blog author
    const authorId = blog.authorId;
    const requestingUserId = req.user!.userId;

    if (requestingUserId !== authorId) {
      try {
        const followerResponse = await axios.get(
          `${FOLLOWER_SERVICE_URL}/api/followers/is-following/${authorId}`,
          {
            headers: {
              Authorization: req.headers.authorization ?? "",
            },
          }
        );

        if (followerResponse.data?.isFollowing === false) {
          res.status(403).json({
            message: "Morate pratiti autora da biste komentarisali",
          });
          return;
        }
      } catch (err: any) {
        if (err.response) {
          // follower-service returned an error (e.g. 401)
          res
            .status(err.response.status)
            .json({ message: "Follower service error", error: err.response.data });
          return;
        }
        // Network / connectivity error
        res
          .status(503)
          .json({ message: "Follower service unavailable" });
        return;
      }
    }

    const now = new Date();
    const comment = {
      userId: req.user!.userId,
      username: req.user!.username,
      text,
      createdAt: now,
      updatedAt: now,
    };

    blog.comments.push(comment as any);
    const updatedBlog = await blog.save();

    res.status(200).json(updatedBlog);
  } catch (error) {
    res.status(500).json({ message: "Failed to add comment", error });
  }
}

/**
 * PUT /api/blogs/:id/comments/:commentId
 * Updates an existing comment. Only the comment author can update it.
 */
export async function updateComment(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const id = req.params.id as string;
    const commentId = req.params.commentId as string;
    const { text } = req.body;

    if (!text) {
      res.status(400).json({ message: "Comment text is required" });
      return;
    }

    const blog = await Blog.findById(id);
    if (!blog) {
      res.status(404).json({ message: "Blog not found" });
      return;
    }

    const comment = blog.comments.id(commentId);
    if (!comment) {
      res.status(404).json({ message: "Comment not found" });
      return;
    }

    // Only the comment author can update their own comment
    if (comment.userId !== req.user!.userId) {
      res
        .status(403)
        .json({ message: "Not authorized to update this comment" });
      return;
    }

    comment.text = text;
    comment.updatedAt = new Date();

    const updatedBlog = await blog.save();
    res.status(200).json(updatedBlog);
  } catch (error) {
    res.status(500).json({ message: "Failed to update comment", error });
  }
}

/**
 * GET /api/blogs
 * Returns a paginated list of blogs. Requires authentication.
 */
export async function getBlogs(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const maxPageSize = parsePositiveInt(process.env.MAX_PAGE_SIZE, 20);
    const page = parsePositiveInt(req.query.page, 1);
    const limit = Math.min(parsePositiveInt(req.query.limit, 10), maxPageSize);
    const authorId =
      typeof req.query.authorId === "string" ? req.query.authorId : undefined;

    const filter = authorId ? { authorId } : {};

    const [blogs, total] = await Promise.all([
      Blog.find(filter)
        .sort({ createdAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit),
      Blog.countDocuments(filter),
    ]);

    const totalPages = Math.ceil(total / limit) || 1;

    res.status(200).json({
      data: blogs,
      total,
      page,
      limit,
      totalPages,
      message: "OK",
    });
  } catch (error) {
    res
      .status(500)
      .json({ error: "Failed to fetch blogs", details: String(error) });
  }
}

/**
 * GET /api/blogs/:id
 * Returns a single blog by id. Requires authentication.
 */
export async function getBlogById(
  req: AuthRequest,
  res: Response
): Promise<void> {
  try {
    const id = req.params.id as string;
    const blog = await Blog.findById(id);

    if (!blog) {
      res.status(404).json({ error: "Blog not found" });
      return;
    }

    res.status(200).json({ data: blog, message: "OK" });
  } catch (error) {
    res
      .status(500)
      .json({ error: "Failed to fetch blog", details: String(error) });
  }
}
