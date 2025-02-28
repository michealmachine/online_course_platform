"use client";

import { CourseForm } from '@/components/courses/course-form';
import { useRouter } from 'next/navigation';
import { useSearchParams } from 'next/navigation';

export default function CreateCoursePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const organizationId = Number(searchParams.get('organizationId'));

  const handleSuccess = (courseId: number) => {
    // 添加成功后跳转到课程列表页
    router.push('/organization/courses?organizationId=' + organizationId);
    router.refresh(); // 刷新数据
  };

  return (
    <main className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">创建课程</h1>
      </div>

      <div className="max-w-2xl mx-auto">
        <CourseForm 
          organizationId={organizationId}
          onSuccess={handleSuccess} 
        />
      </div>
    </main>
  );
} 