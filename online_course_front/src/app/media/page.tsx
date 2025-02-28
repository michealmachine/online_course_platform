"use client";

import { useState, useEffect } from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from "@/components/ui/card";
import { VideoUpload } from "@/components/upload/video-upload";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useToast } from "@/hooks/use-toast";

export default function MediaManagementPage() {
  const { toast } = useToast();
  const [organizationId, setOrganizationId] = useState<string>("1");
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    const savedOrgId = localStorage.getItem('defaultOrganizationId');
    if (savedOrgId) {
      setOrganizationId(savedOrgId);
    }
  }, []);

  const handleOrganizationIdChange = (value: string) => {
    setOrganizationId(value);
    localStorage.setItem('defaultOrganizationId', value);
  };

  return (
    <div className="container mx-auto py-6">
      <h1 className="text-2xl font-bold mb-6">媒体管理</h1>
      
      {/* 机构ID输入框 */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>机构设置</CardTitle>
          <CardDescription>
            设置当前操作的机构ID
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Label htmlFor="organizationId">机构ID</Label>
          <Input
            id="organizationId"
            type="number"
            value={organizationId}
            onChange={(e) => handleOrganizationIdChange(e.target.value)}
            placeholder="请输入机构ID"
            className="max-w-xs"
          />
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* 视频上传卡片 */}
        <Card>
          <CardHeader>
            <CardTitle>上传视频</CardTitle>
            <CardDescription>
              支持 MP4 格式视频文件，单个文件最大 500MB
            </CardDescription>
          </CardHeader>
          <CardContent>
            <VideoUpload 
              organizationId={parseInt(organizationId)}
              onUploadComplete={({ fileUrl, mediaFileId }) => {
                console.log('Upload completed:', { fileUrl, mediaFileId });
                toast({
                  title: "上传成功",
                  description: "视频文件上传完成"
                });
                setRefreshKey(prev => prev + 1);
              }}
              onUploadError={(error) => {
                toast({
                  variant: "destructive",
                  title: "上传失败",
                  description: error.message
                });
              }}
            />
          </CardContent>
        </Card>

        {/* 文件列表卡片 */}
        <Card>
          <CardHeader>
            <CardTitle>文件列表</CardTitle>
            <CardDescription>
              当前机构的媒体文件列表
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="text-muted-foreground">
              暂无文件
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
} 