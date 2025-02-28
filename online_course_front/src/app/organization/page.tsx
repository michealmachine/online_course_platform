import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { CourseList } from "@/components/courses/course-list";
import { TeacherList } from "@/components/teachers/teacher-list";

export default function OrganizationPage() {
  return (
    <main className="container py-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">机构管理</h1>
        {/* 临时：机构ID选择器 */}
        <select className="form-select">
          <option value="1">机构1</option>
          <option value="2">机构2</option>
        </select>
      </div>

      <Tabs defaultValue="courses">
        <TabsList>
          <TabsTrigger value="courses">课程管理</TabsTrigger>
          <TabsTrigger value="teachers">教师管理</TabsTrigger>
        </TabsList>
        
        <TabsContent value="courses">
          <CourseList showAllStatus organizationId={1} />
        </TabsContent>
        
        <TabsContent value="teachers">
          <TeacherList organizationId={1} />
        </TabsContent>
      </Tabs>
    </main>
  );
} 