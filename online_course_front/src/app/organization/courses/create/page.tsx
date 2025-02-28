"use client";

import { useRouter, useSearchParams } from 'next/navigation';
import { CourseForm } from '@/components/courses/course-form';

export default function CreateCoursePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const organizationId = searchParams.get('organizationId');

  if (!organizationId) {
    router.push('/organization/courses');
    return null;
  }

  return (
    <div className="container mx-auto p-4">
      <CourseForm 
        organizationId={Number(organizationId)}
        onSuccess={(courseId) => {
          router.push(`/organization/courses/${courseId}/edit?organizationId=${organizationId}`);
        }}
      />
    </div>
  );
} 