import { z } from 'zod';

export const UploadStateSchema = z.object({
  status: z.enum(['idle', 'uploading', 'paused', 'completed', 'error']),
  progress: z.number().min(0).max(100),
  uploadedChunks: z.number().min(0),
  totalChunks: z.number().min(0),
  currentFile: z.instanceof(File).optional(),
  uploadId: z.string().optional(),
  error: z.string().optional()
});

export type UploadState = z.infer<typeof UploadStateSchema>; 