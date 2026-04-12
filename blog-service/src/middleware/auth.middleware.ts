import { Request, Response, NextFunction } from "express";
import jwt from "jsonwebtoken";

// Extended request interface with user payload
export interface AuthRequest extends Request {
  user?: {
    userId: string;
    username: string;
    role: string;
  };
}

/**
 * Express middleware that validates JWT from the Authorization header.
 * Expects format: "Bearer <token>"
 * On success, attaches decoded payload (userId, username, role) to req.user.
 */
export function authMiddleware(
  req: AuthRequest,
  res: Response,
  next: NextFunction
): void {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    res.status(401).json({ message: "Authorization token is missing" });
    return;
  }

  const token = authHeader.split(" ")[1];

  try {
    const secret = process.env.JWT_SECRET;
    if (!secret) {
      res.status(401).json({ message: "JWT secret is not configured" });
      return;
    }

    const decoded = jwt.verify(token, secret) as unknown as {
      userId: string;
      username: string;
      role: string;
    };

    req.user = {
      userId: decoded.userId,
      username: decoded.username,
      role: decoded.role,
    };

    next();
  } catch {
    res.status(401).json({ message: "Invalid or expired token" });
    return;
  }
}
