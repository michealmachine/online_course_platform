"use client";

import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useRouter, useSearchParams } from 'next/navigation';
import { useCallback } from 'react';

/**
 * 课程状态选项
 */
const STATUS_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '未发布', value: '202001' },
  { label: '已发布', value: '202002' },
  { label: '已下线', value: '202003' },
];

/**
 * 课程分类选项
 */
const CATEGORY_OPTIONS = [
  { label: '全部', value: 'all' },
  { label: '前端开发', value: '1' },
  { label: '后端开发', value: '2' },
  { label: '移动开发', value: '3' },
];

/**
 * 课程过滤器组件
 * 提供课程列表的搜索和筛选功能，包括：
 * 1. 课程名称搜索
 * 2. 课程状态筛选（未发布、已发布、已下线）
 * 3. 课程分类筛选（前端开发、后端开发、移动开发）
 * 4. 重置筛选条件
 * 
 * @example
 * // 在课程列表页面使用
 * <CourseFilter />
 */
export function CourseFilter() {
  const router = useRouter();
  const searchParams = useSearchParams();

  /**
   * 创建查询字符串
   * 处理 URL 查询参数的添加、更新和删除
   * 
   * @param params 需要更新的参数对象
   * @returns 格式化后的查询字符串
   * 
   * @example
   * // 添加或更新参数
   * createQueryString({ courseName: "React" })
   * // 删除参数
   * createQueryString({ courseName: "" })
   */
  const createQueryString = useCallback(
    (params: Record<string, string>) => {
      const newSearchParams = new URLSearchParams(searchParams.toString());
      Object.entries(params).forEach(([key, value]) => {
        if (value === 'all') {
          newSearchParams.delete(key);
        } else if (value) {
          newSearchParams.set(key, value);
        } else {
          newSearchParams.delete(key);
        }
      });
      return newSearchParams.toString();
    },
    [searchParams]
  );

  /**
   * 处理搜索和筛选
   * 根据用户输入更新 URL 查询参数，触发课程列表更新
   * 
   * @param params 搜索和筛选参数
   */
  const handleSearch = (params: Record<string, string>) => {
    const queryString = createQueryString(params);
    router.push(`/courses?${queryString}`);
  };

  return (
    <div className="bg-card border rounded-lg p-4">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {/* 课程名称搜索 */}
        <div className="space-y-2">
          <label className="text-sm font-medium">课程名称</label>
          <Input
            placeholder="搜索课程名称"
            defaultValue={searchParams.get('courseName') || ''}
            onChange={(e) => handleSearch({ courseName: e.target.value })}
          />
        </div>
        
        {/* 课程状态筛选 */}
        <div className="space-y-2">
          <label className="text-sm font-medium">课程状态</label>
          <Select
            value={searchParams.get('status') || 'all'}
            onValueChange={(value) => handleSearch({ status: value })}
          >
            <SelectTrigger>
              <SelectValue placeholder="选择状态" />
            </SelectTrigger>
            <SelectContent>
              {STATUS_OPTIONS.map(option => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 课程分类筛选 */}
        <div className="space-y-2">
          <label className="text-sm font-medium">课程分类</label>
          <Select
            value={searchParams.get('mt') || 'all'}
            onValueChange={(value) => handleSearch({ mt: value })}
          >
            <SelectTrigger>
              <SelectValue placeholder="选择分类" />
            </SelectTrigger>
            <SelectContent>
              {CATEGORY_OPTIONS.map(option => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* 重置按钮 */}
        <div className="flex items-end">
          <Button
            variant="outline"
            onClick={() => {
              router.push('/courses');
            }}
          >
            重置
          </Button>
        </div>
      </div>
    </div>
  );
} 