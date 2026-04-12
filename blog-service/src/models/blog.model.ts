import mongoose, { Schema, Document, Types } from "mongoose";

// TypeScript interface for the Comment subdocument
export interface IComment {
  _id: Types.ObjectId;
  userId: string;
  username: string;
  text: string;
  createdAt: Date;
  updatedAt: Date;
}

// TypeScript interface for the Blog document
export interface BlogDocument extends Document {
  authorId: string;
  title: string;
  description: string;
  createdAt: Date;
  images: string[];
  comments: Types.DocumentArray<IComment & Document>;
  likes: string[];
  likeCount: number;
}

// Comment subdocument schema
const CommentSchema = new Schema<IComment>(
  {
    userId: { type: String, required: true },
    username: { type: String, required: true },
    text: { type: String, required: true },
    createdAt: { type: Date, default: Date.now },
    updatedAt: { type: Date, default: Date.now },
  },
  { _id: true }
);

// Blog document schema
const BlogSchema = new Schema<BlogDocument>({
  authorId: { type: String, required: true },
  title: { type: String, required: true },
  description: { type: String, required: true }, // stored as raw markdown string
  createdAt: { type: Date, default: Date.now },
  images: { type: [String], default: [] }, // array of image URLs
  comments: { type: [CommentSchema], default: [] },
  likes: { type: [String], default: [] }, // array of userIds
  likeCount: { type: Number, default: 0 },
});

const Blog = mongoose.model<BlogDocument>("Blog", BlogSchema);

export default Blog;
