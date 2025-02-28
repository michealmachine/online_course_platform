"use client";

import { useState, useRef } from 'react';
import ReactCrop, { Crop } from 'react-image-crop';
import 'react-image-crop/dist/ReactCrop.css';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';

interface ImageCropperProps {
  open: boolean;
  onClose: () => void;
  imageUrl: string;
  aspectRatio?: number;
  onCropComplete: (croppedImage: Blob) => void;
}

export function ImageCropper({
  open,
  onClose,
  imageUrl,
  aspectRatio = 16/9,
  onCropComplete
}: ImageCropperProps) {
  const [crop, setCrop] = useState<Crop>({
    unit: '%',
    width: 90,
    height: (90 / aspectRatio),
    x: 5,
    y: 5
  });
  const imageRef = useRef<HTMLImageElement>(null);

  const getCroppedImg = async () => {
    if (!imageRef.current) return;

    const image = imageRef.current;
    const canvas = document.createElement('canvas');
    const scaleX = image.naturalWidth / image.width;
    const scaleY = image.naturalHeight / image.height;
    
    canvas.width = crop.width;
    canvas.height = crop.height;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(
      image,
      crop.x * scaleX,
      crop.y * scaleY,
      crop.width * scaleX,
      crop.height * scaleY,
      0,
      0,
      crop.width,
      crop.height
    );

    // 转换为blob
    return new Promise<Blob>((resolve) => {
      canvas.toBlob((blob) => {
        if (blob) resolve(blob);
      }, 'image/jpeg', 0.95);
    });
  };

  const handleComplete = async () => {
    const croppedImage = await getCroppedImg();
    if (croppedImage) {
      onCropComplete(croppedImage);
      onClose();
    }
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="max-w-3xl">
        <DialogHeader>
          <DialogTitle>裁剪图片</DialogTitle>
        </DialogHeader>
        <div className="my-4">
          <ReactCrop
            crop={crop}
            onChange={c => setCrop(c)}
            aspect={aspectRatio}
            className="max-h-[600px] object-contain"
          >
            <img
              ref={imageRef}
              src={imageUrl}
              alt="Crop me"
              className="max-w-full h-auto"
            />
          </ReactCrop>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>取消</Button>
          <Button onClick={handleComplete}>确认</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
} 