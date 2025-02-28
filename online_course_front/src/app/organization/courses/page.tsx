"use client";

import { useState, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { PlusCircle } from "lucide-react";
import { CourseList } from "@/components/courses/course-list";
import { TeacherList } from '@/components/teachers/teacher-list';
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import Link from "next/link";

export default function OrganizationCoursesPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [organizationId, setOrganizationId] = useState(
    searchParams.get("organizationId") || ""
  );

  const handleSubmit = () => {
    if (!organizationId) return;
    router.push(`/organization/courses?organizationId=${organizationId}`);
  };

  return (
    <main className="container py-6 space-y-6">
      <div className="flex items-center">
        <h1 className="text-2xl font-semibold">机构管理</h1>
        <div className="ml-4">
          <Input
            type="number"
            placeholder="请输入机构ID"
            value={organizationId}
            onChange={(e) => setOrganizationId(e.target.value)}
            className="w-40"
          />
        </div>
      </div>

      <Tabs defaultValue="courses" className="space-y-6">
        <TabsList>
          <TabsTrigger value="courses">课程管理</TabsTrigger>
          <TabsTrigger value="teachers">教师管理</TabsTrigger>
        </TabsList>

        <TabsContent value="courses">
          <div className="mb-4 flex justify-end">
            <Button asChild>
              <Link href={`/courses/create?organizationId=${organizationId}`}>
                <PlusCircle className="w-4 h-4 mr-2" />
                创建课程
              </Link>
            </Button>
          </div>
          <Suspense 
            fallback={
              <div className="space-y-4">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-32 animate-pulse bg-muted rounded-lg" />
                ))}
              </div>
            }
          >
            <CourseList showAllStatus organizationId={Number(organizationId)} />
          </Suspense>
        </TabsContent>

        <TabsContent value="teachers">
          <Suspense
            fallback={
              <div className="space-y-4">
                {Array.from({ length: 3 }).map((_, i) => (
                  <div key={i} className="h-32 animate-pulse bg-muted rounded-lg" />
                ))}
              </div>
            }
          >
            <TeacherList organizationId={Number(organizationId)} />
          </Suspense>
        </TabsContent>
      </Tabs>
    </main>
  );
} 