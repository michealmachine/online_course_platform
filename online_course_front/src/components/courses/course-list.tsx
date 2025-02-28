"use client";

import { CourseService, type CourseBase, COURSE_STATUS, COURSE_STATUS_LABELS, COURSE_AUDIT_STATUS, COURSE_AUDIT_STATUS_LABELS } from '@/services/course';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { MoreHorizontal } from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useState, useEffect } from 'react';
import { Pagination } from '@/components/common/pagination';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { useToast } from "@/hooks/use-toast";
import { useRouter } from 'next/navigation';
import { getMediaUrl } from "@/lib/utils/url";

/**
 * 获取课程列表数据
 * @param searchParams URL 查询参数，包含分页、搜索和筛选条件
 * @returns 返回课程列表数据，包含课程项目、总数、当前页码和每页数量
 * 
 * @example
 * // 示例查询参数
 * searchParams = {
 *   page: "1",        // 页码
 *   courseName: "",   // 课程名称搜索
 *   status: "",       // 课程状态筛选
 *   mt: ""           // 课程分类筛选
 * }
 */
async function getCourses(searchParams: URLSearchParams) {
  const params = {
    pageNo: searchParams.get('page') || '1',
    pageSize: '10',
    courseName: searchParams.get('courseName') || '',
    status: searchParams.get('status') || '',
    mt: searchParams.get('mt') || '',
  };

  try {
    return await CourseService.getCourseList(params);
  } catch (error) {
    console.error('获取课程列表失败:', error);
    return {
      items: [],
      counts: 0,
      page: 1,
      pageSize: 10,
    };
  }
}

/**
 * 课程状态映射表
 * 202001: 未发布 - 课程创建后的初始状态
 * 202002: 已发布 - 课程审核通过并发布
 * 202003: 已下线 - 课程从线上环境下线
 */
const CourseStatus: Record<string, string> = {
  '202001': '未发布',
  '202002': '已发布',
  '202003': '已下线',
};

// 使用渐变色作为默认封面
const DEFAULT_COURSE_COVER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjI0MCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImdyYWQiIHgxPSIwJSIgeTE9IjAlIiB4Mj0iMTAwJSIgeTI9IjEwMCUiPjxzdG9wIG9mZnNldD0iMCUiIHN0eWxlPSJzdG9wLWNvbG9yOiMwZWE1ZTk7c3RvcC1vcGFjaXR5OjEiIC8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdHlsZT0ic3RvcC1jb2xvcjojMGQ5NDg4O3N0b3Atb3BhY2l0eToxIiAvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHdpZHRoPSI0MDAiIGhlaWdodD0iMjQwIiBmaWxsPSJ1cmwoI2dyYWQpIi8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtc2l6ZT0iMjQiIGZpbGw9IiNmZmYiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmb250LWZhbWlseT0ic2Fucy1zZXJpZiI+Q291cnNlPC90ZXh0Pjwvc3ZnPg==';

// 状态选项
const STATUS_OPTIONS = [
  { label: COURSE_STATUS_LABELS.ALL, value: "ALL" },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.DRAFT], value: COURSE_STATUS.DRAFT },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.PUBLISHED], value: COURSE_STATUS.PUBLISHED },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.OFFLINE], value: COURSE_STATUS.OFFLINE },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.SUBMITTED], value: COURSE_STATUS.SUBMITTED },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.APPROVED], value: COURSE_STATUS.APPROVED },
  { label: COURSE_STATUS_LABELS[COURSE_STATUS.REJECTED], value: COURSE_STATUS.REJECTED },
];

interface Props {
  organizationId: number;
  showAllStatus?: boolean;
}

/**
 * 课程列表组件
 * 展示课程卡片列表，包含课程基本信息和操作选项
 * 
 * 功能：
 * 1. 展示课程封面图、名称、简介
 * 2. 显示课程分类、价格、状态等标签
 * 3. 提供课程编辑、课程计划、提交审核、下线等操作
 * 4. 支持分页和筛选
 * 
 * @returns 课程列表界面
 */
export function CourseList({ organizationId, showAllStatus }: Props) {
  const searchParams = useSearchParams();
  const [courses, setCourses] = useState<CourseBase[]>([]);
  const [loading, setLoading] = useState(true);
  const [total, setTotal] = useState(0);
  const currentPage = Number(searchParams.get('page') || '1');
  const pageSize = 10;
  const { toast } = useToast();
  const [courseToDelete, setCourseToDelete] = useState<CourseBase | null>(null);
  const router = useRouter();

  useEffect(() => {
    const fetchCourses = async () => {
      setLoading(true);
      try {
        if (showAllStatus) {
          // 机构课程列表 - 显示所有状态
          const data = await CourseService.getOrganizationCourseList(
            organizationId,
            {
              pageNo: currentPage,
              pageSize
            },
            searchParams.get('status') || undefined,
            searchParams.get('auditStatus') || undefined
          );
          setCourses(data.items);
          setTotal(data.counts);
        } else {
          // 公开课程列表 - 只显示已发布的
          const data = await CourseService.getPublicCourseList(
            {
              pageNo: currentPage,
              pageSize
            },
            {
              courseName: searchParams.get('courseName') || undefined,
              mt: searchParams.get('mt') || undefined,
              st: searchParams.get('st') || undefined
            }
          );
          setCourses(data.items);
          setTotal(data.counts);
        }
      } catch (error) {
        console.error('获取课程列表失败:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchCourses();
  }, [searchParams, organizationId, showAllStatus, currentPage]);

  // 处理删除
  const handleDelete = async (courseId: number) => {
    try {
      setLoading(true);
      await CourseService.deleteCourse(courseId);
      toast({
        title: "删除成功",
        description: "课程已删除",
      });
      const data = await CourseService.getOrganizationCourseList(
        organizationId,
        {
          pageNo: currentPage,
          pageSize
        },
        searchParams.get('status') || undefined
      );
      setCourses(data.items);
      setTotal(data.counts);
    } catch (error) {
      console.error('删除课程失败:', error);
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
      setCourseToDelete(null);
    }
  };

  if (loading) {
    return (
      <div className="text-center py-10">
        <p className="text-muted-foreground">加载中...</p>
      </div>
    );
  }

  if (!courses.length) {
    return (
      <div className="text-center py-10">
        <p className="text-muted-foreground">暂无课程数据</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        {courses.map((course) => (
          <Card key={course.id}>
            <CardContent className="p-4">
              <div className="flex items-start gap-4">
                {/* 修改图片部分，添加默认图片和错误处理 */}
                <div className="relative w-40 h-24 bg-muted rounded-lg overflow-hidden">
                  <Image
                    src={course.logo ? getMediaUrl(course.logo) : DEFAULT_COURSE_COVER}
                    alt={course.name}
                    fill
                    className="object-cover"
                    priority
                    // 添加 onError 处理图片加载失败的情况
                    onError={(e) => {
                      const target = e.target as HTMLImageElement;
                      target.src = DEFAULT_COURSE_COVER;
                    }}
                  />
                </div>
                
                <div className="flex-1">
                  {/* 课程标题和操作菜单 */}
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold">
                        <Link href={`/courses/${course.id}`} className="hover:underline">
                          {course.name}
                        </Link>
                      </h3>
                      <p className="text-sm text-muted-foreground mt-1">
                        {course.brief}
                      </p>
                    </div>
                    
                    {/* 课程操作下拉菜单 */}
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                          <span className="sr-only">打开菜单</span>
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem asChild>
                          <Link href={`/courses/${course.id}/edit?organizationId=${organizationId}`}>编辑</Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem asChild>
                          <Link href={`/courses/${course.id}/plan`}>课程计划</Link>
                        </DropdownMenuItem>
                        {/* 查看审核状态选项 */}
                        <DropdownMenuItem
                          onClick={async () => {
                            try {
                              const status = await CourseService.getAuditStatus(course.id, organizationId);
                              // 根据状态码显示不同颜色的提示
                              toast({
                                title: "课程审核状态",
                                description: (
                                  <div className={`font-medium ${
                                    status === COURSE_AUDIT_STATUS.APPROVED 
                                      ? 'text-green-600'
                                      : status === COURSE_AUDIT_STATUS.REJECTED
                                      ? 'text-red-600'
                                      : 'text-yellow-600'
                                  }`}>
                                    {COURSE_AUDIT_STATUS_LABELS[status] || '未知状态'}
                                  </div>
                                ),
                              });
                            } catch (error) {
                              toast({
                                title: "获取状态失败",
                                description: error instanceof Error ? error.message : "未知错误",
                                variant: "destructive",
                              });
                            }
                          }}
                        >
                          查看审核状态
                        </DropdownMenuItem>
                        {/* 根据课程状态显示不同的操作选项 */}
                        {course.status === COURSE_STATUS.DRAFT && (
                          <DropdownMenuItem
                            onClick={async () => {
                              try {
                                await CourseService.submitForAudit(course.id);
                                toast({
                                  title: "提交成功",
                                  description: "课程已提交审核",
                                });
                                // 刷新课程列表
                                router.refresh();
                              } catch (error) {
                                toast({
                                  title: "提交失败",
                                  description: error instanceof Error ? error.message : "未知错误",
                                  variant: "destructive",
                                });
                              }
                            }}
                          >
                            提交审核
                          </DropdownMenuItem>
                        )}
                        {course.status === COURSE_STATUS.APPROVED && (
                          <DropdownMenuItem
                            onClick={() => CourseService.offlineCourse(course.id)}
                          >
                            下线
                          </DropdownMenuItem>
                        )}
                        {/* 添加删除选项 */}
                        <DropdownMenuItem
                          onClick={() => setCourseToDelete(course)}
                          className="text-red-600"
                        >
                          删除
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                  
                  {/* 课程标签信息 */}
                  <div className="flex items-center gap-2 mt-2">
                    {course.mtName && (
                      <span className="text-sm px-2 py-1 rounded-full bg-primary/10 text-primary">
                        {course.mtName}
                      </span>
                    )}
                    {course.stName && (
                      <span className="text-sm px-2 py-1 rounded-full bg-primary/10 text-primary">
                        {course.stName}
                      </span>
                    )}
                    <span className="text-sm px-2 py-1 rounded-full bg-primary/10 text-primary">
                      {course.charge === '201001' ? '免费' : `¥${course.price}`}
                    </span>
                    <span className="text-sm px-2 py-1 rounded-full bg-primary/10 text-primary">
                      {COURSE_STATUS_LABELS[course.status]}
                    </span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* 分页组件 */}
      <Pagination
        total={total}
        page={currentPage}
        pageSize={pageSize}
      />

      {/* 删除确认对话框 */}
      <AlertDialog open={!!courseToDelete} onOpenChange={(open) => !open && setCourseToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              确定要删除课程 "{courseToDelete?.name}" 吗？此操作无法撤销。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => courseToDelete && handleDelete(courseToDelete.id)}
              className="bg-red-600 hover:bg-red-700"
            >
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
} 