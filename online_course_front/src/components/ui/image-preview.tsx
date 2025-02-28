"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import Image from "next/image";
import { VisuallyHidden } from '@/components/ui/visually-hidden';

interface ImagePreviewProps {
  src: string;
  alt?: string;
  open: boolean;
  onClose: () => void;
}

export function ImagePreview({ src, alt, open, onClose }: ImagePreviewProps) {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl p-0 overflow-hidden bg-transparent border-0">
        <DialogHeader>
          <VisuallyHidden>
            <DialogTitle>图片预览</DialogTitle>
          </VisuallyHidden>
        </DialogHeader>
        <div className="relative w-full aspect-video">
          <Image
            src={src}
            alt={alt || "Preview"}
            fill
            className="object-contain"
            priority
          />
        </div>
      </DialogContent>
    </Dialog>
  );
} 