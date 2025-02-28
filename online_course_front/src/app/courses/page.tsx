import { Suspense } from 'react';
import { CourseList } from '@/components/courses/course-list';
import { CourseFilter } from '@/components/courses/course-filter';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { PlusCircle } from 'lucide-react';

export default function CoursesPage() {
  return (
    <main className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">课程管理</h1>
        <Button asChild>
          <Link href="/courses/create">
            <PlusCircle className="w-4 h-4 mr-2" />
            创建课程
          </Link>
        </Button>
      </div>

      <Suspense 
        fallback={
          <div className="h-20 animate-pulse bg-muted rounded-lg" />
        }
      >
        <CourseFilter />
      </Suspense>

      <Suspense
        fallback={
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="h-32 animate-pulse bg-muted rounded-lg" />
            ))}
          </div>
        }
      >
        <CourseList />
      </Suspense>
    </main>
  );
} 