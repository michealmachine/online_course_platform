"use client";

import { useRouter, useSearchParams } from 'next/navigation';
import { Tabs } from "@/components/ui/tabs";
import { TabsContent } from "@/components/ui/tabs";
import { TabsList } from "@/components/ui/tabs";
import { TabsTrigger } from "@/components/ui/tabs";
import { CourseForm } from '@/components/courses/course-form';
import { CourseChapterList } from '@/components/courses/course-chapter-list';

interface Props {
  params: {
    courseId: string;
  };
}

export default function EditCoursePage({ params }: Props) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const organizationId = searchParams.get('organizationId');
  const courseId = Number(params.courseId);

  if (!organizationId) {
    router.push('/organization/courses');
    return null;
  }

  return (
    <div className="container mx-auto p-4">
      <Tabs defaultValue="basic">
        <TabsList>
          <TabsTrigger value="basic">基本信息</TabsTrigger>
          <TabsTrigger value="chapter">课程计划</TabsTrigger>
          <TabsTrigger value="teacher">课程教师</TabsTrigger>
          <TabsTrigger value="resource">课程资源</TabsTrigger>
        </TabsList>

        <TabsContent value="basic">
          <CourseForm 
            courseId={courseId}
            organizationId={Number(organizationId)}
            onSuccess={() => {
              router.refresh();
            }}
          />
        </TabsContent>

        <TabsContent value="chapter">
          <CourseChapterList 
            courseId={courseId}
            organizationId={Number(organizationId)}
          />
        </TabsContent>

        <TabsContent value="teacher">
          <div>教师管理（开发中）</div>
        </TabsContent>

        <TabsContent value="resource">
          <div>资源管理（开发中）</div>
        </TabsContent>
      </Tabs>
    </div>
  );
} 